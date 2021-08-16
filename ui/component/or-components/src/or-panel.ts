import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {DefaultColor2} from "@openremote/core";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const simpleBarStyle = require("simplebar/dist/simplebar.css");
const elevationStyle = require("@material/elevation/dist/mdc.elevation.css");

// language=CSS
const style = css`
    
    :host {
        --internal-or-panel-background-color: var(--or-panel-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-panel-padding: var(--or-panel-padding, 10px);
        --internal-or-panel-heading-margin: var(--or-panel-heading-margin, 0 0 5px 7px);
        --internal-or-panel-border: var(--or-panel-border, 1px solid #e5e5e5);
        --internal-or-panel-border-radius: var(--or-panel-border-radius, 5px);
        --internal-or-panel-heading-font-size: var(--or-panel-heading-font-size, larger);
        
        display: block;
    }

    :host([hidden]) {
        display: none;
    }
        
    #wrapper {
        height: 100%;
        flex: 1;
    }
    
    #panel {
        padding: var(--internal-or-panel-padding);
        background-color: var(--internal-or-panel-background-color);
        border: var(--internal-or-panel-border);
        border-radius: var(--internal-or-panel-border-radius);
    }
    
    strong {
        margin: var(--internal-or-panel-heading-margin);
        font-size: var(--internal-or-panel-heading-font-size);
    }
`;

@customElement("or-panel")
export class OrPanel extends LitElement {

    static get styles() {
        return [
            css `${unsafeCSS(elevationStyle)}`,
            css `${unsafeCSS(simpleBarStyle)}`,
            style
        ];
    }

    @property({type: Number})
    zLevel?: number;

    @property({type: String})
    public heading?: string;

    @query("#panel")
    protected _panel!: HTMLDivElement;

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);

        // if (this._panel) {
        //     new SimpleBar(this._panel, {
        //         autoHide: this.autoHide,
        //         // @ts-ignore
        //         forceVisible: this.forceVisible
        //     });
        // }
    }

    render() {

        return html`
            <div id="wrapper">
                <div id="panel">
                    ${this.heading ? html`<strong>${this.heading}</strong>` : ``}
                    <slot></slot>
                </div>
            </div>
        `;
    }
}
