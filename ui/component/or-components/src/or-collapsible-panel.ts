import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {DefaultColor2} from "@openremote/core";

// language=CSS
const style = css`
    
    :host {
        display: block;
    }

    :host([hidden]) {
        display: none;
    }

    #indicator::after {
        border-style: solid;
        border-width: 0 2px 2px 0;
        content: "";
        display: inline-block;
        padding: 3px;
        transform: rotate(45deg);
        vertical-align: middle;
    }
`;

@customElement("or-collapsible-panel")
export class OrCollapsiblePanel extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Boolean})
    expanded: boolean = false;

    protected _onHeaderClicked(ev: MouseEvent) {
        ev.preventDefault();

        if ((ev.currentTarget as HTMLElement).id !== "header") {
            return;
        }


    }

    render() {

        return html`
            <div id="header" @click="${(ev:MouseEvent) => this._onHeaderClicked(ev)}">
                <span>
                    <slot name="header"></slot>
                    <slot name="header-content"></slot>
                </span>
                <span id="indicator"></span>
            </div>
            <div>
                <slot name="content"></slot>
            </div>
        `;
    }
}
