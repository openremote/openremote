import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {
    AlarmAssetLink,
    AlarmSeverity,
    AlarmSource,
    AlarmStatus,
    Asset,
    SentAlarm,
    User,
    UserQuery
} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor4} from "@openremote/core";
import {i18next} from "@openremote/or-translate"
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {getAlarmsRoute} from "../routes";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {guard} from "lit/directives/guard.js";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAssetTreeRequestSelectionEvent, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {
    OrMwcTable,
    OrMwcTableRowClickEvent,
    OrMwcTableRowSelectEvent
} from "@openremote/or-mwc-components/or-mwc-table";
import "../components/alarms/or-alarms-table";
import {OrVaadinSelect} from "@openremote/or-vaadin-components/or-vaadin-select";
import moment from "moment";
import {OrVaadinButton} from "@openremote/or-vaadin-components/or-vaadin-button";

export interface PageAlarmsConfig {
    initialFilter?: string;
    initialSeverity?: AlarmSeverity;
    assignOnly?: boolean;
}

export function pageAlarmsProvider(store: Store<AppStateKeyed>, config?: PageAlarmsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "alarms",
        routes: ["alarms", "alarms/:id"],
        pageCreator: () => {
            return new PageAlarms(store);
        }
    };
}

interface AlarmModel extends SentAlarm {
    loaded?: boolean;
    loading?: boolean;
    alarmAssetLinks?: AlarmAssetLink[];
    previousAssetLinks?: AlarmAssetLink[];
}

@customElement("page-alarms")
export class PageAlarms extends Page<AppStateKeyed> {
    static get styles() {
        // language=CSS
        return [
            css`
                :host {
                    flex: 1;
                    width: 100%;
                }

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
                    margin: 10px 0;
                    display: flex;
                    align-items: center;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    justify-content: space-between;
                }

                #title-alarm {
                    display: flex;
                    align-items: center;
                    justify-content: flex-start;
                    width: calc(100% - 52px);
                    max-width: 1000px;
                    margin: 20px auto;
                    font-size: 18px;
                    font-weight: bold;
                }

                .breadcrumb-container {
                    width: calc(100% - 52px);
                    max-width: 1000px;
                    margin: 10px 20px 0 34px;
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

                .panel {
                    width: calc(100% - 100px);
                    max-width: 1000px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: auto;
                    padding: 12px 24px 24px;
                }

                .panel-title {
                    text-transform: uppercase;
                    font-weight: bolder;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    line-height: 1em;
                    /*margin-bottom: 10px;*/
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    display: flex;
                    align-items: center;
                    min-height: 36px;
                }

                .alarm-input {
                    margin-bottom: 12px;
                }

                #status-select, #severity-select {
                    width: 180px;
                }

                #controls {
                    flex: 0;
                    display: flex;
                    flex-direction: row;
                    justify-content: space-between;
                    align-items: baseline;
                }

                .controls-left {
                    display: flex;
                    align-items: baseline;
                    width: 100%;
                }

                .controls-left > * {
                    padding-right: 20px;
                }

                h5 {
                    margin-top: 12px;
                    margin-bottom: 6px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                }

                #table-container {
                    margin-left: 20px;
                    margin-right: 20px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    flex-wrap: wrap;
                    margin: auto;
                    flex: 1 1 0;
                    gap: 16px;
                    width: 100%;
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                }

                #details-panel {
                    min-width: 66%;
                    height: max-content;
                }

                #prop-panel {
                    min-width: 32%;
                    height: fit-content;
                }

                .hidden {
                    display: none;
                    margin: 0 !important;
                }

                #title.hidden {
                    display: none;
                    margin: 0 !important;
                }
            `];
    }

    @property()
    public config?: PageAlarmsConfig;

    @property()
    public realm?: string;
    @state()
    public alarm?: AlarmModel;
    @state()
    public creationState?: {
        alarmModel: AlarmModel;
    };

