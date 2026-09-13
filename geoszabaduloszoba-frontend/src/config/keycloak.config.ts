import Keycloak from "keycloak-js";

let keycloak;

if (typeof window !== 'undefined') {
	keycloak = new Keycloak({
		url: "http://localhost:8090",
		realm: "cityscape-realm",
		clientId: "cityscape-frontend-client"
	});
} else {
	keycloak = {
		init: async () => false,
		logout: () => {},
		token: undefined,
		tokenParsed: {}
	};
}

export default keycloak;