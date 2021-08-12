import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {classMap} from "lit/directives/class-map";
import {ifDefined} from "lit/directives/if-defined";
import {MDCDataTable} from "@material/data-table";

import {DefaultColor1, DefaultColor4, DefaultColor8} from "@openremote/core";

const dataTableStyle = require("@material/data-table/dist/mdc.data-table.css");

interface TableOptions {
    columnFilter?: string[];
    stickyFirstColumn?: boolean;
}

// language=CSS
const style = css`

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

    /* first column should be sticky*/
    .mdc-data-table.has-sticky-first-column tr th:first-of-type,
    .mdc-data-table.has-sticky-first-column tr td:first-of-type {
        z-index: 1;
        position: sticky;
        left: 0;
        background-color: var(--internal-or-asset-viewer-panel-color);
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
        background-color: var(--internal-or-asset-viewer-panel-color);
    }

    th, td {
        cursor: default;
    }
    
    th:not(:first-of-type), td:not(:first-of-type) {
        max-width: 100px;
        text-overflow: ellipsis;
    }

    .mdc-data-table__header-cell {
        font-weight: 700;
        color: #aaa;
        font-size: 12px;
    }
`;

@customElement("or-table")
export class OrTable extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(dataTableStyle)}`,
            style
        ];
    }

    @property({type: Array})
    public headers?: string[];

    @property({type: Array})
    public rows?: string[][];

    @property({type: Array})
    public options?: TableOptions = {
        columnFilter: [],
        stickyFirstColumn: true
    };

    protected render() {

        const headerTemplate = (!this.headers) ? html`` : html`
            <thead>
                <tr class="mdc-data-table__header-row">
                    ${this.headers.map(item => html`
                        <th class="mdc-data-table__header-cell" role="columnheader" scope="col" title="${item}">
                            ${item}
                        </th>
                    `)}
                </tr>
            </thead>`;
        return html`
            <div class="mdc-data-table ${classMap({"has-sticky-first-column": !!this.options!.stickyFirstColumn})}">
                <table class="mdc-data-table__table">
                    ${headerTemplate}
                    <tbody class="mdc-data-table__content">
                        ${!this.rows ? `` : this.rows.map(item => html`
                            <tr class="mdc-data-table__row">
                                ${item.map((cell: string|number) => {
                                    return html`<td class="mdc-data-table__cell ${classMap({"mdc-data-table__cell--numeric": typeof cell === "number"})}" title="${cell}">${cell}</td>`;
                                })}  
                            </tr>
                        `)}
                    </tbody>
                </table>
            </div>
        `;
    }

}
