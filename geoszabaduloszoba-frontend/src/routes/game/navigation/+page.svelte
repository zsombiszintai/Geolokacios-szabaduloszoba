<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import L from 'leaflet';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';
	import 'leaflet-routing-machine';
	import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';

	const adventureId = $derived(page.url.searchParams.get('id'));

	let adventureTitle = $state("Pozíció meghatározása...");
	let userPos = $state<{ lat: number; lon: number } | null>(null);
	let firstStation = $state<any>(null);

	let map: L.Map;
	let playerMarker: L.Marker;
	let targetMarker: L.Marker | null = $state(null);
	let routingControl: any = null;
	let watchId: number;

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

	async function updateLocation(newLat: number, newLon: number) {
		const isFirstFix = userPos === null;
		userPos = { lat: newLat, lon: newLon };

		if (isFirstFix) {
			initMap(newLat, newLon);
			await fetchAdventureDetails(newLat, newLon);
		} else {
			if (playerMarker) {
				playerMarker.setLatLng([newLat, newLon]);
				map.panTo([newLat, newLon]);
			}

			if (firstStation) {
				const distToTarget = calculateDistance(userPos, { lat: firstStation.latitude, lon: firstStation.longitude });

				if (routingControl) {
					routingControl.setWaypoints([L.latLng(newLat, newLon), L.latLng(firstStation.latitude, firstStation.longitude)]);
				}

				if (distToTarget < 15) {
					navigator.geolocation.clearWatch(watchId);
					await triggerGameStart();
				}
			}
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
			const res = await fetch(`http://localhost:8080/api/adventures/${adventureId}?lat=${lat}&lon=${lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				const data = await res.json();
				adventureTitle = data.title;
				if (data.stations && data.stations.length > 0) {
					firstStation = data.stations.sort((a: any, b: any) => a.seqNumber - b.seqNumber)[0];
					setupPathfinder({ lat, lon }, { lat: firstStation.latitude, lon: firstStation.longitude });
				}
			}
		} catch (err) {
			console.error("Adatbetöltési hiba:", err);
			adventureTitle = "Hiba az adatok betöltésekor";
		}
	}

	async function triggerGameStart() {
		try {
			const startRes = await fetch(`http://localhost:8080/api/game/start/${adventureId}`, {
				method: 'POST',
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (startRes.ok) {
				const sessionId = await startRes.json();
				goto(`/game?sessionId=${sessionId}&adventureId=${adventureId}`);
			}
		} catch (err) {
			console.error("Hiba a játék indításakor:", err);
		}
	}

	onMount(() => {
		if (navigator.geolocation) {
			watchId = navigator.geolocation.watchPosition(
				(p) => updateLocation(p.coords.latitude, p.coords.longitude),
				(e) => {
					console.error("GPS hiba, fallback a Sensors koordinátákra:", e);
					if (userPos === null) {
						updateLocation(46.073504717136054, 18.22113854980469);
					}
				},
				{ enableHighAccuracy: true }
			);
		} else {
			adventureTitle = "A böngésző nem támogatja a GPS-t";
		}
	});

	onDestroy(() => {
		if (map) map.remove();
		if (navigator.geolocation) navigator.geolocation.clearWatch(watchId);
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
		<div class="absolute bottom-6 right-6 z-[400] flex flex-col items-end gap-1">
     <span class="text-[9px] font-black text-[#2F5D50] bg-white/80 px-2 py-0.5 rounded-full uppercase tracking-wider shadow-sm">
       PC Teszt Mód
     </span>
			<button
				onclick={triggerGameStart}
				class="bg-[#2F5D50] hover:bg-[#244a3f] text-white px-6 py-4 rounded-2xl font-black text-xs uppercase tracking-widest shadow-2xl border border-white/20 active:scale-95 transition-all flex items-center gap-2"
			>
				Játék kényszerített indítása
			</button>
		</div>
	</section>
</main>

<style>
    :global(body) { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; position: fixed; }

    #map-container { height: 100%; width: 100%; }

    :global(.leaflet-control-container) { display: none !important; }

    :global(.custom-user-marker) { filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }

    :global(.custom-station-marker) { filter: drop-shadow(0 0 8px rgba(34, 197, 94, 0.6)); }
</style>