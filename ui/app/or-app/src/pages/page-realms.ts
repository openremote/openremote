import {
  css,
  customElement,
  html,
  property,
  PropertyValues,
  TemplateResult,
  unsafeCSS,
} from "lit-element";
import manager, { OREvent } from "@openremote/core";
import "@openremote/or-panel";
import "@openremote/or-translate";
import { EnhancedStore } from "@reduxjs/toolkit";
import { AppStateKeyed } from "../app";
import { Page } from "../types";
import { ClientRole, Tenant } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { OrIcon } from "@openremote/or-icon";
import { InputType, OrInputChangedEvent } from "@openremote/or-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageRealmsProvider<S extends AppStateKeyed>(
  store: EnhancedStore<S>
) {
  return {
    routes: ["realms"],
    pageCreator: () => {
      return new PageRealms(store);
    },
  };
}


@customElement("page-realms")
class PageRealms<S extends AppStateKeyed> extends Page<S> {
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
        }

        #title or-icon {
          margin-right: 10px;
          margin-left: 14px;
        }

        .panel {
          width: calc(100% - 90px);
          padding: 0 20px;
          max-width: 1320px;
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
            color: var(--internal-or-asset-viewer-title-text-color);
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
          border-top-color: lightgrey;
        }
        
        td, th {
          width: 25%;
          border: none;
        }
  
        td.large, th.large {
          width: 50%
        }

        .meta-item-container {
          flex-direction: row;
          overflow: hidden;
          max-height: 0;
          transition: max-height 0.25s ease-out;
          padding: 0 20px;
        }

        or-input {
            margin-bottom: 10px;
            margin-right: 20px;
        }

        or-icon {
            vertical-align: middle;
            --or-icon-width: 20px;
            --or-icon-height: 20px;
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
        }
        
        .padded-cell {
          overflow-wrap: break-word;
          word-wrap: break-word
        }

        .attribute-meta-row td {
          padding: 0;
        }

        .attribute-meta-row {
          max-width: 0px;
        }

        .attribute-meta-row.expanded .meta-item-container {
          max-height: 1000px;
          max-width: none;
          transition: max-height 1s ease-in;
        }
        
        .button {
          cursor: pointer;
          display: flex;
          flex-direction: row;
          align-content: center;
          margin: 10px 15px;
          align-items: center;
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
            width: calc(100% - 40px);
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
  protected _tenants: Tenant[] = [];

  @property()
  public realm?: string;

  get name(): string {
    return "roles";
  }

  constructor(store: EnhancedStore<S>) {
    super(store);
    this._getTenants();
  }

    protected _onManagerEvent = (event: OREvent) => {
      switch (event) {
          case OREvent.DISPLAY_REALM_CHANGED:
              this.realm = manager.displayRealm;
              break;
      }
  };

  public shouldUpdate(_changedProperties: PropertyValues): boolean {

      if (_changedProperties.has("realm")) {
          this._getTenants();
      }

      return super.shouldUpdate(_changedProperties);
  }

  public connectedCallback() {
      super.connectedCallback();
      manager.addListener(this._onManagerEvent);
  }

  public disconnectedCallback() {
      super.disconnectedCallback();
      manager.removeListener(this._onManagerEvent);
  }


 
  protected async _getTenants() {
    const response = await manager.rest.api.TenantResource.getAll();
    this._tenants = response.data;
    return this._tenants;
  }

  private async _updateTenant(tenant) {
    const response = await manager.rest.api.TenantResource.update(tenant.realm, tenant);
    const data:any = response.data;
    this._tenants = this._tenants.map(t => {
      if(t.id === data.id) {
        return data;
      } else {
        return t
      }
    })
  }

  private async _createTenant(tenant) {
    await manager.rest.api.TenantResource.create(tenant);
    this._getTenants()
  }
  
  private _deleteTenant(tenant) {
    showOkCancelDialog(i18next.t("delete"), i18next.t("deleteTenantConfirm"))
    .then((ok) => {
        if (ok) {
          this._doDelete(tenant);
        }
    });
  }
  
  private async _doDelete(tenant) {
    await manager.rest.api.TenantResource.delete(tenant.realm);
    this._tenants = [...this._tenants.filter(u => u.id != tenant.id)]
  }

  private expanderToggle(ev: MouseEvent, index:number) {
    const metaRow = this.shadowRoot.getElementById('attribute-meta-row-'+index)
    const expanderIcon = this.shadowRoot.getElementById('mdc-data-table-icon-'+index) as OrIcon
    if(metaRow.classList.contains('expanded')){
      metaRow.classList.remove("expanded");
      expanderIcon.icon = "chevron-right";
    } else {
      metaRow.classList.add("expanded");
      expanderIcon.icon = "chevron-down";
    }
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
                <or-icon icon="account-box-multiple"></or-icon>${i18next.t(
                  "realm management"
                )}
                </div>
                <div class="panel">
                <p class="panel-title">${i18next.t("realms")}</p>
                  <div id="table-roles" class="mdc-data-table">
                  <table class="mdc-data-table__table" aria-label="attribute list" >
                      <thead>
                          <tr class="mdc-data-table__header-row">
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="name"></or-translate></th>
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="displayName"></or-translate></th>
                              <th class="mdc-data-table__header-cell hide-mobile large" role="columnheader" scope="col"><or-translate value="status"></or-translate></th>
                          </tr>
                      </thead>
                      <tbody class="mdc-data-table__content">
                      ${this._tenants.map(
                        (tenant, index) => {
                       
                          return html`
                          <tr id="mdc-data-table-row-${index}" class="mdc-data-table__row" @click="${(ev) => this.expanderToggle(ev, index)}">
                            <td  class="padded-cell mdc-data-table__cell"
                            >
                              <or-icon id="mdc-data-table-icon-${index}" icon="chevron-right"></or-icon>
                              <span>${tenant.realm}</span>
                            </td>
                            <td class="padded-cell mdc-data-table__cell">
                              ${tenant.displayName}
                            </td>
                            <td class="padded-cell hide-mobile mdc-data-table__cell large">
                            ${tenant.enabled ? "Enabled" : "Disabled"}

                            </td>
                          </tr>
                          <tr id="attribute-meta-row-${index}" class="attribute-meta-row${!tenant.id ? " expanded" : ""}">
                            <td colspan="100%">
                              <div class="meta-item-container">
                                 
                                  <div class="row">
                                    <div class="column">
                                      <or-input ?readonly="${tenant.id}" .label="${i18next.t("realm")}" .type="${InputType.TEXT}" min="1" required .value="${tenant.realm}" @or-input-changed="${(e: OrInputChangedEvent) => tenant.realm = e.detail.value}"></or-input>            
                                    </div>
                                    <div class="column">
                                      <or-input .label="${i18next.t("displayName")}" .type="${InputType.TEXT}" min="1" required .value="${tenant.displayName}" @or-input-changed="${(e: OrInputChangedEvent) => tenant.displayName = e.detail.value}"></or-input>            
                                    </div>
                                    <or-input ?readonly="${readonly}" .label="${i18next.t("enabled")}" .type="${InputType.SWITCH}" min="1" .value="${tenant.enabled}" @or-input-changed="${(e: OrInputChangedEvent) =>tenant.enabled = e.detail.value}}"></or-input>

                                  </div>



                                  <div class="row" style="margin-bottom: 0;">
                                  ${tenant.id && !readonly ? html`
                                      ${tenant.realm !== "master" ? html`
                                        <or-input hidden .label="${i18next.t("delete")}" .type="${InputType.BUTTON}" @click="${() => this._deleteTenant(tenant)}"></or-input>  
                                      ` : ``}
                                      <or-input style="margin-left: auto;" .label="${i18next.t("save")}" .type="${InputType.BUTTON}" @click="${() => this._updateTenant(tenant)}"></or-input>   
                                  ` : html`
                                    <or-input .label="${i18next.t("cancel")}" .type="${InputType.BUTTON}" @click="${() => {this._tenants.splice(-1,1); this._tenants = [...this._tenants]}}"></or-input>            
                                    <or-input style="margin-left: auto;" .label="${i18next.t("create")}" .type="${InputType.BUTTON}" @click="${() => this._createTenant(tenant)}"></or-input>   
                                  `}    
                                  </div>
                              </div>
                            </td>
                          </tr>
                        `
                      })}
                        ${this._tenants.length > 0 && !!this._tenants[this._tenants.length -1].id && !readonly ? html`
                        <tr class="mdc-data-table__row">
                          <td colspan="100%">
                            <a class="button" @click="${() => this._tenants = [...this._tenants, {displayName:"", enabled: true}]}"><or-icon icon="plus"></or-icon><strong>${i18next.t("add")} ${i18next.t("realm")}</strong></a> 
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

  public stateChanged(state: S) {}
}
