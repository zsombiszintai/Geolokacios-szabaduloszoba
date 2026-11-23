<script lang="ts">
	let {
		src,
		alt,
		class: className = 'w-full h-full object-cover',
		httpsOnly = false
	}: { src?: string | null; alt: string; class?: string; httpsOnly?: boolean } = $props();

	const fallback = '/images/default-avatar.png';
	const avatarSrc = $derived(
		typeof src === 'string' && src && (!httpsOnly || src.startsWith('https://')) ? src : fallback
	);
</script>

{#key avatarSrc}
	<img
		src={avatarSrc}
		{alt}
		class={className}
		onerror={({ currentTarget }) => {
			if (currentTarget.getAttribute('src') !== fallback) currentTarget.setAttribute('src', fallback);
		}}
	/>
{/key}
