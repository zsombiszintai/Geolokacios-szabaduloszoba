A DTO-k összeállítása az `api/mapper` rétegben történik. A service-ek entitásokat, domain eredményeket vagy Spring Data `Page` objektumokat adnak vissza; nem függnek az API-tól vagy a DTO-któl. A bejelentkezett felhasználó azonosítóját az adott service a `UserService.getCurrentUserId()` segítségével olvassa a SecurityContextből.

A `ListAPI` új neve `AdventureCollectionAPI`. A `/lists` útvonalak megmaradtak. Az avatarfeltöltés folyamatát a `SettingsService` végzi. A MinIO-beállításokat a `MinioProperties` köti be; a meglévő `minio.url`, `minio.accessKey`, `minio.secretKey` és `minio.bucketName` kulcsok használhatók.

A meglévő, listát visszaadó keresések kompatibilisek maradtak. A lapozható változatok:

- `GET /search/page?q=...&type=ADVENTURE&page=0&size=20` (a `USER` és `LIST` típus is támogatott; `lat` és `lon` opcionális).
- `GET /api/adventures/search/page?query=...&lat=47&lon=19&page=0&size=20`.

A válasz `PageDTO<T>`: `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. A lapsorszám 0-tól indul, a lapméret 1–100 lehet, a rendezés azonosító szerint növekvő. A kalandok és listák lapozása adatbázisban történik. A felhasználókeresés megtartja a korábbi Keycloak-korlátot (legfeljebb 50 névegyezés), és az ezekhez tartozó helyi profilokat lapozza.

A nehézség az API-ban `EASY`, `MEDIUM`, `HARD` (ismeretlen esetben `UNKNOWN`); a magyar megnevezést a frontend `src/lib/i18n/difficulty.ts` fordítása adja.

A Keycloak maradt a felhasználói identitás forrása. A helyi `UserEntity` alkalmazásadatokat (pontszám, bemutatkozás, avatarkulcs) és a kalandok/követések kapcsolatait tárolja. Ennek megszüntetése külön adatmodell- és adatmigrációt igényelne.

A külső szolgáltatások nélkül futtatható ellenőrzés:

```shell
mvn -Dtest=ApiContractTest,CurrentUserServiceTest,PagedSearchRepositoryTest,SettingsServiceTest,MinioPropertiesTest clean test
```

A repository-tesztek memóriában futó H2 adatbázist használnak; nem kapcsolódnak a működő PostgreSQL-adatbázishoz. A meglévő teljes `contextLoads` teszt az alkalmazás külső konfigurációját és szolgáltatásait is igényli.
