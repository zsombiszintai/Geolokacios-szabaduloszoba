<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { ChevronRightOutline, StarSolid } from 'flowbite-svelte-icons';

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

<main class="min-h-screen bg-[#F5F2EA] font-josefin pb-24 px-6 pt-6">
	<button
		type="button"
		class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group mb-8"
		onclick={() => window.history.back()}
	>
		<div class="p-2 rounded-xl bg-white shadow-sm group-hover:shadow-md transition-all">
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</div>
		<span class="text-[11px] font-black uppercase tracking-widest">Vissza a profilra</span>
	</button>

	<header class="mb-8">
		<h2 class="text-3xl font-black text-[#2F5D50] leading-none uppercase tracking-tighter">
			{titles[type] || 'Lista'}
		</h2>
		<div class="w-12 h-1.5 bg-[#8D7462] mt-4 rounded-full"></div>
	</header>

	<div class="space-y-4">
		{#if loading}
			<div class="text-center py-20">
				<p class="text-[#2F5D50] opacity-40 font-bold animate-pulse uppercase tracking-widest text-xs">Adatok gyűjtése...</p>
			</div>
		{:else if items.length === 0}
			<div class="bg-white/40 border-2 border-dashed border-[#2F5D50]/10 rounded-3xl p-12 text-center">
				<p class="text-[#2F5D50] opacity-40 font-bold italic">Itt még nincs semmi látnivaló.</p>
			</div>
		{:else}
			{#each items as item}
				<div class="bg-white p-5 rounded-3xl shadow-sm border border-[#2F5D50]/5 transition-all active:scale-[0.98]">
					{#if item.username}
						<button
							class="flex items-center gap-4 w-full text-left"
							onclick={() => goto(`/profile/user/${item.username}`)}
						>
							<div class="w-14 h-14 bg-[#8D7462]/10 rounded-full flex items-center justify-center overflow-hidden border-2 border-[#F5F2EA]">
								<img
									src={item.profilePictureUrl || 'http://localhost:8080/images/default-avatar.png'}
									alt="avatar"
									class="w-full h-full object-cover"
								/>
							</div>
							<div class="flex-1">
								<span class="font-black text-[#2F5D50] text-lg block">{item.username}</span>
								<span class="text-xs text-[#8D7462] font-medium line-clamp-1 italic">
                  {item.description || 'Leírás...'}
                </span>
							</div>
							<ChevronRightOutline class="w-5 h-5 text-[#2F5D50] opacity-20" />
						</button>
					{:else}
						<div class="flex flex-col gap-3">
							<div class="flex justify-between items-start">
								<h3 class="font-black text-[#2F5D50] text-xl leading-tight flex-1 mr-4">{item.title}</h3>
								{#if item.difficulty}
                  <span class="text-[10px] font-black px-2.5 py-1 bg-[#775D4D] text-[#F5F2EA] rounded-lg uppercase tracking-tighter shadow-sm">
                    {item.difficulty}
                  </span>
								{/if}
							</div>
							{#if type === "rated"}
								<div class="flex items-center gap-2">
									<span class="text-sm font-bold text-[#2F5D50]">{item.rating} / 5</span>
									<StarSolid class="w-4 h-4 text-yellow-400" />
								</div>
							{/if}
							<button
								onclick={() => goto(`/adventures/${item.id}`)}
								class="mt-2 text-[11px] font-black text-[#8D7462] uppercase tracking-widest flex items-center gap-1 group"
							>
								Részletek megnyitása <ChevronRightOutline class="w-3 h-3 group-hover:translate-x-1 transition-transform" />
							</button>
						</div>
					{/if}
				</div>
			{/each}
		{/if}
	</div>
</main>