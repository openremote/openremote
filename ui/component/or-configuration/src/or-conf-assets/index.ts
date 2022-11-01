import { html, LitElement, css, PropertyValues } from "lit";
import {customElement, property} from "lit/decorators.js";
import './or-conf-asset-panels'
import manager from "@openremote/core";

export interface AssetsPanel{
  type: string,
  title:string,
  hideOnMobile?: boolean
  properties?: {
    include?: [string],
    exclude?: [string]
  },
  attributes?: {
    include?: [string],
    exclude?: [string]
  }
}

export interface MangerConfigAssets {
  tree?: {},
  viewer?: {
    assetTypes?:{
      [name:string] :
        {
          panels: AssetsPanel[]
        }
    },
  }
}


@customElement("or-conf-assets")
export class OrConfAssets extends LitElement {

  static styles = css`
    `;

  @property({attribute: false})
  public assets: MangerConfigAssets = {};




  updated(changedProperties: PropertyValues) {
    super.updated(changedProperties);
  }

  render() {
    const app = this;
    const assets = (this.assets?.viewer?.assetTypes ? this.assets?.viewer?.assetTypes : {})
    return html`
      ${Object.entries(assets).map(function([key, asset]){
        manager.rest.api.AssetModelResource.getAssetInfo(key).then(response => console.log(response))
        return html`
          <or-collapsible-panel>
            <div slot="header" class="header-container">
              <strong>${key}</strong>
            </div>
            <div slot="content">
              <or-conf-asset-panels .panels="${asset.panels}"></or-conf-asset-panels>
            </div>
          </or-collapsible-panel>
        `
      })}
    `
  }
}
