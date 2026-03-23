<script lang="ts">

	import { onMount, tick } from 'svelte';
	import { base } from '$app/paths';
	import { auth } from '$lib/auth.svelte';

	let title = "";
	let description = "";
	let difficulty = 1;
	let showMapModal = false;
	let activeStationIndex: number | null = null;

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

<main class="flex flex-col p-6 pt-24 min-h-screen bg-[#F5F2EA]">

	<section class="flex flex-col gap-4 mb-8">
		<h1 class="label-city"> Kaland létrehozása
		</h1>
		<label for="adventure-title" class="sr-only">Kaland neve</label>
		<input
			id="adventure-title"
			class="input-city-brown"
			placeholder="Add meg a kaland nevét..."
			bind:value={title}
		/>

		<label for="adventure-desc" class="sr-only">Kaland leírása</label>
		<textarea
			id="adventure-desc"
			class="input-city-brown h-24"
			placeholder="Kaland leírása..."
			bind:value={description}
		></textarea>
	</section>

	<section aria-label="Ellenőrzőpontok">
		{#each stations as station, i (station.id)}
			<article class="mb-8 relative border-b border-[#8D7462]/10 pb-6">
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
					placeholder="Add meg a ponthoz tartozó rejtvényt..."
					bind:value={station.riddleText}
				></textarea>

				<button
					type="button"
					class="btn-secondary"
					on:click|preventDefault|stopPropagation={() => openMap(i)}
				>
          <span class="map-icon-box" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
            </svg>
          </span>
					<span class="map-picker-text">
            {#if station.latitude !== 0}
    					<span class="text-[#2F5D50] font-bold">Helyszín rögzítve:</span><br/>
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

	<footer class="flex flex-col items-center gap-8 mt-4">
		<button
			class="w-14 h-14 bg-[#8D7462] rounded-xl flex items-center justify-center shadow-lg text-white text-3xl hover:bg-[#775D4D] transition-colors"
			on:click={addStation}
			aria-label="Új checkpoint hozzáadása"
		>
			+
		</button>

		<button class="btn-post w-full max-w-xs" on:click={handleSubmit}>
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