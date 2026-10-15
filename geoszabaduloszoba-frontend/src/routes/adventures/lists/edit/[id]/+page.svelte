<script lang="ts">
 import { SearchOutline, ArrowLeftOutline, PlusOutline } from 'flowbite-svelte-icons';
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/state';
	import { goto } from '$app/navigation';

	let title = $state("");
	let description = $state("");
	let searchQuery = $state("");
	let searchResults = $state<any[]>([]);
	let selectedAdventures = $state<any[]>([]);
	let errorMessage = $state("");
	let isSearching = $state(false);
	let loading = $state(true);

	const listId = page.params.id;

	async function loadListDetails() {
		if (!auth.token) return;

		try {

			const res = await fetch(`https://api.zsomborszintai.com/lists/${listId}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (!res.ok) {
				goto('/adventures');
				return;
			}

			const listData = await res.json();
			title = listData.title;
			description = listData.description;

			if (listData.adventureIds && listData.adventureIds.length > 0) {
				for (const advId of listData.adventureIds) {
					const advRes = await fetch(`https://api.zsomborszintai.com/api/adventures/${advId}?lat=0&lon=0`, {
						headers: { 'Authorization': `Bearer ${auth.token}` }
					});
					if (advRes.ok) {
						const advJson = await advRes.json();
						selectedAdventures = [...selectedAdventures, advJson];
					}
				}
			}

		} catch (err) {
			console.error("Hiba a lista betöltésekor:", err);
			goto('/adventures');
		} finally {
			loading = false;
		}
	}

	onMount(loadListDetails);

	async function performSearch() {
		if (searchQuery.length < 2) {
			searchResults = [];
			return;
		}
		isSearching = true;
		try {
			const res = await fetch(
				`https://api.zsomborszintai.com/search?q=${searchQuery}&type=adventure&lat=0&lon=0`,
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

	function addAdventure(adv: any) {
		if (!selectedAdventures.find(a => a.id === adv.id)) {
			selectedAdventures = [...selectedAdventures, adv];
		}
		searchQuery = "";
		searchResults = [];
	}

	function removeAdventure(id: number) {
		selectedAdventures = selectedAdventures.filter(a => a.id !== id);
	}

	async function handleUpdate() {
		if (!title.trim()) {
			errorMessage = "Adj nevet a listának!";
			return;
		}
		if (selectedAdventures.length === 0) {
			errorMessage = "Válassz legalább egy kalandot a listához!";
			return;
		}

		try {

			const response = await fetch(`https://api.zsomborszintai.com/lists`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify({
					id: listId,
					title,
					description,
					adventureIds: selectedAdventures.map(a => a.id)
				})
			});

			if (response.ok) {
				goto('/adventures');
			} else {
				errorMessage = "Hiba történt a mentés során.";
			}
		} catch (err) {
			console.error("Hálózati hiba:", err);
			errorMessage = "Nem sikerült elérni a szervert.";
		}
	}

	$effect(() => {
		if (searchQuery.length >= 2) {
			const timer = setTimeout(performSearch, 300);
			return () => clearTimeout(timer);
		} else {
			searchResults = [];
		}
	});
</script>

<section class="flex flex-col p-6 pt-6 min-h-screen bg-[#F5F2EA]">

	<nav class="fixed top-[64px] left-0 w-full z-[55] px-4 py-2 bg-[#F5F2EA]/80 backdrop-blur-sm mb-8">
		<button
			type="button"
			class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group"
			onclick={() => goto('/adventures')}
		>
			<span class="block p-2 rounded-full bg-[#8D7462]/10 group-hover:bg-[#2F5D50]/10">
				<ArrowLeftOutline class="w-6 h-6" aria-hidden="true" />
			</span>
			<span class="text-[10px] font-black uppercase tracking-tighter">Mégse</span>
		</button>
	</nav>

	{#if loading}
		<p class="text-center py-20 italic text-[#2F5D50]/60">Lista betöltése és ellenőrzése...</p>
	{:else}
		{#if errorMessage}
			<p role="alert" class="alert-error-city mt-4">{errorMessage}</p>
		{/if}

		<section class="flex flex-col gap-2 mb-6 mt-4">
			<h1 class="label-city">Lista szerkesztése</h1>

			<input
				class="input-city-brown"
				placeholder="Lista neve..."
				bind:value={title}
			/>

			<textarea
				class="input-city-brown h-24"
				placeholder="Leírás..."
				bind:value={description}
			></textarea>
		</section>

		<section class="mb-8">
			<h2 class="label-city mb-4">Kalandok hozzáadása / eltávolítása</h2>

			<search class="relative mb-4">
       <span class="absolute left-4 top-1/2 -translate-y-1/2 opacity-30">
         <SearchOutline width="20" height="20" class="" color="#2F5D50" aria-hidden="true" />
       </span>
				<input
					type="text"
					placeholder="Keress kalandokat név alapján..."
					class="input-city-brown pl-12 mb-0 w-full"
					bind:value={searchQuery}
				/>

				{#if searchResults.length > 0 || isSearching}
					<ul class="absolute z-[100] w-full bg-white shadow-2xl rounded-2xl mt-2 overflow-hidden border-2 border-[#2F5D50]/10">
						{#if isSearching}
							<li class="p-4 text-center italic text-sm text-[#2F5D50]/50">Keresés...</li>
						{:else}
							{#each searchResults as res}
								<li>
									<button
										type="button"
										class="w-full text-left p-4 hover:bg-[#F5F2EA] flex justify-between items-center transition-colors border-b border-[#F5F2EA] last:border-0"
										onclick={() => addAdventure(res)}
									>
										<span class="block">
											<span class="block font-bold text-[#2F5D50]">{res.title}</span>
											<span class="block font-bold text-[#2F5D50]/30">{res.creator}</span>
										</span>
										<span class="text-[#2F5D50] font-black text-xl"><PlusOutline class="w-6 h-6" aria-hidden="true" /></span>
									</button>
								</li>
							{/each}
						{/if}
					</ul>
				{/if}
			</search>

			<h3 class="label-city mb-2">A lista tartalma ({selectedAdventures.length})</h3>
			<div class="space-y-2">
				{#each selectedAdventures as adv, i}
					<article class="adventure-card flex justify-between items-center bg-white/80 border border-[#2F5D50]/5">
						<span class="flex items-center gap-3">
							<span class="text-[10px] font-black opacity-20">{i + 1}</span>
							<span class="font-bold text-[#2F5D50]">{adv.title}</span>
						</span>
						<button
							onclick={() => removeAdventure(adv.id)}
							class="text-red-500 font-bold p-2 text-sm uppercase tracking-tighter"
						>
							eltávolítás
						</button>
					</article>
				{/each}
			</div>
		</section>

		<footer class="mt-auto pt-10">
			<button
				class="btn-primary w-full shadow-2xl"
				onclick={handleUpdate}
				disabled={!title || selectedAdventures.length === 0}
			>
				Módosítások mentése
			</button>
		</footer>
	{/if}
</section>

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-widest text-[#2F5D50] opacity-40 mb-1 block;
    }
</style>