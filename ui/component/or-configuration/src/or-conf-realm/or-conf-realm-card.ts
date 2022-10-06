import { css, html, LitElement } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { customElement, property } from "lit/decorators.js";
import { ManagerRealmConfig, HeaderNames } from "@openremote/core";
import { DEFAULT_LANGUAGES } from "@openremote/or-app";
import { i18next } from "@openremote/or-translate";


@customElement("or-conf-realm-card")
export class OrConfRealmCard extends LitElement {

  static styles = css`
    or-collapsible-panel img{
      max-width: 16px;
    }
    .btn-add-realm{
      margin-top: 48px;
      width: 100%;
      text-align: center;
    }
    .content{
      padding: 10px;
    }
    or-mwc-input{
      width: 100%;
      margin: 10px 0;
    }
    .row{
      display: flex;
      -ms-flex-wrap: wrap;
      flex-wrap: wrap;
      margin-right: -15px;
      margin-left: -15px;
    }
    .col{
      -ms-flex: 0 0 50%;
      flex: 0 0 50%;
      max-width: 50%;
    }
    .col or-mwc-input{
      margin: 10px;
    }
    
    or-collapsible-panel{
      border-radius: 4px;
    }
    .header-container{
      display: inline-flex;
    }

    .panels>.panels:not(:last-child)>or-collapsible-panel, .panels>or-collapsible-panel:not(:last-child) {
      border-bottom-right-radius: 0;
      border-bottom-left-radius: 0;
    }

    .panels>.panels:not(:first-child)>or-collapisble-panel, .panels>or-collapsible-panel:not(:first-child) {
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }
  `;

  @property({attribute: false})
  public realm: ManagerRealmConfig = {
    appTitle: "OpenRemote Demo",
    language: "en",
    headers:[]
  };

  @property({attribute: true})
  public name: string = "";

  render() {
    const realm = this.realm
    console.log(DEFAULT_LANGUAGES)
    return html`
      <or-collapsible-panel>
        <div slot="header" class="header-container">
          <img src="${this.realm.favicon}" alt="${this.realm.appTitle}">
          <strong>${this.name}</strong>
        </div>
        <div slot="content">
          <or-mwc-input .type="${InputType.TEXT}" value="${this.realm?.appTitle}" label="App Title"></or-mwc-input>
          <or-mwc-input .type="${InputType.SELECT}" value="${this.realm?.language}" .options="${Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => {return [key, i18next.t(value)]})}" label="Default language"></or-mwc-input>
          ${Object.entries(HeaderNames).map(function([key, value]){
            return html`<or-mwc-input .type="${InputType.CHECKBOX}" class="col" label="${key}" .value="${realm.headers !== undefined ? realm.headers.includes(HeaderNames[value]) : false }"></or-mwc-input>`
          })}
          <div class="content row">
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
            <div class="col">
              <or-mwc-input .type="${InputType.COLOUR}" value="Test" label="App Color 1"></or-mwc-input>
            </div>
          </div>
        </div>
        <or-mwc-input id="name-input" .type="${InputType.BUTTON}" label="Save"></or-mwc-input>
      </or-collapsible-panel>
`;
  }
}
