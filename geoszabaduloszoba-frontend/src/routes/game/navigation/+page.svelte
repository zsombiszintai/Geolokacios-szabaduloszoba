<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import type { Map as LeafletMap, Marker } from 'leaflet';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';
	import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';

	const adventureId = $derived(page.url.searchParams.get('id'));

	let adventureTitle = $state("Pozíció meghatározása...");
	let userPos = $state<{ lat: number; lon: number } | null>(null);
	let firstStation = $state<any>(null);

	let L: typeof import('leaflet');
	let map: LeafletMap | undefined;
	let playerMarker: Marker | undefined;
	let targetMarker: Marker | null = null;
	let routingControl: any = null;
	let watchId: number | undefined;

	let locationError = $state('');
	let dataError = $state('');
	let startError = $state('');
	let isStarting = $state(false);

	let accuracy = Infinity;
	let lastFixAt = 0;
	let disposed = false;
	let detailsRequested = false;
	let startRequested = false;
	let lastRouteUpdate = 0;

	const controller = new AbortController();

	function calculateDistance(p1: {lat: number, lon: number}, p2: {lat: number, lon: number}) {
		return L.latLng(p1.lat, p1.lon).distanceTo(L.latLng(p2.lat, p2.lon));
	}

	async function setupPathfinder(start: {lat: number, lon: number}, end: {lat: number, lon: number}) {
		if (!map) return;
		if (routingControl) map.removeControl(routingControl);
		if (targetMarker) map.removeLayer(targetMarker);

		const stationIcon = L.divIcon({
			className: 'custom-station-marker',
			html: `<div class="w-6 h-6 bg-green-500 rounded-full border-4 border-white shadow-lg animate-pulse"></div>`,
			iconSize: [24, 24], iconAnchor: [12, 12]
		});

		targetMarker = L.marker([end.lat, end.lon], { icon: stationIcon }).addTo(map);

		routingControl = L.Routing.control({
			waypoints: [L.latLng(start.lat, start.lon), L.latLng(end.lat, end.lon)],
			lineOptions: { styles: [{ color: '#2F5D50', weight: 6, opacity: 0.8 }] },
			addWaypoints: false,
			draggableWaypoints: false,
			show: false,
			createMarker: () => null
		}).addTo(map);
	}

	function updateLocation(newLat: number, newLon: number) {
		if (disposed) return;

		userPos = { lat: newLat, lon: newLon };

		if (!map) {
			initMap(newLat, newLon);
		} else {
			playerMarker?.setLatLng([newLat, newLon]);
			map.panTo([newLat, newLon]);
		}

		if (
			firstStation &&
			routingControl &&
			Date.now() - lastRouteUpdate >= 10000
		) {
			lastRouteUpdate = Date.now();

			routingControl.setWaypoints([
				L.latLng(newLat, newLon),
				L.latLng(firstStation.latitude, firstStation.longitude)
			]);
		}

		checkArrival();
	}

	function checkArrival() {
		if (
			!userPos ||
			!firstStation ||
			!auth.token ||
			locationError ||
			accuracy > 20 ||
			Date.now() - lastFixAt > 15000 ||
			startRequested ||
			startError
		) return;

		const distance = calculateDistance(userPos, {
			lat: firstStation.latitude,
			lon: firstStation.longitude
		});

		if (distance < 15) {
			void triggerGameStart();
		}
	}

	function initMap(lat: number, lon: number) {
		map = L.map('map-container', { zoomControl: false }).setView([lat, lon], 16);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		const userIcon = L.divIcon({
			className: 'custom-user-marker',
			html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
                <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg"></div>
             </div>`,
			iconSize: [32, 32], iconAnchor: [16, 16]
		});

		playerMarker = L.marker([lat, lon], { icon: userIcon }).addTo(map);
	}

	async function fetchAdventureDetails(lat: number, lon: number) {
		try {
			const response = await fetch(
				`https://api.zsomborszintai.com/api/adventures/${encodeURIComponent(adventureId!)}?lat=${lat}&lon=${lon}`,
				{
					headers: { Authorization: `Bearer ${auth.token}` },
					signal: controller.signal
				}
			);

			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}

			const data = await response.json();
			if (disposed) return;

			if (!Array.isArray(data.stations) || data.stations.length === 0) {
				throw new Error('Nincs elérhető állomás.');
			}

			const station = [...data.stations].sort(
				(a, b) => a.seqNumber - b.seqNumber
			)[0];

			if (
				!Number.isFinite(station.latitude) ||
				!Number.isFinite(station.longitude)
			) {
				throw new Error('Az első állomás koordinátái hibásak.');
			}

			adventureTitle = data.title;
			firstStation = station;

			await setupPathfinder(
				userPos ?? { lat, lon },
				{ lat: station.latitude, lon: station.longitude }
			);

			lastRouteUpdate = Date.now();

			checkArrival();
		} catch (error) {
			if (disposed) return;

			console.error('Adatbetöltési hiba:', error);
			dataError = 'A kaland betöltése sikertelen. Töltsd újra az oldalt.';
		}
	}

	$effect(() => {
		const position = userPos;
		const token = auth.token;
		const id = adventureId;

		if (!position || !token || detailsRequested) return;

		if (!id) {
			dataError = 'Hiányzik a kaland azonosítója.';
			return;
		}

		detailsRequested = true;
		void fetchAdventureDetails(position.lat, position.lon);
	});

	async function triggerGameStart() {
		if (
			disposed ||
			startRequested ||
			!auth.token ||
			!adventureId ||
			!userPos ||
			!firstStation ||
			locationError ||
			accuracy > 20 ||
			Date.now() - lastFixAt > 15000
		) return;

		const distance = calculateDistance(userPos, {
			lat: firstStation.latitude,
			lon: firstStation.longitude
		});

		if (distance >= 15) return;

		startRequested = true;
		isStarting = true;
		startError = '';

		try {
			const response = await fetch(
				`https://api.zsomborszintai.com/api/game/start/${encodeURIComponent(adventureId)}`,
				{
					method: 'POST',
					headers: { Authorization: `Bearer ${auth.token}` },
					signal: controller.signal
				}
			);

			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}

			const sessionId = await response.json();
			if (disposed) return;

			await goto(
				`/game?sessionId=${encodeURIComponent(String(sessionId))}&adventureId=${encodeURIComponent(adventureId)}`
			);
		} catch (error) {
			if (disposed) return;

			console.error('Játékindítási hiba:', error);
			startRequested = false;
			startError = 'A játék indítása sikertelen. Próbáld újra.';
		} finally {
			if (!disposed) isStarting = false;
		}
	}

	onMount(() => {
		async function initialize() {
			if (!navigator.geolocation) {
				locationError = 'A böngésző nem támogatja a helymeghatározást.';
				return;
			}

			try {
				const leaflet = await import('leaflet');
				L = leaflet.default ?? leaflet;

				await import('leaflet-routing-machine');
				if (disposed) return;

				watchId = navigator.geolocation.watchPosition(
					(position) => {
						if (disposed) return;

						locationError = '';
						accuracy = position.coords.accuracy;
						lastFixAt = position.timestamp;

						updateLocation(
							position.coords.latitude,
							position.coords.longitude
						);
					},
					(error) => {
						if (disposed) return;

						locationError =
							error.code === 1
								? 'Engedélyezd a helyhozzáférést a webhelybeállításokban, majd töltsd újra az oldalt.'
								: error.code === 2
									? 'Nem sikerült meghatározni a helyzeted. Ellenőrizd a telefon helymeghatározását.'
									: 'A helymeghatározás túl sokáig tartott. Új helyzetre várunk…';
					},
					{
						enableHighAccuracy: true,
						timeout: 20000,
						maximumAge: 0
					}
				);
			} catch (error) {
				if (disposed) return;

				console.error('Térképbetöltési hiba:', error);
				dataError = 'A térkép betöltése sikertelen.';
			}
		}

		void initialize();
	});

	onDestroy(() => {
		disposed = true;
		controller.abort();

		if (
			typeof navigator !== 'undefined' &&
			navigator.geolocation &&
			watchId !== undefined
		) {
			navigator.geolocation.clearWatch(watchId);
		}

		map?.remove();
	});
