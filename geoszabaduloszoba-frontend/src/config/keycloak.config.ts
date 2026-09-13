let realKeycloak = null;

const keycloak = {
	async init(options) {
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
	get token() {
		return realKeycloak?.token;
	},
	get tokenParsed() {
		return realKeycloak?.tokenParsed;
	}
};

export default keycloak;