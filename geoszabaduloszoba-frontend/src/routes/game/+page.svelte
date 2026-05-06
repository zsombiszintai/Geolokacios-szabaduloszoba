<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import L from 'leaflet';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';
	import { QuestionCircleOutline, ArrowUpOutline, CloseOutline } from 'flowbite-svelte-icons';
	import 'leaflet-routing-machine';
	import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';

	const adventureId = $derived(page.url.searchParams.get('id'));
	const savedStationId = $derived(page.url.searchParams.get('station'));
	const initialElapsed = Number(page.url.searchParams.get('elapsed')) || 0;
	const initialDistance = Number(page.url.searchParams.get('distance')) || 0;

	let sessionId = $state<number | null>(null);
	let adventureTitle = $state("Betöltés...");
	let elapsedSec = $state(initialElapsed);
	let distanceInMeters = $state(initialDistance);
	let userPos = $state({ lat: 46.076504717136054, lon: 18.23013854980469 });

	let allStations = $state<any[]>([]);
	let lastStationId = $state<number | null>(null);
	let routePath = $state<[number, number][]>([]);

	let isApproachingFirstStation = $state(true);
	let isGameStarted = $state(false);
	let isCompassOpen = $state(false);
	let isRiddleOpen = $state(false);
	let compassRotation = $state(0);
	let currentRiddleText = $state("");

	let map: L.Map;
	let playerMarker: L.Marker;
	let targetMarker: L.Marker | null = $state(null);
	let polyline: L.Polyline;
	let guideLine: L.Polyline;
	let routingControl: any = null;
	let timerInterval: any;
	let syncInterval: any;


	function calculateDistance(p1: {lat: number, lon: number}, p2: {lat: number, lon: number}) {
		return L.latLng(p1.lat, p1.lon).distanceTo(L.latLng(p2.lat, p2.lon));
	}

	function calculateAngle(p1: {lat: number, lon: number}, p2: {lat: number, lon: number}) {
		const dLon = (p2.lon - p1.lon) * Math.PI / 180;
		const y = Math.sin(dLon) * Math.cos(p2.lat * Math.PI / 180);
		const x = Math.cos(p1.lat * Math.PI / 180) * Math.sin(p2.lat * Math.PI / 180) -
			Math.sin(p1.lat * Math.PI / 180) * Math.cos(p2.lat * Math.PI / 180) * Math.cos(dLon);
		return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
	}

	const currentTarget = $derived(() => {
		if (allStations.length === 0 || lastStationId === null) return null;
		return allStations.find(s => s.id === lastStationId) || allStations[0];
	});

	async function setupPathfinder(start: {lat: number, lon: number}, end: {lat: number, lon: number}) {
		if (!map) return;

		if (routingControl) map.removeControl(routingControl);

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
		const newPos = { lat: newLat, lon: newLon };
		const target = currentTarget();

		if (isApproachingFirstStation && target) {
			userPos = newPos;
			const distToTarget = calculateDistance(userPos, { lat: target.latitude, lon: target.longitude });

			if (routingControl && routingControl.getPlan()) {
				routingControl.setWaypoints([L.latLng(userPos.lat, userPos.lon), L.latLng(target.latitude, target.longitude)]);
			}

			if (distToTarget < 15) {
				isApproachingFirstStation = false;
				isGameStarted = true;

				if (routingControl) {
					map.removeControl(routingControl);
					routingControl = null;
				}
				if (targetMarker) {
					map.removeLayer(targetMarker);
					targetMarker = null;
				}

				startTracking();

				currentRiddleText = target.riddleText || "Keresd a pontot!";
				isRiddleOpen = true;
			}
		} else if (isGameStarted) {
			distanceInMeters += calculateDistance(userPos, newPos);
			userPos = newPos;
			routePath = [...routePath, [newLat, newLon]];

			if (target) {
				compassRotation = calculateAngle(userPos, { lat: target.latitude, lon: target.longitude });

				const distToTarget = calculateDistance(userPos, { lat: target.latitude, lon: target.longitude });
				if (distToTarget < 20 && !isRiddleOpen) {
					currentRiddleText = target.riddleText || "Megérkeztél!";
					isRiddleOpen = true;
				}
			}
		}

		if (playerMarker) {
			playerMarker.setLatLng([newLat, newLon]);
			if (isGameStarted) polyline.setLatLngs(routePath);
			map.panTo([newLat, newLon]);
		}
	}

	function startTracking() {
		if (timerInterval) clearInterval(timerInterval);
		timerInterval = setInterval(() => { elapsedSec++; }, 1000);

		if (syncInterval) clearInterval(syncInterval);
		syncInterval = setInterval(syncGameProgress, 5000);
	}

	async function syncGameProgress() {
		if (!sessionId || !lastStationId || !auth.token) return;
		try {
			await fetch('http://localhost:8080/api/game/update', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${auth.token}` },
				body: JSON.stringify({
					sessionId, lastStationId, elapsedSec, distanceInMeters,
					currentLat: userPos.lat, currentLon: userPos.lon
				})
			});
		} catch (e) { console.warn("Auto-sync hiba", e); }
	}

	async function saveAndExit() {
		await syncGameProgress();
		goto('/dashboard');
	}

	/* onMount(async () => {
		map = L.map('map-container', { zoomControl: false }).setView([userPos.lat, userPos.lon], 16);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		const userIcon = L.divIcon({
			className: 'custom-user-marker',
			html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
                <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg"></div>
             </div>`,
			iconSize: [32, 32], iconAnchor: [16, 16]
		});

		playerMarker = L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map);
		polyline = L.polyline([], { color: 'black', weight: 4, opacity: 0.7 }).addTo(map);
		guideLine = L.polyline([], { color: '#ef4444', weight: 3, dashArray: '10, 10', opacity: 0.6 }).addTo(map);

		if (!adventureId || !auth.token) return;

		try {
			const advRes = await fetch(`http://localhost:8080/api/adventures/${adventureId}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (advRes.ok) {
				const data = await advRes.json();
				adventureTitle = data.title;
				allStations = data.stations || [];

				lastStationId = savedStationId ? parseInt(savedStationId) : (allStations[0]?.id || null);

				const target = currentTarget();
				if (target) {
					setupPathfinder(userPos, { lat: target.latitude, lon: target.longitude });
				}
			}

			const startRes = await fetch(`http://localhost:8080/api/game/start/${adventureId}`, {
				method: 'POST',
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (startRes.ok) sessionId = await startRes.json();

		} catch (err) { console.error("Init hiba:", err); }

		if (navigator.geolocation) {
			navigator.geolocation.watchPosition(
				(p) => updateLocation(p.coords.latitude, p.coords.longitude),
				(e) => console.error(e),
				{ enableHighAccuracy: true }
			);
		}
	});
