<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { page } from '$app/state';
	import {
		ChevronLeftOutline,
		UserCircleSolid,
		StarSolid
	} from 'flowbite-svelte-icons';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';

	let adventure = $state<any>(null);
	const userPos = { lat: 46.0754, lon: 18.2205 };

	async function fetchAdventureDetails() {
		if (!auth.token) return;

		try {
			const id = page.params.id;
			const url = `http://localhost:8080/api/adventures/${id}?lat=${userPos.lat}&lon=${userPos.lon}`;

			console.log("Hívás:", url);

			const res = await fetch(url, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				adventure = await res.json();
			} else {
				console.error("Szerver válasz hiba:", res.status);
			}
		} catch (e) {
			console.error("Hálózati hiba:", e);
		}
	}

	$effect(() => {
		if (auth.token) {
			fetchAdventureDetails();
		}
	});
</script>

{#if adventure}
	<main class="flex flex-col min-h-screen bg-[#F5F2EA] font-josefin pb-32">

		<div class="p-6 space-y-6">
			<button class="flex items-center text-gray-600 gap-1 text-lg" onclick={() => history.back()}>
				<ChevronLeftOutline class="w-5 h-5" /> Visszalépés
			</button>

			<section class="space-y-4 text-[#1A1A1A]">
				<div>
					<p class="font-bold text-lg">Cím:</p>
					<h2 class="text-xl">{adventure.title}</h2>
				</div>

				<div>
					<p class="font-bold text-lg">Átlag teljesítési idő:</p>
					<p class="text-lg">{adventure.averageTime} s</p>
				</div>

				<div>
					<p class="font-bold text-lg">Táv:</p>
					<p class="text-lg">{adventure.totalDistanceInMeters}m</p>
				</div>

				<div>
					<p class="font-bold text-lg">Nehézségi szint:</p>
					<p class="text-red-900 font-bold text-lg">{adventure.difficulty}</p>
				</div>

				<div>
					<p class="font-bold text-lg">Készítő:</p>
					<div class="flex items-center gap-2 mt-1">
						<UserCircleSolid class="w-8 h-8 text-gray-400" />
						<span class="text-lg">{adventure.creatorName}</span>
					</div>
				</div>
			</section>

			<div class="border-t border-gray-300 pt-4">
				<h3 class="font-bold text-lg mb-4">Értékelések</h3>

				<div class="flex items-end gap-2 h-20 px-2 relative">
					{#each adventure.ratingDistribution as count, i}
						<div
							class="bg-[#8D7462] w-full rounded-t-sm transition-all duration-500"
							style="height: {count}%"
						></div>
					{/each}
					<span class="text-2xl font-bold ml-2">{adventure.averageRating}</span>
				</div>
			</div>

			<div class="space-y-3">
				<h3 class="text-black-city">Legutóbbi vélemények</h3>
				<div class="bg-[#8D7462] p-4 rounded-xl text-[#F5F2EA] shadow-md">
					<div class="flex justify-between items-center mb-1">
						<div class="flex items-center gap-2">
							<UserCircleSolid class="w-6 h-6" />
							<span class="font-bold text-sm">Felhasználónév</span>
						</div>
						<div class="flex text-yellow-300">
							{#each Array(5) as _} <StarSolid class="w-4 h-4" /> {/each}
						</div>
					</div>
					<p class="text-xs italic opacity-90">Rövid vélemény...</p>
				</div>
			</div>
		</div>

		<div class="fixed bottom-24 left-0 right-0 px-8">
			<button class="w-full bg-[#2F5D50] text-white py-4 rounded-xl font-bold text-xl shadow-2xl active:scale-95 transition-all"
							onclick={() => goto(`/game?id=${adventure.id}`)}>
				Indítás
			</button>
		</div>
	</main>
{/if}