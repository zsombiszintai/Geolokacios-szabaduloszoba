<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { CameraPhotoOutline } from 'flowbite-svelte-icons';

	let profile = $state<any>(null);
	let uploadLoading = $state(false);
	let description = $state("");
	let message = $state({ text: "", type: "" });

	async function fetchCurrentSettings() {
		if (!auth.token) return;
		try {
			const res = await fetch('http://localhost:8080/profile/me', {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				profile = await res.json();
				description = profile.profileDescription || "";
			}
		} catch (err) {
			console.error("Hiba a betöltéskor:", err);
		}
	}

	async function handleAvatarUpload(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files || input.files.length === 0) return;

		const file = input.files[0];
		const formData = new FormData();
		formData.append('file', file);

		uploadLoading = true;
		message = { text: "Feltöltés...", type: "info" };

		try {
			const res = await fetch('http://localhost:8080/settings/avatar', {
				method: 'POST',
				headers: { 'Authorization': `Bearer ${auth.token}` },
				body: formData
			});

			if (res.ok) {
				const data = await res.json();
				profile.profilePictureUrl = data.avatarUrl;
				message = { text: "Profilkép sikeresen frissítve!", type: "success" };
			} else {
				message = { text: "Hiba történt a feltöltés során.", type: "error" };
			}
		} catch (err) {
			message = { text: "Hálózati hiba történt.", type: "error" };
		} finally {
			uploadLoading = false;
			input.value = "";
			setTimeout(() => message = { text: "", type: "" }, 3000);
		}
	}

	onMount(fetchCurrentSettings);
</script>

<main class="min-h-screen bg-[#F5F2EA] font-josefin pb-24 px-6 pt-6">
	<button
		type="button"
		class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group mb-8"
		onclick={() => window.history.back()}
	>
		<div class="p-2 rounded-xl bg-white shadow-sm group-hover:shadow-md transition-all">
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</div>
		<span class="text-[11px] font-black uppercase tracking-widest">Vissza a profilra</span>
	</button>

	<header class="mb-8">
		<h2 class="text-3xl font-black text-[#2F5D50] leading-none uppercase tracking-tighter">
			Beállítások
		</h2>
		<div class="w-12 h-1.5 bg-[#8D7462] mt-4 rounded-full"></div>
	</header>

	<div class="max-w-md mx-auto space-y-6">

		<section class="bg-white p-8 rounded-[40px] shadow-sm border border-[#2F5D50]/5 flex flex-col items-center">
			<div class="relative group">
				<div class="w-32 h-32 rounded-full overflow-hidden border-4 border-[#F5F2EA] shadow-lg rotate-3 group-hover:rotate-0 transition-transform duration-500">
					{#if profile}
						<img
							src={profile.profilePictureUrl?.startsWith('http')
                         ? profile.profilePictureUrl
                         : `http://localhost:8080${profile.profilePictureUrl}`}
							alt="Avatar"
							class="w-full h-full object-cover"
						/>
					{:else}
						<div class="w-full h-full bg-[#8D7462]/10 animate-pulse"></div>
					{/if}
				</div>

				<label class="absolute -bottom-2 -right-2 bg-[#2F5D50] text-white p-3 rounded-3xl shadow-xl cursor-pointer hover:scale-110 active:scale-90 transition-all border-4 border-white">
					<CameraPhotoOutline class="w-5 h-5" />
					<input type="file" accept="image/*" class="hidden" onchange={handleAvatarUpload} disabled={uploadLoading} />
				</label>

				{#if uploadLoading}
					<div class="absolute inset-0 bg-white/60 rounded-[32px] flex items-center justify-center backdrop-blur-[2px]">
						<div class="w-8 h-8 border-4 border-[#2F5D50] border-t-transparent rounded-full animate-spin"></div>
					</div>
				{/if}
			</div>

			<p class="mt-6 text-[10px] font-black text-[#8D7462] uppercase tracking-[0.2em]">Profilkép módosítása</p>
		</section>

		<section class="bg-white p-6 rounded-[40px] shadow-sm border border-[#2F5D50]/5 space-y-5">
			<div>
				<label class="block text-[11px] font-black text-[#8D7462] uppercase tracking-widest mb-3 ml-2">Bemutatkozás</label>
				<textarea
					bind:value={description}
					rows="4"
					class="w-full p-5 rounded-[24px] bg-[#F5F2EA]/50 border-2 border-transparent focus:border-[#2F5D50]/20 focus:bg-white outline-none transition-all resize-none text-[#2F5D50] font-medium"
					placeholder="Írj magadról néhány szót..."
				></textarea>
			</div>

			<button
				class="w-full bg-[#2F5D50] text-[#F5F2EA] py-4 rounded-[20px] font-black uppercase tracking-widest shadow-lg shadow-[#2F5D50]/20 hover:bg-[#1e3d34] active:scale-[0.97] transition-all flex items-center justify-center gap-2"
			>
				Változtatások mentése
			</button>
		</section>
	</div>
	{#if message.text}
		<div
			transition:fly={{ y: 20, duration: 500 }}
			class="fixed bottom-20 left-6 right-6 flex justify-center z-50"
		>
			<div class="px-6 py-3 rounded-2xl shadow-2xl font-black uppercase tracking-widest text-[10px] flex items-center gap-3
          {message.type === 'success' ? 'bg-[#2F5D50] text-white' : 'bg-[#8D7462] text-white'}">
				{#if message.type === 'info'}
					<div class="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
				{/if}
				{message.text}
			</div>
		</div>
	{/if}
</main>

<style>
    :global(body) {
        background-color: #F5F2EA;
    }
</style>