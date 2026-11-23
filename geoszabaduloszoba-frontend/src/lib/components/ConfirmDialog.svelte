<script lang="ts">
	import { onMount } from 'svelte';

	let {
		title,
		description,
		confirmLabel,
		onConfirm,
		onCancel
	}: {
		title: string;
		description: string;
		confirmLabel: string;
		onConfirm: () => Promise<void>;
		onCancel: () => void;
	} = $props();

	const id = $props.id();
	let dialog: HTMLDialogElement;
	let pending = $state(false);

	onMount(() => {
		dialog.showModal();
	});

	async function confirm() {
		pending = true;
		try {
			await onConfirm();
		} finally {
			pending = false;
		}
	}
</script>

<dialog
	bind:this={dialog}
	class="m-auto w-[calc(100%_-_3rem)] max-w-sm rounded-3xl border-2 border-[#8D7462] bg-[#F5F2EA] p-8 shadow-2xl backdrop:bg-black/60 backdrop:backdrop-blur-sm"
	aria-labelledby={`${id}-title`}
	aria-describedby={`${id}-description`}
	oncancel={(event) => {
		event.preventDefault();
		if (!pending) onCancel();
	}}
>
	<h3 id={`${id}-title`} class="mb-4 text-xl font-bold text-[#2F5D50]">{title}</h3>
	<p id={`${id}-description`} class="mb-8 text-sm leading-relaxed text-[#8D7462]">{description}</p>
	<footer class="flex gap-4">
		<button type="button" onclick={onCancel} disabled={pending} class="flex-1 rounded-xl py-3 font-bold text-gray-500">
			Mégse
		</button>
		<button type="button" onclick={confirm} disabled={pending} class="flex-1 rounded-xl bg-red-600 py-3 font-bold text-white shadow-lg transition-all active:scale-95 disabled:opacity-60">
			{confirmLabel}
		</button>
	</footer>
</dialog>
