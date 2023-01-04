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
import { ManagerAppRealmConfig, MapRealmConfig } from "@openremote/model";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

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
    .boundary-container{
      display: inline;
    }
    .boundary-item{
      width: calc(50% - 2px);
      max-width: 800px;
      padding: 10px 0;
    }
    .input{
      width: 100%;
      max-width: 800px;
      padding: 10px 0px;
    }
    .zoom-group{
      width: 100%;
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

  protected setBoundary(key: number, value: string) {
    this.map.bounds[key] = value;
    this.map.bounds = JSON.parse(JSON.stringify(this.map.bounds));
    this.requestUpdate();
  }


  render() {
    const app = this;
    return html`
      <or-collapsible-panel
        .expanded="${this.expanded}">
        <div slot="header" class="header-container">
          ${this.name}
        </div>
        <div slot="content" class="panel-content">
          <or-map id="vectorMap" .showBoundaryBoxControl="${true}" .boundary="${this.map.bounds}"
                  style="height: 500px; width: 100%;">
            <or-map-marker id="demo-marker" lng="5.454250" class="marker" icon="or:logo-plain"></or-map-marker>
          </or-map>

          <div class="subheader">${i18next.t("configuration.mapBounds")}</div>
          <div class="boundary-container">
            <or-mwc-input .value="${this.map?.bounds[3]}" .type="${InputType.NUMBER}" label="North"
                          class="boundary-item"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(3, e.detail.value.toString())}"
                          .step="${.01}"></or-mwc-input>
            <or-mwc-input .value="${this.map?.bounds[2]}" .type="${InputType.NUMBER}" label="East"
                          class="boundary-item"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(2, e.detail.value.toString())}"
                          .step="${.01}"></or-mwc-input>
            <or-mwc-input .value="${this.map?.bounds[0]}" .type="${InputType.NUMBER}" label="West"
                          class="boundary-item"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(0, e.detail.value.toString())}"
                          .step="${.01}"></or-mwc-input>
            <or-mwc-input .value="${this.map?.bounds[1]}" .type="${InputType.NUMBER}" label="South"
                          class="boundary-item"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setBoundary(1, e.detail.value.toString())}"
                          .step="${.01}"></or-mwc-input>
          </div>
          <div class="zoom-group">
            <div class="subheader">${i18next.t("configuration.mapZoom")}</div>
            <or-mwc-input .value="${this.map.zoom}" .type="${InputType.NUMBER}" label="Zoom"
                          class="input"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.zoom = e.detail.value}"
                          .step="${1}"></or-mwc-input>
            <or-mwc-input .value="${this.map.boxZoom}" .type="${InputType.SWITCH}" label="BoxZoom"
                          class="input"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.boxZoom = e.detail.value}"
                          .step="${1}"></or-mwc-input>
            <or-mwc-input .value="${this.map.minZoom}" .type="${InputType.NUMBER}" label="Min Zoom"
                          class="input"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.minZoom = e.detail.value}"
                          .step="${1}"></or-mwc-input>
            <or-mwc-input .value="${this.map.maxZoom}" .type="${InputType.NUMBER}" label="Max Zoom"
                          class="input"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.map.maxZoom = e.detail.value}"
                          .step="${1}"></or-mwc-input>
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
