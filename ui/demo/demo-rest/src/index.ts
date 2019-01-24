import {html, render} from "lit-html";
import openremote, {Auth, Manager} from "@openremote/core";
import rest from "@openremote/rest";
import {AssetQuery} from "@openremote/model";

let loggedInTemplate = (openremote: Manager) => html`<span>Welcome ${openremote.username}</span> (<a href="${openremote.getLogoutUrl()}">logout</a>)`;
let loggedOutTemplate = (openremote: Manager) => html`<span>Please <a href="${openremote.getLoginUrl()}">login</a>`;

function renderUi() {
    if (openremote.authenticated) {

        let queryAssetsTemplate = html`
            ${loggedInTemplate(openremote)}
            <br />
            <button @click=${() => queryAssets()}>Get Assets</button><span> (see console window)</span>
        `;
        render(queryAssetsTemplate, document.body);
    } else {
        render(loggedOutTemplate(openremote), document.body);
    }
}

function queryAssets() {
    let assetQuery: AssetQuery = {};
    rest.api.AssetResource.queryAssets(assetQuery).then(response => {
        console.log("Received: " + response.data.length + " Asset(s)");
        console.log(JSON.stringify(response.data, null, 2))
    }).catch(reason => console.log("Error:" + reason));
}

openremote.addListener(event => {
    console.log("OR Event:" + event);
});

openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: "tenantA"
}).then(renderUi);