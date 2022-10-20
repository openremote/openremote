import {css, html, TemplateResult} from "lit";
import {customElement ,property} from "lit/decorators.js";
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
import '@openremote/or-configuration/or-conf-navigation/index'

export function pageConfigurationProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "configuration",
        routes: [
            "/configuration",
            "/configuration/:category",
            "/configuration/:category/:subcategory"
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
            .shadow{
                box-shadow: rgb(0 0 0 / 10%) 5px 5px 5px -5px;
            }
            .conf-navigation-container{
                width: 15%;
                min-width: 250px;
                max-width: 300px;
                padding: 15px 0 0 30px;
            }
            .conf-category-content-container{
                width: 100%;
                padding: 15px;
            }
            .conf-navigation-bar{
                background-color: #fff;
                border-radius: 4px;
                min-width: 150px;
                width: 100%;
            }
            :host {
                --or-collapisble-panel-background-color: #fff;
            }
        `;
    }

    get name(): string {
        return "configuration";
    }

    @property()
    protected _category: string = "style/realms";

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
            <div class="conf-navigation-container">
                <div class="conf-navigation-bar shadow">
                    <or-conf-navigation></or-conf-navigation>
                </div>
            </div>
            <div class="conf-category-content-container">
                <or-conf-realm .realms="${managerConfiguration?.realms}"></or-conf-realm>
                <or-conf-rules .rules="${managerConfiguration?.pages?.rules}"></or-conf-rules>
            </div>
        `;


    }

    public stateChanged(state: AppStateKeyed) {
        this._category = !!state.app.params ? state.app.params.category : null;
        if (this._category === null){
            location.hash = "/configuration/style/realms"
            this._category = "/configuration/style/realms"
        }
    }
}
