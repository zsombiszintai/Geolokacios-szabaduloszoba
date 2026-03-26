<script lang="ts">

	import { onMount, tick } from 'svelte';
	import { ArrowRightToBracketOutline } from 'flowbite-svelte-icons';
	import { auth } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';

	let title = "";
	let description = "";

	let showMapModal = false;
	let activeStationIndex: number | null = null;
	let errorMessage = "";

	const difficultyLabels = ["Könnyű", "Közepes", "Nehéz"];
	let difficulty = 1;

	let L: any;
	let map: any;
	let tempMarker: any;

	interface Station {
		id?: string;
		latitude: number;
		longitude: number;
		riddleText: string;
	}

	let stations: Station[] = [
		{ id: crypto.randomUUID(), latitude: 46.076, longitude: 18.228, riddleText: "" }
	];

	async function openMap(index: number) {
		console.log("Modál nyitása erre az indexre:", index);
		activeStationIndex = index;
		showMapModal = true;
		await tick();
		initMap();
	}

	async function initMap() {
		if (!L) {
			L = await import('leaflet');
			import('leaflet/dist/leaflet.css');

			delete L.Icon.Default.prototype._getIconUrl;
			L.Icon.Default.mergeOptions({
				iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
				iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
				shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
			});
		}

		if (map) {
			map.remove();
		}

		const startCoords: [number, number] = [46.076, 18.228];
		const currentStation = stations[activeStationIndex!];
		const initialView = currentStation.latitude !== 0
			? [currentStation.latitude, currentStation.longitude] as [number, number]
			: startCoords;

		map = L.map('map-selector').setView(initialView, 13);

		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

		tempMarker = null;

		if (currentStation.latitude !== 0) {
			tempMarker = L.marker(initialView).addTo(map);
		}

		map.on('click', (e: any) => {
			const { lat, lng } = e.latlng;
			if (tempMarker) tempMarker.setLatLng(e.latlng);
			else tempMarker = L.marker(e.latlng).addTo(map);

			if (activeStationIndex !== null) {
				stations[activeStationIndex].latitude = lat;
				stations[activeStationIndex].longitude = lng;
			}
		});

		setTimeout(() => {
			if (map) {
				map.invalidateSize();
			}
		}, 100);
	}

	function saveAndClose() {
		if (map) {
			map.remove();
			map = null;
			tempMarker = null;
		}
		showMapModal = false;
	}

	function addStation() {
		stations = [...stations, {
			id: crypto.randomUUID(),
			latitude: 0,
			longitude: 0,
			riddleText: ""
		}];
	}

	function removeStation(index: number) {
		stations = stations.filter((_, i) => i !== index);
	}

	async function handleSubmit() {

		const adventureData = { title, description, difficulty, stations };

		if (!title || title.trim() === "") {
			errorMessage = "Adj nevet a kalandnak!";
			return;
		}

		if (!description || description.trim() === "") {
			errorMessage = "A leírás nem maradhat üresen!";
			return;
		}

		if (stations.length === 0) {
			errorMessage = "Legalább egy állomást létre kell hoznod a térképen!";
			return;
		}

		errorMessage = "";

		try {
			const response = await fetch('http://localhost:8080/api/create-adventure', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify(adventureData)
			});

			if (response.ok) {
				alert("Sikeres mentés!");
			} else {
				console.error("Szerver hiba:", response.status);
				console.log("Küldött token:", auth.token);
			}
		} catch (err) {
			console.error("Hálózati hiba:", err);
		}
	}
</script>

