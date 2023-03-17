import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-alarm-viewer";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {
    AlarmAssetLink,
    AlarmSeverity,
    AlarmStatus,
    AlarmUserLink,
    Asset,
    SentAlarm,
    User,
    UserQuery
} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor4} from "@openremote/core";
import i18next from "i18next";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {getAlarmsRoute} from "../routes";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAssetTreeRequestSelectionEvent} from "@openremote/or-asset-tree";
import {OrMwcTable, OrMwcTableRowClickEvent, TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {MDCDataTable} from "@material/data-table";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export interface PageAlarmsConfig {
    initialFilter?: string;
    initialSeverity?: AlarmSeverity;
    hideControls?: boolean;
    assignOnly?: boolean;
}

export function pageAlarmsProvider(store: Store<AppStateKeyed>, config?: PageAlarmsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "alarms",
        routes: ["alarms", "alarms/:id"],
        pageCreator: () => {
            const page = new PageAlarms(store);
            if (config) page.config = config;
            return page;
        }
    };
}

interface AlarmModel extends SentAlarm {
    loaded?: boolean;
    loading?: boolean;
    alarmAssetLinks?: AlarmAssetLink[];
    alarmUserLinks?: AlarmUserLink[];
}

@customElement("page-alarms")
export class PageAlarms extends Page<AppStateKeyed> {
    static get styles() {
        // language=CSS
        return [
            unsafeCSS(tableStyle),
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
                    padding: 0;
                    font-size: 18px;
                    font-weight: bold;
                    width: 100%;
                    margin: 10px 0 0 0;
                    display: flex;
                    align-items: center;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                #title or-icon {
                    margin-right: 10px;
                    margin-left: 14px;
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
                    margin-bottom: 10px;
                }

                #status-select, #severity-select {
                    width: 180px;
                    padding: 0 10px;
                }

