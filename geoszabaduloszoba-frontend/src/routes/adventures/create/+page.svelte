<script lang="ts">
	import { onDestroy, tick } from 'svelte';
	import 'leaflet/dist/leaflet.css';
	import { auth } from '$lib/auth.svelte.js';
	import { goto } from '$app/navigation';

	let title = $state("");
	let description = $state("");
	let difficulty = $state(1);
	const difficultyLabels = ["Könnyű", "Közepes", "Nehéz"];

	let showMapModal = $state(false);
	let showLeaveModal = $state(false);
	let activeStationIndex = $state<number | null>(null);
	let errorMessage = $state("");
	let skipLeaveCheck = $state(false);

	let L: any;
	let map: any;
	let tempMarker: any;

	type Coordinates = {
		latitude: number;
		longitude: number;
	};

	let hasStartingPoint = $state(false);
	let startingPoint = $state<Coordinates | null>(null);

	let selectingStartingPoint = $state(false);
	let selectedPosition = $state<Coordinates | null>(null);
	let mapError = $state('');

	let mapRequestId = 0;
	let mapSizeFrame: number | undefined;
	interface StationContent {
		riddle: string;
		explanation: string;
		hints: string[];
	}

	interface Station {
		id: string;
		latitude: number | null;
		longitude: number | null;
		content: StationContent;
	}


	let stations = $state<Station[]>([
		{
			id: crypto.randomUUID(),
			latitude: null,
			longitude: null,
			content: { riddle: "", explanation: "", hints: ["", "", ""] }
		}
	]);

	let hasContent = $derived(
		hasStartingPoint ||
		title.trim() !== '' ||
		description.trim() !== '' ||
		stations.some(s =>
			s.latitude !== null ||
			s.longitude !== null ||
			s.content.riddle.trim() !== '' ||
			s.content.explanation.trim() !== '' ||
			s.content.hints.some(h => h.trim() !== '')
		)
	);

	function handleExit() {
		if (!hasContent) {
			skipLeaveCheck = true;
			goto('/adventures');
		} else {
			showLeaveModal = true;
		}
	}

	function buildStationsPayload() {
		const playableStations = stations.map((station, index) => ({
			latitude: station.latitude,
			longitude: station.longitude,
			seqNumber: index + 1,
			content: station.content
		}));

		if (!hasStartingPoint) {
			return playableStations;
		}

		if (!startingPoint) {
			throw new Error('Jelöld ki a kezdőállomást!');
		}

		return [
			{
				latitude: startingPoint.latitude,
				longitude: startingPoint.longitude,
				seqNumber: 0,
				content: {
					riddle: '',
					explanation: '',
					hints: []
				}
			},
			...playableStations
		];
	}

	async function saveDraft() {
		if (!auth.token) {
			errorMessage = 'A mentéshez jelentkezz be!';
			showLeaveModal = false;
			return;
		}

		if (hasStartingPoint && !startingPoint) {
			errorMessage = 'Jelöld ki a kezdőállomást!';
			showLeaveModal = false;
			return;
		}

		const difficultyEnum = ["EASY", "MEDIUM", "HARD"][difficulty];

		const adventureData = {
			title: title.trim() || "Cím nélküli piszkozat",
			description,
			difficulty: difficultyEnum,
			status: "DRAFT",
			stations: buildStationsPayload()
		};

		try {
			const response = await fetch('https://api.zsomborszintai.com/api/create-adventure', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify(adventureData)
			});

			if (response.ok) {
				skipLeaveCheck = true;
				showLeaveModal = false;
				goto('/adventures');
			} else {
				alert("Nem sikerült elmenteni a piszkozatot.");
			}
		} catch (err) {
			console.error(err);
			alert("Hálózati hiba a piszkozat mentésekor.");
		}
	}

	function destroyMap() {
		if (mapSizeFrame !== undefined) {
			cancelAnimationFrame(mapSizeFrame);
			mapSizeFrame = undefined;
		}

		map?.remove();
		map = undefined;

		tempMarker = undefined;
	}

	function drawSelectedMarker(position: Coordinates) {
		if (!map || !L) return;

		const coordinates: [number, number] = [
			position.latitude,
			position.longitude
		];

		if (tempMarker) {
			tempMarker.setLatLng(coordinates);
			return;
		}

		const icon = L.divIcon({
			className: 'cityscape-selection-marker',
			html: `
      <div style="
        width:32px;
        height:32px;
        box-sizing:border-box;
        border:4px solid white;
        border-radius:50%;
        background:${selectingStartingPoint ? '#2F5D50' : '#dc2626'};
        box-shadow:0 2px 12px rgba(0,0,0,.45);
      "></div>
    `,
			iconSize: [32, 32],
			iconAnchor: [16, 16]
		});

		tempMarker = L.marker(coordinates, {
			icon,
			draggable: true,
			autoPan: true
		}).addTo(map);

		tempMarker.on('dragend', () => {
			mapError = '';

			const position = tempMarker.getLatLng();

			selectedPosition = {
				latitude: position.lat,
				longitude: position.lng
			};
		});
	}

	async function openMap(index: number | null) {
		const requestId = ++mapRequestId;

		destroyMap();

		selectingStartingPoint = index === null;
		activeStationIndex = index;
		mapError = '';

		if (index === null) {
			selectedPosition = startingPoint ? { ...startingPoint } : null;
		} else {
			const station = stations[index];

			selectedPosition =
				station.latitude !== null && station.longitude !== null
					? {
						latitude: station.latitude,
						longitude: station.longitude
					}
					: null;
		}

		showMapModal = true;
		await tick();

		try {
			if (!L) {
				L = await import('leaflet');
			}

			if (requestId !== mapRequestId || !showMapModal) return;

			const firstLocatedStation = stations.find(
				station => station.latitude !== null && station.longitude !== null
			);

			const center =
				selectedPosition ??
				(hasStartingPoint ? startingPoint : null) ??
				(firstLocatedStation
					? {
						latitude: firstLocatedStation.latitude!,
						longitude: firstLocatedStation.longitude!
					}
					: null);

			const initialView: [number, number] = center
				? [center.latitude, center.longitude]
				: [46.076, 18.228];

			map = L.map('map-selector').setView(
				initialView,
				center ? 16 : 13
			);

			L.tileLayer(
				'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
				{
					attribution: '&copy; OpenStreetMap contributors',
					maxZoom: 19
				}
			).addTo(map);

			if (selectedPosition) {
				drawSelectedMarker(selectedPosition);
			}

			map.on('click', (event: any) => {
				mapError = '';

				selectedPosition = {
					latitude: event.latlng.lat,
					longitude: event.latlng.lng
				};

				drawSelectedMarker(selectedPosition);
			});

			mapSizeFrame = requestAnimationFrame(() => {
				if (requestId === mapRequestId && map) {
					map.invalidateSize();
				}
			});
		} catch (error) {
			if (requestId !== mapRequestId || !showMapModal) return;

			destroyMap();
			console.error('Térképbetöltési hiba:', error);
		}
	}

	function closeMap() {
		++mapRequestId;
		destroyMap();

		showMapModal = false;
		activeStationIndex = null;
		selectedPosition = null;
	}

	function saveAndClose() {
		if (!selectedPosition) {
			mapError = 'Bökj a térképre a helyszín kijelöléséhez!';
			return;
		}

		if (selectingStartingPoint) {
			startingPoint = { ...selectedPosition };
			hasStartingPoint = true;
		} else if (activeStationIndex !== null) {
			stations[activeStationIndex].latitude = selectedPosition.latitude;
			stations[activeStationIndex].longitude = selectedPosition.longitude;
		}

		closeMap();
	}

	function toggleStartingPoint() {
		if (hasStartingPoint) {
			hasStartingPoint = false;
			startingPoint = null;
		} else {
			void openMap(null);
		}
	}

	onDestroy(() => {
		++mapRequestId;
		destroyMap();
	});

	function addStation() {
		stations = [...stations, {
			id: crypto.randomUUID(),
			latitude: null,
			longitude: null,
			content: { riddle: "", explanation: "", hints: ["", "", ""] }
		}];
	}

	function removeStation(index: number) {
		stations = stations.filter((_, i) => i !== index);
	}

	async function handleSubmit() {

		if (!auth.token) {
			errorMessage = "Nincs érvényes bejelentkezés!";
			return;
		}
		if (!title.trim() || !description.trim()) {
			errorMessage = "A kaland neve és leírása kötelező!";
			return;
		}

		if (hasStartingPoint && !startingPoint) {
			errorMessage = 'Jelöld ki a kezdőállomást!';
			return;
		}

		if (stations.length === 0) {
			errorMessage = 'Legalább egy rejtvényes állomás szükséges!';
			return;
		}

		for (let i = 0; i < stations.length; i++) {
			const s = stations[i];
			if (s.latitude === null || s.longitude === null) {
				errorMessage = `A(z) ${i + 1}. állomás helyszíne nincs kijelölve!`;
				return;
			}
			if (!s.content.riddle.trim()) {
				errorMessage = `A(z) ${i + 1}. állomás rejtvénye hiányzik!`;
				return;
			}
			if (!s.content.explanation.trim()) {
				errorMessage = `A(z) ${i + 1}. állomás magyarázata (érdekesség) kötelező!`;
				return;
			}
			if (s.content.hints.some(h => !h.trim())) {
				errorMessage = `A(z) ${i + 1}. állomáshoz mind a 3 segítséget meg kell adnod!`;
				return;
			}
		}

		errorMessage = "";
		const difficultyEnum = ["EASY", "MEDIUM", "HARD"][difficulty];

		const adventureData = {
			title,
			description,
			status: "PENDING",
			difficulty: difficultyEnum,
			stations: buildStationsPayload()
		};

		try {
			console.log("Küldés indítása...");
			console.log("Token állapota:", auth.token ? "Van token" : "Nincs token");
			console.log("Küldött adatok:", adventureData);
			const response = await fetch('https://api.zsomborszintai.com/api/create-adventure', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify(adventureData)
			});

			console.log("Válasz státusz:", response.status);

			if (response.ok) {
				goto('/adventures');
			} else if (response.status === 401) {
				errorMessage = "Lejárt a munkamenet! Kérlek, jelentkezz be újra.";
			} else {
				errorMessage = "Hiba történt a mentés során!";
			}
		} catch (err) {
			errorMessage = "Hálózati hiba történt!";
		}
	}

	function discardAndLeave() {
		skipLeaveCheck = true;
		showLeaveModal = false;
		goto('/adventures');
	}
