import keycloak from '../config/keycloak.config';

let authenticated = $state(false);
let loading = $state(true);
let error = $state<string | null>(null);

async function init() {
    try {
        authenticated = await keycloak.init({
            onLoad: 'login-required',
            checkLoginIframe: false
        });
    } catch (e) {
        error = e instanceof Error ? e.message : 'Authentication failed';
    } finally {
        loading = false;
    }
}

function logout() {
    keycloak.logout();
}

export const auth = {
    get authenticated() { return authenticated; },
    get loading() { return loading; },
    get error() { return error; },
		get token(): string | undefined { return keycloak.token; },
		get username() { return keycloak.tokenParsed?.preferred_username as string | undefined; },
    get fullName() { return keycloak.tokenParsed?.name as string | undefined; },
    init,
    logout
};
