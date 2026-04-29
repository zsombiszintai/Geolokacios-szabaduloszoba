<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';

	// Most már egy tömböt várunk
	let abandonedAdventures = $state<any[]>([]);
	let loading = $state(true);

	async function fetchAbandonedAdventures() {
		if (!auth.token) return;

		try {
			const res = await fetch('http://localhost:8080/api/adventures/abandoned-all', {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				abandonedAdventures = await res.json();
			}
		} catch (err) {
			console.error("Hiba a listázáskor:", err);
		} finally {
			loading = false;
		}
	}

	onMount(fetchAbandonedAdventures);
</script>

<main class="min-h-screen bg-[#F5F2EA] p-6 pt-24">
	<h1 class="text-2xl font-black text-[#2F5D50] mb-6 uppercase tracking-widest">Félbehagyott kalandjaid</h1>

	{#if loading}
		<div class="text-center py-10 italic animate-pulse text-[#2F5D50]">Kalandok betöltése...</div>
	{:else if abandonedAdventures.length === 0}
		<div class="text-center py-10 text-gray-500 italic bg-white/50 rounded-2xl border-2 border-dashed border-gray-300">
			Nincs félbehagyott kalandod. Indíts egy újat!
		</div>
	{:else}
		<div class="grid gap-4">
			{#each abandonedAdventures as item}
				<div class="bg-[#2F5D50] text-[#F5F2EA] p-5 rounded-2xl shadow-lg border-l-8 border-[#8D7462] flex flex-col md:flex-row justify-between items-center gap-4">
					<div class="flex-1">
						<div class="flex items-center gap-2 mb-1">
							<span class="text-[10px] bg-[#8D7462] px-2 py-1 rounded text-white font-bold uppercase">Mentett állás</span>
							<span class="text-xs opacity-70 italic">{new Date(item.startedAt).toLocaleDateString('hu-HU')}</span>
						</div>
						<h2 class="text-xl font-bold">{item.title || 'Kaland'}</h2>
						<p class="text-xs opacity-80 mt-1">Legutóbbi állomás azonosítója: {item.lastStationId}</p>
					</div>

					<button
						onclick={() => goto(`/game?id=${item.adventureId}&station=${item.lastStationId}`)}
						class="whitespace-nowrap bg-[#F5F2EA] text-[#2F5D50] px-8 py-3 rounded-xl font-black uppercase text-sm shadow-md hover:bg-white transition-all active:scale-95"
					>
						Folytatás
					</button>
				</div>
			{/each}
		</div>
	{/if}

	<div class="mt-12 text-center">
		<button onclick={() => goto('/adventures')} class="text-[#2F5D50] font-bold border-b-2 border-[#2F5D50] pb-1">
			Böngészés az összes kaland között
		</button>
	</div>
</main>