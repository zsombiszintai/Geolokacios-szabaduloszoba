<script lang="ts">
    import { onMount } from 'svelte';
    import { ChevronRightOutline } from 'flowbite-svelte-icons';
    import { auth } from '$lib/auth.svelte';
    import { goto } from '$app/navigation';
    import type { Map as LeafletMap } from 'leaflet';
    import 'leaflet/dist/leaflet.css';

  let mapElement: HTMLElement | undefined = $state(undefined);

  let adventures = $state<any[]>([]);
  let userPos = $state<{ lat: number; lon: number } | null>(null);

  let locationLoading = $state(true);
  let dataLoading = $state(false);
  let locationError = $state('');
  let dataError = $state('');

  onMount(() => {
      let disposed = false;

      if (!navigator.geolocation) {
          locationError = 'Ez a böngésző nem támogatja a helymeghatározást.';
          locationLoading = false;
          return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
            if (disposed) return;

            userPos = {
                lat: position.coords.latitude,
                lon: position.coords.longitude
            };

            locationLoading = false;
        },
        (error) => {
            if (disposed) return;

            locationError =
              error.code === 1
                ? 'A helyhozzáférés le van tiltva. Engedélyezd a böngésző webhelybeállításaiban, majd töltsd újra az oldalt.'
                : error.code === 2
                  ? 'A helyzeted nem állapítható meg. Ellenőrizd, hogy a telefon helymeghatározása be van-e kapcsolva.'
                  : 'A helymeghatározás túllépte az időkorlátot. Töltsd újra az oldalt, és próbáld meg ismét.';

            locationLoading = false;
        },
        {
            enableHighAccuracy: true,
            timeout: 20000,
            maximumAge: 0
        }
      );

      return () => {
          disposed = true;
      };
  });

  function formatTime(totalSeconds: number | null | undefined): string {
      if (!totalSeconds || totalSeconds === 0) return "0 s";

      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;

      const parts: string[] = [];

      if (hours > 0) {
          parts.push(`${hours} h`);
      }

      if (minutes > 0 || hours > 0) {
          parts.push(`${minutes} m`);
      }

      if (seconds > 0 || (hours === 0 && minutes === 0)) {
          parts.push(`${seconds} s`);
      }

      return parts.join(' ');
  }

  $effect(() => {
      const element = mapElement;
      const position = userPos;
      const token = auth.token;

      if (!element || !position || !token) return;

      const controller = new AbortController();
      let disposed = false;
      let currentMap: LeafletMap | undefined;

      dataLoading = true;
      dataError = '';
      adventures = [];

      async function initializeDashboard() {
          try {
              const L = await import('leaflet');
              if (disposed) return;

              const leafletMap = L.map(element!, {
                  zoomControl: false
              }).setView([position!.lat, position!.lon], 17);

              currentMap = leafletMap;

              L.tileLayer(
                'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                {
                    attribution:
                      '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                }
              ).addTo(leafletMap);

              const userIcon = L.divIcon({
                  className: 'custom-div-icon',
                  html: `
            <div class="relative flex items-center justify-center">
              <div class="absolute w-8 h-8 bg-[#2F5D50]/30 rounded-full animate-ping"></div>
              <div class="w-5 h-5 bg-[#2F5D50] rounded-full border-2 border-white shadow-lg z-10"></div>
            </div>
          `,
                  iconSize: [32, 32],
                  iconAnchor: [16, 16]
              });

              L.marker([position!.lat, position!.lon], {
                  icon: userIcon
              })
                .addTo(leafletMap)
                .bindPopup('Itt vagy');

              const response = await fetch(
                `https://api.zsomborszintai.com/api/dashboard?lat=${position!.lat}&lon=${position!.lon}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    },
                    signal: controller.signal
                }
              );

              if (!response.ok) {
                  throw new Error(`HTTP ${response.status}`);
              }

              const results = await response.json();
              if (disposed) return;

              if (!Array.isArray(results)) {
                  throw new Error('A szerver nem kalandlistát adott vissza.');
              }

              const cityIcon = L.divIcon({
                  className: 'custom-div-icon',
                  html: `
            <div class="text-red-600 drop-shadow-lg scale-125">
              <svg width="32" height="32" viewBox="0 0 24 24"
                fill="currentColor" stroke="white" stroke-width="1">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
              </svg>
            </div>
          `,
                  iconSize: [32, 32],
                  iconAnchor: [16, 32]
              });

              for (const adventure of results) {
                  if (
                    Number.isFinite(adventure.advLat) &&
                    Number.isFinite(adventure.advLon)
                  ) {
                      L.marker([adventure.advLat, adventure.advLon], {
                          icon: cityIcon
                      }).addTo(leafletMap);
                  }
              }

              adventures = results;
          } catch (error) {
              if (disposed) return;

              console.error('A dashboard betöltése sikertelen:', error);
              dataError = 'A térkép vagy a közeli kalandok betöltése sikertelen.';
          } finally {
              if (!disposed) {
                  dataLoading = false;
              }
          }
      }

      void initializeDashboard();

      return () => {
          disposed = true;
          controller.abort();
          currentMap?.remove();
      };
  });
</script>

<main class="flex flex-col items-center min-h-screen pt-6 pb-24 px-4 bg-[#F5F2EA] font-josefin">

    <section class="w-full max-w-md mx-auto mb-10">
        <header class="flex justify-between items-end mb-4 px-2">
            <div>
                <h1 class="text-3xl font-black text-[#2F5D50] uppercase tracking-tight leading-none">Térkép</h1>
                <div class="w-8 h-1 bg-[#8D7462] mt-2 rounded-full"></div>
            </div>
            <button
              onclick={() => goto(`/map`)}
              class="flex items-center gap-1 text-[10px] font-black text-[#8D7462] uppercase tracking-widest bg-white py-2 px-4 rounded-xl shadow-sm border border-[#2F5D50]/5 active:scale-95 transition-all"
            >
                Teljes nézet <ChevronRightOutline class="w-3 h-3" />
            </button>
        </header>

        <div class="relative w-full rounded-[1.5rem] overflow-hidden shadow-2xl border-8 border-city-cream group">
            <div bind:this={mapElement} class="h-72 w-full z-10"></div>

            <div class="absolute inset-0 pointer-events-none ring-1 ring-inset ring-black/5"></div>

            {#if locationLoading || locationError || !auth.token || dataLoading || dataError}
                <div
                  class="absolute inset-0 z-20 flex items-center justify-center bg-[#F5F2EA]/95 p-6"
                  role="status"
                  aria-live="polite"
                >
                    <div class="text-center text-[#2F5D50]">
                        <p class="font-bold">
                            {locationError || dataError || (
                              locationLoading
                                ? 'Helyzeted meghatározása…'
                                : !auth.token
                                  ? 'Bejelentkezésre várunk…'
                                  : 'Térkép és kalandok betöltése…'
                            )}
                        </p>

                        {#if locationError || dataError}
                            <button
                              type="button"
                              onclick={() => window.location.reload()}
                              class="mt-4 rounded-xl bg-[#2F5D50] px-5 py-3 font-bold text-white"
                            >
                                Újrapróbálás
                            </button>
                        {/if}
                    </div>
                </div>
            {/if}
        </div>
    </section>

    <section class="w-full max-w-md mx-auto">
        <h2 class="label-city mb-4 ml-2">Új kalandok a közeledben</h2>

        <header class="grid grid-cols-[2fr_1fr_1fr] px-5 mb-3 text-[10px] font-black text-[#2F5D50] opacity-40 uppercase tracking-[0.2em]">
            <span>Kaland</span>
            <span class="text-center">Idő</span>
            <span class="text-right">Távolság</span>
        </header>

        <div class="space-y-4">
            {#if userPos && auth.token && !dataLoading && !locationError && !dataError && adventures.length === 0}
                <p class="px-5 py-6 text-center font-bold text-[#2F5D50]">
                    Nincs megjeleníthető kaland a közeledben.
                </p>
            {/if}
            {#each adventures as adventure (adventure.id)}
                <button
                  class="grid grid-cols-[2fr_1fr_1fr] w-full items-center bg-[#8D7462]/90 p-5 rounded-3xl shadow-xl transition-all active:scale-[0.98] text-left border border-white/10"
                  onclick={() => goto(`/adventures/${adventure.id}`)}
                >
                    <div class="overflow-hidden">
                        <span class="font-black text-white text-lg leading-tight truncate block">{adventure.title}</span>
                    </div>

                    <span class="text-center text-s font-bold text-[#F5F2EA]">
                        {adventure.averageTime ? `${formatTime(adventure.averageTime)}` : '-'}
                    </span>

                    <div class="text-right">
                        <span class="text-s font-black text-white px-2 py-1 rounded-lg shadow-inner">
                            {adventure.distanceInMeters >= 1000 ? (adventure.distanceInMeters / 1000).toFixed(1) + ' km' : Math.round(adventure.distanceInMeters) + ' m'}
                        </span>
                    </div>
                </button>
            {/each}
        </div>
    </section>
</main>
<style>
    .label-city {
        @apply text-[14px] font-black uppercase tracking-[0.2em] text-[#2F5D50] opacity-80;
    }
</style>