/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-file-uploader";
import { i18next } from "@openremote/or-translate";
import { MapRealmConfig } from "@openremote/model";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { OrMapLongPressEvent } from "@openremote/or-map";
import { LngLat } from "maplibre-gl";

@customElement("or-conf-map-card")
export class OrConfMapCard extends LitElement {

  static styles = css`
    #remove-map {
      margin: 12px 0 0 0;
    }

    .subheader {
      padding: 10px 0 4px;
      font-weight: bolder;
    }

    .d-inline-flex {
      display: inline-flex;
    }

    .panel-content {
      padding: 0 24px 24px;
    }

    .description {
      font-size: 12px;
    }

    or-collapsible-panel {
      margin-bottom: 10px;
    }

    .boundary-container {
      width: 50%;
    }

    .boundary-item, .input {
      width: 100%;
      max-width: 800px;
      padding: 10px 0;
    }

    .input or-mwc-input:not([icon]) {
      width: 80%;
    }

    .zoom-group {
      width: 50%;
      padding-left: 12px;
    }

    .map-container {
      width: 100%
    }
    .settings-container{
      display: inline-flex;
    }

    @media screen and (max-width: 768px) {
      .zoom-group, .boundary-container{
        width: 100%;
        padding: unset;
      }
      .settings-container{
        display: block;
      }
    }
  `;

  @property({ attribute: false })
  public map: MapRealmConfig = {};

  @property({ attribute: true })
  public name: string = "";

  @property({ type: Boolean })
  expanded: boolean = false;

  @property({ attribute: true })
  public onRemove: CallableFunction = () => {
  };

