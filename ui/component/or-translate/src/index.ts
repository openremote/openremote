import {translate} from "./translate-mixin";
import i18next, {InitOptions, TOptions } from "i18next";
import {LitElement, html, css} from "lit";
import {customElement, property} from "lit/decorators.js";

export {i18next};

export {translate};

/**
 * # Translate
 * ### `<or-translate>` - `OrTranslate`
 *
 * Utility component that dynamically translates the value attribute using the [i18next](https://www.i18next.com) library. <br />
 * Useful throughout web apps, as it automatically responds to language changes.
 *
 * Based on the selected language in cache, it will look up the `/locales` folder with the `or.json` file, and translate by key. <br />
 * Location of the translation folder;
 * - **OR Manager:** `/manager/src/web/shared/locales/<lang>/app.json`
 * - **Custom projects:** `/ui/app/<your app>/locales/<lang>/app.json`
 *
 * The HTML content is a simple text, with no <span> or similar wrapper.
 */
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
