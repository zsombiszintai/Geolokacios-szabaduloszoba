<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { StarSolid, RefreshOutline } from 'flowbite-svelte-icons';

	interface CompletedAdventure {
		id: number;
		adventureId: number;
		adventureTitle: string;
		completedAt: string;
		distanceTravelled: number;
		durationSec: number;
		rating: number | null;
	}

	const refreshIcon = `
    <svg viewBox="0 0 24 24" class="inline-block w-6 h-6 mr-3 text-white transition-all hover:text-yellow-400" fill="none" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.992 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
    </svg>
  `;

	let adventures = $state<CompletedAdventure[]>([]);
	let selectedAdv = $state<CompletedAdventure | null>(null);
	let rating = $state(0);
	let reviewText = $state('');
	let searchTerm = $state('');

	let filteredAdventures = $derived(
		adventures.filter((a) => a.adventureTitle.toLowerCase().includes(searchTerm.toLowerCase()))
	);

	onMount(async () => {
		const res = await fetch('http://localhost:8080/api/completed-adventures', {
			headers: { Authorization: `Bearer ${auth.token}` }
		});
		if (res.ok) adventures = await res.json();
	});

	function formatDuration(seconds: number) {
		const h = Math.floor(seconds / 3600);
		const m = Math.floor((seconds % 3600) / 60);
		return h > 0 ? `${h} h ${m} m` : `${m} m`;
	}

	async function postReview(e: SubmitEvent) {
		e.preventDefault();
		if (!selectedAdv) return;

		const res = await fetch('http://localhost:8080/api/reviews', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				Authorization: `Bearer ${auth.token}`
			},
			body: JSON.stringify({
				adventureId: selectedAdv.adventureId,
				rating,
				reviewText
			})
		});

		if (res.ok) {
			adventures = adventures.map((a) => (a.id === selectedAdv?.id ? { ...a, rating } : a));
			selectedAdv = null;
		}
	}

	function formatDistance(meters: number) {
		return meters.toFixed(2);
	}

</script>

