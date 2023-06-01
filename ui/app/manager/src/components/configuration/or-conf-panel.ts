import {css, html, LitElement, TemplateResult} from "lit";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-map-card";
import {customElement, property, state} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';
import manager from "@openremote/core";
import {ManagerAppConfig, MapConfig} from "@openremote/model";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";

@customElement("or-conf-panel")
export class OrConfPanel extends LitElement {

    @property({attribute: false})
    public config: MapConfig | ManagerAppConfig = {};

    @state()
    protected _allRealms: { name: string, displayName: string, canDelete: boolean }[] = [];

    protected _addedRealm: null | string = null

    render(): TemplateResult {
        const availableRealms = this.getAvailableRealms(this.config, this._allRealms);
        return html`
            <div class="panels">
                ${Object.entries(this.config === undefined ? {} : this.config).map(([key, value]) => {
                    const realmOption = this._allRealms.find((r) => r.name === key);
                    return html`
                        <or-conf-map-card .expanded="${this._addedRealm === key}" .name="${key}" .map="${value}" .canRemove="${realmOption?.canDelete}"
                                          @remove="${() => this._removeRealm(key)}"
                        ></or-conf-map-card>`
                })}
            </div>

            <div style="display: flex; justify-content: space-between;">
                ${when(availableRealms.length > 0, () => html`
                    <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" .label="${i18next.t('configuration.addMapCustomization')}" icon="plus"
                                  @click="${() => this._showAddingRealmDialog()}"
                    ></or-mwc-input>
                `)}
            </div>
        `
    }

    protected _removeRealm(realm: string) {
        if (this.config) {
            delete this.config[realm];
            this.requestUpdate()
        } else {
            console.error("No config found when attempting to remove realm.")
        }
    }

    protected getAvailableRealms(config?: ManagerAppConfig | MapConfig, realms?: { name: string, displayName: string, canDelete: boolean }[]): { name: string, displayName: string, canDelete: boolean }[] {
        return realms.filter((r) => {
            if (r.name in config) {
                return null;
            }
            return r;
        }).sort((a, b) => {
            if (a.displayName && b.displayName) {
                return (a.displayName > b.displayName) ? 1 : -1;
            }
            return -1;
        })
    }

    /* ------------------------------- */

    protected _showAddingRealmDialog() {
        this._addedRealm = null;
        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                default: true,
                actionName: "ok",
                content: i18next.t("ok"),
                action: () => {
                    if (this._addedRealm) {
                        if (!this.config) {
                            this.config = {}
                        }
                        this.config[this._addedRealm] = {bounds: [4.42, 51.88, 4.55, 51.94], center: [4.485222, 51.911712], zoom: 14, minZoom: 14, maxZoom: 19, boxZoom: false}
                        this.requestUpdate();
                    }
                }
            },
        ];
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t('configuration.addMapCustomization'))
            .setActions(dialogActions)
            .setContent(html`
                <or-mwc-input class="selector" label="Realm" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._addedRealm = e.detail.value}" .type="${InputType.SELECT}"
                              .options="${Object.entries(this._availableRealms).map(([key, value]) => {
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
