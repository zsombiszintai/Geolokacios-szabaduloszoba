<script lang="ts">
	import { browser } from "$app/environment";
  import { ChevronRightOutline } from "flowbite-svelte-icons";

  import 'leaflet/dist/leaflet.css';

  let mapElement: HTMLElement | undefined = $state(undefined);
  let map: L.Map | undefined = undefined;

    $effect(() => {
    if (browser && mapElement && !map) {
      import('leaflet').then((L) => {
        map = L.map(mapElement!).setView([46.0754, 18.2205], 16);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '© OpenStreetMap'
        }).addTo(map);

                const cityIcon = L.divIcon({
                    className: 'custom-div-icon',
                    html: `<div class="w-4 h-4 bg-red-600 rounded-full border-2 border-white shadow-lg"></div>`,
                    iconSize: [16, 16],
                    iconAnchor: [8, 8]
                 });

                L.marker([46.0754, 18.2205], { icon: cityIcon }).addTo(map);
                });
            }
        });

  let adventures = $state([
    { id: 1, name: 'MIK-túra', dist: '2800 m', time: '35040 h' },
    { id: 2, name: 'Kaland #2', dist: '1457 m', time: '1 h 4 m' },
    { id: 3, name: 'Kaland #3', dist: '956 m', time: '24 m' }
  ]);
</script>

<main class="flex flex-col items-center min-h-screen pt-24 pb-24 px-4 bg-[#F5F2EA] font-josefin">
    
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
            <span class="text-center text-grey-city">Táv</span>
            <span class="text-right text-grey-city">Átlag teljesítési idő</span>
        </div>

        <div class="space-y-3">
            {#each adventures as adventure (adventure.id)}
                <button 
                    class="adventure-card"
                >
                    <span class="text-left text-cream-city">{adventure.name}</span>
                    <span class="text-center text-cream-city">{adventure.dist}</span>
                    <div>
                        <span class="text-right text-cream-city">{adventure.time}</span>
                        <ChevronRightOutline class="w-4 h-4" />
                    </div>
                </button>
            {/each}
        </div>
    </div>
</main>