<main class="flex flex-col p-6 pt-6 min-h-screen bg-[#F5F2EA]">


	<nav class="fixed top-[64px] left-0 w-full z-[55] px-4 py-2 bg-[#F5F2EA]/80 backdrop-blur-sm mb-8">
		<button
			type="button"
			class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group"
			on:click={() => goto('/created-adventures')}
		>
			<div class="p-2 rounded-full bg-[#8D7462]/10 group-hover:bg-[#2F5D50]/10">
				<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l-5-5 5-5M13.8 12H5"/>
				</svg>
			</div>
			<span class="text-[10px] font-black uppercase tracking-tighter">Kilépés a szerkesztőből</span>
		</button>
	</nav>

	{#if errorMessage}
		<div class="alert-error-city">{errorMessage}</div>
	{/if}

	<section class="flex flex-col gap-2 mb-1 mt-4">
		<h1 class="label-city"> Kaland létrehozása
		</h1>
		<label for="adventure-title" class="sr-only">Kaland neve</label>
		<input
			id="adventure-title"
			class="input-city-brown"
			placeholder="Kaland neve..."
			bind:value={title}
		/>

		<label for="adventure-desc" class="sr-only">Kaland leírása</label>
		<textarea
			id="adventure-desc"
			class="input-city-brown h-24"
			placeholder="Miről szól ez a kaland? Írj egy kedvcsinálót..."
			bind:value={description}
		></textarea>

		<fieldset class="w-full mt-1 mb-4 border-none p-0">
			<legend class="label-city block w-full text-center mb-4">
				Nehézség: <span class="text-[#2F5D50] font-black uppercase tracking-widest">{difficultyLabels[difficulty]}</span>
			</legend>

			<div class="relative w-full flex flex-col items-center">
				<input
					type="range" min="0" max="2" step="1"
					bind:value={difficulty}
					class="slider-city w-full"
					aria-valuemin="0"
					aria-valuemax="2"
					aria-valuenow={difficulty}
				/>
				<div class="flex justify-between w-full mt-3 px-1 text-[10px] font-bold text-city-brown/60 tracking-tighter" aria-hidden="true">
					<span>KÖNNYŰ</span>
					<span>KÖZEPES</span>
					<span>NEHÉZ</span>
				</div>
			</div>
		</fieldset>
	</section>

	<section aria-label="Ellenőrzőpontok">
		{#each stations as station, i (station.id)}
			<article class="mb-2 relative border-b border-[#8D7462]/10 pb-6">
				<header class="flex justify-between items-center mb-2">
					<h2 class="label-city">{i + 1}. checkpoint</h2>
					{#if stations.length > 1}
						<button
							on:click={() => removeStation(i)}
							class="text-red-600 text-xs uppercase font-bold p-1 hover:bg-red-50 rounded"
							aria-label="{i + 1}. checkpoint törlése"
						>
							Törlés
						</button>
					{/if}
				</header>

				<label for="riddle-{i}" class="sr-only">{i + 1}. checkpoint rejtvénye</label>
				<textarea
					id="riddle-{i}"
					class="input-city-brown min-h-[100px] resize-none"
					placeholder="Milyen rejtvény vezessen ide?"
					bind:value={station.riddleText}
				></textarea>

				<button
					type="button"
					class="btn-secondary"
					on:click|preventDefault|stopPropagation={() => openMap(i)}
				>
          <span class="map-icon-box" aria-hidden="true">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="white">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
            </svg>
          </span>
					<span class="map-picker-text">
            {#if station.latitude !== 0}
    					<span class="text-city-green">Helyszín rögzítve:</span><br/>
    					<span class="text-xs opacity-70">
       		 			{station.latitude.toFixed(4)}, {station.longitude.toFixed(4)}
    					</span>
  					{:else}
    					Válaszd ki a pontot a térképen.
  					{/if}
          </span>
				</button>
			</article>
		{/each}
	</section>

	<footer class="flex flex-col items-center gap-4">
		<button
			class="btn-secondary justify-center"
			on:click={addStation}
			aria-label="Új checkpoint hozzáadása"
		>
			+
		</button>

		<button class="btn-primary" on:click={handleSubmit}>
			Poszt
		</button>
	</footer>
</main>

{#if showMapModal}
	<aside class="modal-overlay" role="dialog" aria-labelledby="modal-title">
		<div class="modal-content">
			<h2 id="modal-title" class="sr-only">Térképes helyszín választó</h2>
			<div id="map-selector" class="flex-grow"></div>
			<button class="btn-primary" on:click={saveAndClose}>
				Kiválasztás mentése
			</button>
		</div>
	</aside>
{/if}