/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import manager, {DefaultColor1, DefaultColor3} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {when} from "lit/directives/when.js"
import "@openremote/or-components/or-collapsible-panel";
import "@openremote/or-mwc-components/or-mwc-input";
import "../components/configuration/or-conf-json";
import "../components/configuration/or-conf-realm/index";
import "../components/configuration/or-conf-map/index";
import {ManagerAppConfig} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";

declare var CONFIG_URL_PREFIX: string;

export function pageConfigurationProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "configuration",
        routes: [
            "configuration",
        ],
        pageCreator: () => {
            return new PageConfiguration(store);
        }
    };
}

@customElement("page-configuration")
export class PageConfiguration extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
            .main-content {
                display: unset !important;
            }

            :host {
                flex: 1;
                width: 100%;

                display: flex;
                justify-content: center;

                --or-collapisble-panel-background-color: #fff;
                --or-panel-background-color: #fff;
                --or-panel-padding: 18px 24px 24px;
                --or-panel-heading-margin: 0 0 10px 0;
                --or-panel-background-color: var(--or-app-color1, ${unsafeCSS(DefaultColor1)});
                --or-panel-heading-font-size: 14px;
            }

            or-panel {
                width: calc(100% - 40px);
                max-width: 1360px;
                margin-bottom: 16px;
            }

            #wrapper {
                display: flex;
                min-width: 0px;
                width: 100%;
                height: 100%;
                flex-direction: column;
                align-items: center;
                overflow: auto;
            }

            #header-wrapper {
                display: flex;
                width: calc(100% - 40px);
                max-width: 1360px;
                padding: 0 20px;
                flex-direction: row;
                align-items: center;
                justify-content: space-between;
                margin: 15px auto;
            }

            #header-title {
                font-size: 18px;
                font-weight: bold;
                align-items: center;
                display: flex;
                color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
            }

            #header-title > or-icon {
                margin-right: 10px;
                margin-left: 14px;
            }

            #header-actions or-mwc-input {
                margin-left: 12px;
            }

            or-icon {
                vertical-align: middle;
                --or-icon-width: 20px;
                --or-icon-height: 20px;
                margin-right: 2px;
                margin-left: -5px;
            }

            @media screen and (max-width: 768px) {
                or-panel {
                    border-left: 0px;
                    border-right: 0px;
                    width: 100%;
                    --or-panel-border-radius: 0;
                }

                #header-wrapper {
                    /*width: 100%*/
                }

                .hide-mobile {
                    display: none;
                }
            }
        `;
    }

    get name(): string {
        return "appearance";
    }

    @state()
    protected managerConfiguration?: ManagerAppConfig;

    @state()
    protected mapConfig: {[id: string]: any};

    @state()
    protected promises: Map<string, Promise<any>> = new Map();

    private readonly urlPrefix: string = (CONFIG_URL_PREFIX || "")


    /* ------------------------------------------ */

    public stateChanged(state: AppStateKeyed) {
    }

    // On every update..
    willUpdate(changedProps: Map<string, any>) {
        console.log(changedProps); // TODO: Temporary use for testing purposes
        if(this.managerConfiguration === undefined && !this.promises.has('getManagerConfig')) {
            this.getManagerConfig(false).then((config) => {
                this.managerConfiguration = config
            }).catch((reason) => {
                console.error(reason);
                this.managerConfiguration = null;
            }).finally(() => {
                this.completePromise('getManagerConfig');
            });
        }
        if(this.mapConfig === undefined && !this.promises.has('getMapConfig')) {
            this.getMapConfig(false).then((config) => {
                this.mapConfig = config;
            }).catch((reason) => {
                console.error(reason);
                this.mapConfig = null;
            }).finally(() => {
                this.completePromise('getMapConfig');
            });
        }
    }


    /* ------------------------ */

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`<or-translate value="notAuthenticated"></or-translate>`;
        }
        const loading = this.isLoading();
        return html`
            ${when(loading, () => html`
                <or-loading-indicator></or-loading-indicator>
            `, () => {
                const realmHeading = html`
                    <div id="heading" style="justify-content: space-between;">
                        <span style="margin: 0;">${i18next.t("configuration.realmStyling")}</span>
                            <or-conf-json .managerConfig="${this.managerConfiguration}" class="hide-mobile"
                                          @saveLocalManagerConfig="${(ev: CustomEvent) => { this.managerConfiguration = ev.detail.value as ManagerAppConfig }}"
                            ></or-conf-json>
                    </div>
                `;
                return html`
                    <div id="wrapper">
                        <div id="header-wrapper">
                            <div id="header-title">
                                <or-icon icon="palette-outline"></or-icon>
                                ${i18next.t("appearance")}
                            </div>
                            <div id="header-actions">
                                <!--<or-conf-json .managerConfig="${this.managerConfiguration}" class="hide-mobile"
                                              @saveLocalManagerConfig="${(ev: CustomEvent) => { this.managerConfiguration = ev.detail.value as ManagerAppConfig }}"
                                ></or-conf-json>-->
                                <or-mwc-input id="save-btn" raised type="button" .label="${i18next.t("save")}" @click="${() => this.saveAllConfigs(this.managerConfiguration)}"></or-mwc-input>
                            </div>
                        </div>
                        <or-panel .heading="${realmHeading}">
                            <or-conf-realm .config="${this.managerConfiguration}"></or-conf-realm>
                        </or-panel>
                        <or-panel .heading="${i18next.t("configuration.mapSettings").toUpperCase()}">
                            <or-conf-map .config="${this.mapConfig}"></or-conf-map>
                        </or-panel>
                    </div>
                `
            })}
        `;
    }


    /* ---------------- */

    // FETCH METHODS

    protected async getManagerConfig(autoComplete = true): Promise<ManagerAppConfig | undefined> {
        console.error("getManagerConfig()");
        const promise = fetch(this.urlPrefix + "/manager_config.json", { cache: "reload" });
        this.registerPromise('getManagerConfig', promise, autoComplete);
        const response = await promise;
        return await response.json() as ManagerAppConfig;
    }

    protected async getMapConfig(autoComplete = true): Promise<{[id: string]: any}> {
        console.error("getMapConfig()");
        const promise = manager.rest.api.MapResource.getSettings();
        this.registerPromise('getMapConfig', promise, autoComplete);
        const response = await promise;
        return (response.data.options as {[id: string]: any});
    }

    // TODO: Improve this code
    protected saveAllConfigs(config: ManagerAppConfig) {
        manager.rest.api.ConfigurationResource.update(config)
            .then(() => {
                fetch(this.urlPrefix + "/manager_config.json", {cache: "reload"});
                this.managerConfiguration = config;
                Object.entries(this.managerConfiguration.realms).map(([name, settings]) => {
                    fetch(this.urlPrefix + settings?.favicon, {cache: "reload"});
                    fetch(this.urlPrefix + settings?.logo, {cache: "reload"});
                    fetch(this.urlPrefix + settings?.logoMobile, {cache: "reload"});
                });
            }).catch((reason) => {
                console.error(reason);
        })
        manager.rest.api.MapResource.saveSettings(this.mapConfig).then(() => {
        })
    }

    /* ---------------------------------------------------- */

    // PROMISE HANDLING

    protected registerPromise(index: string, promise: Promise<any>, autoComplete = true) {
        this.promises.set(index, promise);
        this.requestUpdate("promises");
        if(autoComplete) {
            promise.finally(() => {
                this.completePromise(index);
            });
        }
    }

    protected completePromise(index: string) {
        this.promises.delete(index);
        this.requestUpdate("promises");
    }

    protected isLoading(): boolean {
        return this.promises.size > 0;
    }
}
