import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {DefaultColor2, DefaultColor5} from "@openremote/core";

// language=CSS
const style = css`
    
    :host {
        display: block;
        box-sizing: content-box;
        margin: 0;
        overflow: hidden;
        transition: margin 225ms cubic-bezier(0.4, 0, 0.2, 1),box-shadow 280ms cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        border-radius: 4px;
        border-width: 1px;
        border-style: solid;
    }

    :host([hidden]) {
        display: none;
    }
    
    #header {
        display: flex;
        height: 48px;
        flex-direction: row;
        font-family: Roboto,Helvetica Neue,sans-serif;
        font-size: 15px;
        font-weight: 400;
        align-items: center;
        padding: 0 24px;
        border-radius: inherit;
    }
    
    #header.expandable {
        cursor: pointer;
        transition: height 225ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    
    #header.expanded {
        height: 64px;
    }

    #header.expanded > #indicator {
        transform: rotate(180deg);
    }
    
    #header-content {
        flex: 1;
        display: flex;
        flex-direction: row;
        overflow: hidden;
    }

    #header-title, #header-description {
        margin-right: 16px;
        display: inline-flex;
        align-items: center;
    }

    #header-title {
        flex: 0;
    }
    
    #header-description {
        flex-grow: 2;
    }
    
    #indicator {
        align-self: center;
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
    
    #content {
        display: flex;
        height: 0;
        visibility: hidden;
    }
    
    #content.expanded {
        height: unset;
        visibility: visible;
    }
`;

@customElement("or-collapsible-panel")
export class OrCollapsiblePanel extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Boolean})
    expanded: boolean = false;
    @property({type: Boolean})
    expandable: boolean = true;
    @query("#header")
    protected headerElem!: HTMLDivElement;

    protected _onHeaderClicked(ev: MouseEvent) {
        if (!this.expandable) {
            return;
        }
        ev.preventDefault();
        this.expanded = !this.expanded;
    }

    render() {

        return html`
            <div id="header" class="${this.expandable ? "expandable" : ""} ${this.expandable && this.expanded ? "expanded" : ""}" @click="${(ev:MouseEvent) => this._onHeaderClicked(ev)}">
                <span id="header-content">
                    <span id="header-title"><slot name="header"></slot></span>
                    <span id="header-description"><slot name="header-description"></slot></span>
                </span>
                ${this.expandable ? html`<span id="indicator"></span>` : ""}
            </div>
            <div id="content" class="${this.expandable && this.expanded ? "expanded" : ""}">
                <slot name="content"></slot>
            </div>
        `;
    }
}
