<script lang="ts">
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { auth } from '$lib/auth.svelte';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';
	import {MapPinSolid} from 'flowbite-svelte-icons';

	let mapElement: HTMLElement | undefined = $state(undefined);
	let map: L.Map | undefined = undefined;
	let userPos = { lat: 46.0754, lon: 18.2205 };

	let searchQuery = $state("");
	let searchType = $state("adventure");
	let isExpanded = $state(false);

	let searchResults = $state<any[]>([]);
	let isSearching = $state(false);

	function handleSearch(e?: Event) {
		e?.preventDefault();
		if (!searchQuery.trim()) {
			isExpanded = false;
			return;
		}
		if (searchType === "user") {
			goto(`/profile/user/${searchQuery}`);
		} else if (searchType === "adventure") {
			goto(`/adventures?search=${searchQuery}`);
		}else {
			goto(`/list/${searchQuery}`);
		}
	}

	async function performSearch() {
		if (searchQuery.length < 2) {
			searchResults = [];
			return;
		}
		isSearching = true;
		try {
			const res = await fetch(
				`http://localhost:8080/search?q=${searchQuery}&type=${searchType}&lat=${userPos.lat}&lon=${userPos.lon}`,
				{ headers: { 'Authorization': `Bearer ${auth.token}` } }
			);
			if (res.ok) {
				searchResults = await res.json();
			}
		} catch (err) {
			console.error("Keresési hiba:", err);
		} finally {
			isSearching = false;
		}
	}

	function handleResultClick(res: any) {
		if (searchType === 'user' || res.type === 'USER') {
			goto(`/profile/user/${res.title}`);
		} else {
			if (map && res.advLat && res.advLon) {
				map.setView([res.advLat, res.advLon], 16);
				isExpanded = false;
			} else {
				goto(`/adventures/${res.id}`);
			}
		}
	}

	function createAdventureIcon() {
		return L.divIcon({
			className: 'custom-div-icon',
			html: `<div class="text-red-600 drop-shadow-lg scale-125">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor" stroke="white" stroke-width="1">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
             </div>`,
			iconSize: [32, 32],
			iconAnchor: [16, 32]
		});
	}

	function createUserIcon() {
		return L.divIcon({
			className: 'custom-div-icon',
			html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-8 h-8 bg-[#2F5D50]/30 rounded-full animate-ping"></div>
                <div class="w-5 h-5 bg-[#2F5D50] rounded-full border-2 border-white shadow-lg z-10"></div>
             </div>`,
			iconSize: [32, 32],
			iconAnchor: [16, 16]
		});
	}

	async function loadMapData(L: any) {
		if (!mapElement) return;
		map = L.map(mapElement, { zoomControl: false }).setView([userPos.lat, userPos.lon], 15);

		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '&copy; OpenStreetMap'
		}).addTo(map);

		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition((pos) => {
				userPos = { lat: pos.coords.latitude, lon: pos.coords.longitude };
				if (map) {
					map.setView([userPos.lat, userPos.lon], 15);
					L.marker([userPos.lat, userPos.lon], { icon: createUserIcon() }).addTo(map).bindPopup("Itt vagy");
				}
			}, () => {
				L.marker([userPos.lat, userPos.lon], { icon: createUserIcon() }).addTo(map!);
			});
		}

		try {
			const res = await fetch(`http://localhost:8080/api/adventures/map?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				const adventures = await res.json();
				adventures.forEach((adv: any) => {
					if (adv.advLat && adv.advLon) {
						const marker = L.marker([adv.advLat, adv.advLon], { icon: createAdventureIcon() }).addTo(map!);

						marker.bindPopup(`
              <div class="city-popup font-josefin">
                <h3 class="text-lg font-black text-[#2F5D50] mb-1">${adv.title}</h3>
                <div class="stats flex justify-center gap-4 text-xs font-bold text-[#8D7462] mb-4">
                  <span>${adv.distanceInMeters} m</span>
                </div>
                <a href="/adventures/${adv.id}"
                   class="inline-block w-full bg-[#2F5D50] text-white py-3 rounded-xl font-black text-[10px] uppercase tracking-widest text-center no-underline shadow-lg active:scale-95 transition-transform">
                   Megtekintés
                </a>
              </div>
            `, {
							closeButton: false,
							className: 'cityscape-popup'
						});
					}
				});
			}
		} catch (e) { console.error(e); }
	}

	$effect(() => {
		if (browser && mapElement && !map) {
			import('leaflet').then((L) => loadMapData(L));
		}
	});

	$effect(() => {
		if (searchQuery.length >= 2) {
			const timer = setTimeout(performSearch, 300);
			return () => clearTimeout(timer);
		} else {
			searchResults = [];
		}
	});
</script>

<div class="fixed inset-0 w-full h-full flex flex-col overflow-hidden bg-[#F5F2EA]">

	<section class="absolute top-20 left-0 right-0 px-6 z-[1100] pointer-events-none">
		<form
			onsubmit={handleSearch}
			class="max-w-md mx-auto w-full pointer-events-auto bg-white/80 rounded-xl border-b-4 border-[#2F5D50]/20 transition-all duration-300 ease-in-out flex flex-col overflow-hidden focus-within:border-[#2F5D50]"
			style="height: {isExpanded ? 'auto' : '48px'}; max-height: 500px;"
		>
			<div class="flex items-center px-4 h-12 shrink-0 gap-3">
				<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2F5D50" stroke-width="3" class="opacity-40">
					<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
				</svg>

				<input
					type="search"
					bind:value={searchQuery}
					onfocus={() => isExpanded = true}
					placeholder="Keresés..."
					class="flex-1 bg-transparent border-none outline-none text-[#2F5D50] py-2 font-bold placeholder:text-[#2F5D50]/40"
				/>

				{#if searchQuery}
					<button
						type="button"
						onclick={() => {searchQuery = ""; searchResults = [];}}
						class="text-[#2F5D50] opacity-50 hover:opacity-100 font-bold text-xl"
					>
					</button>
				{/if}
			</div>

			{#if isExpanded}
				<nav class="flex gap-2 px-4 pb-3 overflow-x-auto no-scrollbar shrink-0 border-t border-[#2F5D50]/5 pt-3">
					{#each ['adventure', 'user', 'list'] as type}
						<button
							type="button"
							onclick={() => { searchType = type; performSearch(); }}
							class="px-4 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all
                {searchType === type ? 'bg-[#2F5D50] text-white' : 'bg-[#2F5D50]/10 text-[#2F5D50]'}"
						>
							{type === 'adventure' ? 'Kaland' : type === 'user' ? 'Játékos' : 'Lista'}
						</button>
					{/each}
				</nav>

				{#if searchResults.length > 0 || isSearching}
					<ul class="border-t-2 border-[#2F5D50]/10 overflow-y-auto no-scrollbar bg-white/30 flex-1">
						{#if isSearching}
							<li class="px-4 py-6 italic text-sm text-[#2F5D50]/60 text-center font-bold">
								Keresés...
							</li>
						{:else}
							{#each searchResults as res}
								<li>
									<button
										type="button"
										onclick={() => handleResultClick(res)}
										class="w-full text-left px-4 py-3 hover:bg-white/60 border-b border-[#2F5D50]/5 flex justify-between items-center group transition-colors"
									>
										<div>
											<span class="block font-black text-[#2F5D50] group-hover:text-black">{res.title}</span>
											<span class="text-[11px] font-medium text-[#2F5D50]/60">
                    {res.description || (res.distanceInMeters ? `${res.distanceInMeters} m • ${res.averageTime} perc` : "")}
                  </span>
										</div>
										<span class="text-[#2F5D50] opacity-30 group-hover:opacity-100 font-black">❯</span>
									</button>
								</li>
							{/each}
						{/if}
					</ul>
				{/if}
			{/if}
		</form>
	</section>

	<main bind:this={mapElement} class="flex-1 w-full z-0 saturate-[1.2] contrast-[1.05]"></main>
</div>
<style>
    :global(.cityscape-popup .leaflet-popup-content-wrapper) {
        background: #F5F2EA !important;
        color: #2F5D50 !important;
        border-radius: 1.5rem !important;
        padding: 0 !important;
        overflow: hidden;
        box-shadow: 0 10px 25px rgba(0,0,0,0.2) !important;
    }

    :global(.cityscape-popup .leaflet-popup-tip) {
        background: #F5F2EA !important;
    }

    :global(.city-popup) {
        padding: 20px;
        font-family: 'Josefin Sans', sans-serif;
        text-align: center;
    }

    :global(.city-popup h3) {
        margin: 0 0 10px 0;
        font-weight: 900;
        font-size: 1.1rem;
        color: #2F5D50;
    }

    :global(.city-popup .stats) {
        display: flex;
        justify-content: center;
        gap: 15px;
        font-size: 0.8rem;
        font-weight: 700;
        margin-bottom: 15px;
        opacity: 0.7;
    }

    :global(.city-popup a) {
        display: block;
        background: #2F5D50;
        color: white !important;
        padding: 10px 20px;
        border-radius: 12px;
        text-decoration: none;
        font-weight: 900;
        text-transform: uppercase;
        font-size: 0.75rem;
        letter-spacing: 0.1em;
    }
</style>
