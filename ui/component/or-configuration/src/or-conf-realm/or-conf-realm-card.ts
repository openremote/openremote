import { css, html, LitElement, unsafeCSS } from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-file-uploader";
import "@openremote/or-components/or-info";
import manager, {
  DEFAULT_LANGUAGES,
  DefaultColor1,
  DefaultColor2,
  DefaultColor3,
  DefaultColor4,
  DefaultColor5,
  DefaultColor6,
} from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { FileInfo, ManagerConfRealm, ManagerHeaders } from "@openremote/model";


@customElement("or-conf-realm-card")
export class OrConfRealmCard extends LitElement {

  static styles = css`
    .language {
      width: 50%;
      padding: 12px 4px;
    }

    .appTitle {
      width: 50%;
      padding: 12px 4px;
    }

    .header-group {
      width: 50%;
    }

    .header-group .header-item {
      width: 49%;
    }

    .color-group {
      width: 100%;
    }

    .color-group .color-item {
      width: 40%;
      margin: 4px 8px;
    }

    .logo-group {
      width: 100%;
    }

    .logo-group or-file-uploader {
      margin: 8px;
      width: 25%;
    }

    #remove-realm {
      margin: 8px 4px;
    }

    .subheader {
      margin: 4px 8px;
      font-weight: bold;
    }

    .d-inline-flex {
      display: inline-flex;
      width: 100%;
    }
    .collection-2 {
      width: 50%;
    }
    .panel-content{
      padding: 0 36px;
    }
  `;

  @property({attribute: false})
  public realm: ManagerConfRealm = {
    appTitle: "OpenRemote Demo",
    language: "en",
    styles: "",
    headers:[]
  };

  @property({attribute: true})
  public name: string = "";

  @property({attribute: true})
  public onRemove: CallableFunction = () => {};

  protected headerList = {
    'map': ManagerHeaders.map,
    'assets': ManagerHeaders.assets,
    'rules': ManagerHeaders.rules,
    'insight': ManagerHeaders.insights,
    'realm':ManagerHeaders.realms,

    'language': ManagerHeaders.language,
    'dataExport': ManagerHeaders.export,
    'role': ManagerHeaders.roles,
    'account': ManagerHeaders.account,
    'logs': ManagerHeaders.logs,

    'gatewayConnection': ManagerHeaders.gateway,
    'user' :ManagerHeaders.users,
  }

