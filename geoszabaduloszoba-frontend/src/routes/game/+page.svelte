<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import L from 'leaflet';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';

	const adventureId = $derived(page.url.searchParams.get('id'));

	let sessionId = $state<number | null>(null);
	let adventureTitle = $state("Betöltés...");
	let elapsedSec = $state(0);
	let distanceInMeters = $state(0);
	let userPos = $state({ lat: 46.0754, lon: 18.2205 });
	let firstStationPos = $state<{lat: number, lon: number} | null>(null);
	let routePath = $state<[number, number][]>([]);
	let compassRotation = $state(0);
	let lastStationId = $state<number | null>(null);
	let isCompassOpen = $state(false);
	let isInitializing = false;

	let map: L.Map;
	let playerMarker: L.Marker;
	let polyline: L.Polyline;
	let timerInterval: any;
	let syncInterval: any;

	function toggleCompass() {
		isCompassOpen = !isCompassOpen;
	}

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

	onMount(async () => {
		if (isInitializing) return;
		isInitializing = true;

		map = L.map('map-container', { zoomControl: false }).setView([userPos.lat, userPos.lon], 16);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		routePath = [[userPos.lat, userPos.lon]];
		polyline = L.polyline(routePath, { color: 'black', weight: 4, opacity: 0.7 }).addTo(map);

		const userIcon = L.divIcon({
			className: 'custom-user-marker',
			html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
                <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg"></div>
             </div>`,
			iconSize: [32, 32],
			iconAnchor: [16, 16]
		});

		playerMarker = L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map);

		if (!adventureId || !auth.token) return;

		try {
			const advRes = await fetch(`http://localhost:8080/api/adventures/${adventureId}?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (advRes.ok) {
				const data = await advRes.json();
				adventureTitle = data.title;
				if (data.stations && data.stations.length > 0) {
					lastStationId = data.stations[0].id;
					firstStationPos = { lat: data.stations[0].latitude, lon: data.stations[0].longitude };
					compassRotation = calculateAngle(userPos, firstStationPos);
				}
			}

			const startRes = await fetch(`http://localhost:8080/api/game/start/${adventureId}`, {
				method: 'POST',
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (startRes.ok) {
				sessionId = await startRes.json();
				startTracking();
			}
		} catch (err) {
			console.error("Hiba az inicializálásnál:", err);
			isInitializing = false;
		}
	});

	function updateLocation(newLat: number, newLon: number) {
		const newPos = { lat: newLat, lon: newLon };
		distanceInMeters += calculateDistance(userPos, newPos);

		userPos = newPos;
		routePath = [...routePath, [newLat, newLon]];

		if (firstStationPos) {
			compassRotation = calculateAngle(userPos, firstStationPos);
		}

		if (playerMarker && map) {
			playerMarker.setLatLng([newLat, newLon]);
			polyline.setLatLngs(routePath);
			map.panTo([newLat, newLon]);
		}
	}

	async function saveAndExit() {

		console.log("Kilépés megkezdése...", { sessionId, lastStationId });
		if (sessionId === null) {
			goto('/dashboard');
			return;
		}

		const data = {
			sessionId,
			lastStationId: lastStationId || 1,
			elapsedSec,
			distanceInMeters,
			currentLat: userPos.lat,
			currentLon: userPos.lon
		};

		const payloadString = JSON.stringify(data);

		try {
			fetch('http://localhost:8080/api/game/update', {
				method: 'POST',
				keepalive: true,
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: payloadString
			});

			console.log("Mentési kérés elküldve, navigálás...");
		} catch (err) {
			console.error("Hiba a mentésnél:", err);
		} finally {
			goto('/dashboard');
		}
	}

	function startTracking() {
		if (timerInterval) clearInterval(timerInterval);
		timerInterval = setInterval(() => { elapsedSec++; }, 1000);

		if (syncInterval) clearInterval(syncInterval);
		syncInterval = setInterval(async () => {
			if (sessionId === null || lastStationId === null || !auth.token) return;

			try {
				await fetch('http://localhost:8080/api/game/update', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
						'Authorization': `Bearer ${auth.token}`
					},
					body: JSON.stringify({
						sessionId, lastStationId, elapsedSec, distanceInMeters,
						currentLat: userPos.lat, currentLon: userPos.lon
					})
				});
			} catch (e) {
				console.warn("Auto-sync hiba:", e);
			}
		}, 5000);
	}

	onDestroy(() => {
		if (map) map.remove();
		clearInterval(timerInterval);
		clearInterval(syncInterval);
	});
