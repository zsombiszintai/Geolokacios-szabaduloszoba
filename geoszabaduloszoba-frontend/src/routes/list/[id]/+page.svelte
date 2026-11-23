<script lang="ts">
	import BackButton from '$lib/components/BackButton.svelte';
	import LoadingState from '$lib/components/LoadingState.svelte';
	import { translateDifficulty } from '$lib/i18n/difficulty';
	import { auth } from '$lib/auth.svelte.js';
	import { page } from '$app/state';
	import { ChevronRightOutline, DotsHorizontalOutline } from 'flowbite-svelte-icons';
	import LikeButton from '$lib/components/LikeButton.svelte';

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
	let menuOpen = $state(false);
	let shareMessage = $state('');

	async function copyListLink() {
		try {
			await navigator.clipboard.writeText(window.location.href);
			shareMessage = 'A lista hivatkozása a vágólapra került.';
			menuOpen = false;
		} catch {
			shareMessage = 'A hivatkozás másolása sikertelen.';
		}
	}

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
		menuOpen = false;
		shareMessage = '';
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

<section class="min-h-screen bg-[#F5F2EA] font-josefin px-6 pt-6 pb-24">
	<div class="max-w-md mx-auto">
		<BackButton />

		{#if loading}
			<LoadingState message="Lista betöltése..." />
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
			<header class="relative mb-8">
				<div class="absolute right-0 top-0 flex items-center gap-1">
					{#key list.id}
						<LikeButton type="LIST" likedId={String(list.id)} />
					{/key}
					<button
						type="button"
						class="rounded-xl p-2 text-[#8D7462] hover:bg-[#2F5D50]/5 hover:text-[#2F5D50]"
						aria-label="További opciók"
						aria-expanded={menuOpen}
						onclick={() => { menuOpen = !menuOpen; shareMessage = ''; }}
					>
						<DotsHorizontalOutline class="size-6" />
					</button>
					{#if menuOpen}
						<div class="absolute right-0 top-11 z-50 w-max rounded-xl bg-white p-2 shadow-lg">
							<button type="button" class="rounded-lg px-3 py-2 text-sm font-bold text-city-green hover:bg-city-green/5" onclick={copyListLink}>
								Hivatkozás másolása
							</button>
						</div>
					{/if}
				</div>
				<p class="text-[10px] font-black uppercase tracking-[0.2em] text-[#8D7462] mb-3">
					Kalandlista
				</p>

				<h1 class="pr-24 text-3xl font-black text-[#2F5D50] leading-tight break-words">
					{list.title}
				</h1>

				<span aria-hidden="true" class="block w-12 h-1.5 bg-[#8D7462] mt-4 rounded-full"></span>
				{#if shareMessage}
					<p class="mt-2 text-sm text-city-green" role="status">{shareMessage}</p>
				{/if}
			</header>

			{#if list.description}
				<section class="bg-white/60 p-4 rounded-3xl mb-8 border border-[#2F5D50]/5">
					<h2 class="text-[10px] font-black uppercase tracking-widest text-[#8D7462] mb-2">
						Leírás
					</h2>
					<p class="text-[#2F5D50] leading-relaxed whitespace-pre-line break-words">
						{list.description}
					</p>
				</section>
			{/if}

			<section>
				<header class="flex items-center justify-between mb-5">
					<h2 class="text-sm font-black uppercase tracking-widest text-[#2F5D50]">
						Kalandok
					</h2>

					<span class="text-sm font-bold text-[#8D7462]">
            {list.adventures.length} db
          </span>
				</header>

				{#if list.adventures.length === 0}
					<div class="p-8 rounded-3xl border-2 border-dashed border-[#2F5D50]/15 text-center">
						<p class="text-[#2F5D50] opacity-60 italic">
							Ebben a listában még nincs kaland.
						</p>
					</div>
				{:else}
					<ul class="space-y-4">
						{#each list.adventures as adventure}
							<li>
							<a
								href={`/adventures/${adventure.id}`}
								class="block bg-white p-6 rounded-3xl border border-[#2F5D50]/5 shadow-sm transition-transform active:scale-[0.98]"
							>
								<header class="flex items-start gap-3">
									<h3 class="flex-1 min-w-0 text-xl font-black text-[#2F5D50] leading-tight break-words">
										{adventure.title}
									</h3>

									<ChevronRightOutline
										class="w-5 h-5 shrink-0 text-[#8D7462] mt-1"
									/>
								</header>

								{#if adventure.difficulty}
                  <span class="inline-block mt-3 bg-[#8D7462]/10 text-[#8D7462] px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider">
                    {translateDifficulty(adventure.difficulty)}
                  </span>
								{/if}

								{#if adventure.description}
									<p class="mt-3 text-sm text-[#2F5D50]/70 leading-relaxed line-clamp-3 break-words">
										{adventure.description}
									</p>
								{/if}
							</a>
						</li>
						{/each}
					</ul>
				{/if}
			</section>
		{/if}
	</div>
</section>
