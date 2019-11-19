import {css, customElement, html, LitElement, PropertyValues, query, unsafeCSS, property} from "lit-element";
import SimpleBar from "simplebar";
import {DefaultColor2} from "@openremote/core";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const simpleBarStyle = require("!!raw-loader!simplebar/dist/simplebar.css");
const elevationStyle = require("!!raw-loader!@material/elevation/dist/mdc.elevation.css");

// language=CSS
const style = css`
    
    :host {
        --internal-or-panel-background-color: var(--or-panel-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-panel-padding: var(--or-panel-padding, 10px);
        --internal-or-panel-heading-margin: var(--or-panel-heading-margin, 0 0 5px 0);
                
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
    }
    
    strong {
        margin: var(--internal-or-panel-heading-margin);
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

    @property({type: Boolean})
    autoHide = true;

    @property({type: Boolean})
    forceVisible?: boolean;

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

        let zLevel = this.zLevel || 1;
        zLevel = Math.min(24, Math.max(0, zLevel));

        return html`
            <div id="wrapper">
                <div id="panel" class="mdc-elevation--z${zLevel}">
                    ${this.heading ? html`<strong>${this.heading}</strong>` : ``}
                    <slot></slot>
                </div>
            </div>
        `;
    }
}