import { html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";


@customElement("or-conf-map")
export class OrConfMap extends LitElement {

  protected boundary = ['4.42', '51.88', '4.55','51.94'];

  protected setBoundary(key:number, value:string){
    this.boundary[key] = value
    this.boundary = JSON.parse(JSON.stringify(this.boundary))
    this.requestUpdate()
  }

  render() {
    return html`
      <or-map id="vectorMap" .showBoundaryBoxControl="${true}" .boundary="${this.boundary}" style="height: 500px; width: 100%;">
        <or-map-marker id="demo-marker" lng="5.454250" class="marker" icon="or:logo-plain"></or-map-marker>
      </or-map>


      <or-mwc-input class="color-item" .value="${this.boundary[0]}" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(0, e.detail.value.toString())}" .step="${.01}"></or-mwc-input>
      <or-mwc-input class="color-item" .value="${this.boundary[1]}" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(1, e.detail.value.toString())}" .step="${.01}"></or-mwc-input>
      <or-mwc-input class="color-item" .value="${this.boundary[2]}" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(2, e.detail.value.toString())}" .step="${.01}"></or-mwc-input>
      <or-mwc-input class="color-item" .value="${this.boundary[3]}" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(3, e.detail.value.toString())}" .step="${.01}"></or-mwc-input>
    `;
  }



}
