import { html, LitElement, css, PropertyValues } from "lit";
import {customElement, property} from "lit/decorators.js";
import { AssetsPanel } from "./index";



@customElement("or-conf-asset-panels")
export class OrConfAssetsPanels extends LitElement {

  static styles = css`
    .panel{
      border: 2px solid red;
    }
    `;

  @property({attribute: false})
  public panels: AssetsPanel[] = [];

  render() {
    const app = this;
    return html`
      <div>Panels</div>
      ${Object.entries(this.panels).map(function([key, panel]){
        return html`
          <div class="panel">
            ${panel.title} : ${panel.type} ${JSON.stringify(panel)}
          </div>
        
        `
      })}
    `
  }
}
