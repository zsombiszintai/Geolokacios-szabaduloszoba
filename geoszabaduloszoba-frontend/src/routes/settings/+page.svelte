<script lang="ts">
	import { auth } from '$lib/auth.svelte.js';
	import { onMount, onDestroy } from 'svelte';
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

	let restorePageScroll: (() => void) | undefined;
	let disposed = false;

	const CROP_RATIO = 0.76;

	type TouchPoint = {
		x: number;
		y: number;
	};

	const pointers = new Map<number, TouchPoint>();

	const clamp = (value: number, min: number, max: number) =>
		Math.max(min, Math.min(max, value));

	function lockPageScroll() {
		if (restorePageScroll) return;

		const body = document.body;
		const root = document.documentElement;
		const scrollX = window.scrollX;
		const scrollY = window.scrollY;

		const bodyProperties = [
			'position', 'top', 'left', 'width', 'overflow'
		];

		const savedBody = bodyProperties.map((name) => ({
			name,
			value: body.style.getPropertyValue(name),
			priority: body.style.getPropertyPriority(name)
		}));

		const rootOverflow = root.style.getPropertyValue('overflow');
		const rootOverflowPriority = root.style.getPropertyPriority('overflow');

		body.style.setProperty('position', 'fixed');
		body.style.setProperty('top', `-${scrollY}px`);
		body.style.setProperty('left', `-${scrollX}px`);
		body.style.setProperty('width', '100%');
		body.style.setProperty('overflow', 'hidden');
		root.style.setProperty('overflow', 'hidden');

		restorePageScroll = () => {
			for (const property of savedBody) {
				if (property.value) {
					body.style.setProperty(
						property.name,
						property.value,
						property.priority
					);
				} else {
					body.style.removeProperty(property.name);
				}
			}

			if (rootOverflow) {
				root.style.setProperty(
					'overflow',
					rootOverflow,
					rootOverflowPriority
				);
			} else {
				root.style.removeProperty('overflow');
			}

			window.scrollTo({
				left: scrollX,
				top: scrollY,
				behavior: 'instant'
			});
		};
	}

	function unlockPageScroll() {
		restorePageScroll?.();
		restorePageScroll = undefined;
	}

	function cleanupCrop() {
		pointers.clear();
		cropImage = null;
		cropError = '';
		unlockPageScroll();
	}

	onDestroy(() => {
		disposed = true;
		unlockPageScroll();
		pointers.clear();
	});

	function getAvatarSrc(value: unknown): string {
		return typeof value === 'string' && value.startsWith('https://')
			? value
			: defaultAvatar;
	}

	function getGesture() {
		const points = [...pointers.values()];

		if (points.length === 0) return null;

		if (points.length === 1) {
			return {
				x: points[0].x,
				y: points[0].y,
				distance: 0
			};
		}

		const [a, b] = points;

		return {
			x: (a.x + b.x) / 2,
			y: (a.y + b.y) / 2,
			distance: Math.hypot(b.x - a.x, b.y - a.y)
		};
	}

	function transformCrop(
		element: HTMLElement,
		oldX: number,
		oldY: number,
		newX: number,
		newY: number,
		requestedZoom: number
	) {
		if (!cropImage) return;

		const rect = element.getBoundingClientRect();
		const cropWidth = rect.width * CROP_RATIO;

		if (cropWidth <= 0) return;

		const imageWidth = cropImage.naturalWidth;
		const imageHeight = cropImage.naturalHeight;
		const shortestSide = Math.min(imageWidth, imageHeight);

		const oldSize = shortestSide / zoom;
		const nextZoom = clamp(requestedZoom, 1, 6);
		const nextSize = shortestSide / nextZoom;

		const oldSourceX =
			(imageWidth - oldSize) * (horizontal + 100) / 200;

		const oldSourceY =
			(imageHeight - oldSize) * (vertical + 100) / 200;

		const screenCenterX = rect.left + rect.width / 2;
		const screenCenterY = rect.top + rect.height / 2;

		// A kép azon pontja, amely az ujjak közepe alatt volt.
		const anchorX =
			oldSourceX + oldSize / 2 +
			(oldX - screenCenterX) * oldSize / cropWidth;

		const anchorY =
			oldSourceY + oldSize / 2 +
			(oldY - screenCenterY) * oldSize / cropWidth;

		const nextSourceX = clamp(
			anchorX -
			(newX - screenCenterX) * nextSize / cropWidth -
			nextSize / 2,
			0,
			imageWidth - nextSize
		);

		const nextSourceY = clamp(
			anchorY -
			(newY - screenCenterY) * nextSize / cropWidth -
			nextSize / 2,
			0,
			imageHeight - nextSize
		);

		zoom = nextZoom;

		horizontal = imageWidth > nextSize
			? nextSourceX / (imageWidth - nextSize) * 200 - 100
			: 0;

		vertical = imageHeight > nextSize
			? nextSourceY / (imageHeight - nextSize) * 200 - 100
			: 0;
	}

	function startDrag(event: PointerEvent) {
		if (!cropImage || uploadLoading || pointers.size >= 2) return;
		if (event.pointerType === 'mouse' && event.button !== 0) return;

		const element = event.currentTarget as HTMLDivElement;

		pointers.set(event.pointerId, {
			x: event.clientX,
			y: event.clientY
		});

		element.setPointerCapture(event.pointerId);
	}

	function moveDrag(event: PointerEvent) {
		if (!pointers.has(event.pointerId) || uploadLoading) return;

		const before = getGesture();

		pointers.set(event.pointerId, {
			x: event.clientX,
			y: event.clientY
		});

		const after = getGesture();

		if (!before || !after) return;

		const factor = pointers.size === 2 && before.distance > 0
			? after.distance / before.distance
			: 1;

		transformCrop(
			event.currentTarget as HTMLDivElement,
			before.x,
			before.y,
			after.x,
			after.y,
			zoom * factor
		);
	}

	function endDrag(event: PointerEvent) {
		pointers.delete(event.pointerId);

		const element = event.currentTarget as HTMLDivElement;

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
		if (!cropCanvas || !cropImage) return;

		const context = cropCanvas.getContext('2d');
		if (!context) return;

		const size = cropCanvas.width;
		const cropSize = size * CROP_RATIO;
		const margin = (size - cropSize) / 2;

		const sourceSize =
			Math.min(cropImage.naturalWidth, cropImage.naturalHeight) / zoom;

		const sourceX =
			(cropImage.naturalWidth - sourceSize) * (horizontal + 100) / 200;

		const sourceY =
			(cropImage.naturalHeight - sourceSize) * (vertical + 100) / 200;

		const scale = cropSize / sourceSize;

		context.clearRect(0, 0, size, size);
		context.fillStyle = '#f5f2ea';
		context.fillRect(0, 0, size, size);

		context.imageSmoothingEnabled = true;
		context.imageSmoothingQuality = 'high';

		// Az előnézet a körön kívüli képrészt is megmutatja.
		context.drawImage(
			cropImage,
			margin - sourceX * scale,
			margin - sourceY * scale,
			cropImage.naturalWidth * scale,
			cropImage.naturalHeight * scale
		);
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

			if (disposed) return;

			pointers.clear();

			zoom = 1;
			horizontal = 0;
			vertical = 0;
			cropError = '';
			cropImage = image;

			cropDialog.showModal();
			lockPageScroll();
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
	onclose={cleanupCrop}
>
	<div class="avatar-editor">
		<header class="avatar-editor-header">
			<h2 id="avatar-editor-title">Profilkép igazítása</h2>
		</header>

		<div
			class="avatar-crop-stage"
			role="group"
			aria-label="Profilkép húzása és nagyítása"
			onpointerdown={startDrag}
			onpointermove={moveDrag}
			onpointerup={endDrag}
			onpointercancel={endDrag}
			onlostpointercapture={endDrag}
		>
			<canvas
				bind:this={cropCanvas}
				width="800"
				height="800"
				role="img"
				aria-label="Profilkép előnézete"
			></canvas>

			<div class="avatar-crop-mask" aria-hidden="true"></div>
		</div>

		{#if cropError}
			<p role="alert" class="px-6 text-sm font-bold text-red-700">
				{cropError}
			</p>
		{/if}

		<footer class="avatar-editor-actions">
			<button
				type="button"
				onclick={closeCrop}
				disabled={uploadLoading}
				class="avatar-cancel"
			>
				Mégse
			</button>

			<button
				type="button"
				onclick={saveCroppedAvatar}
				disabled={uploadLoading || !cropImage}
				class="avatar-save"
			>
				{uploadLoading ? 'Feltöltés...' : 'Mentés'}
			</button>
		</footer>
	</div>
</dialog>

<style>
    :global(body) {
        background-color: #F5F2EA;
    }
    .avatar-dialog {
        width: min(440px, calc(100vw - 24px));
        max-height: calc(100dvh - 24px);
        margin: auto;
        padding: 0;
        overflow-y: auto;
        overscroll-behavior: contain;
        border: none;
        border-radius: 28px;
        background: #f5f2ea;
        color: #2f5d50;
        box-shadow: 0 24px 80px rgb(0 0 0 / 25%);
    }

    .avatar-dialog::backdrop {
        background: rgb(0 0 0 / 45%);
        backdrop-filter: blur(5px);
    }

    .avatar-editor {
        padding: 24px 0 0;
    }

    .avatar-editor-header {
        padding: 0 24px 20px;
    }

    .avatar-editor-header h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 800;
        text-transform: uppercase;
    }

    .avatar-crop-stage {
        position: relative;
        width: 100%;
        max-width: 50dvh;
        margin: 0 auto;
        aspect-ratio: 1;
        overflow: hidden;
        touch-action: none;
        user-select: none;
        -webkit-user-select: none;
        cursor: grab;
        background: #f5f2ea;
    }

    .avatar-crop-stage:active {
        cursor: grabbing;
    }

    .avatar-crop-stage canvas {
        display: block;
        width: 100%;
        height: 100%;
        pointer-events: none;
    }

    .avatar-crop-mask {
        position: absolute;
        inset: 12%;
        border-radius: 50%;
        box-shadow:
                0 0 0 999px rgb(0 0 0 / 28%),
                inset 0 0 0 2px rgb(255 255 255 / 95%);
        pointer-events: none;
    }

    .avatar-editor-actions {
        display: flex;
        gap: 12px;
        padding: 20px 24px max(24px, env(safe-area-inset-bottom));
    }

    .avatar-editor-actions button {
        flex: 1;
        min-height: 48px;
        border-radius: 16px;
        font-weight: 700;
    }

    .avatar-cancel {
        border: 1px solid rgb(141 116 98 / 30%);
        background: transparent;
        color: #8d7462;
    }

    .avatar-save {
        border: none;
        background: #2f5d50;
        color: white;
    }

    .avatar-dialog button:disabled {
        opacity: 0.45;
    }
</style>