</script>

<main class="flex flex-col h-[calc(100vh-128px)] bg-[#F5F2EA] font-josefin overflow-hidden relative">
	<header class="h-14 bg-[#775D4D] text-[#F5F2EA] flex items-center px-4 rounded-xl mx-2 shadow-lg z-10 shrink-0">
		<div class="flex-1 font-mono text-xs uppercase font-black text-yellow-400 animate-pulse">
			Navigáció...
		</div>
		<div class="flex-[2] text-center font-bold truncate px-2 text-sm italic">{adventureTitle}</div>
		<div class="flex-1 text-right text-xs">Menj az első állomáshoz!</div>
	</header>

	<section class="flex-grow m-2 mb-6 rounded-3xl overflow-hidden shadow-2xl border-4 border-[#775D4D] relative bg-gray-200">
		<div id="map-container" class="w-full h-full z-0"></div>
		<button onclick={() => goto('/dashboard')} class="absolute top-4 left-4 z-[400] bg-[#775D4D] p-2 px-4 rounded-xl text-white text-[10px] font-black uppercase shadow-lg">
			Kilépés
		</button>
		{#if locationError || dataError || startError || !userPos || !firstStation || isStarting}
			<div
				class="absolute bottom-6 left-4 right-4 z-[450] rounded-2xl bg-[#F5F2EA]/95 p-4 text-center shadow-lg"
				role="status"
				aria-live="polite"
			>
				<p class="text-sm font-bold text-[#2F5D50]">
					{locationError || dataError || startError || (
						!userPos
							? 'Helyzeted meghatározása…'
							: !auth.token
								? 'Bejelentkezésre várunk…'
								: isStarting
									? 'Játék indítása…'
									: 'Első állomás betöltése…'
					)}
				</p>

				{#if startError && !locationError && !dataError}
					<button
						onclick={() => {
          startError = '';
          checkArrival();
        }}
						class="mt-3 rounded-xl bg-[#2F5D50] px-5 py-2 font-bold text-white"
					>
						Indítás újrapróbálása
					</button>
				{/if}

				{#if locationError || dataError}
					<button
						onclick={() => window.location.reload()}
						class="mt-3 rounded-xl bg-[#2F5D50] px-5 py-2 font-bold text-white"
					>
						Oldal újratöltése
					</button>
				{/if}
			</div>
		{/if}
	</section>
</main>

<style>
    :global(body) { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; position: fixed; }

    #map-container { height: 100%; width: 100%; }

    :global(.leaflet-control-container) { display: none !important; }

    :global(.custom-user-marker) { filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }

    :global(.custom-station-marker) { filter: drop-shadow(0 0 8px rgba(34, 197, 94, 0.6)); }
</style>