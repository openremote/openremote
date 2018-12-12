import '@polymer/iron-demo-helpers/demo-pages-shared-styles';
import '@polymer/iron-demo-helpers/demo-snippet';
import "@openremote/or-map";
import "@openremote/or-map-marker/src/or-map-marker-basic";
import openremote, {Auth} from "@openremote/core";

openremote.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "http://localhost:8080/auth",
    managerUrl: "http://localhost:8080",
    realm: "customerA"
});

