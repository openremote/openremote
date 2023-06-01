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
import {css, html, LitElement} from "lit";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-realm-card";
import {customElement, property} from "lit/decorators.js";
import {map} from 'lit/directives/map.js';
import manager from "@openremote/core";
import {ManagerAppConfig, Realm} from "@openremote/model";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";

@customElement("or-conf-realm")
export class OrConfRealm extends LitElement {

    static styles = css`
      #btn-add-realm {
        margin-top: 4px;
      }
    `;

    @property({attribute: false})
    public config: ManagerAppConfig = {};

    @property()
    public allRealms: { name: string, displayName: string, canDelete: boolean }[] = [];

    protected _addedRealm: null | string = null


    /* ---------------- */


    protected willUpdate(changedProps: Map<string, never>) {
        console.log(changedProps); // TODO: Temporary use for testing purposes

        if (this.allRealms.length === 0) {
            this.fetchAccessibleRealms().then((realms) => {
                const allRealms = realms.map((r) => ({name: r.name, displayName: r.displayName, canDelete: true}));
                allRealms.push({name: 'default', displayName: 'Default', canDelete: false});
                this.allRealms = allRealms;
            })
        }
    }

    protected async fetchAccessibleRealms(): Promise<Realm[]> {
        return (await manager.rest.api.RealmResource.getAccessible()).data;
    }

    protected getAvailableRealms(config?: ManagerAppConfig, realms?: { name: string, displayName: string, canDelete: boolean }[]): { name: string, displayName: string, canDelete: boolean }[] {
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

    protected _removeRealm(realm: string) {
        if (this.config.realms) {
            delete this.config?.realms[realm];
            this.requestUpdate();
        }
    }

    render() {
        console.log(Object.entries(this.config?.realms));
        return html`
            <div class="panels">
                ${map(Object.entries(this.config?.realms), ([key, value]) => {
                    const r = this.allRealms.find((r) => r.name === key);
                    return html`
                        <or-conf-realm-card .expanded="${this._addedRealm === key}" .name="${key}" .realm="${value}" .canRemove="${r?.canDelete}"
                                            @remove="${() => this._removeRealm(key)}"
                        ></or-conf-realm-card>
                    `;
                })}
            </div>

            <div style="display: flex; justify-content: space-between;">
                <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" .label="${i18next.t('configuration.addRealmCustomization')}" icon="plus"
                              @click="${() => this._showAddingRealmDialog()}"
                ></or-mwc-input>
            </div>
        `
    }

    protected _showAddingRealmDialog() {
        this._addedRealm = null;
        const _AddRealmToView = () => {
            if (this._addedRealm) {
                if (!this.config.realms) {
                    this.config.realms = {}
                }
                this.config.realms[this._addedRealm] = {
                    styles: ":host > * {--or-app-color1:#FFFFFF;--or-app-color2:#F9F9F9;--or-app-color3:#4c4c4c;--or-app-color4:#4d9d2a;--or-app-color5:#CCCCCC;--or-app-color6:#be0000;"
                }
                return true
            }
            return false
        }
        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                default: true,
                actionName: "ok",
                content: i18next.t("ok"),
                action: _AddRealmToView
            },

        ];
        const availableRealms = this.getAvailableRealms(this.config, this.allRealms);
        const realmOptions = Object.entries(availableRealms).map(([, value]) => [value.name, value.displayName]);
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t('configuration.addRealmCustomization'))
            .setActions(dialogActions)
            .setContent(html`
                <or-mwc-input class="selector" label="Realm" .type="${InputType.SELECT}" .options="${realmOptions}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._addedRealm = e.detail.value}"
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
            .setDismissAction(null)
        );
    }
}