                #controls {
                    flex: 0;
                    display: flex;
                    /*flex-wrap: wrap;*/
                    flex-direction: row;
                    justify-content: space-between;
                    /*margin: var(--internal-or-log-viewer-controls-margin);*/
                    padding: 0 10px 0px 10px;
                }

                .controls-left {
                    display: flex;
                    align-items: center;
                    width: 100%;
                }

                #controls > * {
                    /*margin-top: 0px;*/
                }
                
                h5 {
                    margin-top: 12px;
                    margin-bottom: 5px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                    margin-right: 2px;
                    margin-left: 20px;
                }
                
                #table-container {
                    margin-left: 20px;
                    margin-right: 20px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    margin: auto;
                    flex: 1 1 0;
                    gap: 12px;
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
                    min-width: 33%;
                    height: fit-content; 
                }

                .hidden {
                    display: none;
                    margin: 0 !important;
                }
                
                .breadcrumb-container {
                    padding: 0 20px;
                    width: calc(100% - 40px);
                    max-width: 1360px;
                    margin-top: 10px;
                    display: flex;
                    align-items: center;
                }

                .breadcrumb-clickable {
                    cursor: pointer;
                    color: ${unsafeCSS(DefaultColor4)};
                }

                .breadcrumb-arrow {
                    margin: 0 5px -3px 5px;
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
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
    protected _assign: boolean = false;
    protected _userId?: string;

    @state()
    public severity?: AlarmSeverity;

    @state()
    public status?: AlarmStatus;

    @state()
    public hide: boolean = false;

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
    protected _table?: MDCDataTable;
    protected _refresh?: number;
    protected _pageCount?: number;
    protected _currentPage: number = 1;

    @state()
    protected _selected?: TableRow[];

    get name(): string {
        return "alarm.alarm_plural";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        this._loadData().then(() => {
            setTimeout(() => {
                const elem = this.shadowRoot.querySelector('or-mwc-table') as OrMwcTable;
                elem.paginationSize = 100;
                const checkboxes = elem?.shadowRoot?.querySelectorAll('[id*="checkbox"]');
                if(checkboxes.length > 0){
                    checkboxes.forEach((select, index) => {
                        const inner = select.shadowRoot?.querySelector("input[type=checkbox]");
                        inner?.addEventListener('change', function() {
                            this._selected = elem.selectedRows;
                            console.log(this._selected);
                        }.bind(this));

                    })
                }
            }, 1000);
        });
        super.connectedCallback();
    }

    public disconnectedCallback() {
        this.reset();
        super.disconnectedCallback();
    }

    protected updated(changedProperties: Map<string, any>) {
        setTimeout(() => {
            if(!this.alarm && !this.creationState && this.shadowRoot) {
                const elem = this.shadowRoot.querySelector('or-mwc-table') as OrMwcTable;
                const rows = elem?.shadowRoot?.querySelectorAll('tr');
                if(rows){
                    rows.forEach(row => {
                        const spans = row.querySelectorAll('td span');
                        if(spans){
                            spans.forEach((span, columnIndex) => {
                                span = span as HTMLElement;
                                if (columnIndex == 4 || columnIndex == 3) {
                                    span.parentElement.style.width = '185px';
                                    span.parentElement.style.maxWidth = '185px';
                                    span.parentElement.style.position = 'sticky';
                                    span.parentElement.style.right = '0';
                                }
                                switch (span.textContent) {
                                    case 'LOW':
                                        if(columnIndex == 0){
                                            span.innerHTML = '<or-icon style="color: green;" icon="numeric-3-box"></or-icon>';
                                            span.parentElement.style.width = '1%';
                                            span.parentElement.style.padding = '2px';
                                        }
                                        break;
                                    case 'MEDIUM':
                                        if(columnIndex == 0){
                                            span.innerHTML = '<or-icon style="color: orange;" icon="numeric-2-box"></or-icon>';
                                            span.parentElement.style.width = '1%';
                                            span.parentElement.style.padding = '2px';
                                        }
                                        break;
                                    case 'HIGH':
                                        if(columnIndex == 0){
                                            span.innerHTML = '<or-icon style="color: red;" icon="numeric-1-box"></or-icon>';
                                            span.parentElement.style.width = '1%';
                                            span.parentElement.style.padding = '2px';
                                        }
                                        break;
                                    case 'Open':
                                        if(columnIndex == 2){
                                            span.innerHTML = '<span style="color: white;' +
                                                'padding: 4px;' +
                                                'background-color: mediumblue;' +
                                                'border-radius: 5px;">Open</span>';
                                            span.parentElement.style.width = '125px';
                                        }
                                        break;
                                    case 'Acknowledged':
                                        if(columnIndex == 2){
                                            span.innerHTML = '<span style="border: 1px mediumblue solid;' +
                                                'padding: 4px;' +
                                                'border-radius: 5px;">Acknowledged</span>';
                                            span.parentElement.style.width = '125px';
                                        }
                                        break;
                                    case 'In_progress':
                                        if(columnIndex == 2){
                                            span.innerHTML = '<span style="color: white;' +
                                                'padding: 4px;' +
                                                'background-color: green;' +
                                                'border-radius: 5px;">In progress</span>';
                                            span.parentElement.style.width = '125px';
                                        }
                                        break;
                                    case 'Closed':
                                        if(columnIndex == 2){
                                            span.innerHTML = '<span style="border: 1px grey solid;' +
                                                'padding: 4px;' +
                                                'border-radius: 5px;' +
                                                'color: grey;">Closed</span>';
                                            span.parentElement.style.width = '125px';
                                        }
                                        break;
                                }
                            });
                        }
                    });
                }
            }
        }, );
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
            || changedProperties.has("filter")
            || changedProperties.has("sort")
            || changedProperties.has("hide")
            || changedProperties.has("allActive")) {
            this._pageCount = undefined;
            this._currentPage = 1;
            this._data = undefined;
        }

        if (!this._data) {
            this._loadData().then();
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
        if (!alarm.title || !alarm.content) {
            return;
        }

        if (alarm.content === "" || alarm.title === "") {
            // Means a validation failure shouldn't get here
            return;
        }

        const isUpdate = !!alarm.id;
        if (!isUpdate) {
            alarm.realm = manager.getRealm();
        }

        try {
            action == "update"
                ? await manager.rest.api.AlarmResource.updateAlarm(alarm.id, alarm)
                : await manager.rest.api.AlarmResource.createAlarm(alarm);
        } catch (e) {
            if (isAxiosError(e)) {
                console.error(
                    (isUpdate ? "save alarm failed" : "create alarm failed") + ": response = " + e.response.statusText
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
            await this._loadData();
            this.reset();
        }
    }



    protected render() {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate> `;
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

                <div id="title" class="${this.creationState || this.alarm ? "hidden" : ""}" style="justify-content: space-between; margin-top: 10px; margin-bottom: 10px">
                    <div class="${this.creationState || this.alarm ? "hidden" : ""}">
                        <or-icon icon="bell-outline" style="margin-left: 20px"></or-icon>
                        <span> ${this.alarm != undefined ? this.alarm.title : i18next.t("alarm.alarm_plural")} </span>
                    </div>
                    <div>
                        <div id="controls">
                            <div class="${this.creationState || this.alarm ? "hidden" : "controls-left"}">
                                <or-mwc-input .type="${InputType.CHECKBOX}" id="assign-check"
                                              ?disabled="${disabled}" .label="${i18next.t("alarm.assignedToMe")}"
                                              @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this._onAssignCheckChanged(evt.detail.value)}"
                                              .value="${this.assign}"></or-mwc-input>
                                <or-mwc-input .type="${InputType.SELECT}" id="severity-select" comfortable
                                              ?disabled="${disabled}" .label="${i18next.t("alarm.severity")}"
                                              @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this._onSeverityChanged(evt.detail.value)}"
                                              .value="${this._getSeverityOptions().filter((obj) => obj.value === this.severity || !this.status ? obj.value === 'All' : '').map((obj) => obj.label)[0]}"
                                              .options="${this._getSeverityOptions().map( s => s.label)}"></or-mwc-input>
                                <or-mwc-input .type="${InputType.SELECT}" id="status-select" comfortable
                                              ?disabled="${disabled}" .label="${i18next.t("alarm.status")}"
                                              @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this._onStatusChanged(evt.detail.value)}"
                                              .value="${this._getStatusOptions().filter((obj) => obj.value === this.status || this.allActive ? obj.value === 'All-active' : '' || !this.status ? obj.value === 'All' : '').map((obj) => obj.label)[0]}"
                                              .options="${this._getStatusOptions().map( s => s.label)}"></or-mwc-input>

                            </div>
                            <div class="${this.creationState || this.alarm || assignOnly || readonly ? "hidden" : "panel-title"}"
                                 style="justify-content: flex-end;">
                                <or-mwc-input
                                        raised
                                        style="padding: 0px 10px; margin: 0;"
                                        type="${InputType.BUTTON}"
                                        icon="plus"
                                        label="${i18next.t("add")} ${i18next.t("alarm.")}"
                                        @or-mwc-input-changed="${() => (this.creationState = {alarmModel: this.getNewAlarmModel()})}"
                                ></or-mwc-input>
                            </div>
                            <div class="${this.creationState || this.alarm || assignOnly || readonly ? "hidden" : "panel-title"}"
                                 style="justify-content: flex-end;">
                                <or-mwc-input
                                        outlined
                                        class="${this._selected?.length > 0 ? "" : "hidden"}"
                                        style="margin: 0;"
                                        type="${InputType.BUTTON}"
                                        icon="delete"
                                        @or-mwc-input-changed="${() => (this._deleteAlarms())}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                ${when(this.alarm || this.creationState, () => {
                            const alarm: AlarmModel = this.alarm != undefined ? this.alarm : this.creationState.alarmModel;
                            return html`
<!--                                <div id="content" class="panel">-->
                                 <!--   <p class="panel-title">${i18next.t("alarm.")} ${i18next.t("settings")}</p> -->
                                    ${this.getSingleAlarmView(alarm, readonly)}
<!--                                </div>-->
                                </div> `;
                        }, () =>
                                html`
                                    <!-- List of Alarms page -->
                                    <div id="container">
                                        ${disabled ? html`
                                                    <div id="msg">${i18next.t("loading")}</div>` :
                                                html`
                                                    <div id="table-container">
                                                        ${this._data ? this.getAlarmsTable(writeAlarms) : ``}
                                                    </div>`}
                                    </div>`
                )}
                
        `;
    }

    protected getNewAlarmModel(): AlarmModel {
        return {
            alarmAssetLinks: [],
            alarmUserLinks: [],
            loaded: true,
        };
    }

    protected async loadAlarm(alarm: AlarmModel) {
        if (alarm.loaded) {
            return;
        }
        const stateChecker = () => {
            return this.getState().app.realm === this.realm && this.isConnected;
        };

        const alarmAssetLinksResponse = await manager.rest.api.AlarmResource.getAssetLinks(alarm.id, alarm.realm);
        if (!this.responseAndStateOK(stateChecker, alarmAssetLinksResponse, i18next.t("loadFailedUsers"))) {
            console.log("Failed to load alarm asset links");
            return;
        }

        if (manager.hasRole("read:admin")) {
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
        // Content of Alarm Table
        const columns: TableColumn[] = [
            {title: ''},
            {title: i18next.t('alarm.title')},
            {title: i18next.t('alarm.status')},
            {title: i18next.t('alarm.assignee')},
            {title: i18next.t('alarm.lastModified'), isSortable: true}
        ];

        const rows: TableRow[] = this._data!.map((alarm) => {
            return {
                content: [alarm.severity!, alarm.title, alarm.status!.charAt(0) + alarm.status!.slice(1).toLowerCase(), alarm.assigneeUsername, new Date(alarm.lastModified!).toLocaleString()],
                clickable: true
            } as TableRow
        });

        // Configuration
        const config = {
            columnFilter: [],
            stickyFirstColumn: false,
            pagination: {
                enable: true
            },
            multiSelect: writeAlarms
        }

        return html`
            <or-mwc-table .columns="${columns instanceof Array ? columns : undefined}"
                          .columnsTemplate="${!(columns instanceof Array) ? columns : undefined}"
                          .rows="${rows instanceof Array ? rows : undefined}"
                          .rowsTemplate="${!(rows instanceof Array) ? rows : undefined}"
                          .config="${config}"
                          @or-mwc-table-row-click="${(e: OrMwcTableRowClickEvent) => this._onRowClick(e)}"
            ></or-mwc-table>
        `
    }

    protected async _loadData() {
        if (this._loading) {
            return;
        }

        this._loading = true;
        const response = await manager.rest.api.AlarmResource.getAlarms();
        if (manager.hasRole("read:admin")) {
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
            if (manager.getRealm() != "master") {
                this._data = this._data.filter((e) => e.realm === manager.getRealm());
            }
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
            if(!this.status && this.allActive){
                this._data = this._data.filter((e) => e.status !== AlarmStatus.CLOSED);
            }
        }
        this._loading = false;
    }

    private _deleteAlarm(alarm: SentAlarm) {
        showOkCancelDialog(i18next.t("alarm.deleteAlarm"), i18next.t("alarm.deleteAlarmConfirm", { alarm: alarm.title }), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    this.doDelete(alarm);
                }
            });
    }

    private _deleteAlarms() {
        const table = this.shadowRoot!.querySelector('or-mwc-table') as OrMwcTable;
        const ids = table!.selectedRows;


        // showOkCancelDialog(i18next.t("deleteUser"), i18next.t("deleteUserConfirm", { alarm: alarms.length }), i18next.t("delete"))
        //     .then((ok) => {
        //         if (ok) {
        //             alarms.forEach((alarm) => this.doDelete(alarm));
        //         }
        //     });
    }

    private doDelete(alarm: SentAlarm) {
        manager.rest.api.AlarmResource.removeAlarm(alarm.id).then(response => {
            this._data = [...this._data.filter(u => u.id !== alarm.id)];
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
                        return html` ${until(content, html`${i18next.t("loading")}`)} `;
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
                    >${i18next.t("alarm.alarm_plural")}</span>
                    <or-icon class="breadcrumb-arrow" icon="chevron-right"></or-icon>
                    <span style="margin-left: 2px;"
                    >${this.alarm != undefined ? this.alarm.id : i18next.t("alarm.creatingAlarm")}</span>
                </div>
                <div style="justify-content: flex-start; margin-top: 10px; margin-bottom: 10px">
                    <div style="font-size: 18px; font-weight: bold;">
                        <or-icon icon="bell-outline" style="margin-left: 20px"></or-icon>
                        <span> ${this.alarm != undefined ? this.alarm.title : i18next.t("alarm.alarm_plural")} </span>
                    </div>
                </div>
                       
                <div class="panel" style="margin-top: 0">
                    <div class="row">
                        <div class="column" id="details-panel">
                            <h5>${i18next.t("details").toUpperCase()}</h5>
                            <!-- alarm details -->
                            <or-mwc-input class="alarm-input" ?disabled="${!write}"
                                      .label="${i18next.t("alarm.title")}"
                                      .type="${InputType.TEXT}"
                                      .value="${alarm.title}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        alarm.title = e.detail.value;
                                        this.onAlarmChanged(e);
                                      }}"
                            ></or-mwc-input>
                            <or-mwc-input class="alarm-input" ?disabled="${!write}"
                                      .label="${i18next.t("alarm.content")}"
                                      .type="${InputType.TEXTAREA}"
                                      .value="${alarm.content}"
                                      rows="11"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        alarm.content = e.detail.value;
                                        this.onAlarmChanged(e);
                                      }}"
                            ></or-mwc-input>
                        </div>
                        <div class="column" id="prop-panel">
                            <h5>${i18next.t("properties").toUpperCase()}</h5>
                            <or-mwc-input class="alarm-input" ?disabled="${true}"
                                          ?comfortable=${!this.creationState}
                                  .label="${i18next.t("createdOn")}"
                                  .type="${InputType.DATETIME}"
                                  .value="${new Date(alarm.createdOn)}"
                            ></or-mwc-input>
                            <or-mwc-input class="alarm-input" ?disabled="${true}"
                                          ?comfortable=${!this.creationState}
                                  .label="${i18next.t("alarm.lastModified")}"
                                  .type="${InputType.DATETIME}"
                                  .value="${new Date(alarm.lastModified)}"
                            ></or-mwc-input>
                            <or-mwc-input class=${this.creationState ? "hidden" : "alarm-input"} ?disabled="${true}"
                                          comfortable
                                      .label="${i18next.t("alarm.source")}"
                                      .type="${InputType.TEXT}"
                                      .value="${alarm.source}"
                            ></or-mwc-input>
                            <or-mwc-input class="alarm-input" ?disabled="${!write}"
                                  .label="${i18next.t("alarm.severity")}"
                                  .type="${InputType.SELECT}"
                                  .options="${this._getAddSeverityOptions().map( s => s.label)}"
                                  .value="${this._getAddSeverityOptions().filter((obj) => obj.value === alarm.severity).map((obj) => obj.label)[0]}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        alarm.severity = this._getAddSeverityOptions().filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0];
                                        this.onAlarmChanged(e);
                                  }}"
                            ></or-mwc-input>
                            <or-mwc-input class="alarm-input" ?disabled="${!write}"
                                  .label="${i18next.t("alarm.status")}"
                                  .type="${InputType.SELECT}"
                                  .options="${this._getAddStatusOptions().map( s => s.label)}"
                                  .value="${this._getAddStatusOptions().filter((obj) => obj.value === alarm.status).map((obj) => obj.label)[0]}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        alarm.status = this._getAddStatusOptions().filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0];
                                        this.onAlarmChanged(e);
                                  }}"
                            ></or-mwc-input>
                            <or-mwc-input class="alarm-input" ?disabled="${!manager.hasRole("read:admin")}"
                                  .label="${i18next.t("alarm.assignee")}"
                                  .type="${InputType.SELECT}"
                                  .options="${this._getUsers().map((obj) => obj.label)}"
                                  .value="${this._getUsers().filter((obj) => obj.label === alarm.assigneeUsername).map((obj) => obj.label)[0]}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        alarm.assigneeId = this._getUsers().filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0];
                                        this.onAlarmChanged(e);
                                  }}"
                            ></or-mwc-input>
                            <div class="${this.creationState ? "hidden" : ""}">
                                <span style="margin: 0px auto 10px;">${i18next.t("linkedAssets")}:</span>
                                <or-mwc-input outlined ?disabled="${!manager.hasRole("write:alarms")}" style="margin-left: 4px;"
                                      .type="${InputType.BUTTON}"
                                      .label="${i18next.t("selectRestrictedAssets", {
                                            number: alarm.alarmAssetLinks?.length,
                                      })}"
                                      @or-mwc-input-changed="${(ev: MouseEvent) =>
                                            this._openAssetSelector(ev, alarm, readonly)}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                    <!-- Bottom controls (save/update and delete button) -->
                    ${when(!(readonly && !this._saveAlarmPromise), () => html`
                    <div class="row" style="justify-content: space-between;">
                        ${when((manager.hasRole("write:alarms")), () => html`
                        <or-mwc-input class="alarm-input" style="margin: 0;" outlined ?disabled="${!manager.hasRole("write:admin")}"
                                  .label="${i18next.t("delete")}"
                                  .type="${InputType.BUTTON}"
                                  @click="${() => this._deleteAlarm(this.alarm)}"
                        ></or-mwc-input>
                        `)}
                        <div>
<!--                            <span style="padding-right: 6px; font-weight: bold">Created on ${new Date(alarm.createdOn).toLocaleString()}</span>-->
                        <or-mwc-input id="savebtn"
                                  style="margin: 0;"
                                  raised class="alarm-input"
                                  ?disabled="${readonly}"
                                  .label="${i18next.t(alarm.id ? "save" : "create")}"
                                  .type="${InputType.BUTTON}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                        let error: { status?: number; text: string };
                        this._saveAlarmPromise = this._createUpdateAlarm(alarm, alarm.id ? "update" : "create")
                                .then(() => {
                                    showSnackbar(undefined, i18next.t("alarm.saveAlarmSucceeded"));
                                    this.reset();
                                }).catch((ex) => {
                                    if (isAxiosError(ex)) {
                                        error = {
                                            status: ex.response.status,
                                            text:
                                                    ex.response.status == 403
                                                            ? i18next.t("alarm.alarmAlreadyExists")
                                                            : i18next.t("errorOccurred"),};
                                    }
                                }).finally(() => {
                                    this._saveAlarmPromise = undefined;
                                });
                    }}"
                        ></or-mwc-input>
                        </div>
                    </div>`)}
                </div>
            </div>
        </div>`;
    }

    protected _getStatusOptions() {
        return [{label: 'All active', value: 'All-active'}, {label: 'All', value: 'All'}, {label: 'Open', value: AlarmStatus.OPEN}, {label: 'Acknowledged', value: AlarmStatus.ACKNOWLEDGED}, {label: 'In progress', value: AlarmStatus.IN_PROGRESS}, {label: 'Closed', value: AlarmStatus.CLOSED}];
    }

    protected _getSeverityOptions() {
        return [{label: 'All', value: 'All'}, {label: 'Low', value: AlarmSeverity.LOW}, {label: 'Medium', value: AlarmSeverity.MEDIUM}, {label: 'High', value: AlarmSeverity.HIGH}];
    }

    protected _getAddStatusOptions() {
        return [{label: 'Open', value: AlarmStatus.OPEN}, {label: 'Acknowledged', value: AlarmStatus.ACKNOWLEDGED}, {label: 'In progress', value: AlarmStatus.IN_PROGRESS}, {label: 'Closed', value: AlarmStatus.CLOSED}];
    }

    protected _getAddSeverityOptions() {
        return [{label: 'Low', value: AlarmSeverity.LOW}, {label: 'Medium', value: AlarmSeverity.MEDIUM}, {label: 'High', value: AlarmSeverity.HIGH}];
    }


    protected _onSeverityChanged(severity: any) {
        if(severity == 'All'){
            this.severity = undefined;
            this._loadData();
            return;
        }

        this.severity = this._getSeverityOptions().filter((obj) => obj.label === severity).map((obj) => obj.value)[0] as AlarmSeverity;

        if (!this.severity) {
            return;
        }
        this._data = this._data!.filter((e) => e.severity === this.severity);
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

    protected _onStatusChanged(status: any) {
        if(status == 'All'){
            this.status = undefined;
            this.allActive = undefined;
            this.requestUpdate();
            return;
        }

        if(status == 'All active'){
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

    protected _onRowClick(ev: OrMwcTableRowClickEvent) {
        if (!ev.detail.index && ev.detail.index != 0) {
            return;
        }
        this.alarm = this._data[ev.detail.index] as AlarmModel;
        this.alarm.loaded = false;
        this.alarm.loading = false;
        this.alarm.alarmAssetLinks = [];
        this.alarm.alarmUserLinks = [];
        this.loadAlarm(this.alarm);
        this.requestUpdate();
    }

    protected _onAddClick() {
        this.creationState = {alarmModel: this.getNewAlarmModel()};
        this.requestUpdate();
    }

    protected _onSelectClick() {
        const elem = this.shadowRoot?.querySelector('or-mwc-table');
        if(elem){
            this._selected = (elem as OrMwcTable).selectedRows as TableRow[];
        }
    }

    protected _getUsers() {
        return this._loadedUsers.map((u) => {
            return {value: u.id, label: u.username};
        });
    }

    protected _assignClick() {
        this._assign = !this._assign;
        this.requestUpdate();
    }

    protected async _assignUser(alarm: AlarmModel) {
        if (!this._userId || !this.alarm.id) {
            return;
        }
        try {
            await manager.rest.api.AlarmResource.assignUser(alarm.id, this._userId);
        } catch (e) {
            if (isAxiosError(e)) {
                console.error("save alarm failed" + ": response = " + e.response.statusText);

                if (e.response.status === 400) {
                    showSnackbar(undefined, i18next.t("alarm.saveAlarmFailed"), i18next.t("dismiss"));
                }
            }
            throw e; // Throw exception anyhow to handle individual cases
        } finally {
            await this.loadAlarm(alarm);
        }
    }

    protected _openAssetSelector(ev: MouseEvent, alarm: AlarmModel, readonly: boolean) {
        const openBtn = ev.target as OrMwcInput;
        openBtn.disabled = true;

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
                        ></or-asset-tree>
                    `
                )
                .setActions([
                    {
                        default: true,
                        actionName: "cancel",
                        content: i18next.t("cancel"),
                        action: () => {
                            openBtn.disabled = false;
                        },
                    },
                ])
                .setDismissAction({
                    actionName: "cancel",
                    action: () => {
                        openBtn.disabled = false;
                    },
                })
        );
    }

    // Reset selected alarm and go back to the alarm overview
    protected reset() {
        this.alarm = undefined;
        this.creationState = undefined;
        this._assign = false;
        if (this._table) {
            this._table.destroy();
            this._table = undefined;
        }
        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }
    }

    public stateChanged(state: AppStateKeyed) {
        if (state.app.page == "alarms") {
            this.realm = state.app.realm;
            if (state.app.params && state.app.params.id) {
                const parsedId = Number(state.app.params.id);
                manager.rest.api.AlarmResource.getAlarms().then((alarms) => {
                    this.alarm = alarms.data.find((alarm) => alarm.id === parsedId) as AlarmModel;
                    this.alarm.loaded = false;
                    this.alarm.loading = false;
                    this.alarm.alarmAssetLinks = [];
                    this.alarm.alarmUserLinks = [];
                    this.loadAlarm(this.alarm);
                    this.requestUpdate();
                })
            } else {
                this.alarm = undefined;
            }
        }
    }

    protected _updateRoute(silent: boolean = false) {
        router.navigate(getAlarmsRoute(this.alarm?.id.toString()), {
            callHooks: !silent,
            callHandler: !silent,
        });
    }

    protected onAlarmChanged(e: OrInputChangedEvent | OrMwcInput) {
        // Don't have form-associated custom element support in lit at time of writing which would be the way to go here
        const formElement = e instanceof OrInputChangedEvent ? (e.target as HTMLElement).parentElement : e.parentElement;
        const saveBtn = this.shadowRoot.getElementById("savebtn") as OrMwcInput;

        if (formElement) {
            const saveDisabled = Array.from(formElement.children)
                .filter((e) => e instanceof OrMwcInput)
                .some((input) => !(input as OrMwcInput).valid);
            saveBtn.disabled = saveDisabled;
        }
    }
}