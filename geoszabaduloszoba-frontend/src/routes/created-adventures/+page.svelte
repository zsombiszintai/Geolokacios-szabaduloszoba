<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';

	interface Adventure {
		id: number;
		title: string;
		createdAt: string;
		status: 'PUBLISHED' | 'DRAFT' | 'ARCHIVED' | 'PENDING';
	}

	let adventures: Adventure[] = [];
	let searchTerm = "";
	let loading = true;

	onMount(async () => {
		try {
			const response = await fetch('http://localhost:8080/api/create-adventure/created-adventures', {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (response.ok) adventures = await response.json();
		} finally {
			loading = false;
		}
	});

	$: filteredAdventures = adventures.filter(a =>
		a.title.toLowerCase().includes(searchTerm.toLowerCase())
	);

	const statusColors = {
		'PUBLISHED': 'bg-green-500',
		'PENDING': 'bg-yellow-500',
		'ARCHIVED': 'bg-red-500'
	};
</script>

<main class="flex flex-col p-6 pt-6 pb-20 min-h-screen bg-city-cream">

	<section class="mb-10 px-1">
		<h2 class="text-sm font-bold text-[#2F5D50] mb-4 uppercase tracking-wider">Új kaland létrehozása</h2>

		<button
			type="button"
			class="w-full flex flex-col items-start gap-2 group"
			on:click={() => goto('/create-adventure')}
		>
			<div class="w-full h-16 bg-[#8D7462] rounded-2xl flex items-center justify-center shadow-lg group-active:scale-[0.98] transition-all">
				<span class="text-white text-4xl font-light leading-none">+</span>
			</div>

			<div class="text-left">
				<p class="text-city-brown leading-tight">
					Készítsd el a saját kalandod...
				</p>
			</div>
		</button>
	</section>

	<div class="relative mb-8">
    <span class="absolute left-4 top-1/2 -translate-y-1/2 opacity-50">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
      </svg>
    </span>
		<input
			type="text"
			placeholder="Keress a kalandjaid között..."
			class="input-city-brown pl-12 mb-0"
			bind:value={searchTerm}
		/>
	</div>

	<section>
		<h2 class="label-city mb-4">Létrehozott kalandjaid</h2>

		<header class="grid grid-cols-[2fr_1fr_1.5fr_1fr] px-4 mb-2 text-[10px] font-bold text-gray-600 uppercase tracking-widest border-b border-gray-300 pb-2">
			<span>Kaland neve</span>
			<span class="text-center">Állapot</span>
			<span class="text-center">Létrehozás</span>
			<span class="text-right">Szerk.</span>
		</header>

		<div class="space-y-3">
			{#if loading}
				<p class="text-center py-10 opacity-50 italic">Kalandok betöltése...</p>
			{:else if filteredAdventures.length === 0}
				<p class="text-center py-10 opacity-50 italic">Nincs talált kaland.</p>
			{:else}
				{#each filteredAdventures as adventure}
					<article class="adventure-card grid grid-cols-[2fr_1fr_1.5fr_1fr] items-center">
						<span class="font-bold truncate pr-2">{adventure.title}</span>

						<div class="flex justify-center">
							<div class="w-4 h-4 rounded-full shadow-inner {statusColors[adventure.status] || 'bg-gray-400'}"
									 title={adventure.status}></div>
						</div>

						<span class="text-[10px] text-center opacity-80">
              {new Date(adventure.createdAt).toLocaleDateString('hu-HU')}
            </span>

						<button class="flex justify-end opacity-80 hover:opacity-100 transition-opacity">
							<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
								<path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
							</svg>
						</button>
					</article>
				{/each}
			{/if}
		</div>
	</section>

</main>