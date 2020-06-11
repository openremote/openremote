import {css, customElement, html, property, PropertyValues, TemplateResult, unsafeCSS} from "lit-element";
import manager from "@openremote/core";
import "@openremote/or-panel";
import "@openremote/or-translate";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageAccountProvider<S extends AppStateKeyed>(store: EnhancedStore<S>) {
    return {
        routes: [
            "account"
        ],
        pageCreator: () => {
            return new PageAccount(store);
        }
    };
}

@customElement("page-account")
class PageAccount<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`
            iframe {
                width: 100%;    
            }
        `;
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render(): TemplateResult | void {

        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        if (!manager.isKeycloak()) {
            return html`
                <or-translate value="notSupported"></or-translate>
            `;
        }

        return html`
            <iframe .src="${manager.keycloakUrl + "/realms/" + manager.getRealm() + "/account/"}"></iframe>
        `;
    }

    public stateChanged(state: S) {
    }
}
