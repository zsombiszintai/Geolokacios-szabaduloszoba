# Project Schedule
- 2026-03-01: Adatbázis alapozás és Entity osztályok
- 2026-03-08: Svelte projekt váz és UI alapok
- 2026-03-15: Felhasználókezelés és Regisztráció
- 2026-03-22: Bejelentkezés és JWT hitelesítés
- 2026-03-29: Térkép integráció és GPS kezelés
- 2026-04-05: Kalandok és Állomások API
- 2026-04-12: Játékmenet logika és távolságellenőrzés
- 2026-04-19: Profil oldal
- 2026-04-26: Értékelések és közösségi modul
- 2026-05-03: Admin felület
- 2026-05-10: Tesztelés, hibajavítás
- 2026-05-17: Utolsó simítások és dokumentálás

## Adatbázis alapozás és Entity osztályok
Ezen a héten az adatbázis séma fizikai leképezésére fókuszálok a backendben. Létrehozom az Entity osztályokat a hozzájuk tartozó alapvető mezőkkel.

## Svelte projekt váz és UI alapok
A frontend fejlesztés elindítása. Felépítem a SvelteKit projekt alapjait és létrehozom a főbb útvonalakat (Login, Regisztráció, Kezdőoldal).

## Felhasználókezelés és regisztráció
Megvalósítom a felhasználói adatok mentését. Kidolgozom a regisztrációs végpontot, beállítom a jelszavak titkosítását és elmentem az első tesztfelhasználókat.

## Bejelentkezés és JWT hitelesítés
A biztonsági rendszer kiépítése. Elkészítem a login folyamatot, ami ellenőrzi a hitelesítő adatokat, és sikeres belépés esetén legenerálja a JWT azonosítót a későbbi kérésekhez.

## Térkép integráció és GPS kezelés
Leaflet könyvtár a Svelte projektbe. Megoldom a felhasználó aktuális GPS koordinátáinak lekérdezését és megjelenítését a térképen.

## Kalandok és Állomások API
Kifejlesztem azokat a végpontokat, amik a meglévő kalandokat és azok állomásait (Station) adják vissza a frontend számára, hogy a játékos böngészni tudjon a lehetőségek között.

## Játékmenet logika és távolságellenőrzés
Megírom azt a logikát, ami folyamatosan figyeli a játékos távolságát az aktuális állomástól. Csak akkor engedélyezem a rejtvény megjelenítését, ha a felhasználó fizikailag is ott tartózkodik.

## Profil oldal
Megjelenítem a játékos egyéni statisztikáit (teljesített kalandok, listák stb.) a saját oldalán.

## Értékelések és közösségi modul
Elkészítem a review és user_follow funkciókat. Lehetővé teszem a kalandok szöveges és csillagos értékelését, valamint a felhasználók közötti követési rendszert.

## Admin felület
Létrehozok egy adminisztrátori felületet a kalandok kezelésére.

## Tesztelés, hibajavítás
Kijavítom a tesztelés során felmerült bugokat, és véglegesítem a design-t.

## Utolsó simítások és dokumentálás
A kód végleges takarítása, az API dokumentáció lezárása és a szakdolgozat leadás előtti utolsó formai ellenőrzése.