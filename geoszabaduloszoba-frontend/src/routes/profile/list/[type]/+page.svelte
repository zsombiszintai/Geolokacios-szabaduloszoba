<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';

	let items = $state<any[]>([]);
	let loading = $state(true);

	const type = $derived(page.params.type);

	const titles: Record<string, string> = {
		'completed-adventure': 'Lejátszott kalandok',
		'abandoned-adventure': 'Félbehagyott kalandok',
		'created': 'Saját kalandok',
		'rated': 'Értékelt kalandok',
		'reviewed': 'Vélemények',
		'followers': 'Követők',
		'following': 'Követés'
	};

	async function fetchListData() {
		if (!auth.token) return;
		loading = true;
		try {
			const res = await fetch(`http://localhost:8080/profile/list/${type}`, {
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

<main class="min-h-screen bg-[#F5F2EA] font-sans pb-24 px-6 pt-4">
	<button
		type="button"
		class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group mb-8"
		onclick={() => goto(`/profile/user/${auth.username || 'me'}`)}
	>
		<div class="p-2 rounded-full bg-[#8D7462]/10 group-hover:bg-[#2F5D50]/10">
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</div>
		<span class="text-[10px] font-black uppercase tracking-tighter">Vissza a profilra</span>
	</button>

	<h2 class="text-2xl font-bold text-[#2F5D50] mb-6">{titles[type] || 'Lista'}</h2>

	<div class="space-y-4">
		{#if loading}
			<div class="text-center py-10 text-[#2F5D50] italic animate-pulse">Betöltés...</div>
		{:else if items.length === 0}
			<div class="text-center py-10 text-gray-500 italic">Még nincs itt semmi látnivaló.</div>
		{:else}
			{#each items as item}
				<div class="adventure-card flex flex-col items-start gap-2 bg-city-brown p-4 rounded-xl shadow-sm border border-gray-100">
					{#if item.username}
						<button
							class="flex items-center gap-3 w-full text-left"
							onclick={() => goto(`/profile/user/${item.username}`)}
						>
							<div class="w-12 h-12 bg-gray-200 rounded-full flex items-center justify-center overflow-hidden border border-gray-300">
								{#if item.profilePictureUrl}
									<img src={item.profilePictureUrl} alt="avatar" class="w-full h-full object-cover" />
								{:else}
									<span class="text-city-brown text-xl">t</span>
								{/if}
							</div>
							<div class="flex flex-col">
								<span class="font-bold text-black">@{item.username}</span>
								<span class="text-xs text-city-cream truncate max-w-[200px]">{item.description || 'Nincs leírás'}</span>
							</div>
							<span class="ml-auto text-gray-400">❯</span>
						</button>
					{:else}
						<div class="flex justify-between w-full">
							<span class="font-bold text-lg text-black">{item.title}</span>
							{#if item.difficulty}
                <span class="text-xs font-bold px-2 py-1 bg-[#2F5D50] text-city-cream rounded-md">
                  {item.difficulty}
                </span>
							{/if}
						</div>
						<p class="text-sm text-city-cream line-clamp-2">{item.description || 'Nincs leírás'}</p>
					{/if}
				</div>
			{/each}
		{/if}
	</div>
</main>