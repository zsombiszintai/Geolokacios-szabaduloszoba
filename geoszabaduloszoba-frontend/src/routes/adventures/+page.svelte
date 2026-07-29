<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';
	import { TrashBinOutline, PenOutline } from 'flowbite-svelte-icons';

	interface Adventure {
		id: number;
		title: string;
		createdAt: string;
		status: 'PUBLISHED' | 'DRAFT' | 'PENDING';
	}

	interface AdventureList {
		id: number;
		title: string;
		description: string;
		adventureIds: number[];
	}

	let activeTab = $state<'adventures' | 'lists'>('adventures');
	let adventures = $state<Adventure[]>([]);
	let lists = $state<AdventureList[]>([]);
	let searchTerm = $state("");
	let loading = $state(true);

	let showDeleteModal = $state(false);
	let itemToDelete = $state<{id: number, type: 'adventure' | 'list'} | null>(null);

	async function loadData() {
		loading = true;
		try {
			const [advRes, listRes] = await Promise.all([
				fetch('http://localhost:8080/api/create-adventure/created-adventures', {
					headers: { 'Authorization': `Bearer ${auth.token}` }
				}),
				fetch('http://localhost:8080/lists', {
					headers: { 'Authorization': `Bearer ${auth.token}` }
				})
			]);

			if (advRes.ok) adventures = await advRes.ok ? await advRes.json() : [];
			if (listRes.ok) lists = await listRes.json();
		} catch (err) {
			console.error("Betöltési hiba:", err);
		} finally {
			loading = false;
		}
	}

	onMount(loadData);

	function confirmDelete(id: number, type: 'adventure' | 'list') {
		itemToDelete = { id, type };
		showDeleteModal = true;
	}

	async function executeDelete() {
		if (!itemToDelete) return;

		const url = itemToDelete.type === 'adventure'
			? `http://localhost:8080/api/create-adventure/${itemToDelete.id}`
			: `http://localhost:8080/lists/${itemToDelete.id}`;

		try {
			const response = await fetch(url, {
				method: 'DELETE',
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (response.ok) {
				if (itemToDelete.type === 'adventure') {
					adventures = adventures.filter(a => a.id !== itemToDelete!.id);
				} else {
					lists = lists.filter(l => l.id !== itemToDelete!.id);
				}
			}
		} catch (err) {
			console.error("Hiba a törlés során:", err);
		} finally {
			showDeleteModal = false;
			itemToDelete = null;
		}
	}

	let filteredAdventures = $derived(
		adventures.filter(a => a.title.toLowerCase().includes(searchTerm.toLowerCase()))
	);

	let filteredLists = $derived(
		lists.filter(l => l.title.toLowerCase().includes(searchTerm.toLowerCase()))
	);

	const statusColors = {
		'PUBLIC': 'bg-green-500',
		'DRAFT': 'bg-gray-400',
		'PENDING': 'bg-yellow-500',
		'REJECTED': 'bg-red-500'
	};
</script>

<main class="flex flex-col p-6 pt-24 pb-20 min-h-screen bg-[#F5F2EA]">

	<div class="flex bg-white/50 rounded-2xl p-1 mb-8 shadow-inner border border-[#2F5D50]/10">
		<button
			class="flex-1 py-3 rounded-xl font-bold transition-all {activeTab === 'adventures' ? 'bg-city-brown text-white shadow-md' : 'text-city-brown'}"
			onclick={() => activeTab = 'adventures'}>
			Kalandjaim
		</button>
		<button
			class="flex-1 py-3 rounded-xl font-bold transition-all {activeTab === 'lists' ? 'bg-[#2F5D50] text-white shadow-md' : 'text-[#2F5D50]'}"
			onclick={() => activeTab = 'lists'}>
			Listáim
		</button>
	</div>

	<div class="relative mb-8">
    <span class="absolute left-4 top-1/2 -translate-y-1/2 opacity-30">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2F5D50" stroke-width="2.5">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
      </svg>
    </span>
		<input
			type="text"
			placeholder="Keresés..."
			class="w-full h-12 pl-12 pr-4 bg-white/80 rounded-xl border-b-4 border-[#2F5D50]/20 outline-none focus:border-[#2F5D50] transition-all text-[#2F5D50] font-bold"
			bind:value={searchTerm}
		/>
	</div>

	{#if activeTab === 'adventures'}
		<section>
			<div class="flex justify-between items-end mb-4">
				<h2 class="text-sm font-bold text-[#2F5D50] uppercase tracking-wider">Új kaland</h2>
			</div>

			<button
				type="button"
				class="w-full flex flex-col items-start gap-2 group mb-10"
				onclick={() => goto('/adventures/create')}
			>
				<div class="w-full h-16 bg-[#8D7462] rounded-2xl flex items-center justify-center shadow-lg group-active:scale-[0.98] transition-all">
					<span class="text-white text-4xl font-light">+</span>
				</div>
				<p class="text-[#8D7462] text-sm italic">Készítsd el a saját kalandod...</p>
			</button>

			<h2 class="label-city mb-4">Létrehozott kalandjaid</h2>
			<header class="grid grid-cols-[2fr_1fr_1.5fr_0.5fr_0.5fr] px-4 mb-2 text-[10px] font-bold text-gray-500 uppercase border-b border-gray-200 pb-2">
				<span>Név</span>
				<span class="text-center">Állapot</span>
				<span class="text-center">Dátum</span>
				<span class="text-right">Szerk.</span>
				<span class="text-right">Törlés</span>
			</header>

			<div class="space-y-3">
				{#if loading}
					<p class="text-center py-10 italic opacity-50">Betöltés...</p>
				{:else if filteredAdventures.length === 0}
					<p class="text-center py-10 italic opacity-50">Nincs talált kaland.</p>
				{:else}
					{#each filteredAdventures as adventure}
						<article class="bg-city-brown/90 p-4 rounded-2xl shadow-sm border border-[#2F5D50]/5 grid grid-cols-[2fr_1fr_1.5fr_0.5fr_0.5fr] items-center">
							<span class="font-bold truncate text-city-cream">{adventure.title}</span>
							<div class="flex justify-center">
								<div class="w-3 h-3 rounded-full {statusColors[adventure.status] || 'bg-gray-400'}" title={adventure.status}></div>
							</div>
							<span class="text-[10px] text-center text-city-cream">{new Date(adventure.createdAt).toLocaleDateString('hu-HU')}</span>
							<button class="flex justify-end text-city-cream hover:text-[#2F5D50]"><PenOutline class=" w-6 h-6"/></button>
							<button onclick={() => confirmDelete(adventure.id, 'adventure')} class="flex justify-end text-red-400 hover:text-red-600"><TrashBinOutline class=" w-6 h-6 "/></button>
						</article>
					{/each}
				{/if}
			</div>
		</section>

	{:else}
		<section>
			<h2 class="text-sm font-bold text-[#2F5D50] mb-4 uppercase tracking-wider">Új lista</h2>
			<button
				type="button"
				class="w-full flex flex-col items-start gap-2 group mb-10"
				onclick={() => goto('/adventures/lists/create')}
			>
				<div class="w-full h-16 bg-[#2F5D50] rounded-2xl flex items-center justify-center shadow-lg group-active:scale-[0.98] transition-all">
					<span class="text-white text-4xl font-light">+</span>
				</div>
				<p class="text-[#2F5D50] text-sm italic">Gyűjtsd össze kedvenc kalandjaidat...</p>
			</button>

			<h2 class="label-city mb-4">Saját listáid</h2>
			<div class="space-y-4">
				{#if loading}
					<p class="text-center py-10 italic opacity-50">Betöltés...</p>
				{:else if filteredLists.length === 0}
					<p class="text-center py-10 italic opacity-50">Nincs létrehozott listád.</p>
				{:else}
					{#each filteredLists as list}
						<article class="bg-city-green p-5 rounded-2xl shadow-sm border border-[#2F5D50]/10 flex items-center justify-between gap-4">
							<div class="flex-1 min-w-0">
								<h3 class="font-bold text-city-cream text-lg truncate">{list.title}</h3>
								<div class="flex gap-1 mt-1">
      <span class="text-[10px] font-bold bg-[#2F5D50]/80 text-city-cream px-1 py-1 rounded-lg uppercase">
        {list.adventureIds?.length || 0} KALAND
      </span>
								</div>
							</div>
							<div class="flex items-center gap-2 shrink-0">
								<button
									onclick={() => goto(`/adventures/lists/edit/${list.id}`)}
									class="p-2 text-city-cream hover:bg-white/10 rounded-xl transition-all active:scale-90"
									aria-label="Szerkesztés"
								>
									<PenOutline class="w-6 h-6"/>
								</button>

								<button
									onclick={() => confirmDelete(list.id, 'list')}
									class="p-2 text-red-400 hover:bg-red-50/10 rounded-xl transition-all active:scale-90"
									aria-label="Törlés"
								>
									<TrashBinOutline class="w-6 h-6"/>
								</button>
							</div>
						</article>
					{/each}
				{/if}
			</div>
		</section>
	{/if}

	{#if showDeleteModal}
		<div class="fixed inset-0 z-[2000] flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
			<div class="bg-[#F5F2EA] w-full max-w-sm rounded-3xl p-8 shadow-2xl border-2 border-[#8D7462]">
				<h3 class="text-[#2F5D50] text-xl font-bold mb-4">Biztosan törlöd?</h3>
				<p class="text-[#8D7462] mb-8 leading-relaxed text-sm">
					Ez a művelet végleges. A {itemToDelete?.type === 'adventure' ? 'kaland' : 'lista'} minden adata törlődik a rendszerből.
				</p>
				<div class="flex gap-4">
					<button onclick={() => showDeleteModal = false} class="flex-1 py-3 rounded-xl font-bold text-gray-400">Mégse</button>
					<button onclick={executeDelete} class="flex-1 py-3 rounded-xl font-bold bg-red-600 text-white shadow-lg active:scale-95 transition-all">Törlés</button>
				</div>
			</div>
		</div>
	{/if}

</main>

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-widest text-[#2F5D50] opacity-40;
    }
</style>