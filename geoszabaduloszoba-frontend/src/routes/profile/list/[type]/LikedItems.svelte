<script lang="ts">
	import { ChevronRightOutline } from 'flowbite-svelte-icons';
	import { auth } from '$lib/auth.svelte';
	import LoadingState from '$lib/components/LoadingState.svelte';

	type LikeType = 'ADVENTURE' | 'LIST';
	type LikedItem = { id: number; title: string; description: string | null };

	const tabs = [
		{ type: 'ADVENTURE', label: 'Kalandok' },
		{ type: 'LIST', label: 'Listák' }
	] as const;

	let selected = $state<LikeType>('ADVENTURE');
	let items = $state<LikedItem[]>([]);
	let loading = $state(true);
	let error = $state('');
	let retry = $state(0);

	$effect(() => {
		const type = selected;
		const token = auth.token;
		retry;
		items = [];
		error = '';
		loading = Boolean(token);

		if (!token) return;
		const controller = new AbortController();
		void loadLikes(type, token, controller.signal);
		return () => controller.abort();
	});

	async function loadLikes(type: LikeType, token: string, signal: AbortSignal) {
		try {
			const response = await fetch(`https://api.zsomborszintai.com/likes/mine/${type}`, {
				headers: { Authorization: `Bearer ${token}` },
				signal
			});
			if (!response.ok) throw new Error();

			const data: LikedItem[] = await response.json();
			if (!signal.aborted) items = data;
		} catch {
			if (!signal.aborted) error = 'A kedvelések betöltése sikertelen.';
		} finally {
			if (!signal.aborted) loading = false;
		}
	}
</script>

	<div class="mx-auto max-w-md">
		<div class="mb-6 grid grid-cols-2 gap-1 rounded-2xl bg-white p-1" role="group" aria-label="Kedvelés típusa">
			{#each tabs as tab (tab.type)}
				<button
					type="button"
					aria-pressed={selected === tab.type}
					class="rounded-xl px-4 py-3 text-sm font-bold transition-colors {selected === tab.type
						? 'bg-city-green text-city-cream'
						: 'text-city-green hover:bg-city-green/5'}"
					onclick={() => selected = tab.type}
				>
					{tab.label}
				</button>
			{/each}
		</div>

		{#if loading}
			<LoadingState message="Kedvelések betöltése..." />
		{:else if !auth.token}
			<p class="rounded-2xl bg-white p-5 text-city-green">A kedvelések megtekintéséhez jelentkezz be.</p>
		{:else if error}
			<div class="rounded-2xl bg-white p-5 text-city-green">
				<p role="alert">{error}</p>
				<button type="button" class="mt-3 font-bold underline" onclick={() => retry += 1}>
					Újrapróbálás
				</button>
			</div>
		{:else if items.length === 0}
			<p class="rounded-2xl bg-white p-5 text-city-green">
				{selected === 'ADVENTURE' ? 'Még nincs kedvelt kalandod.' : 'Még nincs kedvelt listád.'}
			</p>
		{:else}
			<ul class="space-y-3">
				{#each items as item (item.id)}
					<li>
						<a
							href={selected === 'ADVENTURE' ? `/adventures/${item.id}` : `/list/${item.id}`}
							class="flex items-center justify-between gap-4 rounded-2xl bg-white p-5 text-city-green shadow-sm transition-colors hover:bg-city-green/5"
						>
							<div class="min-w-0">
								<h2 class="break-words font-bold">{item.title}</h2>
								{#if item.description}
									<p class="mt-1 line-clamp-2 break-words text-sm">{item.description}</p>
								{/if}
							</div>
							<ChevronRightOutline class="size-5 shrink-0" aria-hidden="true" />
						</a>
					</li>
				{/each}
			</ul>
		{/if}
	</div>
