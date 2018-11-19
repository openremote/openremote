import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';
import openremote, {Auth} from "@openremote/core";
import rest from "@openremote/rest";

/**
 * `or-asset-tree`
 * Displays assets in a tree
 *
 * @customElement
 * @polymer
 * @demo demo/index.html
 */
class OrAssetTree extends PolymerElement {
  static get template() {
    return html`
      <style>
        :host {
          display: block;
        }
      </style>
      <h2>Hello [[prop1]]!</h2>
    `;
  }
  static get properties() {
    return {
      prop1: {
        type: String,
        value: openremote.authenticated,
      },
    };
  }
}

window.customElements.define('or-asset-tree', OrAssetTree);

openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: "customerA"
});
