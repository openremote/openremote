import {html, render} from "lit-html";
import {when} from "lit-html/directives/when";
import openremote, {Auth, Manager} from "@openremote/core";



let loggedInTemplate = (openremote:Manager) => html `<span>Welcome ${openremote.username}</span> (<a href="${openremote.getLogoutUrl()}">logout</a>)`;
let loggedOutTemplate = (openremote:Manager) => html `<span>Please <a href="${openremote.getLoginUrl()}">login</a>`;

let mainTemplate = (openremote: Manager) => html`
<p><b>Message:</b> ${when(openremote.authenticated, () => loggedInTemplate(openremote), () => loggedOutTemplate(openremote))}</p>
<br/>
<p><b>Initialised: </b> ${openremote.initialised}</p>
<p><b>Manager Version: </b> ${openremote.managerVersion}</p>
<p><b>Authenticated: </b> ${openremote.authenticated}</p>
<p><b>Username: </b> ${openremote.username}</p>
<p><b>Roles: </b> ${openremote.roles ? openremote.roles.join(", ") : ""}</p>
<p><b>Is Super User: </b> ${openremote.isSuperUser()}</p>
<p><b>Is Manager Same Origin: </b> ${openremote.isManagerSameOrigin()}</p>

<p><b>Is Error: </b> ${openremote.isError}</p>
<p><b>Error:</b> ${openremote.error}</p>
<p><b>Config: </b> ${openremote.config ? JSON.stringify(openremote.config) : ""}</p>
`;

function refresh() {
    render(mainTemplate(openremote), document.body);
}

openremote.addListener(event => {
    console.log("OR Event:" + event);
});

openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: false,
    realm: "master"
}).then(refresh);