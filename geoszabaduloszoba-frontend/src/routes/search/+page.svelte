<script lang="ts">
	import BackButton from '$lib/components/BackButton.svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { auth } from '$lib/auth.svelte';
	import { translateDifficulty } from '$lib/i18n/difficulty';
	import { ChevronDownOutline, ChevronRightOutline } from 'flowbite-svelte-icons';

	const types = { adventure: 'Kalandok', user: 'Játékosok', list: 'Listák' };
	const difficulties = ['EASY', 'MEDIUM', 'HARD'];
	type SearchType = keyof typeof types;
	type Result = {
		id: number;
		title: string;
		averageTime?: number | null;
		averageRating?: number | null;
		routeDistanceMeters?: number | null;
	};
	const numberFormat = new Intl.NumberFormat('hu-HU', { maximumFractionDigits: 2 });
	type ResultPage = { content: Result[]; totalElements: number; totalPages: number };

	const query = $derived(page.url.searchParams.get('q')?.trim() ?? '');
	const type = $derived(parseType(page.url.searchParams.get('type')));
	const difficulty = $derived(page.url.searchParams.get('difficulty') ?? '');
	const advancedFields = ['minRating', 'maxDistanceKm', 'maxDurationMinutes'] as const;
	const advancedValues = $derived(advancedFields.map((field) => page.url.searchParams.get(field) ?? ''));
	const hasAdvancedFilters = $derived(advancedValues.some((value) => value !== ''));
	const pageParam = $derived(Number(page.url.searchParams.get('page') ?? 0));
	const pageIndex = $derived(Number.isSafeInteger(pageParam) && pageParam >= 0 ? pageParam : 0);
	let draftType = $state<SearchType>('adventure');
	let results = $state<ResultPage | null>(null);
	let loading = $state(false);
	let error = $state('');
	let attempts = $state(0);

	function parseType(value: string | null): SearchType {
		return value === 'user' || value === 'list' ? value : 'adventure';
	}

	$effect(() => {
		draftType = type;
	});

	function submit(event: SubmitEvent) {
		event.preventDefault();
		const fields = new FormData(event.currentTarget as HTMLFormElement);
		const params = new URLSearchParams({
			q: String(fields.get('q') ?? '').trim(),
			type: draftType
		});
		const selectedDifficulty = fields.get('difficulty');
		if (draftType === 'adventure' && selectedDifficulty) {
			params.set('difficulty', String(selectedDifficulty));
		}
		if (draftType === 'adventure') {
			for (const field of advancedFields) {
				const value = String(fields.get(field) ?? '').trim();
				if (value) params.set(field, value);
			}
		}
		const href = '/search?' + params.toString();
		if (href === page.url.pathname + page.url.search) {
			attempts += 1;
		} else {
			void goto(href);
		}
	}

	function pageHref(index: number) {
		const params = new URLSearchParams(page.url.searchParams);
		params.set('page', String(index));
		return '/search?' + params.toString();
	}

	function resultHref(result: Result, resultType: SearchType) {
		if (resultType === 'user') return `/profile/user/${encodeURIComponent(result.title)}`;
		if (resultType === 'list') return `/list/${result.id}`;
		return `/adventures/${result.id}`;
	}

	async function loadResults(url: URL, token: string, signal: AbortSignal) {
		try {
			const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` }, signal });
			if (!response.ok) throw new Error('A keresés sikertelen. Próbáld újra.');
			const data: ResultPage = await response.json();
			if (!Array.isArray(data.content)) throw new Error('A keresés eredménye nem tölthető be.');
			if (!signal.aborted) results = data;
		} catch (cause) {
			if (!signal.aborted) {
				error = cause instanceof Error ? cause.message : 'A keresés sikertelen.';
			}
		} finally {
			if (!signal.aborted) loading = false;
		}
	}

	$effect(() => {
		void attempts;
		const token = auth.token;
		const isAdventure = type === 'adventure';
		const url = new URL(isAdventure ? '/api/adventures/search/page' : '/search/page',
			'https://api.zsomborszintai.com');
		url.searchParams.set(isAdventure ? 'query' : 'q', query);
		url.searchParams.set('page', String(pageIndex));
		if (!isAdventure) url.searchParams.set('type', type);
		if (isAdventure && difficulties.includes(difficulty)) {
			url.searchParams.set('difficulty', difficulty);
		}
		if (isAdventure) {
			const [rating, distanceKm, durationMinutes] = advancedValues;
			if (rating) url.searchParams.set('minRating', rating);
			if (distanceKm) url.searchParams.set('maxDistanceMeters', String(Math.round(Number(distanceKm) * 1000)));
			if (durationMinutes) url.searchParams.set('maxDurationSeconds', String(Number(durationMinutes) * 60));
		}

		results = null;
		error = '';
		loading = false;
		if (!token || (!isAdventure && !query)) return;

		const controller = new AbortController();
		loading = true;
		void loadResults(url, token, controller.signal);
		return () => controller.abort();
	});
</script>

<svelte:head>
	<title>Keresés – CityScape</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-6 text-[#2F5D50]">
	<BackButton label="Vissza a térképre" boxed />
	<h1 class="mb-5 mt-3 text-3xl font-black">Keresés</h1>

	{#key JSON.stringify([query, type, difficulty, advancedValues])}
		<form onsubmit={submit} class="grid gap-5 rounded-2xl border border-[#2F5D50]/10 bg-white/80 p-5 sm:grid-cols-2 sm:p-6">
			<label class="grid gap-1 text-sm font-bold sm:col-span-2">
				Keresőkifejezés
				<input name="q" type="search" value={query} required={draftType !== 'adventure'}
					placeholder={draftType === 'user' ? 'Felhasználónév' : 'Cím'}
					class="min-h-11 w-full rounded-xl border border-[#2F5D50]/25 bg-white px-3" />
			</label>
			<label class="grid gap-1 text-sm font-bold">
				Találat típusa
				<span class="relative">
					<select bind:value={draftType} class="min-h-11 w-full appearance-none rounded-xl border border-[#2F5D50]/25 bg-white pl-4 pr-11">
						{#each Object.entries(types) as [value, label]}
							<option {value}>{label}</option>
						{/each}
					</select>
					<ChevronDownOutline class="pointer-events-none absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2" aria-hidden="true" />
				</span>
			</label>
			{#if draftType === 'adventure'}
				<label class="grid gap-1 text-sm font-bold">
					Nehézség
					<span class="relative">
						<select name="difficulty" value={difficulty} class="min-h-11 w-full appearance-none rounded-xl border border-[#2F5D50]/25 bg-white pl-4 pr-11">
							<option value="">Bármelyik</option>
							{#each difficulties as value}
								<option {value}>{translateDifficulty(value)}</option>
							{/each}
						</select>
						<ChevronDownOutline class="pointer-events-none absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2" aria-hidden="true" />
					</span>
				</label>
				<details open={hasAdvancedFilters} class="group rounded-xl border border-[#2F5D50]/15 sm:col-span-2">
					<summary class="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 px-4 text-sm font-bold [&::-webkit-details-marker]:hidden">
						Bővített keresés
						<ChevronDownOutline class="h-5 w-5 shrink-0 group-open:rotate-180" aria-hidden="true" />
					</summary>
					<div class="grid gap-4 border-t border-[#2F5D50]/10 p-4 sm:grid-cols-3">
						<label class="grid gap-1 text-sm font-bold">
							Minimum értékelés
							<input name="minRating" type="number" min="1" max="5" step="0.1"
								value={advancedValues[0]} placeholder="1–5"
								class="min-h-11 w-full rounded-xl border border-[#2F5D50]/25 bg-white px-3" />
						</label>
						<label class="grid gap-1 text-sm font-bold">
							Max. útvonalhossz (km)
							<input name="maxDistanceKm" type="number" min="0.1" step="0.1"
								value={advancedValues[1]}
								class="min-h-11 w-full rounded-xl border border-[#2F5D50]/25 bg-white px-3" />
						</label>
						<label class="grid gap-1 text-sm font-bold">
							Max. átlagidő (perc)
							<input name="maxDurationMinutes" type="number" min="1" max="35791394" step="1"
								value={advancedValues[2]}
								class="min-h-11 w-full rounded-xl border border-[#2F5D50]/25 bg-white px-3" />
						</label>
					</div>
				</details>
			{/if}
			<button type="submit" class="min-h-11 rounded-xl bg-[#2F5D50] px-5 font-bold text-white sm:col-span-2 sm:justify-self-end">
				Keresés
			</button>
		</form>
	{/key}

	<section class="mt-6" aria-label="Keresési találatok" aria-busy={loading}>
		{#if loading}
			<p role="status">Keresés…</p>
		{:else if error}
			<p role="alert" class="text-red-700">{error}</p>
			<button type="button" onclick={() => attempts += 1} class="mt-2 min-h-11 font-bold underline">Újrapróbálás</button>
		{:else if results}
			{#if results.content.length === 0}
				<p role="status">Nincs találat ezen az oldalon.</p>
			{:else}
				<p role="status" class="mb-3 text-sm font-bold">{results.totalElements} találat</p>
				<ul class="grid gap-2">
					{#each results.content as result (result.id)}
						<li>
							<a href={resultHref(result, type)}
								class="flex min-h-12 items-center justify-between gap-3 rounded-xl border border-[#2F5D50]/10 bg-white/80 px-4 py-3 font-bold hover:bg-white">
								<span class="min-w-0 break-words">
									{result.title}
									{#if type === 'adventure'}
										<span class="mt-1 flex flex-wrap gap-x-3 text-sm font-normal">
											{#if result.averageRating != null && result.averageRating > 0}
												<span>Értékelés {numberFormat.format(result.averageRating)}/5</span>
											{/if}
											{#if result.routeDistanceMeters != null}
												<span>{numberFormat.format(result.routeDistanceMeters / 1000)} km</span>
											{/if}
											{#if result.averageTime != null && result.averageTime > 0}
												<span>Átlagosan {Math.ceil(result.averageTime / 60)} perc</span>
											{/if}
										</span>
									{/if}
								</span>
								<ChevronRightOutline class="h-5 w-5 shrink-0" aria-hidden="true" />
							</a>
						</li>
					{/each}
				</ul>
			{/if}
			{#if results.totalPages > 1 || pageIndex > 0}
				<nav aria-label="Találatok lapozása" class="mt-4 flex flex-wrap items-center gap-4 text-sm">
					{#if pageIndex > 0}<a class="py-3 font-bold underline" href={pageHref(pageIndex - 1)}>Előző oldal</a>{/if}
					{#if pageIndex < results.totalPages - 1}<a class="py-3 font-bold underline" href={pageHref(pageIndex + 1)}>Következő oldal</a>{/if}
				</nav>
			{/if}
		{:else if !auth.token}
			<p role="status">A kereséshez jelentkezz be.</p>
		{:else}
			<p role="status">Adj meg egy keresőkifejezést.</p>
		{/if}
	</section>
</section>