    @state()
    protected _alarms: AlarmModel[] = [];
    @state()
    protected _linkedAssets: Asset[] = [];
    @state()
    protected _linkedUsers: User[] = [];
    @state()
    protected _loadedUsers: User[] = [];

    protected _loading: boolean = false;

    @state()
    public severity?: AlarmSeverity;

    @state()
    public status?: AlarmStatus;

    @state()
    public assign: boolean = false;

    @state()
    public allActive: boolean = true;

    @state()
    protected _data?: SentAlarm[];

    @state()
    protected _loadAlarmsPromise?: Promise<any>;

    @state()
    protected _saveAlarmPromise?: Promise<any>;

    @state()
    protected _loadUsersPromise?: Promise<any>;

    @query("#table")
    protected _tableElem!: HTMLDivElement;
    protected _table?: OrMwcTable;
    protected _refresh?: number;
    @state()
    protected _selectedIds?: number[];

    get statusValue() {
        const matchingOption = this._getStatusOptions().find(option =>
            option.value === this.status ||
            (this.allActive && option.value === 'allActive') ||
            (!this.status && option.value === 'all')
        );
        return matchingOption?.value;
    }

    get name(): string {
        return "alarm.alarm_plural";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
        this.realm = this.getState().app.realm;
        this._loadData();
    }

