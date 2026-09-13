import { auth } from '$lib/auth.svelte';

if (typeof window !== 'undefined') {
	const originalFetch = window.fetch;

	window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
		let urlString =
			typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

		if (urlString.includes('http://localhost:8080') || urlString.startsWith('/api/')) {
			const isProduction =
				window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1';

			const baseUrl = isProduction ? 'https://api.zsomborszintai.com' : 'http://localhost:8080';

			if (urlString.startsWith('/api/')) {
				urlString = `${baseUrl}${urlString}`;
			} else {
				urlString = urlString.replace('http://localhost:8080', baseUrl);
			}
		}

		const headers = new Headers(init?.headers);
		if (auth.token && !headers.has('Authorization')) {
			headers.set('Authorization', `Bearer ${auth.token}`);
		}

		return originalFetch(urlString, {
			...init,
			headers
		});
	};
}
