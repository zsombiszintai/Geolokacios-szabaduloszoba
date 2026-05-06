<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount, untrack } from 'svelte';
	import { page } from '$app/state';
	import { ChevronRightOutline } from 'flowbite-svelte-icons';

	let stats = $state<any>(null);
	let loading = $state(true);
	let isFollowing = $state(false);
	let followLoading = $state(false);

	const usernameParam = $derived(page?.params?.username || null);
	const isOwnProfile = $derived(
		!usernameParam ||
		usernameParam === 'me' ||
		(stats && auth.username && stats.username === auth.username)
	);

	async function checkFollowStatus() {
		if (isOwnProfile || !stats?.id) return;
		try {
			const res = await fetch(`http://localhost:8080/follows/is-following/${stats.id}`, {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) isFollowing = await res.json();
		} catch (err) {
			console.error("Hiba a követési állapot lekérdezésekor:", err);
		}
	}

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
			usernameParam;
			fetchProfile();
		}
	});

	$effect(() => {
		if (stats?.id && !isOwnProfile) {
			untrack(() => checkFollowStatus());
		}
	});

	async function toggleFollow() {
		if (!stats?.id || followLoading) return;
		followLoading = true;

		const method = isFollowing ? 'DELETE' : 'POST';
		try {
			const res = await fetch(`http://localhost:8080/follows/${stats.id}`, {
				method,
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});

			if (res.ok) {
				isFollowing = !isFollowing;
			}
		} finally {
			followLoading = false;
		}
	}
</script>

<main class="min-h-screen bg-[#F5F2EA] font-josefin pb-24 px-6 pt-12">
	{#if stats}
		<section class="flex flex-col items-center text-center mb-8">
			<div class="relative mb-4">
				<div class="w-28 h-28 bg-white rounded-full border-4 border-[#2F5D50]/10 shadow-xl flex items-center justify-center overflow-hidden">
					<img
						src={stats.profilePictureUrl ? (
						stats.profilePictureUrl.startsWith('http')
							? stats.profilePictureUrl
							: `http://localhost:8080/images/${stats.profilePictureUrl}`
							) : 'http://localhost:8080/images/default-avatar.png'}
								alt="Avatar"
								class="w-full h-full object-cover"
								onerror={(e) => {
									const target = e.currentTarget;
									const defaultSrc = 'http://localhost:8080/images/default-avatar.png';
									if (target.src !== defaultSrc) {
											target.src = defaultSrc;

									}
     						}}
					/>
				</div>
			</div>

			<h1 class="text-3xl font-black text-[#2F5D50] tracking-tight mb-1">{stats.username}</h1>

			{#if !isOwnProfile}
				<button
					onclick={toggleFollow}
					disabled={followLoading}
					class="mt-2 px-8 py-2 rounded-2xl font-black text-sm transition-all active:scale-95 uppercase tracking-widest {isFollowing ? 'bg-white text-[#8D7462] border-2 border-[#8D7462]/20' : 'bg-[#2F5D50] text-white shadow-lg'}"
				>
					{followLoading ? '...' : (isFollowing ? 'Kikövetés' : 'Követés')}
				</button>
			{/if}
		</section>

		<div class="bg-white/60 backdrop-blur-sm p-6 rounded-3xl border border-[#2F5D50]/5 shadow-sm mb-8 relative overflow-hidden">
			<div class="absolute top-0 left-0 w-1 h-full bg-[#8D7462]"></div>
			<h2 class="label-city mb-2">Bemutatkozás</h2>
			<p class="text-[#2F5D50] font-medium italic leading-relaxed">
				{stats.profileDescription || "Leírás..."}
			</p>
		</div>

		<nav class="space-y-3">
			<h2 class="label-city ml-2 mb-4">Statisztikák</h2>
			{#each [
				{ label: 'Lejátszott kalandok', key: 'completed-adventure', count: stats.completedCount, color: 'bg-city-brown/90' },
				{ label: 'Félbehagyott kalandok', key: 'abandoned-adventure', count: stats.abandonedCount, color: 'bg-city-brown/90' },
				{ label: 'Saját kalandok', key: 'created', count: stats.ownedCount, color: 'bg-city-brown/90' },
				{ label: 'Értékelt kalandok', key: 'rated', count: stats.ratedCount, color: 'bg-city-brown/90' },
				{ label: 'Vélemények', key: 'reviewed', count: stats.reviewsCount, color: 'bg-city-brown/90' },
				{ label: 'Követők', key: 'followers', count: stats.followerCount, color: 'bg-city-brown/90' },
				{ label: 'Követés', key: 'following', count: stats.followingCount, color: 'bg-city-brown/90' }
			] as item}
				<a
					href="/profile/list/{item.key}"
					class="flex items-center justify-between p-5 rounded-2xl border border-[#2F5D50]/5 shadow-sm transition-all active:scale-[0.98] {item.color === 'bg-white' ? 'bg-white/80' : item.color + ' text-white'}"
				>
					<span class="font-bold uppercase tracking-wider text-sm {item.color === 'bg-white' ? 'text-[#2F5D50]' : 'text-[#F5F2EA]'}">{item.label}</span>
					<div class="flex items-center gap-3">
						<span class="text-2xl font-black">{item.count || 0}</span>
						<ChevronRightOutline class="w-5 h-5 opacity-40" />
					</div>
				</a>
			{/each}
		</nav>
	{:else}
		<div class="flex flex-col justify-center items-center h-[60vh] gap-4">
			<div class="w-12 h-12 border-4 border-[#2F5D50] border-t-transparent rounded-full animate-spin"></div>
			<p class="font-bold text-[#2F5D50] animate-pulse uppercase tracking-widest text-xs">Profil betöltése...</p>
		</div>
	{/if}
</main>

<style>
    .label-city {
        @apply text-[10px] font-black uppercase tracking-[0.2em] text-[#2F5D50] opacity-40;
    }
</style>