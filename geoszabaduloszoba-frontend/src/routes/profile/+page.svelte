<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { onMount } from 'svelte';
	import { page } from '$app/state';

	let stats = $state<any>(null);
	let loading = $state(true);

	const usernameParam = $derived(page?.params?.username || null);

	async function fetchProfile() {
		if (!auth.token) return;

		loading = true;
		const baseUrl = 'http://localhost:8080/profile';
		const url = usernameParam
			? `${baseUrl}/user/${usernameParam}`
			: `${baseUrl}/me`;

		try {
			const res = await fetch(url, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				stats = await res.json();
			}
		} catch (err) {
			console.error("Hiba a profil betöltésekor:", err);
		} finally {
			loading = false;
		}
	}

	$effect(() => {
		if (auth.token) {
			fetchProfile();
		}
	});
</script>

<main class="min-h-screen bg-[#F5F2EA] font-sans pb-24 px-6 pt-24">
	{#if stats}
		<section class="flex items-center gap-5 py-4">
			<div class="w-20 h-20 bg-white rounded-full border-2 border-gray-200 shadow-md flex items-center justify-center overflow-hidden">
				{#if stats.profilePictureUrl}
					<img src={stats.profilePictureUrl} alt="Avatar" class="w-full h-full object-cover" />
				{:else}
					<span class="text-4xl">👤</span>
				{/if}
			</div>
			<span class="text-xl font-bold text-black tracking-tight">@{stats.username}</span>
		</section>

		<div class="bg-[#2F5D50] text-[#F5F2EA] p-4 rounded-xl shadow-[0_4px_10px_rgba(0,0,0,0.3)] min-h-[60px] text-lg font-medium italic">
			{stats.profileDescription || "Leírás..."}
		</div>

		<nav class="space-y-3 pt-2">
			{#each [
				{ label: 'Lejátszott kalandok', key: 'completed-adventure', count: stats.completedCount },
				{ label: 'Félbehagyott kalandok', key: 'abandoned-adventure', count: stats.abandonedCount },
				{ label: 'Saját kalandok', key: 'created', count: stats.ownedCount },
				{ label: 'Értékelt kalandok', key: 'rated', count: stats.ratedCount },
				{ label: 'Vélemények', key: 'reviewed', count: stats.reviewsCount }
			] as item}
				<a href="/profile/list/{item.key}" class="adventure-card block no-underline">
					<span class="text-cream-city">{item.label}</span>
					<div class="flex items-center gap-4">
						<span class="text-xl font-bold">{item.count || 0}</span>
						<span class="opacity-60">❯</span>
					</div>
				</a>
			{/each}
		</nav>
	{:else}
		<div class="flex justify-center items-center h-64 italic text-[#2F5D50]">
			Profil betöltése...
		</div>
	{/if}
</main>