import {
  css,
  html,
  PropertyValues,
  TemplateResult,
  unsafeCSS,
} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, { OREvent, DefaultColor3 } from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import { Store } from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import { ClientRole, Role } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { OrIcon } from "@openremote/or-icon";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageRolesProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "roles",
        routes: ["roles"],
        pageCreator: () => {
          return new PageRoles(store);
        },
    };
}


@customElement("page-roles")
export class PageRoles extends Page<AppStateKeyed> {
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
          max-width: 1310px;
          background-color: white;
          border: 1px solid #e5e5e5;
          border-radius: 5px;
          position: relative;
          margin: 0 auto;
          padding: 18px 24px 24px;
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
          cursor: pointer;
          border-top-color: #D3D3D3;
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
          padding-left: 16px;
        }

        or-mwc-input {
            margin-bottom: 10px;
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

        .column-title {
            padding-bottom: 10px;
        }

        .mdc-data-table__header-cell {
            font-weight: bold;
            color: ${unsafeCSS(DefaultColor3)};
        }

        .mdc-data-table__header-cell:first-child {
            padding-left: 36px;
        }
        
        .padded-cell {
          overflow-wrap: break-word;
          word-wrap: break-word;
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
            padding: 16px;
            align-items: center;
            font-size: 14px;
            text-transform: uppercase;
            color: var(--or-app-color4);
        }

        .button or-icon {
            --or-icon-fill: var(--or-app-color4);
            margin-right: 5px;
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

  @state()
  protected _compositeRoles: Role[] = [];

  @state()
  protected _roles: Role[] = [];

  @state()
  protected _rolesMapper = {};

  @state()
  public realm?: string;

  get name(): string {
    return "role_plural";
  }

  public shouldUpdate(_changedProperties: PropertyValues): boolean {

      if (_changedProperties.has("realm")) {
          this.getRoles();
      }

      return super.shouldUpdate(_changedProperties);
  }

  public connectedCallback() {
      super.connectedCallback();
      this.realm = this.getState().app.realm;
  }

  protected getState() {
      return this._store.getState();
  }

  protected async getRoles() {
    const roleResponse = await manager.rest.api.UserResource.getRoles(this.realm);
    this._compositeRoles = [...roleResponse.data.filter(role => role.composite)];
    this._roles = [...roleResponse.data.filter(role => !role.composite)];
    this._roles.map(role => {
        this._rolesMapper[role.id] = role.name
    });
  }

  private async _updateRoles() {
    if(this._compositeRoles.some(role => role.compositeRoleIds.length === 0)) {
      return
    }
    const roles = [...this._compositeRoles, ...this._roles];
    await manager.rest.api.UserResource.updateRoles(this.realm, roles);
    this.getRoles();
  }

  private _deleteRole(role, rowIndex) {
    showOkCancelDialog(i18next.t("deleteRole"), i18next.t("deleteRoleConfirm", { roleName: role.name }), i18next.t("delete"))
    .then((ok) => {
        if (ok) {
          this.doDelete(role, rowIndex);
        }
    });
  }
  
  private doDelete(role, rowIndex) {
    this.expanderToggle(rowIndex);
    this._compositeRoles = [...this._compositeRoles.filter(u => u.id !== role.id)]
    this._updateRoles()
  }

  private addRemoveRole(e, r, index) {
    if(e.detail.value) {
      this._compositeRoles[index].compositeRoleIds = [...this._compositeRoles[index].compositeRoleIds, r.id]
    } else {
      this._compositeRoles[index].compositeRoleIds = this._compositeRoles[index].compositeRoleIds.filter(id=> id !== r.id)
    }
    this.requestUpdate('_compositeRoles')
  }

  private expanderToggle(index: number) {
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

    if (!this._roles || this._roles.length === 0) {
        return html``;
    }

    const readonly = !manager.hasRole(ClientRole.WRITE_USER);
    const readRoles = this._roles.filter(role => role.name.includes('read')).sort((a, b) => a.name.localeCompare(b.name))
    const writeRoles = this._roles.filter(role => role.name.includes('write')).sort((a, b) => a.name.localeCompare(b.name))
    const otherRoles = this._roles.filter(role => !role.name.includes('read') && !role.name.includes('write')).sort((a, b) => a.name.localeCompare(b.name))
    return html`
      <div id="wrapper">
        <div id="title">
            <or-icon icon="account-box-multiple"></or-icon>${i18next.t("role_plural")}
        </div>
        <div class="panel">
            <p class="panel-title">${i18next.t("role")}</p>
            <div id="table-roles" class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="attribute list">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                <or-translate value="name"></or-translate>
                            </th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                <or-translate value="description"></or-translate>
                            </th>
                            <th class="mdc-data-table__header-cell hide-mobile large" role="columnheader" scope="col">
                                <or-translate value="permissions"></or-translate>
                            </th>
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this._compositeRoles.map((role, index) => {
                          const compositeRoleName = role.compositeRoleIds.map(id => this._rolesMapper[id]).sort((a, b) => a.localeCompare(b)).join(', '); return html`
                        <tr id="mdc-data-table-row-${index}" class="mdc-data-table__row" @click="${() => this.expanderToggle(index)}">
                            <td class="padded-cell mdc-data-table__cell">
                                <or-icon id="mdc-data-table-icon-${index}" icon="chevron-right"></or-icon>
                                <span>${role.name}</span>
                            </td>
                            <td class="padded-cell mdc-data-table__cell">
                                ${role.description}
                            </td>
                            <td class="padded-cell hide-mobile mdc-data-table__cell large">
                                ${compositeRoleName}
                            </td>
                        </tr>
                        <tr id="attribute-meta-row-${index}" class="attribute-meta-row${!role.id ? " expanded " : " "}">
                            <td colspan="100%">
                                <div class="meta-item-container">

                                    <div class="row">
                                        <div class="column">
                                            <or-mwc-input .label="${i18next.t("role")}" .type="${InputType.TEXT}" min="1" required .value="${role.name}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => role.name = e.detail.value}"></or-mwc-input>
                                        </div>
                                        <div class="column">
                                            <or-mwc-input .label="${i18next.t("description")}" .type="${InputType.TEXT}" min="1" required .value="${role.description}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => role.description = e.detail.value}"></or-mwc-input>
                                        </div>
                                    </div>

                                    <div class="row">
                                        <div class="column">
                                            <strong class="column-title">${i18next.t("readPermissions")}</strong> ${readRoles.map(r => {
                                            return html`
                                              <or-mwc-input ?readonly="${readonly}" .label="${r.name.split(":")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.addRemoveRole(e, r, index)}"></or-mwc-input>
                                            ` })}

                                        </div>
                                        <div class="column">
                                            <strong class="column-title">${i18next.t("writePermissions")}</strong> ${writeRoles.map(r => {
                                            return html`
                                              <or-mwc-input ?readonly="${readonly}" .label="${r.name.split(":")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.addRemoveRole(e, r, index)}"></or-mwc-input>
                                            ` })}
                                        </div>
                                    </div>

                                    <div class="row">
                                        <div class="column">
                                            ${otherRoles.map(r => {
                                            return html`
                                            <or-mwc-input ?readonly="${readonly}" .label="${r.name.split(" : ")[1]}: ${r.description}" .type="${InputType.CHECKBOX}" .value="${role.compositeRoleIds && role.compositeRoleIds.find(id => id === r.id)}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.addRemoveRole(e, r, index)}"></or-mwc-input>
                                            ` })}
                                        </div>
                                    </div>

                                    ${readonly ? html`` : html`
                                    <div class="row" style="margin-bottom: 0;">
                                        ${role.id ? html`
                                        <or-mwc-input label="delete" .type="${InputType.BUTTON}" @click="${() => this._deleteRole(role, index)}"></or-mwc-input>
                                        <or-mwc-input ?disabled="${this._compositeRoles.some(role => role.compositeRoleIds.length === 0)}" style="margin-left: auto;" label="save" .type="${InputType.BUTTON}" @click="${() => this._updateRoles()}"></or-mwc-input>
                                        ` : html`
                                        <or-mwc-input label="cancel" .type="${InputType.BUTTON}" @click="${() => { this._compositeRoles.splice(-1, 1); this._compositeRoles = [...this._compositeRoles] }}"></or-mwc-input>
                                        <or-mwc-input ?disabled="${this._compositeRoles.some(role => role.compositeRoleIds.length === 0)}" style="margin-left: auto;" label="create" .type="${InputType.BUTTON}" @click="${() => this._updateRoles()}"></or-mwc-input>
                                        `}
                                    </div>
                                    `}
                                </div>
                            </td>
                        </tr>
                        ` })} ${this._compositeRoles.length > 0 && !!this._compositeRoles[this._compositeRoles.length - 1].id && !readonly ? html`
                        <tr class="mdc-data-table__row">
                            <td colspan="100%">
                                <a class="button" @click="${() => this._compositeRoles = [...this._compositeRoles, { composite: true, name: "", compositeRoleIds: [] }]}">
                                    <or-icon icon="plus"></or-icon>${i18next.t("add")} ${i18next.t("role")}</a>
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
      this.realm = state.app.realm;
  }
}
