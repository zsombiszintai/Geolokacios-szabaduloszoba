<script lang="ts">
	import { ChevronLeftOutline } from 'flowbite-svelte-icons';

	let { label = 'Vissza', boxed = false }: { label?: string; boxed?: boolean } = $props();
</script>

<button
	type="button"
	class="group mb-8 flex w-fit items-center gap-2 text-[#8D7462] transition-colors hover:text-[#2F5D50]"
	onclick={() => window.history.back()}
>
	{#if boxed}
		<span class="block rounded-xl bg-white p-2 shadow-sm transition-all group-hover:shadow-md">
			<ChevronLeftOutline class="size-5" aria-hidden="true" />
		</span>
		<span class="text-[11px] font-black uppercase tracking-widest">{label}</span>
	{:else}
		<ChevronLeftOutline class="size-5 transition-transform group-hover:-translate-x-1" aria-hidden="true" />
		<span class="font-bold">{label}</span>
	{/if}
</button>
