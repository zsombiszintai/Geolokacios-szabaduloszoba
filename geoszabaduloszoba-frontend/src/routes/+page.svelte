<script lang="ts">
	import { auth } from '$lib/auth.svelte';
	import { ArrowRight, Compass, MapPin, Route, Sparkles } from 'lucide-svelte';

	let actionError = $state('');
	let pending = $state(false);

	async function start(action: 'login' | 'register') {
		pending = true;
		actionError = '';
		try {
			await auth[action]();
		} catch {
			actionError = 'Nem sikerült megnyitni a belépési felületet. Próbáld újra!';
		} finally {
			pending = false;
		}
	}
</script>

<svelte:head>
	<title>CityScape</title>
	<meta name="description" content="Fedezd fel a várost a CityScape szabadtéri szabadulós kalandjaival! Kövesd a nyomokat, és oldj meg rejtvényeket." />
</svelte:head>

<div class="landing">
	<header class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-7">
		<a href="/" aria-label="CityScape kezdőlap" class="flex items-center gap-2 text-xl font-bold tracking-widest"><Compass size={28} /> CityScape</a>
		{#if auth.authenticated}
			<a href="/dashboard" class="nav-link">Kalandok <ArrowRight size={16} /></a>
		{:else}
			<button class="nav-link" disabled={auth.loading || !!auth.error || pending} onclick={() => start('login')}>Bejelentkezés <ArrowRight size={16} /></button>
		{/if}
	</header>
	<main class="mx-auto max-w-6xl px-6 pb-16 pt-10 sm:pt-20">
		<section class="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
			<div>
				<p class="mb-6 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.2em] text-[#775D4D]"><MapPin size={16} /> Szabadtéri szabadulós kalandok</p>
				<h1 class="text-5xl font-bold leading-[1.08] tracking-tight sm:text-7xl">A város a te<br /><span class="text-[#8D7462]">játszótered.</span></h1>
				<p class="mt-6 max-w-md text-lg leading-relaxed text-[#58665e]">Fordulj be egy új utcába. Kövesd a nyomokat. Oldd meg a rejtélyt. A CityScape-pel egy egyszerű séta is kalanddá válik.</p>
				<div class="mt-9 flex flex-wrap gap-3">
					{#if auth.authenticated}
						<a class="primary-action" href="/dashboard">Irány a kaland! <ArrowRight size={20} /></a>
					{:else}
						<button class="primary-action" disabled={auth.loading || !!auth.error || pending} onclick={() => start('register')}>Regisztráció <ArrowRight size={20} /></button>
						<button class="secondary-action" disabled={auth.loading || !!auth.error || pending} onclick={() => start('login')}>Bejelentkezés</button>
					{/if}
				</div>
				{#if auth.error || actionError}
					<p role="alert" class="mt-4 text-sm text-red-700">{actionError || 'A bejelentkezési szolgáltatás nem érhető el. Töltsd újra az oldalt, és próbáld ismét.'}</p>
				{:else if auth.loading}
					<p role="status" class="mt-4 text-sm text-[#58665e]">Bejelentkezési lehetőségek betöltése…</p>
				{/if}
			</div>
			<div class="adventure-map" aria-hidden="true">
				<div class="park park-one"></div><div class="park park-two"></div>
				<div class="street street-one"></div><div class="street street-two"></div>
				<svg class="trail" viewBox="0 0 400 420" fill="none"><path d="M90 325V245Q90 220 120 220H265Q295 220 295 190V110" stroke="#2F5D50" stroke-width="5" stroke-linecap="round" stroke-dasharray="9 12" /><circle cx="90" cy="325" r="10" fill="#2F5D50" stroke="#F5F2EA" stroke-width="5" /></svg>
				<div class="destination"><MapPin size={38} /></div>
				<div class="map-label"><Compass size={20} /> Egy város. Ezer felfedezés.</div>
				<div class="clue"><Sparkles size={20} /><span>A következő nyom<br /><strong>csak egy sétára van.</strong></span></div>
			</div>
		</section>
		<section aria-label="Így működik a CityScape" class="mt-20 grid gap-8 border-t border-[#2F5D50]/15 pt-9 sm:grid-cols-3 lg:mt-24">
			<div><MapPin class="mb-4 text-[#8D7462]" size={26} /><h2 class="text-lg font-bold">Válassz egy kalandot</h2><p class="mt-2 leading-relaxed text-[#58665e]">Keress izgalmas történeteket és felfedezésre váró helyeket a közeledben.</p></div>
			<div><Route class="mb-4 text-[#8D7462]" size={26} /><h2 class="text-lg font-bold">Kövesd a nyomokat</h2><p class="mt-2 leading-relaxed text-[#58665e]">Járd be az állomásokat, és fejtsd meg az út közben rád váró rejtvényeket.</p></div>
			<div><Sparkles class="mb-4 text-[#8D7462]" size={26} /><h2 class="text-lg font-bold">Alkoss saját történetet</h2><p class="mt-2 leading-relaxed text-[#58665e]">Készíts saját kalandot, és mutasd meg másoknak a város általad ismert arcát.</p></div>
		</section>
	</main>
	<footer class="mx-auto max-w-6xl px-6 py-6 text-sm text-[#58665e]">CityScape · Lépj ki a megszokottból.</footer>
</div>

<style>
	.landing { min-height: 100vh; background: #f5f2ea; color: #2f5d50; }
	.nav-link, .primary-action, .secondary-action { display: inline-flex; align-items: center; justify-content: center; gap: .65rem; font-weight: 700; }
	.nav-link { font-size: .875rem; }
	.primary-action, .secondary-action { border-radius: 1rem; padding: 1rem 1.5rem; transition: background .2s; }
	.primary-action { background: #2f5d50; color: #f5f2ea; }
	.primary-action:hover { background: #24493e; }
	.secondary-action { border: 1px solid #2f5d5040; }
	.secondary-action:hover { background: #2f5d500c; }
	button:disabled { cursor: wait; opacity: .55; }
	a:focus-visible, button:focus-visible { outline: 3px solid #8d7462; outline-offset: 5px; }
	.adventure-map { position: relative; min-height: 420px; overflow: hidden; border: 8px solid #fff9; border-radius: 2.5rem; background: #e9e5da; box-shadow: 0 24px 65px #2f5d5012; }
	.park { position: absolute; border-radius: 2rem; background: #ccd8be; }
	.park-one { width: 42%; height: 36%; top: 8%; left: 7%; transform: rotate(-12deg); }
	.park-two { width: 32%; height: 32%; bottom: 5%; right: 5%; transform: rotate(-12deg); }
	.street { position: absolute; background: #f9f7f1; border: 1px solid #ded9cd; }
	.street-one { width: 130%; height: 34px; left: -15%; top: 48%; transform: rotate(-12deg); }
	.street-two { width: 34px; height: 130%; left: 54%; top: -15%; transform: rotate(-12deg); }
	.trail { position: absolute; width: 100%; height: 100%; }
	.destination { position: absolute; left: 70%; top: 18%; padding: .75rem; border-radius: 50%; background: #2f5d50; color: #f5f2ea; box-shadow: 0 0 0 10px #2f5d5015; }
	.map-label { position: absolute; left: 1.25rem; top: 1.25rem; display: flex; align-items: center; gap: .5rem; font-size: .75rem; font-weight: 700; background: #f5f2ea; padding: .75rem 1rem; border-radius: 1rem; }
	.clue { position: absolute; bottom: 1.5rem; right: 1.5rem; display: flex; align-items: center; gap: .75rem; border-radius: 1rem; padding: 1rem 1.25rem; background: #2f5d50; color: #f5f2ea; font-size: .875rem; }
	@media (max-width: 639px) { .adventure-map { min-height: 340px; } }
</style>
