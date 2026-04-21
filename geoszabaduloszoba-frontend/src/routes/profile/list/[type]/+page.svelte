<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { page } from '$app/state';
	import { onMount } from 'svelte';

	let items = $state<any[]>([]);
	let loading = $state(true);

	const type = $derived(page.params.type);

	const titles: Record<string, string> = {
		completed: 'Lejátszott kalandok',
		abandoned: 'Félbehagyott kalandok',
		owned: 'Saját kalandok',
		rated: 'Értékelt kalandok',
		reviews: 'Vélemények'
	};

	async function fetchListData() {
		loading = true;
		try {
			const res = await fetch(`http://localhost:8080/profile/list?type=${type}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				items = await res.json();
			}
		} catch (err) {
			console.error("Hiba a lista betöltésekor:", err);
		} finally {
			loading = false;
		}
	}

	onMount(fetchListData);
</script>

<main class="min-h-screen bg-[#F5F2EA] font-sans pb-24">
	<header class="header-city h-20">
		<a href="/profile" class="header-icon-btn">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
		</a>
		<h1 class="header-city-title text-sm">{titles[type] || 'Lista'}</h1>
		<div class="w-8"></div>
	</header>

	<div class="pt-24 px-6 space-y-4">
		{#if loading}
			<div class="text-center py-10 text-[#2F5D50] italic animate-pulse">Betöltés...</div>
		{:else if items.length === 0}
			<div class="text-center py-10 text-gray-500 italic">Még nincs itt semmi látnivaló.</div>
		{:else}
			{#each items as adventure}
				<div class="adventure-card flex-col items-start gap-2">
					<div class="flex justify-between w-full">
						<span class="font-bold text-lg">{adventure.title}</span>
						<span class="text-sm opacity-80">{adventure.difficulty || ''}</span>
					</div>
					<p class="text-sm opacity-90 line-clamp-2">{adventure.description || ''}</p>
				</div>
			{/each}
		{/if}
	</div>
</main>