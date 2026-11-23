<script lang="ts">
	import BackButton from '$lib/components/BackButton.svelte';
	import Avatar from '$lib/components/Avatar.svelte';
	import { translateDifficulty } from '$lib/i18n/difficulty';
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/state';
	import LikeButton from '$lib/components/LikeButton.svelte';
	import {
		ChevronLeftOutline,
		ChevronRightOutline,
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
	let listCreateOpen = $state(false);
	let newListTitle = $state('');
	let isCreatingList = $state(false);
	let myLists = $state<any[]>([]);
	let searchQuery = $state<string>("");
	let errorMessage = $state<string>("");

	let isStarting = $state(false);
	let startError = $state('');

	let startedSession: {
		adventureId: string;
		sessionId: number;
	} | null = null;

	let filteredLists = $derived(
		searchQuery.trim() === ""
			? myLists
			: myLists.filter(l => l.title?.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	async function startAdventure() {
		if (isStarting || !adventure) return;

		startError = '';

		if (!auth.token) {
			startError = 'A játék indításához jelentkezz be!';
			return;
		}

		if (!Array.isArray(adventure.stations)) {
			startError = 'Az állomások nem töltődtek be. Frissítsd az oldalt!';
			return;
		}

		const id = String(adventure.id);

		const startingStation = adventure.stations.find(
			(station: any) => station.seqNumber === 0
		);

		const hasPlayableStation = adventure.stations.some(
			(station: any) => station.seqNumber > 0
		);

		if (!hasPlayableStation) {
			startError = 'A kalandnak nincs játszható állomása.';
			return;
		}

		isStarting = true;

		try {
			if (startingStation) {
				await goto(`/game/navigation?id=${encodeURIComponent(id)}`);
				return;
			}

			if (startedSession?.adventureId !== id) {
				const response = await fetch(
					`https://api.zsomborszintai.com/api/game/start/${encodeURIComponent(id)}`,
					{
						method: 'POST',
						headers: {
							Authorization: `Bearer ${auth.token}`
						}
					}
				);

				if (!response.ok) {
					throw new Error(
						`A játék indítása sikertelen (HTTP ${response.status}).`
					);
				}

				const sessionId = await response.json();

				if (!Number.isSafeInteger(sessionId) || sessionId <= 0) {
					throw new Error('A szerver hibás játékmenet-azonosítót adott vissza.');
				}

				startedSession = {
					adventureId: id,
					sessionId
				};
			}

			await goto(
				`/game?sessionId=${startedSession.sessionId}&adventureId=${encodeURIComponent(id)}`
			);
		} catch (error) {
			console.error('Játékindítási hiba:', error);

			startError = error instanceof Error
				? error.message
				: 'A játék indítása sikertelen. Próbáld újra.';
		} finally {
			isStarting = false;
		}
	}

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
				const res = await fetch(`https://api.zsomborszintai.com/lists/${listId}/adventures/${adventure.id}`, {
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
				const res = await fetch(`https://api.zsomborszintai.com/lists/${listId}/adventures/${adventure.id}`, {
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
			if (!id) return;
			const url = `https://api.zsomborszintai.com/api/adventures/${encodeURIComponent(id)}`;

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
		if (isCreatingList) return;
		menuOpen = !menuOpen;
		if (!menuOpen) {
			listSelectorOpen = false;
			listCreateOpen = false;
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

	async function createListWithAdventure(event: SubmitEvent) {
		event.preventDefault();
		if (isCreatingList || !adventure) return;
		const title = newListTitle.trim();
		if (!title) {
			errorMessage = 'Adj nevet a listának!';
			return;
		}
		isCreatingList = true;
		errorMessage = '';
		try {
			const response = await fetch('https://api.zsomborszintai.com/lists', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					Authorization: `Bearer ${auth.token}`
				},
				body: JSON.stringify({ title, description: '', adventureIds: [adventure.id] })
			});
			if (!response.ok) {
				errorMessage = 'Nem sikerült létrehozni a listát. Próbáld újra!';
				return;
			}
			newListTitle = '';
			searchQuery = '';
			listCreateOpen = false;
			await fetchMyLists();
		} catch (error) {
			console.error('Lista létrehozási hiba:', error);
			errorMessage = 'Nem sikerült elérni a szervert.';
		} finally {
			isCreatingList = false;
		}
	}

	async function fetchMyLists() {
		if (!auth.token) return;
		try {
			const res = await fetch('https://api.zsomborszintai.com/lists', {
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

	<svelte:head>
		<title>{adventure?.title || 'Kaland'} | CityScape</title>
	</svelte:head>

{#if adventure}
	<section class="flex flex-col min-h-screen bg-[#F5F2EA] font-josefin p-6 pt-6 pb-28">

		<BackButton />

		<div class="absolute right-6 flex items-center gap-1">
			{#key adventure.id}
				<LikeButton type="ADVENTURE" likedId={String(adventure.id)} />
			{/key}
			<button
				class="p-2 text-[#8D7462] hover:text-[#2F5D50] hover:bg-[#2F5D50]/5 rounded-xl transition-all"
				aria-label="További opciók"
				aria-expanded={menuOpen}
				onclick={toggleMenu}
			>
				<DotsHorizontalOutline class="w-6 h-6" />
			</button>

			{#if menuOpen}
				<div class="absolute right-0 top-11 w-64 max-w-[calc(100vw-3rem)] z-50">

					{#if listCreateOpen}
						<section class="bg-city-cream text-city-green rounded-2xl shadow-2xl p-3 w-full border border-[#2F5D50]/10">
							<button type="button" disabled={isCreatingList}
								class="flex items-center gap-2 rounded-lg px-2 py-2 mb-2 text-left text-sm font-bold hover:bg-[#2F5D50]/5 disabled:opacity-50"
								onclick={() => { listCreateOpen = false; errorMessage = ''; }}>
								<ChevronLeftOutline class="w-4 h-4 shrink-0" />
								<span>Vissza a listákhoz</span>
							</button>
							<form onsubmit={createListWithAdventure} class="space-y-3">
								<h2 class="px-2 text-sm font-bold">Új lista létrehozása</h2>
								{#if errorMessage}
									<p role="alert" class="text-xs font-bold text-red-600 bg-red-50 p-2 rounded-lg">{errorMessage}</p>
								{/if}
								<label for="new-list-title" class="block px-2 text-xs font-bold">Lista neve</label>
								<input id="new-list-title" type="text" required bind:value={newListTitle} disabled={isCreatingList}
									placeholder="Lista neve..." class="w-full bg-white text-sm text-city-green px-3 py-2 rounded-lg border border-[#2F5D50]/20 focus:ring-1 focus:ring-emerald-500" />
								<p class="px-2 text-xs break-words">A listához hozzáadjuk: {adventure.title}</p>
								<button type="submit" disabled={isCreatingList || !newListTitle.trim()}
									class="w-full bg-city-green text-white py-2.5 rounded-lg text-sm font-bold disabled:opacity-50">
									{isCreatingList ? 'Létrehozás...' : 'Lista létrehozása'}
								</button>
							</form>
						</section>
					{:else if listSelectorOpen}
						<section class="bg-city-cream text-city-green rounded-2xl shadow-2xl p-3 w-full border border-[#2F5D50]/10 flex flex-col max-h-80">
							<button
								type="button"
								class="flex items-center gap-2 rounded-lg px-2 py-2 mb-2 text-left text-sm font-bold hover:bg-[#2F5D50]/5 transition-colors"
								onclick={() => { listSelectorOpen = false; errorMessage = ''; }}
							>
								<ChevronLeftOutline class="w-4 h-4 shrink-0" />
								<span>Vissza</span>
							</button>

							{#if errorMessage}
								<p role="alert" class="text-xs font-bold text-red-600 bg-red-50 p-2 rounded-lg mb-2 border border-red-200">
									{errorMessage}
								</p>
							{/if}

							<search class="relative mb-2">
								<SearchOutline class="w-4 h-4 text-gray-400 absolute left-2.5 top-2.5" />
								<input
									type="text"
									placeholder="Lista keresése..."
									bind:value={searchQuery}
									class="w-full bg-white text-sm text-city-green pl-9 pr-3 py-1.5 rounded-lg border-none focus:ring-1 focus:ring-emerald-500 placeholder-gray-500"
								/>
							</search>

							<button onclick={() => { listCreateOpen = true; errorMessage = ''; }} class="flex items-center gap-2 w-full text-left px-2 py-2 text-xs font-bold text-city-green hover:bg-[#2F5D50]/5 rounded-lg transition-colors mb-1">
								<PlusOutline class="w-4 h-4" />
								<span>Új lista létrehozása</span>
							</button>

							<hr class="border-[#2F5D50]/10 my-1" />

							<div class="min-h-0 overflow-y-auto flex-1 space-y-0.5 pr-1">
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
					{:else}

					<nav class="bg-city-cream text-white rounded-2xl shadow-2xl py-2 w-full border border-[#2F5D50]/10">
						<button
							class="w-full text-left px-4 py-2.5 text-sm font-bold text-city-green hover:bg-[#2F5D50]/5 transition-colors"
							onclick={openListSelector}
						>
							<span class="flex items-center justify-between gap-2">
								<span>Mentés listára</span>
								<ChevronRightOutline class="w-4 h-4 shrink-0" />
							</span>
						</button>
						<button
							class="w-full text-left px-4 py-2.5 text-sm font-bold text-city-green hover:bg-[#2F5D50]/5 transition-colors"
							onclick={shareAdventure}
						>
							Kaland megosztása
						</button>
					</nav>
					{/if}
				</div>
			{/if}
		</div>

		{#if errorMessage && !listSelectorOpen}
			<p role="alert" class="alert-error-city mb-4">{errorMessage}</p>
		{/if}

		<div class="space-y-8">

			<section class="space-y-6">
				<hgroup class="border-b border-[#2F5D50]/10 pb-4">
					<h2 class="label-city mb-1">Kaland címe</h2>
					<h1 class="text-2xl font-black text-[#2F5D50] leading-tight">{adventure.title}</h1>
				</hgroup>

				<div class="grid grid-cols-2 gap-4">
					<section class="col-span-2 min-w-0 bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Átlagos értékelés</h2>
						<p class="font-bold text-[#2F5D50] flex items-center gap-2">
							<StarSolid class="w-5 h-5 text-yellow-500" />
							{adventure.averageRating > 0 ? `${Number(adventure.averageRating).toLocaleString('hu-HU', { maximumFractionDigits: 1 })} / 5` : '-'}
						</p>
					</section>
					<section class="min-w-0 bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Időtartam</h2>
						<p class="font-bold text-[#2F5D50]">
							{adventure.averageTime}
						</p>
					</section>

					<section class="min-w-0 bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Kaland hossza</h2>
						<p class="font-bold text-[#2F5D50]">
							{adventure.distanceInMeters == null
								? 'Nincs adat'
								: adventure.distanceInMeters >= 1000
									? `${(adventure.distanceInMeters / 1000).toFixed(1)} km`
									: `${Math.round(adventure.distanceInMeters)} m`}
						</p>
					</section>

					<section class="min-w-0 bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Nehézség</h2>
						<p class="font-black {getDifficultyColor(adventure.difficulty)} uppercase tracking-tighter break-words">
							{translateDifficulty(adventure.difficulty)}
						</p>
					</section>

					<section class="min-w-0 bg-white/60 p-4 rounded-2xl border border-[#2F5D50]/5 shadow-sm">
						<h2 class="label-city mb-1">Készítő</h2>

						<span class="flex items-center gap-2 min-w-0">
							<Avatar
								src={adventure.creatorProfilePictureUrl}
								alt="A készítő profilképe"
								class="w-8 h-8 shrink-0 rounded-full object-cover border border-[#2F5D50]/10"
							/>

							{#if adventure.creatorName && !['Ismeretlen', 'Törölt felhasználó'].includes(adventure.creatorName)}
								<a
									href={`/profile/user/${encodeURIComponent(adventure.creatorName)}`}
									class="min-w-0 font-bold text-[#8D7462] hover:underline [overflow-wrap:anywhere]"
								>
									{adventure.creatorName}
								</a>
							{:else}
								<span class="min-w-0 font-bold text-[#8D7462] [overflow-wrap:anywhere]">
									{adventure.creatorName || 'Ismeretlen'}
								</span>
							{/if}
						</span>
					</section>
				</div>
			</section>

			<section class="space-y-4">
				<h2 class="label-city">Legutóbbi vélemények</h2>

				{#if adventure.reviews && adventure.reviews.length > 0}
					<div class="space-y-4">
						{#each adventure.reviews as review}
							<article class="bg-city-brown p-5 rounded-2xl shadow-sm border border-[#2F5D50]/5">
								<header class="flex justify-between items-start mb-3">
									<div class="flex items-center gap-3">
										<span class="block p-2 rounded-xl">
											<UserCircleSolid class="w-6 h-6 text-city-cream" />
										</span>
										<span class="flex flex-col">
											<span class="font-bold text-city-cream text-sm">{review.authorName || 'Törölt felhasználó'}</span>
											<span class="text-[10px] font-bold text-city-cream opacity-60 uppercase">
              {new Date(review.reviewedAt).toLocaleDateString('hu-HU')}
            </span>
										</span>
									</div>

									<span class="flex bg-city-brown px-2 py-1 rounded-lg gap-0.5">
										{#each Array(5) as _, i}
											<StarSolid
												class="w-3 h-3 {i < review.rating ? 'text-yellow-400' : 'text-gray-300'}"
											/>
										{/each}
									</span>
								</header>

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

		<footer class="fixed bottom-20 left-0 right-0 z-40 mx-auto w-full max-w-md px-6 pt-4 pb-2 bg-gradient-to-t from-[#F5F2EA] via-[#F5F2EA] to-transparent">
			{#if startError}
				<p
					role="alert"
					class="mb-3 rounded-xl bg-red-50 p-3 text-center text-sm font-bold text-red-700"
				>
					{startError}
				</p>
			{/if}

			<button
				type="button"
				class="w-full bg-[#2F5D50] text-white px-4 py-3 rounded-xl font-black text-sm shadow-lg active:scale-[0.97] transition-all uppercase tracking-wider disabled:opacity-50"
				onclick={startAdventure}
				disabled={isStarting}
			>
				{isStarting ? 'Indítás...' : 'Kaland indítása'}
			</button>
		</footer>
	</section>
{/if}

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-widest text-[#2F5D50] opacity-40;
    }
</style>
