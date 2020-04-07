import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult, unsafeCSS, query} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {MDCDataTable } from "@material/data-table";

import {DefaultColor1, DefaultColor4, DefaultColor8} from "@openremote/core";

// language=CSS
const style = css`
    
    :host([hidden]) {
        display: none;
    }
    
    #wrapper {
        display: flex;
        align-items: center;
    }
    
    #component {
        max-width: 100%;
    }
`;

@customElement("or-table")
export class OrTable extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    protected render() {

        return html`<span>tabel</span>`;
    }

}
