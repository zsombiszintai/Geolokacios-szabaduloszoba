import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "http://localhost:8090",
    realm: "cityscape-realm",
    clientId: "cityscape-frontend-client"
});

export default keycloak;