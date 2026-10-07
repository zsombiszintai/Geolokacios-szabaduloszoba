<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { TrashBinOutline, PenOutline } from 'flowbite-svelte-icons';

	interface AiModerationResponse {
		isProfane: boolean;
		isSolvable: boolean;
		profanityDetails: string | null;
		solvabilityDetails: string | null;
		overallApproved: boolean;
		reason: string | null;
	}

	interface Adventure {
		id: number;
		title: string;
		createdAt: string;
		status: 'PUBLIC' | 'DRAFT' | 'PENDING' | 'REJECTED';
		aiModeration?: AiModerationResponse | null;
	}

	interface AdventureList {
		id: number;
		title: string;
		description: string;
		adventureIds: number[];
	}

	let moderationDialog: HTMLDialogElement;
	let selectedAdventure = $state<Adventure | null>(null);

	let activeTab = $state<'adventures' | 'lists'>('adventures');
	let adventures = $state<Adventure[]>([]);
	let lists = $state<AdventureList[]>([]);
	let searchTerm = $state("");
	let loading = $state(true);
	let errorMessage = $state("");

	let showDeleteModal = $state(false);
	let itemToDelete = $state<{id: number, type: 'adventure' | 'list'} | null>(null);

	function openModeration(adventure: Adventure) {
		selectedAdventure = adventure;
		moderationDialog.showModal();
	}

	function closeModeration() {
		moderationDialog.close();
	}

	$effect(() => {
		if (!selectedAdventure) return;

		const previousOverflow = document.body.style.overflow;
		document.body.style.overflow = 'hidden';

		return () => {
			document.body.style.overflow = previousOverflow;
		};
	});

	async function loadData() {
		loading = true;
		try {
			const [advRes, listRes] = await Promise.all([
				fetch('https://api.zsomborszintai.com/api/create-adventure/created-adventures', {
					headers: { 'Authorization': `Bearer ${auth.token}` }
				}),
				fetch('https://api.zsomborszintai.com/lists', {
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

	onMount(() => {
		const errorParam = page.url.searchParams.get('error');
		if (errorParam === 'unauthorized') {
			errorMessage = "Nincs jogosultságod a kaland szerkesztéséhez!";
		}

		loadData();
	});

	function confirmDelete(id: number, type: 'adventure' | 'list') {
		itemToDelete = { id, type };
		showDeleteModal = true;
	}

	async function executeDelete() {
		if (!itemToDelete) return;

		const url = itemToDelete.type === 'adventure'
			? `https://api.zsomborszintai.com/api/create-adventure/${itemToDelete.id}`
			: `https://api.zsomborszintai.com/lists/${itemToDelete.id}`;

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

<main class="flex flex-col p-6 pt-12 pb-6 min-h-screen bg-[#F5F2EA]">

	{#if errorMessage}
		<div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded-xl mb-6 font-bold text-xs flex justify-between items-center shadow-sm">
			<span>{errorMessage}</span>
			<button onclick={() => errorMessage = ""} class="text-red-700 hover:text-red-900 font-black ml-2 text-sm">✕</button>
		</div>
	{/if}

	<div class="flex bg-white/50 rounded-2xl p-1 mb-4 shadow-inner border border-[#2F5D50]/10">
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
			<header class="grid grid-cols-[minmax(0,1fr)_44px_132px] items-center gap-2 px-4 mb-2 text-[10px] font-bold text-gray-500 uppercase border-b border-gray-200 pb-2">
				<span>Név / dátum</span>
				<span class="text-center">Állapot</span>
				<span class="text-right">Műveletek</span>
			</header>

			<div class="space-y-3">
				{#if loading}
					<p class="text-center py-10 italic opacity-50">Betöltés...</p>
				{:else if filteredAdventures.length === 0}
					<p class="text-center py-10 italic opacity-50">Nincs talált kaland.</p>
				{:else}
					{#each filteredAdventures as adventure}
						<article class="bg-city-brown/90 p-4 rounded-2xl shadow-sm border border-[#2F5D50]/5 grid grid-cols-[minmax(0,1fr)_44px_132px] items-center gap-2">
						<div class="min-w-0">
							<p class="font-bold truncate text-city-cream" title={adventure.title}>
								{adventure.title}
							</p>
							<p class="mt-1 text-[10px] text-city-cream/70">
								{new Date(adventure.createdAt).toLocaleDateString('hu-HU')}
							</p>
						</div>
						<div class="flex justify-center">
						<span
							class="w-3 h-3 rounded-full {statusColors[adventure.status] || 'bg-gray-400'}"
							title={adventure.status}
						>
							<span class="sr-only">{adventure.status}</span>
						</span>
						</div>

						<div class="flex items-center justify-end">
							{#if adventure.status === 'REJECTED'}
								<button
									type="button"
									onclick={() => openModeration(adventure)}
									class="w-11 h-11 shrink-0 flex items-center justify-center rounded-xl text-amber-300 hover:bg-white/10 active:scale-95"
									aria-label={`${adventure.title}: elutasítás részletei`}
									title="Miért lett elutasítva?"
								>
									<svg
										width="24"
										height="24"
										viewBox="0 0 24 24"
										fill="none"
										stroke="currentColor"
										stroke-width="2"
										stroke-linecap="round"
										stroke-linejoin="round"
										aria-hidden="true"
									>
										<path d="M10.3 4.1 2.5 17.5A2 2 0 0 0 4.2 20.5h15.6a2 2 0 0 0 1.7-3L13.7 4.1a2 2 0 0 0-3.4 0Z" />
										<path d="M12 9v4" />
										<path d="M12 17h.01" />
									</svg>
								</button>
							{/if}

							<button
								type="button"
								onclick={() => goto(`/adventures/edit/${adventure.id}`)}
								class="w-11 h-11 shrink-0 flex items-center justify-center rounded-xl text-city-cream hover:bg-white/10 active:scale-95"
								aria-label="Kaland szerkesztése"
							>
								<PenOutline class="w-6 h-6" />
							</button>

							<button
								type="button"
								onclick={() => confirmDelete(adventure.id, 'adventure')}
								class="w-11 h-11 shrink-0 flex items-center justify-center rounded-xl text-red-400 hover:bg-white/10 active:scale-95"
								aria-label="Kaland törlése"
							>
								<TrashBinOutline class="w-6 h-6" />
							</button>
						</div>
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

<dialog
	bind:this={moderationDialog}
	onclose={() => selectedAdventure = null}
	aria-labelledby="moderation-title"
	class="moderation-dialog"
>
	{#if selectedAdventure}
		{@const moderation = selectedAdventure.aiModeration}

		<div class="p-6">
			<div class="flex items-start justify-between gap-4">
				<div class="min-w-0">
					<p class="text-[10px] font-black uppercase tracking-widest text-[#8D7462] mb-2">
						Automatikus ellenőrzés
					</p>

					<h2
						id="moderation-title"
						class="text-xl font-black text-[#2F5D50]"
					>
						Elutasítás részletei
					</h2>

					<p class="mt-2 text-sm text-[#8D7462] break-words">
						{selectedAdventure.title}
					</p>
				</div>

				<button
					type="button"
					onclick={closeModeration}
					class="w-11 h-11 shrink-0 rounded-xl bg-[#8D7462]/10 text-[#2F5D50] text-xl"
					aria-label="Ablak bezárása"
				>
					×
				</button>
			</div>

			{#if moderation}
				<div class="mt-6 rounded-2xl bg-red-50 border border-red-200 p-4">
					<h3 class="text-sm font-black text-red-800 mb-2">
						Indoklás
					</h3>

					<p class="text-sm text-red-900 leading-relaxed whitespace-pre-line break-words">
						{moderation.reason || 'Az ellenőrzés nem adott meg külön indoklást.'}
					</p>
				</div>

				<dl class="mt-5 space-y-4 text-sm">
					<div>
						<dt class="font-bold text-[#2F5D50]">
							Sértő tartalom jelzése
						</dt>
						<dd class="mt-1 text-[#8D7462]">
							{moderation.isProfane ? 'Igen' : 'Nem'}
						</dd>

						{#if moderation.profanityDetails}
							<dd class="mt-2 text-[#2F5D50] whitespace-pre-line break-words">
								{moderation.profanityDetails}
							</dd>
						{/if}
					</div>

					<div class="border-t border-[#8D7462]/20 pt-4">
						<dt class="font-bold text-[#2F5D50]">
							Megoldhatónak értékelve
						</dt>
						<dd class="mt-1 text-[#8D7462]">
							{moderation.isSolvable ? 'Igen' : 'Nem'}
						</dd>

						{#if moderation.solvabilityDetails}
							<dd class="mt-2 text-[#2F5D50] whitespace-pre-line break-words">
								{moderation.solvabilityDetails}
							</dd>
						{/if}
					</div>

					<div class="border-t border-[#8D7462]/20 pt-4">
						<dt class="font-bold text-[#2F5D50]">
							Automatikus jóváhagyás
						</dt>
						<dd class="mt-1 text-[#8D7462]">
							{moderation.overallApproved ? 'Jóváhagyva' : 'Nem jóváhagyott'}
						</dd>
					</div>
				</dl>
			{:else}
				<p class="mt-6 rounded-2xl bg-[#8D7462]/10 p-4 text-sm text-[#2F5D50] leading-relaxed">
					Ehhez a kalandhoz még nincs eltárolt részletes ellenőrzési eredmény.
				</p>
			{/if}

			<button
				type="button"
				onclick={closeModeration}
				class="mt-6 w-full rounded-xl bg-[#2F5D50] text-white py-3 font-bold active:scale-[0.98]"
			>
				Bezárás
			</button>
		</div>
	{/if}
</dialog>

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-widest text-[#2F5D50] opacity-40;
    }
    .moderation-dialog {
        position: fixed;
        inset: 0;
        margin: auto;
        width: calc(100% - 2rem);
        max-width: 28rem;
        max-height: 85dvh;
        padding: 0;
        overflow-y: auto;
        overscroll-behavior: contain;
        border: 2px solid #8d7462;
        border-radius: 1.5rem;
        background: #f5f2ea;
        box-shadow: 0 24px 70px rgb(0 0 0 / 30%);
    }

    .moderation-dialog::backdrop {
        background: rgb(0 0 0 / 60%);
        backdrop-filter: blur(4px);
    }
</style>