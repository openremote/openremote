import {
  css,
  customElement,
  html,
  property,
  TemplateResult,
  unsafeCSS,
} from "lit-element";
import manager from "@openremote/core";
import "@openremote/or-panel";
import "@openremote/or-translate";
import { EnhancedStore } from "@reduxjs/toolkit";
import { AppStateKeyed } from "../app";
import { Page } from "../types";
import { User } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { OrIcon } from "@openremote/or-icon";
import { InputType, OrInputChangedEvent } from "@openremote/or-input";

const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

export function pageUsersProvider<S extends AppStateKeyed>(
  store: EnhancedStore<S>
) {
  return {
    routes: ["users"],
    pageCreator: () => {
      return new PageUsers(store);
    },
  };
}

@customElement("page-users")
class PageUsers<S extends AppStateKeyed> extends Page<S> {
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
          margin: 20px auto;
          padding: 0;
          font-size: 18px;
          font-weight: bold;
          width: 100%;
          max-width: 1360px;
        }

        #table-users {
          width: 100%;
          max-width: 1400px;
          margin: 0 auto;
        }

        .meta-item-container {
            flex-direction: row;
          overflow: hidden;
          max-height: 0;
          transition: max-height 0.25s ease-out;
          padding: 0 20px 0 50px;
        }

        or-input {
            margin-bottom: 20px;
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
            margin: 10px;
            flex: 1 1 0;
        }

        .attribute-meta-row.expanded .meta-item-container {
          max-height: 1000px;
          transition: max-height 1s ease-in;
        }
      `,
    ];
  }

  @property()
  protected _users: User[] = [];

  get name(): string {
    return "users";
  }

  constructor(store: EnhancedStore<S>) {
    super(store);
    this.getUsers();
  }

  private getUsers() {
    manager.rest.api.UserResource.getAll(manager.displayRealm).then(
      (usersResponse) => (this._users = [...usersResponse.data])
    );
  }
  private _updateUser(user, passwords) {
      if(passwords && passwords.resetPassword && passwords.repeatPassword) {
          if(passwords.resetPassword !== passwords.repeatPassword) return;
          const credentials = {value: passwords.resetPassword}
          manager.rest.api.UserResource.resetPassword(manager.displayRealm, user.id, credentials);

      }
    manager.rest.api.UserResource.update(manager.displayRealm, user.id, user);
  }

  private _deleteUser(user) {
    manager.rest.api.UserResource.delete(manager.displayRealm, user.id).then(response => console.log(response));
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
    const expanderToggle = (ev: MouseEvent) => {
      const tdElem = ev.target as HTMLElement;
      if (tdElem.className.indexOf("expander-cell") < 0) {
        return;
      }
      const expanderIcon = tdElem.getElementsByTagName("or-icon")[0] as OrIcon;
      const headerRow = tdElem.parentElement! as HTMLTableRowElement;
      const metaRow = (headerRow.parentElement! as HTMLTableElement).rows[
        headerRow.rowIndex
      ];
      if (expanderIcon.icon === "chevron-right") {
        expanderIcon.icon = "chevron-down";
        metaRow.classList.add("expanded");
      } else {
        expanderIcon.icon = "chevron-right";
        metaRow.classList.remove("expanded");
      }
    };
    return html`
         <div id="wrapper">
                <div id="title">
                <or-icon icon="account-group"></or-icon>${i18next.t(
                  "user_plural"
                )}
                </div>
                <div id="table-users" class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="attribute list" @click="${expanderToggle}">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="username"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="email"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="user_role"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="status"></or-translate></th>
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                    ${this._users.map(
                      (user) => {
                        let passwords = {resetPassword: "", repeatPassword:""};
                        return html`
                        <tr class="mdc-data-table__row">
                          <td
                            class="padded-cell mdc-data-table__cell expander-cell"
                          >
                            <or-icon icon="chevron-right"></or-icon>
                            <span>${user.username}</span>
                          </td>
                          <td class="padded-cell mdc-data-table__cell">
                            ${user.email}
                          </td>
                          <td class="padded-cell mdc-data-table__cell">
                          </td>
                          <td class="padded-cell mdc-data-table__cell">
                            ${user.enabled}
                          </td>
                        </tr>
                        <tr class="attribute-meta-row">
                          <td colspan="4">
                            <div class="meta-item-container">
                                <div class="row">
                                    <div class="column">
                                        <or-input .label="${i18next.t("user")}" .type="${InputType.TEXT}" min="1" required .value="${user.username}" @or-input-changed="${(e: OrInputChangedEvent) => user.username = e.detail.value}"></or-input>            
                                        <or-input .label="${i18next.t("email")}" .type="${InputType.EMAIL}" min="1" required .value="${user.email}" @or-input-changed="${(e: OrInputChangedEvent) => user.email = e.detail.value}"></or-input>            
                                        <or-input .label="${i18next.t("firstName")}" .type="${InputType.TEXT}" min="1" required .value="${user.firstName}" @or-input-changed="${(e: OrInputChangedEvent) => user.firstName = e.detail.value}"></or-input>            
                                        <or-input .label="${i18next.t("lastName")}" .type="${InputType.TEXT}" min="1" required .value="${user.lastName}" @or-input-changed="${(e: OrInputChangedEvent) => user.lastName = e.detail.value}"></or-input>            
                                    </div>

                                    <div class="column">
                                        <or-input .label="${i18next.t("resetPassword")}" .type="${InputType.PASSWORD}" min="1"  @or-input-changed="${(e: OrInputChangedEvent) => passwords.resetPassword = e.detail.value}"></or-input>            
                                        <or-input .label="${i18next.t("repeatPassword")}" .type="${InputType.PASSWORD}" min="1" @or-input-changed="${(e: OrInputChangedEvent) => passwords.repeatPassword = e.detail.value}"></or-input>            

                                        <or-input .label="${i18next.t("enabled")}" .type="${InputType.SWITCH}" min="1" required .value="${user.enabled}" @or-input-changed="${(e: OrInputChangedEvent) => user.enabled = e.detail.value}"></or-input>            
                                    </div>
                                </div>

                                <div class="row">       
                                    <or-input .label="${i18next.t("delete")}" .type="${InputType.BUTTON}" @click="${() => this._deleteUser(user)}"></or-input>            
                                    <or-input style="margin-left: auto;" .label="${i18next.t("save")}" .type="${InputType.BUTTON}" @click="${() => this._updateUser(user, passwords)}"></or-input>      
                                </div>
                               
                            </div>
                          </td>
                        </tr>
                      `
                    })}
                    </tbody>
                </table>
            </div>
            </div>
           
        `;
  }

  public stateChanged(state: S) {}
}
