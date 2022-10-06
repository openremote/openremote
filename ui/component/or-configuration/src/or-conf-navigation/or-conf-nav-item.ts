import { html, LitElement, css } from "lit";
import {customElement, property} from "lit/decorators.js";


@customElement("or-conf-nav-item")
export class OrConfNavItem extends LitElement {

  @property({type: String})
  href: string | null = null;

  static styles = css`
    .nav-item{
      padding: 16px 24px;
      border-radius: 4px;
      background-color: inherit;
      color: rgb(76, 76, 76);
    }
    .nav-item:hover, .nav-item:focus, .nav-item:active, .nav-item.active{
      color: var(--or-app-color4);
      background-color: var(--or-app-color2);
    }
    .nav-container{
      text-decoration: none;
    }
    `;

  render() {
    return html`
      <a class="nav-container" href="#/configuration/${this.href}">
        <div class="nav-item ${location.hash === ('#/configuration/' + this.href) ? 'active' : ''}">
          <slot></slot>
        </div>
      </a>
    `
  }
}
