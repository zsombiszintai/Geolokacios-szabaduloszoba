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
		return `${h} h ${m} m`;
	}

	async function postReview() {
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
	<div class="relative mb-6">
		<input
			bind:value={searchTerm}
			type="text"
			placeholder="Keress a lejátszott kalandjaid között..."
			class="w-full rounded-lg bg-[#775D4D] p-3 pl-10 text-[#F5F2EA] placeholder-[#F5F2EA]/60 shadow-inner outline-none"
		/>
		<span class="absolute left-3 top-3 opacity-60">🔍</span>
	</div>

	<h2 class="mb-4 text-lg font-bold text-black uppercase tracking-tight">Lejátszott kalandjaid</h2>

	<div class="mb-2 grid grid-cols-3 px-4 text-xs font-bold text-black opacity-80 uppercase">
		<div>Kaland neve</div>
		<div class="text-center">Befejezve</div>
		<div class="text-right">Értékelés</div>
	</div>

	<div class="space-y-3">
		{#each filteredAdventures as adv (adv.id)}
			<button
				onclick={() => { selectedAdv = adv; rating = adv.rating || 0; }}
				class="flex w-full items-center justify-between rounded-xl bg-[#775D4D] p-4 text-[#F5F2EA] shadow-lg transition-transform active:scale-95"
			>
				<div class="w-1/3 text-left font-bold">{adv.adventureTitle}</div>
				<div class="w-1/3 text-center text-sm opacity-90">{adv.completedAt}</div>
				<div class="w-1/3 text-right text-2xl">
					<span class={adv.rating ? 'text-white' : 'opacity-30'}>★</span>
				</div>
			</button>
		{/each}
	</div>

	{#if selectedAdv}
		<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
			<div class="w-full max-w-md overflow-hidden rounded-2xl bg-[#F5F2EA] shadow-2xl border-t-8 border-[#3A5A40]">
				<div class="border-b border-gray-200 bg-white/50 p-4">
					<div class="flex items-start justify-between">
						<h3 class="text-xl font-bold text-[#775D4D]">{selectedAdv.adventureTitle}</h3>
						<button onclick={() => (selectedAdv = null)} class="text-2xl">&times;</button>
					</div>
					<div class="mt-4 grid grid-cols-3 text-center text-[10px] font-bold text-gray-500 uppercase">
						<div>Befejezve<br /><span class="text-sm text-[#775D4D]">{selectedAdv.completedAt}</span></div>
						<div>Megtett út<br /><span class="text-sm text-[#775D4D]">{selectedAdv.distanceTravelled} m</span></div>
						<div>Idő<br /><span class="text-sm text-[#775D4D]">{formatDuration(selectedAdv.durationSec)}</span></div>
					</div>
				</div>

				<div class="p-6">
					<p class="mb-2 font-bold text-[#775D4D]">Értékelés</p>
					<div class="flex justify-center space-x-2 text-4xl mb-6">
						{#each [1, 2, 3, 4, 5] as star}
							<button onclick={() => (rating = star)} class={star <= rating ? 'text-yellow-500' : 'text-gray-300'}>
								★
							</button>
						{/each}
					</div>

					<p class="mb-2 font-bold text-[#775D4D]">Vélemény</p>
					<textarea
						bind:value={reviewText}
						placeholder="Írd le a véleményed a kalandról..."
						class="h-32 w-full rounded-xl bg-[#775D4D] p-3 text-white placeholder-white/50 outline-none"
					></textarea>

					<button
						onclick={postReview}
						class="mt-6 w-full rounded-xl bg-[#3A5A40] py-4 text-lg font-bold text-white shadow-lg active:bg-[#2d4431]"
					>
						Poszt
					</button>
				</div>
			</div>
		</div>
	{/if}
</main>