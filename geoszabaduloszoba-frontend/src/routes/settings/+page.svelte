<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { CameraPhotoOutline } from 'flowbite-svelte-icons';

	let profile = $state<any>(null);
	let uploadLoading = $state(false);
	let description = $state("");
	let message = $state({ text: "", type: "" });

	const defaultAvatar = '/images/default-avatar.png';

	let cropDialog: HTMLDialogElement;
	let cropCanvas = $state<HTMLCanvasElement>();
	let cropImage = $state.raw<HTMLImageElement | null>(null);

	let zoom = $state(1);
	let horizontal = $state(0);
	let vertical = $state(0);
	let cropError = $state('');
	let imageLoading = $state(false);

	let drag: {
		pointerId: number;
		startX: number;
		startY: number;
		horizontal: number;
		vertical: number;
		width: number;
		sourceSize: number;
		imageWidth: number;
		imageHeight: number;
	} | null = null;

	let isDragging = $state(false);

	function getAvatarSrc(value: unknown): string {
		return typeof value === 'string' && value.startsWith('https://')
			? value
			: defaultAvatar;
	}

	function startDrag(event: PointerEvent) {
		if (!cropImage || uploadLoading || drag) return;
		if (event.pointerType === 'mouse' && event.button !== 0) return;

		const element = event.currentTarget as HTMLDivElement;
		const width = element.getBoundingClientRect().width;

		if (!width) return;

		element.setPointerCapture(event.pointerId);

		drag = {
			pointerId: event.pointerId,
			startX: event.clientX,
			startY: event.clientY,
			horizontal,
			vertical,
			width,
			sourceSize:
				Math.min(cropImage.naturalWidth, cropImage.naturalHeight) / zoom,
			imageWidth: cropImage.naturalWidth,
			imageHeight: cropImage.naturalHeight
		};

		isDragging = true;
	}

	function moveDrag(event: PointerEvent) {
		if (!drag || event.pointerId !== drag.pointerId || uploadLoading) return;

		const dx = event.clientX - drag.startX;
		const dy = event.clientY - drag.startY;

		const availableX = drag.imageWidth - drag.sourceSize;
		const availableY = drag.imageHeight - drag.sourceSize;

		if (availableX > 0) {
			horizontal = Math.max(-100, Math.min(100,
				drag.horizontal -
				(dx / drag.width) * drag.sourceSize / availableX * 200
			));
		}

		if (availableY > 0) {
			vertical = Math.max(-100, Math.min(100,
				drag.vertical -
				(dy / drag.width) * drag.sourceSize / availableY * 200
			));
		}
	}

	function endDrag(event: PointerEvent) {
		if (!drag || event.pointerId !== drag.pointerId) return;

		const element = event.currentTarget as HTMLDivElement;

		drag = null;
		isDragging = false;

		if (element.hasPointerCapture(event.pointerId)) {
			element.releasePointerCapture(event.pointerId);
		}
	}

	function drawCrop(
		canvas: HTMLCanvasElement,
		image: HTMLImageElement,
		scale: number,
		x: number,
		y: number
	) {
		const context = canvas.getContext('2d');
		if (!context) throw new Error('A képszerkesztő nem indítható el.');

		const size = canvas.width;

		const sourceSize = Math.min(image.naturalWidth, image.naturalHeight) / scale;
		const sourceX = (image.naturalWidth - sourceSize) * (x + 100) / 200;
		const sourceY = (image.naturalHeight - sourceSize) * (y + 100) / 200;

		context.clearRect(0, 0, size, size);
		context.fillStyle = '#ffffff';
		context.fillRect(0, 0, size, size);
		context.imageSmoothingEnabled = true;
		context.imageSmoothingQuality = 'high';

		context.drawImage(
			image,
			sourceX, sourceY, sourceSize, sourceSize,
			0, 0, size, size
		);
	}

	$effect(() => {
		if (cropCanvas && cropImage) {
			drawCrop(cropCanvas, cropImage, zoom, horizontal, vertical);
		}
	});

	function closeCrop() {
		if (uploadLoading) return;

		cropDialog.close();
		cropImage = null;
		cropError = '';
	}

	async function fetchCurrentSettings() {
		if (!auth.token) return;
		try {
			const res = await fetch('https://api.zsomborszintai.com/profile/me', {
				headers: { 'Authorization': `Bearer ${auth.token}` }
			});
			if (res.ok) {
				profile = await res.json();
				description = profile.profileDescription || "";
			}
		} catch (err) {
			console.error("Hiba a betöltéskor:", err);
		}
	}

	async function handleSaveDescription() {
		if (!auth.token) return;

		try {
			const res = await fetch('https://api.zsomborszintai.com/settings/description', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Bearer ${auth.token}`
				},
				body: JSON.stringify({ description })
			});

			if (res.ok) {
				if (profile) profile.profileDescription = description;
				message = { text: "Leírás sikeresen frissítve!", type: "success" };
			} else {
				message = { text: "Hiba történt a mentés során.", type: "error" };
			}
		} catch (err) {
			message = { text: "Hálózati hiba történt.", type: "error" };
		} finally {
			setTimeout(() => message = { text: "", type: "" }, 3000);
		}
	}

	async function handleAvatarUpload(event: Event) {
		const input = event.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		input.value = '';

		if (!file || imageLoading || uploadLoading) return;

		if (!auth.token || !profile) {
			message = {
				text: 'Várd meg a profil betöltését.',
				type: 'error'
			};
			return;
		}

		if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
			message = {
				text: 'JPG, PNG vagy WebP képet válassz.',
				type: 'error'
			};
			return;
		}

		imageLoading = true;
		const objectUrl = URL.createObjectURL(file);

		try {
			const image = new Image();
			image.src = objectUrl;
			await image.decode();

			zoom = 1;
			horizontal = 0;
			vertical = 0;
			cropError = '';
			cropImage = image;

			cropDialog.showModal();
		} catch (error) {
			console.error('Képmegnyitási hiba:', error);
			message = {
				text: 'A képet nem sikerült megnyitni.',
				type: 'error'
			};
		} finally {
			URL.revokeObjectURL(objectUrl);
			imageLoading = false;
		}
	}

	async function saveCroppedAvatar() {
		if (!cropImage || !auth.token || !profile || uploadLoading) return;

		uploadLoading = true;
		cropError = '';

		try {
			const canvas = document.createElement('canvas');
			canvas.width = 512;
			canvas.height = 512;

			drawCrop(canvas, cropImage, zoom, horizontal, vertical);

			const blob = await new Promise<Blob>((resolve, reject) => {
				canvas.toBlob(
					(result) => {
						if (result) resolve(result);
						else reject(new Error('Nem sikerült elkészíteni a képet.'));
					},
					'image/jpeg',
					0.9
				);
			});

			const formData = new FormData();
			formData.append('file', blob, 'avatar.jpg');

			const res = await fetch(
				'https://api.zsomborszintai.com/settings/avatar',
				{
					method: 'POST',
					headers: {
						Authorization: `Bearer ${auth.token}`
					},
					body: formData
				}
			);

			if (!res.ok) {
				throw new Error(`A feltöltés sikertelen (HTTP ${res.status}).`);
			}

			const data = await res.json();

			if (
				typeof data.avatarUrl !== 'string' ||
				!data.avatarUrl.startsWith('https://')
			) {
				throw new Error(
					'A feltöltés után nem érkezett érvényes HTTPS kép-URL.'
				);
			}

			profile.profilePictureUrl = data.avatarUrl;

			cropDialog.close();
			cropImage = null;

			message = {
				text: 'Profilkép sikeresen frissítve!',
				type: 'success'
			};
		} catch (error) {
			cropError = error instanceof Error
				? error.message
				: 'A profilkép mentése sikertelen.';
		} finally {
			uploadLoading = false;
		}
	}

	onMount(fetchCurrentSettings);
</script>

<main class="min-h-screen bg-[#F5F2EA] font-josefin pb-24 px-6 pt-6">
	<button
		type="button"
		class="flex items-center gap-2 text-[#8D7462] hover:text-[#2F5D50] transition-colors group mb-8"
		onclick={() => window.history.back()}
	>
		<div class="p-2 rounded-xl bg-white shadow-sm group-hover:shadow-md transition-all">
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</div>
		<span class="text-[11px] font-black uppercase tracking-widest">Vissza a profilra</span>
	</button>

	<header class="mb-8">
		<h2 class="text-3xl font-black text-[#2F5D50] leading-none uppercase tracking-tighter">
			Beállítások
		</h2>
		<div class="w-12 h-1.5 bg-[#8D7462] mt-4 rounded-full"></div>
	</header>

	<div class="max-w-md mx-auto space-y-6">

		<section class="bg-white p-8 rounded-[40px] shadow-sm border border-[#2F5D50]/5 flex flex-col items-center">
			<div class="relative group">
				<div class="w-32 h-32 rounded-full overflow-hidden border-4 border-[#F5F2EA] shadow-lg rotate-3 group-hover:rotate-0 transition-transform duration-500">
					{#if profile}
						<img
							src={getAvatarSrc(profile.profilePictureUrl)}
							alt="Profilkép"
							class="w-full h-full object-cover"
							onerror={(event) => {
								const image = event.currentTarget;
								const fallback = new URL(defaultAvatar, window.location.origin).href;

								if (image.src !== fallback) {
									image.src = fallback;
								}
							}}
						/>
					{:else}
						<div class="w-full h-full bg-[#8D7462]/10 animate-pulse"></div>
					{/if}
				</div>

				<label class="absolute -bottom-2 -right-2 bg-[#2F5D50] text-white p-3 rounded-3xl shadow-xl cursor-pointer hover:scale-110 active:scale-90 transition-all border-4 border-white">
					<CameraPhotoOutline class="w-5 h-5" />
					<input
						type="file"
						accept="image/jpeg,image/png,image/webp"
						aria-label="Új profilkép kiválasztása"
						class="hidden"
						onchange={handleAvatarUpload}
						disabled={uploadLoading || imageLoading || !profile}
					/>
				</label>

				{#if uploadLoading}
					<div class="absolute inset-0 bg-white/60 rounded-[32px] flex items-center justify-center backdrop-blur-[2px]">
						<div class="w-8 h-8 border-4 border-[#2F5D50] border-t-transparent rounded-full animate-spin"></div>
					</div>
				{/if}
			</div>

			<p class="mt-6 text-[10px] font-black text-[#8D7462] uppercase tracking-[0.2em]">Profilkép módosítása</p>
		</section>

		<section class="bg-white p-6 rounded-[40px] shadow-sm border border-[#2F5D50]/5 space-y-5">
			<div>
				<label class="block text-[11px] font-black text-[#8D7462] uppercase tracking-widest mb-3 ml-2">Bemutatkozás</label>
				<textarea
					bind:value={description}
					rows="4"
					class="w-full p-5 rounded-[24px] bg-[#F5F2EA]/50 border-2 border-transparent focus:border-[#2F5D50]/20 focus:bg-white outline-none transition-all resize-none text-[#2F5D50] font-medium"
					placeholder="Írj magadról néhány szót..."
				></textarea>
			</div>

			<button
				class="w-full bg-[#2F5D50] text-[#F5F2EA] py-4 rounded-[20px] font-black uppercase tracking-widest shadow-lg shadow-[#2F5D50]/20 hover:bg-[#1e3d34] active:scale-[0.97] transition-all flex items-center justify-center gap-2"
				onclick={handleSaveDescription}
			>
				Változtatások mentése
			</button>
		</section>
	</div>
	{#if message.text}
		<div
			transition:fly={{ y: 20, duration: 500 }}
			class="fixed bottom-20 left-6 right-6 flex justify-center z-50"
		>
			<div class="px-6 py-3 rounded-2xl shadow-2xl font-black uppercase tracking-widest text-[10px] flex items-center gap-3
          {message.type === 'success' ? 'bg-[#2F5D50]/90 text-white' : 'bg-[#8D7462] text-white'}">
				{#if message.type === 'info'}
					<div class="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
				{/if}
				{message.text}
			</div>
		</div>
	{/if}
</main>

<dialog
	bind:this={cropDialog}
	aria-labelledby="avatar-editor-title"
	class="avatar-dialog font-josefin"
	oncancel={(event) => {
    event.preventDefault();
    closeCrop();
  }}
	onclose={() => {
    cropImage = null;
  }}
>
	<div class="p-6 sm:p-8">
		<h2
			id="avatar-editor-title"
			class="text-2xl font-black uppercase text-[#2F5D50]"
		>
			Profilkép igazítása
		</h2>

		<p class="mt-2 mb-6 text-sm text-[#8D7462]">
			Nagyíts és igazítsd a képet a kör közepére.
		</p>

		<div
			class="avatar-crop-area mx-auto w-full max-w-64 aspect-square overflow-hidden rounded-full ring-4 ring-white shadow-lg"
			class:dragging={isDragging}
			role="group"
			aria-label="Profilkép igazítása húzással vagy a nyílbillentyűkkel"
			tabindex="0"
			onpointerdown={startDrag}
			onpointermove={moveDrag}
			onpointerup={endDrag}
			onpointercancel={endDrag}
			onlostpointercapture={endDrag}
			onkeydown={moveWithKeyboard}
		>
			<canvas
				bind:this={cropCanvas}
				width="512"
				height="512"
				class="block w-full h-full pointer-events-none"
				role="img"
				aria-label="A kivágott profilkép előnézete"
			></canvas>
		</div>

		{#if cropError}
			<p role="alert" class="mt-4 text-sm font-bold text-red-700">
				{cropError}
			</p>
		{/if}

		<div class="mt-7 flex gap-3">
			<button
				type="button"
				onclick={closeCrop}
				disabled={uploadLoading}
				class="flex-1 rounded-2xl border border-[#8D7462]/30 px-4 py-3 font-bold text-[#8D7462] disabled:opacity-50"
			>
				Mégse
			</button>

			<button
				type="button"
				onclick={saveCroppedAvatar}
				disabled={uploadLoading || !cropImage}
				class="flex-1 rounded-2xl bg-[#2F5D50] px-4 py-3 font-bold text-white disabled:opacity-50"
			>
				{uploadLoading ? 'Feltöltés...' : 'Mentés'}
			</button>
		</div>
	</div>
</dialog>

<style>
    :global(body) {
        background-color: #F5F2EA;
    }
    .avatar-dialog {
        width: min(440px, calc(100vw - 32px));
        max-height: calc(100dvh - 32px);
        margin: auto;
        padding: 0;
        overflow-y: auto;
        border: none;
        border-radius: 28px;
        background: #f5f2ea;
        box-shadow: 0 24px 80px rgb(0 0 0 / 25%);
    }

    .avatar-dialog::backdrop {
        background: rgb(0 0 0 / 50%);
        backdrop-filter: blur(4px);
    }
</style>