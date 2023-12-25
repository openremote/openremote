import {html, LitElement, TemplateResult} from "lit";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {customElement, property} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';
import {ManagerAppConfig} from "@openremote/model";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";
import "./or-conf-map/or-conf-map-card";
import "./or-conf-realm/or-conf-realm-card";
import {OrConfRealmCard} from "./or-conf-realm/or-conf-realm-card";
import {OrConfMapCard} from "./or-conf-map/or-conf-map-card";

@customElement("or-conf-panel")
export class OrConfPanel extends LitElement {

    @property()
    public config?: {[id: string]: any} | ManagerAppConfig = {};

    @property()
    public realmOptions: { name: string, displayName: string, canDelete: boolean }[] = [];

    protected _addedRealm: null | string = null

    public getCardElements(): OrConfRealmCard[] | OrConfMapCard[] | undefined {
        if(this.isManagerConfig(this.config)) {
            return Array.from(this.shadowRoot?.querySelectorAll('or-conf-realm-card')) as OrConfRealmCard[];
        } else if (this.isMapConfig(this.config)) {
            return Array.from(this.shadowRoot?.querySelectorAll('or-conf-map-card')) as OrConfMapCard[];
        }
    }

    protected getRealmsProperty(config: unknown): { [index: string]: any } | undefined {
        if(this.isManagerConfig(config)) {
            return config.realms;
        } else if(this.isMapConfig(config)) {
            return config;
        }
        return undefined;
    }

    protected isMapConfig(object: unknown): boolean {
        const keys = Object.keys(object);
        return this.realmOptions.filter((o) => keys.includes(o.name)).length === keys.length;
    }

    protected isManagerConfig(object: any): object is ManagerAppConfig {
        return 'realms' in object;
    }

    protected notifyConfigChange(config: {[id: string]: any} | ManagerAppConfig) {
        this.dispatchEvent(new CustomEvent("change", { detail: config }));
    }

    /* ------------------------------ */

    render(): TemplateResult {

        // Define the type of config
        let type: 'managerconfig' | 'mapconfig' | undefined;
        if(this.isManagerConfig(this.config)) {
            type = 'managerconfig';
        } else if(this.isMapConfig(this.config)) {
            type = 'mapconfig';
        }

        // Render the panels
        const realmConfigs = this.getRealmsProperty(this.config);
        const availableRealms = this.getAvailableRealms(this.config, this.realmOptions);
        return html`
            <div class="panels">
                ${Object.entries(realmConfigs === undefined ? {} : realmConfigs).map(([key, value]) => {
                    const realmOption = this.realmOptions.find((r) => r.name === key);
                    switch (type) {
                        case "managerconfig":
                            return html`
                                <or-conf-realm-card .expanded="${this._addedRealm === key}" .name="${key}" .realm="${value}" .canRemove="${realmOption?.canDelete}"
                                                    @change="${() => this.notifyConfigChange(this.config)}" @remove="${() => this._removeRealm(key)}"
                                ></or-conf-realm-card>
                            `;
                        case "mapconfig":
                            return html`
                                <or-conf-map-card .expanded="${this._addedRealm === key}" .name="${key}" .map="${value}" .canRemove="${realmOption?.canDelete}"
                                                  @change="${() => this.notifyConfigChange(this.config)}" @remove="${() => this._removeRealm(key)}"
                                ></or-conf-map-card>
                            `;
                        default:
                            return html`Unknown error.`
                    }
                })}
            </div>
            <!-- Show an "ADD REALM" button if there are realms available to be added -->
            <div style="display: flex; justify-content: space-between;">
                ${when(availableRealms.length > 0, () => html`
                    <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" label="${type === 'mapconfig' ? 'configuration.addMapCustomization' : 'configuration.addRealmCustomization'}" icon="plus"
                                  @click="${() => this._showAddingRealmDialog()}"
                    ></or-mwc-input>
                `)}
            </div>
        `
    }


    /* ----------------------------------- */

    protected _removeRealm(realm: string) {
        const realms = this.getRealmsProperty(this.config);
        if (realms) {
            delete realms[realm];
            this.requestUpdate("config")
            this.notifyConfigChange(this.config);
        } else {
            console.error("No config found when attempting to remove realm.")
        }
    }

    // Filter the list of realms that are not present in the config.
    // Most used for the "add realm" dialog, to hide the realms that are already present.
    protected getAvailableRealms(config?: ManagerAppConfig | {[id: string]: any}, realmOptions?: { name: string, displayName: string, canDelete: boolean }[]): { name: string, displayName: string, canDelete: boolean }[] {
        const realms = this.getRealmsProperty(config);
        if(realms) {
            return realmOptions.filter((r) => {
                if (r.name in realms) {
                    return null;
                }
                return r;
            }).sort((a, b) => {
                if (a.displayName && b.displayName) {
                    return (a.displayName > b.displayName) ? 1 : -1;
                }
                return -1;
            })
        } else {
            console.error("Could not filter available realms!");
            return [];
        }
    }


    /* ------------------------------- */

    // Show the dialog to "add realm", that allows users to override realm options.
    protected _showAddingRealmDialog() {
        this._addedRealm = null;
        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                default: true,
                actionName: "ok",
                content: "ok",
                action: () => {
                    if (this._addedRealm) {
                        let realms = this.getRealmsProperty(this.config);
                        if (!realms) {
                            realms = {}
                        }
                        if(this.isManagerConfig(this.config)) {
                            realms[this._addedRealm] = {}; // empty object since no fields are required
                        } else if(this.isMapConfig(this.config)) {
                            realms[this._addedRealm] = {bounds: [4.42, 51.88, 4.55, 51.94], center: [4.485222, 51.911712], zoom: 14, minZoom: 14, maxZoom: 19, boxZoom: false}
                        }
                        this.requestUpdate("config");
                        this.notifyConfigChange(this.config);
                    }
                }
            },
        ];
        showDialog(new OrMwcDialog()
            .setHeading(i18next.t('configuration.addMapCustomization'))
            .setActions(dialogActions)
            .setContent(html`
                <or-mwc-input class="selector" label="Realm" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._addedRealm = e.detail.value}" .type="${InputType.SELECT}"
                              .options="${Object.entries(this.getAvailableRealms(this.config, this.realmOptions)).map(([, value]) => {
                return [value.name, value.displayName]
            })}"
                ></or-mwc-input>
            `)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        padding: 4px 8px;
                    }

                    #dialog-content {
                        flex: 1;
                        overflow: visible;
                        min-height: 0;
                        padding: 0;
                    }

                    or-mwc-input.selector {
                        width: 300px;
                        display: block;
                        padding: 10px 20px;
                    }
                </style>
            `)
            .setDismissAction(null));

    }
}
