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
        UserSettingsOutline,
        ArrowRightToBracketOutline
    } from "flowbite-svelte-icons";
    import { page } from '$app/stores';
    import { onMount } from "svelte";
    import { goto } from '$app/navigation';
    import { auth } from "$lib/auth.svelte";


    let { children } = $props();

    const activePath = $derived($page.url.pathname);
    const hideNavbarPaths = ['/login', '/register', '/'];
    const showNavbar = $derived(!hideNavbarPaths.includes(activePath));

    onMount(async () => {
        await auth.init();
        if ($page.url.pathname === '/') {
            goto('/dashboard');
        }
    });

    async function handleLogout() {
        await auth.logout();
        goto('/');
    }
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
            <a href="/settings">
                <UserSettingsOutline class="w-7 h-7 cursor-pointer" />
            </a>
            <h1 class="text-2xl font-bold tracking-[0.2em] font-josefin">CityScape</h1>
            <div class="flex items-center gap-3">
                 <button
                   onclick={handleLogout}
                   class="flex items-center justify-center hover:text-red-400 transition-colors"
                   title="Kijelentkezés"
                 >
                     <ArrowRightToBracketOutline class="w-7 h-7" />
                 </button>
            </div>
        </header>
    {/if}

    <main class="flex-grow w-full {showNavbar ? 'pt-20 pb-20' : ''}">
        {@render children()}
    </main>

    {#if showNavbar}
        <nav class="fixed bottom-0 left-0 w-full h-16 bg-[#2F5D50] border-t border-white/10 z-50 flex items-center justify-around px-2">
            
            <a href="/dashboard" class="flex flex-col items-center justify-center w-full h-full transition-colors">
                <HomeOutline class="w-7 h-7 {activePath === '/dashboard' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="/map" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <MapPinSolid class="w-7 h-7 {activePath === '/map' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="/adventures" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <PlusOutline class="w-7 h-7 {activePath === '/create-adventure' ? 'text-white' : 'text-white/50'}" />
            </a>
            
            <a href="/completed-adventures" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <StarOutline class="w-7 h-7 text-white/50" />
            </a>
            
            <a href="/play" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <PlayOutline class="w-7 h-7 text-white/50" />
            </a>

            <a href="/profile/user/{auth.username || 'me'}"
               class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <UserCircleOutline class="w-7 h-7 {activePath.startsWith('/profile') ? 'text-white' : 'text-white/50'}" />
            </a>

        </nav>
    {/if}

</div>

{/if}