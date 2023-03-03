import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {DefaultColor3, DefaultColor4, Util} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {SentAlarm, ClientRole, Role, UserQuery, AlarmSeverity, AlarmStatus} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {getAlarmsRoute} from "../routes";
import {when} from 'lit/directives/when.js';
import {until} from 'lit/directives/until.js';
import {OrMwcTableRowClickEvent, TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageAlarmsProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "alarms",
        routes: [
            "alarms",
            "alarms/:id"
        ],
        pageCreator: () => {
            return new PageAlarms(store);
        },
    };
}

interface AlarmModel extends SentAlarm {
    loaded?: boolean;
    loading?: boolean;
    alarmAssetLinks?: string[]; // To change data type
    alarmUserLinks?: string[]; // To change data type
}

@customElement("page-alarms")
export class PageAlarms extends Page<AppStateKeyed> {
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
                    margin: 0 auto 10px;
                    padding: 12px 24px 24px;
                }

                .panel-title {
                    text-transform: uppercase;
                    font-weight: bolder;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    line-height: 1em;
                    margin-bottom: 10px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    display: flex;
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
                    margin: 10px 0;
                    flex: 1 1 0;
                    gap: 24px;
                }
                
                .row > td[value="LOW"]{
                    color: green;
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
                    color: ${unsafeCSS(DefaultColor4)}
                }

                .breadcrumb-arrow {
                    margin: 0 5px -3px 5px;
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
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
                }
            `,
        ];
    }

    @property()
    public realm?: string;
    @property()
    public alarmId?: string;
    @property()
    public creationState?: {
        alarmModel: AlarmModel
    }

    @state()
    protected _activeAlarms: AlarmModel[] = [];
    @state()
    protected _inactiveAlarms: AlarmModel[] = [];

    protected _realmRolesFilter = (role: Role) => {
        return role.name === "admin" || (!role.composite && !["uma_authorization", "offline_access", "create-realm"].includes(role.name) && !role.name.startsWith("default-roles"))
    };

    protected _loading: boolean = false;

    @state()
    protected _loadAlarmsPromise?: Promise<any>;

    get name(): string {
        return "alarm.alarm_plural";
    }


    public shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.reset();
            this.loadAlarms();
        }
        if (changedProperties.has('alarmId')) {
            this._updateRoute();
        }
        return super.shouldUpdate(changedProperties);
    }

    public connectedCallback() {
        super.connectedCallback();
        this.loadAlarms();
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

    protected async loadAlarms(): Promise<void> {
        this._loadAlarmsPromise = this.fetchAlarms();
        this._loadAlarmsPromise.then(() => {
            this._loadAlarmsPromise = undefined;
        })
        return this._loadAlarmsPromise;
    }

    protected async fetchAlarms(): Promise<void> {
        if (!this.realm || this._loading || !this.isConnected) {
            return;
        }

        this._loading = true;

        this._activeAlarms = [];
        this._inactiveAlarms = [];

        if (!manager.authenticated || !manager.hasRole(ClientRole.READ_ALARMS)) {
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

        const realmResponse = await manager.rest.api.RealmResource.get(manager.displayRealm);

        if (!this.responseAndStateOK(stateChecker, realmResponse, i18next.t("loadFailedRoles"))) {
            return;
        }

        const usersResponse = await manager.rest.api.UserResource.query({realmPredicate: {name: manager.displayRealm}} as UserQuery);

        if (!this.responseAndStateOK(stateChecker, usersResponse, i18next.t("loadFailedUsers"))) {
            return;
        }

        // this._compositeRoles = roleResponse.data.filter(role => role.composite).sort(Util.sortByString(role => role.name));
        // this._roles = roleResponse.data.filter(role => !role.composite).sort(Util.sortByString(role => role.name));
        // this._realmRoles = (realmResponse.data.realmRoles || []).sort(Util.sortByString(role => role.name));
        // this._users = usersResponse.data.filter(user => !user.serviceAccount).sort(Util.sortByString(u => u.username));
        // this._serviceUsers = usersResponse.data.filter(user => user.serviceAccount).sort(Util.sortByString(u => u.username));
        this._loading = false;
    }

    private async _updateAlarm(alarm: AlarmModel) {
        // if (!user.username) {
        //     return;
        // }
        //
        // if (user.password === "") {
        //     // Means a validation failure shouldn't get here
        //     return;
        // }
        //
        // const isUpdate = !!user.id;
        //
        // try {
        //     const response = action == 'update'
        //         ? await manager.rest.api.UserResource.update(manager.displayRealm, user)
        //         : await manager.rest.api.UserResource.create(manager.displayRealm, user);
        //
        //     // Ensure user ID is set
        //     user.id = response.data.id;
        //
        //     if (user.password) {
        //         const credentials = {value: user.password}
        //         manager.rest.api.UserResource.resetPassword(manager.displayRealm, user.id, credentials);
        //     }
        //
        //     await this._updateRoles(user, false);
        //     await this._updateRoles(user, true);
        //     await this._updateUserAssetLinks(user);
        // } catch (e) {
        //     if (isAxiosError(e)) {
        //         console.error((isUpdate ? "save user failed" : "create user failed") + ": response = " + e.response.statusText);
        //
        //         if (e.response.status === 400) {
        //             showSnackbar(undefined, i18next.t(isUpdate ? "saveUserFailed" : "createUserFailed"), i18next.t("dismiss"));
        //         } else if (e.response.status === 403) {
        //             showSnackbar(undefined, i18next.t('userAlreadyExists'))
        //         }
        //     }
        //     throw e; // Throw exception anyhow to handle individual cases
        //
        // } finally {
        //     await this.loadUsers();
        // }
    }

    /**
     * Backend only uses name of role not the ID so although service client roles are not the same as composite roles
     * the names will match so that's ok
     */
    private async _updateRoles(realmRoles: boolean) {
        // const roles = realmRoles ? user.realmRoles.filter(role => role.assigned) : user.roles.filter(role => role.assigned);
        // const previousRoles = realmRoles ? user.previousRealmRoles : user.previousRoles;
        // const removedRoles = previousRoles.filter(previousRole => !roles.some(role => role.name === previousRole.name));
        // const addedRoles = roles.filter(role => !previousRoles.some(previousRole => previousRole.name === role.name));
        //
        // if (removedRoles.length === 0 && addedRoles.length === 0) {
        //     return;
        // }
        //
        // if (realmRoles) {
        //     await manager.rest.api.UserResource.updateUserRealmRoles(manager.displayRealm, user.id, roles);
        // } else {
        //     await manager.rest.api.UserResource.updateUserRoles(manager.displayRealm, user.id, roles);
        // }
    }

    private async _updateUserAssetLinks() {
        // if (!user.previousAssetLinks) {
        //     return;
        // }
        //
        // const removedLinks = user.previousAssetLinks.filter(assetLink => !user.userAssetLinks.some(newLink => assetLink.id.assetId === newLink.id.assetId));
        // const addedLinks = user.userAssetLinks.filter(assetLink => !user.previousAssetLinks.some(oldLink => assetLink.id.assetId === oldLink.id.assetId)).map(link => {
        //     // Ensure user ID is added as new users wouldn't have had an ID at the time the links were created in the UI
        //     link.id.userId = user.id;
        //     return link;
        // });
        //
        // if (removedLinks.length > 0) {
        //     await manager.rest.api.AssetResource.deleteUserAssetLinks(removedLinks);
        // }
        // if (addedLinks.length > 0) {
        //     await manager.rest.api.AssetResource.createUserAssetLinks(addedLinks);
        // }
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
        // manager.rest.api.UserResource.delete(manager.displayRealm, user.id).then(response => {
        //     if (user.serviceAccount) {
        //         this._serviceUsers = [...this._serviceUsers.filter(u => u.id !== user.id)];
        //         this.reset();
        //     } else {
        //         this._users = [...this._users.filter(u => u.id !== user.id)];
        //         this.reset();
        //     }
        // })
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        const readonly = !manager.hasRole(ClientRole.WRITE_ALARMS);

        // Content of User Table
        const alarmTableColumns: TableColumn[] = [
            {title: i18next.t('createdOn')},
            {title: i18next.t('alarm.title')},
            {title: i18next.t('alarm.content')},
            {title: i18next.t('alarm.severity')},
            {title: i18next.t('status')}
        ];
        const activeAlarmTableRows: TableRow[] = this._activeAlarms.map((alarm) => {
            return {
                content: [alarm.createdOn.toString(), alarm.title, alarm.content, alarm.severity, alarm.status],
                clickable: true
            }
        });

        // const activeAlarmTableRows: TableRow[] = [({content: ["", "Test", "Test", AlarmSeverity.LOW, AlarmStatus.ACTIVE], clickable: true} as TableRow)];

        const inactiveAlarmTableRows: TableRow[] = this._inactiveAlarms.map((alarm) => {
            return {
                content: [alarm.createdOn.toString(), alarm.title, alarm.content, alarm.severity, alarm.status],
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

        const mergedAlarmList: AlarmModel[] = [...this._activeAlarms, ...this._inactiveAlarms];
        const index: number | undefined = (this.alarmId ? mergedAlarmList.findIndex((alarm) => alarm.id == this.alarmId) : undefined);

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="alert-outline"></or-icon>
                    <span>${this.alarmId && index != undefined ? mergedAlarmList[index]?.title : i18next.t('alarm.alarm_plural')}</span>
                </div>

                <!-- Alarm Specific page -->
                ${when((this.alarmId && index != undefined) || this.creationState, () => html`
                    ${when(mergedAlarmList[index] != undefined || this.creationState, () => {
            const alarm: AlarmModel = (index != undefined ? mergedAlarmList[index] : this.creationState.alarmModel);
            return html`
                            <div id="content" class="panel">
                                <p class="panel-title">
                                    ${i18next.t('alarm')}
                                    ${i18next.t('settings')}</p>
                                ${this.getSingleAlarmView(alarm, (readonly || this._loadAlarmsPromise != undefined))}
                            </div>
                        `;
        })}

                    <!-- List of Alarms page -->
                `, () => html`
                    <div id="content" class="panel">
                        <div class="panel-title" style="justify-content: space-between;">
                            <p>${i18next.t("alarm.active_plural")}</p>
                        </div>
                        ${until(this.getAlarmsTable(alarmTableColumns, activeAlarmTableRows, tableConfig, (ev) => {
            this.alarmId = this._activeAlarms[ev.detail.index].id;
        }), html`${i18next.t('loading')}`)}
                    </div>

                    <div id="content" class="panel">
                        <div class="panel-title" style="justify-content: space-between;">
                            <p>${i18next.t("alarm.inactive_plural")}</p>
                        </div>
                        ${until(this.getAlarmsTable(alarmTableColumns, inactiveAlarmTableRows, tableConfig, (ev) => {
            this.alarmId = this._inactiveAlarms[ev.detail.index].id;
        }), html`${i18next.t('loading')}`)}
                    </div>
                `)}
            </div>
        `;
    }

    protected async getAlarmsTable(columns: TemplateResult | TableColumn[] | string[], rows: TemplateResult | TableRow[] | string[][], config: any, onRowClick: (event: OrMwcTableRowClickEvent) => void): Promise<TemplateResult> {
        if (this._loadAlarmsPromise) {
            await this._loadAlarmsPromise;
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

    protected getNewAlarmModel(serviceAccount: boolean): AlarmModel {
        return {
            alarmAssetLinks: [],
            alarmUserLinks: []
        }
    }

    protected async loadAlarm(alarm: AlarmModel) {
        // if (user.roles || user.realmRoles) {
        //     return;
        // }

        // Load users assigned roles
        // const userRolesResponse = await (manager.rest.api.UserResource.getUserRoles(manager.displayRealm, user.id));
        // if (!this.responseAndStateOK(() => true, userRolesResponse, i18next.t("loadFailedUserInfo"))) {
        //     return;
        // }
        //
        // const userRealmRolesResponse = await manager.rest.api.UserResource.getUserRealmRoles(manager.displayRealm, user.id);
        // if (!this.responseAndStateOK(() => true, userRolesResponse, i18next.t("loadFailedUserInfo"))) {
        //     return;
        // }
        //
        // const userAssetLinksResponse = await manager.rest.api.AssetResource.getUserAssetLinks({
        //     realm: manager.displayRealm,
        //     userId: user.id
        // });
        // if (!this.responseAndStateOK(() => true, userAssetLinksResponse, i18next.t("loadFailedUserInfo"))) {
        //     return;
        // }
        //
        // user.roles = userRolesResponse.data.filter(r => r.assigned);
        // user.realmRoles = userRealmRolesResponse.data.filter(r => r.assigned);
        // this._realmRoles = [...userRealmRolesResponse.data];
        // user.previousRealmRoles = [...user.realmRoles];
        // user.previousRoles = [...user.roles];
        // user.userAssetLinks = userAssetLinksResponse.data;
        // user.loaded = true;
        // user.loading = false;

        // Update the dom
        this.requestUpdate();
    }

    protected _openAssetSelector(ev: MouseEvent, user: AlarmModel, readonly: boolean) {
        // const openBtn = ev.target as OrMwcInput;
        // openBtn.disabled = true;
        // user.previousAssetLinks = [...user.userAssetLinks];
        //
        // const onAssetSelectionChanged = (e: OrAssetTreeSelectionEvent) => {
        //     user.userAssetLinks = e.detail.newNodes.map(node => {
        //         const userAssetLink: UserAssetLink = {
        //             id: {
        //                 userId: user.id,
        //                 realm: user.realm,
        //                 assetId: node.asset.id
        //             }
        //         };
        //         return userAssetLink;
        //     })
        // };
        //
        // const dialog = showDialog(new OrMwcDialog()
        //     .setHeading(i18next.t("linkedAssets"))
        //     .setContent(html`
        //         <or-asset-tree
        //                 id="chart-asset-tree" readonly .selectedIds="${user.userAssetLinks.map(ual => ual.id.assetId)}"
        //                 .showSortBtn="${false}" expandNodes checkboxes
        //                 @or-asset-tree-request-selection="${(e: OrAssetTreeRequestSelectionEvent) => {
        //         if (readonly) {
        //             e.detail.allow = false;
        //         }
        //     }}"
        //                 @or-asset-tree-selection="${(e: OrAssetTreeSelectionEvent) => {
        //         if (!readonly) {
        //             onAssetSelectionChanged(e);
        //         }
        //     }}"></or-asset-tree>
        //     `)
        //     .setActions([
        //         {
        //             default: true,
        //             actionName: "cancel",
        //             content: i18next.t("cancel"),
        //             action: () => {
        //                 user.userAssetLinks = user.previousAssetLinks;
        //                 user.previousAssetLinks = undefined;
        //                 openBtn.disabled = false;
        //             }
        //         },
        //         {
        //             actionName: "ok",
        //             content: i18next.t("ok"),
        //             action: () => {
        //                 openBtn.disabled = false;
        //                 this.requestUpdate();
        //             }
        //         }
        //     ])
        //     .setDismissAction({
        //         actionName: "cancel",
        //         action: () => {
        //             user.userAssetLinks = user.previousAssetLinks;
        //             user.previousAssetLinks = undefined;
        //             openBtn.disabled = false;
        //         }
        //     }));
    }

    protected onUserChanged(e: OrInputChangedEvent | OrMwcInput, suffix: string) {
        // Don't have form-associated custom element support in lit at time of writing which would be the way to go here
        // const formElement = e instanceof OrInputChangedEvent ? (e.target as HTMLElement).parentElement : e.parentElement;
        // const saveBtn = this.shadowRoot.getElementById("savebtn-" + suffix) as OrMwcInput;
        //
        // if (formElement) {
        //     const saveDisabled = Array.from(formElement.children).filter(e => e instanceof OrMwcInput).some(input => !(input as OrMwcInput).valid);
        //     saveBtn.disabled = saveDisabled;
        // }
    }

    protected _onPasswordChanged(user: AlarmModel, suffix: string) {
        // const passwordComponent = this.shadowRoot.getElementById("password-" + suffix) as OrMwcInput;
        // const repeatPasswordComponent = this.shadowRoot.getElementById("repeatPassword-" + suffix) as OrMwcInput;
        //
        // if (repeatPasswordComponent.value !== passwordComponent.value) {
        //     const error = i18next.t("passwordMismatch");
        //     repeatPasswordComponent.setCustomValidity(error);
        //     user.password = "";
        // } else {
        //     repeatPasswordComponent.setCustomValidity(undefined);
        //     user.password = passwordComponent.value;
        // }
    }

    protected async _regenerateSecret(ev: OrInputChangedEvent, user: AlarmModel, secretInputId: string) {
        // const btnElem = ev.currentTarget as OrMwcInput;
        // const secretElem = this.shadowRoot.getElementById(secretInputId) as OrMwcInput;
        // if (!btnElem || !secretElem) {
        //     return;
        // }
        // btnElem.disabled = true;
        // secretElem.disabled = true;
        // const resetResponse = await manager.rest.api.UserResource.resetSecret(manager.displayRealm, user.id);
        // if (resetResponse.data) {
        //     secretElem.value = resetResponse.data;
        // }
        // btnElem.disabled = false;
        // secretElem.disabled = false;
    }

    protected _updateUserSelectedRoles(user: AlarmModel, suffix: string) {
        // const roleCheckboxes = [...((this.shadowRoot.getElementById("role-list-" + suffix) as HTMLDivElement).children as any)] as OrMwcInput[];
        // const implicitRoleNames = this.getImplicitUserRoles(user);
        // roleCheckboxes.forEach((checkbox) => {
        //     const roleName = checkbox.label;
        //     const r = this._roles.find(role => roleName === role.name);
        //     checkbox.disabled = !!implicitRoleNames.find(name => r.name === name);
        //     checkbox.value = !!user.roles.find(userRole => userRole.name === r.name) || implicitRoleNames.some(implicitRoleName => implicitRoleName === r.name);
        // });
    }

    protected getSingleAlarmView(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
        return html`
            ${when((alarm.loaded), () => {
            return this.getSingleAlarmTemplate(alarm, readonly);
        }, () => {
            const getTemplate = async () => {
                await this.loadAlarm(alarm);
                return this.getSingleAlarmTemplate(alarm, readonly);
            }
            const content: Promise<TemplateResult> = getTemplate();
            return html`
                    ${until(content, html`${i18next.t('loading')}`)}
                `;
        })}
        `;
    }

    protected getSingleAlarmTemplate(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
        return html`

        `;
    }

    // Reset selected alarm and go back to the alarm overview
    protected reset() {
        this.alarmId = undefined;
    }

    public stateChanged(state: AppStateKeyed) {
        if (state.app.page == 'alarms') {
            this.realm = state.app.realm;
            this.alarmId = (state.app.params && state.app.params.id) ? state.app.params.id : undefined;
        }
    }

    protected _updateRoute(silent: boolean = false) {
        router.navigate(getAlarmsRoute(this.alarmId), {
            callHooks: !silent,
            callHandler: !silent
        });
    }
}
