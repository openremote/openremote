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
import "../components/configuration/or-conf-json";
import "../components/configuration/or-conf-panel";
import "../components/configuration/or-conf-map/or-conf-map-global";
import {ManagerAppConfig, MapConfig, Realm} from "@openremote/model";
import "@openremote/or-components/or-loading-indicator";
import {OrConfRealmCard} from "../components/configuration/or-conf-realm/or-conf-realm-card";
import {OrConfPanel} from "../components/configuration/or-conf-panel";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import {DefaultHeaderMainMenu, DefaultHeaderSecondaryMenu, DefaultRealmConfig} from "../index";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";

declare const APP_VERSION: string;

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
                --or-panel-padding: 12px 24px 24px;
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

            .subheader {
                padding: 10px 0 4px;
                font-weight: bolder;
            }
        `;
    }

    get name(): string {
        return "appearance";
    }

    @state()
    public managerConfiguration?: ManagerAppConfig;

    @state()
    public mapConfig?: MapConfig;

    @state()
    protected realms?: Realm[];

    @state()
    protected loading = false;

    @state()
    protected managerConfigurationChanged = false;

    @state()
    protected mapConfigChanged = false;

    @state()
    protected customMapFilename: string;

    @state()
    protected customMapLimit: number;

    @state()
    protected tilesForUpload: File;

    @state()
    protected tilesForDeletion = false;

    @query("#managerConfig-panel")
    protected realmConfigPanel?: OrConfPanel;


    /* ------------------------------------------ */

    public stateChanged(state: AppStateKeyed) {
    }

    public async firstUpdated() {
        const response = await manager.rest.api.MapResource.getCustomMapInfo();
        this.customMapLimit = response.data.limit as number;
        this.customMapFilename = response.data.filename as string | null;
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
                const saveDisabled = !this.managerConfigurationChanged && !this.mapConfigChanged && !this.tilesForUpload && !this.tilesForDeletion;
                const realmHeading = html`
                    <div id="heading" style="justify-content: space-between;">
                        <span style="margin: 0;"><or-translate style="text-transform: uppercase;" value="configuration.realmStyling"></or-translate></span>
                        <or-conf-json .heading="${'manager_config.json'}" .config="${this.managerConfiguration}" class="hide-mobile" @saveLocalConfig="${(ev: CustomEvent) => {
                            this.managerConfiguration = ev.detail.value as ManagerAppConfig;
                            this.managerConfigurationChanged = true;
                        }}"
                        ></or-conf-json>
                    </div>`;
                const mapHeading = html`<div id="heading" style="justify-content: space-between;">
                        <span style="margin: 0;"><or-translate style="text-transform: uppercase;" value="configuration.mapSettings"></or-translate></span>
                    </div>`;
                const realmOptions = this.realms?.map((r) => ({name: r.name, displayName: r.displayName, canDelete: true}));
                realmOptions.push({name: 'default', displayName: 'Default', canDelete: false});
                return html`
                    <div id="wrapper">
                        <div id="header-wrapper">
                            <div id="header-title">
                                <or-icon icon="palette-outline"></or-icon>
                                <or-translate value="appearance"></or-translate>
                            </div>
                            <div id="header-actions">
                                <or-mwc-input id="save-btn" .disabled="${saveDisabled}" raised type="button" label="save"
                                    @or-mwc-input-changed="${() => this.saveAllConfigs(this.managerConfiguration, this.mapConfig)}"
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
                                    <span><or-translate value="configuration.managerConfigNotFound"></or-translate></span>
                                    <or-mwc-input type="${InputType.BUTTON}" label="configuration.tryAgain"
                                        @or-mwc-input-changed="${() => this.getManagerConfig().then(val => {
                                            this.managerConfiguration = val;
                                        }).catch(e => console.error(e))}"
                                    ></or-mwc-input>
                                </div>
                            `)}
                        </or-panel>
                        <or-panel .heading="${mapHeading}">
                            ${when(this.mapConfig, () => html`
                                <or-conf-map-global .config="${this.mapConfig}" .filename="${this.customMapFilename}" .limit="${this.customMapLimit}"
                                    @change="${() => { this.mapConfigChanged = true; }}"
                                    @map-file-changed="${(e: CustomEvent) => {
                                        // If a map file is provided prepare for upload
                                        if (e.detail) {
                                            this.tilesForUpload = e.detail;
                                        } else {
                                            this.customMapFilename = undefined;
                                        }
                                        // Otherwise assume it is meant for deletion
                                        this.tilesForDeletion = !e.detail;
                                    }}"
                                ></or-conf-map-global>
                                <div class="subheader"><or-translate value="configuration.realmMapSettingsTitle"></or-translate></div>
                                <or-conf-panel id="mapConfig-panel" .config="${this.mapConfig}" .realmOptions="${realmOptions}"
                                               @change="${() => { this.mapConfigChanged = true; }}"
                                ></or-conf-panel>
                            `, () => html`
                                <div class="notFound-container">
                                    <span><or-translate value="configuration.mapSettingsNotFound"></or-translate></span>
                                    <or-mwc-input type="${InputType.BUTTON}" label="configuration.tryAgain"
                                        @or-mwc-input-changed="${() => this.getMapConfig().then(val => {
                                            this.mapConfig = val;
                                        }).catch(e => console.error(e))}"
                                    ></or-mwc-input>
                                </div>
                            `)}
                        </or-panel>
                        <div  style="margin: 0px auto; font-size: smaller;">
                            OpenRemote Manager v${APP_VERSION}
                        </div>
                    </div>
                `
            })}
        `;
    }


    /* ---------------- */

    // FETCH METHODS

    protected async getManagerConfig(): Promise<ManagerAppConfig | undefined> {
        const response = await manager.rest.api.ConfigurationResource.getManagerConfig();
        return response.status === 200 ? response.data as ManagerAppConfig : {
            realms: {
                default: {
                    appTitle: DefaultRealmConfig.appTitle,
                    headers: [...Object.keys(DefaultHeaderMainMenu),...Object.keys(DefaultHeaderSecondaryMenu)]
                }
            }
        };
    }

    protected async getMapConfig(): Promise<MapConfig> {
        const response = await manager.rest.api.MapResource.getSettings();
        if (response.data) {
          const { options, sources, layers, glyphs, override, sprite } = response.data as MapConfig;
          return { options, sources, layers, glyphs, override, sprite };
        }
        return null;
    }

    protected async getAccessibleRealms(): Promise<Realm[]> {
        return (await manager.rest.api.RealmResource.getAccessible()).data;
    }

    protected saveAllConfigs(config: ManagerAppConfig, mapConfig: MapConfig) {
        this.loading = true;
        let managerPromise;

        // Save the images to the server that have been uploaded by the user.
        // TODO: Optimize code so it only saves images that have been changed.
        const filePromises = [];
        if(this.realmConfigPanel !== undefined) {
            const elems = this.realmConfigPanel.getCardElements() as OrConfRealmCard[];
            elems.forEach((elem, index) => {
                const files = elem?.getFiles();
                Object.entries(files).forEach(async ([x, y]) => {
                    filePromises.push(
                        manager.rest.api.ConfigurationResource.fileUpload(y, {path: (y as any).path}).then(file =>{
                            config.realms[elem.name][x] = file.data;
                        })
                    );
                });
            })
            this.managerConfiguration = config;
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

        if (this.tilesForUpload) {
            showSnackbar(undefined, "configuration.global.uploadingMapTiles");
            const filename = this.tilesForUpload.name;
            filePromises.push(manager.rest.api.MapResource.uploadMap({ filename }, {
              headers: {'Content-Type': 'application/octet-stream'},
              data: this.tilesForUpload
            }).then(({ data }) => {
                this.customMapFilename = filename;
                this.mapConfig = data as MapConfig;
            }).catch((reason) => {
                showSnackbar(undefined, "configuration.global.uploadingMapTilesError");
                console.error(reason);
            }).finally(() => {
                this.tilesForUpload = null;
            }));
        }

        // We first wait for the filePromises to finish, so that
        // we can use the path returned from the backend to store to the
        // manager_config.
        Promise.all(filePromises).then((arr:string[]) => {
            // Wait for all requests to complete, then finish loading.
            const promises = [
                this.managerConfigurationChanged ? manager.rest.api.ConfigurationResource.update(config) : null,
                this.mapConfigChanged ?  mapPromise : null
            ];
            Promise.all(promises).finally(() => {
                // The deletion must happen after the config changes since deletion will re-center to the default map.
                if (this.tilesForDeletion && !this.tilesForUpload) {
                    manager.rest.api.MapResource.deleteMap().then(({ data }) => {
                        this.tilesForDeletion = false;
                        this.customMapFilename = undefined;
                        this.mapConfig = data as MapConfig;
                    }).catch((reason) => {
                        console.error(reason);
                    });
                }
                this.requestUpdate();
                this.loading = false;
                this.managerConfigurationChanged = false;
                this.mapConfigChanged = false;
                const configURL = (manager.managerUrl ?? "") + "/api/master/configuration/manager";
                fetch(configURL, {cache: "reload"})
                window.location.reload();
            })
        })

    }
}
