import {css, html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import manager from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import "@openremote/or-components/or-collapsible-panel";
import "@openremote/or-mwc-components/or-mwc-input";
import '@openremote/or-configuration/or-conf-realm/index'
import '@openremote/or-configuration/or-conf-rules/index'

export function pageConfigurationProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "configuration",
        routes: [
            "/configuration",
        ],
        pageCreator: () => {
            return new PageConfiguration(store);
        }
    };
}

@customElement("page-configuration")
export class PageConfiguration extends Page<AppStateKeyed>  {

    static get styles() {
        // language=CSS
        return css`
            :host {
                --or-collapisble-panel-background-color: #fff;
            }
        `;
    }

    get name(): string {
        return "configuration";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    protected render(): TemplateResult | void {

        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        const managerConfiguration = manager.managerAppConfig

        return html`
            <div class="conf-category-content-container">
                <or-conf-realm .realms="${managerConfiguration?.realms}"></or-conf-realm>
                <or-conf-rules .rules="${managerConfiguration?.pages?.rules}"></or-conf-rules>
            </div>
        `;


    }

    public stateChanged(state: AppStateKeyed) {
    }
}
