import type Keycloak from 'keycloak-js';
import type { KeycloakInitOptions } from 'keycloak-js';

let realKeycloak: Keycloak | null = null;

const keycloak = {
	async init(options: KeycloakInitOptions) {
		if (typeof window !== 'undefined') {
			if (!realKeycloak) {
				const KeycloakModule = await import('keycloak-js');
				const Keycloak = KeycloakModule.default || KeycloakModule;

				const keycloakUrl = 'https://auth.zsomborszintai.com';

				realKeycloak = new Keycloak({
					url: keycloakUrl,
					realm: 'cityscape-realm',
					clientId: 'cityscape-frontend'
				});
			}
			return await realKeycloak.init(options);
		}
		return false;
	},
	logout() {
		if (realKeycloak) {
			realKeycloak.logout();
		}
	},
	login() {
		return realKeycloak?.login({ redirectUri: window.location.origin + '/dashboard' });
	},
	register() {
		return realKeycloak?.register({ redirectUri: window.location.origin + '/dashboard' });
	},
	get token() {
		return realKeycloak?.token;
	},
	get tokenParsed() {
		return realKeycloak?.tokenParsed;
	}
};

export default keycloak;
