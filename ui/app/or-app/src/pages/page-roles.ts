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
import { ClientRole, Role } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { OrIcon } from "@openremote/or-icon";
import { InputType, OrInputChangedEvent } from "@openremote/or-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

export function pageRolesProvider<S extends AppStateKeyed>(
  store: EnhancedStore<S>
) {
  return {
    routes: ["roles"],
    pageCreator: () => {
      return new PageRoles(store);
    },
  };
}

@customElement("page-roles")
class PageRoles<S extends AppStateKeyed> extends Page<S> {
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
          margin: 0 auto;
          padding: 20px;
          font-size: 18px;
          font-weight: bold;
          width: calc(100% - 40px);
          max-width: 1400px;
        }

        #title or-icon {
          margin-right: 10px;
        }

        .panel {
          width: calc(100% - 80px);
          max-width: 1400px;
          background-color: white;
          border: 1px solid #e5e5e5;
          border-radius: 5px;
          position: relative;
          margin: 0 auto;
          padding: 20px;
        }

        .panel-title {
            text-transform: uppercase;
            font-weight: bolder;
            line-height: 1em;
            color: var(--internal-or-asset-viewer-title-text-color);
            margin-bottom: 20px;
            flex: 0 0 auto;
            letter-spacing: 0.025em;
        }

        #table-users {
            width: 100%;
        }

        .meta-item-container {
          flex-direction: row;
          overflow: hidden;
          max-height: 0;
          transition: max-height 0.25s ease-out;
          padding: 0 20px 0 30px;
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

          .panel {
            border-radius: 0;
          }
        }

        @media screen and (max-width: 768px){
          .hide-mobile {
            display: none;
          }
        }
      `,
    ];
  }
  @property()
  protected _compositeRoles: Role[] = [];

  @property()
  protected _roles: Role[] = [];

  @property()
  protected _rolesMapper = {};

  @property()
  public realm?: string;

  get name(): string {
    return "roles";
  }

  constructor(store: EnhancedStore<S>) {
    super(store);
    this.getRoles();
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
          this.getRoles();
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


  private getRoles() {
    manager.rest.api.UserResource.getRoles(manager.displayRealm).then(roleResponse => {
      this._compositeRoles = [...roleResponse.data.filter(role => role.composite)];
      this._roles = [...roleResponse.data.filter(role => !role.composite)];
      this._roles.map(role => {
        this._rolesMapper[role.id] = role.name
      })
    })
  }

  private _updateRoles() {
    const roles = [...this._compositeRoles, ...this._roles];
    manager.rest.api.UserResource.updateRoles(manager.displayRealm, roles).then(response => {
      this.getRoles()
    });
  }

  private _deleteRole(role) {
    showOkCancelDialog(i18next.t("delete"), i18next.t("deleteRoleConfirm"))
    .then((ok) => {
        if (ok) {
          this.doDelete(role);
        }
    });
  }
  
  private doDelete(role) {
    this._compositeRoles = [...this._compositeRoles.filter(u => u.id != role.id)]
    this._updateRoles()

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
    const expanderToggle = (ev: MouseEvent, index:number) => {
      const metaRow = this.shadowRoot.getElementById('attribute-meta-row-'+index)
      const expanderIcon = this.shadowRoot.getElementById('mdc-data-table-icon-'+index) as OrIcon
      if(metaRow.classList.contains('expanded')){
        metaRow.classList.remove("expanded");
        expanderIcon.icon = "chevron-right";
      } else {
        metaRow.classList.add("expanded");
        expanderIcon.icon = "chevron-down";
      }
    };
    const readonly = !manager.hasRole(ClientRole.WRITE_USER);
    const readRoles = this._roles.filter(role => role.name.includes('read')).sort((a, b) => a.name.localeCompare(b.name))
    const writeRoles = this._roles.filter(role => role.name.includes('write')).sort((a, b) => a.name.localeCompare(b.name))
    const otherRoles = this._roles.filter(role => !role.name.includes('read') && !role.name.includes('write')).sort((a, b) => a.name.localeCompare(b.name))
    return html`
         <div id="wrapper">
                <div id="title">
                <or-icon icon="account-group"></or-icon>${i18next.t(
                  "role"
                )}
                </div>
                <div class="panel">
                <p class="panel-title">${i18next.t("role")}</p>
                  <div id="table-users" class="mdc-data-table">
                  <table class="mdc-data-table__table" aria-label="attribute list" >
                      <thead>
                          <tr class="mdc-data-table__header-row">
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="name"></or-translate></th>
                              <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="description"></or-translate></th>
                              <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col"><or-translate value="permissions"></or-translate></th>
                          </tr>
                      </thead>
                      <tbody class="mdc-data-table__content">
                      ${this._compositeRoles.map(
                        (role, index) => {
                          const compositeRoleName = role.compositeRoleIds.map(id => this._rolesMapper[id]).join(', ');
                          return html`
                          <tr id="mdc-data-table-row-${index}" class="mdc-data-table__row" @click="${(ev) => expanderToggle(ev, index)}">
                            <td
                              class="padded-cell mdc-data-table__cell"
                            >
                              <or-icon id="mdc-data-table-icon-${index}" icon="chevron-right"></or-icon>
                              <span>${role.name}</span>
                            </td>
                            <td class="padded-cell mdc-data-table__cell">
                              ${role.description}
                            </td>
                            <td class="padded-cell hide-mobile mdc-data-table__cell">
                              ${compositeRoleName}
                            </td>
                          </tr>
                          <tr id="attribute-meta-row-${index}" class="attribute-meta-row${!role.name ? " expanded" : ""}">
                            <td colspan="4">
                              <div class="meta-item-container">
                                 
                                  <div class="row">
                                    <div class="column">
                                      <or-input .label="${i18next.t("role")}" .type="${InputType.TEXT}" min="1" required .value="${role.name}" @or-input-changed="${(e: OrInputChangedEvent) => role.name = e.detail.value}"></or-input>            
                                    </div>
                                    <div class="column">
                                      <or-input .label="${i18next.t("description")}" .type="${InputType.TEXT}" min="1" required .value="${role.description}" @or-input-changed="${(e: OrInputChangedEvent) => role.description = e.detail.value}"></or-input>            
                                    </div>
                                  </div>

                                  <div class="row">
                                      <div class="column">
                                        <strong>${i18next.t("read")}</strong>

                                        ${readRoles.map(r => {
                                          return html`
                                             <or-input ?readonly="${readonly}" .label="${r.name.split(":")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-input-changed="${(e: OrInputChangedEvent) => e.detail.value ? role.compositeRoleIds = [...role.compositeRoleIds, r.id]: role.compositeRoleIds = role.compositeRoleIds.filter(id=> id !== r.id) }"></or-input>            
                                          `
                                        })}
                                      
                                      </div>

                                      <div class="column">
                                        <strong>${i18next.t("write")}</strong>
                                        ${writeRoles.map(r => {
                                          return html`
                                             <or-input ?readonly="${readonly}" .label="${r.name.split(":")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-input-changed="${(e: OrInputChangedEvent) => e.detail.value ? role.compositeRoleIds = [...role.compositeRoleIds, r.id]: role.compositeRoleIds = role.compositeRoleIds.filter(id=> id !== r.id) }"></or-input>            
                                          `
                                        })}
                                      </div>
                                  </div>
                                  <div class="row">
                                      <div class="column">
                                        ${otherRoles.map(r => {
                                          return html`
                                             <or-input ?readonly="${readonly}" .label="${r.name.split(":")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-input-changed="${(e: OrInputChangedEvent) => e.detail.value ? role.compositeRoleIds = [...role.compositeRoleIds, r.id]: role.compositeRoleIds = role.compositeRoleIds.filter(id=> id !== r.id) }"></or-input>            
                                          `
                                        })}
                                      </div>
                                  </div>

                                  <div class="row">
                                  
                                  ${role.id && !readonly ? html`
                                      <or-input .label="${i18next.t("delete")}" .type="${InputType.BUTTON}" @click="${() => this._deleteRole(role)}"></or-input>            
                                      <or-input style="margin-left: auto;" .label="${i18next.t("save")}" .type="${InputType.BUTTON}" @click="${() => this._updateRoles()}"></or-input>   
                                  ` : html`
                                    <or-input .label="${i18next.t("cancel")}" .type="${InputType.BUTTON}" @click="${() => {this._compositeRoles.splice(-1,1); this._compositeRoles = [...this._compositeRoles]}}"></or-input>            
                                    <or-input style="margin-left: auto;" .label="${i18next.t("create")}" .type="${InputType.BUTTON}" @click="${() => this._updateRoles()}"></or-input>   
                                  `}    
                                  </div>
                              </div>
                            </td>
                          </tr>
                        `
                      })}
                        ${!!this._compositeRoles[this._compositeRoles.length -1].id && !readonly ? html`
                        <tr class="mdc-data-table__row">
                          <td colspan="4">
                            <a class="button" @click="${() => this._compositeRoles = [...this._compositeRoles, {compositeRoleIds:[]}]}"><or-icon icon="plus"></or-icon><strong>${i18next.t("add")} ${i18next.t("role")}</strong></a> 
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
