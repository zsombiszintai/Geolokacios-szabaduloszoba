<script lang="ts">
	import { browser } from "$app/environment";
  import { ChevronRightOutline } from "flowbite-svelte-icons";
  import { auth } from '$lib/auth.svelte';

  import 'leaflet/dist/leaflet.css';
  import { goto } from '$app/navigation';

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
              map = L.map(mapElement!, {
                  zoomControl: false
              }).setView([userPos.lat, userPos.lon], 17);

              L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                  attribution: '&copy; OpenStreetMap'
              }).addTo(map);

              const userIcon = L.divIcon({
                  className: 'custom-div-icon',
                  html: `<div class="relative flex items-center justify-center">
                            <div class="absolute w-8 h-8 bg-[#2F5D50]/30 rounded-full animate-ping"></div>
                            <div class="w-5 h-5 bg-[#2F5D50] rounded-full border-2 border-white shadow-lg z-10"></div>
                         </div>`,
                  iconSize: [32, 32],
                  iconAnchor: [16, 16]
              });
              L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map);

              fetchNearbyAdventures().then(() => {
                  adventures.forEach(adv => {
                      const cityIcon = L.divIcon({
                          className: 'custom-div-icon',
                          html: `<div class="text-red-600 drop-shadow-lg scale-125">
                                   <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor" stroke="white" stroke-width="1">
                                     <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                                   </svg>
                                 </div>`,
                          iconSize: [32, 32],
                          iconAnchor: [16, 32]
                      });
                      L.marker([adv.advLat, adv.advLon], { icon: cityIcon }).addTo(map!);
                  });
              });
          });
      }
  });
</script>

<main class="flex flex-col items-center min-h-screen pt-6 pb-24 px-4 bg-[#F5F2EA] font-josefin">

    <section class="w-full max-w-md mx-auto mb-10">
        <header class="flex justify-between items-end mb-4 px-2">
            <div>
                <h1 class="text-3xl font-black text-[#2F5D50] uppercase tracking-tight leading-none">Térkép</h1>
                <div class="w-8 h-1 bg-[#8D7462] mt-2 rounded-full"></div>
            </div>
            <button
              onclick={() => goto(`/map`)}
              class="flex items-center gap-1 text-[10px] font-black text-[#8D7462] uppercase tracking-widest bg-white py-2 px-4 rounded-xl shadow-sm border border-[#2F5D50]/5 active:scale-95 transition-all"
            >
                Teljes nézet <ChevronRightOutline class="w-3 h-3" />
            </button>
        </header>

        <div class="relative w-full rounded-[1.5rem] overflow-hidden shadow-2xl border-8 border-city-cream group">
            <div bind:this={mapElement} class="h-72 w-full z-10"></div>
            <div class="absolute inset-0 pointer-events-none ring-1 ring-inset ring-black/5"></div>
        </div>
    </section>

    <section class="w-full max-w-md mx-auto">
        <h2 class="label-city mb-4 ml-2">Új kalandok a közeledben</h2>

        <header class="grid grid-cols-[2fr_1fr_1fr] px-5 mb-3 text-[10px] font-black text-[#2F5D50] opacity-40 uppercase tracking-[0.2em]">
            <span>Kaland</span>
            <span class="text-center">Idő</span>
            <span class="text-right">Távolság</span>
        </header>

        <div class="space-y-4">
            {#each adventures as adventure (adventure.id)}
                <button
                  class="grid grid-cols-[2fr_1fr_1fr] w-full items-center bg-[#8D7462]/90 p-5 rounded-3xl shadow-xl transition-all active:scale-[0.98] text-left border border-white/10"
                  onclick={() => goto(`/adventures/${adventure.id}`)}
                >
                    <div class="overflow-hidden">
                        <span class="font-black text-white text-lg leading-tight truncate block">{adventure.title}</span>
                    </div>

                    <span class="text-center text-s font-bold text-[#F5F2EA]">{adventure.averageTime} p</span>

                    <div class="text-right">
                        <span class="text-s font-black text-white px-2 py-1 rounded-lg shadow-inner">
                            {adventure.distanceInMeters >= 1000 ? (adventure.distanceInMeters / 1000).toFixed(1) + ' km' : Math.round(adventure.distanceInMeters) + ' m'}
                        </span>
                    </div>
                </button>
            {/each}
        </div>
    </section>
</main>
<style>
    .label-city {
        @apply text-[14px] font-black uppercase tracking-[0.2em] text-[#2F5D50] opacity-80;
    }
</style>