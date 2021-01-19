import {html, render} from "lit-html";
import manager, {Auth, Manager} from "@openremote/core";
import {AssetQuery} from "@openremote/model";

let loggedInTemplate = (manager: Manager) => html `<span>Welcome ${manager.username}</span>(<button @click="${ ()=> {manager.logout()}}">logout</button>)`;
let loggedOutTemplate = (manager: Manager) => html `<span>Please</span><button @click="${() => {manager.login()}}">login</button>`;

function renderUi() {
    if (manager.authenticated) {

        let queryAssetsTemplate = html`
            ${loggedInTemplate(manager)}
            <br />
            <button @click="${() => queryAssets()}">Get Assets</button><span> (see console window)</span>
        `;
        render(queryAssetsTemplate, document.body);
    } else {
        render(loggedOutTemplate(manager), document.body);
    }
}

function queryAssets() {
    let assetQuery: AssetQuery = {};
    manager.rest.api.AssetResource.queryAssets(assetQuery).then(response => {
        console.log("Received: " + response.data.length + " Asset(s)");
        console.log(JSON.stringify(response.data, null, 2))
    }).catch(reason => console.log("Error:" + reason));
}

manager.addListener(event => {
    console.log("OR Event:" + event);
});

manager.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: "smartcity"
}).then(renderUi);
