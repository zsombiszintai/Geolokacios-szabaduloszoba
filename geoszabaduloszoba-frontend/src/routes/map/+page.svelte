<script lang="ts">
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { auth } from '$lib/auth.svelte';
	import 'leaflet/dist/leaflet.css';
	import { goto } from '$app/navigation';

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
			goto('/list/${searchQuery}')
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

	async function loadMapData(L: any) {
		if (!mapElement) return;
		map = L.map(mapElement).setView([userPos.lat, userPos.lon], 14);

		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: '© OpenStreetMap'
		}).addTo(map);

		const userIcon = L.divIcon({
			className: 'custom-user-marker',
			html: `<div class="relative flex items-center justify-center">
              <div class="absolute w-8 h-8 bg-blue-500 rounded-full opacity-30 animate-ping"></div>
              <div class="relative w-5 h-5 bg-blue-600 rounded-full border-2 border-white shadow-lg"></div>
            </div>`,
			iconSize: [32, 32],
			iconAnchor: [16, 16]
		});

		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition((pos) => {
				userPos = { lat: pos.coords.latitude, lon: pos.coords.longitude };
				if (map) {
					map.setView([userPos.lat, userPos.lon], 14);
					L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map).bindPopup("Te itt vagy");
				}
			}, () => {
				L.marker([userPos.lat, userPos.lon], { icon: userIcon }).addTo(map!);
			});
		}

		try {
			const res = await fetch(`http://localhost:8080/adventures/map?lat=${userPos.lat}&lon=${userPos.lon}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				const adventures = await res.json();
				adventures.forEach((adv: any) => {
					if (adv.advLat && adv.advLon) {
						L.marker([adv.advLat, adv.advLon]).addTo(map!)
							.bindPopup(`<b>${adv.title}</b><br><a href="/adventures/${adv.id}">Megtekintés</a>`);
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
	<header class="bg-[#2F5D50] text-white p-4 flex items-center shrink-0 z-[1200] shadow-md">
		<a href="/dashboard" class="mr-4 text-white">
			<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5m7 7-7-7 7-7"/></svg>
		</a>
		<h1 class="italic text-xl font-medium">Felfedezés</h1>
	</header>

	<section class="absolute top-20 left-0 right-0 px-6 z-[1100] pointer-events-none">
		<form
			onsubmit={handleSearch}
			class="max-w-md mx-auto w-full pointer-events-auto bg-[#F5F2EA] rounded-3xl shadow-2xl border-2 border-[#2F5D50]/20 transition-all duration-300 ease-in-out flex flex-col overflow-hidden"
			style="height: {isExpanded ? 'auto' : '48px'}; max-height: 400px;"
		>
			<div class="flex items-center px-4 h-12 shrink-0 gap-2">
				<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2F5D50" stroke-width="2.5" class="opacity-50">
					<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
				</svg>
				<input
					type="search"
					bind:value={searchQuery}
					onfocus={() => isExpanded = true}
					placeholder="Keresés..."
					class="flex-1 bg-transparent border-none outline-none text-[#2F5D50] py-2 font-medium placeholder:text-[#2F5D50]/40"
				/>
				{#if searchQuery}
					<button type="button" onclick={() => {searchQuery = ""; searchResults = [];}} class="text-gray-400">✕</button>
				{/if}
			</div>

			{#if isExpanded}
				<nav class="flex gap-2 px-4 pb-3 overflow-x-auto no-scrollbar shrink-0">
					{#each ['adventure', 'user', 'list'] as type}
						<button
							type="button"
							onclick={() => { searchType = type; performSearch(); }}
							class="px-3 py-1 rounded-full text-[10px] font-bold uppercase transition-all
              {searchType === type ? 'bg-[#2F5D50] text-white' : 'bg-[#E8E4D8] text-[#2F5D50]'}"
						>
							{type === 'adventure' ? '🗺️ Kaland' : type === 'user' ? '👤 Játékos' : '📜 Lista'}
						</button>
					{/each}
				</nav>

				{#if searchResults.length > 0 || isSearching}
					<ul class="border-t border-[#2F5D50]/10 overflow-y-auto no-scrollbar bg-white/50 flex-1">
						{#if isSearching}
							<li class="px-4 py-3 italic text-sm text-[#2F5D50]/60 text-center">Keresés...</li>
						{:else}
							{#each searchResults as res}
								<li>
									<button
										type="button"
										onclick={() => handleResultClick(res)}
										class="w-full text-left px-4 py-3 hover:bg-[#2F5D50]/5 border-b border-[#2F5D50]/5 flex justify-between items-center group"
									>
										<div>
											<span class="block font-bold text-[#2F5D50] group-hover:underline">{res.title}</span>
											<span class="text-[10px] text-gray-500">
                        {res.subtitle || (res.distanceInMeters ? `${res.distanceInMeters} m • ${res.averageTime} perc` : 'Kalandor')}
                      </span>
										</div>
										<span class="text-xs opacity-40">❯</span>
									</button>
								</li>
							{/each}
						{/if}
					</ul>
				{/if}
			{/if}
		</form>
	</section>

	<main bind:this={mapElement} class="flex-1 w-full z-0"></main>
</div>