import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import * as Model from "@openremote/model";
import manager, {DefaultColor2, DefaultColor3} from "@openremote/core";
import "@openremote/or-input";
import "@openremote/or-panel";
import "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {MDCDataTable} from "@material/data-table";
import moment from "moment";
import "@openremote/or-mwc-components/dist/or-mwc-menu";
import {getContentWithMenuTemplate, MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import { GenericAxiosResponse } from "axios";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const linkParser = require("parse-link-header");
const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

export interface ViewerConfig {
    allowedCategories?: Model.SyslogCategory[];
    initialCategories?: Model.SyslogCategory[];
    initialFilter?: string;
    initialLevel?: Model.SyslogLevel;
    hideCategories?: boolean;
    hideFilter?: boolean;
    hideLevel?: boolean;
}

// language=CSS
const style = css`
    :host {
        --internal-or-log-viewer-background-color: var(--or-log-viewer-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-log-viewer-text-color: var(--or-log-viewer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-log-viewer-controls-margin: var(--or-log-viewer-controls-margin, 0);
        
        display: block;                
    }
    
    :host([hidden]) {
        display: none;
    }
    
    #container {
        display: flex;
        min-width: 0;
        width: 100%;
        height: 100%;
        flex-direction: column;
    }
       
    #msg {
        height: 100%;
        width: 100%;
        justify-content: center;
        align-items: center;
        text-align: center;
    }
    
    #msg:not([hidden]) {
        display: flex;    
    }
    
    #controls {
        flex: 0;
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        margin: var(--internal-or-log-viewer-controls-margin);
        padding: 0 20px 20px 20px;
    }
    
    #controls > * {
        margin-top: 20px;
    }
    
    #time-controls {
        display: flex;
        align-items: center;
    }
    
    #live-button {
        margin-right: 40px;
    }
    
    #period-select {
        width: 90px;
    }
    
    #level-select {
        width: 120px;
    }
    
    #limit-select {
        width: 80px;
    }
    
    #log-controls, #ending-controls, #page-controls {
        display: flex;
        max-width: 100%;
        align-items: center;
    }
    
    #ending-controls {
        float: right;
        margin: 0 10px;
    }
    
    #log-controls > *, #ending-controls > *, #page-controls > * {
        padding: 0 5px;
    }
        
    #page-controls[hidden] {
        visibility: hidden;
    }
    
    #ending-date {
        min-width: 0;
    }
    
    #table-container {
        height: 100%;
        overflow: auto;
    }
    
    #table {
        width: 100%;
        margin-bottom: 10px;
    }
    
    #table > table {
        width: 100%;
        table-layout: fixed;
    }
    
    #table th, #table td {
        word-wrap: break-word;
        white-space: pre-wrap;
    }
`;

@customElement("or-log-viewer")
export class OrLogViewer extends translate(i18next)(LitElement) {

    public static DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";
    public static DEFAULT_LIMIT = 50;
    public static DEFAULT_LEVEL = Model.SyslogLevel.INFO;
    public static DEFAULT_INTERVAL = Model.DatapointInterval.HOUR;

    static get styles() {
        return [
            css`${unsafeCSS(tableStyle)}`,
            style
        ];
    }

    @property({type: String})
    public interval?: Model.DatapointInterval;

    @property({type: Number})
    public timestamp?: Date;

    @property({type: Number})
    public limit?: number;

    @property({type: Array})
    public categories?: Model.SyslogCategory[];

    @property({type: String})
    public filter?: string;

    @property({type: String})
    public level?: Model.SyslogLevel;

    @property({type: Boolean})
    public live: boolean = false;

    @property({type: Object})
    public config?: ViewerConfig;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _data?: Model.SyslogEvent[];

    @query("#table")
    protected _tableElem!: HTMLDivElement;
    protected _table?: MDCDataTable;
    protected _eventSubscriptionId?: string;
    protected _refresh?: number;
    protected _pageCount?: number;
    protected _currentPage: number = 1;
    protected _pendingCategories?: Model.SyslogCategory[];

    connectedCallback() {
        super.connectedCallback();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
        this._unsubscribeEvents();
    }

    shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("level")
            || _changedProperties.has("interval")
            || _changedProperties.has("timestamp")
            || _changedProperties.has("limit")
            || _changedProperties.has("categories")
            || _changedProperties.has("filter")
            || _changedProperties.has("live")) {
            if (!this.live) {
                this._pageCount = undefined;
                this._currentPage = 1;
                this._data = undefined;
            }
        }

        if (!this.interval) {
            this.interval = OrLogViewer.DEFAULT_INTERVAL;
        }

        if (!this.limit) {
            this.limit = OrLogViewer.DEFAULT_LIMIT;
        }

        if (!this.categories) {
            if (this.config && this.config.initialCategories) {
                this.categories = [...this.config.initialCategories];
            } else {
                this.categories = Object.keys((Model as any)["SyslogCategory"]) as Model.SyslogCategory[];
            }
        }

        if (this.filter === undefined && this.config && this.config.initialFilter) {
            this.filter = this.config.initialFilter;
        }

        if (!this.level) {
            if (this.config && this.config.initialLevel) {
                this.level = this.config.initialLevel;
            } else {
                this.level = OrLogViewer.DEFAULT_LEVEL;
            }
        }

        if (!this.live && !this.timestamp) {
            this.timestamp = new Date();
        }

        if (!this._data && !this.live) {
            this._loadData();
        }

        if (_changedProperties.has("live")) {
            if (this.live) {
                this._subscribeEvents();
            } else {
                this._unsubscribeEvents();
            }
        }

        return super.shouldUpdate(_changedProperties);
    }

    render() {

        const disabled = this._loading;
        const isLive = this.live;

        const hideCategories = this.config && this.config.hideCategories;
        const hideFilter = this.config && this.config.hideFilter;
        const hideLevel = this.config && this.config.hideLevel;

        return html`
            <div id="container">
                <div id="controls">
                    <div id="log-controls">
                        ${hideCategories ? `` : getContentWithMenuTemplate(
                            html`<or-input .type=${InputType.BUTTON} raised ?disabled="${disabled}" .label="${i18next.t("categories")}" icontrailing="chevron-down"></or-input>`,
                            this._getCategoryMenuItems(),
                            this.categories,
                            (v) => this._onCategoriesChanged(v as Model.SyslogCategory[]),
                            () => this._onCategoriesClosed(),
                            true
                        )}
                        <or-input ?hidden="${hideFilter}" .type="${InputType.TEXT}" outlined ?disabled="${disabled}" icontrailing="magnify" .label="${i18next.t("subCategoryFilters")}" .value="${this.filter}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onFilterChanged(evt.detail.value)}"></or-input>
                        <or-input ?hidden="${hideLevel}" .type="${InputType.SELECT}" id="level-select" outlined ?disabled="${disabled}" .label="${i18next.t("level")}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onLevelChanged(evt.detail.value)}" .value="${this.level}" .options="${this._getLevelOptions()}"></or-input>
                        <or-input .type="${ InputType.SELECT}" id="limit-select" outlined ?disabled="${disabled}" .label="${i18next.t("limit")}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onLimitChanged(evt.detail.value)}" .value="${this.limit}" .options="${this._getLimitOptions()}"></or-input>
                    </div>
                    <div id="page-controls" ?hidden="${isLive || !this._pageCount || !this._data || this._data.length === 0}">
                        <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive || this._currentPage === 1}" icon="chevron-left" @click="${() => this._updatePage(false)}"></or-input>
                        <span>${this._currentPage}</span><or-translate value="of"></or-translate><span>${this._pageCount}</span>
                        <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive || this._currentPage === this._pageCount || (this._data && this._data.length < (this.limit || 50))}" icon="chevron-right" @click="${() => this._updatePage(true)}"></or-input>
                    </div>
                    <div id="time-controls">
                        <or-input id="live-button" .type="${InputType.CHECKBOX}" ?disabled="${disabled}" .label="${i18next.t("live")}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onLiveChanged(evt.detail.value)}" .value="${this.live}"></or-input>
                        <or-input .type="${InputType.SELECT}" id="period-select" outlined ?disabled="${disabled || isLive}" .label="${i18next.t("period")}" @or-input-changed="${(evt: OrInputChangedEvent) => this.interval = evt.detail.value}" .value="${this.interval}" .options="${this._getIntervalOptions()}"></or-input>
                        <div id="ending-controls">
                            <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-left" @click="${() => this.timestamp = this._calculateTimestamp(this.timestamp!, false)}"></or-input>
                            <or-input id="ending-date" outlined .type="${InputType.DATETIME}" ?disabled="${disabled || isLive}" label="${i18next.t("ending")}" .value="${this.timestamp}" @or-input-changed="${(evt: OrInputChangedEvent) => this.timestamp = this._calculateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                            <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-right" @click="${() => this.timestamp = this._calculateTimestamp(this.timestamp!, true)}"></or-input>
                            <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-double-right" @click="${() => this.timestamp = this._calculateTimestamp(new Date())}"></or-input>
                        </div>
                    </div>
                </div>
                
                ${disabled ? html`<div id="msg">${i18next.t("loading")}</div>` :
                    html`<div id="table-container">
                        ${this._data ? this._getTable() : ``}
                    </div>`}
            </div>
        `;
    }

    protected _getLimitOptions() {
        return [25, 50, 100, 200];
    }

    protected _getLevelOptions() {
        return Object.keys((Model as any)["SyslogLevel"]).map((key) => [key, i18next.t(key.toLocaleLowerCase())]);
    }

    protected _getCategoryMenuItems(): MenuItem[] {
        const categories = this.config && this.config.allowedCategories ? this.config.allowedCategories : Object.keys((Model as any)["SyslogCategory"]) as Model.SyslogCategory[];
        return categories.map((cat) => {
            return {
                text: cat,
                value: cat
            } as MenuItem;
        });
    }

    protected _onCategoriesChanged(values: Model.SyslogCategory[]) {
        this._pendingCategories = values;
    }

    protected _onCategoriesClosed() {
        if (!this._pendingCategories) {
            return;
        }

        this.categories = [...this._pendingCategories];

        if (this.categories && this.live) {
            this._data = this._data!.filter((e) => this.categories!.find((c) => c === e.category));
        }

        this._pendingCategories = undefined;
    }

    protected _onLiveChanged(live: boolean) {
        this.live = live;
        if (live) {
            this._data = [];
            this._pageCount = undefined;
            this._currentPage = 1;
        }
    }

    protected _onLimitChanged(limit: number) {
        this.limit = limit;

        if (this.live && this._data!.length > limit) {
            this._data!.splice(limit - 1);
        }
    }

    protected _onFilterChanged(filter: string) {
        this.filter = filter;

        if (!this.filter) {
            return;
        }

        if (this.live) {
            const filters = this.filter.split(";");
            this._data = this._data!.filter((e) => filters.find((f) => f === e.subCategory));
        }
    }

    protected _onLevelChanged(level: Model.SyslogLevel) {
        this.level = level;

        if (!this.level) {
            return;
        }

        if (this.live) {
            this._data = this._data!.filter((e) => this._eventMatchesLevel(e));
        }
    }

    protected _getTable(): TemplateResult {

        return html`
            <div id="table" class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="logs list">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            <th style="width: 180px" class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t("timestamp")}</th>
                            <th style="width: 80px" class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t("level")}</th>
                            <th style="width: 130px" class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t("category")}</th>
                            <th style="width: 180px" class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t("subCategory")}</th>
                            <th style="width: 100%; min-width: 300px;" class="mdc-data-table__header-cell" role="columnheader" scope="col">${i18next.t("message")}</th>                            
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this._data!.map((ev) => {
                            return html`
                                <tr class="mdc-data-table__row">
                                    <td class="mdc-data-table__cell">${moment(ev.timestamp).format(OrLogViewer.DEFAULT_TIMESTAMP_FORMAT)}</td>
                                    <td class="mdc-data-table__cell">${i18next.t(ev.level!)}</td>
                                    <td class="mdc-data-table__cell">${i18next.t(ev.category!)}</td>                                    
                                    <td class="mdc-data-table__cell">${i18next.t(ev.subCategory!)}</td>                                    
                                    <td class="mdc-data-table__cell">${ev.message}</td>                                    
                                </tr>
                            `;            
                        })}
                    </tbody>
                </table>
            </div>
            `;
    }

    protected _getIntervalOptions(): [string, string][] {
        return [
            Model.DatapointInterval.HOUR,
            Model.DatapointInterval.DAY,
            Model.DatapointInterval.WEEK,
            Model.DatapointInterval.MONTH,
            Model.DatapointInterval.YEAR
        ].map((interval) => {
            return [interval, i18next.t(interval.toLowerCase())];
        });
    }

    protected async _loadData() {
        if (this._loading) {
            return;
        }

        this._loading = true;

        const response = await manager.rest.api.SyslogResource.getEvents({
            level: this.level,
            per_page: this.limit,
            page: this._currentPage,
            from: this._getFrom(),
            to: this.timestamp ? this.timestamp.getTime() : undefined,
            category: this.categories as Model.SyslogCategory[],
            subCategory: this.filter ? this.filter.split(";") : undefined
        });

        this._loading = false;

        if (response.status === 200) {
            // Get page count
            this._pageCount = this._getPageCount(response);
            this._data = response.data;
        }
    }

    protected _getPageCount(response: GenericAxiosResponse<any>): number | undefined {
        const linkHeaders = response.headers["link"] as string;
        if (linkHeaders) {
            const links = linkParser(linkHeaders);
            const lastLink = links["last"];
            if (lastLink) {
                return lastLink["page"] as number;
            }
        }
    }

    protected _updatePage(forward: boolean) {
        if (!this._pageCount) {
            return;
        }

        if (forward) {
            if (this._currentPage < this._pageCount) {
                this._data = undefined;
                this._currentPage++;
            }
        } else {
            if (this._currentPage > 1) {
                this._data = undefined;
                this._currentPage--;
            }
        }
    }

    protected async _subscribeEvents() {
        if (manager.events) {
            this._eventSubscriptionId = await manager.events.subscribe<Model.SyslogEvent>({
                eventType: "syslog"
            }, (ev) => this._onEvent(ev));
        }
    }

    protected _unsubscribeEvents() {
        if (this._eventSubscriptionId) {
            manager.events!.unsubscribe(this._eventSubscriptionId);
            this._eventSubscriptionId = undefined;
        }
    }

    protected _getFrom(): number | undefined {
        if (!this.timestamp || !this.interval) {
            return;
        }

        return this._calculateTimestamp(this.timestamp, false)!.getTime();
    }

    protected _calculateTimestamp(timestamp: Date, forward?: boolean): Date | undefined {
        if (!this.interval) {
            return;
        }

        const newMoment = moment(timestamp);

        if (forward !== undefined) {
            switch (this.interval) {
                case Model.DatapointInterval.HOUR:
                    newMoment.add(forward ? 1 : -1, "hour");
                    break;
                case Model.DatapointInterval.DAY:
                    newMoment.add(forward ? 1 : -1, "day");
                    break;
                case Model.DatapointInterval.WEEK:
                    newMoment.add(forward ? 1 : -1, "week");
                    break;
                case Model.DatapointInterval.MONTH:
                    newMoment.add(forward ? 1 : -1, "month");
                    break;
                case Model.DatapointInterval.YEAR:
                    newMoment.add(forward ? 1 : -1, "year");
                    break;
            }
        }

        return newMoment.toDate();
    }

    protected _onEvent(event: Model.SyslogEvent) {

        // TODO: Move filtering to server side
        if (this.categories && !this.categories.find((c) => c === event.category)) {
            return;
        }

        if (this.filter) {
            const filters = this.filter.split(";");
            if (!filters.find((f) => f === event.subCategory)) {
                return;
            }
        }

        if (!this._eventMatchesLevel(event)) {
            return;
        }

        const limit = this.limit || OrLogViewer.DEFAULT_LIMIT;
        if (this._data!.length === limit - 1) {
            this._data!.pop();
        }
        this._data!.splice(0, 0, event);

        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }

        // Buffer updates to prevent excessive re-renders
        this._refresh = window.setTimeout(() => this.requestUpdate("_data"), 2000);
    }

    protected _eventMatchesLevel(event: Model.SyslogEvent): boolean {
        if (!this.level || this.level === Model.SyslogLevel.INFO) {
            return true;
        }

        if (this.level === Model.SyslogLevel.WARN && (event.level === Model.SyslogLevel.WARN || event.level === Model.SyslogLevel.ERROR)) {
            return true;
        }

        if (this.level === Model.SyslogLevel.ERROR && event.level === Model.SyslogLevel.ERROR) {
            return true;
        }

        return false;
    }

    protected _cleanup() {
        if (this._table) {
            this._table.destroy();
            this._table = undefined;
        }

        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }
    }
}
