import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

@customElement("confirmation-dialog")
export class ConfirmationDialog extends translate(i18next)(LitElement) {
    @property({ type: String }) public agreeText = i18next.t("ok");
    @property({ type: String }) public disagreeText = i18next.t("cancel");
    @property({ type: String }) public question = i18next.t("areYouSure", "Are you sure?");

    public static get styles() {
        return css`
        .question{
            width: 100%;
            padding: 15px 5px 25px 5px;
            text-align: center;
        }
        .container{
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            justify-content: space-evenly;
            justify-content: space-around;
        }`;
    }

    protected render() {
        return html`
        <div class="question">${this.question}</div>
        <div class="container">
            <or-mwc-input type="${InputType.BUTTON}" unElevated label="${this.agreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("agreed")); }}">
            </or-mwc-input>

            <or-mwc-input type="button" label="${this.disagreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("disagreed")); }}">
            </or-mwc-input>
        </div>
        `;
    }
}
