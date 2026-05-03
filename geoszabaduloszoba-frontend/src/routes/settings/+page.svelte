<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount } from 'svelte';

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
		}
	}

	onMount(fetchCurrentSettings);
</script>

<main class="min-h-screen bg-[#F5F2EA] p-6 font-sans">
	<div class="max-w-md mx-auto space-y-8">
		<h1 class="text-3xl font-bold text-[#2F5D50]">Beállítások</h1>

		<section class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center gap-4">
			<div class="relative group">
				<div class="w-32 h-32 rounded-full overflow-hidden border-4 border-[#2F5D50]/20 shadow-inner">
					{#if profile}
						<img
							src={profile.profilePictureUrl?.startsWith('http')
                   ? profile.profilePictureUrl
                   : `http://localhost:8080${profile.profilePictureUrl}`}
							alt="Avatar"
							class="w-full h-full object-cover"
						/>
					{:else}
						<div class="w-full h-full bg-gray-200 animate-pulse"></div>
					{/if}
				</div>

				{#if uploadLoading}
					<div class="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center">
						<div class="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin"></div>
					</div>
				{/if}
			</div>

			<label class="cursor-pointer bg-[#2F5D50] text-white px-4 py-2 rounded-full font-bold hover:bg-[#23463c] transition-colors active:scale-95">
				{uploadLoading ? 'Feltöltés...' : 'Új fotó választása'}
				<input type="file" accept="image/*" class="hidden" onchange={handleAvatarUpload} disabled={uploadLoading} />
			</label>
		</section>

		<section class="space-y-4">
			<label class="block text-sm font-bold text-gray-700 ml-2">Bemutatkozás</label>
			<textarea
				bind:value={description}
				class="w-full p-4 rounded-xl border-2 border-gray-200 focus:border-[#2F5D50] outline-none min-h-[100px] resize-none"
				placeholder="Mesélj magadról..."
			></textarea>

			<button class="w-full bg-[#2F5D50] text-white py-3 rounded-xl font-bold shadow-lg active:scale-95 transition-transform">
				Mentés
			</button>
		</section>

		{#if message.text}
			<div class="text-center p-3 rounded-lg font-medium {message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'}">
				{message.text}
			</div>
		{/if}
	</div>
</main>