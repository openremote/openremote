import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import manager, {DefaultColor3} from "@openremote/core";
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
                    padding: 24px;
                }

                .panel-title {
                    text-transform: uppercase;
                    font-weight: bolder;
                    line-height: 1em;
                    color: var(--internal-or-asset-viewer-title-text-color);
                    margin-bottom: 20px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
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
                
                or-mwc-input {
                    margin-bottom: 20px;
                    margin-right: 16px;
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
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                    max-width: 50%;
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
                    padding-left: 16px;
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
    protected _roleOptions?: [string, string][];
    @state()
    protected _realmOptions?: string[];

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
                            ${this._configs.map((config, index) => this._getConfigTemplate(() => {
                                this._configs.pop(); this._configs = [...this._configs];
                            }, config))}
                            ${(this._configs.length === 0 || (this._configs.length > 0 && !!this._configs[this._configs.length - 1].id)) ? html`
                                <tr class="mdc-data-table__row" @click="${() => {
                                        this._configs = [...this._configs, {type: "x509"}];
                                    }}">
                                    <td colspan="100%">
                                        <a class="button"><or-icon icon="plus"></or-icon>${i18next.t("add")}</a>
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

        const rolesResponse = await manager.rest.api.UserResource.getRoles(manager.displayRealm);

        if (!responseAndStateOK(rolesResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const provisioningResponse = await manager.rest.api.ProvisioningResource.getProvisioningConfigs();

        if (!responseAndStateOK(provisioningResponse, i18next.t("loadFailedProvisioningConfigs"))) {
            return;
        }

        this._realmOptions = realmResponse.data.map(r => r.name);
        this._roleOptions = rolesResponse.data.filter(role => !role.composite).map(r => [r.name, r.name] as [string, string]).sort();
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
                <or-mwc-input .label="${i18next.t("CACertPem")}" required
                              .type="${InputType.TEXTAREA}"
                              .value="${data.CACertPEM}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => data.CACertPEM = e.detail.value}"></or-mwc-input>
                <or-mwc-input .label="${i18next.t("ignoreExpiryDate")}"
                              .type="${InputType.CHECKBOX}"
                              .value="${data.ignoreExpiryDate}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => data.ignoreExpiryDate = !e.detail.value}"
                              style="height: 56px;"></or-mwc-input>
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
                                <or-mwc-input .label="${i18next.t("name")}"
                                              .type="${InputType.TEXT}" min="1" required
                                              .value="${config.name}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.name = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?disabled="${!!config.id}" required
                                              .label="${i18next.t("type")}"
                                              .type="${InputType.SELECT}"
                                              .options="${[["x509", "X.509"]]}"
                                              .value="${!config.type ? "x509" : config.type}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.type = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?disabled="${!!config.id}" required
                                              .label="${i18next.t("realm")}"
                                              .type="${InputType.SELECT}"
                                              .options="${this._realmOptions}"
                                              .value="${config.realm}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.realm = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?disabled="${!!config.id}"
                                              .value="${config.userRoles}"
                                              .type="${InputType.SELECT}" multiple
                                              .options="${this._roleOptions}" 
                                              .label="${i18next.t("role_plural")}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.userRoles = e.detail.value as ClientRole[]}"></or-mwc-input>
                                <or-mwc-input .label="${i18next.t("assetTemplate")}"
                                              .type="${InputType.JSON}"
                                              .value="${config.assetTemplate ? JSON.stringify(JSON.parse(config.assetTemplate), null, 2) : undefined}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.assetTemplate = e.detail.value ? JSON.stringify(e.detail.value) : undefined}"></or-mwc-input>
                                <or-mwc-input .label="${i18next.t("createAsRestrictedUser")}"
                                              .type="${InputType.CHECKBOX}"
                                              .value="${config.restrictedUser}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.restrictedUser = e.detail.value}"></or-mwc-input>
                                <or-mwc-input .label="${i18next.t("disabled")}"
                                              .type="${InputType.CHECKBOX}"
                                              .value="${config.disabled}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => config.disabled = e.detail.value}"></or-mwc-input>
                            </div>

                            <div class="column">                                
                                ${typeColumnContents}
                            </div>
                        </div>

                        <div class="row" style="margin-bottom: 0;">
                            ${config.id ? html`<or-mwc-input label="delete"
                                          .type="${InputType.BUTTON}"
                                          @click="${() => this._deleteConfig(config)}"></or-mwc-input>
                            ` : ``}              
                            ${!config.id ? html`<or-mwc-input label="cancel"
                                      .type="${InputType.BUTTON}"
                                      @click="${() => addCancel()}"></or-mwc-input>
                            ` : ``}
                            <or-mwc-input class="savebtn" style="margin-left: auto;"
                                  label="${config.id ? "save" : "create"}"
                                  .type="${InputType.BUTTON}"
                                  @click="${() => this._createUpdateConfig(config)}"></or-mwc-input>
                        </div>
                    </div>
                </td>
            </tr>
        `
    }
}
