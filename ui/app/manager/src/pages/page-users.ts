import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {DefaultColor3, OPENREMOTE_CLIENT_ID, Util} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {ClientRole, Credential, Role, User, UserAssetLink, UserQuery, UserSession} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrAssetTreeRequestSelectionEvent, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {when} from 'lit/directives/when.js';
import {until} from 'lit/directives/until.js';
import {map} from 'lit/directives/map.js';

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageUsersProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "users",
        routes: ["users", "users/:id"],
        pageCreator: () => {
            return new PageUsers(store);
        },
    };
}

export interface UserModel extends User {
    password?: string;
    loaded?: boolean;
    loading?: boolean;
    previousRoles?: string[];
    roles?: string[];
    previousRealmRoles?: string[];
    realmRoles?: string[];
    previousAssetLinks?: UserAssetLink[];
    userAssetLinks?: UserAssetLink[];
}

@customElement("page-users")
export class PageUsers extends Page<AppStateKeyed> {
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
                    text-transform: uppercase;
                    font-weight: bolder;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    line-height: 1em;
                    margin-bottom: 10px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    align-items: center;
                    min-height: 36px;
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
                    flex: 1 1 0;
                    gap: 24px;
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                }

                .hidden {
                    display: none;
                }

                #table-users,
                #table-users table,
                #table-service-users,
                #table-service-users table {
                    width: 100%;
                    white-space: nowrap;
                }

                .mdc-data-table__row {
                    cursor: pointer;
                    border-top-color: #D3D3D3;
                }

                td, th {
                    width: 16%;
                    border: none;
                }

                .mdc-data-table__header-cell {
                    font-weight: bold;
                    color: ${unsafeCSS(DefaultColor3)};
                }

                .mdc-data-table__header-cell:first-child {
                    padding-left: 36px;
                }

                .padded-cell {
                    overflow-wrap: break-word;
                    word-wrap: break-word;
                }

                .attribute-meta-row td {
                    padding: 0;
                }

                .attribute-meta-row {
                    max-width: 0px;
                }

                .meta-item-container {
                    flex-direction: row;
                    overflow: hidden;
                    max-height: 0;
                    transition: max-height 0.25s ease-out;
                    padding-left: 16px;
                    padding-right: 16px;
                }

                .attribute-meta-row.expanded .meta-item-container {
                    max-height: 10000px;
                    max-width: none;
                    transition: max-height 1s ease-in;
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

                .button or-icon {
                    --or-icon-fill: var(--or-app-color4);
                    margin-right: 5px;
                }

                .button-row {
                    display: flex !important;
                    flex-direction: row !important;
                    margin-bottom: 0;
                    justify-content: space-between;
                }

                .mqtt-section {
                    border-top: 1px solid #e5e5e5;
                    margin-top: 8px;
                    padding-top: 16px;
                    padding-bottom: 16px;
                }

                #session-table {
                    --or-mwc-table-column-width-3: 0;
                }

                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }
                    .row {
                        display: block;
                        flex-direction: column;
                        gap: 0;
                    }
                    .panel {
                        border-radius: 0;
                        border-left: 0px;
                        border-right: 0px;
                        width: calc(100% - 48px);
                    }
                    .hide-mobile {
                        display: none;
                    }
                    td, th {
                        width: 50%;
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
    protected _userFilter = this.getDefaultUserFilter(false);
    @state()
    protected _serviceUserFilter = this.getDefaultUserFilter(true);
    @state()
    protected _passwordPolicy: string[] = [];
    @state()
    protected _roles: Role[] = [];
    @state()
    protected _realmRoles: string[] = [];
    @state()
    protected _registrationEmailAsUsername: boolean = false;
    @state()
    protected _compositeRoles: Role[] = [];
    @state()
    protected _loadDataPromise?: Promise<any>;
    @state()
    protected _saveUserPromise?: Promise<any>;
    @state()
    private _sessionLoader?: Promise<TemplateResult>;
    @state()
    private _expandedUserId: string | null = null;
    @state()
    private _expandedServiceUserId: string | null = null;

    get name(): string {
        return "user_plural";
    }

    public shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.reset();
            this.loadData();
        }
        if (changedProperties.has('_expandedServiceUserId')) {
            this._sessionLoader = undefined;
        }
        return super.shouldUpdate(changedProperties);
    }

    public connectedCallback() {
        super.connectedCallback();
        this.loadData();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
    }

    protected responseAndStateOK(stateChecker: () => boolean, response: GenericAxiosResponse<any>, errorMsg: string): boolean {
        if (!stateChecker()) {
            return false;
        }
        if (!response.data) {
            showSnackbar(undefined, errorMsg, "dismiss");
            console.error(errorMsg + ": response = " + response.statusText);
            return false;
        }
        return true;
    }

    protected async loadData(): Promise<void> {
        if (!this._loadDataPromise) {
            this._loadDataPromise = this.fetchUsers();
            this._loadDataPromise.finally(() => {
                this._loadDataPromise = undefined;
            });
        }
        return this._loadDataPromise;
    }

    protected async fetchUsers(): Promise<void> {
        if (!this.realm || !this.isConnected) {
            return;
        }

        this._compositeRoles = [];
        this._roles = [];
        this._realmRoles = [];
        this._users = [];
        this._serviceUsers = [];

        if (!manager.authenticated || !manager.hasRole(ClientRole.READ_USERS)) {
            console.warn("Not authenticated or insufficient access");
            return;
        }

        const stateChecker = () => {
            return this.getState().app.realm === this.realm && this.isConnected;
        };

        const roleResponse = await manager.rest.api.UserResource.getClientRoles(manager.displayRealm, OPENREMOTE_CLIENT_ID);
        if (!this.responseAndStateOK(stateChecker, roleResponse, "loadFailedRoles")) {
            return;
        }

        const realmResponse = await manager.rest.api.RealmResource.get(manager.displayRealm);
        if (!this.responseAndStateOK(stateChecker, realmResponse, "loadFailedRoles")) {
            return;
        }

        const usersResponse = await manager.rest.api.UserResource.query({realmPredicate: {name: manager.displayRealm}} as UserQuery);
        if (!this.responseAndStateOK(stateChecker, usersResponse, "loadFailedUsers")) {
            return;
        }

        this._compositeRoles = roleResponse.data.filter(role => role.composite).sort(Util.sortByString(role => role.name));
        this._roles = roleResponse.data.filter(role => !role.composite).sort(Util.sortByString(role => role.name));
        this._registrationEmailAsUsername = realmResponse.data.registrationEmailAsUsername;
        this._realmRoles = (realmResponse.data.realmRoles || []).map(role => role.name).sort();
        this._passwordPolicy = realmResponse.data.passwordPolicy;
        this._users = usersResponse.data.filter(user => !user.serviceAccount).sort(Util.sortByString(u => u.username));
        this._serviceUsers = usersResponse.data.filter(user => user.serviceAccount).sort(Util.sortByString(u => u.username));
    }

    private async _createUpdateUser(user: UserModel, action: 'update' | 'create'): Promise<boolean> {
        let result = false;

        if (this._registrationEmailAsUsername && !user.serviceAccount && !user.email) {
            showSnackbar(undefined, "noEmailSet", "dismiss");
            return false;
        } else if ((!this._registrationEmailAsUsername || user.serviceAccount) && !user.username) {
            showSnackbar(undefined, "noUsernameSet", "dismiss");
            return false;
        }
        if (user.serviceAccount && user.username.startsWith('gateway-')) {
            showSnackbar(undefined, "noGatewayUsername", "dismiss");
            return false;
        }
        if (user.password === "") {
            return false;
        }

        const isUpdate = !!user.id;

        try {
            const response = action === 'update'
                ? await manager.rest.api.UserResource.update(manager.displayRealm, user)
                : await manager.rest.api.UserResource.create(manager.displayRealm, user);

            user.id = response.data.id;

            await this._updateRoles(user, false);
            await this._updateRoles(user, true);
            await this._updateUserAssetLinks(user);
            result = true;
        } catch (e) {
            if (isAxiosError(e)) {
                console.error((isUpdate ? "save user failed" : "create user failed") + ": response = " + e.response.statusText);
                if (e.response.status === 400) {
                    showSnackbar(undefined, (isUpdate ? "saveUserFailed" : "createUserFailed"), "dismiss");
                } else if (e.response.status === 403) {
                    showSnackbar(undefined, "userAlreadyExists");
                }
            }
            result = false;
            throw e;
        } finally {
            await this.loadData();

            if (user.password) {
                const credentials = {value: user.password} as Credential;
                manager.rest.api.UserResource.updatePassword(manager.displayRealm, user.id, credentials).catch(e => {
                    if (isAxiosError(e) && e.response.status !== 404) {
                        showSnackbar(undefined, "savePasswordFailed");
                    }
                });
            }

            return result;
        }
    }

    private async _updateRoles(user: UserModel, realmRoles: boolean) {
        if (realmRoles) {
            if (!Util.objectsEqual(user.realmRoles, user.previousRealmRoles)) {
                await manager.rest.api.UserResource.updateUserRealmRoles(manager.displayRealm, user.id, user.realmRoles);
            }
        } else {
            const roles = user.roles;
            const previousRoles = user.previousRoles;
            const removedRoles = previousRoles.filter(previousRole => !roles.some(role => role === previousRole));
            const addedRoles = roles.filter(role => !previousRoles.some(previousRole => previousRole === role));

            if (removedRoles.length === 0 && addedRoles.length === 0) {
                return;
            }
            await manager.rest.api.UserResource.updateUserClientRoles(manager.displayRealm, user.id, OPENREMOTE_CLIENT_ID, roles);
        }
    }

    private async _updateUserAssetLinks(user: UserModel) {
        if (!user.previousAssetLinks) {
            return;
        }

        const removedLinks = user.previousAssetLinks.filter(assetLink => !user.userAssetLinks.some(newLink => assetLink.id.assetId === newLink.id.assetId));
        const addedLinks = user.userAssetLinks.filter(assetLink => !user.previousAssetLinks.some(oldLink => assetLink.id.assetId === oldLink.id.assetId)).map(link => {
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
        showOkCancelDialog(i18next.t("deleteUser"), i18next.t("deleteUserConfirm", {userName: user.username}), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this.doDelete(user);
                }
            });
    }

    private doDelete(user) {
        manager.rest.api.UserResource.delete(manager.displayRealm, user.id).then(() => {
            if (user.serviceAccount) {
                this._serviceUsers = [...this._serviceUsers.filter(u => u.id !== user.id)];
            } else {
                this._users = [...this._users.filter(u => u.id !== user.id)];
            }
            this.reset();
        });
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`<or-translate value="notAuthenticated"></or-translate>`;
        }

        const users = this._userFilter(this._users);
        const serviceUsers = this._serviceUserFilter(this._serviceUsers);
        const compositeRoleOptions: string[] = this._compositeRoles.map(cr => cr.name);
        const realmRoleOptions: [string, string][] = this._realmRoles.map(r => [r, i18next.t("realmRole." + r, Util.camelCaseToSentenceCase(r.replace("_", " ").replace("-", " ")))]);
        const readonly = !manager.hasRole(ClientRole.WRITE_ADMIN);

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="account-group"></or-icon>
                    <span>${i18next.t('user_plural')}</span>
                </div>

                <!-- Regular users panel -->
                <div class="panel">
                    <div class="panel-title" style="justify-content: space-between;">
                        <p style="margin:0;">${i18next.t("regularUser_plural")}</p>
                        <or-mwc-input type="${InputType.TEXT}" placeholder="${i18next.t('search')}"
                                      style="margin: 0; text-transform: none;" iconTrailing="magnify" compact outlined
                                      @input="${(ev) => this.onRegularUserSearch(ev)}"
                        ></or-mwc-input>
                    </div>
                    <div id="table-users" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="user list">
                            <thead>
                                <tr class="mdc-data-table__header-row">
                                    <th class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t('username')}</th>
                                    <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">${i18next.t('firstName')}</th>
                                    <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">${i18next.t('surname')}</th>
                                    <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">${i18next.t('email')}</th>
                                    <th class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t('status')}</th>
                                    <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">${i18next.t('tag')}</th>
                                </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                                ${users.map((user, index) => {
                                    const userKey = user.id || 'new';
                                    const isExpanded = this._expandedUserId === userKey;
                                    const suffix = 'reg-' + index;
                                    return html`
                                        <tr class="mdc-data-table__row" @click="${() => this._toggleUser(user)}">
                                            <td class="padded-cell mdc-data-table__cell">
                                                <or-icon icon="${isExpanded ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                                <span>${user.username}</span>
                                            </td>
                                            <td class="padded-cell mdc-data-table__cell hide-mobile">${user.firstName}</td>
                                            <td class="padded-cell mdc-data-table__cell hide-mobile">${user.lastName}</td>
                                            <td class="padded-cell mdc-data-table__cell hide-mobile">${user.email}</td>
                                            <td class="padded-cell mdc-data-table__cell">${user.enabled ? i18next.t('enabled') : i18next.t('disabled')}</td>
                                            <td class="padded-cell mdc-data-table__cell hide-mobile">${user.attributes?.Tag?.[0]}</td>
                                        </tr>
                                        <tr class="attribute-meta-row${isExpanded ? ' expanded' : ''}">
                                            <td colspan="100%">
                                                <div class="meta-item-container">
                                                    ${this.getSingleUserView(user, compositeRoleOptions, realmRoleOptions, suffix, (readonly || this._saveUserPromise != undefined), () => this._collapseUser(user))}
                                                </div>
                                            </td>
                                        </tr>
                                    `;
                                })}
                                ${(!readonly && this._expandedUserId !== 'new') ? html`
                                    <tr class="mdc-data-table__row">
                                        <td colspan="100%">
                                            <a class="button" @click="${(e) => { e.stopPropagation(); this._addNewUser(false); }}">
                                                <or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("user")}
                                            </a>
                                        </td>
                                    </tr>
                                ` : ''}
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Service users panel -->
                <div class="panel">
                    <div class="panel-title" style="justify-content: space-between;">
                        <p style="margin:0;">${i18next.t("serviceUser_plural")}</p>
                        <or-mwc-input type="${InputType.TEXT}" placeholder="${i18next.t('search')}"
                                      style="margin: 0; text-transform: none;" iconTrailing="magnify" compact outlined
                                      @input="${(ev) => this.onServiceUserSearch(ev)}"
                        ></or-mwc-input>
                    </div>
                    <div id="table-service-users" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="service user list">
                            <thead>
                                <tr class="mdc-data-table__header-row">
                                    <th class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t('username')}</th>
                                    <th class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t('status')}</th>
                                    <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">${i18next.t('tag')}</th>
                                </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                                ${serviceUsers.map((user, index) => {
                                    const userKey = user.id || 'new';
                                    const isExpanded = this._expandedServiceUserId === userKey;
                                    const suffix = 'svc-' + index;
                                    return html`
                                        <tr class="mdc-data-table__row" @click="${() => this._toggleServiceUser(user)}">
                                            <td class="padded-cell mdc-data-table__cell">
                                                <or-icon icon="${isExpanded ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                                <span>${user.username}</span>
                                            </td>
                                            <td class="padded-cell mdc-data-table__cell">${user.enabled ? i18next.t('enabled') : i18next.t('disabled')}</td>
                                            <td class="padded-cell mdc-data-table__cell hide-mobile">${user.attributes?.Tag?.[0]}</td>
                                        </tr>
                                        <tr class="attribute-meta-row${isExpanded ? ' expanded' : ''}">
                                            <td colspan="100%">
                                                <div class="meta-item-container">
                                                    ${this.getSingleUserView(user, compositeRoleOptions, realmRoleOptions, suffix, (readonly || this._saveUserPromise != undefined), () => this._collapseUser(user))}
                                                    ${when(user.id && isExpanded, () => this._getMQTTSessionInline(user))}
                                                </div>
                                            </td>
                                        </tr>
                                    `;
                                })}
                                ${(!readonly && this._expandedServiceUserId !== 'new') ? html`
                                    <tr class="mdc-data-table__row">
                                        <td colspan="100%">
                                            <a class="button" @click="${(e) => { e.stopPropagation(); this._addNewUser(true); }}">
                                                <or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("serviceUser")}
                                            </a>
                                        </td>
                                    </tr>
                                ` : ''}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    private _toggleUser(user: UserModel) {
        const userKey = user.id || 'new';
        this._expandedUserId = this._expandedUserId === userKey ? null : userKey;
    }

    private _toggleServiceUser(user: UserModel) {
        const userKey = user.id || 'new';
        this._expandedServiceUserId = this._expandedServiceUserId === userKey ? null : userKey;
    }

    private _addNewUser(serviceAccount: boolean) {
        const newUser = this.getNewUserModel(serviceAccount);
        if (serviceAccount) {
            this._serviceUsers = [...this._serviceUsers, newUser];
            this._expandedServiceUserId = 'new';
        } else {
            this._users = [...this._users, newUser];
            this._expandedUserId = 'new';
        }
    }

    private _collapseUser(user: UserModel) {
        if (user.serviceAccount) {
            this._expandedServiceUserId = null;
            if (!user.id) {
                this._serviceUsers = this._serviceUsers.filter(u => u !== user);
            }
        } else {
            this._expandedUserId = null;
            if (!user.id) {
                this._users = this._users.filter(u => u !== user);
            }
        }
    }

    protected getNewUserModel(serviceAccount: boolean): UserModel {
        return {
            enabled: true,
            password: undefined,
            realm: manager.displayRealm,
            roles: [],
            previousRoles: [],
            realmRoles: [],
            previousRealmRoles: [],
            userAssetLinks: [],
            serviceAccount: serviceAccount
        };
    }

    protected getDefaultUserFilter(serviceUser: boolean): (users: UserModel[]) => UserModel[] {
        return (users) => users;
    }

    protected onRegularUserSearch(ev: InputEvent) {
        const value = (ev.target as OrMwcInput).nativeValue?.toLowerCase();
        if (!value) {
            this._userFilter = this.getDefaultUserFilter(false);
        } else {
            this._userFilter = (users) => {
                return users.filter(u =>
                    (u.username as string)?.toLowerCase().includes(value) ||
                    (u.firstName as string)?.toLowerCase().includes(value) ||
                    (u.lastName as string)?.toLowerCase().includes(value) ||
                    (u.email as string)?.toLowerCase().includes(value) ||
                    (u.attributes?.Tag?.[0] as string)?.toLowerCase().includes(value)
                );
            };
        }
    }

    protected onServiceUserSearch(ev: InputEvent) {
        const value = (ev.target as OrMwcInput).nativeValue?.toLowerCase();
        if (!value) {
            this._serviceUserFilter = this.getDefaultUserFilter(true);
        } else {
            this._serviceUserFilter = (users) => {
                return users.filter(u =>
                    (u.username as string)?.includes(value) ||
                    (u.attributes?.Tag?.[0] as string)?.toLowerCase().includes(value)
                );
            };
        }
    }

    protected async loadUser(user: UserModel) {
        if (user.roles || user.realmRoles) {
            return;
        }

        const userRolesResponse = await (manager.rest.api.UserResource.getUserClientRoles(manager.displayRealm, user.id, OPENREMOTE_CLIENT_ID));
        if (!this.responseAndStateOK(() => true, userRolesResponse, "loadFailedUserInfo")) {
            return;
        }

        const userRealmRolesResponse = await manager.rest.api.UserResource.getUserRealmRoles(manager.displayRealm, user.id);
        if (!this.responseAndStateOK(() => true, userRolesResponse, "loadFailedUserInfo")) {
            return;
        }

        const userAssetLinksResponse = await manager.rest.api.AssetResource.getUserAssetLinks({
            realm: manager.displayRealm,
            userId: user.id
        });
        if (!this.responseAndStateOK(() => true, userAssetLinksResponse, "loadFailedUserInfo")) {
            return;
        }

        user.roles = userRolesResponse.data;
        user.realmRoles = userRealmRolesResponse.data;
        user.previousRealmRoles = [...user.realmRoles];
        user.previousRoles = [...user.roles];
        user.userAssetLinks = userAssetLinksResponse.data;
        user.loaded = true;
        user.loading = false;

        this.requestUpdate();
    }

    protected _openAssetSelector(ev: MouseEvent, user: UserModel, readonly: boolean, suffix: string) {
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
            });
        };

        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("linkedAssets"))
            .setContent(html`
                <or-asset-tree
                        id="chart-asset-tree" readonly .selectedIds="${user.userAssetLinks.map(ual => ual.id.assetId)}"
                        .showSortBtn="${false}" expandNodes checkboxes
                        @or-asset-tree-request-selection="${(e: OrAssetTreeRequestSelectionEvent) => {
                            if (readonly) {
                                e.detail.allow = false;
                            }
                        }}"
                        @or-asset-tree-selection="${(e: OrAssetTreeSelectionEvent) => {
                            if (!readonly) {
                                onAssetSelectionChanged(e);
                            }
                        }}"></or-asset-tree>
            `)
            .setActions([
                {
                    default: true,
                    actionName: "cancel",
                    content: "cancel",
                    action: () => {
                        user.userAssetLinks = user.previousAssetLinks;
                        user.previousAssetLinks = undefined;
                        openBtn.disabled = false;
                    }
                },
                {
                    actionName: "ok",
                    content: "ok",
                    action: () => {
                        openBtn.disabled = false;
                        this.onUserChanged(suffix);
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

    protected onUserChanged(suffix: string) {
        const validateArray = this.shadowRoot.querySelectorAll(".validate");
        const saveBtn = this.shadowRoot.getElementById("savebtn-" + suffix) as OrMwcInput;
        const saveDisabled = Array.from(validateArray).filter(e => e instanceof OrMwcInput).some(input => !(input as OrMwcInput).valid);
        saveBtn.disabled = saveDisabled;
        this.requestUpdate();
    }

    protected _onPasswordChanged(user: UserModel, suffix: string) {
        const passwordComponent = this.shadowRoot.getElementById("new-password-" + suffix) as OrMwcInput;
        const repeatPasswordComponent = this.shadowRoot.getElementById("new-repeatPassword-" + suffix) as OrMwcInput;

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
            showSnackbar(undefined, "errorOccurred");
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
        const roleCheckboxes = [...((this.shadowRoot.getElementById("role-list-" + suffix) as HTMLDivElement).children as any)] as OrMwcInput[];
        const implicitRoleNames = this.getImplicitUserRoles(user);
        roleCheckboxes.forEach((checkbox) => {
            const roleName = checkbox.label;
            const r = this._roles.find(role => roleName === role.name);
            checkbox.disabled = !!implicitRoleNames.find(name => r.name === name);
            checkbox.value = !!user.roles.find(userRole => userRole === r.name) || implicitRoleNames.some(implicitRoleName => implicitRoleName === r.name);
        });
    }

    protected getImplicitUserRoles(user: UserModel) {
        return this._compositeRoles.filter((role) => user.roles.some(ur => ur === role.name)).flatMap((role) => role.compositeRoleIds).map(id => this._roles.find(r => r.id === id).name);
    }

    protected _isUsernameTaken(username: string, currentUser: UserModel): boolean {
        if (!username) return false;
        const lower = username.toLowerCase();
        return [...this._users, ...this._serviceUsers].some(u => u !== currentUser && u.username?.toLowerCase() === lower);
    }

    protected getSingleUserView(user: UserModel, compositeRoleOptions: string[], realmRoleOptions: [string, string][], suffix: string, readonly: boolean = true, onClose?: () => void): TemplateResult {
        return html`
            ${when((user.loaded || (user.roles && user.realmRoles)), () => {
                return this.getSingleUserTemplate(user, compositeRoleOptions, realmRoleOptions, suffix, readonly, onClose);
            }, () => {
                const getTemplate = async () => {
                    await this.loadUser(user);
                    return this.getSingleUserTemplate(user, compositeRoleOptions, realmRoleOptions, suffix, readonly, onClose);
                };
                const content: Promise<TemplateResult> = getTemplate();
                return html`${until(content, html`${i18next.t('loading')}`)}`;
            })}
        `;
    }

    private _getMQTTSessionInline(user: UserModel): TemplateResult {
        if (!this._sessionLoader) {
            this._sessionLoader = this.getSessionLoader(user);
        }
        return html`
            <div class="mqtt-section">
                <p class="panel-title" style="margin-bottom: 10px;">${i18next.t('mqttSessions')}</p>
                ${until(this._sessionLoader, html`${i18next.t('loading')}`)}
            </div>
        `;
    }

    protected async getSessionLoader(user: UserModel): Promise<TemplateResult> {
        const userSessionsResponse = await (manager.rest.api.UserResource.getUserSessions(manager.displayRealm, user.id));

        if (!this.responseAndStateOK(() => userSessionsResponse.status === 200, userSessionsResponse, "loadFailedUserInfo")) {
            return html``;
        }

        const cols = [i18next.t("address"), i18next.t("since"), ""];
        const rows = userSessionsResponse.data.map((session) => {
            return [session.remoteAddress, new Date(session.startTimeMillis), html`<or-mwc-input .type="${InputType.BUTTON}" label="disconnect" @or-mwc-input-changed="${() => {this.disconnectSession(user, session)}}"></or-mwc-input>`];
        });
        if (rows.length < 1) {
            return html`<or-mwc-table .rows="${[[i18next.t('noMqttSessions'), null]]}" .config="${{stickyFirstColumn: false}}" .columns="${cols}"></or-mwc-table>`;
        }

        return html`<or-mwc-table id="session-table" .rows="${rows}" .config="${{stickyFirstColumn: false}}" .columns="${cols}"></or-mwc-table>`;
    }

    protected getSingleUserTemplate(user: UserModel, compositeRoleOptions: string[], realmRoleOptions: [string, string][], suffix: string, readonly: boolean = true, onClose?: () => void): TemplateResult {
        const isServiceUser = user.serviceAccount;
        const isSameUser = user.username === manager.username;
        const isGatewayServiceUser = isServiceUser && user.username?.startsWith("gateway-");
        const implicitRoleNames = user.loaded ? this.getImplicitUserRoles(user) : [];
        return html`
            <div class="row" style="padding: 16px 0 8px;">
                <div class="column">
                    <h5>${i18next.t("details")}</h5>
                    <or-mwc-input id="new-username-${suffix}" ?readonly="${!!user.id || readonly}" .disabled="${!!user.id || (!isServiceUser && this._registrationEmailAsUsername)}"
                                  class="validate"
                                  .label="${i18next.t("username")}"
                                  .type="${InputType.TEXT}" minLength="3" maxLength="255"
                                  ?required="${isServiceUser || !this._registrationEmailAsUsername}"
                                  pattern="[A-Za-z0-9_+@\\.\\-ßçʊÇʊ]+"
                                  .value="${user.username}" autocomplete="false"
                                  .validationMessage="${i18next.t("invalidUsername")}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.username = e.detail.value;
                                      (e.target as OrMwcInput).setCustomValidity(this._isUsernameTaken(user.username, user) ? i18next.t('userAlreadyExists') : undefined);
                                      this.onUserChanged(suffix);
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-email" ?readonly="${(!!user.id && this._registrationEmailAsUsername) || readonly}"
                                  .disabled="${!!user.id && this._registrationEmailAsUsername}"
                                  class="${isServiceUser ? "hidden" : "validate"}"
                                  .label="${i18next.t("email")}"
                                  .type="${InputType.EMAIL}"
                                  .value="${user.email}" autocomplete="false"
                                  ?required="${!isServiceUser && this._registrationEmailAsUsername}"
                                  pattern="^[\\w\\.\\-\\+\\%]+@([\\w\\-]+\\.)+[\\w]{2,}$"
                                  .validationMessage="${i18next.t("invalidEmail")}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      if (this._registrationEmailAsUsername) {
                                          user.username = e.detail.value;
                                          (e.target as OrMwcInput).setCustomValidity(this._isUsernameTaken(user.username, user) ? i18next.t('userAlreadyExists') : undefined);
                                      }
                                      user.email = e.detail.value;
                                      this.onUserChanged(suffix);
                                      this.requestUpdate();
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-firstName" ?readonly="${readonly}"
                                  class="${isServiceUser ? "hidden" : "validate"}"
                                  .label="${i18next.t("firstName")}"
                                  .type="${InputType.TEXT}" minLength="1"
                                  .value="${user.firstName}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.firstName = e.detail.value;
                                      this.onUserChanged(suffix);
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-surname" ?readonly="${readonly}"
                                  class="${isServiceUser ? "hidden" : "validate"}"
                                  .label="${i18next.t("surname")}"
                                  .type="${InputType.TEXT}" minLength="1"
                                  .value="${user.lastName}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.lastName = e.detail.value;
                                      this.onUserChanged(suffix);
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-tag" ?readonly="${readonly || isGatewayServiceUser}"
                                  class="validate"
                                  .label="${i18next.t("tag")}"
                                  .type="${InputType.TEXT}" minLength="1"
                                  .value="${user.attributes?.Tag?.[0]}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      if (!user.attributes) {
                                          user.attributes = {};
                                      }
                                      if (!Array.isArray(user.attributes.Tag)) {
                                          user.attributes.Tag = [];
                                      }
                                      user.attributes.Tag[0] = e.detail.value;
                                      this.onUserChanged(suffix);
                                  }}"></or-mwc-input>
                    <h5>${i18next.t("password")}</h5>
                    ${isServiceUser ? html`
                        ${when(user.secret, () => html`
                            <or-mwc-input id="new-password-${suffix}" readonly
                                          class="validate"
                                          .label="${i18next.t("secret")}"
                                          .value="${user.secret}"
                                          .type="${InputType.TEXT}"></or-mwc-input>
                            <or-mwc-input ?readonly="${!user.id || readonly}"
                                          ?disabled="${isGatewayServiceUser}"
                                          .label="${i18next.t("regenerateSecret")}"
                                          .type="${InputType.BUTTON}"
                                          @or-mwc-input-changed="${(ev) => {
                                              this._regenerateSecret(ev, user, "new-password-" + suffix).catch(() => showSnackbar(undefined, 'errorOccurred'));
                                              this.onUserChanged(suffix);
                                          }}"></or-mwc-input>
                        `, () => html`
                            <span>${i18next.t("generateSecretInfo")}</span>
                        `)}
                    ` : html`
                        <or-mwc-input id="new-password-${suffix}"
                                      ?readonly="${readonly}"
                                      class="validate"
                                      .label="${i18next.t("password")}"
                                      .type="${InputType.PASSWORD}" min="1" autocomplete="false"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          this._onPasswordChanged(user, suffix);
                                          this.onUserChanged(suffix);
                                      }}"
                        ></or-mwc-input>
                        <or-mwc-input id="new-repeatPassword-${suffix}"
                                      helperPersistent ?readonly="${readonly}"
                                      .label="${i18next.t("repeatPassword")}"
                                      .type="${InputType.PASSWORD}" min="1" autocomplete="false"
                                      style="${this._passwordPolicy ? 'margin-bottom: 0' : undefined}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          this._onPasswordChanged(user, suffix);
                                          this.onUserChanged(suffix);
                                      }}"
                        ></or-mwc-input>
                        ${when(this._passwordPolicy, () => until(this._getPasswordPolicyTemplate(user, this._passwordPolicy)))}
                    `}
                </div>

                <div class="column">
                    <h5>${i18next.t("settings")}</h5>
                    <or-mwc-input ?readonly="${readonly || isGatewayServiceUser}"
                                  class="validate"
                                  .label="${i18next.t("active")}"
                                  .type="${InputType.CHECKBOX}"
                                  .value="${user.enabled}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.enabled = e.detail.value;
                                      this.onUserChanged(suffix);
                                  }}"
                                  style="height: 56px;"
                    ></or-mwc-input>
                    <or-mwc-input
                            ?readonly="${readonly}"
                            ?disabled="${isSameUser || isGatewayServiceUser}"
                            class="validate"
                            .value="${user.realmRoles}"
                            .type="${InputType.SELECT}" multiple
                            .options="${realmRoleOptions}"
                            .label="${i18next.t("realm_role_plural")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                this.onUserChanged(suffix);
                                user.realmRoles = e.detail.value as string[];
                            }}"></or-mwc-input>
                    <or-mwc-input
                            ?readonly="${readonly}"
                            ?disabled="${isSameUser || isGatewayServiceUser}"
                            class="validate"
                            .value="${user.roles && user.roles.length > 0 ? user.roles.filter(r => this._compositeRoles.some(cr => cr.name === r)) : undefined}"
                            .type="${InputType.SELECT}" multiple
                            .options="${compositeRoleOptions}"
                            .label="${i18next.t("manager_role_plural")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                user.roles = e.detail.value as string[];
                                this._updateUserSelectedRoles(user, suffix);
                                this.onUserChanged(suffix);
                            }}"></or-mwc-input>
                    <div style="display:flex;flex-wrap:wrap;margin-bottom: 20px;"
                         id="role-list-${suffix}">
                        ${this._roles.map(r => {
                            return html`
                                <or-mwc-input
                                        ?readonly="${readonly}"
                                        ?disabled="${implicitRoleNames.find(name => r.name === name) || isGatewayServiceUser}"
                                        class="validate"
                                        .value="${!!user.roles.find(userRole => userRole === r.name) || implicitRoleNames.some(implicitRoleName => implicitRoleName === r.name)}"
                                        .type="${InputType.CHECKBOX}"
                                        .label="${r.name}"
                                        title="${r.description}"
                                        style="flex: 0 1 160px; margin: 0; overflow: hidden;"
                                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                            if (!!e.detail.value) {
                                                user.roles.push(r.name);
                                            } else {
                                                user.roles = user.roles.filter(ur => ur !== r.name);
                                            }
                                            this.onUserChanged(suffix);
                                        }}"></or-mwc-input>
                            `;
                        })}
                    </div>
                    <div>
                        <span>${i18next.t("linkedAssets")}:</span>
                        <or-mwc-input outlined ?disabled="${readonly || isGatewayServiceUser}" style="margin-left: 4px;"
                                      .type="${InputType.BUTTON}"
                                      .label="${i18next.t("selectRestrictedAssets", {number: user.userAssetLinks.length})}"
                                      @or-mwc-input-changed="${(ev: MouseEvent) => this._openAssetSelector(ev, user, readonly, suffix)}"></or-mwc-input>
                    </div>
                </div>
            </div>
            ${when(!(readonly && !this._saveUserPromise), () => html`
                <div class="row" style="margin-bottom: 0;">
                    ${when(!user.id, () => html`
                        <or-mwc-input label="cancel"
                                      .type="${InputType.BUTTON}"
                                      @or-mwc-input-changed="${() => onClose?.()}"
                        ></or-mwc-input>
                    `)}
                    ${when((!isSameUser && !isGatewayServiceUser && user.id), () => html`
                        <or-mwc-input label="delete" ?disabled="${readonly}"
                                      .type="${InputType.BUTTON}"
                                      @or-mwc-input-changed="${() => this._deleteUser(user)}"
                        ></or-mwc-input>
                    `)}
                        <or-mwc-input id="savebtn-${suffix}" style="margin-left: auto;" disabled
                                      label="${user.id ? "save" : "create"}"
                                      .type="${InputType.BUTTON}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          let error: { status?: number, text: string };
                                          this._saveUserPromise = this._createUpdateUser(user, user.id ? 'update' : 'create').then((result) => {
                                              if (result) {
                                                  showSnackbar(undefined, "saveUserSucceeded");
                                                  onClose?.();
                                              }
                                          }).catch((ex) => {
                                              console.error(ex);
                                              if (isAxiosError(ex)) {
                                                  error = {
                                                      status: ex.response.status,
                                                      text: (ex.response.status == 403 ? i18next.t('userAlreadyExists') : i18next.t('errorOccurred'))
                                                  };
                                              }
                                          }).finally(() => {
                                              this._saveUserPromise = undefined;
                                              if (error) {
                                                  this.updateComplete.then(() => {
                                                      showSnackbar(undefined, error.text);
                                                      if (error.status === 403) {
                                                          const elem = this.shadowRoot?.getElementById('new-username-' + suffix) as OrMwcInput | null;
                                                          if (elem) {
                                                              elem.setCustomValidity(error.text);
                                                              const inputElem = elem.shadowRoot?.getElementById("elem") as HTMLInputElement | null;
                                                              inputElem?.reportValidity();
                                                              this.onUserChanged(suffix);
                                                          }
                                                      }
                                                  });
                                              }
                                          });
                                      }}">
                        </or-mwc-input>
                </div>
            `)}
        `;
    }

    protected async _getPasswordPolicyTemplate(user: UserModel, passwordPolicy = this._passwordPolicy): Promise<TemplateResult> {
        const policyMap = new Map(passwordPolicy.map(policyStr => {
            const name = policyStr.split("(")[0];
            const value = policyStr.split("(")[1].split(")")[0];
            return [name, value];
        }));
        const policies = Array.from(policyMap.keys());
        const policyTexts: TemplateResult[] = [];

        if (policies.includes("length") && policies.includes("maxLength")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length" .options="${{0: policyMap.get("length"), 1: policyMap.get("maxLength")}}"></or-translate>`);
        } else if (policies.includes("length")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length-too-short" .options="${{0: policyMap.get("length")}}"></or-translate>`);
        } else if (policies.includes("maxLength")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length-too-long" .options="${{0: policyMap.get("maxLength")}}"></or-translate>`);
        }

        if (policies.includes("specialChars")) {
            const value = policyMap.get("specialChars");
            const translation = value == "1" ? "password-policy-special-chars-single" : "password-policy-special-chars";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        if (policies.includes("digits")) {
            const value = policyMap.get("digits");
            const translation = value == "1" ? "password-policy-digits-single" : "password-policy-digits";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        if (policies.includes("upperCase")) {
            const value = policyMap.get("upperCase");
            const translation = value == "1" ? "password-policy-uppercase-single" : "password-policy-uppercase";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        if (policies.includes("passwordHistory")) {
            policyTexts.push(html`<or-translate value="password-policy-recently-used"></or-translate>`);
        }

        if (policies.includes("notUsername") && policies.includes("notEmail")) {
            policyTexts.push(html`<or-translate value="password-policy-not-email-username"></or-translate>`);
        } else if (policies.includes("notUsername")) {
            policyTexts.push(html`<or-translate value="password-policy-not-username"></or-translate>`);
        } else if (policies.includes("notEmail")) {
            policyTexts.push(html`<or-translate value="password-policy-not-email"></or-translate>`);
        }

        return html`
            <ul>
                ${map(policyTexts, text => html`<li>${text}</li>`)}
            </ul>
        `;
    }

    protected reset() {
        this._expandedUserId = null;
        this._expandedServiceUserId = null;
        this._users = this._users.filter(u => !!u.id);
        this._serviceUsers = this._serviceUsers.filter(u => !!u.id);
        this._userFilter = this.getDefaultUserFilter(false);
        this._serviceUserFilter = this.getDefaultUserFilter(true);
    }

    public stateChanged(state: AppStateKeyed) {
        if (state.app.page == 'users') {
            this.realm = state.app.realm;
            // Support direct navigation to a user via users/:id (e.g. from the asset viewer)
            const userId: string | undefined = state.app.params?.id;
            if (userId && (this._expandedUserId !== userId || this._expandedServiceUserId !== userId)) {
                this._setExpandedUserFromRoute(userId);
            }
        }
    }

    protected _setExpandedUserFromRoute(userId: string) {
        const isServiceUser = this._serviceUsers.some(u => u.id === userId);
        const isRegularUser = this._users.some(u => u.id === userId);
        if (isServiceUser) {
            this._expandedServiceUserId = userId;
            this._expandedUserId = null;
            return;
        }
        if (isRegularUser) {
            this._expandedUserId = userId;
            this._expandedServiceUserId = null;
            return;
        }
        // Users may not have loaded yet; keep the route id on both expansion targets so
        // the correct table can still auto-expand once the relevant data is available.
        this._expandedUserId = userId;
        this._expandedServiceUserId = userId;
    }

    protected disconnectSession(user: UserModel, session: UserSession) {
        this._sessionLoader = manager.rest.api.UserResource.disconnectUserSession(manager.displayRealm, session.ID)
            .then(() => showSnackbar(undefined, "userDisconnected"))
            .catch((e) => {
                showSnackbar(undefined, "userDisconnectFailed");
                console.error("Failed to disconnect user", e);
            })
            .then(() => this.getSessionLoader(user));
    }
}
