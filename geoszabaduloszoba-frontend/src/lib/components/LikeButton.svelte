<script lang="ts">
	import { Heart } from 'lucide-svelte';
	import { auth } from '$lib/auth.svelte';

	let { type, likedId }: { type: 'ADVENTURE' | 'LIST'; likedId: string } = $props();
	const url = $derived(`https://api.zsomborszintai.com/likes/${type}/${encodeURIComponent(likedId)}`);

	let liked = $state<boolean | null>(null);
	let pending = $state(false);
	let failed = $state(false);
	let retry = $state(0);

	$effect(() => {
		const token = auth.token;
		const endpoint = url;
		const controller = new AbortController();
		retry;
		liked = null;
		failed = false;

		if (token) void load(endpoint, token, controller.signal);
		return () => controller.abort();
	});

	async function load(endpoint: string, token: string, signal: AbortSignal) {
		try {
			const response = await fetch(endpoint, {
				headers: { Authorization: `Bearer ${token}` },
				signal
			});
			if (!response.ok) throw new Error();
			const status = await response.json();
			if (typeof status !== 'boolean') throw new Error();
			if (!signal.aborted) liked = status;
		} catch {
			if (!signal.aborted) failed = true;
		}
	}

	async function toggle() {
		const token = auth.token;
		if (!token || pending) return;
		if (liked === null) {
			retry += 1;
			return;
		}

		const next = !liked;
		const endpoint = url;
		pending = true;
		failed = false;
		try {
			const response = await fetch(endpoint, {
				method: next ? 'PUT' : 'DELETE',
				headers: { Authorization: `Bearer ${token}` }
			});
			if (!response.ok) throw new Error();
			if (endpoint === url && auth.token) liked = next;
		} catch {
			if (endpoint === url && auth.token) failed = true;
		} finally {
			pending = false;
		}
	}

	const label = $derived(
		liked === null && failed
			? 'Kedvelés állapotának újratöltése'
			: liked ? 'Kedvelés visszavonása' : 'Kedvelés'
	);
</script>

<div class="relative">
	<button
		type="button"
		class="grid size-10 place-items-center rounded-xl text-[#8D7462] transition-colors hover:bg-[#2F5D50]/5 hover:text-[#2F5D50] disabled:opacity-50"
		aria-label={label}
		title={label}
		aria-pressed={liked ?? undefined}
		disabled={!auth.token || pending || (liked === null && !failed)}
		onclick={toggle}
	>
		<Heart class={liked ? 'size-6 fill-rose-600 text-rose-600' : 'size-6'} aria-hidden="true" />
	</button>
	{#if failed}
		<p class="absolute right-0 top-full z-40 w-max max-w-48 rounded-lg bg-white p-2 text-xs text-red-700 shadow" role="alert">
			A kedvelés kezelése sikertelen. Próbáld újra.
		</p>
	{/if}
</div>
