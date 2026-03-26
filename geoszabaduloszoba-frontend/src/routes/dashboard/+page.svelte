<script lang="ts">
	import { browser } from "$app/environment";
  import { ChevronRightOutline } from "flowbite-svelte-icons";
  import { auth } from '$lib/auth.svelte';

  import 'leaflet/dist/leaflet.css';

  let mapElement: HTMLElement | undefined = $state(undefined);
  let map: L.Map | undefined = undefined;

  let adventures = $state<any[]>([]);
  const userPos = { lat: 46.0754, lon: 18.2205 };

  async function fetchNearbyAdventures() {
      try {
          const res = await fetch(`http://localhost:8080/api/dashboard?lat=${userPos.lat}&lon=${userPos.lon}`, {
              headers: { 'Authorization': `Bearer ${auth.token}` }
          });
          if (res.ok) {
              adventures = await res.json();
          }
      } catch (e) {
          console.error("Hiba a közeli kalandok betöltésekor:", e);
      }
  }

  $effect(() => {
      if (browser && mapElement && !map) {
          import('leaflet').then((L) => {
              map = L.map(mapElement!).setView([userPos.lat, userPos.lon], 16);
              L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

              const userIcon = L.divIcon({
                  className: 'custom-div-icon',
                  html: `<div class="w-4 h-4 bg-[#2F5D50] rounded-full border-2 border-white shadow-lg"></div>`,
                  iconSize: [16, 16],
                  iconAnchor: [8, 8]
              });
              L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map);

              fetchNearbyAdventures().then(() => {
                  adventures.forEach(adv => {
                      const cityIcon = L.divIcon({
                          className: 'custom-div-icon',
                          html: `<div class="w-3 h-3 bg-red-600 rounded-full border-2 border-white shadow-md"></div>`,
                          iconSize: [12, 12],
                          iconAnchor: [6, 6]
                      });
                      L.marker([adv.advLat, adv.advLon], { icon: cityIcon }).addTo(map!);
                  });
              });
          });
      }
  });
</script>

<main class="flex flex-col items-center min-h-screen pt-6 pb-24 px-4 bg-[#F5F2EA] font-josefin">
    
    <div class="w-full max-w-md bg-white rounded-2xl shadow-lg overflow-hidden border-4 border-white mb-8">
        <div class="bg-[#775D4D] text-[#F5F2EA] p-3 flex justify-between items-center cursor-pointer">
            <span class="text-cream-city">Fedezz fel kalandokat</span>
            <ChevronRightOutline class="w-5 h-5" />
        </div>
        
        <div bind:this={mapElement} class="h-64 w-full z-10"></div>
    </div>

    <div class="w-full max-w-md">
        <h2 class="text-black-city">Új kalandok a közeledben</h2>
        
        <div class="px-4 mb-2 text-[10px] border-b border-gray-300 pb-1">
            <span class="text-right text-grey-city">Kaland neve</span>
            <span class="text-center text-grey-city">Teljesítési idő</span>
            <span class="text-right text-grey-city">Táv</span>
        </div>

        <div class="space-y-3">
            {#each adventures as adventure (adventure.id)}
                <button 
                    class="adventure-card"
                >
                    <span class="text-left text-cream-city">{adventure.title}</span>
                    <span class="text-center text-cream-city">{adventure.averageTime} p</span>
                    <span class="text-right text-cream-city">{adventure.distanceInMeters} m</span>
                    <ChevronRightOutline class="w-4 h-4" />
                </button>
            {/each}
        </div>
    </div>
</main>