    public shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.reset();
        }
        if (changedProperties.has("alarm")) {
            this._updateRoute();
        }
        if (changedProperties.has("severity")
            || changedProperties.has("status")
            || changedProperties.has("allActive")) {
            this._data = undefined;
        }

        if (!this._data) {
            this._loadData();
        }

        return super.shouldUpdate(changedProperties);
    }

    protected responseAndStateOK(
        stateChecker: () => boolean,
        response: GenericAxiosResponse<any>,
        errorMsg: string
    ): boolean {
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

    protected async _createUpdateAlarm(alarm: AlarmModel, action: "update" | "create") {
        if (!alarm.title || alarm.title === "") {
            return;
        }

        const isUpdate = !!alarm.id;
        let assetsIds: string[];

        if (!isUpdate) {
            alarm.realm = manager.displayRealm;
            assetsIds = alarm.alarmAssetLinks.map(l => l.id.assetId)
        }

        try {
            action == "update"
                ? await manager.rest.api.AlarmResource.updateAlarm(alarm.id, alarm)
                : await manager.rest.api.AlarmResource.createAlarm(alarm, {assetIds: assetsIds}).then(async (response) => {
                    if (alarm.alarmAssetLinks.length > 0) {
                        alarm.alarmAssetLinks.forEach((link) => {
                            link.id.sentalarmId = response.data.id;
                            link.id.realm = response.data.realm;
                        })
                        await manager.rest.api.AlarmResource.setAssetLinks(alarm.alarmAssetLinks);
                    }
                });
        } catch (e) {
            if (isAxiosError(e)) {
                console.error(
                    (isUpdate ? "alarm.saveAlarmFailed" : "alarm.createAlarmFailed") + ": response = " + e.response.statusText
                );

                if (e.response.status === 400) {
                    showSnackbar(
                        undefined,
                        i18next.t(isUpdate ? "alarm.saveAlarmFailed" : "alarm.createAlarmFailed"),
                        i18next.t("dismiss")
                    );
                } else if (e.response.status === 403) {
                    showSnackbar(undefined, i18next.t("alarm.alarmAlreadyExists"));
                }
            }
            throw e; // Throw exception anyhow to handle individual cases
        } finally {
            this.reset();
            await this._loadData();
        }
    }

    protected render() {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"/>`;
        }

        const disabled = false;
        const readAlarms = manager.hasRole("read:alarms");
        const writeAlarms = manager.hasRole("write:alarms");

        const readonly = readAlarms && !writeAlarms;
        const assignOnly = !readAlarms && !writeAlarms;
        this.config = {assignOnly: assignOnly};
        return html`
            <div id="wrapper">
                <!-- Alarm Specific page -->
                <div id="title" class="${this.creationState || this.alarm ? "hidden" : ""}">
                    <div class="${this.creationState || this.alarm ? "hidden" : ""}"
                         style="display: flex; align-items: center;">
                        <or-icon icon="bell-outline" style="padding: 0 10px 0 4px;"></or-icon>
                        <span style="font-weight: bold;">${this.alarm != undefined ? this.alarm.title : html`
                            <or-translate value="alarm.alarm_plural"/>`}</span>
                    </div>
                    <div>
                        <div id="controls">
                            <!-- Actions when alarm is selected -->
                            <div class="${this.creationState || this.alarm || assignOnly || readonly ? "hidden" : "panel-title"}"
                                 style="justify-content: flex-end; margin-right: 12px;">
                                <or-vaadin-button theme="icon" class="${this._selectedIds?.length > 0 ? "" : "hidden"}"
                                                  @click=${() => this._deleteAlarms()}>
                                    <or-icon icon="delete"></or-icon>
                                </or-vaadin-button>
                            </div>
                            
                            <!-- Filters -->
                            <div class="${this.creationState || this.alarm ? "hidden" : "controls-left"}">
                                <or-vaadin-checkbox id="assign-check" ?disabled=${disabled} ?checked=${this.assign}
                                                    @change=${(ev: Event) => this._onAssignCheckChanged((ev.currentTarget as HTMLInputElement).checked)}>
                                    <or-translate slot="label" value="alarm.assignedToMe"></or-translate>
                                </or-vaadin-checkbox>
                                <or-vaadin-select id="severity-select" ?disabled=${disabled}
                                                  .items=${this._getSeverityOptions()} value=${this.severity ?? 'all'}
                                                  @change=${(ev: Event) => this._onSeverityChanged((ev.currentTarget as OrVaadinSelect).value as AlarmSeverity | 'all')}>
                                    <or-translate slot="label" value="alarm.severity"></or-translate>
                                </or-vaadin-select>
                                <or-vaadin-select id="status-select" ?disabled=${disabled}
                                                  .items=${this._getStatusOptions()} value=${this.statusValue}>
                                                  @change=${(ev: Event) => this._onStatusChanged((ev.currentTarget as OrVaadinSelect).value as AlarmStatus | 'all' | 'allActive')}>
                                    <or-translate slot="label" value="alarm.status"></or-translate>
                                </or-vaadin-select>

                            </div>
                            <div class="${this.creationState || this.alarm || assignOnly || readonly ? "hidden" : "panel-title"}"
                                 style="justify-content: flex-end;">
                                <or-vaadin-button @click=${() => (this.creationState = {alarmModel: this.getNewAlarmModel()})}>
                                    <or-icon slot="prefix" icon="plus"></or-icon>
                                    <or-translate value="alarm.add"></or-translate>
                                </or-vaadin-button>
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.alarm || this.creationState, () => {
                    const alarm: AlarmModel = this.alarm ?? this.creationState.alarmModel;
                    return this.getSingleAlarmView(alarm, readonly);
                }, () => {
                    return html`
                        <!-- List of Alarms page -->
                        <div id="container">
                            ${disabled ? html`
                                <div id="msg">
                                    <or-translate value="loading"/>
                                </div>
                            ` : html`
                                <div id="table-container">
                                    ${when(this._data, () => guard([this._data], () => this.getAlarmsTable(writeAlarms)))}
                                </div>
                            `}
                        </div>
                    `;
                })}
            </div>
        `;
    }

    protected getNewAlarmModel(): AlarmModel {
        return {
            alarmAssetLinks: [],
            previousAssetLinks: [],
            loaded: true,
            source: AlarmSource.MANUAL,
            severity: AlarmSeverity.MEDIUM,
            status: AlarmStatus.OPEN,
            realm: manager.displayRealm
        };
    }

    protected async loadAlarm(alarm: AlarmModel) {
        if (alarm.loaded) {
            return;
        }
        const stateChecker = () => {
            return this.getState().app.realm === this.realm && this.isConnected;
        };

        const alarmAssetLinksResponse = await manager.rest.api.AlarmResource.getAssetLinks(alarm.id, {realm: alarm.realm});

        if (!this.responseAndStateOK(stateChecker, alarmAssetLinksResponse, i18next.t("loadFailedUsers"))) {
            return;
        }

        if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            const usersResponse = await manager.rest.api.UserResource.query({
                realmPredicate: {name: manager.displayRealm},
            } as UserQuery);

            if (!this.responseAndStateOK(stateChecker, usersResponse, i18next.t("loadFailedUsers"))) {
                return;
            }

            this._loadedUsers = usersResponse.data.filter((user) => user.enabled && !user.serviceAccount);
        }
        this.alarm.alarmAssetLinks = alarmAssetLinksResponse.data;

        this.alarm.loaded = true;
        this.alarm.loading = false;

        // Update the dom
        this.requestUpdate();
    }

    protected getAlarmsTable(writeAlarms: boolean) {
        return html`
            <or-alarms-table .alarms=${this._data} .readonly=${!writeAlarms}
                             @or-mwc-table-row-select="${(e: OrMwcTableRowSelectEvent) => this._onRowSelect(e)}"
                             @or-mwc-table-row-click="${(e: OrMwcTableRowClickEvent) => this._onRowClick(e)}"
            ></or-alarms-table>
        `;
    }

    protected async _loadData() {
        if (this._loading || (!manager.hasRole("read:alarms") && !manager.hasRole("write:alarms"))) {
            return;
        }

        this._loading = true;
        const response = await manager.rest.api.AlarmResource.getAlarms({realm: manager.displayRealm});
        if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            const usersResponse = await manager.rest.api.UserResource.query({
                realmPredicate: {name: manager.displayRealm},
            } as UserQuery);

            if (usersResponse.status === 200) {
                this._loadedUsers = usersResponse.data.filter((user) => user.enabled && !user.serviceAccount);
            }
        }

        if (response.status === 200) {
            // Get page count
            this._data = response.data;
            this._data = this._data.filter((e) => e.realm === manager.displayRealm);
            this._selectedIds = [];

            if (this.config?.assignOnly) {
                const userResponse = await manager.rest.api.UserResource.getCurrent();
                if (userResponse.status === 200) {
                    this._data = this._data.filter((e) => e.assigneeId === userResponse.data.id);
                }
            }
            if (this.severity) {
                this._data = this._data.filter((e) => e.severity === this.severity);
            }
            if (this.status) {
                this._data = this._data.filter((e) => e.status === this.status);
            }
            if (!this.status && this.allActive) {
                this._data = this._data.filter((e) => e.status !== AlarmStatus.RESOLVED && e.status !== AlarmStatus.CLOSED);
            }
            if (this.assign) {
                await this._onAssignCheckChanged(this.assign);
            }
        }
        this._loading = false;
    }

    private _deleteAlarm(alarm: SentAlarm) {
        showOkCancelDialog(i18next.t("alarm.deleteAlarm"), i18next.t("alarm.deleteAlarmConfirm", {alarm: alarm.title}), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this.doDelete(alarm.id);
                }
            });
    }

    private _deleteAlarms() {
        showOkCancelDialog(i18next.t("alarm.deleteAlarms"), i18next.t("alarm.deleteAlarmsConfirm", {count: this._selectedIds.length}), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this.doMultipleDelete(this._selectedIds);
                }
            });
    }

    private doDelete(alarmId: any) {
        manager.rest.api.AlarmResource.removeAlarm(alarmId).then(response => {
            this.reset();
        })
    }

    private doMultipleDelete(alarmIds: any[]) {
        manager.rest.api.AlarmResource.removeAlarms(alarmIds).then(response => {
            this.reset();
        })
    }

    protected getSingleAlarmView(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
        return html`
            ${when(
                    alarm.loaded,
                    () => {
                        return this.getSingleAlarmTemplate(alarm, readonly);
                    },
                    () => {
                        const getTemplate = async () => {
                            await this.loadAlarm(alarm);
                            return this.getSingleAlarmTemplate(alarm, readonly);
                        };
                        const content: Promise<TemplateResult> = getTemplate();
                        return html`${until(content, html`
                            <or-translate value="loading"/>`)} `;
                    }
            )}
        `;
    }

    protected getSingleAlarmTemplate(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
        const write = manager.hasRole("write:alarms");
        return html`
            <!-- Breadcrumb on top of the page-->
            <div style="margin: 0 auto auto auto;
                        width: calc(100% - 100px);
                        max-width: 1000px;">
                <div class="breadcrumb-container">
                    <span class="breadcrumb-clickable" @click="${() => this.reset()}"
                    ><or-translate value="alarm.alarm_plural"/></span>
                    <or-icon class="breadcrumb-arrow" icon="chevron-right"></or-icon>
                    <span style="margin-left: 2px;"
                    >${this.alarm != undefined ? this.alarm.id : html`
                        <or-translate value="alarm.creatingAlarm"/>`}</span>
                </div>
                <div id="title-alarm">
                    <or-icon icon="bell-outline" style="margin-right: 10px; margin-left: 14px;"></or-icon>
                    <span>${this.alarm != undefined ? this.alarm.title : html`
                        <or-translate value="alarm.alarm_plural"/>`}</span>
                </div>

                <div class="panel" style="margin-top: 0">
                    <div class="row">
                        <div class="column" id="details-panel">
                            <h5>
                                <or-translate value="details"/>
                            </h5>
                            <!-- alarm details -->
                            <or-vaadin-text-field class="alarm-input" ?readonly=${!write} required value=${alarm.title}
                                                  @change=${(ev: Event) => {
                                                      alarm.title = (ev.currentTarget as HTMLInputElement).value;
                                                      this.onAlarmChanged(ev);
                                                  }}>
                                <or-translate slot="label" value="alarm.title"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-area class="alarm-input" ?readonly=${!write} value=${alarm.content} min-rows="12"
                                                 @change=${(ev: Event) => {
                                                     alarm.content = (ev.currentTarget as HTMLInputElement).value;
                                                     this.onAlarmChanged(ev);
                                                 }}>
                                <or-translate slot="label" value="alarm.content"></or-translate>
                            </or-vaadin-text-area>
                        </div>
                        <div class="column" id="prop-panel">
                            <h5>
                                <or-translate value="properties"/>
                            </h5>
                            <or-vaadin-text-field class="alarm-input hidden" disabled
                                                  value=${moment(alarm.createdOn).format("LLL")}>
                                <or-translate slot="label" value="createdOn"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-field class=${this.creationState ? "hidden" : "alarm-input"} disabled
                                                  value=${moment(alarm.lastModified).format("LLL")}>
                                <or-translate slot="label" value="alarm.lastModified"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-field class="alarm-input" ?disabled=${!write || this.creationState || this.alarm}
                                                  value=${this._getSourceText()}>
                                <or-translate slot="label" value="alarm.source"></or-translate>
                            </or-vaadin-text-field>
                            
                            <or-vaadin-select class="alarm-input" ?readonly=${!write}
                                              .items=${this._getAddSeverityOptions()} value=${alarm.severity}
                                              @change=${(ev: Event) => {
                                                  alarm.severity = (ev.currentTarget as OrVaadinSelect).value as AlarmSeverity;
                                                  this.onAlarmChanged(ev);
                                              }}>
                                <or-translate slot="label" value="alarm.severity"></or-translate>
                            </or-vaadin-select>
                            
                            <or-vaadin-select class="alarm-input"
                                              ?readonly=${!manager.hasRole("read:users") && !manager.hasRole("read:admin")}
                                              .items=${this._getUsers()} value=${alarm.assigneeId}
                                              @change=${(ev: Event) => {
                                                  console.debug(this._getUsers());
                                                  alarm.assigneeId = (ev.currentTarget as OrVaadinSelect).value;
                                                  this.onAlarmChanged(ev);
                                              }}>
                                <or-translate slot="label" value="alarm.assignee"></or-translate>
                            </or-vaadin-select>
                            <div>
                                <span><or-translate value="linkedAssets"></or-translate>:</span>
                                <or-vaadin-button ?disabled=${!manager.hasRole("write:alarms")} style="margin-left: 4px;"
                                                  @click=${(ev: Event) => this._openAssetSelector(ev, alarm, readonly)}>
                                    <span>${i18next.t("selectRestrictedAssets", {number: alarm.alarmAssetLinks?.length})}</span>
                                </or-vaadin-button>
                            </div>
                        </div>
                    </div>
                    <!-- Bottom controls (save/update and delete button) -->
                    ${when(!(readonly && !this._saveAlarmPromise), () => html`
                        <div class="row" style="justify-content: space-between; margin-top: 10px;">
                            ${when((manager.hasRole("write:alarms")), () => html`
                                <or-vaadin-button class="alarm-input" ?disabled=${!manager.hasRole("write:alarms") || !alarm.id}
                                                  @click=${() => this._deleteAlarm(this.alarm)}>
                                    <or-translate value="delete"></or-translate>
                                </or-vaadin-button>
                            `)}
                            <div>
                                <or-vaadin-button class="alarm-input" @click=${() => this.reset()}>
                                    <or-translate value="cancel"></or-translate>
                                </or-vaadin-button>
                                <or-vaadin-button id="savebtn" class="alarm-input" theme="primary" disabled
                                                  @click=${() => {
                                                      let error: { status?: number; text: string };
                                                      this._saveAlarmPromise = this._createUpdateAlarm(alarm, alarm.id ? "update" : "create")
                                                              .then(() => {
                                                                  showSnackbar(undefined, i18next.t("alarm.saveAlarmSucceeded"));
                                                                  this.reset();
                                                              }).catch((ex) => {
                                                                  if (isAxiosError(ex)) {
                                                                      error = {
                                                                          status: ex.response.status,
                                                                          text: ex.response.status === 403 ? i18next.t("alarm.alarmAlreadyExists") : i18next.t("errorOccurred"),
                                                                      };
                                                                  }
                                                              }).finally(() => {
                                                                  this._saveAlarmPromise = undefined;
                                                              });
                                                  }}>
                                    <or-translate value=${alarm.id ? "save" : "create"}></or-translate>
                                </or-vaadin-button>
                            </div>
                        </div>
                    `)}
                </div>
            </div>
            </div>`;
    }

    protected _getStatusOptions() {
        return [
            {label: i18next.t('alarm.allActive'), value: 'allActive'},
            {label: i18next.t('alarm.all'), value: 'all'}
        ].concat(this._getAddStatusOptions());
    }

    protected _getSeverityOptions() {
        return [{label: i18next.t('alarm.all'), value: 'all'}].concat(this._getAddSeverityOptions());
    }

    protected _getAddStatusOptions() {
        const statuses = [AlarmStatus.OPEN, AlarmStatus.ACKNOWLEDGED, AlarmStatus.IN_PROGRESS, AlarmStatus.RESOLVED, AlarmStatus.CLOSED];
        return statuses.map(status => ({label: i18next.t('alarm.status_' + status), value: status}));
    }

    protected _getAddSeverityOptions() {
        const severities = [AlarmSeverity.LOW, AlarmSeverity.MEDIUM, AlarmSeverity.HIGH];
        return severities.map(status => ({label: i18next.t('alarm.severity_' + status), value: status}));
    }

    protected _getSourceText(): string {
        const alarm: AlarmModel = this.alarm ?? this.creationState.alarmModel
        const alarmSource = alarm.source;
        const alarmSourceId = alarm.sourceId;
        const sourceText = i18next.t('alarm.source_' + alarmSource);

        if (alarmSourceId) {
            let sourceIdText: string;
            if ([AlarmSource.REALM_RULESET, AlarmSource.ASSET_RULESET, AlarmSource.GLOBAL_RULESET].includes(alarmSource)) {
                sourceIdText = "ID: " + alarmSourceId;
            } else if (alarmSource === AlarmSource.MANUAL) {
                sourceIdText = alarm.sourceUsername;
            }
            return sourceIdText ? sourceText + " (" + sourceIdText + ")" : sourceText;
        }
        return sourceText;
    }

    protected _onSeverityChanged(severity: AlarmSeverity | 'all') {
        console.debug(severity);
        if (severity === 'all') {
            this.severity = undefined;
            this._loadData();
            return;
        }

        this.severity = severity;
        this._data = this._data?.filter((e) => e.severity === this.severity);
    }

    protected async _onAssignCheckChanged(assign: boolean) {
        this.assign = assign;

        if (this.assign === undefined) {
            return;
        }
        const response = await manager.rest.api.UserResource.getCurrent();
        if (response.status === 200 && this.assign) {
            this._data = this._data!.filter((e) => e.assigneeId === response.data.id);
        } else if (!this.assign) {
            this._loadData();
        }
    }

    protected _onStatusChanged(status: AlarmStatus | 'all' | 'allActive') {
        if (status === 'all') {
            this.status = undefined;
            this.allActive = undefined;
            this.requestUpdate();
            return;
        }
        if (status === 'allActive') {
            this.status = undefined;
            this.allActive = true;
            this.requestUpdate();
            return;
        }
        this.allActive = undefined;
        this.status = this._getStatusOptions().filter((obj) => obj.label === status).map((obj) => obj.value)[0] as AlarmStatus;
        if (!this.status) {
            return;
        }

        this._data = this._data!.filter((e) => e.status === this.status);
    }

    protected _onRowSelect(ev: OrMwcTableRowSelectEvent) {
        const alarm = this._data[ev.detail.index];
        if (alarm) {
            if (ev.detail.state) {
                if (this._selectedIds === undefined) {
                    this._selectedIds = [alarm.id];
                } else {
                    this._selectedIds.push(alarm.id);
                    this.requestUpdate('_selectedIds');
                }
            } else {
                this._selectedIds = this._selectedIds.filter(id => id !== alarm.id);
            }
        } else {
            console.warn("Tried selecting an alarm that does not exist?")
        }
    }

    protected _onRowClick(ev: OrMwcTableRowClickEvent) {
        if (!ev.detail.index && ev.detail.index != 0) {
            return;
        }
        this.alarm = this._data[ev.detail.index] as AlarmModel;
        this.alarm.loaded = false;
        this.alarm.loading = false;
        this.alarm.alarmAssetLinks = [];
        this.loadAlarm(this.alarm);
        this.requestUpdate();
    }


    protected _getUsers() {
        let options = this._loadedUsers.filter((u) => u.username != 'manager-keycloak').map((u) => {
            return {value: u.id, label: u.username};
        });
        options.unshift({value: null, label: i18next.t("none")})
        return options;
    }

    protected _openAssetSelector(ev: Event, alarm: AlarmModel, readonly: boolean) {
        const openBtn = ev.target as HTMLElement;
        openBtn.toggleAttribute("disabled", true);
        alarm.previousAssetLinks = alarm.alarmAssetLinks ? [...alarm.alarmAssetLinks] : [];

        const onAssetSelectionChanged = (e: OrAssetTreeSelectionEvent) => {
            alarm.alarmAssetLinks = e.detail.newNodes.map(node => {
                const alarmAssetLink: AlarmAssetLink = {
                    id: {
                        sentalarmId: alarm.id,
                        realm: alarm.realm,
                        assetId: node.asset.id
                    }
                };
                return alarmAssetLink;
            })
        };

        const dialog = showDialog(
            new OrMwcDialog()
                .setHeading(i18next.t("linkedAssets"))
                .setContent(
                    html`
                        <or-asset-tree
                                id="chart-asset-tree"
                                readonly="true"
                                .selectedIds="${alarm.alarmAssetLinks?.map((al) => al.id.assetId)}"
                                .showSortBtn="${false}"
                                expandNodes
                                checkboxes
                                @or-asset-tree-request-selection="${(e: OrAssetTreeRequestSelectionEvent) => {
                                    this.creationState ? e.detail.allow = true : e.detail.allow = false;
                                }}"
                                @or-asset-tree-selection="${(e: OrAssetTreeSelectionEvent) => {
                                    if (!readonly) {
                                        onAssetSelectionChanged(e);
                                    }
                                }}"
                        ></or-asset-tree>
                    `
                )
                .setActions([
                    {
                        default: true,
                        actionName: "cancel",
                        content: i18next.t("cancel"),
                        action: () => {
                            openBtn.toggleAttribute("disabled", false);
                        },
                    },
                    {
                        actionName: "ok",
                        content: "ok",
                        action: () => {
                            openBtn.toggleAttribute("disabled", false);
                            this.onAlarmChanged(this.shadowRoot.querySelector('or-vaadin-text-field') as HTMLElement);
                            this.requestUpdate();
                        }
                    }
                ])
                .setDismissAction({
                    actionName: "cancel",
                    action: () => {
                        openBtn.toggleAttribute("disabled", false);
                    },
                })
        );
    }

    // Reset selected alarm and go back to the alarm overview
    protected reset() {
        this._selectedIds = [];
        this.alarm = undefined;
        this.creationState = undefined;
        this._data = undefined;
        this._selectedIds = undefined;
        this.assign = false;
        if (this._table) {
            this._table = undefined;
        }
        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }
    }

    public stateChanged(state: AppStateKeyed) {
        if (state.app.page == "alarms") {
            if (this.realm === undefined || this.realm == state.app.realm) {
                if (state.app.params && state.app.params.id) {
                    const parsedId = Number(state.app.params.id);
                    manager.rest.api.AlarmResource.getAlarm(parsedId).then((alarm: any) => {
                        this.alarm = alarm.data as AlarmModel;
                        this.alarm.loaded = false;
                        this.alarm.loading = false;
                        this.alarm.alarmAssetLinks = [];
                        this.loadAlarm(this.alarm);
                        this.requestUpdate();
                    }).catch((ex) => {
                        if (isAxiosError(ex)) {
                            this.reset();
                        }
                    })
                } else if (!this.creationState) {
                    this.reset();
                }
            } else {
                this.realm = state.app.realm;
                this.requestUpdate('realm');
            }
        }
    }

    protected _updateRoute(silent: boolean = false) {
        router.navigate(getAlarmsRoute(this.alarm?.id.toString()), {
            callHooks: !silent,
            callHandler: !silent,
        });
    }

    protected onAlarmChanged(e: Event | HTMLElement) {
        const formElement = e instanceof Event ? (e.target as HTMLElement).parentElement : e.parentElement;
        const saveBtn = this.shadowRoot.getElementById("savebtn") as OrVaadinButton;

        if (formElement) {
            saveBtn.disabled = Array.from(formElement.children)
                .filter((e) => e instanceof HTMLInputElement)
                .some((input) => !input.checkValidity());
        }

        if (this.alarm && !this.alarm.title) {
            saveBtn.disabled = true;
        }
    }
}
