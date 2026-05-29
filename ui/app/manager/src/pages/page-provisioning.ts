import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import manager, {DefaultColor3, OPENREMOTE_CLIENT_ID} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {ClientRole, ProvisioningConfig, X509ProvisioningData} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse} from "@openremote/rest";
import {OrVaadinSelect} from "@openremote/or-vaadin-components/or-vaadin-select";
import {OrVaadinMultiSelectComboBox} from "@openremote/or-vaadin-components/or-vaadin-multi-select-combo-box";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageProvisioningProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "provisioning",
        routes: ["provisioning"],
        pageCreator: () => {
            return new PageProvisioning(store);
        },
    };
}

@customElement("page-provisioning")
export class PageProvisioning extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return [
            unsafeCSS(tableStyle),
            css`
                #wrapper {
                    height: 100%;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    overflow: auto;
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
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                #title or-icon {
                    margin-right: 10px;
                    margin-left: 14px;
                }

                .panel {
                    width: calc(100% - 90px);
                    max-width: 1310px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: 5px auto;
                    padding: 12px 24px 24px;
                }

                .panel-title {
                    display: flex;
                    align-items: center;
                    text-transform: uppercase;
                    font-weight: bolder;
                    line-height: 1em;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    margin-bottom: 10px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    min-height: 36px;
                }

                .panel-title p {
                    margin: 0;
                }

                #table-users,
                #table-users table {
                    width: 100%;
                    white-space: nowrap;
                }

                .mdc-data-table__row {
                    cursor: pointer;
                    border-top-color: #D3D3D3;
                }

                td, th {
                    width: 25%
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                    margin-right: 2px;
                    margin-left: -5px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    margin: 10px 0;
                    flex: 1 1 0;
                    gap: 16px;
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                    max-width: 50%;
                    gap: 16px;
                }

                .mdc-data-table__header-cell {
                    font-weight: bold;
                    color: ${unsafeCSS(DefaultColor3)};
                }

                .mdc-data-table__header-cell:first-child {
                    padding-left: 36px;
                }

                .item-row td {
                    padding: 0;
                }

                .item-row-content {
                    flex-direction: row;
                    overflow: hidden;
                    max-height: 0;
                    padding: 0 16px;
                }

                .item-row.expanded .item-row-content {
                    overflow: visible;
                    max-height: unset;
                }

                .button {
                    cursor: pointer;
                    display: flex;
                    flex-direction: row;
                    align-content: center;
                    padding: 16px;
                    align-items: center;
                    font-size: 14px;
                    text-transform: uppercase;
                    color: var(--or-app-color4);
                }

                .hidden {
                    display: none;
                }
                
                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }

                    .hide-mobile {
                        display: none;
                    }

                    .row {
                        display: block;
                        flex-direction: column;
                    }

                    .panel {
                        border-radius: 0;
                        border-left: 0px;
                        border-right: 0px;
                        width: calc(100% - 48px);
                    }

                    td, th {
                        width: 50%
                    }
                }
            `,
        ];
    }

    @state()
    protected _configs?: ProvisioningConfig<any, any>[];
    @state()
    protected _roleOptions?: {value: any, label: string}[];
    @state()
    protected _realmOptions?: {value: any, label: string}[];
    @state()
    protected _configFilter: (configs: ProvisioningConfig<any, any>[]) => ProvisioningConfig<any, any>[] = (configs) => configs;

    get name(): string {
        return "autoProvisioning";
    }

    public connectedCallback() {
        super.connectedCallback();
        this._loadConfigs();
    }

    stateChanged(state: AppStateKeyed): void {
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        if (!this._configs) {
            return html``;
        }

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="cellphone-cog"></or-icon>
                    ${i18next.t("provisioningConfigs")}
                </div>

                <div class="panel">
                    <div class="panel-title" style="justify-content: space-between;">
                        <p><or-translate value="provisioningConfigs"></or-translate></p>
                        <or-vaadin-text-field placeholder=${i18next.t("search")} style="width: 240px;"
                                              @input=${(ev: InputEvent) => this.onConfigSearch(ev)}>
                            <or-icon slot="suffix" icon="magnify"></or-icon>
                        </or-vaadin-text-field>
                    </div>
                    <div id="table-users" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="attribute list">
                            <thead>
                            <tr class="mdc-data-table__header-row">
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="name"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="realm"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="type"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="enabled"></or-translate>
                                </th>
                            </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                            ${this._configFilter(this._configs).map((config, index) => this._getConfigTemplate(() => {
                                this._configs.pop(); this._configs = [...this._configs];
                            }, config))}
                            ${(this._configs.length === 0 || (this._configs.length > 0 && !!this._configs[this._configs.length - 1].id)) ? html`
                                <tr class="mdc-data-table__row" @click="${() => {
                                        this._configs = [...this._configs, {type: "x509"}];
                                    }}">
                                    <td colspan="100%">
                                        <a class="button"><or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("provisioningConfig")}</a>
                                    </td>
                                </tr>
                            ` : ``}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    protected async _loadConfigs() {

        this._configs = undefined;
        this._roleOptions = undefined;
        this._realmOptions = undefined;

        const responseAndStateOK: <T extends GenericAxiosResponse<any[]>>(response: T, errorMsg: string) => boolean = (response, errorMsg) => {

            // After async op check that the response still matches current state and that the component is still loaded in the UI
            if (!this.isConnected) {
                // Component disconnected - don't continue
                return false;
            }

            if (!response.data) {
                showSnackbar(undefined, errorMsg, "dismiss");
                console.error(errorMsg + ": response = " + response.statusText);
                return false;
            }

            return true;
        };

        const realmResponse = await manager.rest.api.RealmResource.getAll();

        if (!responseAndStateOK(realmResponse, i18next.t("loadFailedRealms"))) {
            return;
        }

        const rolesResponse = await manager.rest.api.UserResource.getClientRoles(manager.displayRealm, OPENREMOTE_CLIENT_ID);

        if (!responseAndStateOK(rolesResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const provisioningResponse = await manager.rest.api.ProvisioningResource.getProvisioningConfigs();

        if (!responseAndStateOK(provisioningResponse, i18next.t("loadFailedProvisioningConfigs"))) {
            return;
        }

        this._realmOptions = realmResponse.data.map(r => ({value: r.name, label: r.displayName}));
        this._roleOptions = rolesResponse.data.filter(role => !role.composite).map(r => ({value: r.name, label: r.name})).sort();
        this._configs = provisioningResponse.data;
    }

    protected async _createUpdateConfig(config: ProvisioningConfig<any, any>) {
        if (config.id) {
            const response = await manager.rest.api.ProvisioningResource.updateProvisioningConfig(config.id, config);
        } else {
            const response = await manager.rest.api.ProvisioningResource.createProvisioningConfig(config);
            config.id = response.data;
            this._configs = [...this._configs];
        }
    }

    protected _deleteConfig(config: ProvisioningConfig<any, any>) {
        showOkCancelDialog(i18next.t("delete"), i18next.t("deleteProvisioningConfigConfirm"), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this._doDelete(config);
                }
            });
    }

    protected _doDelete(config: ProvisioningConfig<any, any>) {
        manager.rest.api.ProvisioningResource.deleteProvisioningConfig(config.id).then(response => {
            this._configs = [...this._configs.filter(c => c.id !== config.id)];
        });
    }

    protected _toggleConfigExpand(ev: MouseEvent) {
        const trElem = ev.currentTarget as HTMLTableRowElement;
        const expanderIcon = trElem.getElementsByTagName("or-icon")[0] as OrIcon;
        const userRow = (trElem.parentElement! as HTMLTableElement).rows[trElem.rowIndex];

        if (expanderIcon.icon === "chevron-right") {
            expanderIcon.icon = "chevron-down";
            userRow.classList.add("expanded");
        } else {
            expanderIcon.icon = "chevron-right";
            userRow.classList.remove("expanded");
        }
    }

    protected _getConfigTemplate(addCancel: () => void, config: ProvisioningConfig<any, any>): TemplateResult {

        let typeColumnContents: TemplateResult;

        if (config.type === "x509") {
            if (!config.data) {
                config.data = {};
            }
            const data = config.data as X509ProvisioningData;

            typeColumnContents = html`
                <or-vaadin-text-area required value=${data.CACertPEM}
                                     @change=${(ev: Event) => { data.CACertPEM = (ev.currentTarget as HTMLInputElement).value; this.requestUpdate(); }}>
                    <or-translate slot="label" value="CACertPem"></or-translate>
                </or-vaadin-text-area>
                <or-vaadin-checkbox ?checked=${data.ignoreExpiryDate}
                                    @change=${(ev: Event) => data.ignoreExpiryDate = !(ev.currentTarget as HTMLInputElement).checked}>
                    <or-translate slot="label" value="ignoreExpiryDate"></or-translate>
                </or-vaadin-checkbox>
            `;
        } else {
            typeColumnContents = html`
                <or-translate .value="notSupported"></or-translate>
            `;
        }

        return html`
            <tr class="mdc-data-table__row" @click="${(ev) => this._toggleConfigExpand(ev)}">
                <td class="padded-cell mdc-data-table__cell">
                    <or-icon icon="chevron-right"></or-icon>
                    <span>${!config.id ? "" : config.name}</span>
                </td>
                <td class="padded-cell mdc-data-table__cell">
                    ${!config.id ? "" : config.realm}
                </td>
                <td class="padded-cell mdc-data-table__cell">
                    ${!config.id ? "" : config.type}
                </td>
                <td class="padded-cell mdc-data-table__cell">
                    <or-input .type="${InputType.CHECKBOX}" .value="${!config.id ? "" : !config.disabled}"></or-input>
                </td>
            </tr>
            <tr class="item-row${!config.id ? " expanded" : ""}">
                <td colspan="4">
                    <div class="item-row-content">
                        <div class="row">
                            <div class="column">
                                <or-vaadin-text-field required minlength="1" maxlength="255" value=${config.name}
                                                      @change=${(ev: Event) => { config.name = (ev.currentTarget as HTMLInputElement).value; this.requestUpdate(); }}>
                                    <or-translate slot="label" value="name"></or-translate>
                                </or-vaadin-text-field>
                                <or-vaadin-select required readonly
                                                  .items=${[{value: "x509", label: "X.509"}]}
                                                  value=${!config.type ? "x509" : config.type}>
                                    <or-translate slot="label" value="type"></or-translate>
                                </or-vaadin-select>
                                <or-vaadin-select required ?readonly=${!!config.id}
                                                  .items=${this._realmOptions} value=${config.realm}
                                                  @change=${(ev: Event) => { config.realm = (ev.currentTarget as OrVaadinSelect).value; this.requestUpdate(); }}>
                                    <or-translate slot="label" value="realm"></or-translate>
                                </or-vaadin-select>
                                <or-vaadin-multi-select-combo-box ?readonly=${!!config.id}
                                                                  .items=${this._roleOptions}
                                                                  .selectedItems=${this._roleOptions?.filter(r => config.userRoles?.includes(r.value))}
                                                                  @change=${(ev: Event) => { config.userRoles = (ev.currentTarget as OrVaadinMultiSelectComboBox).selectedItems.map(i => i.value as ClientRole)}}>
                                    <or-translate slot="label" value="role_plural"></or-translate>
                                </or-vaadin-multi-select-combo-box>
                                <or-mwc-input label="${i18next.t("assetTemplate")}"
                                              .type="${InputType.JSON}"
                                              .value="${config.assetTemplate ? JSON.stringify(JSON.parse(config.assetTemplate), null, 2) : undefined}"
                                              resizeVertical
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.assetTemplate = e.detail.value ? JSON.stringify(e.detail.value) : undefined}"></or-mwc-input>
                                <or-vaadin-checkbox ?checked=${config.restrictedUser}
                                                    @change=${(ev: Event) => config.restrictedUser = (ev.currentTarget as HTMLInputElement).checked}>
                                    <or-translate slot="label" value="createAsRestrictedUser"></or-translate>
                                </or-vaadin-checkbox>
                                <or-vaadin-checkbox ?checked=${config.disabled}
                                                    @change=${(ev: Event) => config.disabled = (ev.currentTarget as HTMLInputElement).checked}>
                                    <or-translate slot="label" value="disabled"></or-translate>
                                </or-vaadin-checkbox>
                            </div>

                            <div class="column">                                
                                ${typeColumnContents}
                            </div>
                        </div>

                        <div class="row" style="justify-content: space-between; margin: 16px 0;">
                            ${config.id ? html`
                                <or-vaadin-button @click=${() => this._deleteConfig(config)}>
                                    <or-translate value="delete"></or-translate>
                                </or-vaadin-button>
                            ` : ``}              
                            ${!config.id ? html`
                                <or-vaadin-button @click=${() => addCancel()}>
                                    <or-translate value="cancel"></or-translate>
                                </or-vaadin-button>
                            ` : ``}
                            <or-vaadin-button class="savebtn" theme="primary"
                                              ?disabled=${!config.id && (!config.name || !config.realm || !config.type || !(config.data as X509ProvisioningData)?.CACertPEM)}
                                              @click=${() => this._createUpdateConfig(config)}>
                                <or-translate value=${config.id ? "save" : "create"}></or-translate>
                            </or-vaadin-button>
                        </div>
                    </div>
                </td>
            </tr>
        `
    }

    protected onConfigSearch(ev: InputEvent) {
        const value = (ev.target as HTMLInputElement).value?.toLowerCase();
        if (!value) {
            this._configFilter = (configs) => configs;
        } else {
            this._configFilter = (configs) => configs.filter(c =>
                (c.name as string)?.toLowerCase().includes(value) ||
                (c.realm as string)?.toLowerCase().includes(value) ||
                (c.type as string)?.toLowerCase().includes(value)
            );
        }
    }
}