</script>

<main class="min-h-screen bg-[#F5F2EA] px-6 pt-16 pb-32">
	<nav class="fixed top-16 left-0 w-full z-[100] p-4">
		<button
			onclick={handleExit}
			class="p-3 bg-white rounded-2xl shadow-sm text-[#8D7462] border-b-4 border-[#8D7462]/10 active:scale-95 transition-all"
		>
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
				<path d="M19 12H5M12 19l-7-7 7-7"/>
			</svg>
		</button>
	</nav>

	<header class="mb-10 mt-4">
		<h1 class="text-4xl font-black text-[#2F5D50] uppercase tracking-tight leading-none mb-2">
			Új Kaland<br/>Tervezése
		</h1>
		<div class="w-12 h-1.5 bg-[#8D7462] rounded-full"></div>
	</header>

	{#if errorMessage}
		<div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded-xl mb-6 font-bold text-xs">
			{errorMessage}
		</div>
	{/if}

	<section class="space-y-4 mb-10">
		<div class="relative">
			<input
				class="input-custom"
				placeholder="Kaland neve..."
				bind:value={title}
			/>
		</div>
		<textarea
			class="input-custom min-h-[120px] py-4"
			placeholder="Írj egy kedvcsináló leírást..."
			bind:value={description}
		></textarea>
	</section>

	<section class="mb-10 bg-white/50 p-6 rounded-[2rem] border-b-4 border-[#2F5D50]/10">
		<header class="flex justify-between items-center mb-4 px-2">
			<span class="label-city">Nehézségi szint</span>
			<span class="text-[#2F5D50] font-black uppercase text-xs">{difficultyLabels[difficulty]}</span>
		</header>
		<input type="range" min="0" max="2" bind:value={difficulty} class="slider-city w-full" />
	</section>

	<section class="mb-8 bg-white p-6 rounded-[2rem] shadow-sm">
		<button
			type="button"
			role="checkbox"
			aria-checked={hasStartingPoint}
			onclick={toggleStartingPoint}
			class="w-full flex items-center gap-3 text-left text-[#2F5D50] font-black"
		>
    <span
			aria-hidden="true"
	    class="w-7 h-7 shrink-0 rounded-lg border-2 border-[#2F5D50] flex items-center justify-center
        {hasStartingPoint ? 'bg-[#2F5D50] text-white' : 'bg-white'}"
		>
      {hasStartingPoint ? '✓' : '+'}
    </span>

			Kezdőállomás hozzáadása
		</button>

		<p class="mt-3 text-sm text-[#8D7462]">
			{hasStartingPoint
				? 'A játék előtt ide navigáljuk a játékost.'
				: 'Kezdőállomás nélkül rögtön a játék indul.'}
		</p>

		{#if hasStartingPoint && startingPoint}
			<p class="mt-3 text-xs text-[#8D7462]">
				{startingPoint.latitude.toFixed(6)},
				{startingPoint.longitude.toFixed(6)}
			</p>

			<button
				type="button"
				onclick={() => openMap(null)}
				class="mt-3 bg-[#2F5D50] text-white px-5 py-3 rounded-xl font-bold text-sm"
			>
				Helyszín módosítása
			</button>
		{/if}
	</section>

	<section class="space-y-6">
		<header class="flex justify-between items-center px-2">
			<h2 class="label-city">Állomások ({stations.length})</h2>
		</header>

		{#each stations as station, i (station.id)}
			<article class="bg-[#8D7462] p-6 rounded-[2.5rem] shadow-xl border border-white/10 relative overflow-hidden">
				<header class="flex justify-between items-start mb-6">
					<div class="flex items-center gap-3">
            <span class="w-10 h-10 rounded-2xl bg-white text-[#8D7462] flex items-center justify-center font-black text-lg shadow-md">
              {i + 1}
            </span>
						<div>
							<p class="text-[10px] font-black text-white/50 uppercase tracking-widest leading-none">Állomás</p>
							<h3 class="text-white font-black text-xl tracking-tight">Helyszín</h3>
						</div>
					</div>
					{#if stations.length > 1}
						<button onclick={() => removeStation(i)} class="p-2 bg-black/20 rounded-xl text-white/70 hover:text-white transition-colors">
							<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6L6 18M6 6l12 12"/></svg>
						</button>
					{/if}
				</header>

				<div class="space-y-5">
					<div class="space-y-2">
						<label class="text-[10px] font-black text-[#F5F2EA]/60 uppercase ml-2">feladvány</label>
						<textarea
							class="input-card-inner min-h-[80px]"
							placeholder="Add meg a rejtvényt..."
							bind:value={station.content.riddle}
						></textarea>
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-black text-[#F5F2EA]/60 uppercase ml-2">Segítségek</label>
						<div class="grid gap-2">
							<input class="input-card-inner text-sm" placeholder="1. Könnyű nyom" bind:value={station.content.hints[0]} />
							<input class="input-card-inner text-sm" placeholder="2. Konkrétabb segítség" bind:value={station.content.hints[1]} />
							<input class="input-card-inner text-sm" placeholder="3. Szinte a megoldás" bind:value={station.content.hints[2]} />
						</div>
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-black text-[#F5F2EA]/60 uppercase ml-2">Magyarázat</label>
						<textarea
							class="input-card-inner min-h-[60px]"
							placeholder="Add meg a rejtvény magyarázatát..."
							bind:value={station.content.explanation}
						></textarea>
					</div>

					<button
						type="button"
						onclick={() => openMap(i)}
						class="w-full py-4 rounded-2xl {station.latitude !== null && station.longitude !== null
						 ? 'bg-[#2F5D50]'
						 : 'bg-white/10 border-2 border-dashed border-white/20'} text-white font-black text-[10px] uppercase tracking-[0.2em] transition-all active:scale-[0.98]"
					>
						{station.latitude !== 0 ? 'Helyszín rögzítve' : 'Jelöld ki a térképen'}
					</button>
				</div>
			</article>
		{/each}

		<button
			class="w-full py-6 border-4 border-dashed border-[#8D7462]/20 rounded-[2.5rem] text-[#8D7462] font-black text-3xl hover:bg-[#8D7462]/5 transition-all"
			onclick={addStation}
		>
			+
		</button>
	</section>

	<footer class="fixed bottom-0 left-0 w-full p-6 bg-gradient-to-t from-[#F5F2EA] via-[#F5F2EA] to-transparent z-[100]">
		<button
			class="w-full bg-[#2F5D50] text-white py-5 rounded-[2rem] font-black uppercase tracking-[0.2em] shadow-2xl active:scale-95 transition-all disabled:opacity-30"
			onclick={handleSubmit}
			disabled={!title || stations.length === 0}
		>
			Kaland Publikálása
		</button>
	</footer>
</main>

{#if showMapModal}
	<div class="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-md p-4 flex items-center justify-center">
		<div
			role="dialog"
			aria-modal="true"
			aria-label="Helyszín kijelölése"
			class="bg-[#F5F2EA] w-full max-w-sm h-[80dvh] rounded-[2rem] shadow-2xl flex flex-col overflow-hidden border-2 border-[#8D7462]"
		>
			<div class="p-4 shrink-0">
				<h3 class="font-black text-[#2F5D50]">
					{selectingStartingPoint
						? 'Kezdőállomás kijelölése'
						: `${(activeStationIndex ?? 0) + 1}. állomás kijelölése`}
				</h3>
			</div>

			<div id="map-selector" class="flex-1 min-h-0 w-full"></div>

			<div class="p-4 bg-white shrink-0">
				{#if mapError}
					<p role="alert" class="text-red-600 text-sm mb-3">
						{mapError}
					</p>
				{/if}

				{#if selectedPosition}
					<p class="text-xs text-[#8D7462] mb-3">
						{selectedPosition.latitude.toFixed(6)},
						{selectedPosition.longitude.toFixed(6)}
					</p>
				{/if}

				<div class="flex gap-3">
					<button
						type="button"
						onclick={closeMap}
						class="flex-1 bg-[#F5F2EA] text-[#8D7462] py-4 rounded-2xl font-bold"
					>
						Mégse
					</button>

					<button
						type="button"
						onclick={saveAndClose}
						disabled={!selectedPosition || !!mapError}
						class="flex-1 bg-[#2F5D50] text-white py-4 rounded-2xl font-black disabled:opacity-40"
					>
						Mentés
					</button>
				</div>
			</div>
		</div>
	</div>
{/if}

{#if showLeaveModal}
	<div class="fixed inset-0 z-[3000] bg-black/60 backdrop-blur-md p-6 flex items-center justify-center">
		<div class="bg-[#F5F2EA] w-full max-w-sm rounded-[2.5rem] p-6 shadow-2xl border-2 border-[#8D7462] flex flex-col gap-4 text-center">
			<h3 class="text-xl font-black text-[#2F5D50] uppercase tracking-tight">Félkész kaland</h3>
			<p class="text-xs font-bold text-[#8D7462]">Szeretnéd elmenteni a kalandot piszkozatként (Draft), mielőtt kilépsz?</p>

			<div class="flex flex-col gap-2 mt-2">
				<button
					onclick={saveDraft}
					class="w-full bg-[#2F5D50] text-white py-3.5 rounded-2xl font-black uppercase text-xs tracking-wider shadow-md active:scale-95 transition-all"
				>
					Mentés piszkozatként
				</button>

				<button
					onclick={discardAndLeave}
					class="w-full bg-red-500/10 text-red-600 py-3.5 rounded-2xl font-black uppercase text-xs tracking-wider hover:bg-red-500/20 active:scale-95 transition-all"
				>
					Kilépés mentés nélkül
				</button>

				<button
					onclick={() => showLeaveModal = false}
					class="w-full py-2.5 font-bold text-xs text-gray-500 hover:text-gray-700"
				>
					Mégse
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
    @import url('https://fonts.googleapis.com/css2?family=Josefin+Sans:wght@400;600;700&display=swap');

    :global(body) {
        font-family: 'Josefin Sans', sans-serif;
    }

    .label-city {
        @apply text-[10px] font-black uppercase tracking-[0.2em] text-[#2F5D50] opacity-40;
    }

    .input-custom {
        @apply w-full h-14 px-6 bg-white rounded-2xl border-b-4 border-[#2F5D50]/10 outline-none focus:border-[#2F5D50] transition-all text-[#2F5D50] font-bold shadow-sm placeholder:text-[#2F5D50]/60 placeholder:font-normal;
    }

    .input-card-inner {
        @apply w-full bg-black/20 border border-white/10 rounded-2xl px-4 py-3 text-white font-bold outline-none focus:bg-black/30 transition-all placeholder:text-white/20 placeholder:font-normal;
    }

    .slider-city {
        @apply appearance-none h-2 bg-[#2F5D50]/10 rounded-full outline-none;
    }

    .slider-city::-webkit-slider-thumb {
        @apply appearance-none w-6 h-6 bg-[#2F5D50] rounded-full cursor-pointer shadow-lg border-4 border-white;
    }

    :global(.leaflet-container) {
        cursor: crosshair !important;
    }
</style>