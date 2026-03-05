<script lang="ts">
    import "../app.css";
    import favicon from '$lib/assets/favicon.svg';
    import { BottomNav, BottomNavItem } from "flowbite-svelte";
    import { 
        HomeOutline, 
        LockOutline, 
        PlusOutline, 
        StarOutline, 
        PlayOutline, 
        UserCircleOutline,
        UserSettingsOutline
    } from "flowbite-svelte-icons";
    import { page } from '$app/stores';

    let { children } = $props();

    const activePath = $derived($page.url.pathname);
    const hideNavbarPaths = ['/login', '/register', '/'];
    const showNavbar = $derived(!hideNavbarPaths.includes(activePath));
</script>

<svelte:head>
    <link rel="icon" href="{favicon}" />
</svelte:head>

<div class="flex flex-col min-h-screen bg-[#F5F2EA]">
    
    {#if showNavbar}
        <header class="fixed top-0 left-0 w-full h-16 bg-[#2F5D50] text-[#F5F2EA] flex items-center justify-between px-6 z-50 shadow-md">
            <UserSettingsOutline class="w-7 h-7 cursor-pointer" />
            <h1 class="text-2xl font-bold tracking-[0.2em] font-josefin">CityScape</h1>
            <div class="w-7"></div> 
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
            
            <a href="/games" class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <LockOutline class="w-7 h-7 text-white/50" />
            </a>
            
            <button class="flex flex-col items-center justify-center w-full h-full border-l border-white/10">
                <PlusOutline class="w-7 h-7 text-white" />
            </button>
            
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