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
	let userPos = $state({ lat: 46.079721, lon: 18.227176 });
	let firstStationPos = $state<{lat: number, lon: number} | null>(null);
	let routePath = $state<[number, number][]>([]);
	let compassRotation = $state(0);
	let lastStationId = $state<number | null>(null);
	let isCompassOpen = $state(false);
	let isInitializing = false;
	let allStations = $state<any[]>([]);
	let guideLine: L.Polyline;
	let showRiddle = $state(false);
	let currentRiddleText = $state("");

	let map: L.Map;
	let playerMarker: L.Marker;
	let polyline: L.Polyline;
	let timerInterval: any;
	let syncInterval: any;

	const savedStationId = $derived(page.url.searchParams.get('station'));

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

	function updateCompass() {
		const target = currentTarget();
		if (target) {
			compassRotation = calculateAngle(userPos, { lat: target.latitude, lon: target.longitude });
		}
	}

	function nextStation() {
		showRiddle = false;
		const currentIndex = allStations.findIndex(s => s.id === lastStationId);

		if (currentIndex !== -1 && currentIndex < allStations.length - 1) {
			const next = allStations[currentIndex + 1];
			lastStationId = next.id;

			guideLine.setLatLngs([[userPos.lat, userPos.lon], [next.latitude, next.longitude]]);
			updateCompass();
		} else {
			guideLine.setLatLngs([]);
			alert("Gratulálunk! Teljesítetted a kalandot!");
			saveAndExit();
		}
	}
	const currentTarget = $derived(() => {
		if (allStations.length === 0 || lastStationId === null) return null;
		const currentIndex = allStations.findIndex(s => s.id === lastStationId);
		return allStations[currentIndex];
	});

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

		guideLine = L.polyline([], {
			color: '#ef4444',
			weight: 3,
			dashArray: '10, 10',
			opacity: 0.6
		}).addTo(map);

		if (!adventureId || !auth.token) return;

		try {
			const advRes = await fetch(`http://localhost:8080/api/adventures/${adventureId}?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (advRes.ok) {
				const data = await advRes.json();
				adventureTitle = data.title;
				allStations = data.stations || [];

				if (allStations.length > 0) {
					let targetStation = allStations[0];

					if (savedStationId) {
						const found = allStations.find(s => s.id === parseInt(savedStationId));
						if (found) {
							targetStation = found;
							console.log("Játék folytatása a mentett állomástól:", targetStation.id);
						}
					}

					lastStationId = targetStation.id;

					guideLine.setLatLngs([
						[userPos.lat, userPos.lon],
						[targetStation.latitude, targetStation.longitude]
					]);

					updateCompass();
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

		const target = currentTarget();
		if (target) {
			compassRotation = calculateAngle(userPos, { lat: target.latitude, lon: target.longitude });

			guideLine.setLatLngs([
				[userPos.lat, userPos.lon],
				[target.latitude, target.longitude]
			]);

			const distToTarget = calculateDistance(userPos, { lat: target.latitude, lon: target.longitude });

			if (distToTarget < 20 && !showRiddle) {
				currentRiddleText = target.riddleText || "Nincs megadva rejtvény szöveg, de megérkeztél!";
				showRiddle = true;
			}
		}

		if (playerMarker && map) {
			playerMarker.setLatLng([newLat, newLon]);
			polyline.setLatLngs(routePath);
			map.panTo([newLat, newLon]);
		}
	}

	async function saveAndExit() {
		if (sessionId === null) {
			goto('/completed-adventures');
			return;
		}

		const data = {
			sessionId,
			lastStationId,
			elapsedSec,
			distanceInMeters,
			currentLat: userPos.lat,
			currentLon: userPos.lon
		};

		try {
			await fetch('http://localhost:8080/api/game/update', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify(data)
			});
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

			if (!sessionId || !lastStationId || !auth.token) return;

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

<main class="flex flex-col h-[calc(100vh-64px)] bg-[#F5F2EA] font-josefin overflow-hidden">
	<header class="h-14 bg-[#775D4D] text-[#F5F2EA] flex items-center px-4 rounded-xl mx-2 mt-2 shadow-lg z-10 shrink-0">
		<div class="flex-1 font-mono text-sm tracking-tighter">
			{new Date(elapsedSec * 1000).toISOString().substr(11, 8)}
		</div>
		<div class="flex-[2] text-center font-bold truncate px-2 text-sm">{adventureTitle}</div>
		<div class="flex-1 text-right text-sm font-bold">{Math.round(distanceInMeters)} m</div>
	</header>

	<section class="flex-grow m-2 mb-1 rounded-2xl overflow-hidden shadow-inner border-2 border-[#775D4D] relative bg-gray-200">
		<div id="map-container" class="w-full h-full z-0"></div>

		<button onclick={saveAndExit} class="absolute top-4 left-4 z-[400] bg-[#775D4D]/90 p-2 px-4 rounded-lg text-white text-xs font-bold shadow-md">
			Kilépés
		</button>

		{#if !isCompassOpen}
			<button
				onclick={toggleCompass}
				class="absolute bottom-4 right-4 z-[450] bg-[#775D4D] w-14 h-14 rounded-full shadow-2xl border-2 border-white/20 flex items-center justify-center transition-transform active:scale-90"
				style="transform: rotate({compassRotation}deg)"
			>
				<span class="text-2xl">🧭</span>
			</button>
		{/if}

		{#if isCompassOpen}
			<div class="absolute inset-0 z-[500] bg-black/40 backdrop-blur-sm flex items-center justify-center" onclick={toggleCompass}>
				<div class="text-white font-bold">Iránytű nyitva</div>
			</div>
		{/if}
	</section>

	<div class="h-5 flex justify-center items-center shrink-0 mb-1">
		<button onclick={() => updateLocation(userPos.lat + 0.0001, userPos.lon + 0.0001)} class="text-[#775D4D] text-[8px] font-black opacity-20">
			[ DEBUG ]
		</button>
	</div>
</main>

<style>
    #map-container {
        height: 100%;
        width: 100%;
        background: #e5e7eb;
        display: block;
        box-sizing: border-box;
    }

    :global(.leaflet-control-container) {
        display: none !important;
    }

    :global(body) {
        overflow: hidden;
        position: fixed;
        width: 100%;
        height: 100%;
    }
</style>