<script lang="ts">
  import { UserSettingsOutline, ChevronRightOutline } from "flowbite-svelte-icons";

  let adventures = $state([
    { id: 1, name: 'MIK-túra', dist: '2800 m', time: '35040 h' },
    { id: 2, name: 'Kaland #2', dist: '1457 m', time: '1 h 4 m' },
    { id: 3, name: 'Kaland #3', dist: '956 m', time: '24 m' }
  ]);
</script>

<main class="flex flex-col items-center min-h-screen pt-24 pb-24 px-4 bg-[#F5F2EA] font-josefin">
    
    <div class="w-full max-w-md bg-white rounded-2xl shadow-lg overflow-hidden border-4 border-white mb-8">
        <div class="bg-[#775D4D] text-[#F5F2EA] p-3 flex justify-between items-center cursor-pointer">
            <span class="text-lg font-medium tracking-tight">Fedezz fel kalandokat</span>
            <ChevronRightOutline class="w-5 h-5" />
        </div>
        <div class="h-48 bg-gray-200 relative">
            <img 
                alt="Térkép előnézet" 
                class="w-full h-full object-cover"
            />
            <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <div class="w-4 h-4 bg-red-600 rounded-full border-2 border-white shadow-md"></div>
            </div>
        </div>
    </div>

    <div class="w-full max-w-md">
        <h2 class="text-xl font-bold text-black mb-4 tracking-tight">Új kalandok a közeledben</h2>
        
        <div class="grid grid-cols-3 px-4 mb-2 text-[10px] font-bold uppercase text-gray-600 tracking-wider border-b border-gray-300 pb-1">
            <span>Kaland neve</span>
            <span class="text-center">Táv</span>
            <span class="text-right">Átlag teljesítési idő</span>
        </div>

        <div class="space-y-3">
            {#each adventures as adventure (adventure.id)}
                <button 
                    class="w-full bg-[#775D4D] hover:bg-[#634d40] text-[#F5F2EA] rounded-xl p-4 flex justify-between items-center shadow-md transition-all active:scale-[0.98]"
                >
                    <span class="w-1/3 text-left font-bold text-sm">{adventure.name}</span>
                    <span class="w-1/3 text-center text-xs opacity-90">{adventure.dist}</span>
                    <div class="w-1/3 flex justify-end items-center gap-2">
                        <span class="text-xs opacity-90">{adventure.time}</span>
                        <ChevronRightOutline class="w-4 h-4" />
                    </div>
                </button>
            {/each}
        </div>
    </div>
</main>