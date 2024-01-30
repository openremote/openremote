import {css, html, PropertyValues, TemplateResult, unsafeCSS,} from "lit";
import {createSelector} from "reselect";
import {createRef, Ref, ref} from "lit/directives/ref.js";
import {customElement, property} from "lit/decorators.js";
import manager, {DefaultColor3} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {ClientRole, Realm} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {Util} from "@openremote/core"
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageRealmsProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
  return {
    name: "realms",
    routes: ["realms"],
    pageCreator: () => {
      return new PageRealms(store);
    },
  };
}


@customElement("page-realms")
export class PageRealms extends Page<AppStateKeyed> {

    static get styles() {
    // language=CSS
    return [
      unsafeCSS(tableStyle),
      css`
        #wrapper {
          height: 100%;
          width: 100%;
          display: flex;
          flex-direction: column;
          overflow: auto;
        }

        #title {
          padding: 0 20px;
          font-size: 18px;
          font-weight: bold;
          width: calc(100% - 40px);
          max-width: 1360px;
          margin: 20px auto;
          align-items: center;
          display: flex;
          color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
        }

        #title or-icon {
          margin-right: 10px;
          margin-left: 14px;
        }

        .panel {
          width: calc(100% - 90px);
          padding: 0 20px;
          max-width: 1310px;
          background-color: white;
          border: 1px solid #e5e5e5;
          border-radius: 5px;
          position: relative;
          margin: 0 auto;
          padding: 24px;
        }

        .panel-title {
            text-transform: uppercase;
            font-weight: bolder;
            line-height: 1em;
            color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
            margin-bottom: 20px;
            margin-top: 0;
            flex: 0 0 auto;
            letter-spacing: 0.025em;
        }


        #table-roles,
        #table-roles table{
          width: 100%;
          white-space: nowrap;
        }

        .mdc-data-table__row {
          border-top-color: #D3D3D3;
        }
        
        td, th {
          width: 25%;
          border: none;
        }
  
        td.large, th.large {
          width: 50%
        }

        .realm-container {
          flex-direction: row;
          overflow: hidden;
          max-height: 0;
          transition: max-height 0.25s ease-out;
          padding-left: 16px;
        }

        or-mwc-input {
            margin-bottom: 20px;
            margin-right: 16px;
        }

        or-icon {
            vertical-align: middle;
            --or-icon-width: 20px;
            --or-icon-height: 20px;
            margin-right: 2px;
            margin-left: -5px;
        }

        .row {
            display: flex;
            flex-direction: row;
            margin: 10px 0;
            flex: 1 1 0;
        }

        .column {
            display: flex;
            flex-direction: column;
            margin: 0px;
            flex: 1 1 0;
            
        }

        .mdc-data-table__header-cell {
            font-weight: bold;
            color: ${unsafeCSS(DefaultColor3)};
        }

        .mdc-data-table__header-cell:first-child {
            padding-left: 36px;
        }

        .mdc-data-table__row {
            cursor: pointer;
        }
        
        .padded-cell {
          overflow-wrap: break-word;
          word-wrap: break-word
        }

        .realm-row td {
          padding: 0;
        }

        .realm-row {
          max-width: 0px;
        }

        .realm-row.expanded .realm-container {
          max-height: 1000px;
          max-width: none;
          transition: max-height 1s ease-in;
        }
        
        .button {
            cursor: pointer;
            display: flex;
            flex-direction: row;
            align-content: center;
            padding: 16px;
            align-items: center;
            font-size: 14px;
            text-transform: uppercase;
            color: var(--or-app-color4);
        }

        .button or-icon {
          --or-icon-fill: var(--or-app-color4);
        }

        @media screen and (max-width: 1024px){
          .row {
            display: block;
            flex-direction: column;
          }
        }

        @media screen and (max-width: 768px){
          #title {
            padding: 0;
            width: 100%;
          }
          .panel {
            border-left: 0px;
            border-right: 0px;
            width: calc(100% - 48px);
            border-radius: 0;
          }
          .hide-mobile {
            display: none;
          }
          td, th {
            width: 50%
          }
        }
      `,
    ];
  }

  @property()
  protected _realms: Realm[] = [];

  protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

