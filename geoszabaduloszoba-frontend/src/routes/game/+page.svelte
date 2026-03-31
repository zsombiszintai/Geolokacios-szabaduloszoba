<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import L from 'leaflet';
	import 'leaflet/dist/leaflet.css';

	const adventureId = $derived(page.url.searchParams.get('id'));

	let sessionId = $state<number | null>(null);
	let elapsedSec = $state(0);
	let distanceInMeters = $state(0);
	let lastStationId = $state<number | null>(null);
	let userPos = $state({ lat: 46.0754, lon: 18.2205 });

	let map: L.Map;
	let marker: L.Marker;
	let timerInterval: any;
	let syncInterval: any;

	onMount(async () => {
		map = L.map('map-container').setView([userPos.lat, userPos.lon], 15);

		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '© OpenStreetMap contributors'
		}).addTo(map);

		marker = L.marker([userPos.lat, userPos.lon]).addTo(map)
			.bindPopup('Te itt vagy!')
			.openPopup();

		if (!adventureId || !auth.token) return;

		const res = await fetch(`http://localhost:8080/api/game/start/${adventureId}`, {
			method: 'POST',
			headers: { 'Authorization': `Bearer ${auth.token}` }
		});

		if (res.ok) {
			sessionId = await res.json();
			startTracking();
		}
	});

	function updateLocation(lat: number, lon: number) {
		userPos = { lat, lon };
		if (marker && map) {
			marker.setLatLng([lat, lon]);
			map.panTo([lat, lon]);
		}
	}

	function startTracking() {
		timerInterval = setInterval(() => { elapsedSec++; }, 1000);

		syncInterval = setInterval(async () => {
			if (sessionId === null || lastStationId === null) return;

			const dto = {
				sessionId,
				lastStationId,
				elapsedSec,
				distanceInMeters,
				currentLat: userPos.lat,
				currentLon: userPos.lon
			};

			await fetch('http://localhost:8080/api/game/update', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify(dto)
			});
		}, 5000);
	}

	onDestroy(() => {
		if (map) map.remove();
		clearInterval(timerInterval);
		clearInterval(syncInterval);
	});

	function formatTime(sec: number) {
		return new Date(sec * 1000).toISOString().substr(11, 8);
	}
</script>

<main class="flex flex-col h-screen bg-[#F5F2EA] font-josefin">
	<header class="bg-[#775D4D] text-[#F5F2EA] p-4 mx-4 mt-4 rounded-xl shadow-lg flex justify-between items-center z-10">
		<span class="text-lg font-mono">{formatTime(elapsedSec)}</span>
		<span class="font-bold text-lg">${adventure.title}</span>
		<span class="text-lg">{Math.round(distanceInMeters)} m</span>
	</header>

	<div class="flex-grow m-4 rounded-2xl overflow-hidden shadow-inner z-0 border-2 border-[#775D4D]">
		<div id="map-container" class="w-full h-full"></div>
	</div>

	<div class="px-4 pb-4 flex gap-2">
		<button onclick={() => updateLocation(userPos.lat + 0.001, userPos.lon + 0.001)}
						class="bg-white text-xs p-1 rounded border">Pozíció teszt</button>
	</div>

	<nav class="bg-[#2F5D50] p-4 flex justify-around">
	</nav>
</main>

<style>
    #map-container {
        background: #e5e7eb;
    }
</style>