import {translate} from "./translate-mixin";
import i18next from "i18next";
import {LitElement, customElement, property, html, css} from "lit-element";

@customElement("or-translate")
export class OrTranslate extends translate(i18next)(LitElement) {

    public static styles = css`
        :host {
            display: inline-block;
        }
        
        :host([hidden]) {
            display: none;
        }
    `;

    @property({type: String})
    public value?: string;

    @property({type: Object})
    public options?: i18next.TOptions<i18next.InitOptions>;

    protected render() {
        return html`
            ${this._getTranslatedValue()}
        `;
    }

    protected _getTranslatedValue() {
        return this.value ? i18next.t(this.value, this.options) : "";
    }
}
