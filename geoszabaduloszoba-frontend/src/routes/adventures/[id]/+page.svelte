<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/state';
	import {
		ChevronLeftOutline,
		UserCircleSolid,
		StarSolid,
		DotsHorizontalOutline,
		SearchOutline,
		PlusOutline,
		CheckOutline
	} from 'flowbite-svelte-icons';
	import { goto } from '$app/navigation';

	let adventure = $state<any>(null);

	let menuOpen = $state<boolean>(false);
	let listSelectorOpen = $state<boolean>(false);
	let myLists = $state<any[]>([]);
	let searchQuery = $state<string>("");
	let errorMessage = $state<string>("");

	let filteredLists = $derived(
		searchQuery.trim() === ""
			? myLists
			: myLists.filter(l => l.title?.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	const userPos = { lat: 46.0754, lon: 18.2205 };

	function getDifficultyColor(difficulty: string): string {
		if (!difficulty) return 'bg-gray-400';

		const normalized = difficulty.toUpperCase().trim();

		if (normalized.includes('KÖNNYÜ') || normalized.includes('KÖNNYŰ') || normalized.includes('EASY')) {
			return 'text-green-700';
		}
		if (normalized.includes('KÖZEPES') || normalized.includes('MEDIUM')) {
			return 'text-yellow-700';
		}
		if (normalized.includes('NEHÉZ') || normalized.includes('HARD')) {
			return 'text-red-700';
		}

		return 'text-gray-400';
	}

	async function toggleAdventureInList(listId: number) {
		const targetList = myLists.find(l => l.id === listId);
		if (!targetList) return;

		const isAlreadyInList = targetList.adventureIds?.includes(adventure.id);

		if (isAlreadyInList) {
			try {
				const res = await fetch(`http://localhost:8080/lists/${listId}/adventures/${adventure.id}`, {
					method: 'DELETE',
					headers: { 'Authorization': `Bearer ${auth.token}` }
				});

				if (res.ok) {
					targetList.adventureIds = targetList.adventureIds.filter((id: number) => id !== adventure.id);

					errorMessage = "";
				} else {
					errorMessage = "Hiba történt az eltávolítás során.";
				}
			} catch (e) {
				console.error(e);
				errorMessage = "Hálózati hiba az eltávolításkor.";
			}

		} else {
			try {
				const res = await fetch(`http://localhost:8080/lists/${listId}/adventures/${adventure.id}`, {
					method: 'POST',
					headers: { 'Authorization': `Bearer ${auth.token}` }
				});

				if (res.ok) {
					if (!targetList.adventureIds) targetList.adventureIds = [];
					targetList.adventureIds.push(adventure.id);

					errorMessage = "";
				} else {
					errorMessage = "Hiba történt a hozzáadás során.";
				}
			} catch (e) {
				console.error(e);
				errorMessage = "Hálózati hiba a mentéskor.";
			}
		}
	}

	async function fetchAdventureDetails() {
		if (!auth.token) return;

		try {
			const id = page.params.id;
			const url = `http://localhost:8080/api/adventures/${id}?lat=${userPos.lat}&lon=${userPos.lon}`;

			const res = await fetch(url, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				adventure = await res.json();
			} else {
				errorMessage = "Nem sikerült betölteni a kaland részleteit.";
			}
		} catch (e) {
			console.error(e);
			errorMessage = "Hálózati hiba a kaland részleteinek lekérésekor.";
		}
	}

	function toggleMenu() {
		menuOpen = !menuOpen;
		if (!menuOpen) {
			listSelectorOpen = false;
			errorMessage = "";
		}
	}

	function shareAdventure() {
		if (navigator.share) {
			navigator.share({
				title: adventure?.title,
				text: 'Nézd meg ezt a geolokációs szabadulószobát!',
				url: window.location.href
			}).catch(console.error);
		} else {
			errorMessage = "A megosztás funkció ezen a böngészőn nem támogatott.";
		}
		menuOpen = false;
	}

	async function openListSelector() {
		errorMessage = "";
		await fetchMyLists();
		listSelectorOpen = true;
	}

	async function fetchMyLists() {
		if (!auth.token) return;
		try {
			const res = await fetch('http://localhost:8080/lists', {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				myLists = await res.json();
			} else {
				errorMessage = "Nem sikerült betölteni a listáidat.";
			}
		} catch (e) {
			console.error(e);
			errorMessage = "Hálózati hiba a listák lekérésekor.";
		}
	}

	$effect(() => {
		if (auth.token) {
			fetchAdventureDetails();
		}
	});
</script>

{#if adventure}
	<main class="flex flex-col min-h-screen bg-[#F5F2EA] font-josefin p-6 pt-6 pb-6">

		<button
			class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group mb-8 w-fit"
			onclick={() => window.history.back()}
		>
			<ChevronLeftOutline class="w-5 h-5 transition-transform group-hover:-translate-x-1" />
			<span class="font-bold">Vissza</span>
		</button>

		<div class="absolute right-0">
			<button
				class="p-2 text-[#8D7462] hover:text-[#2F5D50] hover:bg-[#2F5D50]/5 rounded-xl transition-all"
				aria-label="További opciók"
				aria-expanded={menuOpen}
				onclick={toggleMenu}
			>
				<DotsHorizontalOutline class="w-6 h-6" />
			</button>

			{#if menuOpen}
				<div class="absolute right-0 top-11 flex items-start gap-2 z-50">

					{#if listSelectorOpen}
						<section class="bg-city-cream text-city-green rounded-2xl shadow-2xl p-3 w-64 border border-[#2F5D50]/10 flex flex-col max-h-80">

							{#if errorMessage}
								<div class="text-xs font-bold text-red-600 bg-red-50 p-2 rounded-lg mb-2 border border-red-200">
									{errorMessage}
								</div>
							{/if}

							<div class="relative mb-2">
								<SearchOutline class="w-4 h-4 text-gray-400 absolute left-2.5 top-2.5" />
								<input
									type="text"
									placeholder="Lista keresése..."
									bind:value={searchQuery}
									class="w-full bg-white text-sm text-city-green pl-9 pr-3 py-1.5 rounded-lg border-none focus:ring-1 focus:ring-emerald-500 placeholder-gray-500"
								/>
							</div>

							<button class="flex items-center gap-2 w-full text-left px-2 py-2 text-xs font-bold text-city-green hover:bg-[#2F5D50]/5 rounded-lg transition-colors mb-1">
								<PlusOutline class="w-4 h-4" />
								<span>Új lista létrehozása</span>
							</button>

							<hr class="border-[#2F5D50]/10 my-1" />

							<div class="overflow-y-auto flex-1 space-y-0.5 pr-1">
								{#each filteredLists as list}
									<button
										class="w-full text-left px-2 py-2 text-sm font-semibold rounded-lg hover:bg-[#2F5D50]/5 transition-colors text-city-green flex justify-between items-center gap-2"
										onclick={() => toggleAdventureInList(list.id)}
									>
										<span class="truncate flex-1">{list.title}</span>
										{#if list.adventureIds?.includes(adventure.id)}
											<CheckOutline class="w-4 h-4 text-emerald-600 shrink-0" />
										{/if}
									</button>
								{:else}
									<p class="text-xs text-gray-500 italic p-2 text-center">Nincs találat</p>
								{/each}
							</div>
						</section>
					{/if}

					<nav class="bg-city-cream text-white rounded-2xl shadow-2xl py-2 w-52 border border-[#2F5D50]/10">
						<button
							class="w-full text-left px-4 py-2.5 text-sm font-bold text-city-green hover:bg-[#2F5D50]/5 transition-colors flex justify-between items-center"
							onclick={openListSelector}
						>
							<ChevronLeftOutline class="w-4 h-4" />
							<span>Mentés listára</span>
						</button>
						<button
							class="w-full text-left px-4 py-2.5 text-sm font-bold text-city-green hover:bg-[#2F5D50]/5 transition-colors"
							onclick={shareAdventure}
						>
							Kaland megosztása
						</button>
					</nav>
				</div>
			{/if}
		</div>

		{#if errorMessage && !listSelectorOpen}
			<div class="alert-error-city mb-4">{errorMessage}</div>
		{/if}

		<div class="space-y-8">

			<section class="space-y-6">
				<div class="border-b border-[#2F5D50]/10 pb-4">
					<h2 class="label-city mb-1">Kaland címe</h2>
					<h1 class="text-2xl font-black text-[#2F5D50] leading-tight">{adventure.title}</h1>
				</div>

				<div class="grid grid-cols-2 gap-4">
					<div class="bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Időtartam</h2>
						<p class="font-bold text-[#2F5D50]">{adventure.averageTime}</p>
					</div>

					<div class="bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Távolság</h2>
						<p class="font-bold text-[#2F5D50]">{adventure.distanceInMeters} m</p>
					</div>
				</div>

				<div class="flex justify-between items-center bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
					<div>
						<h2 class="label-city mb-1">Nehézség</h2>
						<p class="font-black {getDifficultyColor(adventure.difficulty)} uppercase tracking-tighter">{adventure.difficulty}</p>
					</div>
					<div class="text-right">
						<h2 class="label-city mb-1">Készítő</h2>
						<div class="flex items-center gap-2 justify-end">
							<a
								href="/profile/user/{adventure.creatorName}"
								class="font-bold text-[#8D7462] hover:underline"
							>
								{adventure.creatorName}
							</a>
							<UserCircleSolid class="w-6 h-6 text-[#8D7462]/40" />
						</div>
					</div>
				</div>
			</section>

			<section class="space-y-4">
				<h2 class="label-city">Legutóbbi vélemények</h2>

				{#if adventure.reviews && adventure.reviews.length > 0}
					<div class="space-y-4">
						{#each adventure.reviews as review}
							<article class="bg-city-brown p-5 rounded-2xl shadow-sm border border-[#2F5D50]/5">
								<div class="flex justify-between items-start mb-3">
									<div class="flex items-center gap-3">
										<div class="p-2 rounded-xl">
											<UserCircleSolid class="w-6 h-6 text-city-cream" />
										</div>
										<div class="flex flex-col">
											<span class="font-bold text-city-cream text-sm">Névtelen kalandor</span>
											<span class="text-[10px] font-bold text-city-cream opacity-60 uppercase">
              {new Date(review.reviewedAt).toLocaleDateString('hu-HU')}
            </span>
										</div>
									</div>

									<div class="flex bg-city-brown px-2 py-1 rounded-lg gap-0.5">
										{#each Array(5) as _, i}
											<StarSolid
												class="w-3 h-3 {i < review.rating ? 'text-yellow-400' : 'text-gray-300'}"
											/>
										{/each}
									</div>
								</div>

								{#if review.reviewText}
									<p class="text-sm text-city-cream italic leading-relaxed border-l-2 border-[#8D7462]/40 pl-3">
										"{review.reviewText}"
									</p>
								{/if}
							</article>
						{/each}
					</div>
				{:else}
					<div class="bg-white/40 border-2 border-dashed border-[#2F5D50]/10 rounded-3xl p-10 text-center">
						<p class="text-[#2F5D50] opacity-40 font-bold italic text-sm">Még nincsenek vélemények...</p>
					</div>
				{/if}
			</section>
		</div>

		<div class="fixed bottom-0 left-0 right-0 p-16 bg-gradient-to-t from-[#F5F2EA] via-[#F5F2EA] to-transparent">
			<button
				class="w-full bg-[#2F5D50] text-white py-4 rounded-2xl font-black text-xl shadow-xl active:scale-[0.97] transition-all uppercase tracking-widest"
				onclick={() => goto(`/game/navigation?id=${adventure.id}`)}
			>
				Kaland Indítása
			</button>
		</div>
	</main>
{/if}

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-widest text-[#2F5D50] opacity-40;
    }
</style>