<script lang="ts">
    import { Card, Button, Label, Input, Alert, Helper } from 'flowbite-svelte';
    import { UserOutline, MailBoxOutline, LockOutline} from 'flowbite-svelte-icons';
    import { goto } from '$app/navigation';

    let username = $state('');
    let email = $state('');
    let password = $state('');
    let message = $state('');
    let status: 'success' | 'danger' | 'none' = $state('none');

    async function handleRegister() {
        status = 'none';
        try {
            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });

            if (response.ok) {
                status = 'success';
                message = "Sikeres regisztráció! Irány a belépés...";
                setTimeout(() => goto('/login'), 1500);
            } else {
                status = 'danger';
                message = "Hiba: A felhasználónév vagy email már foglalt.";
            }
        } catch (err) {
            status = 'danger';
            message = "A szerver nem elérhető.";
        }
    }

</script>

<header class="header-city">
    <h1 class="header-city-title">
        CityScape
    </h1>
    
    <div class="w-8"></div> 
</header>

<main class="flex flex-col items-center justify-center min-h-screen pt-24 px-8 bg-[#F5F2EA]">
    <section class="w-full max-w-sm">
        <header class="mb-6">
            <h2 class="text-3xl font-extrabold text-[#775D4D] text-center tracking-tight uppercase">
                Regisztráció
            </h2>
        </header>

        {#if status !== 'none'}
            <Alert color={status === 'success' ? 'green' : 'red'} class="mb-4 bg-white/80">
                {message}
            </Alert>
        {/if}

        <form onsubmit={handleRegister} class="space-y-4">
            <fieldset class="space-y-3 border-none p-0 m-0">
                <div>
                    <Label for="username" class="text-lg font-bold text-[#775D4D] mb-0.5 ml-1">
                        Felhasználónév
                    </Label>
                    <Input 
                        id="username" 
                        class="input-city"
                        bind:value={username} 
                        placeholder="Név" 
                        required
                    >
                    </Input>
                </div>

                <div>
                    <Label for="email" class="text-lg font-bold text-[#775D4D] mb-0.5 ml-1">
                        E-mail
                    </Label>
                    <Input
                        id="email" 
                        type="email" 
                        class="input-city" 
                        bind:value={email} 
                        placeholder="Email" 
                        required
                    >
                    </Input>
                </div>

                <div>
                    <Label for="password" class="text-lg font-bold text-[#775D4D] mb-0.5 ml-1">
                        Jelszó
                    </Label>
                    <Input 
                        id="password" 
                        type="password" 
                        class="input-city" 
                        bind:value={password} 
                        placeholder="Jelszó" 
                        required
                    >
                    </Input>
                </div>
            </fieldset>

            <footer class="pt-4 flex flex-col space-y-6">
                <button type="submit" class="bg-[#775D4D] text-[#F5F2EA] py-3.5 rounded-2xl font-bold uppercase tracking-widest shadow-md hover:bg-[#5e4a3d] transition-all active:scale-95">
                    Regisztráció
                </button>
                
                <nav class="text-center">
                    <p class="text-[#775D4D] font-medium">
                        Van már fiókja? 
                        <button 
                            type="button" 
                            onclick={() => goto('/login')} 
                            class="font-bold underline ml-1 hover:text-black transition-colors"
                        >
                            Jelentkezzen be.
                        </button>
                    </p>
                </nav>
            </footer>
        </form>
    </section>
</main>