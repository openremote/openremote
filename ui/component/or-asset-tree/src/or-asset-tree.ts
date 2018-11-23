import openremote, {Auth} from "@openremote/core";
import {html, PolymerElement} from "@polymer/polymer";

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

window.customElements.define("or-asset-tree", OrAssetTree);

openremote.init({
  auth: Auth.KEYCLOAK,
  autoLogin: true,
  keycloakUrl: "http://localhost:8080/auth",
  managerUrl: "http://localhost:8080",
  realm: "customerA"
});
