var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var OrLogViewer_1;
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
import * as Model from "@openremote/model";
import manager, { DefaultColor2, DefaultColor3, Util } from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import moment from "moment";
import "@openremote/or-mwc-components/or-mwc-menu";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const linkParser = require("parse-link-header");
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");
// language=CSS
const style = css `
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
    
/*  min-width of selects is 200px somehow
    #period-select {
        width: 90px;
    }
    
    #level-select {
        width: 120px;
    }
    
    #limit-select {
        width: 80px;
    }
*/
    
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
let OrLogViewer = OrLogViewer_1 = class OrLogViewer extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.live = false;
        this._loading = false;
        this._currentPage = 1;
    }
    static get styles() {
        return [
            css `${unsafeCSS(tableStyle)}`,
            style
        ];
    }
    connectedCallback() {
        super.connectedCallback();
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        this._cleanup();
        this._unsubscribeEvents();
    }
    shouldUpdate(_changedProperties) {
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
            this.interval = OrLogViewer_1.DEFAULT_INTERVAL;
        }
        if (!this.categories) {
            if (this.config && this.config.initialCategories) {
                this.categories = [...this.config.initialCategories];
            }
            else {
                this.categories = Object.keys(Model["SyslogCategory"]);
            }
        }
        if (this.filter === undefined && this.config && this.config.initialFilter) {
            this.filter = this.config.initialFilter;
        }
        if (!this.level) {
            if (this.config && this.config.initialLevel) {
                this.level = this.config.initialLevel;
            }
            else {
                this.level = OrLogViewer_1.DEFAULT_LEVEL;
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
            }
            else {
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
        return html `
            <div id="container">
                <div id="controls">
                    <div id="log-controls">
                        ${hideCategories ? `` : getContentWithMenuTemplate(html `<or-mwc-input .type=${InputType.BUTTON} raised ?disabled="${disabled}" .label="${i18next.t("categories")}" icontrailing="chevron-down"></or-mwc-input>`, this._getCategoryMenuItems(), this.categories, (v) => this._onCategoriesChanged(v), () => this._onCategoriesClosed(), true)}
                        <or-mwc-input ?hidden="${hideFilter}" .type="${InputType.TEXT}" outlined ?disabled="${disabled}" icontrailing="magnify" .label="${i18next.t("subCategoryFilters")}" .value="${this.filter}" @or-mwc-input-changed="${(evt) => this._onFilterChanged(evt.detail.value)}"></or-mwc-input>
                        <or-mwc-input ?hidden="${hideLevel}" .type="${InputType.SELECT}" id="level-select" ?disabled="${disabled}" .label="${i18next.t("level")}" @or-mwc-input-changed="${(evt) => this._onLevelChanged(evt.detail.value)}" .value="${this.level}" .options="${this._getLevelOptions()}"></or-mwc-input>
                        <or-mwc-input .type="${InputType.SELECT}" id="limit-select" ?disabled="${disabled}" .label="${i18next.t("limit")}" @or-mwc-input-changed="${(evt) => this._onLimitChanged(evt.detail.value)}" .value="${"" + this.getLimit()}" .options="${this._getLimitOptions()}"></or-mwc-input>
                    </div>
                    <div id="page-controls" ?hidden="${isLive || !this._pageCount || !this._data || this._data.length === 0}">
                        <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive || this._currentPage === 1}" icon="chevron-left" @or-mwc-input-changed="${() => this._updatePage(false)}"></or-mwc-input>
                        <span>${this._currentPage}</span><or-translate value="of"></or-translate><span>${this._pageCount}</span>
                        <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive || this._currentPage === this._pageCount || (this._data && this._data.length < this.getLimit())}" icon="chevron-right" @or-mwc-input-changed="${() => this._updatePage(true)}"></or-mwc-input>
                    </div>
                    <div id="time-controls">
                        <or-mwc-input id="live-button" .type="${InputType.CHECKBOX}" ?disabled="${disabled}" .label="${i18next.t("live")}" @or-mwc-input-changed="${(evt) => this._onLiveChanged(evt.detail.value)}" .value="${this.live}"></or-mwc-input>
                        <or-mwc-input .type="${InputType.SELECT}" id="period-select" ?disabled="${disabled || isLive}" .label="${i18next.t("period")}" @or-mwc-input-changed="${(evt) => this.interval = evt.detail.value}" .value="${this.interval}" .options="${this._getIntervalOptions()}"></or-mwc-input>
                        <div id="ending-controls">
                            <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-left" @or-mwc-input-changed="${() => this.timestamp = this._calculateTimestamp(this.timestamp, false)}"></or-mwc-input>
                            <or-mwc-input id="ending-date" .type="${InputType.DATETIME}" ?disabled="${disabled || isLive}" label="${i18next.t("ending")}" .value="${this.timestamp}" @or-mwc-input-changed="${(evt) => this.timestamp = this._calculateTimestamp(moment(evt.detail.value).toDate())}"></or-mwc-input>
                            <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-right" @or-mwc-input-changed="${() => this.timestamp = this._calculateTimestamp(this.timestamp, true)}"></or-mwc-input>
                            <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled || isLive}" icon="chevron-double-right" @or-mwc-input-changed="${() => this.timestamp = this._calculateTimestamp(new Date())}"></or-mwc-input>
                        </div>
                    </div>
                </div>
                
                ${disabled ? html `<div id="msg">${i18next.t("loading")}</div>` :
            html `<div id="table-container">
                        ${this._data ? this._getTable() : ``}
                    </div>`}
            </div>
        `;
    }
    _getLimitOptions() {
        return ["25", "50", "100", "200"];
    }
    _getLevelOptions() {
        return Object.keys(Model["SyslogLevel"]).map((key) => [key, i18next.t(key.toLocaleLowerCase())]);
    }
    _getCategoryMenuItems() {
        const categories = this.config && this.config.allowedCategories ? this.config.allowedCategories : Object.keys(Model["SyslogCategory"]);
        return categories.map((cat) => {
            return {
                text: i18next.t("logCategory." + cat, { defaultValue: Util.capitaliseFirstLetter(cat.toLowerCase().replace(/_/g, " ")) }),
                value: cat
            };
        });
    }
    getLimit() {
        return this.limit || OrLogViewer_1.DEFAULT_LIMIT;
    }
    _onCategoriesChanged(values) {
        this._pendingCategories = values;
    }
    _onCategoriesClosed() {
        if (!this._pendingCategories) {
            return;
        }
        this.categories = [...this._pendingCategories];
        if (this.categories && this.live) {
            this._data = this._data.filter((e) => this.categories.find((c) => c === e.category));
        }
        this._pendingCategories = undefined;
    }
    _onLiveChanged(live) {
        this.live = live;
        if (live) {
            this._data = [];
            this._pageCount = undefined;
            this._currentPage = 1;
        }
    }
    _onLimitChanged(limit) {
        if (!limit) {
            this.limit = undefined;
            return;
        }
        this.limit = parseInt(limit);
        const newLimit = this.getLimit();
        if (this.live && this._data.length > newLimit) {
            this._data.splice(newLimit - 1);
        }
    }
    _onFilterChanged(filter) {
        this.filter = filter;
        if (!this.filter) {
            return;
        }
        if (this.live) {
            const filters = this.filter.split(";");
            this._data = this._data.filter((e) => filters.find((f) => f === e.subCategory));
        }
    }
    _onLevelChanged(level) {
        this.level = level;
        if (!this.level) {
            return;
        }
        if (this.live) {
            this._data = this._data.filter((e) => this._eventMatchesLevel(e));
        }
    }
    _getTable() {
        return html `
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
                        ${this._data.map((ev) => {
            return html `
                                <tr class="mdc-data-table__row">
                                    <td class="mdc-data-table__cell">${moment(ev.timestamp).format(OrLogViewer_1.DEFAULT_TIMESTAMP_FORMAT)}</td>
                                    <td class="mdc-data-table__cell">${i18next.t(ev.level)}</td>
                                    <td class="mdc-data-table__cell">${i18next.t(ev.category)}</td>                                    
                                    <td class="mdc-data-table__cell">${i18next.t(ev.subCategory)}</td>                                    
                                    <td class="mdc-data-table__cell">${ev.message}</td>                                    
                                </tr>
                            `;
        })}
                    </tbody>
                </table>
            </div>
            `;
    }
    _getIntervalOptions() {
        return [
            "HOUR" /* Model.DatapointInterval.HOUR */,
            "DAY" /* Model.DatapointInterval.DAY */,
            "WEEK" /* Model.DatapointInterval.WEEK */,
            "MONTH" /* Model.DatapointInterval.MONTH */,
            "YEAR" /* Model.DatapointInterval.YEAR */
        ].map((interval) => {
            return [interval, i18next.t(interval.toLowerCase())];
        });
    }
    _loadData() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._loading) {
                return;
            }
            this._loading = true;
            const response = yield manager.rest.api.SyslogResource.getEvents({
                level: this.level,
                per_page: this.getLimit(),
                page: this._currentPage,
                from: this._getFrom(),
                to: this.timestamp ? this.timestamp.getTime() : undefined,
                category: this.categories,
                subCategory: this.filter ? this.filter.split(";") : undefined
            });
            this._loading = false;
            if (response.status === 200) {
                // Get page count
                this._pageCount = this._getPageCount(response);
                this._data = response.data;
            }
        });
    }
    _getPageCount(response) {
        const linkHeaders = response.headers["link"];
        if (linkHeaders) {
            const links = linkParser(linkHeaders);
            const lastLink = links["last"];
            if (lastLink) {
                return lastLink["page"];
            }
        }
    }
    _updatePage(forward) {
        if (!this._pageCount) {
            return;
        }
        if (forward) {
            if (this._currentPage < this._pageCount) {
                this._data = undefined;
                this._currentPage++;
            }
        }
        else {
            if (this._currentPage > 1) {
                this._data = undefined;
                this._currentPage--;
            }
        }
    }
    _subscribeEvents() {
        return __awaiter(this, void 0, void 0, function* () {
            if (manager.events) {
                this._eventSubscriptionId = yield manager.events.subscribe({
                    eventType: "syslog"
                }, (ev) => this._onEvent(ev));
            }
        });
    }
    _unsubscribeEvents() {
        if (this._eventSubscriptionId) {
            manager.events.unsubscribe(this._eventSubscriptionId);
            this._eventSubscriptionId = undefined;
        }
    }
    _getFrom() {
        if (!this.timestamp || !this.interval) {
            return;
        }
        return this._calculateTimestamp(this.timestamp, false).getTime();
    }
    _calculateTimestamp(timestamp, forward) {
        if (!this.interval) {
            return;
        }
        const newMoment = moment(timestamp);
        if (forward !== undefined) {
            switch (this.interval) {
                case "HOUR" /* Model.DatapointInterval.HOUR */:
                    newMoment.add(forward ? 1 : -1, "hour");
                    break;
                case "DAY" /* Model.DatapointInterval.DAY */:
                    newMoment.add(forward ? 1 : -1, "day");
                    break;
                case "WEEK" /* Model.DatapointInterval.WEEK */:
                    newMoment.add(forward ? 1 : -1, "week");
                    break;
                case "MONTH" /* Model.DatapointInterval.MONTH */:
                    newMoment.add(forward ? 1 : -1, "month");
                    break;
                case "YEAR" /* Model.DatapointInterval.YEAR */:
                    newMoment.add(forward ? 1 : -1, "year");
                    break;
            }
        }
        return newMoment.toDate();
    }
    _onEvent(event) {
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
        const limit = this.getLimit();
        if (this._data.length === limit - 1) {
            this._data.pop();
        }
        this._data.splice(0, 0, event);
        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }
        // Buffer updates to prevent excessive re-renders
        this._refresh = window.setTimeout(() => this.requestUpdate("_data"), 2000);
    }
    _eventMatchesLevel(event) {
        if (!this.level || this.level === "INFO" /* Model.SyslogLevel.INFO */) {
            return true;
        }
        if (this.level === "WARN" /* Model.SyslogLevel.WARN */ && (event.level === "WARN" /* Model.SyslogLevel.WARN */ || event.level === "ERROR" /* Model.SyslogLevel.ERROR */)) {
            return true;
        }
        if (this.level === "ERROR" /* Model.SyslogLevel.ERROR */ && event.level === "ERROR" /* Model.SyslogLevel.ERROR */) {
            return true;
        }
        return false;
    }
    _cleanup() {
        if (this._table) {
            this._table.destroy();
            this._table = undefined;
        }
        if (this._refresh) {
            window.clearTimeout(this._refresh);
        }
    }
};
OrLogViewer.DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";
OrLogViewer.DEFAULT_LIMIT = 50;
OrLogViewer.DEFAULT_LEVEL = "INFO" /* Model.SyslogLevel.INFO */;
OrLogViewer.DEFAULT_INTERVAL = "HOUR" /* Model.DatapointInterval.HOUR */;
__decorate([
    property({ type: String })
], OrLogViewer.prototype, "interval", void 0);
__decorate([
    property({ type: Number })
], OrLogViewer.prototype, "timestamp", void 0);
__decorate([
    property({ type: Number })
], OrLogViewer.prototype, "limit", void 0);
__decorate([
    property({ type: Array })
], OrLogViewer.prototype, "categories", void 0);
__decorate([
    property({ type: String })
], OrLogViewer.prototype, "filter", void 0);
__decorate([
    property({ type: String })
], OrLogViewer.prototype, "level", void 0);
__decorate([
    property({ type: Boolean })
], OrLogViewer.prototype, "live", void 0);
__decorate([
    property({ type: Object })
], OrLogViewer.prototype, "config", void 0);
__decorate([
    property()
], OrLogViewer.prototype, "_loading", void 0);
__decorate([
    property()
], OrLogViewer.prototype, "_data", void 0);
__decorate([
    query("#table")
], OrLogViewer.prototype, "_tableElem", void 0);
OrLogViewer = OrLogViewer_1 = __decorate([
    customElement("or-log-viewer")
], OrLogViewer);
export { OrLogViewer };
//# sourceMappingURL=index.js.map