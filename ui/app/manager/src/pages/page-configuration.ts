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
import {css, html, TemplateResult, PropertyValues, unsafeCSS} from "lit";
import {customElement, state, query} from "lit/decorators.js";
import manager, {DefaultColor1, DefaultColor3} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {when} from "lit/directives/when.js"
import "@openremote/or-components/or-collapsible-panel";
import "@openremote/or-mwc-components/or-mwc-input";
import "../components/configuration/or-conf-json";
import "../components/configuration/or-conf-panel";
import {ManagerAppConfig, MapRealmConfig, Realm} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";
import {OrConfRealmCard} from "../components/configuration/or-conf-realm/or-conf-realm-card";
import {OrConfPanel} from "../components/configuration/or-conf-panel";
import {Input} from "@openremote/or-rules/lib/flow-viewer/services/input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

declare const CONFIG_URL_PREFIX: string;

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
            
            .notFound-container {
                display: flex;
                flex-direction: column;
                gap: 24px;
            }

            @media screen and (max-width: 768px) {
                or-panel {
                    border-left: 0px;
                    border-right: 0px;
                    width: 100%;
                    --or-panel-border-radius: 0;
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
    public managerConfiguration?: ManagerAppConfig;

    @state()
    public mapConfig?: {[id: string]: any};

    @state()
    protected realms?: Realm[];

    @state()
    protected loading: boolean = false;

    @state()
    protected managerConfigurationChanged = false;

    @state()
    protected mapConfigChanged = false;

    @query("#managerConfig-panel")
    protected realmConfigPanel?: OrConfPanel;

    private readonly urlPrefix: string = (CONFIG_URL_PREFIX || "")


    /* ------------------------------------------ */

    public stateChanged(state: AppStateKeyed) {
    }

    // On every update..
    willUpdate(changedProps: PropertyValues<this>) {

        if(!this.loading) {
            let managerConfigPromise;
            if(this.managerConfiguration === undefined) {
                managerConfigPromise = this.getManagerConfig().then((value) => {
                    this.managerConfiguration = value;
                }).catch(() => {
                    this.managerConfiguration = null;
                });
            }
            let mapConfigPromise;
            if(this.mapConfig === undefined) {
                mapConfigPromise = this.getMapConfig().then((value) => {
                    this.mapConfig = value;
                }).catch(() => {
                    this.mapConfig = null;
                });
            }
            let realmsPromise;
            if(this.realms === undefined) {
                realmsPromise = this.getAccessibleRealms().then((value) => {
                    this.realms = value;
                }).catch(() => {
                    this.realms = null;
                });
            }

            // Wait for both promises to complete, to only update UI once.
            if(managerConfigPromise || mapConfigPromise || realmsPromise) {
                this.loading = true;
                Promise.all([managerConfigPromise, mapConfigPromise, realmsPromise]).finally(() => {
                    this.loading = false;
                })
            }
        }
    }


    /* ------------------------ */

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`<or-translate value="notAuthenticated"></or-translate>`;
        }
        return html`
            ${when(this.loading, () => html`
                <or-loading-indicator></or-loading-indicator>
            `, () => {
                const realmHeading = html`
                    <div id="heading" style="justify-content: space-between;">
                        <span style="margin: 0;">${i18next.t("configuration.realmStyling").toUpperCase()}</span>
                        <or-conf-json .managerConfig="${this.managerConfiguration}" class="hide-mobile"
                                      @saveLocalManagerConfig="${(ev: CustomEvent) => {
                                          this.managerConfiguration = ev.detail.value as ManagerAppConfig;
                                          this.managerConfigurationChanged = true;
                                      }}"
                        ></or-conf-json>
                    </div>
                `;
                const realmOptions = this.realms?.map((r) => ({name: r.name, displayName: r.displayName, canDelete: true}));
                realmOptions.push({name: 'default', displayName: 'Default', canDelete: false});
                return html`
                    <div id="wrapper">
                        <div id="header-wrapper">
                            <div id="header-title">
                                <or-icon icon="palette-outline"></or-icon>
                                ${i18next.t("appearance")}
                            </div>
                            <div id="header-actions">
                                <or-mwc-input id="save-btn" .disabled="${!this.managerConfigurationChanged && !this.mapConfigChanged}" raised type="button" label="save"
                                              @click="${() => this.saveAllConfigs(this.managerConfiguration, this.mapConfig)}"
                                ></or-mwc-input>
                            </div>
                        </div>
                        <or-panel .heading="${realmHeading}">
                            ${when(this.managerConfiguration, () => html`
                                <or-conf-panel id="managerConfig-panel" .config="${this.managerConfiguration}" .realmOptions="${realmOptions}"
                                               @change="${() => { this.managerConfigurationChanged = true; }}"
                                ></or-conf-panel>
                            `, () => html`
                                <div class="notFound-container">
                                    <span>${i18next.t('configuration.managerConfigNotFound')}</span>
                                    <or-mwc-input type="${InputType.BUTTON}" label="configuration.tryAgain"
                                                  @or-mwc-input-changed="${() => this.getManagerConfig().then(val => {
                                                      this.managerConfiguration = val;
                                                  }).catch(e => console.error(e))}"
                                    ></or-mwc-input>
                                </div>
                            `)}
                        </or-panel>
                        <or-panel .heading="${i18next.t("configuration.mapSettings").toUpperCase()}">
                            ${when(this.mapConfig, () => html`
                                <or-conf-panel id="mapConfig-panel" .config="${this.mapConfig}" .realmOptions="${realmOptions}"
                                               @change="${() => { this.mapConfigChanged = true; }}"
                                ></or-conf-panel>
                            `, () => html`
                                <div class="notFound-container">
                                    <span>${i18next.t('configuration.mapSettingsNotFound')}</span>
                                    <or-mwc-input type="${InputType.BUTTON}" label="configuration.tryAgain"
                                                  @or-mwc-input-changed="${() => this.getMapConfig().then(val => {
                                                      this.mapConfig = val;
                                                  }).catch(e => console.error(e))}"
                                    ></or-mwc-input>
                                </div>
                            `)}
                        </or-panel>
                    </div>
                `
            })}
        `;
    }


    /* ---------------- */

    // FETCH METHODS

    protected async getManagerConfig(): Promise<ManagerAppConfig | undefined> {
        const response = await fetch(this.urlPrefix + "/manager_config.json", { cache: "reload" });
        return await response.json() as ManagerAppConfig;
    }

    protected async getMapConfig(): Promise<{[id: string]: any}> {
        const response = await manager.rest.api.MapResource.getSettings();
        return (response.data.options as {[id: string]: any});
    }

    protected async getAccessibleRealms(): Promise<Realm[]> {
        return (await manager.rest.api.RealmResource.getAccessible()).data;
    }

    protected saveAllConfigs(config: ManagerAppConfig, mapConfig: {[p: string]: MapRealmConfig}) {
        this.loading = true;
        let managerPromise;

        // POST manager config if changed...
        if (this.managerConfigurationChanged) {
            managerPromise = manager.rest.api.ConfigurationResource.update(config).then(() => {
                fetch(this.urlPrefix + "/manager_config.json", {cache: "reload"});
                this.managerConfiguration = config;
                Object.entries(this.managerConfiguration.realms).forEach(([name, settings]) => {
                    fetch(this.urlPrefix + settings?.favicon, {cache: "reload"});
                    fetch(this.urlPrefix + settings?.logo, {cache: "reload"});
                    fetch(this.urlPrefix + settings?.logoMobile, {cache: "reload"});
                });
            }).catch((reason) => {
                console.error(reason);
            });
        }

        // POST map config if changed...
        let mapPromise;
        if(this.mapConfigChanged) {
            mapPromise = manager.rest.api.MapResource.saveSettings(mapConfig)
                .then(() => {
                    this.mapConfig = mapConfig;
                }).catch((reason) => {
                    console.error(reason);
                });
        }

        // Save the images to the server that have been uploaded by the user.
        // TODO: Optimize code so it only saves images that have been changed.
        const imagePromises = [];
        if(this.realmConfigPanel !== undefined) {
            const elems = this.realmConfigPanel.getCardElements() as OrConfRealmCard[];
            elems.forEach((elem, index) => {
                const files = elem?.getFiles();
                Object.entries(files).forEach(async ([x, y]) => {
                    imagePromises.push(manager.rest.api.ConfigurationResource.fileUpload(y, {path: x}));
                });
            })
        }

        // Wait for all requests to complete, then finish loading.
        const promises = [...imagePromises, managerPromise, mapPromise];
        Promise.all(promises).finally(() => {
            this.loading = false;
            this.managerConfigurationChanged = false;
            this.mapConfigChanged = false;
        })
    }
}