*/

	onMount(async () => {
		map = L.map('map-container', { zoomControl: false }).setView([userPos.lat, userPos.lon], 16);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		const userIcon = L.divIcon({
			className: 'custom-user-marker',
			html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
                <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg"></div>
             </div>`,
			iconSize: [32, 32], iconAnchor: [16, 16]
		});

		playerMarker = L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map);
		polyline = L.polyline([], { color: 'black', weight: 4, opacity: 0.7 }).addTo(map);
		guideLine = L.polyline([], { color: '#ef4444', weight: 3, dashArray: '10, 10', opacity: 0.6 }).addTo(map);

		try {
			const res = await fetch(`http://localhost:8080/api/adventures/${adventureId}?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				const data = await res.json();
				adventureTitle = data.title;
				allStations = data.stations || [];
			} else {
				throw new Error("400/500 hiba");
			}
		} catch (err) {
		}

		lastStationId = savedStationId ? parseInt(savedStationId) : (allStations[0]?.id || null);

		const target = currentTarget();
		if (target && target.seqNumber === 1) {
			isApproachingFirstStation = true;
			isGameStarted = false;
			setupPathfinder(userPos, { lat: target.latitude, lon: target.longitude });
		} else {
			isApproachingFirstStation = false;
			isGameStarted = true;
			startTracking();

			if (target) {
				currentRiddleText = target.riddleText || "";
				isRiddleOpen = true;
			}
		}

		setTimeout(() => {
			updateLocation(userPos.lat, userPos.lon);
		}, 1000);
	});

	onDestroy(() => {
		if (map) map.remove();
		clearInterval(timerInterval);
		clearInterval(syncInterval);
	});
