import { PUBLIC_API_URL } from '$env/static/public';
import { auth } from '$lib/auth.svelte';

if (typeof window !== 'undefined') {
	const originalFetch = window.fetch;

	window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
		let urlString = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

		if (urlString.includes('http://localhost:8080')) {
			const isProduction = window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1';
			const fallbackApi = isProduction ? 'https://zsomborszintai.com' : 'http://localhost:8080';

			const baseUrl = PUBLIC_API_URL || fallbackApi;
			urlString = urlString.replace('http://localhost:8080', baseUrl);
		}

		const headers = new Headers(init?.headers);
		if (auth.token && !headers.has('Authorization')) {
			headers.set('Authorization', `Bearer ${auth.token}`);
		}

		return originalFetch(urlString, {
			...init,
			headers,
		});
	};
}