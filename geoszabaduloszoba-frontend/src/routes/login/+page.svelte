<script lang="ts">
    let username = $state('');
    let password = $state('');

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
                    console.log('3. Sikeres JSON feldolgozás:', userAdatok);
                }else {
                console.log("Hiba: Rossz felhasználónév vagy jelszó.");
                }

        }catch(err){
            console.log("A szerver nem elérhető.");
        }
    }
</script>

<main style="display: flex; justify-content: center; margin-top: 50px;">
    
    <form onsubmit={handleLogin} 
    style="display: flex; 
            flex-direction: column; 
            gap: 15px; width: 300px;
            padding: 20px; border: 1px solid #ccc;
            border-radius: 8px;">
        
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