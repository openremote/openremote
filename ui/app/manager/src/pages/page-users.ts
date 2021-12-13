import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {DefaultColor3, Util} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {EnhancedStore} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {ClientRole, RealmRole, Role, User, UserAssetLink, UserQuery} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {AxiosError, isAxiosError, GenericAxiosResponse} from "@openremote/rest";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";

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
    loaded?: boolean;
    loading?: boolean;
    previousRoles?: Role[];
    roles?: Role[];
    previousRealmRoles?: Role[];
    realmRoles?: Role[];
    previousAssetLinks?: UserAssetLink[];
    userAssetLinks?: UserAssetLink[];
}

const RESTRICTED_USER_REALM_ROLE = "restricted_user";

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

                .mdc-data-table__row.disabled {
                    cursor: progress;
                    opacity: 0.4;
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
    protected _realmRoles: RealmRole[] = [];
    @state()
    protected _compositeRoles: Role[] = [];
    protected _loading: boolean = false;

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

    protected responseAndStateOK(stateChecker: () => boolean, response: GenericAxiosResponse<any>, errorMsg: string): boolean {

        if (!stateChecker()) {
            return false;
        }

        if (!response.data) {
            showSnackbar(undefined, errorMsg, i18next.t("dismiss"));
            console.error(errorMsg + ": response = " + response.statusText);
            return false;
        }

        return true;
    }

    protected async loadUsers() {

        if (!this.realm || this._loading || !this.isConnected) {
            return;
        }

        this._loading = true;

        this._compositeRoles = [];
        this._roles = [];
        this._realmRoles = [];
        this._users = [];
        this._serviceUsers = [];

        if (!manager.authenticated || !manager.hasRole(ClientRole.READ_USERS)) {
            console.warn("Not authenticated or insufficient access");
            return;
        }

        // After async op check that the response still matches current state and that the component is still loaded in the UI
        const stateChecker = () => {
            return this.getState().app.realm === this.realm && this.isConnected;
        }

        const roleResponse = await manager.rest.api.UserResource.getRoles(manager.displayRealm);

        if (!this.responseAndStateOK(stateChecker, roleResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const tenantResponse = await manager.rest.api.TenantResource.get(manager.displayRealm);

        if (!this.responseAndStateOK(stateChecker, tenantResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const usersResponse = await manager.rest.api.UserResource.query({tenantPredicate: {realm: manager.displayRealm}} as UserQuery);

        if (!this.responseAndStateOK(stateChecker, usersResponse, i18next.t("loadFailedUsers"))) {
            return;
        }

        this._compositeRoles = roleResponse.data.filter(role => role.composite).sort(Util.sortByString(role => role.name));
        this._roles = roleResponse.data.filter(role => !role.composite).sort(Util.sortByString(role => role.name));
        this._realmRoles = (tenantResponse.data.realmRoles || []).sort(Util.sortByString(role => role.name));
        this._users = usersResponse.data.filter(user => !user.serviceAccount).sort(Util.sortByString(u => u.username));
        this._serviceUsers = usersResponse.data.filter(user => user.serviceAccount).sort(Util.sortByString(u => u.username));
        this._loading = false;
    }

    private async _createUpdateUser(user: UserModel) {

        if (!user.username) {
            return;
        }

        if (user.password === "") {
            // Means a validation failure shouldn't get here
            return;
        }

        const isUpdate = !!user.id;

        try {
            const response = await manager.rest.api.UserResource.createUpdate(manager.displayRealm, user);

            // Ensure user ID is set
            user.id = response.data.id;

            if (user.password) {
                const credentials = {value: user.password}
                manager.rest.api.UserResource.resetPassword(manager.displayRealm, user.id, credentials);
            }

            await this._updateRoles(user, false);
            await this._updateRoles(user, true);
            await this._updateUserAssetLinks(user);
        } catch (e) {
            if (isAxiosError(e)) {
                console.error((isUpdate ? "save user failed" : "create user failed") + ": response = " + e.response.statusText);

                if (e.response.status === 400) {
                    showSnackbar(undefined, i18next.t(isUpdate ? "saveUserFailed" : "createUserFailed"), i18next.t("dismiss"));
                }
            }
        } finally {
            this.loadUsers();
        }
    }

    /**
     * Backend only uses name of role not the ID so although service client roles are not the same as composite roles
     * the names will match so that's ok
     */
    private async _updateRoles(user: UserModel, realmRoles: boolean) {
        const roles = realmRoles ? user.realmRoles.filter(role => role.assigned) : user.roles.filter(role => role.assigned);
        const previousRoles = realmRoles ? user.previousRealmRoles : user.previousRoles;
        const removedRoles = previousRoles.filter(previousRole => !roles.some(role => role.name === previousRole.name));
        const addedRoles = roles.filter(role => !previousRoles.some(previousRole => previousRole.name === role.name));

        if (removedRoles.length === 0 && addedRoles.length === 0) {
            return;
        }

        if (realmRoles) {
            await manager.rest.api.UserResource.updateUserRealmRoles(manager.displayRealm, user.id, roles);
        } else {
            if (!user.serviceAccount) {
                await manager.rest.api.UserResource.updateUserRoles(manager.displayRealm, user.id, roles);
            } else {
                await manager.rest.api.UserResource.updateUserClientRoles(manager.displayRealm, user.id, user.username, roles);
            }
        }
    }

    private async _updateUserAssetLinks(user: UserModel) {
        if (!user.previousAssetLinks) {
            return;
        }

        const removedLinks = user.previousAssetLinks.filter(assetLink => !user.userAssetLinks.some(newLink => assetLink.id.assetId === newLink.id.assetId));
        const addedLinks = user.userAssetLinks.filter(assetLink => !user.previousAssetLinks.some(oldLink => assetLink.id.assetId === oldLink.id.assetId)).map(link => {
            // Ensure user ID is added as new users wouldn't have had an ID at the time the links were created in the UI
            link.id.userId = user.id;
            return link;
        });

        if (removedLinks.length > 0) {
            await manager.rest.api.AssetResource.deleteUserAssetLinks(removedLinks);
        }
        if (addedLinks.length > 0) {
            await manager.rest.api.AssetResource.createUserAssetLinks(addedLinks);
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
                        <table class="mdc-data-table__table" aria-label="user list">
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
                                    this._users = [...this._users, {realm: manager.displayRealm, userAssetLinks: [], roles:[], previousRoles: [], realmRoles: [], previousRealmRoles: [], enabled: true, loaded: true}];
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
                        <table class="mdc-data-table__table" aria-label="user list">
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
                                        userAssetLinks: [],
                                        roles: [],
                                        previousRoles: [],
                                        realmRoles: [],
                                        previousRealmRoles: [],
                                        loaded: true,
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

    protected async _toggleUserExpand(ev: MouseEvent, user: UserModel) {
        const trElem = ev.currentTarget as HTMLTableRowElement;
        const expanderIcon = trElem.getElementsByTagName("or-icon")[0] as OrIcon;
        const userRow = (trElem.parentElement! as HTMLTableElement).rows[trElem.rowIndex];

        if (user.loading) {
            return;
        }

        if (!user.loaded) {
            trElem.classList.add("disabled");
            user.loading = true;

            // Load users assigned roles
            const userRolesResponse = await (user.serviceAccount ? manager.rest.api.UserResource.getUserClientRoles(manager.displayRealm, user.id, user.username) : manager.rest.api.UserResource.getUserRoles(manager.displayRealm, user.id));

            if (!this.responseAndStateOK(() => true, userRolesResponse, i18next.t("loadFailedUserInfo"))) {
                user.loading = false;
                trElem.classList.remove("disabled");
                return;
            }

            const userRealmRolesResponse = await manager.rest.api.UserResource.getUserRealmRoles(manager.displayRealm, user.id);

            if (!this.responseAndStateOK(() => true, userRolesResponse, i18next.t("loadFailedUserInfo"))) {
                user.loading = false;
                trElem.classList.remove("disabled");
                return;
            }


            const userAssetLinksResponse = await manager.rest.api.AssetResource.getUserAssetLinks({realm: manager.displayRealm, userId: user.id});

            if (!this.responseAndStateOK(() => true, userAssetLinksResponse, i18next.t("loadFailedUserInfo"))) {
                user.loading = false;
                trElem.classList.remove("disabled");
                return;
            }

            user.roles = userRolesResponse.data.filter(r => r.assigned);
            user.realmRoles = userRealmRolesResponse.data.filter(r => r.assigned);
            user.previousRealmRoles = [...user.realmRoles];
            user.previousRoles = [...user.roles];
            user.userAssetLinks = userAssetLinksResponse.data;
            user.loaded = true;
            user.loading = false;
            trElem.classList.remove("disabled");
            // Update the dom
            this.requestUpdate();
        }

        if (expanderIcon.icon === "chevron-right") {
            expanderIcon.icon = "chevron-down";
            userRow.classList.add("expanded");
        } else {
            expanderIcon.icon = "chevron-right";
            userRow.classList.remove("expanded");
        }
    }

    protected _openAssetSelector(ev: MouseEvent, user: UserModel) {
        const openBtn = ev.target as OrMwcInput;
        openBtn.disabled = true;
        user.previousAssetLinks = [...user.userAssetLinks];

        const onAssetSelectionChanged = (e: OrAssetTreeSelectionEvent) => {
            user.userAssetLinks = e.detail.newNodes.map(node => {
                const userAssetLink: UserAssetLink = {
                    id: {
                        userId: user.id,
                        realm: user.realm,
                        assetId: node.asset.id
                    }
                };
                return userAssetLink;
            })
        };

        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("restrictAccess"))
            .setContent(html`
                <or-asset-tree 
                    id="chart-asset-tree" readonly .selectedIds="${user.userAssetLinks.map(ual => ual.id.assetId)}"
                    .showSortBtn="${false}" expandNodes checkboxes
                    @or-asset-tree-selection="${(e: OrAssetTreeSelectionEvent) => onAssetSelectionChanged(e)}"></or-asset-tree>
            `)
            .setActions([
                {
                    default: true,
                    actionName: "cancel",
                    content: i18next.t("cancel"),
                    action: () => {
                        user.userAssetLinks = user.previousAssetLinks;
                        user.previousAssetLinks = undefined;
                        openBtn.disabled = false;
                    }
                },
                {
                    actionName: "ok",
                    content: i18next.t("ok"),
                    action: () => {
                        openBtn.disabled = false;
                        this.requestUpdate();
                    }
                }
            ])
            .setDismissAction({
                actionName: "cancel",
                action: () => {
                    user.userAssetLinks = user.previousAssetLinks;
                    user.previousAssetLinks = undefined;
                    openBtn.disabled = false;
                }
            }));
    }

    protected onUserChanged(e: OrInputChangedEvent, suffix: string) {
        // Don't have form-associated custom element support in lit at time of writing which would be the way to go here
        const formElement = (e.target as HTMLElement).parentElement;
        const saveBtn = this.shadowRoot.getElementById("savebtn-" + suffix) as OrMwcInput;

        if (formElement) {
            const saveDisabled = Array.from(formElement.children).filter(e => e instanceof OrMwcInput).some(input => !(input as OrMwcInput).valid);
            saveBtn.disabled = saveDisabled;
        }
    }

    protected _onPasswordChanged(user: UserModel, suffix: string) {
        const passwordComponent = this.shadowRoot.getElementById("password-" + suffix) as OrMwcInput;
        const repeatPasswordComponent = this.shadowRoot.getElementById("repeatPassword-" + suffix) as OrMwcInput;

        if (repeatPasswordComponent.value !== passwordComponent.value) {
            const error = i18next.t("passwordMismatch");
            repeatPasswordComponent.setCustomValidity(error);
            user.password = "";
        } else {
            repeatPasswordComponent.setCustomValidity(undefined);
            user.password = passwordComponent.value;
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
        const implicitRoleNames = user.loaded ? this.getImplicitUserRoles(user) : [];

        return html`
            <tr class="mdc-data-table__row" @click="${(ev) => this._toggleUserExpand(ev, user)}">
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
                    ${!user.loaded ? `` : html`
                        <div class="item-row-content">
                            <div class="row">
                                <div class="column">
                                    <h5>${i18next.t("details")}</h5>
                                    <!-- user details -->
                                    <or-mwc-input ?readonly="${!!user.id || readonly}"
                                                  .label="${i18next.t("username")}"
                                                  .type="${InputType.TEXT}" minLength="3" maxLength="255" required
                                                  pattern="[a-zA-Z0-9-_]+"
                                                  .value="${user.username}" .validationMessage="${i18next.t("invalidUsername")}"
                                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {user.username = e.detail.value; this.onUserChanged(e, suffix)}}"></or-mwc-input>
                                    <or-mwc-input ?readonly="${readonly}"
                                                  class="${isServiceUser ? "hidden" : ""}"
                                                  .label="${i18next.t("email")}"
                                                  .type="${InputType.EMAIL}"
                                                  .value="${user.email}" .validationMessage="${i18next.t("invalidEmail")}"
                                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {user.email = e.detail.value; this.onUserChanged(e, suffix)}}"></or-mwc-input>
                                    <or-mwc-input ?readonly="${readonly}"
                                                  class="${isServiceUser ? "hidden" : ""}"
                                                  .label="${i18next.t("firstName")}"
                                                  .type="${InputType.TEXT}" minLength="1"
                                                  .value="${user.firstName}"
                                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {user.firstName = e.detail.value; this.onUserChanged(e, suffix)}}"></or-mwc-input>
                                    <or-mwc-input ?readonly="${readonly}"
                                                  class="${isServiceUser ? "hidden" : ""}"
                                                  .label="${i18next.t("surname")}"
                                                  .type="${InputType.TEXT}" minLength="1"
                                                  .value="${user.lastName}"
                                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {user.lastName = e.detail.value; this.onUserChanged(e, suffix)}}"></or-mwc-input>

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
                                                              @or-mwc-input-changed="${(ev) => this._regenerateSecret(ev, user, "password-" + suffix)}"></or-mwc-input>`
                                            : html`
                                                <or-mwc-input id="password-${suffix}"
                                                              ?readonly="${readonly}"
                                                              .label="${i18next.t("password")}"
                                                              .type="${InputType.PASSWORD}" min="1"
                                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                                                  this._onPasswordChanged(user, suffix);
                                                                  this.onUserChanged(e, suffix);
                                                              }}"></or-mwc-input>
                                                <or-mwc-input id="repeatPassword-${suffix}"
                                                              helperPersistent ?readonly="${readonly}"
                                                              .label="${i18next.t("repeatPassword")}"
                                                              .type="${InputType.PASSWORD}" min="1"
                                                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                                                  this._onPasswordChanged(user, suffix);
                                                                  this.onUserChanged(e, suffix);
                                                              }}"></or-mwc-input>
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
                                                user.roles = this._compositeRoles.filter(cr => roleNames.some(name => cr.name === name)).map(r => {
                                                    return {...r, assigned: true};
                                                });
                                                this._updateUserSelectedRoles(user, suffix);
                                            }}"></or-mwc-input>

                                    <!-- roles -->
                                    <div style="display:flex;flex-wrap:wrap;margin-bottom: 20px;"
                                         id="role-list-${suffix}">
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
                                    <div>
                                        <or-mwc-input
                                                id="${user.id}-restricted"
                                                .type="${InputType.CHECKBOX}"
                                                .label="${i18next.t("restrictedAccessToAssets") + ':'}"
                                                @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._addRemoveRealmRole(user, RESTRICTED_USER_REALM_ROLE, ev.detail.value as boolean)}"
                                                .value="${user.realmRoles ? !!user.realmRoles.find(r => r.name === RESTRICTED_USER_REALM_ROLE) : undefined}"></or-mwc-input>

                                        <or-mwc-input outlined
                                                      .type="${InputType.BUTTON}"
                                                      .label="${i18next.t("selectRestrictedAssets", {number: user.userAssetLinks.length})}"
                                                      @click="${(ev: MouseEvent) => this._openAssetSelector(ev, user)}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>

                            ${readonly ? `` : html`
                                <div class="row" style="margin-bottom: 0;">

                                    ${!isSameUser && user.id ? html`
                                        <or-mwc-input .label="${i18next.t("delete")}"
                                                      .type="${InputType.BUTTON}"
                                                      @click="${() => this._deleteUser(user)}"></or-mwc-input>
                                    ` : ``}
                                    ${!user.id ? html`
                                        <or-mwc-input .label="${i18next.t("cancel")}"
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
                    `}
                </td>
            </tr>
        `;
    }

    protected _addRemoveRealmRole(user: UserModel, roleName: string, add: boolean) {
        if (!add) {
            user.realmRoles = user.realmRoles.filter(role => role.name !== roleName);
        } else {
            const role = this._realmRoles.find(role => role.name === roleName);
            if (role) {
                user.realmRoles.push({...role, assigned: true});
            }
        }
        this.requestUpdate();
    }
}
