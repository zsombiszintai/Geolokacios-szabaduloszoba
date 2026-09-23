<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount, onDestroy } from 'svelte';
	import { page } from '$app/state';
	import type { Map as LeafletMap, Marker } from 'leaflet';
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

	let L: typeof import('leaflet');
	let map: LeafletMap | undefined;
	let playerMarker: Marker | undefined;
	let watchId: number | undefined;
	let timerInterval: ReturnType<typeof setInterval> | undefined;

	let locationError = $state('');
	let gameError = $state('');
	let gameLoading = $state(true);
	let locationAccuracy = $state<number | null>(null);

	let finishing = $state(false);
	let finished = $state(false);
	let finishError = $state('');

	let disposed = false;
	let gameLoadStarted = false;
	const gameController = new AbortController();

	let progressSaveQueue: Promise<boolean> = Promise.resolve(true);
	let saveError = $state('');

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
		if (finishing) return;

		if (finished) {
			await goto('/dashboard');
			return;
		}

		if (!isExplanationOpen) return;

		const currentIndex = allStations.findIndex(
			station => station.id === lastStationId
		);

		if (currentIndex < 0) return;

		const earnedPoints = currentStationPoints();
		const isLast = currentIndex === allStations.length - 1;

		if (!isLast) {
			accumulatedPoints += earnedPoints;

			isExplanationOpen = false;
			lastStationId = allStations[currentIndex + 1].id;

			activeHintsCount = 0;
			currentHintViewIndex = 0;
			compassUsedAtCurrentStation = false;
			isRiddleOpen = true;

			const target = currentTarget();

			if (userPos && target) {
				distanceInMeters = calculateDistance(userPos, {
					lat: target.latitude,
					lon: target.longitude
				});
			}

			await syncGameProgress();
			return;
		}

		if (!auth.token) {
			finishError = 'A mentéshez bejelentkezés szükséges.';
			return;
		}

		finishing = true;
		finishError = '';
		clearInterval(timerInterval);

		const finalPoints = accumulatedPoints + earnedPoints;

		try {
			const res = await fetch(
				'https://api.zsomborszintai.com/api/game/finish',
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
						Authorization: `Bearer ${auth.token}`
					},
					body: JSON.stringify({
						sessionId,
						lastStationId,
						elapsedSec,
						distanceInMeters: Math.round(distanceInMeters),
						points: finalPoints
					})
				}
			);
			await progressSaveQueue;

			if (!res.ok) {
				throw new Error(`A befejezés mentése sikertelen (HTTP ${res.status}).`);
			}

			accumulatedPoints = finalPoints;
			finished = true;
		} catch (error) {
			finishError = error instanceof Error
				? error.message
				: 'A mentés sikertelen. Próbáld újra.';

			return;
		} finally {
			finishing = false;
		}

		await goto('/dashboard');
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

			if (
				distanceInMeters < 15 &&
				locationAccuracy !== null &&
				locationAccuracy <= 20 &&
				!locationError &&
				!isExplanationOpen
			) {
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
			if (
				gameLoading ||
				gameError ||
				locationError ||
				!userPos ||
				finishing ||
				finished
			) return;

			elapsedSec++;

			if (elapsedSec % 10 === 0) {
				void syncGameProgress();
			}
		}, 1000);
	}

	function syncGameProgress(): Promise<boolean> {
		if (
			gameLoading ||
			gameError ||
			finishing ||
			finished ||
			!sessionId ||
			!lastStationId ||
			!auth.token
		) {
			return Promise.resolve(false);
		}

		const token = auth.token;

		const payload = {
			sessionId,
			lastStationId,
			elapsedSec,
			distanceInMeters: Math.round(distanceInMeters),
			points: accumulatedPoints
		};

		progressSaveQueue = progressSaveQueue.then(async () => {
			try {
				const response = await fetch(
					'https://api.zsomborszintai.com/api/game/update',
					{
						method: 'POST',
						headers: {
							'Content-Type': 'application/json',
							Authorization: `Bearer ${token}`
						},
						body: JSON.stringify(payload)
					}
				);

				if (!response.ok) {
					throw new Error(`Mentési hiba: HTTP ${response.status}`);
				}

				saveError = '';
				return true;
			} catch (error) {
				console.error('Mentési hiba:', error);
				saveError = 'A mentés sikertelen. Ellenőrizd a kapcsolatot!';
				return false;
			}
		});

		return progressSaveQueue;
	}

	onMount(() => {
		async function initializeLocation() {
			if (!navigator.geolocation) {
				locationError = 'Ez a böngésző nem támogatja a helymeghatározást.';
				return;
			}

			try {
				L = await import('leaflet');
				if (disposed) return;

				watchId = navigator.geolocation.watchPosition(
					(position) => {
						if (disposed) return;

						locationError = '';
						locationAccuracy = position.coords.accuracy;

						updateLocation(
							position.coords.latitude,
							position.coords.longitude
						);
					},
					(error) => {
						if (disposed) return;

						locationError =
							error.code === 1
								? 'A játékhoz helyhozzáférés szükséges. Engedélyezd a webhelybeállításokban, majd próbáld újra.'
								: error.code === 2
									? 'A helyzeted jelenleg nem állapítható meg. Ellenőrizd a telefon helymeghatározását.'
									: 'A helymeghatározás túllépte az időkorlátot. Új helyzetre várunk…';
					},
					{
						enableHighAccuracy: true,
						timeout: 20000,
						maximumAge: 0
					}
				);
			} catch (error) {
				if (disposed) return;

				console.error('Térkép inicializálási hiba:', error);
				gameError = 'Nem sikerült betölteni a térképet.';
			}
		}

		void initializeLocation();
		startTracking();
	});

	$effect(() => {
		const token = auth.token;
		const activeSessionId = sessionId;
		const requestedAdventureId = adventureId;

		if (!token || gameLoadStarted) return;

		if (!Number.isSafeInteger(activeSessionId) || activeSessionId <= 0) {
			gameError = 'Hiányzó vagy hibás mentésazonosító.';
			gameLoading = false;
			return;
		}

		gameLoadStarted = true;

		async function loadGame() {
			try {
				const options = {
					headers: { Authorization: `Bearer ${token}` },
					signal: gameController.signal
				};

				const sessionResponse = await fetch(
					`https://api.zsomborszintai.com/api/game/session/${activeSessionId}`,
					options
				);

				if (!sessionResponse.ok) {
					throw new Error(
						`A mentés betöltése sikertelen (HTTP ${sessionResponse.status}).`
					);
				}

				const saved = await sessionResponse.json();
				if (disposed) return;

				if (saved.completed) {
					throw new Error('Ezt a játékmenetet már befejezted.');
				}

				if (
					requestedAdventureId &&
					String(saved.adventureId) !== requestedAdventureId
				) {
					throw new Error('A mentés nem ehhez a kalandhoz tartozik.');
				}

				const response = await fetch(
					`https://api.zsomborszintai.com/api/adventures/${saved.adventureId}`,
					options
				);

				if (!response.ok) {
					throw new Error(
						`A kaland betöltése sikertelen (HTTP ${response.status}).`
					);
				}

				const data = await response.json();
				if (disposed) return;

				if (!Array.isArray(data.stations)) {
					throw new Error('A kaland állomásai nem tölthetők be.');
				}

				const stations = data.stations
					.filter((station: any) => station.seqNumber > 0)
					.sort((a: any, b: any) => a.seqNumber - b.seqNumber);

				const savedStation = stations.find(
					(station: any) => station.id === saved.lastStationId
				);

				if (!savedStation) {
					throw new Error(
						'A mentett állomás már nem található ebben a kalandban.'
					);
				}

				adventureTitle = data.title;
				allStations = stations;
				lastStationId = savedStation.id;

				elapsedSec = saved.elapsedSec ?? 0;
				accumulatedPoints = saved.points ?? 0;
				distanceInMeters = saved.distanceInMeters ?? 0;

				activeHintsCount = 0;
				currentHintViewIndex = 0;
				compassUsedAtCurrentStation = false;

				isExplanationOpen = false;
				isHintModalOpen = false;
				isRiddleOpen = true;

				gameError = '';
				gameLoading = false;
			} catch (error) {
				if (disposed) return;

				gameError = error instanceof Error
					? error.message
					: 'A mentés betöltése sikertelen.';

				gameLoading = false;
			}
		}

		void loadGame();
	});

	async function exitGame() {
		if (finishing) return;

		if (!gameLoading && !gameError && !finished) {
			const saved = await syncGameProgress();
			if (!saved) return;
		}

		if (!adventureId) {
			await goto('/dashboard');
			return;
		}

		const coordinates = userPos
			? `?lat=${userPos.lat}&lon=${userPos.lon}`
			: '';

		await goto(
			`/adventures/${encodeURIComponent(adventureId)}${coordinates}`
		);
	}

	onDestroy(() => {
		disposed = true;
		gameController.abort();

		clearInterval(timerInterval);

		if (
			typeof navigator !== 'undefined' &&
			navigator.geolocation &&
			watchId !== undefined
		) {
			navigator.geolocation.clearWatch(watchId);
		}

		map?.remove();
		map = undefined;
		playerMarker = undefined;
	});
