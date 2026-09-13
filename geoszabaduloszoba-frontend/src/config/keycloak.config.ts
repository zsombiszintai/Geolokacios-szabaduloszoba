let realKeycloak = null;

const keycloak = {
	async init(options) {
		if (typeof window !== 'undefined') {
			if (!realKeycloak) {
				const KeycloakModule = await import('keycloak-js');
				const Keycloak = KeycloakModule.default || KeycloakModule;
				realKeycloak = new Keycloak({
					url: 'http://localhost:8090',
					realm: 'cityscape-realm',
					clientId: 'cityscape-frontend-client'
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
