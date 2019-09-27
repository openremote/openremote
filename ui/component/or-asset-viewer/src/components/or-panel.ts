import {css, customElement, html, LitElement, PropertyValues, query, unsafeCSS, property} from "lit-element";
import "@openremote/or-select";
import "@openremote/or-icon";
import "@openremote/or-translate";
import SimpleBar from "simplebar";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const scrollStyle = require("simplebar/dist/simplebar.css");

// language=CSS
const style = css`
    :host {
        display: block;
    }

    :host([hidden]) {
        display: none;
    }
    
    #panel {
        height: 100%;
    }
`;

@customElement("or-panel")
export class OrPanel extends LitElement {

    static get styles() {
        return [
            css `${unsafeCSS(scrollStyle)}`,
            style
        ];
    }

    @property({type: Boolean})
    autoHide = true;

    @property({type: Boolean})
    forceVisible?: boolean;

    @query("#panel")
    protected _panel!: HTMLDivElement;

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);

        if (this._panel) {
            new SimpleBar(this._panel, {
                autoHide: this.autoHide,
                // @ts-ignore
                forceVisible: this.forceVisible
            });
        }
    }

    render() {
        return html`
            <div id="panel"><slot></slot></div>
        `;
    }
}