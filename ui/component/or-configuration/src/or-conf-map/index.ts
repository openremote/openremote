import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import {OrMap, OrMapClickedEvent, OrMapMarker, OrMapMarkerClickedEvent} from "@openremote/or-map";

@customElement("or-conf-map")
export class OrConfMap extends LitElement {

  render() {
    return html`
      <or-map id="vectorMap" .showBoundaryBoxControl="${true}" style="height: 500px; width: 100%;">
        <or-map-marker id="demo-marker" lng="5.454250" class="marker" icon="or:logo-plain"></or-map-marker>
      </or-map>
    `
  }



}
