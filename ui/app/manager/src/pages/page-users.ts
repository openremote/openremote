import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {DefaultColor3, DefaultColor4, OPENREMOTE_CLIENT_ID, Util} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {ClientRole, Credential, Role, User, UserAssetLink, UserQuery, UserSession} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrAssetTreeRequestSelectionEvent, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {getNewUserRoute, getUsersRoute} from "../routes";
import {when} from 'lit/directives/when.js';
import {until} from 'lit/directives/until.js';
import {map} from 'lit/directives/map.js';
import {OrMwcTableRowClickEvent, TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageUsersProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "users",
        routes: [
            "users",
            "users/:id",
            "users/new/:type"
        ],
        pageCreator: () => {
            return new PageUsers(store);
        },
    };
}

interface UserModel extends User {
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
                    flex: 0;
                    width: 100%;
                    box-sizing: border-box;
                    max-width: 1360px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: 0 auto 16px;
                    padding: 12px 24px 24px;
                    display: flex;
                    flex-direction: column;
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

                .breadcrumb-container {
                    padding: 0 20px;
                    width: calc(100% - 40px);
                    max-width: 1360px;
                    margin: 12px auto 0;
                    display: flex;
                    align-items: center;
                }

                .breadcrumb-clickable {
                    cursor: pointer;
                    color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
                }

                .breadcrumb-arrow {
                    margin: 0 5px -3px 5px;
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
                }
                
