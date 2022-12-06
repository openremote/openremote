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
  DefaultColor8
} from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { FileInfo, ManagerConfRealm, ManagerHeaders } from "@openremote/model";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";


@customElement("or-conf-realm-card")
export class OrConfRealmCard extends LitElement {

  static styles = css`
    .language {
      width: 100%;
      padding: 10px 0px;
      max-width: 800px;
    }

    .appTitle {
      width: 100%;
      max-width: 800px;
      padding: 10px 0px;
    }

    .header-group .header-item {
      width: 100%;
      padding: 10px 0px;
      max-width: 800px;
    }

    .color-group .color-item {
      width: 100%;
    }

    .logo-group {
      width: 100%;
    }

    .logo-group or-file-uploader {
      padding: 0 24px 12px 0;
    }

    #remove-realm {
      margin: 12px 0 0 0;
    }

    .subheader {
      padding: 10px 0 4px;
      font-weight: bolder;
    }

    .d-inline-flex {
      display: inline-flex;
    }

    .panel-content {
      padding: 0 24px 24px;
    }

    .description {
      font-size: 12px;
    }

    or-collapsible-panel {
      margin-bottom: 10px;
    }
  `;

  @property({attribute: false})
  public realm: ManagerConfRealm = {
    appTitle: "OpenRemote Demo",
    language: "en",
    styles: "",
    headers: [],
  };

  @property({ attribute: true })
  public name: string = "";

  @property({type: Boolean})
  expanded: boolean = false;

  @property({ attribute: true })
  public onRemove: CallableFunction = () => {
  };

  protected headerListPrimary = [
    ManagerHeaders.map,
    ManagerHeaders.assets,
    ManagerHeaders.rules,
    ManagerHeaders.insights,
  ];


  protected headerListSecondary = [
    ManagerHeaders.gateway,
    ManagerHeaders.export,
    ManagerHeaders.logs,
    ManagerHeaders.realms,

    ManagerHeaders.users,
    ManagerHeaders.roles,

    ManagerHeaders.account,
    ManagerHeaders.language,
    ManagerHeaders.appearance,
    ManagerHeaders.logout

  ];

  protected _getColors() {
    const colors: { [name: string]: string } = {
      "--or-app-color1": unsafeCSS(DefaultColor1).toString(),
      "--or-app-color2": unsafeCSS(DefaultColor2).toString(),
      '--or-app-color3': unsafeCSS(DefaultColor3).toString(),
      '--or-app-color4': unsafeCSS(DefaultColor4).toString(),
      '--or-app-color5': unsafeCSS(DefaultColor5).toString(),
      '--or-app-color6': unsafeCSS(DefaultColor6).toString(),
      '--or-app-color8': unsafeCSS(DefaultColor8).toString()
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

  protected _setHeader(keys:ManagerHeaders[], list: ManagerHeaders[]){
    if (!this.realm.headers){
      this.realm.headers = this.headerListSecondary.concat(this.headerListPrimary)
    }
    this.realm.headers = this.realm.headers?.filter(function(ele){
      return !list.includes(ele);
    });
    this.realm.headers = this.realm.headers?.concat(keys)
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
      case "image/svg+xml":
        extension = "svg"
        break;
    }
    return "/images/" + this.name + "/" + fileName + "." +  extension
  }

  protected files: {[name:string] : FileInfo} = {}

  protected async _setImageForUpload(file: File, fileName: string) {
    const path = this._getImagePath(file, fileName)
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


  protected _showRemoveRealmDialog(){

    const dialogActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: i18next.t("cancel")
      },
      {
        default: true,
        actionName: "ok",
        content: i18next.t("yes"),
        action: () => {this.onRemove()}},

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setHeading(i18next.t('delete'))
      .setActions(dialogActions)
      .setContent(html `
        ${i18next.t('configuration.deleteRealmCustomizationConfirm')}
      `)
      .setStyles(html`
                        <style>
                            .mdc-dialog__surface {
                              padding: 4px 8px;
                            }
                            #dialog-content {
                              padding: 24px;
                            }
                        </style>
                    `)
      .setDismissAction(null));

  }


