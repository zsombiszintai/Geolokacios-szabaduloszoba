<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';

	let abandonedAdventures = $state<any[]>([]);
	let loading = $state(true);

	let searchTerm = $state("");

	let filteredAdventures = $derived(
		abandonedAdventures.filter(a =>
			(a.title || "").toLowerCase().includes(searchTerm.toLowerCase())
		)
	);

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

<main class="flex flex-col p-6 pt-24 pb-20 min-h-screen bg-[#F5F2EA]">

	<header class="mb-10">
		<h1 class="text-3xl font-black text-[#2F5D50] uppercase tracking-[0.2em] leading-none mb-2">
			Félbehagyott<br/>Kalandok
		</h1>
		<div class="w-12 h-1 bg-[#8D7462] mb-4"></div>
	</header>

	<div class="relative mb-10 max-w-md">
    <span class="absolute left-4 top-1/2 -translate-y-1/2 opacity-30">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2F5D50" stroke-width="3">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
      </svg>
    </span>
		<input
			type="text"
			placeholder="Kaland keresése..."
			class="w-full h-12 pl-12 pr-4 bg-white/80 rounded-xl border-b-4 border-[#2F5D50]/20 outline-none focus:border-[#2F5D50] transition-all text-[#2F5D50] font-bold"
			bind:value={searchTerm}
		/>
	</div>

	<section class="max-w-md">
		<h2 class="label-city mb-6">Mentett állások</h2>

		<div class="space-y-8">
			{#if loading}
				<div class="text-left py-4 italic opacity-50 text-[#2F5D50]">Adatok lehívása...</div>
			{:else if filteredAdventures.length === 0}
				<div class="bg-white/30 p-6 rounded-xl border-2 border-dashed border-[#2F5D50]/10">
					<p class="text-[#2F5D50] opacity-50 italic">Nincs ilyen mentésed.</p>
				</div>
			{:else}
				{#each filteredAdventures as item}
					<article class="bg-city-brown/90 p-6 rounded-xl border-2 border-[#F5F2EA] shadow-[8px_8px_0px_0px_rgba(47,93,80,0.1)] flex flex-col items-start transition-transform active:scale-[0.98]">

        <span class="text-[10px] font-black bg-[#8D7462] text-[#F5F2EA] px-2 py-1 rounded mb-4 uppercase tracking-wider border border-[#F5F2EA]/20">
          Mentett állás
        </span>

						<h3 class="text-2xl font-bold text-[#F5F2EA] leading-tight mb-1 text-left">
							{item.title || 'Ismeretlen kaland'}
						</h3>

						<p class="text-sm text-[#F5F2EA] opacity-80 mb-6 font-medium text-left">
							{item.lastStationSeq}. állomásnál tartasz
						</p>

						<button
							onclick={() => goto(`/game?id=${item.adventureId}&station=${item.lastStationId}`)}
							class="w-full bg-city-green text-[#F5F2EA] py-4 rounded-lg font-black uppercase text-sm tracking-widest shadow-md hover:bg-[#005c34] transition-colors"
						>
							Folytatás
						</button>
					</article>
				{/each}
			{/if}
		</div>
	</section>

	<footer class="mt-16 text-left pb-10">
		<button
			onclick={() => goto('/adventures')}
			class="group flex items-center gap-2 text-[#2F5D50] text-xs font-black uppercase tracking-widest"
		>
			<span class="border-b-2 border-[#2F5D50] pb-1">Összes kaland</span>
			<span class="group-hover:translate-x-1 transition-transform">→</span>
		</button>
	</footer>

</main>

<style>
    .label-city {
        @apply text-[11px] font-black uppercase tracking-[0.3em] text-[#2F5D50] opacity-40;
    }

    :global(a) {
        text-decoration: none;
        color: inherit;
    }
</style>