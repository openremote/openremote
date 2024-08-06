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
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { classMap } from "lit/directives/class-map.js";
import { until } from 'lit/directives/until.js';
import { MDCDataTable } from "@material/data-table";
import { when } from 'lit/directives/when.js';
import { DefaultColor3, DefaultColor2, DefaultColor1 } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { InputType } from "./or-mwc-input";
import { styleMap } from "lit/directives/style-map.js";
const dataTableStyle = require("@material/data-table/dist/mdc.data-table.css");
// language=CSS
const style = css `

    :host {
        width: 100%;
    }

    :host([hidden]) {
        display: none;
    }

    .mdc-data-table {
        width: 100%;
        overflow: auto;
        max-height: 500px;
    }

    .mdc-data-table__paginated {
        overflow: hidden;
        max-height: 700px;
        justify-content: space-between;
    }
    
    .mdc-data-table__fullheight {
        height: 100%;
        max-height: none !important;
    }

    /* first column should be sticky*/
    .mdc-data-table.has-sticky-first-column tr th:first-of-type,
    .mdc-data-table.has-sticky-first-column tr td:first-of-type {
        z-index: 1;
        position: sticky;
        left: 0;
        background-color: ${unsafeCSS(DefaultColor2)};
    }

    .mdc-data-table.has-sticky-first-column tr th:first-of-type {
        z-index: 2;
    }

    thead th {
        box-shadow: 0 1px 0 0 rgb(229, 229, 229);
    }

    .mdc-data-table.has-sticky-first-column tr td:first-of-type {
        box-shadow: 1px 0 0 0 rgb(229, 229, 229);
    }

    thead th:first-of-type {
        box-shadow: 1px 1px 0 0 rgb(229, 229, 229);
    }

    th {
        position: sticky;
        top: 0;
        background-color: ${unsafeCSS(DefaultColor1)};
    }

    th, td {
        cursor: default;
    }

    .mdc-data-table__header-cell {
        font-weight: bold;
        color: ${unsafeCSS(DefaultColor3)};
        font-size: 14px;
    }

    .mdc-data-table__pagination-rows-per-page-select {
        /*min-width: 112px;*/
    }

    .mdc-data-table__pagination {
        min-height: 64px;
    }

    .mdc-data-table__cell--clickable {
        cursor: pointer;
    }
    
    .sort-button {
        padding-right: 0;
        border: none;
        background-color: ${unsafeCSS(DefaultColor1)};
        color: ${unsafeCSS(DefaultColor3)};
        cursor: pointer;
    }

    .sort-button-reverse {
        padding-left: 0;
        border: none;
        background-color: ${unsafeCSS(DefaultColor1)};
        color: ${unsafeCSS(DefaultColor3)};
        cursor: pointer;
    }
    
    .sortable {
        flex-direction: row;
        cursor: pointer;
    }
    
    .sortable-reverse {
        flex-direction: row-reverse;
        cursor: pointer;
    }
    
    .hidden {
        visibility: hidden;
    }
    
    #column-1 {
        width: var(--or-mwc-table-column-width-1, unset);
    }    
    #column-2 {
        width: var(--or-mwc-table-column-width-2, unset);
    }    
    #column-3 {
        width: var(--or-mwc-table-column-width-3, unset);
    }    
    #column-4 {
        width: var(--or-mwc-table-column-width-4, unset);
    }    
    #column-5 {
        width: var(--or-mwc-table-column-width-5, unset);
    }    
    #column-6 {
        width: var(--or-mwc-table-column-width-6, unset);
    }    
    #column-7 {
        width: var(--or-mwc-table-column-width-7, unset);
    }    
    #column-8 {
        width: var(--or-mwc-table-column-width-8, unset);
    }    
    #column-9 {
        width: var(--or-mwc-table-column-width-9, unset);
    }    
    #column-10 {
        width: var(--or-mwc-table-column-width-10, unset);
    }

    @media screen and (max-width: 768px) {
        .hide-mobile {
            display: none;
        }
    }
`;
export class OrMwcTableRowClickEvent extends CustomEvent {
    constructor(index) {
        super(OrMwcTableRowClickEvent.NAME, {
            detail: {
                index: index
            },
            bubbles: true,
            composed: true
        });
    }
}
OrMwcTableRowClickEvent.NAME = "or-mwc-table-row-click";
let OrMwcTable = class OrMwcTable extends LitElement {
    constructor() {
        super(...arguments);
        this.config = {
            columnFilter: [],
            stickyFirstColumn: true,
            fullHeight: false,
            pagination: {
                enable: false
            }
        };
        this.paginationIndex = 0;
        this.paginationSize = 10;
        this.sortDirection = 'ASC';
        this.sortIndex = -1;
        this.selectedRows = [];
        this.indeterminate = false;
        this.allSelected = false;
    }
    static get styles() {
        return [
            css `${unsafeCSS(dataTableStyle)}`,
            style
        ];
    }
    /* ------------------- */
    firstUpdated(changedProperties) {
        const elem = this.shadowRoot.querySelector('.mdc-data-table');
        this._dataTable = new MDCDataTable(elem);
    }
    updated(changedProperties) {
        var _a;
        if ((changedProperties.has('paginationIndex') || changedProperties.has('paginationSize')) && ((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.enable)) {
            const elem = (this._dataTable ? this._dataTable.root.children[0] : this.shadowRoot.querySelector('.mdc-data-table__table-container'));
            // Using an observer to prevent forced reflow / DOM measurements; prevents blocking the thread
            const observer = new IntersectionObserver((entries, observer) => {
                entries[0].target.scrollTop = 0;
                observer.unobserve(entries[0].target);
            });
            observer.observe(elem);
            // Reset selected rows properties
            this.selectedRows = [];
            this.indeterminate = false;
            this.allSelected = false;
        }
    }
    render() {
        var _a, _b, _c, _d;
        const tableClasses = {
            "mdc-data-table": true,
            "mdc-data-table__paginated": !!((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.enable),
            "mdc-data-table__fullheight": !!this.config.fullHeight,
            "has-sticky-first-column": !!this.config.stickyFirstColumn
        };
        // Only show pagination if enabled in config, and when "the amount of rows doesn't fit on the page".
        const showPagination = ((_b = this.config.pagination) === null || _b === void 0 ? void 0 : _b.enable) && (!!this.rowsTemplate || (this.rows && (this.rows.length > this.paginationSize)));
        const tableWidth = (_d = (_c = this.shadowRoot) === null || _c === void 0 ? void 0 : _c.firstElementChild) === null || _d === void 0 ? void 0 : _d.clientWidth;
        return html `
            <div class="${classMap(tableClasses)}">
                <div class="mdc-data-table__table-container" style="flex: 1;">
                    <table class="mdc-data-table__table">
                        <!-- Header row that normally includes entries like 'id' and 'name'. You can use either a template or a list of columns -->
                        ${when(this.columnsTemplate, () => this.columnsTemplate, () => {
            return this.columns ? html `
                                <thead>
                                <tr class="mdc-data-table__header-row">
                                    ${this.columns.map((column, index) => {
                const styles = {
                    maxWidth: tableWidth ? `${tableWidth / (this.columns.length / 2)}px` : undefined
                };
                if (index == 0 && this.config.multiSelect) {
                    return html ` 
                                            <th class="mdc-data-table__header-cell mdc-data-table__header-cell--checkbox"
                                                role="columnheader" scope="col">
                                                <div class="">
                                                    <or-mwc-input type="${InputType.CHECKBOX}" id="checkbox-${index}"
                                                                  class="${classMap({ 'mdi-checkbox-intermediate': this.indeterminate })}"
                                                                  .indeterminate="${this.indeterminate}"
                                                                  @or-mwc-input-changed="${(ev) => this.onCheckChanged(ev.detail.value, "all")}" .value="${this.allSelected}">
                                                    </or-mwc-input>
                                                </div>
                                            </th>
                                            `;
                }
                return (typeof column == "string") ? html `
                                            <th class="mdc-data-table__header-cell ${!!this.config.multiSelect ? "mdc-data-table__header-cell mdc-data-table__header-cell--checkbox" : ''}" id="column-${index + 1}" role="columnheader" scope="col"
                                                title="${column}">
                                                ${column}
                                            </th>
                                        ` : html `
                                            <th class="mdc-data-table__header-cell ${classMap({
                    'mdc-data-table__cell--numeric': !!column.isNumeric,
                    'hide-mobile': !!column.hideMobile,
                    'mdc-data-table__header-cell--with-sort': !!column.isSortable,
                })}"
                                                style="${styleMap(styles)}"
                                                role="columnheader" scope="col" title="${column.title}" data-column-id="${column.title}"
                                                @click="${(ev) => !!column.isSortable ? this.sortRows(ev, index, this.sortDirection) : ''}">
                                                ${(!column.isSortable ? column.title : until(this.getSortHeader(index, column.title, this.sortDirection, !!column.isNumeric), html `${i18next.t('loading')}`))}
                                            </th>
                                        `;
            })}
                                </tr>
                                </thead>
                            ` : undefined;
        })}
                        <!-- Table content, where either the template or an array of rows is displayed -->
                        <tbody class="mdc-data-table__content">
                        ${when(this.rowsTemplate, () => {
            var _a;
            if ((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.enable) { // if paginated, filter out the rows by index by manually collecting a list of <tr> elements.
                this.updateComplete.then(() => __awaiter(this, void 0, void 0, function* () {
                    const elem = yield this.getTableElem(false);
                    const rows = elem === null || elem === void 0 ? void 0 : elem.querySelectorAll('tr');
                    rows === null || rows === void 0 ? void 0 : rows.forEach((row, index) => {
                        const hidden = (index <= (this.paginationIndex * this.paginationSize) || index > (this.paginationIndex * this.paginationSize) + this.paginationSize) && !row.classList.contains('mdc-data-table__header-row');
                        row.style.display = (hidden ? 'none' : 'table-row');
                    });
                }));
            }
            return html `${this.rowsTemplate}`;
        }, () => {
            return this.rows ? this.rows
                .filter((row, index) => { var _a; return !((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.enable) || (index >= (this.paginationIndex * this.paginationSize)) && (index < (this.paginationIndex * this.paginationSize + this.paginationSize)); })
                .map((item) => {
                const content = (Array.isArray(item) ? item : item.content);
                const styles = {
                    maxWidth: tableWidth ? `${tableWidth / (this.columns.length / 2)}px` : undefined
                };
                return html `
                                                    <tr class="mdc-data-table__row" @click="${(ev) => this.onRowClick(ev, item)}">
                                                        ${content === null || content === void 0 ? void 0 : content.map((cell, index) => {
                    var _a;
                    const classes = {
                        "mdc-data-table__cell": true,
                        "mdc-data-table__cell--numeric": typeof cell === "number",
                        "mdc-data-table__cell--clickable": (!Array.isArray(item) && item.clickable),
                        "hide-mobile": (this.columns && typeof this.columns[index] != "string" && this.columns[index].hideMobile)
                    };
                    if (index == 0 && this.config.multiSelect) {
                        return html `
                                                                    <td class="mdc-data-table__cell mdc-data-table__cell--checkbox" >
                                                                        <div class="">
                                                                            <or-mwc-input type="${InputType.CHECKBOX}" id="checkbox-${index}"
                                                                                          @or-mwc-input-changed="${(ev) => this.onCheckChanged(ev.detail.value, "single", item)}" 
                                                                                          .value="${(_a = this.selectedRows) === null || _a === void 0 ? void 0 : _a.includes(item)}"
                                                                            ></or-mwc-input>
                                                                        </div>
                                                                    </td> `;
                    }
                    return html `
                                                                <td class="${classMap(classes)}" title="${cell}" style="${styleMap(styles)}">
                                                                    <span>${cell}</span>
                                                                </td>
                                                            `;
                })}
                                                    </tr>
                                                `;
            })
                : undefined;
        })}
                        </tbody>
                    </table>
                </div>
                <!-- Pagination HTML, shown on the bottom right. Same as Material Design spec -->
                ${when(showPagination, () => {
            var _a;
            const options = ((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.options) || [10, 25, 100];
            return html `
                        <div class="mdc-data-table__pagination">
                            <div class="mdc-data-table__pagination-trailing">
                                <div class="mdc-data-table__pagination-rows-per-page">
                                    <div class="mdc-data-table__pagination-rows-per-page-label">
                                        ${i18next.t('rowsPerPage')}
                                    </div>
                                    <or-mwc-input class="mdc-data-table__pagination-rows-per-page-select"
                                                  .type="${InputType.SELECT}" compact comfortable outlined .readonly="${options.length === 1}"
                                                  .value="${this.paginationSize}" .options="${options}"
                                                  @or-mwc-input-changed="${(ev) => {
                this.paginationSize = ev.detail.value;
                this.paginationIndex = 0;
            }}"
                                    ></or-mwc-input>
                                </div>
                                ${until(this.getPaginationControls(), html `${i18next.t('loading')}`)}
                            </div>
                        </div>
                    `;
        })}
            </div>
        `;
    }
    onRowClick(ev, item) {
        if (this.config.multiSelect) {
            const elem = ev.target;
            if (elem.nodeName === "OR-MWC-INPUT" && elem.id.includes('checkbox')) {
                return; // if checkbox is clicked, the regular "click on row" should not trigger.
            }
        }
        this.dispatchEvent(new OrMwcTableRowClickEvent(this.rows.indexOf(item)));
    }
    getSortHeader(index, title, sortDirection, arrowOnLeft = false) {
        return __awaiter(this, void 0, void 0, function* () {
            this.sortIndex === -1 ? this.sortIndex = index : '';
            return html `
            <div class="mdc-data-table__header-cell-wrapper ${arrowOnLeft ? 'sortable-reverse' : 'sortable'}">
                <div class="mdc-data-table__header-cell-label">
                    ${title}
                </div>
                <button class="mdc-icon-button material-icons ${arrowOnLeft ? 'sort-button-reverse' : 'sort-button'} ${this.sortIndex === index ? '' : 'hidden'}"
                        aria-label="Sort by ${title}" aria-describedby="${title}-status-label" aria-hidden="${!(this.sortIndex === index)}">
                    <or-icon icon="${sortDirection == 'ASC' ? "arrow-up" : "arrow-down"}"></or-icon>
                </button>
                <div class="mdc-data-table__sort-status-label" aria-hidden="true" id="${title}-status-label">
                </div>
            </div>
        `;
        });
    }
    sortRows(ev, index, sortDirection) {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.config.multiSelect) {
                const elem = ev.target;
                if (elem.nodeName === "OR-MWC-INPUT" && elem.id.includes('checkbox')) {
                    return; // if checkbox is clicked, sort should not trigger.
                }
            }
            sortDirection == 'ASC' ? this.sortDirection = 'DESC' : this.sortDirection = 'ASC';
            sortDirection = this.sortDirection;
            this.sortIndex = index;
            if (this.rows) {
                if ('content' in this.rows[0]) {
                    this.rows.sort((a, b) => {
                        return this.customSort(a.content, b.content, index, sortDirection);
                    });
                }
                else {
                    this.rows.sort((a, b) => {
                        return this.customSort(a, b, index, sortDirection);
                    });
                }
                return;
            }
            return;
        });
    }
    customSort(a, b, index, sortDirection) {
        if (!a[index] && !b[index]) {
            return 0;
        }
        if (!a[index]) {
            return 1;
        }
        if (!b[index]) {
            return -1;
        }
        if (typeof a[index] === 'string' && typeof b[index] === 'string') {
            return sortDirection == 'DESC' ? b[index].localeCompare(a[index], 'en', { numeric: true }) : a[index].localeCompare(b[index], 'en', { numeric: true });
        }
        else {
            return sortDirection == 'DESC' ? (a[index] > b[index] ? -1 : 1) : (a[index] < b[index] ? -1 : 1);
        }
    }
    onCheckChanged(checked, type, item) {
        var _a, _b, _c, _d, _e;
        let rowCount = (((_a = this.config.pagination) === null || _a === void 0 ? void 0 : _a.enable) && this.rows.length > this.paginationSize ? this.paginationSize : this.rows.length);
        if (type === "all") {
            if (checked) {
                this.selectedRows = this.rows ? this.rows
                    .filter((row, index) => (index >= (this.paginationIndex * this.paginationSize)) && (index < (this.paginationIndex * this.paginationSize + this.paginationSize))) : [];
                this.indeterminate = false;
                this.allSelected = true;
            }
            else {
                this.selectedRows = [];
                this.allSelected = false;
                this.indeterminate = false;
            }
        }
        else {
            if (checked) {
                if (this.selectedRows.indexOf(item) === -1) {
                    this.selectedRows.push(item);
                    this.indeterminate = (this.selectedRows.length < (((_b = this.config.pagination) === null || _b === void 0 ? void 0 : _b.enable) ? rowCount : this.rows.length) && this.selectedRows.length > 0);
                    this.allSelected = (this.selectedRows.length === (((_c = this.config.pagination) === null || _c === void 0 ? void 0 : _c.enable) ? rowCount : this.rows.length) && this.selectedRows.length > 0);
                    this.requestUpdate("selectedRows");
                }
            }
            else {
                this.selectedRows = this.selectedRows.filter((e) => e !== item);
            }
            this.indeterminate = (this.selectedRows.length < (((_d = this.config.pagination) === null || _d === void 0 ? void 0 : _d.enable) ? rowCount : this.rows.length) && this.selectedRows.length > 0);
            this.allSelected = (this.selectedRows.length === (((_e = this.config.pagination) === null || _e === void 0 ? void 0 : _e.enable) ? rowCount : this.rows.length) && this.selectedRows.length > 0);
        }
    }
    // HTML for the controls on the bottom of the table.
    // Includes basic pagination for browsing pages, with calculations of where to go.
    getPaginationControls() {
        return __awaiter(this, void 0, void 0, function* () {
            const max = yield this.getRowCount();
            const start = (this.paginationIndex * this.paginationSize) + 1;
            let end = this.paginationIndex * this.paginationSize + this.paginationSize;
            if (end > max) {
                end = max;
            }
            return html `
            <div class="mdc-data-table__pagination-navigation">
                <div class="mdc-data-table__pagination-total">
                    <span>${start}-${end} of ${max}</span>
                </div>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                              data-first-page="true" icon="page-first" .disabled="${this.paginationIndex == 0}"
                              @or-mwc-input-changed="${() => this.paginationIndex = 0}"></or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                              data-prev-page="true" icon="chevron-left" .disabled="${this.paginationIndex == 0}"
                              @or-mwc-input-changed="${() => this.paginationIndex--}"></or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                              data-next-page="true" icon="chevron-right"
                              .disabled="${this.paginationIndex * this.paginationSize + this.paginationSize >= max}"
                              @or-mwc-input-changed="${() => this.paginationIndex++}"></or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                              data-last-page="true" icon="page-last"
                              .disabled="${this.paginationIndex * this.paginationSize + this.paginationSize >= max}"
                              @or-mwc-input-changed="${() => __awaiter(this, void 0, void 0, function* () {
                let pages = max / this.paginationSize;
                pages = pages.toString().includes('.') ? Math.floor(pages) : (pages - 1);
                this.paginationIndex = pages;
            })}"
                ></or-mwc-input>
            </div>
        `;
        });
    }
    // Getting the amount of rows/entries in the table.
    // Makes sure that both the rows, and rowsTemplate properties work.
    getRowCount(wait = true, tableElem) {
        var _a, _b;
        return __awaiter(this, void 0, void 0, function* () {
            if ((_a = this.rows) === null || _a === void 0 ? void 0 : _a.length) {
                return (_b = this.rows) === null || _b === void 0 ? void 0 : _b.length;
            }
            if (!tableElem) {
                tableElem = yield this.getTableElem(wait);
            }
            const rowElems = tableElem === null || tableElem === void 0 ? void 0 : tableElem.querySelectorAll('tr');
            return rowElems.length;
        });
    }
    getTableElem(wait = false) {
        return __awaiter(this, void 0, void 0, function* () {
            if (wait) {
                yield this.updateComplete;
            }
            return this._dataTable ? this._dataTable.root : this.shadowRoot.querySelector('.mdc-data-table');
        });
    }
};
__decorate([
    property({ type: Array })
], OrMwcTable.prototype, "columns", void 0);
__decorate([
    property({ type: Object }) // to manually control HTML
], OrMwcTable.prototype, "columnsTemplate", void 0);
__decorate([
    property({ type: Array })
], OrMwcTable.prototype, "rows", void 0);
__decorate([
    property({ type: Object }) // to manually control HTML (requires td and tr elements)
], OrMwcTable.prototype, "rowsTemplate", void 0);
__decorate([
    property({ type: Array })
], OrMwcTable.prototype, "config", void 0);
__decorate([
    property({ type: Number })
], OrMwcTable.prototype, "paginationIndex", void 0);
__decorate([
    property({ type: Number })
], OrMwcTable.prototype, "paginationSize", void 0);
__decorate([
    state()
], OrMwcTable.prototype, "_dataTable", void 0);
__decorate([
    property({ type: String })
], OrMwcTable.prototype, "sortDirection", void 0);
__decorate([
    property({ type: Number })
], OrMwcTable.prototype, "sortIndex", void 0);
__decorate([
    property({ type: Array })
], OrMwcTable.prototype, "selectedRows", void 0);
__decorate([
    property()
], OrMwcTable.prototype, "indeterminate", void 0);
__decorate([
    property()
], OrMwcTable.prototype, "allSelected", void 0);
OrMwcTable = __decorate([
    customElement("or-mwc-table")
], OrMwcTable);
export { OrMwcTable };
//# sourceMappingURL=or-mwc-table.js.map