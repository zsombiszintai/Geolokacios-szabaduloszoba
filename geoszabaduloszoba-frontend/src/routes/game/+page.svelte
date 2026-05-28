<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import L from 'leaflet';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';
	import { QuestionCircleOutline, ArrowUpOutline, CheckCircleSolid, LightbulbOutline, ChevronLeftOutline, ChevronRightOutline} from 'flowbite-svelte-icons';

	const sessionId = $derived(Number(page.url.searchParams.get('sessionId')));
	const adventureId = $derived(page.url.searchParams.get('adventureId'));

	let adventureTitle = $state("Pozíció meghatározása...");
	let elapsedSec = $state(0);
	let distanceInMeters = $state(0);

	let accumulatedPoints = $state(0);

	let userPos = $state<{ lat: number; lon: number } | null>(null);
	let allStations = $state<any[]>([]);
	let lastStationId = $state<number | null>(null);

	let isRiddleOpen = $state(false);
	let isHintModalOpen = $state(false);
	let isExplanationOpen = $state(false);

	let activeHintsCount = $state(0);
	let currentHintViewIndex = $state(0);
	let compassUsedAtCurrentStation = $state(false);

	let compassRotation = $state(34);
	let map: L.Map;
	let playerMarker: L.Marker;
	let watchId: number;
	let timerInterval: any;

	function calculateDistance(p1: {lat: number, lon: number}, p2: {lat: number, lon: number}) {
		return L.latLng(p1.lat, p1.lon).distanceTo(L.latLng(p2.lat, p2.lon));
	}

	const currentTarget = $derived(() => {
		if (allStations.length === 0 || lastStationId === null) return null;
		return allStations.find(s => s.id === lastStationId);
	});

	const currentStationPoints = $derived(() => {
		if (compassUsedAtCurrentStation) return 0;
		return Math.max(1, 4 - activeHintsCount);
	});

	const totalHintTabs = $derived(() => {
		const hintsLength = currentTarget()?.content?.hints?.length || 0;
		return compassUsedAtCurrentStation ? hintsLength + 1 : hintsLength;
	});

	const totalScore = $derived(accumulatedPoints + currentStationPoints());

	function unlockNextHint() {
		const totalHints = currentTarget()?.content?.hints?.length || 0;
		if (activeHintsCount < totalHints) {
			activeHintsCount++;
			currentHintViewIndex = activeHintsCount - 1;
		}
	}

	function useCompass() {
		compassUsedAtCurrentStation = true;
		currentHintViewIndex = (currentTarget()?.content?.hints?.length || 0);
	}

	async function nextStation() {
		const currentIndex = allStations.findIndex(s => s.id === lastStationId);

		accumulatedPoints += currentStationPoints();

		console.log(`Állomás teljesítve! Pontszám: ${accumulatedPoints}`);

		if (currentIndex < allStations.length - 1) {
			lastStationId = allStations[currentIndex + 1].id;
			activeHintsCount = 0;
			currentHintViewIndex = 0;
			compassUsedAtCurrentStation = false;
			isExplanationOpen = false;
			isRiddleOpen = true;

			const target = currentTarget();
			if (userPos && target) {
				distanceInMeters = calculateDistance(userPos, { lat: target.latitude, lon: target.longitude });
			}
		} else {
			await syncGameProgress();
			goto('/dashboard');
		}
	}

	function updateLocation(newLat: number, newLon: number) {
		const isFirstFix = userPos === null;
		userPos = { lat: newLat, lon: newLon };

		if (isFirstFix) {
			initMap(newLat, newLon);
		}

		const target = currentTarget();
		if (target) {
			const dLon = (target.longitude - userPos.lon) * Math.PI / 180;
			const y = Math.sin(dLon) * Math.cos(target.latitude * Math.PI / 180);
			const x = Math.cos(userPos.lat * Math.PI / 180) * Math.sin(target.latitude * Math.PI / 180) -
				Math.sin(userPos.lat * Math.PI / 180) * Math.cos(target.latitude * Math.PI / 180) * Math.cos(dLon);
			compassRotation = (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;

			distanceInMeters = calculateDistance(userPos, { lat: target.latitude, lon: target.longitude });

			if (distanceInMeters < 15 && !isExplanationOpen) {
				isExplanationOpen = true;
				isRiddleOpen = false;
				isHintModalOpen = false;
			}
		}

		if (playerMarker) playerMarker.setLatLng([newLat, newLon]);
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

	function startTracking() {
		clearInterval(timerInterval);
		timerInterval = setInterval(() => {
			elapsedSec++;
			if (elapsedSec % 10 === 0) syncGameProgress();
		}, 1000);
	}

	async function syncGameProgress() {
		if (!sessionId || !lastStationId || !auth.token) return;
		try {
			await fetch('http://localhost:8080/api/game/update', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${auth.token}` },
				body: JSON.stringify({
					sessionId, lastStationId, elapsedSec, distanceInMeters: Math.round(distanceInMeters),
					points: totalScore
				})
			});
		} catch (e) { console.warn("Auto-sync hiba", e); }
	}

	onMount(async () => {
		startTracking();

		if (navigator.geolocation) {
			watchId = navigator.geolocation.watchPosition(
				p => updateLocation(p.coords.latitude, p.coords.longitude),
				e => {
					console.error("GPS hiba a játék közben:", e);
					if (userPos === null) updateLocation(46.073504717136054, 18.22113854980469);
				},
				{ enableHighAccuracy: true }
			);
		}

		try {
			const startLat = userPos?.lat || 46.073504717136054;
			const startLon = userPos?.lon || 18.22113854980469;

			const res = await fetch(`http://localhost:8080/api/adventures/${adventureId}?lat=${startLat}&lon=${startLon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				const data = await res.json();
				adventureTitle = data.title;
				allStations = data.stations || [];
				lastStationId = allStations[0]?.id || null;
				isRiddleOpen = true;

				if (userPos) {
					distanceInMeters = calculateDistance(userPos, { lat: allStations[0].latitude, lon: allStations[0].longitude });
				}
			}
		} catch (e) { console.error(e); }
	});

	async function exitGame() {

		await syncGameProgress();
		goto(`/adventures/${adventureId}?lat=${userPos.lat}&lon=${userPos.lon}`);
	}

	onDestroy(() => {
		if (map) map.remove();
		clearInterval(timerInterval);
		if (navigator.geolocation) navigator.geolocation.clearWatch(watchId);
		syncGameProgress();
	});
</script>

<main class="flex flex-col h-[calc(100vh-128px)] bg-[#F5F2EA] font-josefin overflow-hidden relative">
	<header class="h-14 bg-[#775D4D] text-[#F5F2EA] flex items-center px-4 rounded-xl mx-2 shadow-lg z-10 shrink-0">
		<div class="flex-1 font-mono text-sm tracking-tighter">
			{new Date(elapsedSec * 1000).toISOString().substr(11, 8)}
		</div>
		<div class="flex-[2] text-center font-bold truncate px-2 text-sm italic">{adventureTitle}</div>
		<div class="flex-1 text-right text-sm font-bold">
			{#if userPos !== null}{Math.round(distanceInMeters)} m{:else}... m{/if}
		</div>
	</header>

	<section class="flex-grow m-2 mb-6 rounded-3xl overflow-hidden shadow-2xl border-4 border-[#775D4D] relative bg-gray-200">

		<button
			onclick={exitGame}
			class="absolute top-4 left-4 z-[450] bg-[#775D4D] p-2 px-4 rounded-xl text-white text-[10px] font-black uppercase shadow-lg active:scale-95 transition-transform"
		>
			Kilépés
		</button>

		<div id="map-container" class="w-full h-full z-0">
			{#if userPos === null}
				<div class="absolute inset-0 flex flex-col items-center justify-center bg-[#F5F2EA] z-[500] gap-3">
					<div class="w-10 h-10 border-4 border-[#775D4D] border-t-transparent rounded-full animate-spin"></div>
					<p class="text-sm font-bold text-[#775D4D]">GPS koordináták betöltése...</p>
				</div>
			{/if}
		</div>

		<div class="absolute bottom-6 left-0 right-0 px-6 flex justify-between items-center z-[450]">
			<button onclick={() => isRiddleOpen = true} class="bg-city-brown w-14 h-14 rounded-full shadow-xl flex items-center justify-center border-2 border-city-cream">
				<QuestionCircleOutline class="w-8 h-8 text-white" />
			</button>

			<button onclick={() => isHintModalOpen = true} class="bg-city-brown w-14 h-14 rounded-full shadow-xl flex items-center justify-center border-2 border-city-cream active:scale-95 transition-transform">
				<LightbulbOutline class="w-8 h-8 text-white" />
			</button>
		</div>

		{#if isRiddleOpen}
			<div class="absolute inset-0 z-[500] bg-[#775D4D]/90 backdrop-blur-sm flex items-center justify-center p-6 text-center">
				<div class="bg-[#F5F2EA] p-8 rounded-3xl shadow-2xl border-t-8 border-[#775D4D] max-w-xs">
					<h3 class="text-[#775D4D] font-black uppercase tracking-widest mb-4 text-xs">Aktuális Rejtvény</h3>
					<p class="text-gray-800 italic font-medium">{currentTarget()?.content?.riddle || "Keresd az állomást!"}</p>
					<button onclick={() => isRiddleOpen = false} class="mt-6 bg-[#775D4D] text-white px-6 py-2 rounded-full font-bold text-xs uppercase">Bezárás</button>
				</div>
			</div>
		{/if}

		{#if isHintModalOpen}
			<div class="absolute inset-0 z-[600] bg-black/60 backdrop-blur-md flex items-center justify-center p-4">
				<div class="bg-[#F5F2EA] w-full max-w-xs rounded-3xl p-6 shadow-2xl border-t-8 border-[#775D4D]">
					<h3 class="text-city-brown font-black uppercase text-[10px] tracking-widest mb-2 text-center">
						{currentHintViewIndex === (currentTarget()?.content?.hints?.length || 0) ? 'Iránytű' : 'Segítség'}
					</h3>

					<div class="min-h-[120px] flex flex-col items-center justify-center text-center py-4">
						{#if activeHintsCount === 0}
							<p class="text-gray-600">Biztosan segítséget kérsz?</p>
							<p class="text-red-500 text-xs font-bold mt-1">(-1 pont)</p>
						{:else if currentHintViewIndex === (currentTarget()?.content?.hints?.length || 0)}
							<div class="flex flex-col items-center gap-2 animate-fade-in">
								<p class="text-xs text-gray-500 mb-1">Kövesd a nyilat a cél felé!</p>
								<div style="transform: rotate({compassRotation}deg)" class="transition-transform duration-300 w-16 h-16 rounded-full bg-red-600 flex items-center justify-center shadow-lg">
									<ArrowUpOutline class="w-10 h-10 text-white" />
								</div>
							</div>
						{:else}
							<p class="italic text-gray-800">
								{currentTarget()?.content?.hints?.[currentHintViewIndex] || "Nincs elérhető hint."}
							</p>
						{/if}
					</div>

					<div class="flex flex-col gap-2 mt-4">
						{#if activeHintsCount === 0}
							<button onclick={unlockNextHint} class="bg-[#775D4D] text-white py-3 rounded-xl font-bold uppercase text-xs">Igen, kérem</button>
						{:else}
							<div class="flex justify-between items-center bg-gray-100 rounded-xl p-2 mb-2">
								<button disabled={currentHintViewIndex === 0} onclick={() => currentHintViewIndex--} class="p-1 disabled:opacity-20">
									<ChevronLeftOutline/>
								</button>

								<span class="text-xs font-bold">{currentHintViewIndex + 1} / {totalHintTabs()}</span>

								<button
									disabled={currentHintViewIndex === totalHintTabs() - 1 || currentHintViewIndex === activeHintsCount - 1}
									onclick={() => currentHintViewIndex++}
									class="p-1 disabled:opacity-20"
								>
									<ChevronRightOutline/>
								</button>
							</div>

							{#if activeHintsCount < (currentTarget()?.content?.hints?.length || 0)}
								<button onclick={unlockNextHint} class="bg-city-brown text-city-cream py-3 rounded-xl text-[10px] font-bold uppercase">Újabb hint (-1 pont)</button>
							{:else}
								{#if activeHintsCount === (currentTarget()?.content?.hints?.length || 0) && !compassUsedAtCurrentStation}
									<button onclick={useCompass} class="bg-red-500 text-white py-3 rounded-xl text-[10px] font-bold uppercase">Iránytű aktiválása (0 pont)</button>
								{/if}
							{/if}
						{/if}
						<button onclick={() => isHintModalOpen = false} class="text-gray-400 text-[10px] font-bold uppercase mt-2">Bezárás</button>
					</div>
				</div>
			</div>
		{/if}

		{#if isExplanationOpen}
			<div class="absolute inset-0 z-[700] bg-[#2F5D50]/90 backdrop-blur-xl flex items-center justify-center p-4">
				<div class="bg-white w-full max-w-sm rounded-[40px] p-8 text-center shadow-2xl">
					<CheckCircleSolid class="w-16 h-16 text-green-500 mx-auto mb-4" />
					<h2 class="text-2xl font-black text-[#2F5D50]">Sikerült!</h2>
					<div class="bg-green-100 text-green-700 py-1 px-3 rounded-full text-xs font-bold inline-block my-2">+{currentStationPoints()} PONT</div>

					<div class="text-left bg-gray-50 p-4 rounded-2xl my-4 text-sm italic text-gray-600 border-l-4 border-[#2F5D50]">
						{currentTarget()?.content?.explanation}
					</div>

					<button onclick={nextStation} class="w-full bg-[#2F5D50] text-white py-4 rounded-2xl font-black uppercase">
						{allStations.indexOf(currentTarget()) === allStations.length - 1 ? 'Befejezés' : 'Következő állomás'}
					</button>
				</div>
			</div>
		{/if}
	</section>
</main>

<style>
    :global(body) { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; position: fixed; }
    #map-container { height: 100%; width: 100%; }
    :global(.leaflet-control-container) { display: none !important; }
</style>