import {translate} from "./translate-mixin";
import i18next, {InitOptions, TOptions } from "i18next";
import {LitElement, html, css} from "lit";
import {customElement, property} from "lit/decorators.js";

export {i18next};

export {translate};

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
    public options?: TOptions<InitOptions>;

    protected render() {
        return html`
            ${this._getTranslatedValue()}
        `;
    }

    protected _getTranslatedValue() {
        return this.value ? i18next.isInitialized ? i18next.t(this.value, this.options) : this.value : "";
    }
}
