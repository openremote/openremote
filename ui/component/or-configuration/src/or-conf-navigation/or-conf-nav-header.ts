import { html, LitElement, css } from "lit";
import {customElement} from "lit/decorators.js";


@customElement("or-conf-nav-header")
export class OrConfNavHeader extends LitElement {

  static styles = css`
    .container{
      padding: 18px 16px;
      font-weight: bold;
    }
    `;

  render() {
    return html`
      <div class="container">
        <slot></slot>
      </div>
    `
  }
}
