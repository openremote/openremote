/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { html, render } from "lit-html";
import manager, { type Manager } from "@openremote/core";
import { Auth, type AssetQuery } from "@openremote/model";

const loggedInTemplate = (manager: Manager) =>
  html`<span>Welcome ${manager.username}</span>(<button
      @click="${() => {
        manager.logout();
      }}"
    >
      logout</button
    >)`;
const loggedOutTemplate = (manager: Manager) =>
  html`<span>Please</span
    ><button
      @click="${() => {
        manager.login();
      }}"
    >
      login
    </button>`;

function renderUi() {
  if (manager.authenticated) {
    const queryAssetsTemplate = html`
      ${loggedInTemplate(manager)}
      <br />
      <button @click="${() => queryAssets()}">Get Assets</button><span> (see console window)</span>
    `;
    render(queryAssetsTemplate, document.body);
  } else {
    render(loggedOutTemplate(manager), document.body);
  }
}

function queryAssets() {
  const assetQuery: AssetQuery = {};
  manager.rest.api.AssetResource.queryAssets(assetQuery)
    .then((response) => {
      console.log("Received: " + response.data.length + " Asset(s)");
      console.log(JSON.stringify(response.data, null, 2));
    })
    .catch((reason) => console.log("Error:" + reason));
}

manager.addListener((event) => {
  console.log("OR Event:" + event);
});

manager
  .init({
    managerUrl: "http://localhost:8080",
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: "smartcity",
  })
  .then(renderUi);