  render() {
    const colors = this._getColors();
    const app = this;
    document.addEventListener('saveManagerConfig', () => {
      Object.entries(this.files).map(async ([x, y]) => {
        await manager.rest.api.ConfigurationResource.fileUpload(y, { path: x })
      })
    });

    return html`
      <or-collapsible-panel
        .expanded="${this.expanded}">
        <div slot="header" class="header-container">
          ${this.name}
        </div>
        <div slot="content" class="panel-content">
          <div class="subheader">${i18next.t("configuration.main")}</div>
          <or-mwc-input class="appTitle" .type="${InputType.TEXT}" value="${this.realm?.appTitle}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.realm.appTitle = e.detail.value}"
                        .label="${i18next.t("configuration.realmTitle")}"></or-mwc-input>
          <or-mwc-input class="language" .type="${InputType.SELECT}" value="${this.realm?.language}"
                        .options="${Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => {
                          return [key, i18next.t(value)];
                        })}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.realm.language = e.detail.value}"
                        .label="${i18next.t("configuration.defaultLanguage")}"></or-mwc-input>
          <div class="logo-group">
            <div class="subheader">${i18next.t("configuration.images")}</div>
            <div class="d-inline-flex">
              <or-file-uploader .title="${i18next.t('configuration.logo')}"
                                @change="${async (e: CustomEvent) => this.realm.logo = await this._setImageForUpload(e.detail.value[0], "logo")}"
                                .src="${this.realm?.logo}"></or-file-uploader>
              <or-file-uploader .title="${i18next.t('configuration.logoMobile')}"
                                @change="${async (e: CustomEvent) => this.realm.logoMobile = await this._setImageForUpload(e.detail.value[0], "logoMobile")}"
                                .src="${this.realm?.logoMobile}"></or-file-uploader>
              <or-file-uploader .title="${html`Favicon`}"
                                @change="${async (e: CustomEvent) => this.realm.favicon = await this._setImageForUpload(e.detail.value[0], "favicon")}"
                                .src="${this.realm?.favicon}"></or-file-uploader>
            </div>
          </div>
          <div class="color-group">
            <div class="subheader">${i18next.t('configuration.realmColors')}</div>
            <div>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color4"]}"
                            .label="${i18next.t('configuration.--or-app-color4')}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color4", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color5"]}"
                            .label="${i18next.t('configuration.--or-app-color5')}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color5", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color6"]}"
                            .label="${i18next.t("configuration.--or-app-color6")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color6", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color1"]}"
                            .label="${i18next.t("configuration.--or-app-color1")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color1", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color2"]}"
                            .label="${i18next.t("configuration.--or-app-color2")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color2", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color3"]}"
                            .label="${i18next.t("configuration.--or-app-color3")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color3", e.detail.value)}"></or-mwc-input>
              <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color8"]}"
                            .label="${i18next.t("configuration.--or-app-color8")}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setColor("--or-app-color8", e.detail.value)}"></or-mwc-input>
            </div>
          </div>
          <div class="header-group">
            <div class="subheader">${i18next.t("configuration.navigation")}</div>
            <span>${i18next.t("configuration.navigationDescription")}</span>
            <div>
              <or-mwc-input
                .type="${InputType.SELECT}" multiple
                class="header-item"
                .label="${i18next.t("configuration.primaryNavigation")}"
                .value="${!!this.realm.headers ? this.realm.headers?.filter(function(ele) {
                  return app.headerListPrimary.includes(ele);
                }) : this.headerListPrimary}"
                .options="${this.headerListPrimary}"
                @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setHeader(e.detail.value, this.headerListPrimary)}"
              ></or-mwc-input>
              <or-mwc-input
                .type="${InputType.SELECT}" multiple
                class="header-item"
                .label="${i18next.t("configuration.secondaryNavigation")}"
                .value="${!!this.realm.headers ? this.realm.headers?.filter(function(ele) {
                  return app.headerListSecondary.includes(ele);
                }) : this.headerListSecondary}"
                .options="${this.headerListSecondary}"
                @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setHeader(e.detail.value, this.headerListSecondary)}"
              ></or-mwc-input>
            </div>
          </div>

          <or-mwc-input outlined id="remove-realm" .type="${InputType.BUTTON}" .label="${i18next.t("configuration.deleteRealmCustomization")}"
                        @click="${() => {
                          this._showRemoveRealmDialog();
                        }}"></or-mwc-input>
        </div>
      </or-collapsible-panel>
    `;
  }
}