</script>

<main class="flex flex-col h-screen bg-[#F5F2EA] font-josefin overflow-hidden">
	<header class="h-14 bg-[#775D4D] text-[#F5F2EA] flex items-center px-4 rounded-xl mx-2 mt-2 shadow-lg z-10">
		<div class="flex-1 font-mono text-sm tracking-tighter">
			{new Date(elapsedSec * 1000).toISOString().substr(11, 8)}
		</div>
		<div class="flex-[2] text-center font-bold truncate px-2 text-sm">{adventureTitle}</div>
		<div class="flex-1 text-right text-sm font-bold">{Math.round(distanceInMeters)} m</div>
	</header>

	<section class="flex-grow m-2 rounded-2xl overflow-hidden shadow-inner border-2 border-[#775D4D] relative">
		<div id="map-container" class="w-full h-full z-0"></div>

		<button onclick={saveAndExit} class="absolute top-4 left-4 z-[400] bg-[#775D4D]/90 p-2 px-4 rounded-lg text-white shadow-md active:scale-95 transition-transform">
			🚪 Kilépés
		</button>

		{#if isCompassOpen}
			<div
				class="absolute inset-0 z-[500] bg-black/40 backdrop-blur-sm flex flex-col items-center justify-center animate-in fade-in duration-300"
				onclick={toggleCompass}
				role="button"
				tabindex="0"
				onkeydown={(e) => e.key === 'Enter' && toggleCompass()}
			>
				<div class="text-[#F5F2EA] text-center pointer-events-none">
					<p class="mb-8 text-lg font-bold tracking-widest uppercase drop-shadow-md">Következő állomás</p>

					<div
						class="relative w-56 h-56 border-4 border-[#F5F2EA] rounded-full flex items-center justify-center shadow-2xl bg-[#775D4D]/20 transition-transform duration-700 ease-out"
						style="transform: rotate({compassRotation}deg)"
					>
						<span class="absolute top-2 font-black text-red-500 text-xl">N</span>
						<div class="w-1.5 h-36 bg-gradient-to-b from-red-500 via-[#F5F2EA] to-[#775D4D] rounded-full shadow-lg"></div>
						<div class="absolute w-4 h-4 bg-[#775D4D] rounded-full border-2 border-[#F5F2EA] shadow-sm"></div>
					</div>

					<p class="mt-8 opacity-70 italic text-xs">Kattints a bezáráshoz</p>
				</div>
			</div>
		{/if}

		{#if !isCompassOpen}
			<button
				onclick={toggleCompass}
				class="absolute bottom-6 right-6 z-[400] bg-[#775D4D] p-4 rounded-full shadow-2xl transition-all border-2 border-white/20 active:scale-90 hover:brightness-110"
				style="transform: rotate({compassRotation}deg)"
			>
				<span class="text-white text-2xl block">🧭</span>
			</button>
		{/if}
	</section>

	<footer class="p-2 flex justify-center bg-transparent">
		<button onclick={() => updateLocation(userPos.lat + 0.0001, userPos.lon + 0.0001)}
						class="bg-[#775D4D]/10 text-[#775D4D] text-[10px] font-bold px-3 py-1 rounded-full border border-[#775D4D]/20 hover:bg-[#775D4D]/20 transition-colors">
			POZÍCIÓ SZIMULÁLÁSA
		</button>
	</footer>
</main>

<style>
    #map-container {
        height: 100%;
        width: 100%;
        background: #e5e7eb;
    }

    :global(.leaflet-control-container) {
        display: none !important;
    }

    .relative, button {
        transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
</style>