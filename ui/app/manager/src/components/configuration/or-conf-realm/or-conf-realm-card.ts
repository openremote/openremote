/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { css, html, LitElement, unsafeCSS } from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-file-uploader";
import {
    DEFAULT_LANGUAGES,
    DefaultColor1,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    DefaultColor6,
    DefaultColor8,
    manager,
    Util,
} from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { FileInfo, ManagerAppRealmConfig } from "@openremote/model";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {when} from 'lit/directives/when.js';
import ISO6391 from 'iso-639-1';
import {DefaultHeaderMainMenu, DefaultHeaderSecondaryMenu} from "../../../index";

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

      @media screen and (max-width: 768px) {
        .logo-group or-file-uploader {
          min-width: calc(50% - 6px);
          padding: 0 12px 12px 0!important;
        }

        .logo-group or-file-uploader:nth-child(2n + 2){
          padding: 0 0 12px 0!important;
        }

        .logo-group .d-inline-flex{
          display: flex;
          flex-wrap: wrap;
        }
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

    @property({ attribute: false })
    public realm: ManagerAppRealmConfig = {};

    @property({ attribute: true })
    public name: string = "";

    @property({type: Boolean})
    expanded: boolean = false;

    @property()
    public canRemove: boolean = false;

    protected logo:string = this.realm.logo;
    protected logoMobile:string = this.realm.logoMobile;
    protected favicon:string = this.realm.favicon;
    protected headerListPrimary: string[] = Object.keys(DefaultHeaderMainMenu);
    protected headerListSecondary: string[] = Object.keys(DefaultHeaderSecondaryMenu);

    protected commonLanguages: string[] = Object.entries(DEFAULT_LANGUAGES).map(entry => ISO6391.getName(entry[0]));
    protected _languages: string[][] = [];

    connectedCallback() {
        const languageNames = ISO6391.getAllNames();
        this._languages = ISO6391.getAllCodes()
            .map((code, index) => ([code, languageNames[index]]))
            .sort((a, b) => a[1].localeCompare(b[1]));

        return super.connectedCallback();
    }

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
        Object.entries(colors).forEach(([key, value]) => {
            css += key +":" +value + ";"
        })
        css += "}";
        this.realm.styles = css;
        this.notifyConfigChange(this.realm);
    }

    protected _setHeader(keys: string[], list: string[]) {
        if (!this.realm.headers) {
            this.realm.headers = this.headerListSecondary.concat(this.headerListPrimary);
        }
        this.realm.headers = this.realm.headers?.filter(function(ele) {
            return !list.includes(ele);
        });
        this.realm.headers = this.realm.headers?.concat(keys);
        this.notifyConfigChange(this.realm);
    }

    protected _getImagePath(file:File, fileName: string){
        if (file.type.startsWith("image/")){
            const extension = file.name.slice(file.name.lastIndexOf('.'), file.name.length);
            return this.name + "/" + fileName + extension
        }
        return null
    }

    protected files: {[name:string] : FileInfo} = {}

    protected async _setImageForUpload(file: File, fileName: string) {
        const path = this._getImagePath(file, fileName)
        if (path){
            this.files[fileName] = {
                name: fileName,
                path: path,
                contents: await Util.blobToBase64(file),
            } as FileInfo;
            this.realm[fileName] = path
            this[fileName] = this.files[fileName].contents
            this.requestUpdate()
            this.notifyConfigChange(this.realm);
            return this.files[fileName].contents;
        }
    }

    // Public GET method for getting the files uploaded by the user.
    public getFiles() {
        return this.files;
    }

    protected _showRemoveRealmDialog(){

        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                default: true,
                actionName: "ok",
                content: "yes",
                action: () => {
                    this.dispatchEvent(new CustomEvent("remove"));
                }},

        ];
        showDialog(new OrMwcDialog()
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

    protected notifyConfigChange(config: ManagerAppRealmConfig) {
        this.dispatchEvent(new CustomEvent("change", { detail: config }));
    }


    render() {
        const colors = this._getColors();
        const app = this;
        const managerUrl = (manager.managerUrl ?? "");

        // On an empty search; return the common language as set in DEFAULT_LANGUAGES
        // If searching, compare strings using lowercase. (with no maximum)
        const searchProvider = (search: string) => this._languages.filter(entry =>
            (!search && this.commonLanguages.includes(entry[1])) ||
            (!search && app.realm.notifications?.languages.includes(entry[0])) ||
            entry[1].toLowerCase().includes(search?.toLowerCase()))

        return html`
            <or-collapsible-panel .expanded="${app.expanded}">
                <div slot="header" class="header-container">
                    ${app.name}
                </div>
                <div slot="content" class="panel-content">
                    <div class="subheader">${i18next.t("configuration.main")}</div>
                    <or-mwc-input class="appTitle" .type="${InputType.TEXT}" .label="${i18next.t("configuration.realmTitle")}" value="${app.realm?.appTitle}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => { 
                                      app.realm.appTitle = e.detail.value;
                                      app.notifyConfigChange(app.realm);
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input class="language" .type="${InputType.SELECT}" .label="${i18next.t("configuration.defaultLanguage")}" value="${app.realm?.language}"
                                  .options="${Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => [key, i18next.t(value)])}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      app.realm.language = e.detail.value;
                                      app.notifyConfigChange(app.realm);
                                  }}"
                    ></or-mwc-input>
                    <div class="logo-group">
                        <div class="subheader">${i18next.t("configuration.images")}</div>
                        <div class="d-inline-flex">
                            <or-file-uploader .title="${i18next.t('configuration.logo')}"
                                              @change="${async (e: CustomEvent) => await app._setImageForUpload(e.detail.value[0], "logo")}"
                                              .src="${app.logo ? app.logo : app.realm.logo}" .managerUrl="${managerUrl}"></or-file-uploader>
                            <or-file-uploader .title="${i18next.t('configuration.logoMobile')}"
                                              @change="${async (e: CustomEvent) => await app._setImageForUpload(e.detail.value[0], "logoMobile")}"
                                              .src="${app.logoMobile ? app.logoMobile : app.realm.logoMobile}" .managerUrl="${managerUrl}"></or-file-uploader>
                            <or-file-uploader .title="${html`Favicon`}"
                                              @change="${async (e: CustomEvent) => await app._setImageForUpload(e.detail.value[0], "favicon")}"
                                              .src="${app.favicon ? app.favicon : app.realm.favicon}" .managerUrl="${managerUrl}"></or-file-uploader>
                        </div>
                    </div>
                    <div class="color-group">
                        <div class="subheader">${i18next.t('configuration.realmColors')}</div>
                        <div>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color4"]}"
                                          .label="${i18next.t('configuration.--or-app-color4')}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color4", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color5"]}"
                                          .label="${i18next.t('configuration.--or-app-color5')}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color5", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color6"]}"
                                          .label="${i18next.t("configuration.--or-app-color6")}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color6", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color1"]}"
                                          .label="${i18next.t("configuration.--or-app-color1")}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color1", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color2"]}"
                                          .label="${i18next.t("configuration.--or-app-color2")}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color2", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color3"]}"
                                          .label="${i18next.t("configuration.--or-app-color3")}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color3", e.detail.value)}"></or-mwc-input>
                            <or-mwc-input class="color-item" .type="${InputType.COLOUR}" value="${colors["--or-app-color8"]}"
                                          .label="${i18next.t("configuration.--or-app-color8")}"
                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setColor("--or-app-color8", e.detail.value)}"></or-mwc-input>
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
                                    .value="${!!app.realm.headers ? app.realm.headers?.filter(function(ele: string) {
                                        return app.headerListPrimary.includes(ele);
                                    }) : app.headerListPrimary}"
                                    .options="${app.headerListPrimary}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setHeader(e.detail.value, app.headerListPrimary)}"
                            ></or-mwc-input>
                            <or-mwc-input
                                    .type="${InputType.SELECT}" multiple
                                    class="header-item"
                                    .label="${i18next.t("configuration.secondaryNavigation")}"
                                    .value="${!!app.realm.headers ? app.realm.headers?.filter(function(ele: string) {
                                        return app.headerListSecondary.includes(ele);
                                    }) : app.headerListSecondary}"
                                    .options="${app.headerListSecondary}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => app._setHeader(e.detail.value, app.headerListSecondary)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                    <div class="header-group">
                        <div class="subheader">${i18next.t("configuration.notificationLanguages")}</div>
                        <span>${i18next.t("configuration.notificationLanguagesDesc")}</span>
                        <div>
                            <or-mwc-input
                                    .type="${InputType.SELECT}" multiple
                                    class="header-item"
                                    searchLabel="configuration.notificationLanguagesLabel"
                                    .label="${i18next.t("configuration.notificationLanguages")}"
                                    .value="${app.realm.notifications?.languages || []}"
                                    .options="${this._languages}"
                                    .searchProvider="${searchProvider}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                        const newLanguages: string[] | undefined = e.detail.value;
                                        const currentDefault: string | undefined = app.realm.notifications?.defaultLanguage;
                                        app.realm.notifications = {
                                            languages: newLanguages,
                                            defaultLanguage: newLanguages?.includes(currentDefault) ? currentDefault : newLanguages?.[0]
                                        }
                                        app.notifyConfigChange(app.realm);
                                        this.requestUpdate(); // force render
                                    }}"
                            ></or-mwc-input>
                            ${when(app.realm.notifications?.languages?.length > 0, () => html`
                                <or-mwc-input
                                        .type="${InputType.SELECT}"
                                        class="header-item"
                                        .label="${i18next.t("configuration.defaultLanguage")}"
                                        .value="${app.realm.notifications?.defaultLanguage || []}"
                                        .options="${this._languages.filter(entry => app.realm.notifications?.languages.includes(entry[0]))}"
                                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                            app.realm.notifications.defaultLanguage = e.detail.value;
                                            app.notifyConfigChange(app.realm);
                                            this.requestUpdate(); // force render
                                        }}"
                                ></or-mwc-input>
                            `, () => html`
                                <or-mwc-input
                                        .type="${InputType.SELECT}" disabled
                                        class="header-item"
                                        .label="${i18next.t("configuration.defaultLanguage")}"
                                ></or-mwc-input>
                            `)}
                        </div>
                    </div>

                    ${when(app.canRemove, () => html`
                        <or-mwc-input outlined id="remove-realm" .type="${InputType.BUTTON}"
                                      label="configuration.deleteRealmCustomization"
                                      @or-mwc-input-changed="${() => { app._showRemoveRealmDialog(); }}"
                        ></or-mwc-input>
                    `)}
                </div>
            </or-collapsible-panel>
        `;
    }
}
