<script lang="ts">
    import "../app.css";
    import favicon from '$lib/assets/favicon.svg';
    import {
        HomeOutline,
        MapPinSolid,
        PlusOutline,
        StarOutline,
        PlayOutline,
        UserCircleOutline,
        UserSettingsOutline
    } from "flowbite-svelte-icons";
    import { page } from '$app/stores';
    import { onMount } from "svelte";
    import { goto } from '$app/navigation';
    import { auth } from "$lib/auth.svelte";
    import { base } from '$app/paths';

    onMount(async () => {
        await auth.init();
        if ($page.url.pathname === '/') {
            goto('/dashboard');
        }
    });

    let { children } = $props();

    const activePath = $derived($page.url.pathname);
    const hideNavbarPaths = ['/login', '/register', '/'];
    const showNavbar = $derived(!hideNavbarPaths.includes(activePath));
</script>

<svelte:head>
    <link rel="icon" href="{favicon}" />
</svelte:head>

{#if auth.loading}

<div class="flex items-center justify-center min-h-screen">
    <p>Authenticating...</p>
</div>

{:else if auth.error}

<div class="flex items-center justify-center min-h-screen">
    <p class="text-red-500">{auth.error}</p>
</div>

{:else if auth.authenticated}

<div class="flex flex-col min-h-screen bg-[#F5F2EA]">
    
    {#if showNavbar}
        <header class="fixed top-0 left-0 w-full h-16 bg-[#2F5D50] text-[#F5F2EA] flex items-center justify-between px-6 z-50 shadow-md">
            <UserSettingsOutline class="w-7 h-7 cursor-pointer" />
            <h1 class="text-2xl font-bold tracking-[0.2em] font-josefin">CityScape</h1>
            <span class="text-sm font-josefin opacity-80 truncate max-w-[7rem] text-right">
                {auth.fullName ?? auth.username ?? ''}
            </span>
        </header>
    {/if}

    <main class="flex-grow w-full {showNavbar ? 'pt-20 pb-20' : ''}">
        {@render children()}
    </main>

    {#if showNavbar}
        <nav class="fixed bottom-0 left-0 w-full h-16 bg-[#2F5D50] border-t border-white/10 z-50 flex items-center justify-around px-2">
            
            <a href="./dashboard" class="flex flex-col items-center justify-center w-full h-full transition-colors">
                <HomeOutline class="w-7 h-7 {activePath === '/dashboard' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="./map" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <MapPinSolid class="w-7 h-7 {activePath === '/map' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="./created-adventures" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <PlusOutline class="w-7 h-7 {activePath === '/create-adventure' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="/favorites" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <StarOutline class="w-7 h-7 text-white/50" />
            </a>
            
            <a href="/play" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <PlayOutline class="w-7 h-7 text-white/50" />
            </a>
            
            <a href="/profile" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <UserCircleOutline class="w-7 h-7 text-white/50" />
            </a>

        </nav>
    {/if}

</div>

{/if}