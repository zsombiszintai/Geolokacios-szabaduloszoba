<script lang="ts">
	import { onMount } from 'svelte';
	import { auth } from '$lib/auth.svelte';
	import { replaceState } from '$app/navigation';

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
		e.preventDefault(); // Ez helyettesíti a régi |preventDefault módosítót
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
</script>

<main class="min-h-screen bg-[#F5F2EA] p-4 font-sans">
	<section class="relative mb-6">
		<label for="adventure-search" class="sr-only">Kaland keresése</label>
		<input
			id="adventure-search"
			bind:value={searchTerm}
			type="search"
			placeholder="Keress a lejátszott kalandjaid között..."
			class="w-full rounded-lg bg-[#775D4D] p-3 pl-10 text-[#F5F2EA] placeholder-[#F5F2EA]/60 shadow-inner outline-none"
		/>
		<span class="absolute left-3 top-3 opacity-60" aria-hidden="true">🔍</span>
	</section>

	<h2 class="mb-4 text-lg font-bold text-black uppercase tracking-tight">Lejátszott kalandjaid</h2>

	<header class="mb-2 grid grid-cols-3 px-4 text-xs font-bold text-black opacity-80 uppercase">
		<span>Kaland neve</span>
		<span class="text-center">Befejezve</span>
		<span class="text-right">Értékelés</span>
	</header>

	<section class="space-y-3" aria-label="Befejezett kalandok listája">
		{#each filteredAdventures as adv (adv.id)}
			<button
				onclick={() => { selectedAdv = adv; rating = adv.rating || 0; }}
				class="grid w-full grid-cols-3 items-center rounded-xl bg-[#775D4D] p-5 text-[#F5F2EA] shadow-xl transition-all hover:brightness-105 active:scale-95"
			>
				<article class="text-left">
					<h3 class="font-bold text-xl tracking-tight leading-tight">{adv.adventureTitle}</h3>
				</article>

				<div class="text-center">
					<time datetime={adv.completedAt} class="text-sm opacity-90 font-medium">{adv.completedAt}</time>
				</div>

				<figure class="flex items-center justify-end m-0">
					{#if adv.rating}
						{@html refreshIcon}
						<span class="text-3xl text-white" aria-label="{adv.rating} csillagos értékelés">★</span>
					{:else}
						<span class="text-3xl opacity-30" aria-label="Még nincs értékelve">★</span>
					{/if}
				</figure>
			</button>
		{/each}
	</section>

	{#if selectedAdv}
		<dialog open class="fixed inset-0 z-50 flex h-full w-full items-center justify-center bg-black/60 p-4 backdrop-blur-sm border-none">
			<article class="w-full max-w-md overflow-hidden rounded-2xl bg-[#F5F2EA] shadow-2xl border-t-8 border-[#3A5A40]">

				<header class="border-b border-gray-200 bg-white/50 p-4">
					<div class="flex items-start justify-between">
						<h3 class="text-xl font-bold text-[#775D4D]">{selectedAdv.adventureTitle}</h3>
						<button onclick={() => (selectedAdv = null)} class="text-2xl" aria-label="Bezárás">&times;</button>
					</div>

					<dl class="mt-4 grid grid-cols-3 text-center text-[10px] font-bold text-gray-500 uppercase">
						<div>
							<dt>Befejezve</dt>
							<dd class="text-sm text-[#775D4D] m-0">{selectedAdv.completedAt}</dd>
						</div>
						<div>
							<dt>Megtett út</dt>
							<dd class="text-sm text-[#775D4D] m-0">{selectedAdv.distanceTravelled} m</dd>
						</div>
						<div>
							<dt>Idő</dt>
							<dd class="text-sm text-[#775D4D] m-0">{formatDuration(selectedAdv.durationSec)}</dd>
						</div>
					</dl>
				</header>

				<form method="dialog" class="p-6" onsubmit={postReview}>
					<fieldset class="border-none p-0 m-0">
						<legend class="mb-2 font-bold text-[#775D4D]">Értékelés</legend>
						<nav class="flex justify-center space-x-2 text-4xl mb-6" aria-label="Csillagos értékelés választó">
							{#each [1, 2, 3, 4, 5] as star}
								<button
									type="button"
									onclick={() => (rating = star)}
									class={star <= rating ? 'text-yellow-500' : 'text-gray-300'}
									aria-label="{star} csillag"
								>
									★
								</button>
							{/each}
						</nav>

						<label for="review-text" class="block mb-2 font-bold text-[#775D4D]">Vélemény</label>
						<textarea
							id="review-text"
							bind:value={reviewText}
							placeholder="Írd le a véleményed a kalandról..."
							class="h-32 w-full rounded-xl bg-[#775D4D] p-3 text-white placeholder-white/50 outline-none"
						></textarea>
					</fieldset>

					<button
						type="submit"
						class="mt-6 w-full rounded-xl bg-[#3A5A40] py-4 text-lg font-bold text-white shadow-lg active:bg-[#2d4431]"
					>
						{selectedAdv.rating ? 'Módosítás' : 'Poszt'}
					</button>
				</form>
			</article>
		</dialog>
	{/if}
</main>