<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/state';
	import {
		ChevronLeftOutline,
		UserCircleSolid,
		StarSolid
	} from 'flowbite-svelte-icons';
	import { goto } from '$app/navigation';

	let adventure = $state<any>(null);
	const userPos = { lat: 46.0754, lon: 18.2205 };

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
				const errorText = await res.text();
				console.log("Szerver hiba- ", "status:",  res.status, "message:" , errorText);
			}
		} catch (e: any) {
			console.log("Hálózati hiba", "message:", e.message);
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
						<p class="font-black text-yellow-600 uppercase tracking-tighter">{adventure.difficulty}</p>
					</div>
					<div class="text-right">
						<h2 class="label-city mb-1">Készítő</h2>
						<div class="flex items-center gap-2 justify-end">
							<span class="font-bold text-[#8D7462]">{adventure.creatorName}</span>
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