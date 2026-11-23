<script lang="ts">
	let {
		message,
		class: className = 'py-24',
		large = false,
		visuallyHiddenMessage = false
	}: { message: string; class?: string; large?: boolean; visuallyHiddenMessage?: boolean } = $props();
</script>

<div class="flex flex-col items-center justify-center gap-4 {className}" role="status">
	<span aria-hidden="true" class="block {large ? 'size-12' : 'size-10'} animate-spin rounded-full border-4 border-[#2F5D50] border-t-transparent"></span>
	<p class:sr-only={visuallyHiddenMessage} class="font-bold text-[#2F5D50]">{message}</p>
</div>
