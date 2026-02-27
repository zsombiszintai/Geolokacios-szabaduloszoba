<script lang="ts">
    let username = '';
    let password = '';

    function handleLogin() {
        console.log('Adatok:', username, password);
    }
</script>

<main style="display: flex; justify-content: center; margin-top: 50px;">
    
    <form on:submit|preventDefault={handleLogin} style="display: flex; flex-direction: column; gap: 15px; width: 300px; padding: 20px; border: 1px solid #ccc; border-radius: 8px;">
        
        <h1 style="text-align: center;">Bejelentkezés</h1>

        <label>
            Felhasználónév:
            <input 
                type="text" 
                bind:value={username} 
                required 
                style="width: 100%; padding: 8px; margin-top: 5px;"
            />
        </label>
        
        <label>
            Jelszó:
            <input 
                type="password" 
                bind:value={password} 
                required 
                style="width: 100%; padding: 8px; margin-top: 5px;"
            />
        </label>
        
        <button type="submit" style="padding: 10px; cursor: pointer;">Belépés</button>
    </form>
</main>