</script>

<main class="flex flex-col h-[calc(100vh-128px)] bg-[#F5F2EA] font-josefin overflow-hidden relative">
	<header class="h-14 bg-[#775D4D] text-[#F5F2EA] flex items-center px-4 rounded-xl mx-2 shadow-lg z-10 shrink-0">
		<div class="flex-1 font-mono text-sm tracking-tighter">
			{#if isApproachingFirstStation}
				<span class="text-yellow-400 animate-pulse text-[10px] uppercase font-black">Navigáció...</span>
			{:else}
				{new Date(elapsedSec * 1000).toISOString().substr(11, 8)}
			{/if}
		</div>
		<div class="flex-[2] text-center font-bold truncate px-2 text-sm italic">{adventureTitle}</div>
		<div class="flex-1 text-right text-sm font-bold">{Math.round(distanceInMeters)} m</div>
	</header>

	<section class="flex-grow m-2 mb-6 rounded-3xl overflow-hidden shadow-2xl border-4 border-[#775D4D] relative bg-gray-200">
		<div id="map-container" class="w-full h-full z-0"></div>

		<button onclick={saveAndExit} class="absolute top-4 left-4 z-[400] bg-[#775D4D] p-2 px-4 rounded-xl text-white text-[10px] font-black uppercase shadow-lg active:scale-95 transition-transform">
			Kilépés
		</button>

		{#if isGameStarted}
			<button onclick={() => isRiddleOpen = !isRiddleOpen} class="absolute bottom-6 left-6 z-[450] bg-[#775D4D] w-14 h-14 rounded-full shadow-2xl flex items-center justify-center border-2 border-white/20 active:scale-90 transition-transform">
				<QuestionCircleOutline class="w-8 h-8 text-white" />
			</button>

			<button onclick={() => isCompassOpen = !isCompassOpen} class="absolute bottom-6 right-6 z-[450] bg-[#775D4D] w-14 h-14 rounded-full shadow-2xl flex items-center justify-center border-2 border-white/20 active:scale-90 transition-transform">
				<div style="transform: rotate({compassRotation}deg)" class="transition-transform duration-300">
					<ArrowUpOutline class="w-8 h-8 text-white" />
				</div>
			</button>
		{/if}

		{#if isRiddleOpen}
			<div class="absolute inset-0 z-[500] bg-[#775D4D]/90 backdrop-blur-sm flex flex-col items-center justify-center p-6 text-center">
				<div class="bg-[#F5F2EA] p-8 rounded-3xl shadow-2xl border-t-8 border-[#775D4D] max-w-xs transform scale-110">
					<h3 class="text-[#775D4D] font-black uppercase tracking-[0.2em] mb-4 text-xs">Aktuális Rejtvény</h3>
					<p class="text-gray-800 italic font-medium leading-relaxed">{currentTarget()?.riddleText || "Keresd a pontot!"}</p>
					<button onclick={() => isRiddleOpen = false} class="mt-6 bg-[#775D4D] text-white px-6 py-2 rounded-full font-bold text-xs uppercase">x</button>
				</div>
			</div>
		{/if}

		{#if isCompassOpen}
			<div class="absolute inset-0 z-[500] bg-[#775D4D]/95 flex flex-col items-center justify-center" onclick={() => isCompassOpen = false}>
				<div class="relative w-72 h-72 border-8 border-white/10 rounded-full flex items-center justify-center bg-white/5 shadow-[0_0_50px_rgba(255,255,255,0.1)]">
					<div style="transform: rotate({compassRotation}deg)" class="transition-transform duration-200">
						<ArrowUpOutline class="w-40 h-40 text-white drop-shadow-[0_0_15px_rgba(255,255,255,0.5)]" />
					</div>
				</div>
				<p class="text-white/60 mt-10 font-black tracking-widest uppercase text-[10px]">Következő állomás</p>
			</div>
		{/if}
	</section>
</main>

<style>
    :global(body) { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; position: fixed; }
    #map-container { height: 100%; width: 100%; }
    :global(.leaflet-control-container) { display: none !important; }
    :global(.custom-user-marker) { filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }
    :global(.custom-station-marker) {
        filter: drop-shadow(0 0 8px rgba(34, 197, 94, 0.6));
    }
</style>