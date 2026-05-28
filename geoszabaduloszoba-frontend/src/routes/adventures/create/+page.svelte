<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { auth } from '$lib/auth.svelte.js';
	import { goto } from '$app/navigation';

	let title = $state("");
	let description = $state("");
	let difficulty = $state(1);
	const difficultyLabels = ["Könnyű", "Közepes", "Nehéz"];

	let showMapModal = $state(false);
	let activeStationIndex = $state<number | null>(null);
	let errorMessage = $state("");

	let L: any;
	let map: any;
	let tempMarker: any;

	interface StationContent {
		riddle: string;
		explanation: string;
		hints: string[];
	}

	interface Station {
		id: string;
		latitude: number;
		longitude: number;
		content: StationContent;
	}

	let stations = $state<Station[]>([
		{
			id: crypto.randomUUID(),
			latitude: 0,
			longitude: 0,
			content: { riddle: "", explanation: "", hints: ["", "", ""] }
		}
	]);

	async function openMap(index: number) {
		activeStationIndex = index;
		showMapModal = true;
		await tick();
		initMap();
	}

	async function initMap() {
		if (!L) {
			L = await import('leaflet');
			import('leaflet/dist/leaflet.css');
		}

		if (map) map.remove();

		const currentStation = stations[activeStationIndex!];
		const initialView: [number, number] = currentStation.latitude !== 0
			? [currentStation.latitude, currentStation.longitude]
			: [46.076, 18.228];

		map = L.map('map-selector').setView(initialView, 14);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		if (currentStation.latitude !== 0) {
			tempMarker = L.marker(initialView).addTo(map);
		}

		map.on('click', (e: any) => {
			const { lat, lng } = e.latlng;
			if (tempMarker) tempMarker.setLatLng(e.latlng);
			else tempMarker = L.marker(e.latlng).addTo(map);

			stations[activeStationIndex!].latitude = lat;
			stations[activeStationIndex!].longitude = lng;
		});

		setTimeout(() => map?.invalidateSize(), 100);
	}

	function saveAndClose() {
		if (activeStationIndex !== null && stations[activeStationIndex].latitude === 0) {
			alert("Kérlek, bökj rá a helyszínre a térképen!");
			return;
		}
		showMapModal = false;
	}

	function addStation() {
		stations = [...stations, {
			id: crypto.randomUUID(),
			latitude: 0,
			longitude: 0,
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

		for (let i = 0; i < stations.length; i++) {
			const s = stations[i];
			if (s.latitude === 0) {
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
			difficulty: difficultyEnum,
			stations: stations.map((s, index) => ({
				latitude: s.latitude,
				longitude: s.longitude,
				seqNumber: index + 1,
				content: s.content
			}))
		};

		try {
			console.log("Küldés indítása...");
			console.log("Token állapota:", auth.token ? "Van token" : "Nincs token");
			console.log("Küldött adatok:", adventureData);
			const response = await fetch('http://localhost:8080/api/create-adventure', {
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
</script>

<main class="min-h-screen bg-[#F5F2EA] px-6 pt-16 pb-32">
	<nav class="fixed top-16 left-0 w-full z-[100] p-4">
		<button
			onclick={() => goto('/adventures')}
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
						class="w-full py-4 rounded-2xl {station.latitude !== 0 ? 'bg-[#2F5D50]' : 'bg-white/10 border-2 border-dashed border-white/20'} text-white font-black text-[10px] uppercase tracking-[0.2em] transition-all active:scale-[0.98]"
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
		<div class="bg-[#F5F2EA] w-full max-w-sm h-[80vh] rounded-[3rem] shadow-2xl flex flex-col overflow-hidden border-2 border-[#8D7462]">
			<div id="map-selector" class="flex-grow"></div>
			<div class="p-6 bg-white">
				<button class="w-full bg-[#2F5D50] text-white py-4 rounded-2xl font-black uppercase tracking-widest" onclick={saveAndClose}>
					Mentés
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