</script>

<main class="flex flex-col h-[calc(100vh-128px)] bg-[#F5F2EA] font-josefin overflow-hidden relative">
	{#if saveError}
		<div
			role="alert"
			class="relative z-[900] bg-red-50 px-4 py-2 text-sm font-bold text-red-700"
		>
			{saveError}
		</div>
	{/if}

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

		<div id="map-container" class="w-full h-full z-0"></div>

		{#if !userPos || gameLoading || locationError || gameError}
			<div
				class="absolute inset-0 flex flex-col items-center justify-center bg-[#F5F2EA] z-[800] gap-4 p-6 text-center"
				role="status"
				aria-live="polite"
			>
				<p class="text-sm font-bold text-[#775D4D]">
					{locationError || gameError || (
						!userPos
							? 'Helyzeted meghatározása…'
							: !auth.token
								? 'Bejelentkezésre várunk…'
								: 'Kaland betöltése…'
					)}
				</p>

				{#if locationError || gameError}
					<button
						onclick={() => window.location.reload()}
						class="bg-[#775D4D] text-white px-6 py-3 rounded-xl font-bold"
					>
						Újrapróbálás
					</button>
				{/if}

				<button
					onclick={exitGame}
					class="text-[#775D4D] underline font-bold"
				>
					Kilépés
				</button>
			</div>
		{/if}

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

					{#if finishError}
						<p role="alert" class="text-red-600 text-sm font-bold mb-3">
							{finishError}
						</p>
					{/if}

					<button
						onclick={nextStation}
						disabled={finishing}
						class="w-full bg-[#2F5D50] text-white py-4 rounded-2xl font-black uppercase disabled:opacity-50"
					>
						{finishing
							? 'Mentés...'
							: finished
								? 'Vissza a főoldalra'
								: allStations.indexOf(currentTarget()) === allStations.length - 1
									? 'Befejezés'
									: 'Következő állomás'}
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