  get name(): string {
    return "realm_plural";
  }

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            this.requestUpdate();
        }
    )

  constructor(store: Store<AppStateKeyed>) {
    super(store);
    this._getRealms();
  }

  public shouldUpdate(_changedProperties: PropertyValues): boolean {

      if (_changedProperties.has("realm")) {
          this._getRealms();
      }

      return super.shouldUpdate(_changedProperties);
  }

  protected render(): TemplateResult | void {
    if (!manager.authenticated) {
      return html`
        <or-translate value="notAuthenticated"></or-translate>
      `;
    }

    if (!manager.isKeycloak()) {
      return html`
        <or-translate value="notSupported"></or-translate>
      `;
    }

    const readonly = !manager.hasRole(ClientRole.WRITE_USER);
    return html`
         <div id="wrapper">
                <div id="title">
                <or-icon icon="domain"></or-icon>${i18next.t(
                  "realm_plural"
                )}
                </div>
                <div class="panel">
                <p class="panel-title">${i18next.t("realm_plural")}</p>
                  <div id="table-roles" class="mdc-data-table">
                  <table class="mdc-data-table__table" aria-label="realm list" >
                      <thead>
                          <tr class="mdc-data-table__header-row">
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="name"></or-translate></th>
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="displayName"></or-translate></th>
                              <th class="mdc-data-table__header-cell hide-mobile large" role="columnheader" scope="col"><or-translate value="status"></or-translate></th>
                          </tr>
                      </thead>
                      <tbody class="mdc-data-table__content">
                      ${this._realms.map(
                        (realm, index) => {
                       
                          return html`
                          <tr id="mdc-data-table-row-${index}" class="mdc-data-table__row" @click="${(ev) => this.expanderToggle(ev, index)}">
                            <td  class="padded-cell mdc-data-table__cell"
                            >
                              <or-icon id="mdc-data-table-icon-${index}" icon="chevron-right"></or-icon>
                              <span>${realm.name}</span>
                            </td>
                            <td class="padded-cell mdc-data-table__cell">
                              ${realm.displayName}
                            </td>
                            <td class="padded-cell hide-mobile mdc-data-table__cell large">
                            ${realm.enabled ? "Enabled" : "Disabled"}

                            </td>
                          </tr>
                          <tr id="realm-row-${index}" class="realm-row${!realm.id ? " expanded" : ""}">
                            <td colspan="100%">
                              <div class="realm-container">
                                 
                                  <div class="row">
                                    <div class="column">
                                      <or-mwc-input ?readonly="${realm.id}" .label="${i18next.t("realm")}" .type="${InputType.TEXT}" min="1" required .value="${realm.name}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => realm.name = e.detail.value}"></or-mwc-input>
                                      <or-mwc-input ?readonly="${readonly}" .label="${i18next.t("loginTheme", Util.camelCaseToSentenceCase("loginTheme"))}" .type="${InputType.TEXT}" .value="${realm.loginTheme}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => realm.loginTheme = e.detail.value}"></or-mwc-input>
                                      <or-mwc-input ?readonly="${readonly}" .label="${i18next.t("resetPasswordAllowed")}" .type="${InputType.SWITCH}" min="1" .value="${realm.resetPasswordAllowed}" @or-mwc-input-changed="${(e: OrInputChangedEvent) =>realm.resetPasswordAllowed = e.detail.value}"></or-mwc-input>
                                      <or-mwc-input ?readonly="${readonly}" .label="${i18next.t("enabled")}" .type="${InputType.SWITCH}" min="1" .value="${realm.enabled}" @or-mwc-input-changed="${(e: OrInputChangedEvent) =>realm.enabled = e.detail.value}"></or-mwc-input>
                                      <or-mwc-input ?readonly="${readonly}" .label="${i18next.t("rememberMe")}" .type="${InputType.SWITCH}" min="1" .value="${realm.rememberMe}" @or-mwc-input-changed="${(e: OrInputChangedEvent) =>realm.rememberMe = e.detail.value}"></or-mwc-input>
                                    </div>
                                    <div class="column">
                                      <or-mwc-input .label="${i18next.t("displayName")}" .type="${InputType.TEXT}" min="1" required .value="${realm.displayName}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => realm.displayName = e.detail.value}"></or-mwc-input>
                                      <or-mwc-input ?readonly="${readonly}" .label="${i18next.t("emailTheme", Util.camelCaseToSentenceCase("emailTheme"))}" .type="${InputType.TEXT}" .value="${realm.emailTheme}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => realm.emailTheme = e.detail.value}"></or-mwc-input>
                                    </div>
                                  </div>

                                  <div class="row" style="margin-bottom: 0;">
                                  ${realm.id && !readonly ? html`
                                      ${realm.name !== "master" && manager.isSuperUser() ? html`
                                        <or-mwc-input label="delete" .type="${InputType.BUTTON}" .disabled="${manager.displayRealm === realm.name}" @or-mwc-input-changed="${() => this._deleteRealm(realm)}"></or-mwc-input>  
                                      ` : ``}
                                      <or-mwc-input style="margin-left: auto;" label="save" .type="${InputType.BUTTON}" @or-mwc-input-changed="${() => this._updateRealm(realm)}"></or-mwc-input>   
                                  ` : !readonly ? html`
                                    <or-mwc-input label="cancel" .type="${InputType.BUTTON}" @or-mwc-input-changed="${() => {this._realms.splice(-1,1); this._realms = [...this._realms]}}"></or-mwc-input>            
                                    <or-mwc-input style="margin-left: auto;" label="create" .type="${InputType.BUTTON}" @or-mwc-input-changed="${() => this._createRealm(realm)}"></or-mwc-input>   
                                  `: ``}    
                                  </div>
                              </div>
                            </td>
                          </tr>
                        `
                      })}
                        ${this._realms.length > 0 && !!this._realms[this._realms.length -1].id && !readonly ? html`
                        <tr class="mdc-data-table__row">
                          <td colspan="100%">
                            <a class="button" @click="${() => this._realms = [...this._realms, {displayName:"", enabled: true}]}"><or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("realm")}</a>
                          </td>
                        </tr>
                      ` : ``}
                      </tbody>
                  </table>
                </div>

            </div>
            </div>
           
        `;
  }

  public stateChanged(state: AppStateKeyed) {
      this.getRealmState(state);
  }

    protected async _getRealms() {
        const response = await manager.rest.api.RealmResource.getAll();
        this._realms = response.data;
        return this._realms;
    }

    private async _updateRealm(realm) {
        const response = await manager.rest.api.RealmResource.update(realm.name, realm);
        if (response.status === 204) {
            showSnackbar(undefined, "saveRealmSucceeded");
        } else {
            showSnackbar(undefined, "saveRealmFailed");
        }
        //TODO improve this so that header realm picker is updated
        window.location.reload();
    }

    private async _createRealm(realm) {
        await manager.rest.api.RealmResource.create(realm).then(response => {
            if (response.status === 204) {
                showSnackbar(undefined, "saveRealmSucceeded");
            } else {
                showSnackbar(undefined, "saveRealmFailed");
            }
            //TODO improve this so that header realm picker is updated
            window.location.reload();
        });
    }

    private _deleteRealm(realm: Realm) {

      let confirmedName = "";
      let okBtnRef: Ref<OrMwcInput> = createRef();

      const doDelete = async (dialog: OrMwcDialog) => {
        try {
            await manager.rest.api.RealmResource.delete(realm.name);
            this._realms = this._realms.filter(r => r !== realm);
        } catch (e) {
            showSnackbar(undefined, "realmDeleteFailed", "dismiss");
        }
      };

      const inputChanged = (value: string) => {
          confirmedName = value;
          if (okBtnRef.value) {
              okBtnRef.value.disabled = confirmedName !== realm.name;
          }
      };

      const dialogContent = html`<div>
          <p style="text-align: justify; font-weight: bold;">${i18next.t("realmDeleteConfirm", {realmName: realm.name})}</p>
          <or-mwc-input .type="${InputType.TEXT}" @input=${(ev: Event) => inputChanged((ev.target as OrMwcInput).nativeValue)} .label="${i18next.t("realm")}"></or-mwc-input>
      </div>`;

      const dialogActions: DialogAction[] = [
          {
              actionName: "ok",
              content: html`<or-mwc-input .type="${InputType.BUTTON}" ${ref(okBtnRef)} @click="${(ev: MouseEvent) => {if ((ev.currentTarget as OrMwcInput).disabled) ev.stopPropagation()}}" disabled label="ok"></or-mwc-input>`,
              action: doDelete
          },
          {
              default: true,
              actionName: "cancel",
              content: "cancel"
          }
      ];

      const dialog = showDialog(new OrMwcDialog()
          .setContent(dialogContent)
          .setActions(dialogActions)
          .setStyles(html`
                        <style>
                            .mdc-dialog__surface {
                                display: flex;
                                width: 400px;
                                max-width: 100%;
                                overflow: visible;
                                overflow-x: visible !important;
                                overflow-y: visible !important;
                            }
                            #dialog-content {
                                text-align: center;
                                flex: 1;
                                overflow: visible;
                                min-height: 0;
                            }
                            or-asset-tree {
                                height: 100%;
                            }
                        </style>
                    `)
          .setDismissAction(null));
    }

    private async _doDelete(realm) {
        await manager.rest.api.RealmResource.delete(realm.realm);
        this._realms = [...this._realms.filter(u => u.id != realm.id)];
    }

    private expanderToggle(ev: MouseEvent, index:number) {
        const metaRow = this.shadowRoot.getElementById('realm-row-'+index)
        const expanderIcon = this.shadowRoot.getElementById('mdc-data-table-icon-'+index) as OrIcon
        if (metaRow.classList.contains('expanded')) {
            metaRow.classList.remove("expanded");
            expanderIcon.icon = "chevron-right";
        } else {
            metaRow.classList.add("expanded");
            expanderIcon.icon = "chevron-down";
        }
    }
}

