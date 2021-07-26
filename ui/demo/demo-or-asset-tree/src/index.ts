import "@polymer/iron-demo-helpers/demo-pages-shared-styles";
import "@polymer/iron-demo-helpers/demo-snippet";
import "@openremote/or-asset-tree";
import manager, {Auth} from "@openremote/core";
import {html, LitElement, TemplateResult, css} from "lit";
import {customElement, property} from "lit/decorators.js";

@customElement("or-demo-app")
class App extends LitElement {

    static get styles() {
        return css`
            :host {
                color: #4c4c4c;
            }
        
            or-asset-tree {
                width: 450px;
                height: 1000px;
                border: 1px solid grey;
            }
        `;
    }

    @property()
    public loaded: boolean = false;

    protected render(): TemplateResult | void {
        if (!this.loaded) {
            return;
        }

        return html`
            <demo-snippet>
                <template>
                    <or-asset-tree></or-asset-tree>
                </template>
            </demo-snippet>
        `;
    }
}

manager.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "http://localhost:8080/auth",
    managerUrl: "http://localhost:8080",
    realm: "smartcity"
}).then(() => {
    const app = document.getElementsByTagName("or-demo-app")[0] as App;
    app.loaded = true;
});
