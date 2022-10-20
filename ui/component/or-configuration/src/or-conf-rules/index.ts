import { html, LitElement, css } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import {customElement, property} from "lit/decorators.js";
import { RulesetLang } from "@openremote/model";




@customElement("or-conf-rules")
export class OrConfRealm extends LitElement {

  static styles = css`
    `;

  public static DEFAULT_ALLOWED_LANGUAGES = [RulesetLang.JSON, RulesetLang.GROOVY, RulesetLang.JAVASCRIPT, RulesetLang.FLOW]

  @property({attribute: false})
  public rules: {rules?: {controls?: {allowedLanguages?: RulesetLang[]}}} = {};

  render() {
    console.log(this.rules)
    const rules = this.rules?.rules
    return html`
      <or-collapsible-panel>
        <div slot="header">
          Rules
        </div>
        <div slot="content">
          ${Object.entries(OrConfRealm.DEFAULT_ALLOWED_LANGUAGES).map(function([key, value]){
            return html`<or-mwc-input .type="${InputType.CHECKBOX}" class="col" label="${value.toUpperCase()}" .value="${rules?.controls?.allowedLanguages !== undefined ? rules?.controls?.allowedLanguages.includes(value) : false }"></or-mwc-input>`
          })}
        </div>
      </or-collapsible-panel>
    `
  }
}