                #session-table {
                    --or-mwc-table-column-width-3: 0;
                }
                
                .button-row {
                    display: flex !important;
                    flex-direction: row !important;
                    margin-bottom: 0;
                    justify-content: space-between;
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
                    }
                }
            `,
        ];
    }

    @property()
    public realm?: string;
    @property()
    public userId?: string;
    @property()
    public creationState?: {
        userModel: UserModel
    }

    @state()
    protected _users: UserModel[] = [];
    @state()
    protected _serviceUsers: UserModel[] = [];
    @state()
    protected _userFilter = this.getDefaultUserFilter(false);
    @state()
    protected _serviceUserFilter = this.getDefaultUserFilter(true);
    @state()
    protected _passwordPolicy: string[] = []
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

    get name(): string {
        return "user_plural";
    }


    public shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.reset();
            this.loadData();
        }
        if (changedProperties.has('userId')) {
            this._sessionLoader = undefined; // Reset the MQTT sessions view
            this._updateRoute();
        } else if (changedProperties.has('creationState')) {
            this._updateNewUserRoute();
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
        if(!this._loadDataPromise) {
            this._loadDataPromise = this.fetchUsers();
            this._loadDataPromise.finally(() => {
                this._loadDataPromise = undefined;
            })
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

        // After async op check that the response still matches current state and that the component is still loaded in the UI
        const stateChecker = () => {
            return this.getState().app.realm === this.realm && this.isConnected;
        }

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
        // New service users with the 'gateway-' prefix are not allowed
        if(user.serviceAccount && user.username.startsWith('gateway-')) {
            showSnackbar(undefined, "noGatewayUsername", "dismiss")
            return false;
        }

        if (user.password === "") {
            // Means a validation failure shouldn't get here
            return false;
        }

        const isUpdate = !!user.id;

        try {
            const response = action === 'update'
                ? await manager.rest.api.UserResource.update(manager.displayRealm, user)
                : await manager.rest.api.UserResource.create(manager.displayRealm, user);

            // Ensure user ID is set
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
                    showSnackbar(undefined, "userAlreadyExists")
                }
            }
            result = false;
            throw e; // Throw exception anyhow to handle individual cases

        } finally {
            await this.loadData();

            // After updating the user, reset the password if it has changed.
            // This is handled asynchronously, so it will not 'wait' before the request has succeeded.
            if (user.password) {
                const credentials = {value: user.password} as Credential;
                manager.rest.api.UserResource.updatePassword(manager.displayRealm, user.id, credentials).catch(e => {
                    if(isAxiosError(e) && e.response.status !== 404) {
                        showSnackbar(undefined, "savePasswordFailed");
                    }
                });
            }

            return result;
        }
    }

    /**
     * Backend only uses name of role not the ID so although service client roles are not the same as composite roles
     * the names will match so that's ok
     */
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
        showOkCancelDialog(i18next.t("deleteUser"), i18next.t("deleteUserConfirm", { userName: user.username }), i18next.t("delete"))
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
                this.reset();
            } else {
                this._users = [...this._users.filter(u => u.id !== user.id)];
                this.reset();
            }
        })
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }
        // Apply filter (such as search input)
        const users = this._userFilter(this._users);
        const serviceUsers = this._serviceUserFilter(this._serviceUsers);

        const compositeRoleOptions: string[] = this._compositeRoles.map(cr => cr.name);
        const realmRoleOptions: [string, string][] = this._realmRoles.map(r => [r, i18next.t("realmRole." + r, Util.camelCaseToSentenceCase(r.replace("_", " ").replace("-", " ")))]);
        const readonly = !manager.hasRole(ClientRole.WRITE_ADMIN);

        // Content of User Table
        const userTableColumns: TableColumn[] = [
            {title: i18next.t('username')},
            {title: i18next.t('firstName')},
            {title: i18next.t('surname')},
            {title: i18next.t('email'), hideMobile: true},
            {title: i18next.t('status')}
        ];
        const userTableRows: TableRow[] = users.map((user) => {
            return {
                content: [user.username, user.firstName, user.lastName, user.email, user.enabled ? i18next.t('enabled') : i18next.t('disabled')] as string[],
                clickable: true
            }
        });

        // Content of Service user Table
        const serviceUserTableColumns: TableColumn[] = [
            {title: i18next.t('username')},
            {title: i18next.t('status')}
        ];
        const serviceUserTableRows: TableRow[] = serviceUsers.map((user) => {
            return {
                content: [user.username, user.enabled ? i18next.t('enabled') : i18next.t('disabled')] as string[],
                clickable: true
            }
        })

        // Configuration
        const tableConfig = {
            columnFilter: [],
            stickyFirstColumn: false,
            pagination: {
                enable: true
            }
        }

        const mergedUserList: UserModel[] = [...users, ...serviceUsers];
        const index: number | undefined = (this.userId ? mergedUserList.findIndex((user) => user.id === this.userId) : undefined);

        return html`
            <div id="wrapper">

                <!-- Breadcrumb on top of the page-->
                ${when((this.userId && index !== undefined) || this.creationState, () => html`
                    <div class="breadcrumb-container">
                        <span class="breadcrumb-clickable"
                              @click="${() => this.reset()}">${i18next.t("user_plural")}</span>
                        <or-icon class="breadcrumb-arrow" icon="chevron-right"></or-icon>
                        <span style="margin-left: 2px;">${index !== undefined ? mergedUserList[index]?.username : (this.creationState.userModel.serviceAccount ? i18next.t('creating_serviceUser') : i18next.t('creating_regularUser'))}</span>
                    </div>
                `)}

                <div id="title">
                    <or-icon icon="account-group"></or-icon>
                    <span>${this.userId && index !== undefined ? mergedUserList[index]?.username : i18next.t('user_plural')}</span>
                </div>

                <!-- User Specific page -->
                ${when((this.userId && index !== undefined) || this.creationState, () => html`
                    ${when(mergedUserList[index] !== undefined || this.creationState, () => {
                        const user: UserModel = (index !== undefined ? mergedUserList[index] : this.creationState.userModel);
                        const showMqttSessions = user.serviceAccount && this.userId;
                        return html`
                            <div id="content" class="panel">
                                <p class="panel-title">
                                    ${user.serviceAccount ? i18next.t('serviceUser') : i18next.t('user')}
                                    ${i18next.t('settings')}</p>
                                ${this.getSingleUserView(user, compositeRoleOptions, realmRoleOptions, ("user" + index), (readonly || this._saveUserPromise != undefined))}
                            </div>
                            
                            ${when(showMqttSessions, () => this.getMQTTSessionTemplate(user))}
                        `;
                    })}

                    <!-- List of Users page -->
                `, () => html`
                    <div id="content" class="panel">
                        <div class="panel-title" style="justify-content: space-between;">
                            <p>${i18next.t("regularUser_plural")}</p>
                            <div style="display: flex; align-items: center; gap: 12px;">
                                <or-mwc-input type="${InputType.TEXT}" placeholder="${i18next.t('search')}" 
                                              style="margin: 0; text-transform: none;" iconTrailing="magnify" compact outlined
                                              @input="${(ev) => this.onRegularUserSearch(ev)}"
                                ></or-mwc-input>
                                <or-mwc-input style="margin: 0;" type="${InputType.BUTTON}" icon="plus" label="${i18next.t('add')} ${i18next.t("user")}"
                                              @or-mwc-input-changed="${() => this.creationState = {userModel: this.getNewUserModel(false)}}"
                                ></or-mwc-input>
                            </div>
                        </div>
                        ${until(this.getUsersTable(userTableColumns, userTableRows, tableConfig, (ev) => {
                            this.userId = users[ev.detail.index].id;
                        }), html`${i18next.t('loading')}`)}
                    </div>

                    <div id="content" class="panel">
                        <div class="panel-title" style="justify-content: space-between;">
                            <p>${i18next.t("serviceUser_plural")}</p>
                            <div style="display: flex; align-items: center; gap: 12px;">
                                <or-mwc-input style="margin: 0; text-transform: none;" type="${InputType.TEXT}" iconTrailing="magnify" placeholder="${i18next.t('search')}" compact outlined
                                              @input="${(ev) => this.onServiceUserSearch(ev)}"
                                ></or-mwc-input>
                                <or-mwc-input style="margin: 0;" type="${InputType.BUTTON}" icon="plus" label="${i18next.t('add')} ${i18next.t("user")}"
                                              @or-mwc-input-changed="${() => this.creationState = {userModel: this.getNewUserModel(true)}}"
                                ></or-mwc-input>
                            </div>
                        </div>
                        ${until(this.getUsersTable(serviceUserTableColumns, serviceUserTableRows, tableConfig, (ev) => {
                            this.userId = serviceUsers[ev.detail.index].id;
                        }), html`${i18next.t('loading')}`)}
                    </div>
                `)}
            </div>
        `;
    }

    protected async getUsersTable(columns: TemplateResult | TableColumn[] | string[], rows: TemplateResult | TableRow[] | string[][], config: any, onRowClick: (event: OrMwcTableRowClickEvent) => void): Promise<TemplateResult> {
        if (this._loadDataPromise) {
            await this._loadDataPromise;
        }
        return html`
            <or-mwc-table .columns="${columns instanceof Array ? columns : undefined}"
                          .columnsTemplate="${!(columns instanceof Array) ? columns : undefined}"
                          .rows="${rows instanceof Array ? rows : undefined}"
                          .rowsTemplate="${!(rows instanceof Array) ? rows : undefined}"
                          .config="${config}"
                          @or-mwc-table-row-click="${rows instanceof Array ? onRowClick : undefined}"
            ></or-mwc-table>
        `
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
        }
    }

    protected getDefaultUserFilter(serviceUser: boolean): (users: UserModel[]) => UserModel[] {
        return (users) => users;
    }

    protected onRegularUserSearch(ev: InputEvent) {
        const value = (ev.target as OrMwcInput).nativeValue?.toLowerCase();
        if(!value) {
            this._userFilter = this.getDefaultUserFilter(false);
        } else {
            this._userFilter = (users) => {
                return users.filter(u =>
                    (u.username as string)?.toLowerCase().includes(value) ||
                    (u.firstName as string)?.toLowerCase().includes(value) ||
                    (u.lastName as string)?.toLowerCase().includes(value) ||
                    (u.email as string)?.toLowerCase().includes(value)
                );
            }
        }
    }

    protected onServiceUserSearch(ev: InputEvent) {
        const value = (ev.target as OrMwcInput).nativeValue?.toLowerCase();
        if(!value) {
            this._serviceUserFilter = this.getDefaultUserFilter(true);
        } else {
            this._serviceUserFilter = (users) => {
                return users.filter(u =>
                    (u.username as string)?.includes(value)
                );
            }
        }
    }

    protected async loadUser(user: UserModel) {
        if (user.roles || user.realmRoles) {
            return;
        }

        // Load users assigned roles
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

        // Update the dom
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
            })
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
        // Don't have form-associated custom element support in lit at time of writing which would be the way to go here
        const validateArray = this.shadowRoot.querySelectorAll(".validate");
        const saveBtn = this.shadowRoot.getElementById("savebtn-" + suffix) as OrMwcInput;
        const saveDisabled = Array.from(validateArray).filter(e => e instanceof OrMwcInput).some(input => !(input as OrMwcInput).valid);
        saveBtn.disabled = saveDisabled;
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

    protected getSingleUserView(user: UserModel, compositeRoleOptions: string[], realmRoleOptions: [string, string][], suffix: string, readonly: boolean = true): TemplateResult {
        return html`
            ${when((user.loaded || (user.roles && user.realmRoles)), () => {
                return this.getSingleUserTemplate(user, compositeRoleOptions, realmRoleOptions, suffix, readonly);
            }, () => {
                const getTemplate = async () => {
                    await this.loadUser(user);
                    return this.getSingleUserTemplate(user, compositeRoleOptions, realmRoleOptions, suffix, readonly);
                }
                const content: Promise<TemplateResult> = getTemplate();
                return html`
                    ${until(content, html`${i18next.t('loading')}`)}
                `;
            })}
        `;
    }

    protected getMQTTSessionTemplate(user: UserModel): TemplateResult {

        if (!this._sessionLoader) {
            this._sessionLoader = this.getSessionLoader(user);
        }

        return html`
            <div id="content" class="panel">
                <p class="panel-title">${i18next.t('mqttSessions')}</p>
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
            return [session.remoteAddress, new Date(session.startTimeMillis), html`<or-mwc-input .type="${InputType.BUTTON}" label="disconnect" @or-mwc-input-changed="${() => {this.disconnectSession(user, session)}}"></or-mwc-input>`]
        });
        if (rows.length < 1){
            return html`<or-mwc-table .rows="${[['This user has no active MQTT sessions',null]]}" .config="${{stickyFirstColumn:false}}" .columns="${cols}"></or-mwc-table>`;
        }

        return html`<or-mwc-table id="session-table" .rows="${rows}" .config="${{stickyFirstColumn:false}}" .columns="${cols}"></or-mwc-table>`;
    }

    protected getSingleUserTemplate(user: UserModel, compositeRoleOptions: string[], realmRoleOptions: [string, string][], suffix: string, readonly: boolean = true): TemplateResult {
        const isServiceUser = user.serviceAccount;
        const isSameUser = user.username === manager.username;
        const isGatewayServiceUser = isServiceUser && user.username?.startsWith("gateway-");
        const implicitRoleNames = user.loaded ? this.getImplicitUserRoles(user) : [];
        return html`
            <div class="row">
                <div class="column">
                    <h5>${i18next.t("details")}</h5>
                    <!-- user details -->
                    <or-mwc-input id="new-username-${suffix}" ?readonly="${!!user.id || readonly}" .disabled="${!!user.id || (!isServiceUser && this._registrationEmailAsUsername)}"
                                  class = "validate"
                                  .label="${i18next.t("username")}"
                                  .type="${InputType.TEXT}" minLength="3" maxLength="255" 
                                  ?required="${isServiceUser || !this._registrationEmailAsUsername}"
                                  pattern="[A-Za-z0-9_+@\\.\\-ßçʊÇʊ]+"
                                  .value="${user.username}" autocomplete="false"
                                  .validationMessage="${i18next.t("invalidUsername")}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.username = e.detail.value;
                                      this.onUserChanged(suffix)
                                  }}"></or-mwc-input>
                    <!-- if identity provider is set to use email as username, make it required -->
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
                                      if(this._registrationEmailAsUsername) {
                                          user.username = e.detail.value;
                                      }
                                      user.email = e.detail.value;
                                      this.onUserChanged(suffix);
                                      this.requestUpdate(); // in case of username update, we trigger a state change
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-firstName" ?readonly="${readonly}"
                                  class="${isServiceUser ? "hidden" : "validate"}"
                                  .label="${i18next.t("firstName")}"
                                  .type="${InputType.TEXT}" minLength="1"
                                  .value="${user.firstName}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.firstName = e.detail.value;
                                      this.onUserChanged(suffix)
                                  }}"></or-mwc-input>
                    <or-mwc-input id="new-surname" ?readonly="${readonly}"
                                  class="${isServiceUser ? "hidden" : "validate"}"
                                  .label="${i18next.t("surname")}"
                                  .type="${InputType.TEXT}" minLength="1"
                                  .value="${user.lastName}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.lastName = e.detail.value;
                                      this.onUserChanged(suffix)
                                  }}"></or-mwc-input>

                    <!-- password -->
                    <h5>${i18next.t("password")}</h5>
                    ${isServiceUser ? html`
                        ${when(user.secret, () => html`
                            <or-mwc-input id="new-password-${suffix}" readonly
                                          class = "validate"
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
                                      class = "validate"
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
                    <!-- enabled -->
                    <or-mwc-input ?readonly="${readonly}"
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

                    <!-- realm roles -->
                    <or-mwc-input
                            ?readonly="${readonly}"
                            ?disabled="${isSameUser || isGatewayServiceUser}"
                            class = "validate"
                            .value="${user.realmRoles}"
                            .type="${InputType.SELECT}" multiple
                            .options="${realmRoleOptions}"
                            .label="${i18next.t("realm_role_plural")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                this.onUserChanged(suffix);
                                user.realmRoles = e.detail.value as string[];
                            }}"></or-mwc-input>

                    <!-- composite client roles -->
                    <or-mwc-input
                            ?readonly="${readonly}"
                            ?disabled="${isSameUser || isGatewayServiceUser}"
                            class = "validate"
                            .value="${user.roles && user.roles.length > 0 ? user.roles.filter(r => this._compositeRoles.some(cr => cr.name === r)) : undefined}"
                            .type="${InputType.SELECT}" multiple
                            .options="${compositeRoleOptions}"
                            .label="${i18next.t("manager_role_plural")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                user.roles = e.detail.value as string[];
                                this._updateUserSelectedRoles(user, suffix);
                                this.onUserChanged(suffix);
                            }}"></or-mwc-input>

                    <!-- roles -->
                    <div style="display:flex;flex-wrap:wrap;margin-bottom: 20px;"
                         id="role-list-${suffix}">
                        ${this._roles.map(r => {
                            return html`
                                <or-mwc-input
                                        ?readonly="${readonly}"
                                        ?disabled="${implicitRoleNames.find(name => r.name === name) || isGatewayServiceUser}"
                                        class = "validate"
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
                            `
                        })}
                    </div>

                    <!-- Asset-User links -->
                    <div>
                        <span>${i18next.t("linkedAssets")}:</span>
                        <or-mwc-input outlined ?disabled="${readonly || isGatewayServiceUser}" style="margin-left: 4px;"
                                      .type="${InputType.BUTTON}"
                                      .label="${i18next.t("selectRestrictedAssets", {number: user.userAssetLinks.length})}"
                                      @or-mwc-input-changed="${(ev: MouseEvent) => this._openAssetSelector(ev, user, readonly, suffix)}"></or-mwc-input>
                    </div>
                </div>
            </div>
            <!-- Bottom controls (save/update and delete button) -->
            ${when(!(readonly && !this._saveUserPromise), () => html`
                <div class="row button-row">

                    ${when((!isSameUser && !isGatewayServiceUser && user.id), () => html`
                        <or-mwc-input style="margin: 0;" outlined ?disabled="${readonly}"
                                      .label="${i18next.t("delete")}"
                                      .type="${InputType.BUTTON}"
                                      @click="${() => this._deleteUser(user)}"
                        ></or-mwc-input>
                    `)}
                    <div style="display: flex; align-items: center; gap: 16px; margin: 0 0 0 auto;">
                        <!-- Button disabled until an input has input, and by that a valid check is done -->
                        <or-mwc-input id="savebtn-${suffix}" style="margin: 0;" raised disabled
                                      .label="${i18next.t(user.id ? "save" : "create")}"
                                      .type="${InputType.BUTTON}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          let error: { status?: number, text: string };
                                          this._saveUserPromise = this._createUpdateUser(user, user.id ? 'update' : 'create').then((result) => {
                                              // Return to the users page on successful user create/update
                                              if (result) {                                              
                                                  showSnackbar(undefined, "saveUserSucceeded");
                                                  this.reset();
                                              }
                                          }).catch((ex) => {
                                              console.error(ex);
                                              if (isAxiosError(ex)) {
                                                  error = {
                                                      status: ex.response.status,
                                                      text: (ex.response.status == 403 ? i18next.t('userAlreadyExists') : i18next.t('errorOccurred'))
                                                  }
                                              }
                                          }).finally(() => {
                                              this._saveUserPromise = undefined;
                                              if (error) {
                                                  this.updateComplete.then(() => {
                                                      showSnackbar(undefined, error.text);
                                                      if (error.status === 403) {
                                                          const elem = this.shadowRoot.getElementById('username-' + suffix) as OrMwcInput;
                                                          elem.setCustomValidity(error.text);
                                                          (elem.shadowRoot.getElementById("elem") as HTMLInputElement).reportValidity(); // manually reporting was required since we're not editing the username at all.
                                                          this.onUserChanged(suffix); // onUserChanged to trigger validation of all fields again.
                                                      }
                                                  })
                                              }
                                          })
                                      }}">
                        </or-mwc-input>
                    </div>
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

        // Minimum / maximum length warning
        if(policies.includes("length") && policies.includes("maxLength")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length" .options="${{ 0: policyMap.get("length"), 1: policyMap.get("maxLength") }}"></or-translate>`);
        } else if(policies.includes("length")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length-too-short" .options="${{ 0: policyMap.get("length") }}"></or-translate>`);
        } else if(policies.includes("maxLength")) {
            policyTexts.push(html`<or-translate value="password-policy-invalid-length-too-long" .options="${{ 0: policyMap.get("maxLength") }}"></or-translate>`);
        }

        // Special characters
        if(policies.includes("specialChars")) {
            const value = policyMap.get("specialChars");
            const translation = value == "1" ? "password-policy-special-chars-single" : "password-policy-special-chars";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{ 0: value }}"></or-translate>`);
        }

        // Digits/numbers
        if(policies.includes("digits")) {
            const value = policyMap.get("digits");
            const translation = value == "1" ? "password-policy-digits-single" : "password-policy-digits";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{ 0: value }}"></or-translate>`);
        }

        // Uppercase / lowercase letters
        if(policies.includes("upperCase")) {
            const value = policyMap.get("upperCase");
            const translation = value == "1" ? "password-policy-uppercase-single" : "password-policy-uppercase";
            policyTexts.push(html`<or-translate value="${translation}" .options="${{ 0: value }}"></or-translate>`);
        }

        // Warn for recently used passwords
        if(policies.includes("passwordHistory")) {
            policyTexts.push(html`<or-translate value="password-policy-recently-used"></or-translate>`);
        }

        // Cannot be username and/or email
        if(policies.includes("notUsername") && policies.includes("notEmail")) {
            policyTexts.push(html`<or-translate value="password-policy-not-email-username"></or-translate>`);
        } else if(policies.includes("notUsername")) {
            policyTexts.push(html`<or-translate value="password-policy-not-username"></or-translate>`);
        } else if(policies.includes("notEmail")) {
            policyTexts.push(html`<or-translate value="password-policy-not-email"></or-translate>`);
        }

        return html`
            <ul>
                ${map(policyTexts, text => html`<li>${text}</li>`)}
            </ul>
        `;
    }

    // Reset selected user or creation page, and go back to the list
    protected reset() {
        this.userId = undefined;
        this.creationState = undefined;
        this._userFilter = this.getDefaultUserFilter(false);
        this._serviceUserFilter = this.getDefaultUserFilter(true);
    }

    public stateChanged(state: AppStateKeyed) {
        if (state.app.page == 'users') { // it seems that the check is necessary here
            this.realm = state.app.realm;
            this.userId = (state.app.params && state.app.params.id) ? state.app.params.id : undefined;
            this.creationState = (state.app.params?.type ? {userModel: this.getNewUserModel(state.app.params.type == 'serviceuser')} : undefined);
        }
    }

    protected _updateRoute(silent: boolean = false) {
        router.navigate(getUsersRoute(this.userId), {
            callHooks: !silent,
            callHandler: !silent
        });
    }

    protected _updateNewUserRoute(silent: boolean = false) {
        router.navigate(getNewUserRoute(this.creationState?.userModel.serviceAccount), {
            callHooks: !silent,
            callHandler: !silent
        });
    }

    protected disconnectSession(user: UserModel, session: UserSession) {
        this._sessionLoader = manager.rest.api.UserResource.disconnectUserSession(manager.displayRealm, session.ID)
            .then(() => showSnackbar(undefined, "userDisconnected"))
            .catch((e) => {
                showSnackbar(undefined, "userDisconnectFailed");
                console.error("Failed to disconnect user", e);
            })
            .then(() =>this.getSessionLoader(user));
    }
}