  protected _showRemoveMapDialog() {

    const dialogActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: i18next.t("cancel"),
      },
      {
        default: true,
        actionName: "ok",
        content: i18next.t("yes"),
        action: () => {
          this.onRemove();
        },
      },

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setHeading(i18next.t("delete"))
      .setActions(dialogActions)
      .setContent(html`
        ${i18next.t("configuration.deleteMapCustomizationConfirm")}
      `)
      .setStyles(html`
        <style>
          .mdc-dialog__surface {
            padding: 4px 8px;
          }

          #dialog-content {
            padding: 24px;
          }
        </style>
      `)
      .setDismissAction(null));

  }

  protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
  }

  protected setBoundary(key: number, value: any) {
    this.map.bounds[key] = parseFloat(parseFloat(value).toFixed(2));
    this.map.bounds = JSON.parse(JSON.stringify(this.map.bounds));
    this.requestUpdate();
  }

  protected setCenter(cor: LngLat|string) {
    let centerCoordinate: LngLat
    if (typeof cor == "string"){
      // @ts-ignore
      centerCoordinate = {
        lat: parseFloat(cor.split(',')[1]),
        lng: parseFloat(cor.split(',')[0])
      }
    } else {
      centerCoordinate = cor
    }
    const BetweenNorthSouth = this.map.bounds[3] > centerCoordinate.lat && this.map.bounds[1] < centerCoordinate.lat;
    const BetweenWestEast = this.map.bounds[0] < centerCoordinate.lng && this.map.bounds[2] > centerCoordinate.lng;
    if (!(BetweenNorthSouth && BetweenWestEast)) {
      this.map.bounds = [
        parseFloat((centerCoordinate.lng - .2).toFixed(3)),
        parseFloat((centerCoordinate.lat - .1).toFixed(3)),
        parseFloat((centerCoordinate.lng + .2).toFixed(3)),
        parseFloat((centerCoordinate.lat + .1).toFixed(3)),
      ];
    }
    this.map.center = [centerCoordinate.lng, centerCoordinate.lat];
    this.requestUpdate();
  }

  protected setZoom(nr:number){
    this.zoom = nr;
    return this.requestUpdate()
  }

  protected zoom = 1;


  render() {
    return html`
      <or-collapsible-panel
        .expanded="${this.expanded}">
        <div slot="header" class="header-container">
          ${this.name}
        </div>
        <div slot="content" class="panel-content">
          <div class="map-container">
            <div class="subheader">${i18next.t("map")}</div>
            <or-map id="vectorMap" .showBoundaryBoxControl="${true}" .zoom="${this.zoom}"
                    .boundary="${this.map.bounds}"
                    @or-map-long-press="${(ev: OrMapLongPressEvent) => {
                      this.setCenter(ev.detail.lngLat);
                    }}" .showGeoCodingControl="${true}"
                    .useZoomControl="${false}"
                    style="height: 500px; width: 100%;">
              ${this.map.center ? html`
                <or-map-marker id="geo-json-point-marker" .lng="${this.map.center[0]}"
                               .lat="${this.map.center[1]}"></or-map-marker>` : ""}

            </or-map>
          </div>

          <div class="settings-container">
            <div class="boundary-container">
              <div class="subheader">${i18next.t("configuration.mapBounds")}</div>
              <span>${i18next.t("configuration.mapBoundsDescription")}</span>
              <or-mwc-input .value="${this.map?.bounds[3]}" .type="${InputType.NUMBER}" .label="${i18next.t("north")}"
                            class="boundary-item"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(3, e.detail.value)}"
                            .step="${.01}"></or-mwc-input>
              <or-mwc-input .value="${this.map?.bounds[2]}" .type="${InputType.NUMBER}" .label="${i18next.t("east")}"
                            class="boundary-item"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(2, e.detail.value)}"
                            .step="${.01}"></or-mwc-input>
              <or-mwc-input .value="${this.map?.bounds[0]}" .type="${InputType.NUMBER}" .label="${i18next.t("west")}"
                            class="boundary-item"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(0, e.detail.value)}"
                            .step="${.01}"></or-mwc-input>
              <or-mwc-input .value="${this.map?.bounds[1]}" .type="${InputType.NUMBER}" .label="${i18next.t("south")}"
                            class="boundary-item"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(1, e.detail.value)}"
                            .step="${.01}"></or-mwc-input>
              <div class="subheader">${i18next.t("configuration.center")}</div>
              <span>${i18next.t("configuration.centerDescription")}</span>
              <or-mwc-input .value="${Array.isArray(this.map.center) ? this.map.center.join() : undefined}"
                            .type="${InputType.TEXT}" label="${i18next.t("configuration.center")}"
                            class="boundary-item"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setCenter(e.detail.value)}"
                            .step="${.01}"></or-mwc-input>
            </div>

            <div class="zoom-group">
              <div class="subheader">${i18next.t("configuration.mapZoom")}</div>
              <span>${i18next.t("configuration.mapZoomDescription")}</span>
              <div class="input">
                <or-mwc-input .value="${this.map.zoom}" .type="${InputType.NUMBER}" .label="${i18next.t('default')}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.zoom = e.detail.value}"
                              .step="${1}"></or-mwc-input>
                <or-mwc-input .type="${InputType.BUTTON}" icon="eye"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setZoom(this.map.zoom)}"
                              .step="${1}"></or-mwc-input>
              </div>
              <div class="input">
                <or-mwc-input .value="${this.map.minZoom}" .type="${InputType.NUMBER}"
                              .label="${i18next.t("configuration.minZoom")}"
                              max="${this.map.maxZoom}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {this.map.minZoom = e.detail.value; this.requestUpdate() }}"
                              .step="${1}"></or-mwc-input>
                <or-mwc-input .type="${InputType.BUTTON}" icon="eye"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setZoom(this.map.minZoom)}"
                              .step="${1}"></or-mwc-input>
              </div>
              <div class="input">
                <or-mwc-input .value="${this.map.maxZoom}" .type="${InputType.NUMBER}"
                              .label="${i18next.t("configuration.maxZoom")}"
                              min="${this.map.minZoom}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => { this.map.maxZoom = e.detail.value;this.requestUpdate()}}"
                              .step="${1}"></or-mwc-input>
                <or-mwc-input .type="${InputType.BUTTON}" icon="eye"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setZoom(this.map.maxZoom)}"
                              .step="${1}"></or-mwc-input>
              </div>

              <or-mwc-input .value="${this.map.boxZoom}" .type="${InputType.SWITCH}" label="BoxZoom"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.boxZoom = e.detail.value}"
                            .step="${1}"></or-mwc-input>
            </div>
          </div>


          <or-mwc-input outlined .type="${InputType.BUTTON}" id="remove-map"
                        .label="${i18next.t("configuration.deleteMapCustomization")}"
                        @click="${() => {
                          this._showRemoveMapDialog();
                        }}"></or-mwc-input>
        </div>
      </or-collapsible-panel>
    `;
  }
}
