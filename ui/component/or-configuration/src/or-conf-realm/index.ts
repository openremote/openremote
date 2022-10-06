import { html, LitElement, css } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-realm-card";
import {customElement, property} from "lit/decorators.js";
import { ManagerRealmConfig } from "@openremote/core";


@customElement("or-conf-realm")
export class OrConfRealm extends LitElement {

  static styles = css`
    :host {
      --or-panel-background-color: #fff;
    }
    `;

  @property({attribute: false})
  public realms: { [name: string]: ManagerRealmConfig } = {};

  render() {

    return html`
      ${Object.entries(this.realms === undefined ? {} : this.realms).map(function([key , value]){
        return html`<or-conf-realm-card .name="${key}" .realm="${value}"></or-conf-realm-card>`
      })}
      
      <or-mwc-input class="btn-add-realm" id="name-input" .type="${InputType.BUTTON}" label="Add Realm" icon="plus"></or-mwc-input>
    `
  }
}