  protected _getColors(){
    //TODO settings default colors
    const colors : {[name:string] : string} = {
      '--or-app-color1': unsafeCSS(DefaultColor1).toString(),
      '--or-app-color2': unsafeCSS(DefaultColor2).toString(),
      '--or-app-color3': unsafeCSS(DefaultColor3).toString(),
      '--or-app-color4': unsafeCSS(DefaultColor4).toString(),
      '--or-app-color5': unsafeCSS(DefaultColor5).toString(),
      '--or-app-color6': unsafeCSS(DefaultColor6).toString(),
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

  protected _setColor(key:string, value:string){
    const colors  = this._getColors()
    colors[key] = value
    let css = ":host > * {"
    Object.entries(colors).map(([key, value]) => {
      css += key +":" +value + ";"
    })
    this.realm.styles = css
  }

  protected _setHeader(key:ManagerHeaders, value:boolean){
    if (!('headers' in this.realm)){
      this.realm.headers = Object.values(this.headerList)
    }
    if (value){
      this.realm.headers?.push(key)
    } else {
      this.realm.headers =  this.realm.headers?.filter(function(ele){
        return ele != key;
      });
    }
  }

  protected _getImagePath(file:File, fileName: string){
    let extension = "";
    switch (file.type){
      case "image/png":
        extension = "png"
        break;
      case "image/jpg":
        extension = "jpg"
        break;
      case "image/jpeg":
        extension = "jpeg"
        break;
      case "image/vnd.microsoft.icon":
        extension = "ico"
        break;
    }
    return "/images/" + this.name + "/" + fileName + "." +  extension
  }

  protected files: {[name:string] : FileInfo} = {}

  protected async _setImageForUpload(file: File, fileName: string) {
    const path = this._getImagePath(file, fileName)
    console.log(path)
    this.files[path] = {
      // name: 'filename',
      contents: await this.convertBase64(file),
      // binary: true
    } as FileInfo;
    return path;
  }

  convertBase64 (file:any) {
    return new Promise((resolve, reject) => {
      const fileReader = new FileReader();
      fileReader.readAsDataURL(file);

      fileReader.onload = () => {
        resolve(fileReader.result);
      };

      fileReader.onerror = (error) => {
        reject(error);
      };
    });
  };

  render() {
    const app = this
    const colors = this._getColors()
    document.addEventListener('saveManagerConfig', () => {
      Object.entries(this.files).map(async ([x, y]) => {
        await manager.rest.api.ConfigurationResource.fileUpload(y, { path: x })
      })
    });

    return html`
      <or-collapsible-panel>
        <div slot="header" class="header-container">
          <strong>${this.name}</strong>
        </div>
        <div slot="content" class="panel-content">
          <div class="d-inline-flex">
            <or-mwc-input class="appTitle" .type="${InputType.TEXT}" value="${this.realm?.appTitle}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.realm.appTitle = e.detail.value}" label="App Title"></or-mwc-input>
            <or-mwc-input class="language" .type="${InputType.SELECT}" value="${this.realm?.language}" .options="${Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => {return [key, i18next.t(value)]})}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.realm.language = e.detail.value}"
                          label="Default language"></or-mwc-input>
          </div>
          <div class="d-inline-flex">
            <div class="header-group">
              <div class="subheader">Headers</div>
              <div>
                ${Object.entries(this.headerList).map(function([key, value]) {
                  return html`
                    <or-mwc-input
                      .type="${InputType.CHECKBOX}"
                      class="header-item" label="${i18next.t(key)}"
                      .value="${!!app.realm.headers ? app.realm.headers?.includes(<ManagerHeaders>value) : true}"
                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setHeader(value, e.detail.value)}"
                    ></or-mwc-input>`;
                })}
              </div>
            </div>
            <div class="collection-2">
              <div class="logo-group">
                <div class="subheader">Logo's</div>
                <div class="d-inline-flex">
                  <or-file-uploader .title="${html`Header <or-info></or-info>`}"
                                    @change="${async (e: CustomEvent) => this.realm.logo = await this._setImageForUpload(e.detail.value[0], "logo")}"
                                    .src="${this.realm?.logo}"></or-file-uploader>
                  <or-file-uploader .title="${html`Header mobile <or-info></or-info>`}"
                                    @change="${async (e: CustomEvent) => this.realm.logoMobile = await this._setImageForUpload(e.detail.value[0], "logoMobile")}"
                                    .src="${this.realm?.logoMobile}"></or-file-uploader>
                  <or-file-uploader .title="${html`Favicon <or-info></or-info>`}"
                                    @change="${async (e: CustomEvent) => this.realm.favicon = await this._setImageForUpload(e.detail.value[0], "favicon")}"
                                    .src="${this.realm?.favicon}"></or-file-uploader>
                </div>
              </div>
              <div class="color-group">
                <div class="subheader">Manager colors</div>
                <div>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color4"]}"
                                label="Primary"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color4", e.detail.value)}"></or-mwc-input>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color5"]}"
                                label="Borders and lines"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color5", e.detail.value)}"></or-mwc-input>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color6"]}"
                                label="Invalid and error"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color6", e.detail.value)}"></or-mwc-input>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color1"]}"
                                label="Surface"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color1", e.detail.value)}"></or-mwc-input>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color2"]}"
                                label="Background"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color2", e.detail.value)}"></or-mwc-input>
                  <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color3"]}"
                                label="Text"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color3", e.detail.value)}"></or-mwc-input>
                </div>
              </div>
            </div>
          </div>

          <or-mwc-input id="remove-realm" .type="${InputType.BUTTON}" .label="${i18next.t("delete")}" @click="${() => {
            this.onRemove();
          }}"></or-mwc-input>
        </div>
      </or-collapsible-panel>
    `;
  }
}
