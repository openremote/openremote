import { html, LitElement, css } from "lit";
import {customElement} from "lit/decorators.js";
import "./or-conf-nav-header";
import "./or-conf-nav-item";


@customElement("or-conf-navigation")
export class OrConfRealm extends LitElement {

  static styles = css`
    
    `;

  render() {
    return html`
      <or-conf-nav-header>Manager Configuration</or-conf-nav-header>
      <or-conf-nav-header>Style</or-conf-nav-header>
      <or-conf-nav-item href="style/realms">Realms</or-conf-nav-item>
      <or-conf-nav-header>Map</or-conf-nav-header>
      <or-conf-nav-item href="map/view">View</or-conf-nav-item>
      <or-conf-nav-item href="map/assets">Assets</or-conf-nav-item>
    `
  }
}
