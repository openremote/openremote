import { css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import manager from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import { Store } from "@reduxjs/toolkit";
import { AppStateKeyed, Page, PageProvider } from "@openremote/or-app";
import "@openremote/or-components/or-collapsible-panel";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-configuration/or-conf-json";
import "@openremote/or-configuration/or-conf-realm/index";
import { ManagerConf } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";

export function pageConfigurationProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "configuration",
        routes: [
            "/configuration",
        ],
        pageCreator: () => {
            return new PageConfiguration(store);
        }
    };
}

@customElement("page-configuration")
export class PageConfiguration extends Page<AppStateKeyed>  {

    static get styles() {
        // language=CSS
        return css`
            @media screen and (max-width: 768px) {
                or-panel {
                    border-left: 0px;
                    border-right: 0px;
                    width: 100%!important;
                    border-radius: 0px;
                }
                #header-wrapper{
                    width: calc(100% - 30px)!important;
                }
            }

            :host {
                --or-collapisble-panel-background-color: #fff;
                --or-panel-background-color: #fff;
            }

            or-panel {
                width: calc(100% - 90px);
                max-width: 1310px;
            }

            .conf-category-content-container {
                display: flex;
                min-width: 0px;
                width: 100%;
                height: 100%;
                flex-direction: column;
                align-items: center;
            }

            #header {
                width: 100%;
            }

            #header-wrapper {
                display: flex;
                width: calc(100% - 90px);
                max-width: 1310px;
                flex-direction: row;
                align-items: center;
                margin: 20px auto;
            }

            #header-title {
                font-size: 18px;
                font-weight: bold;
            }

            #header-title > or-icon {
                margin-right: 10px;
            }
            #header-actions {
                flex: 1 1 auto;
                text-align: right;
            }
            #header-actions-content {
                display: flex;
                flex-direction: row;
                align-items: center;
                float: right;
            }
            #header-actions-content or-mwc-input{
                margin-left: 6px;
            }
        `;
    }

    get name(): string {
        return "configuration";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    protected _showReloadDialogDialog() {
        const dialogActions: DialogAction[] = [
            {
                action: () => {
                    if ('submit' in document.getElementById("forceReloadForm")){
                        // @ts-ignore
                        document.getElementById("forceReloadForm").submit();
                    }
                },
                actionName: "ok",
                content: i18next.t("reload"),
                default: true,
            },

        ];
        const dialog = showDialog(new OrMwcDialog()
          .setHeading(i18next.t('reload'))
          .setActions(dialogActions)
          .setContent(html`${i18next.t('configuration.reloadPage')}
          <form id="forceReloadForm" method="POST"></form>`)
          .setStyles(html`
              <style>
                  .mdc-dialog__surface {
                      padding: 4px 8px;
                  }
              </style>
          `)
          .setDismissAction(null));

    }

    protected render(): TemplateResult | void {

        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }



        const app = this
        document.addEventListener('saveLocalManagerConfig', (e:CustomEvent) => {
            manager.managerAppConfig = e.detail?.value as ManagerConf
            app.requestUpdate()
        })

        document.addEventListener('saveManagerConfig', (e:CustomEvent) => {
            manager.rest.api.ConfigurationResource.update(e.detail?.value as ManagerConf).then(()=>{
                app._showReloadDialogDialog()
                manager.managerAppConfig = e.detail?.value as ManagerConf
                app.requestUpdate()
            })
        })

        const managerConfiguration = manager.managerAppConfig

        return html`
            <div class="conf-category-content-container">
                <div id="header">
                    <div id="header-wrapper">
                        <div id="header-title">
                            <or-icon icon="cog"></or-icon>
                            ${i18next.t('configuration.')}
                        </div>
                        <div id="header-actions">
                            <div id="header-actions-content">
                                <or-conf-json .managerConfig="${managerConfiguration}"></or-conf-json>
                                <or-mwc-input id="save-btn" raised="" type="button" label="Opslaan" @click="${() => {
                                    document.dispatchEvent(new CustomEvent("saveManagerConfig", { detail: { value: managerConfiguration } }));
                                }}"></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                <or-panel .heading="${"Realms"}">
                    <or-conf-realm .config="${managerConfiguration}"></or-conf-realm>
                </or-panel>
            </div>
        `;


    }

    public stateChanged(state: AppStateKeyed) {
    }
}
