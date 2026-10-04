<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { page } from '$app/state';
	import { ChevronRightOutline } from 'flowbite-svelte-icons';

	type AdventureSummary = {
		id: number;
		title: string;
		description: string | null;
		difficulty: string | null;
	};

	type ListDetails = {
		id: number;
		title: string;
		description: string | null;
		adventures: AdventureSummary[];
	};

	let list = $state<ListDetails | null>(null);
	let loading = $state(true);
	let error = $state('');
	let retry = $state(0);

	async function fetchList(
		id: string,
		token: string,
		signal: AbortSignal
	) {
		try {
			const res = await fetch(
				`https://api.zsomborszintai.com/lists/${encodeURIComponent(id)}/details`,
				{
					headers: {
						Authorization: `Bearer ${token}`
					},
					signal
				}
			);

			if (!res.ok) {
				const messages: Record<number, string> = {
					401: 'A lista megtekintéséhez jelentkezz be újra.',
					403: 'Nincs jogosultságod a lista megtekintéséhez.',
					404: 'A lista nem található.'
				};

				throw new Error(
					messages[res.status] ||
					`A lista betöltése sikertelen (HTTP ${res.status}).`
				);
			}

			const data: ListDetails = await res.json();

			if (!data || !Array.isArray(data.adventures)) {
				throw new Error('A szerver hibás listaadatokat küldött.');
			}

			if (!signal.aborted) {
				list = data;
			}
		} catch (err) {
			if (signal.aborted) return;

			error = err instanceof Error
				? err.message
				: 'A lista betöltése sikertelen.';
		} finally {
			if (!signal.aborted) {
				loading = false;
			}
		}
	}

	$effect(() => {
		const id = page.params.id;
		const token = auth.token;
		retry;

		list = null;
		error = '';
		loading = true;

		if (!id || !/^[1-9]\d*$/.test(id)) {
			error = 'Érvénytelen listaazonosító.';
			loading = false;
			return;
		}

		if (!token) {
			error = 'A lista megtekintéséhez jelentkezz be.';
			loading = false;
			return;
		}

		const controller = new AbortController();

		void fetchList(id, token, controller.signal);

		return () => controller.abort();
	});
</script>

<svelte:head>
	<title>{list?.title || 'Lista'} | CityScape</title>
</svelte:head>

<main class="min-h-screen bg-[#F5F2EA] font-josefin px-6 pt-6 pb-24">
	<div class="max-w-md mx-auto">
		<a
			href="/map"
			class="inline-flex items-center gap-2 mb-8 text-[#8D7462] font-black text-xs uppercase tracking-widest"
		>
			<span aria-hidden="true">←</span>
			Vissza a térképre
		</a>

		{#if loading}
			<div class="flex flex-col items-center justify-center py-24 gap-4">
				<div
					class="w-10 h-10 rounded-full border-4 border-[#2F5D50] border-t-transparent animate-spin"
				></div>

				<p class="text-[#2F5D50] font-bold" role="status">
					Lista betöltése...
				</p>
			</div>
		{:else if error}
			<div class="bg-white rounded-3xl p-8 text-center shadow-sm">
				<p class="text-[#2F5D50] font-bold mb-5" role="alert">
					{error}
				</p>

				<button
					type="button"
					onclick={() => retry += 1}
					class="bg-[#2F5D50] text-white px-6 py-3 rounded-xl font-bold"
				>
					Újrapróbálás
				</button>
			</div>
		{:else if list}
			<header class="mb-8">
				<p class="text-[10px] font-black uppercase tracking-[0.2em] text-[#8D7462] mb-3">
					Kalandlista
				</p>

				<h1 class="text-3xl font-black text-[#2F5D50] leading-tight break-words">
					{list.title}
				</h1>

				<div class="w-12 h-1.5 bg-[#8D7462] mt-4 rounded-full"></div>
			</header>

			{#if list.description}
				<section class="bg-white/60 p-6 rounded-3xl mb-8 border border-[#2F5D50]/5">
					<h2 class="text-[10px] font-black uppercase tracking-widest text-[#8D7462] mb-3">
						A listáról
					</h2>

					<p class="text-[#2F5D50] leading-relaxed whitespace-pre-line break-words">
						{list.description}
					</p>
				</section>
			{/if}

			<section>
				<div class="flex items-center justify-between mb-5">
					<h2 class="text-sm font-black uppercase tracking-widest text-[#2F5D50]">
						Kalandok
					</h2>

					<span class="text-sm font-bold text-[#8D7462]">
            {list.adventures.length} db
          </span>
				</div>

				{#if list.adventures.length === 0}
					<div class="p-8 rounded-3xl border-2 border-dashed border-[#2F5D50]/15 text-center">
						<p class="text-[#2F5D50] opacity-60 italic">
							Ebben a listában még nincs kaland.
						</p>
					</div>
				{:else}
					<div class="space-y-4">
						{#each list.adventures as adventure}
							<a
								href={`/adventures/${adventure.id}`}
								class="block bg-white p-6 rounded-3xl border border-[#2F5D50]/5 shadow-sm transition-transform active:scale-[0.98]"
							>
								<div class="flex items-start gap-3">
									<h3 class="flex-1 min-w-0 text-xl font-black text-[#2F5D50] leading-tight break-words">
										{adventure.title}
									</h3>

									<ChevronRightOutline
										class="w-5 h-5 shrink-0 text-[#8D7462] mt-1"
									/>
								</div>

								{#if adventure.difficulty}
                  <span class="inline-block mt-3 bg-[#8D7462]/10 text-[#8D7462] px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider">
                    {adventure.difficulty}
                  </span>
								{/if}

								{#if adventure.description}
									<p class="mt-3 text-sm text-[#2F5D50]/70 leading-relaxed line-clamp-3 break-words">
										{adventure.description}
									</p>
								{/if}

								<span class="block mt-5 text-[10px] font-black uppercase tracking-widest text-[#8D7462]">
                  Kaland megnyitása →
                </span>
							</a>
						{/each}
					</div>
				{/if}
			</section>
		{/if}
	</div>
</main>