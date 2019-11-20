import {customElement, html, LitElement, property, PropertyValues, TemplateResult, css} from "lit-element";
import "@openremote/or-select";
import "@openremote/or-icon";
import "@openremote/or-translate";

@customElement("or-modal")
export class OrModal extends LitElement {

    @property({type: Boolean, reflect: true})
    public show: boolean = false;

    @property({type: Boolean})
    public dismissible: boolean = true;

    public static get styles() {
        return css`
            :host {
                position: relative;            
            }
            
            div[hidden] {
                height: 0px;
            }
            
            div {
                transition: all 0.8s ease-in;
                position: absolute;
                height: auto;
            }
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("show") && this.dismissible) {
            if (this.show) {
                document.body.addEventListener("click", this._onMouseClick);
            } else {
                document.body.removeEventListener("click", this._onMouseClick)
            }
        }
    }

    protected _onMouseClick(evt: MouseEvent) {
        if (!this.contains(evt.target as Node)) {
            this.show = false;
        }
    }

    protected render(): TemplateResult | void {
            return html`<div ?hidden="${!this.show}">TEST</div>`;
    }
}