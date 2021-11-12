import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {DefaultColor3, Util} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {EnhancedStore} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {ClientRole, Role, User} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse} from "@openremote/rest";
import {UiAssetTreeNode} from "@openremote/or-asset-tree";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageUsersProvider<S extends AppStateKeyed>(store: EnhancedStore<S>): PageProvider<S> {
    return {
        name: "users",
        routes: ["users"],
        pageCreator: () => {
            return new PageUsers(store);
        },
    };
}

interface UserModel extends User {
    password?: string;
    roles?: Role[];
}

@customElement("page-users")
class PageUsers<S extends AppStateKeyed> extends Page<S> {
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

    @property()
    public realm?: string;

    @state()
    protected _users: UserModel[] = [];
    @state()
    protected _serviceUsers: UserModel[] = [];
    @state()
    protected _roles: Role[] = [];
    @state()
    protected _compositeRoles: Role[] = [];

    get name(): string {
        return "user_plural";
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("realm")) {
            this.loadUsers();
        }

        return super.shouldUpdate(_changedProperties);
    }

    public connectedCallback() {
        super.connectedCallback();
        this.loadUsers();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
    }

    protected async loadUsers() {

        this._compositeRoles = [];
        this._roles = [];
        this._users = [];
        this._serviceUsers = [];

        if (!manager.authenticated || !manager.hasRole(ClientRole.READ_USERS)) {
            console.warn("Not authenticated or insufficient access");
            return;
        }

        const realm = this.getState().app.realm;
        const responseAndStateOK: <T extends GenericAxiosResponse<any[]>>(response: T, errorMsg: string) => boolean = (response, errorMsg) => {

            // After async op check that the response still matches current state and that the component is still loaded in the UI
            if (this.getState().app.realm !== realm || !this.isConnected) {
                // Realm change or component disconnected - don't continue
                return false;
            }

            if (!response.data) {
                showSnackbar(undefined, errorMsg, i18next.t("dismiss"));
                console.error(errorMsg + ": response = " + response.statusText);
                return false;
            }

            return true;
        };

        const roleResponse = await manager.rest.api.UserResource.getRoles(manager.displayRealm);

        if (!responseAndStateOK(roleResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const compositeRoles = roleResponse.data.filter(role => role.composite);
        const roles = roleResponse.data.filter(role => !role.composite);
        const usersResponse = await manager.rest.api.UserResource.getAll(manager.displayRealm);

        if (!responseAndStateOK(usersResponse, i18next.t("loadFailedUsers"))) {
            return;
        }

        const serviceUsersResponse = await manager.rest.api.UserResource.getAllService(manager.displayRealm);

        if (!responseAndStateOK(serviceUsersResponse, i18next.t("loadFailedServiceUsers"))) {
            return;
        }

        const users: UserModel[] = usersResponse.data;
        const serviceUsers: UserModel[] = serviceUsersResponse.data;

        // Load each users assigned roles
        const roleLoaders = [...users, ...serviceUsers].map(async user => {
            const userRolesResponse = await (user.serviceAccount ? manager.rest.api.UserResource.getUserClientRoles(manager.displayRealm, user.id, user.username) : manager.rest.api.UserResource.getUserRoles(manager.displayRealm, user.id));
            user.roles = userRolesResponse.data.filter(r => r.assigned);
        });
        
        await Promise.all(roleLoaders);

        // Only update state once all promises are fulfilled
        this._compositeRoles = compositeRoles.sort(Util.sortByString(role => role.name));
        this._roles = roles.sort(Util.sortByString(role => role.name));
        this._users = users.sort(Util.sortByString(u => u.username));
        this._serviceUsers = serviceUsers.sort(Util.sortByString(u => u.username));
    }

    private async _createUpdateUser(user: UserModel) {

        if (user.password === "") {
            // Means a validation failure shouldn't get here
            return;
        }

        try {
            const response = await manager.rest.api.UserResource.createUpdate(manager.displayRealm, user);

            if (user.password) {
                const id = response.data.id;
                const credentials = {value: user.password}
                manager.rest.api.UserResource.resetPassword(manager.displayRealm, id, credentials);
            }
            (response.data as UserModel).roles = user.roles;
            await this._updateRoles(response.data);
        } finally {
            this.loadUsers();
        }
    }

    /**
     * Backend only uses name of role not the ID so although service client roles are not the same as composite roles
     * the names will match so that's ok
     */
    private async _updateRoles(user: UserModel) {
        if (this._compositeRoles.length === 0 || !user.roles) {
            return;
        }

        const roles = user.roles.filter(role => role.assigned);

        if (!user.serviceAccount) {
            await manager.rest.api.UserResource.updateUserRoles(manager.displayRealm, user.id, roles);
        } else {
            await manager.rest.api.UserResource.updateUserClientRoles(manager.displayRealm, user.id, user.username, roles);
        }
    }
    
    private _deleteUser(user) {
        showOkCancelDialog(i18next.t("delete"), i18next.t("deleteUserConfirm"), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this.doDelete(user);
                }
            });
    }

    private doDelete(user) {
        manager.rest.api.UserResource.delete(manager.displayRealm, user.id).then(response => {
            if (user.serviceAccount) {
                this._serviceUsers = [...this._serviceUsers.filter(u => u.id !== user.id)];
            } else {
                this._users = [...this._users.filter(u => u.id !== user.id)];
            }
        })
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        if (!this._roles || this._roles.length === 0) {
            return html``;
        }

        const compositeRoleOptions: string[] = this._compositeRoles.map(cr => cr.name);
        const readonly = !manager.hasRole(ClientRole.WRITE_USER);

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="account-group"></or-icon>
                    ${i18next.t("user_plural")}
                </div>

                <div class="panel">
                    <p class="panel-title">${i18next.t("regularUser_plural")}</p>
                    <div id="table-users" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="attribute list">
                            <thead>
                            <tr class="mdc-data-table__header-row">
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="username"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="email"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="role"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="status"></or-translate>
                                </th>
                            </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                            ${this._users.map((user, index) => this._getUserTemplate(() => {
                                this._users.pop(); this._users = [...this._users];
                            }, user, readonly, compositeRoleOptions, "user"+index))}
                            ${(this._users.length === 0 || (this._users.length > 0 && !!this._users[this._users.length - 1].id)) && !readonly ? html`
                                <tr class="mdc-data-table__row" @click="${() => {
                                    this._users = [...this._users, {enabled: true}];
                                }}">
                                    <td colspan="100%">
                                        <a class="button"><or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("user")}</a>
                                    </td>
                                </tr>
                            ` : ``}
                            </tbody>
                        </table>
                    </div>
                </div>
                
                <div class="panel">
                    <p class="panel-title">${i18next.t("serviceUser_plural")}</p>
                    <div id="table-users" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="attribute list">
                            <thead>
                            <tr class="mdc-data-table__header-row">
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="username"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="email"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="role"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="status"></or-translate>
                                </th>
                            </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                            ${this._serviceUsers.map((user, index) => this._getUserTemplate(() => {
                                this._serviceUsers.pop(); this._serviceUsers = [...this._serviceUsers];
                            }, user, readonly, compositeRoleOptions, "serviceuser" + index))}
                            ${(this._serviceUsers.length === 0 || (this._serviceUsers.length > 0 && !!this._serviceUsers[this._serviceUsers.length - 1].id)) && !readonly ? html`
                                <tr class="mdc-data-table__row" @click="${() => {
                                    this._serviceUsers = [...this._serviceUsers, {
                                        enabled: true,
                                        serviceAccount: true}]
                                }}">
                                    <td colspan="100%">
                                        <a class="button"><or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("user")}</a>
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

    public stateChanged(state: S) {
        this.realm = state.app.realm;
    }

    protected _toggleUserExpand(ev: MouseEvent) {
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

    protected _openAssetSelector() {
        const hostElement = document.body;
        let selectedAssets: UiAssetTreeNode[] = [];

        const dialog = new OrMwcDialog();
        dialog.isOpen = true;
        dialog.dialogTitle = i18next.t("restrictAccess");
        dialog.dialogContent = html`
            <or-asset-tree id="chart-asset-tree" readonly .selectedIds="" .showSortBtn="${false}" .expandNodes="${true}" .checkboxes="${true}"
                @or-asset-tree-selection="${(e) => selectedAssets = e.detail.newNodes}"></or-asset-tree>
        `;
        dialog.dialogActions = [
            {
                default: true,
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                actionName: "ok",
                content: i18next.t("ok"),
                action: () => console.log(selectedAssets)
            }
        ];
        
        hostElement.append(dialog);
        return dialog;
    }

    protected _onPasswordChanged(user: UserModel, suffix: string) {

        const passwordComponent = this.shadowRoot.getElementById("password-" + suffix) as OrMwcInput;
        const repeatPasswordComponent = this.shadowRoot.getElementById("repeatPassword-" + suffix) as OrMwcInput;
        const saveBtn = this.shadowRoot.getElementById("savebtn-" + suffix) as OrMwcInput;

        if (repeatPasswordComponent.value !== passwordComponent.value) {
            const error = i18next.t("passwordMismatch");
            repeatPasswordComponent.setCustomValidity(error);
            saveBtn.disabled = true;
            user.password = "";
        } else {
            repeatPasswordComponent.setCustomValidity(undefined);
            user.password = passwordComponent.value;
            saveBtn.disabled = false;
        }
    }

    protected async _regenerateSecret(ev: OrInputChangedEvent, user: UserModel, secretInputId: string) {
        const btnElem = ev.currentTarget as OrMwcInput;
        const secretElem = this.shadowRoot.getElementById(secretInputId) as OrMwcInput;
        if (!btnElem || !secretElem) {
            return;
        }
        btnElem.disabled = true;
        secretElem.disabled = true;
        const resetResponse = await manager.rest.api.UserResource.resetSecret(manager.displayRealm, user.id);
        if (resetResponse.data) {
            secretElem.value = resetResponse.data;
        }
        btnElem.disabled = false;
        secretElem.disabled = false;
    }

    protected _updateUserSelectedRoles(user: UserModel, suffix: string) {
        const roleCheckboxes = [...((this.shadowRoot!.getElementById("role-list-" + suffix) as HTMLDivElement).children as any)] as OrMwcInput[];
        const implicitRoleNames = this.getImplicitUserRoles(user);
        roleCheckboxes.forEach((checkbox) => {
            const roleName = checkbox.label;
            const r = this._roles.find(role => roleName === role.name);
            checkbox.disabled = !!implicitRoleNames.find(name => r.name === name);
            checkbox.value = !!user.roles.find(userRole => userRole.name === r.name) || implicitRoleNames.some(implicitRoleName => implicitRoleName === r.name);
        });
    }

    protected getImplicitUserRoles(user: UserModel) {
        return this._compositeRoles.filter((role) => user.roles.some(ur => ur.name === role.name)).flatMap((role) => role.compositeRoleIds).map(id => this._roles.find(r => r.id === id).name);
    }

    protected _getUserTemplate(addCancel: () => void, user: UserModel, readonly: boolean, compositeRoleOptions: string[], suffix: string): TemplateResult {
        const isServiceUser = user.serviceAccount;
        const isSameUser = user.username === manager.username;
        user.roles = user.roles || [];
        const implicitRoleNames = this.getImplicitUserRoles(user);

        return html`
            <tr class="mdc-data-table__row" @click="${(ev) => this._toggleUserExpand(ev)}">
                <td class="padded-cell mdc-data-table__cell">
                    <or-icon icon="chevron-right"></or-icon>
                    <span>${user.username}</span>
                </td>
                <td class="padded-cell mdc-data-table__cell  hide-mobile">
                    ${user.email}
                </td>
                <td class="padded-cell mdc-data-table__cell">
                    ${user.roles ? user.roles.filter(r => r.composite).map(r => r.name).join(",") : null}
                </td>
                <td class="padded-cell mdc-data-table__cell hide-mobile">
                    <or-translate .value="${user.enabled ? "enabled" : "disabled"}"></or-translate>
                </td>
            </tr>
            <tr class="item-row${!user.id ? " expanded" : ""}">
                <td colspan="4">
                    <div class="item-row-content">
                        <div class="row">
                            <div class="column">
                                <h5>${i18next.t("details")}</h5>
                                <!-- user details -->
                                <or-mwc-input ?readonly="${!!user.id || readonly}"
                                              .label="${i18next.t("username")}"
                                              .type="${InputType.TEXT}" min="1" required
                                              .value="${user.username}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => user.username = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?readonly="${readonly}"
                                              class="${isServiceUser ? "hidden" : ""}"
                                              .label="${i18next.t("email")}"
                                              .type="${InputType.EMAIL}" min="1"
                                              .value="${user.email}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => user.email = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?readonly="${readonly}"
                                              class="${isServiceUser ? "hidden" : ""}"
                                              .label="${i18next.t("firstName")}"
                                              .type="${InputType.TEXT}" min="1"
                                              .value="${user.firstName}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => user.firstName = e.detail.value}"></or-mwc-input>
                                <or-mwc-input ?readonly="${readonly}"
                                              class="${isServiceUser ? "hidden" : ""}"
                                              .label="${i18next.t("surname")}"
                                              .type="${InputType.TEXT}" min="1"
                                              .value="${user.lastName}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => user.lastName = e.detail.value}"></or-mwc-input>

                                <!-- password -->
                                <h5>${i18next.t("password")}</h5>
                                ${user.serviceAccount ? html`
                                    <or-mwc-input id="password-${suffix}" readonly
                                                  .label="${i18next.t("secret")}"
                                                  .value="${user.secret}"
                                                  .type="${InputType.TEXT}"></or-mwc-input>
                                    <or-mwc-input ?readonly="${!user.id || readonly}"
                                                  .label="${i18next.t("regenerateSecret")}"
                                                  .type="${InputType.BUTTON}"
                                                  @or-mwc-input-changed="${(ev) => this._regenerateSecret(ev, user, "password-"+suffix)}"></or-mwc-input>`
                                        : html`
                                    <or-mwc-input id="password-${suffix}"
                                              ?readonly="${readonly}"
                                              .label="${i18next.t("password")}"
                                              .type="${InputType.PASSWORD}" min="1"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {this._onPasswordChanged(user, suffix)}}"></or-mwc-input>
                                    <or-mwc-input id="repeatPassword-${suffix}"
                                              helperPersistent ?readonly="${readonly}"
                                              .label="${i18next.t("repeatPassword")}"
                                              .type="${InputType.PASSWORD}" min="1"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {this._onPasswordChanged(user, suffix)}}"></or-mwc-input>
                                `}
                            </div>

                            <div class="column">
                                <h5>${i18next.t("settings")}</h5>
                                <!-- enabled -->
                                <or-mwc-input ?readonly="${readonly}"
                                              .label="${i18next.t("active")}"
                                              .type="${InputType.CHECKBOX}"
                                              .value="${user.enabled}"
                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => user.enabled = e.detail.value}"
                                              style="height: 56px;"></or-mwc-input>

                                <!-- is admin -->
                                <!--<or-mwc-input ?readonly="${true}"
                                              .label="${i18next.t("fullAccessLabel")}"
                                              .type="${InputType.CHECKBOX}"
                                              .value="${user.enabled}"
                                              style="height: 56px;"></or-mwc-input>-->
                                
                                <!-- composite roles -->
                                <or-mwc-input
                                    ?readonly="${readonly}"
                                    class="${isServiceUser ? "hidden" : ""}"
                                    ?disabled="${isSameUser}"
                                    .value="${user.roles && user.roles.length > 0 ? user.roles.filter(r => r.composite).map(r => r.name) : undefined}"
                                    .type="${InputType.SELECT}" multiple
                                    .options="${compositeRoleOptions}" 
                                    .label="${i18next.t("role_plural")}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        const roleNames = e.detail.value as string[];
                                        user.roles = this._compositeRoles.filter(cr => roleNames.some(name => cr.name === name)).map(r => {return {...r, assigned: true};});
                                        this._updateUserSelectedRoles(user, suffix);
                                    }}"></or-mwc-input>

                                <!-- roles -->
                                <div style="display:flex;flex-wrap:wrap;margin-bottom: 20px;" id="role-list-${suffix}">
                                    ${this._roles.map(r => {
                                        return html`
                                            <or-mwc-input 
                                                ?readonly="${readonly}"
                                                ?disabled="${implicitRoleNames.find(name => r.name === name)}"
                                                .value="${!!user.roles.find(userRole => userRole.name === r.name) || implicitRoleNames.some(implicitRoleName => implicitRoleName === r.name)}"
                                                .type="${InputType.CHECKBOX}"
                                                .label="${r.name}"
                                                style="width:25%;margin:0"
                                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                                    if (!!e.detail.value) {
                                                        user.roles.push({...r, assigned: true});
                                                    } else {
                                                        user.roles = user.roles.filter(e => e.name !== r.name);
                                                    }
                                                }}"></or-mwc-input>
                                        `
                                    })}
                                </div>

                                <!-- restricted access -->
                                <or-mwc-input outlined .type="${InputType.BUTTON}" .label="${i18next.t("restrictAccess")}"
                                              @click="${() => this._openAssetSelector()}"></or-mwc-input>

                                <!-- placeholder -->
                            </div>
                        </div>

                        ${readonly ? `` : html`
                            <div class="row" style="margin-bottom: 0;">

                                ${!isSameUser && user.id ? html`
                                    <or-mwc-input .label="${i18next.t("delete")}"
                                                  .type="${InputType.BUTTON}"
                                                  @click="${() => this._deleteUser(user)}"></or-mwc-input>
                                ` : ``}
                                ${!user.id ? html`<or-mwc-input .label="${i18next.t("cancel")}"
                                          .type="${InputType.BUTTON}"
                                          @click="${() => addCancel()}"></or-mwc-input>
                                ` : ``}
                                <or-mwc-input id="savebtn-${suffix}" style="margin-left: auto;"
                                      .label="${i18next.t(user.id ? "save" : "create")}"
                                      .type="${InputType.BUTTON}"
                                      @click="${() => this._createUpdateUser(user)}"></or-mwc-input>
                            </div>
                        `}
                    </div>
                </td>
            </tr>
        `
    }
}
