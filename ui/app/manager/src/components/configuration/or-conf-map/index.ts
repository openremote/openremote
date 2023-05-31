import {css, html, LitElement} from "lit";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-map-card";
import {customElement, property, state} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';
import manager from "@openremote/core";
import {MapConfig} from "@openremote/model";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";


@customElement("or-conf-map")
export class OrConfMap extends LitElement {


    static styles = css`
      #btn-add-realm {
        margin-top: 4px;
      }
    `;

    @property({attribute: false})
    public config: MapConfig = {};

    @state()
    protected _availableRealms: { name: string, displayName: string, canDelete: boolean }[] = [];

    @state()
    protected _allRealms: { name: string, displayName: string, canDelete: boolean }[] = [];

    protected _addedRealm: null | string = null

    protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
        manager.rest.api.RealmResource.getAccessible().then((response) => {
            this._allRealms = response.data.map((r) => ({name: r.name, displayName: r.displayName, canDelete: true}));
            this._allRealms.push({name: 'default', displayName: 'Default', canDelete: false})
            this._loadListOfAvailableRealms()
        });
    }

    protected _removeRealm(realm: string) {
        const {config, _loadListOfAvailableRealms, requestUpdate} = this;
        if (config) {
            delete config[realm]
            _loadListOfAvailableRealms()
            requestUpdate()
        }
    }

    protected _loadListOfAvailableRealms() {
        const {config: mapConfig} = this
        this._availableRealms = this._allRealms.filter(function (realm) {
            if (!!realm.name && !!mapConfig) {
                if (realm.name in mapConfig) {
                    return null
                }
            }
            return realm
        }).sort(function (a, b) {
            if (a.displayName && b.displayName) {
                return (a.displayName > b.displayName) ? 1 : -1
            }
            return -1
        })
    }

    protected _showAddingRealmDialog() {
        this._addedRealm = null;
        const _AddRealmToView = () => {
            if (this._addedRealm) {
                if (!this.config) {
                    this.config = {}
                }
                this.config[this._addedRealm] = {bounds: [4.42, 51.88, 4.55, 51.94], center: [4.485222, 51.911712], zoom: 14, minZoom: 14, maxZoom: 19, boxZoom: false}
                this._loadListOfAvailableRealms()
                this.requestUpdate()
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

    render() {
        const {_addedRealm, _removeRealm} = this;
        return html`
            <div class="panels">
                ${Object.entries(this.config === undefined ? {} : this.config).map(([key, value]) => {
                    const realmOption = this._allRealms.find((r) => r.name === key);
                    return html`
                        <or-conf-map-card .expanded="${_addedRealm === key}" .name="${key}" .map="${value}" .canRemove="${realmOption?.canDelete}"
                                          .onRemove="${() => { _removeRealm(key); }}"
                        ></or-conf-map-card>`
                })}
            </div>

            <div style="display: flex; justify-content: space-between;">
                ${when(this._availableRealms.length > 0, () => html`
                    <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" .label="${i18next.t('configuration.addMapCustomization')}" icon="plus"
                                  @click="${() => this._showAddingRealmDialog()}"
                    ></or-mwc-input>
                `)}
            </div>
        `
    }
}