<main class="min-h-screen bg-[#F5F2EA] font-josefin pb-24 px-6 pt-12">

	<section class="relative mb-10 max-w-md mx-auto">
    <span class="absolute left-4 top-1/2 -translate-y-1/2 opacity-30">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2F5D50" stroke-width="3">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
      </svg>
    </span>
		<input
			id="adventure-search"
			bind:value={searchTerm}
			type="search"
			placeholder="Keress a kalandjaid között..."
			class="w-full h-12 pl-12 pr-4 bg-white/80 rounded-2xl border-b-4 border-[#2F5D50]/20 outline-none focus:border-[#2F5D50] transition-all text-[#2F5D50] font-bold shadow-sm"
		/>
	</section>

	<header class="mb-6">
		<h1 class="text-3xl font-black text-[#2F5D50] uppercase tracking-tight leading-none mb-2">
			Lejátszott<br/>Kalandok
		</h1>
		<div class="w-12 h-1.5 bg-[#8D7462] rounded-full"></div>
	</header>

	<header class="grid grid-cols-[2fr_1fr_1fr] px-4 mb-3 text-[10px] font-black text-[#2F5D50] opacity-40 uppercase tracking-[0.2em]">
		<span>Kaland neve</span>
		<span class="text-center">Dátum</span>
		<span class="text-right">Értékelés</span>
	</header>

	<section class="space-y-4">
		{#each filteredAdventures as adv (adv.id)}
			<button
				onclick={() => { selectedAdv = adv; rating = adv.rating || 0; }}
				class="grid grid-cols-[2fr_1fr_1fr] w-full items-center bg-[#8D7462]/90 p-5 rounded-3xl shadow-xl transition-all active:scale-[0.98] text-left border border-white/10"
			>
				<div class="overflow-hidden">
					<div class="flex items-center gap-2">
						<h3 class="font-black text-white text-lg leading-tight truncate">{adv.adventureTitle}</h3>
					</div>
				</div>

				<div class="text-center">
					<time datetime={adv.completedAt} class="text-xs font-bold text-[#F5F2EA]/80">
						{new Date(adv.completedAt).toLocaleDateString('hu-HU')}
					</time>
				</div>

				<div class="flex justify-end items-center gap-1">
					{#if adv.rating}
						<RefreshOutline class="w-6 h-6 text-city-cream"/>
					{/if}
					<div class="bg-[#8D7462]/90 px-2 py-1 rounded-lg flex items-center gap-1 shadow-inner border border-white/10">
						<StarSolid class="w-6 h-6 text-yellow-400" />
					</div>
				</div>
			</button>
		{/each}
	</section>

	{#if selectedAdv}
		<dialog open class="fixed inset-0 z-[2000] flex h-full w-full items-center justify-center bg-black/60 p-6 backdrop-blur-sm border-none">
			<article class="w-full max-w-sm overflow-hidden rounded-[2.5rem] bg-[#F5F2EA] shadow-2xl border-2 border-[#8D7462]">

				<header class="p-8 pb-4">
					<div class="flex items-start justify-between mb-6">
						<div>
							<h3 class="text-2xl font-black text-[#2F5D50] leading-tight">{selectedAdv.adventureTitle}</h3>
							{#if selectedAdv.rating}
								<div class="flex items-center gap-1 mt-1 text-[#8D7462]">
									<RefreshOutline class="w-4 h-4 text-city-brown"/>
									<span class="text-[10px] font-black uppercase tracking-widest">Értékeld újra</span>
								</div>
							{/if}
						</div>
						<button onclick={() => (selectedAdv = null)} class="p-2 bg-white rounded-xl shadow-sm text-[#8D7462]" aria-label="Bezárás">
							<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6L6 18M6 6l12 12"/></svg>
						</button>
					</div>

					<div class="flex flex-wrap gap-2 justify-center py-4">
							<div class="bg-white/60 px-4 py-2 rounded-full border border-[#2F5D50]/10 flex flex-col items-center min-w-[80px]">
								<p class="label-city !opacity-60 !tracking-tighter">Távolság</p>
								<p class="text-sm font-black text-[#2F5D50]">{formatDistance(selectedAdv.distanceTravelled)} m</p>
							</div>
						<div class="bg-white/60 px-4 py-2 rounded-full border border-[#2F5D50]/10 flex flex-col items-center min-w-[80px]">
							<p class="label-city !opacity-60 !tracking-tighter">Időtartam</p>
							<p class="text-sm font-black text-[#2F5D50]">{formatDuration(selectedAdv.durationSec)}</p>
						</div>
						<div class="bg-white/60 px-4 py-2 rounded-full border border-[#2F5D50]/10 flex flex-col items-center min-w-[80px]">
							<p class="label-city !opacity-60 !tracking-tighter">Dátum</p>
							<p class="text-[10px] font-black text-[#2F5D50]">{selectedAdv.completedAt}</p>
						</div>
					</div>
				</header>

				<form method="dialog" class="p-8 pt-4" onsubmit={postReview}>
					<div class="mb-6 text-center">
						<p class="label-city mb-4">Milyen volt a kaland?</p>
						<div class="flex justify-center gap-2">
							{#each [1, 2, 3, 4, 5] as star}
								<button
									type="button"
									onclick={() => (rating = star)}
									class="text-4xl transition-all {star <= rating ? 'text-yellow-400 scale-110' : 'text-gray-300 opacity-40'}"
								>
									★
								</button>
							{/each}
						</div>
					</div>

					<div class="space-y-2">
						<label for="review-text" class="label-city ml-1">Véleményed</label>
						<textarea
							id="review-text"
							bind:value={reviewText}
							placeholder="Meséld el a tapasztalataidat..."
							class="w-full h-32 rounded-2xl bg-white p-4 text-[#2F5D50] font-medium placeholder-[#2F5D50]/30 outline-none border border-[#2F5D50]/5 focus:border-[#2F5D50] transition-all resize-none shadow-inner"
						></textarea>
					</div>

					<button
						type="submit"
						class="mt-8 w-full bg-[#2F5D50] text-white py-4 rounded-2xl font-black uppercase tracking-widest shadow-xl active:scale-95 transition-all"
					>
						{selectedAdv.rating ? 'Értékelés frissítése' : 'Értékelés küldése'}
					</button>
				</form>
			</article>
		</dialog>
	{/if}
</main>

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-[0.2em] text-[#2F5D50] opacity-40;
    }
</style>