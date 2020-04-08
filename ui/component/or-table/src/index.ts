import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult, unsafeCSS, query} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {MDCDataTable} from "@material/data-table";

import {DefaultColor1, DefaultColor4, DefaultColor8} from "@openremote/core";

const dataTableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

// language=CSS
const style = css`

    :host([hidden]) {
        display: none;
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
    public headers!: string[];

    @property({type: Array})
    public rows!: [string[]];

    protected render() {

        return html`
            <div class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="Dessert calories">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            ${this.headers.map(item => html`
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    ${item}
<!--                                    <or-icon icon="menu-swap" @click="${() => console.log('click')}"></or-icon>  -->
                                </th>
                            `)}  
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this.rows.map(item => html`
                            <tr class="mdc-data-table__row">
                                ${item.map(cell => html`<td class="mdc-data-table__cell">${cell}</td>`)}  
                            </tr>
                        `)}
                    </tbody>
                </table>
            </div>
        `;
    }

}
