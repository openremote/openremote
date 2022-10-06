import { html, LitElement, css } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-realm-card";
import {customElement} from "lit/decorators.js";


@customElement("or-conf-realm")
export class OrConfRealm extends LitElement {

  static styles = css`
    :host {
      --or-panel-background-color: #fff;
    }
    `;

  render() {
    return html`
      <or-conf-realm-card></or-conf-realm-card>
      <or-mwc-input class="btn-add-realm" id="name-input" .type="${InputType.BUTTON}" label="Add Realm" icon="plus"></or-mwc-input>
    `
  }
}
