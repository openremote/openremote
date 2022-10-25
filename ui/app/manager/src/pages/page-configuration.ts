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
import '@openremote/or-configuration/or-conf-json'

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

            .panel {
                width: calc(100% - 90px);
                max-width: 1310px;
                background-color: white;
                border: 1px solid #e5e5e5;
                border-radius: 5px;
                position: relative;
                margin: 5px auto;
                padding: 24px;
            }

            .panel-title {
                text-transform: uppercase;
                font-weight: bolder;
                line-height: 1em;
                margin-bottom: 20px;
                margin-top: 0;
                flex: 0 0 auto;
                letter-spacing: 0.025em;
            }

            #title {
                padding: 0 20px;
                font-size: 18px;
                font-weight: bold;
                width: calc(100% - 40px);
                max-width: 1360px;
                margin: 20px auto;
                align-items: center;
                display: flex;
            }

            #title or-icon {
                margin-right: 10px;
                margin-left: 14px;
            }
            .conf-category-content-container{
                width: 100%;
            }

            #header {
                width: 100%;
            }
            #header-wrapper {
                /*padding: 14px 30px;*/
                display: flex;
                width: calc(100% - 40px);
                max-width: 1360px;
                flex-direction: row;
                align-items: center;
                margin: 20px auto;
            }
            #header-title {
                font-size: 18px;
                font-weight: bold;
            }
            #header-title > or-icon {
                margin-right: 10px;
            }
            #header-actions {
                flex: 1 1 auto;
                text-align: right;
            }
            #header-actions-content {
                display: flex;
                flex-direction: row;
                align-items: center;
                float: right;
            }
            #header-actions-content or-mwc-input{
                margin-left: 6px;
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
                <div id="header">
                    <div id="header-wrapper">
                        <div id="header-title">
                            <or-icon icon="cog"></or-icon>
                            Configuration
                        </div>
                        <div id="header-actions">
                            <div id="header-actions-content">
                                <or-conf-json .managerConfig="${managerConfiguration}"></or-conf-json>
                                <or-mwc-input id="save-btn" raised="" type="button" label="Opslaan"></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="panel">
                    <div class="panel-title">
                        Assets
                    </div>
                </div>
                <div class="panel">
                    <div class="panel-title">
                        Map
                    </div>
                </div>
                <div class="panel">
                    <div class="panel-title">
                        Realms
                    </div>
                    <or-conf-realm .realms="${managerConfiguration?.realms}"></or-conf-realm>
                </div>

                <div class="panel">
                    <div class="panel-title">
                        Rules
                    </div>
                    <or-conf-rules .rules="${managerConfiguration?.pages?.rules}"></or-conf-rules>
                </div>
            </div>
        `;


    }

    public stateChanged(state: AppStateKeyed) {
    }
}
