<script lang="ts">
    import { Label, Input, Alert, Helper } from 'flowbite-svelte';
    import { goto } from '$app/navigation';

    let username = $state('');
    let password = $state('');
    let message = $state('');
    let status = $state('');

    async function handleLogin(event: SubmitEvent) {
        console.log('Adatok:', username, password);

        try{
                const response = await fetch('http://localhost:8080/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ username, password})
                });

                console.log('2. Válasz megérkezett, státusz:', response.status);

                if (response.ok){
                    const userAdatok = await response.json();
                    message = "Sikeres bejelentkezés.";
                    setTimeout(() => {
                        goto('/dashboard');
                    }, 1200);
                }else {
                message = "Rossz felhasználónév vagy jelszó.";
                }

        }catch(err){
            console.log("A szerver nem elérhető.");
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
                Bejelentkezés
            </h2>
        </header>

        {#if status !== 'none'}
            <Alert color={status === 'success' ? 'green' : 'red'} class="mb-4 cream">
                {message}
            </Alert>
        {/if}

        <form onsubmit={handleLogin} class="space-y-4">
            <fieldset class="space-y-3 border-none p-0 m-0">
                <div>
                    <Label for="username" class="label-city">
                        Felhasználónév
                    </Label>
                    <Input 
                        id="username" 
                        class="input-city"
                        bind:value={username} 
                        placeholder="Felhasználónév" 
                        required
                    >
                    </Input>
                </div>

                <div>
                    <Label for="password" class="label-city">
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
                    Bejelentkezés
                </button>
                
                <nav class="text-center">
                    <p class="text-[#775D4D] font-medium">
                        Nincs még fiókja? 
                        <button 
                            type="button" 
                            onclick={() => goto('/register')} 
                            class="font-bold underline ml-1 hover:text-black transition-colors"
                        >
                            Regisztráljon.
                        </button>
                    </p>
                </nav>
            </footer>
        </form>
    </section>
</main>