<script lang="ts">
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { auth } from '$lib/auth.svelte';
	import 'leaflet/dist/leaflet.css';
	import { MapPinSolid } from 'flowbite-svelte-icons';

	let mapElement: HTMLElement | undefined = $state(undefined);
	let map: L.Map | undefined = undefined;
	let userPos = { lat: 46.0754, lon: 18.2205 };

	async function loadMapData(L: any) {
		map = L.map(mapElement!).setView([userPos.lat, userPos.lon], 14);

		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '© OpenStreetMap'
		}).addTo(map);

		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition((pos) => {
				userPos = { lat: pos.coords.latitude, lon: pos.coords.longitude };
				if (map) {
					map.setView([userPos.lat, userPos.lon], 14);

					const userIcon = L.divIcon({
						className: 'custom-user-marker',
						html: `
              <div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
                <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg flex items-center justify-center">
                  <div class="w-1.5 h-1.5 bg-white rounded-full"></div>
                </div>
              </div>`,
						iconSize: [32, 32],
						iconAnchor: [16, 16]
					});

					L.marker([userPos.lat, userPos.lon], { icon: userIcon })
						.addTo(map)
						.bindPopup("<b>Te itt vagy</b>");
				}
			});
		}

		try {
			const res = await fetch(`http://localhost:8080/api/adventures/map?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			const adventures = await res.json();

			adventures.forEach((adv: any) => {
				if (adv.advLat && adv.advLon) {
					const adventureIcon = L.divIcon({
						className: 'custom-adventure-icon',
						html: `
              <div class="text-red-600 drop-shadow-xl hover:scale-110 transition-transform">
                <svg class="w-10 h-10" fill="currentColor" viewBox="0 0 24 24">
                  <path fill-rule="evenodd" d="M11.54 22.351l.07.04.028.016a.76.76 0 00.723 0l.028-.015.071-.041a16.975 16.975 0 001.155-1.122c1.112-1.158 2.502-2.823 3.48-5.076 1.077-2.479 1.444-4.733 1.444-6.382 0-4.667-3.806-8.45-8.5-8.45s-8.5 3.783-8.5 8.45c0 1.649.367 3.903 1.445 6.382.977 2.253 2.367 3.918 3.479 5.076a16.994 16.994 0 001.156 1.122zM12 13.25a3.25 3.25 0 100-6.5 3.25 3.25 0 000 6.5z" clip-rule="evenodd" />
                </svg>
              </div>`,
						iconSize: [40, 40],
						iconAnchor: [20, 40]
					});

					L.marker([adv.advLat, adv.advLon], { icon: adventureIcon })
						.addTo(map!)
						.bindPopup(`
              <div class="p-1 font-josefin">
                <p class="font-bold text-[#2F5D50] text-sm">${adv.title}</p>
                <p class="text-[10px] text-gray-500">${adv.distanceInMeters} m távolságra</p>
              </div>
            `);
				}
			});
		} catch (e) {
			console.error("Hiba a térképadatok betöltésekor:", e);
		}
	}

	$effect(() => {
		if (browser && mapElement && !map) {
			import('leaflet').then((L) => loadMapData(L));
		}
	});
</script>

<div class="fixed inset-0 w-full h-full flex flex-col">
	<header class="bg-[#2F5D50] text-white p-4 flex items-center z-[1000]">
		<a href="/dashboard" class="mr-4">
			<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5m7 7-7-7 7-7"/></svg>
		</a>
		<h1 class="italic text-xl">Felfedezés</h1>
	</header>

	<div bind:this={mapElement} class="flex-1 w-full z-10"></div>

</div>

<style>
    :global(.leaflet-div-icon) {
    @apply bg-transparent border-none;
    }

    :global(.leaflet-popup-content-wrapper) {
        @apply bg-[#F5F2EA] text-[#775D4D] rounded-lg border-none;

    }
</style>