import { css, html, LitElement } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { customElement, property } from "lit/decorators.js";
import { ManagerRealmConfig, HeaderNames, DEFAULT_LANGUAGES } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { Realm } from "@openremote/model";


@customElement("or-conf-realm-card")
export class OrConfRealmCard extends LitElement {

  static styles = css`
    div{
      width: 100%;
    }
    .language{
      width: 40%;
      padding: 8px 4px;
    }
    .appTitle{
      width: 60%;
      padding: 8px 4px;
    }
    .d-inline-flex{
      display: inline-flex;
    }
    .flex-wrap{
      flex-wrap: wrap;
      justify-content: space-between;
    }
    .header-group{
      width: 50%;
    }
    .header-group .header-item{
      width: 50%;
    }
    .color-group{
      width: 100%;
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
    }
    .color-group .color-item{
      width: 33%;
    }
    .logo-group{
      width: 50%;
      height: 300px;
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

  @property({attribute: true})
  public onRemove: CallableFunction = () => {};

  protected _getColors(){
    //TODO settings default colors
    const colors : {[name:string] : string} = {
      '--or-app-color1': '',
      '--or-app-color2': '',
      '--or-app-color3': '',
      '--or-app-color4': '',
      '--or-app-color5': '',
      '--or-app-color6': '',
    }
    if (this.realm?.styles){
      //TODO use regex for filtering and getting color codes CSS
      const css = this.realm.styles.slice(this.realm.styles.indexOf("{") +1, this.realm.styles.indexOf("}"))
      css.split(";").forEach(function(value){
        const col = value.split(":")
        if (col.length >= 2){
          colors[col[0].trim()] = col[1].trim()
        }
      })
    }
    return colors
  }

  render() {
    const realm = this.realm
    return html`
      <or-collapsible-panel>
        <div slot="header" class="header-container">
          <strong>${this.name}</strong>
        </div>
        <div slot="content">
          <div class="d-inline-flex">
            <or-mwc-input class="appTitle" .type="${InputType.TEXT}" value="${this.realm?.appTitle}" label="App Title"></or-mwc-input>
            <or-mwc-input class="language" .type="${InputType.SELECT}" value="${this.realm?.language}" .options="${Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => {return [key, i18next.t(value)]})}" label="Default language"></or-mwc-input>
          </div>
          <div class="d-inline-flex">
            <div class="header-group">
              ${Object.entries(HeaderNames).map(function([key, value]){
                return html`<or-mwc-input .type="${InputType.CHECKBOX}" class="header-item" label="${key}" .value="${realm.headers !== undefined ? realm.headers.includes(HeaderNames[value]) : false }"></or-mwc-input>`
              })}
            </div>
            <div class="logo-group">
              ${this.realm?.favicon}
              ${this.realm?.logo}
              ${this.realm?.logoMobile}
            </div>
          </div>
          <div class="color-group">
            ${Object.entries(this._getColors()).map(function([key, value]){
              return html`<or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${value}" label="${key}"></or-mwc-input>`
            })}
          </div>
          <or-mwc-input id="name-input" .type="${InputType.BUTTON}" label="Remove" @click="${() => {this.onRemove()}}"></or-mwc-input>
        </div>
      </or-collapsible-panel>
`;
  }
}
