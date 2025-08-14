import { css, html, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { ParkingWidgetConfig } from "../widgets/parking-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { Attribute, AttributeRef } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-icon";

const styling = css`
`;

@customElement("parking-settings")
export class ParkingSettings extends AssetWidgetSettings {
  // Override of widgetConfig with extended type
  protected readonly widgetConfig!: ParkingWidgetConfig;

  protected render(): TemplateResult {
    const busFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
      return [
        "positiveInteger",
        "positiveNumber",
        "number",
        "long",
        "integer",
        "bigInteger",
        "bigNumber",
        "integerByte",
      ].includes(attr.type!);
    };

    const occupiedFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
      return ["boolean"].includes(attr.type!);
    };

    return html`
      <div class="field" style="padding-inline-start:14px">
        <or-mwc-input
          .type=${InputType.TEXT}
          .value=${this.widgetConfig.parkingLabel ?? ""}
          @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onParkingLabelChange(e)}
          label="Parkplatz Name"
        ></or-mwc-input>
      </div>
      <settings-panel displayName="attributes" expanded="${true}">
        <settings-panel displayName="Belegungsstatus" expanded="true">
          <attributes-panel
            .attributeRefs=${this.widgetConfig.occupiedRefs}
            style="padding-bottom: 12px;"
            .attributeFilter="${occupiedFilter}"
            @attribute-select=${(ev: AttributesSelectEvent) => this.onOccupiedSelect(ev)}
          ></attributes-panel>
          <div class="switch-row">
            <span>Belegung invertieren</span>
            <or-mwc-input
              .type=${InputType.SWITCH}
              .value=${this.widgetConfig.invertOccupied}
              @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onInvertOccupiedToggle(ev)}
            ></or-mwc-input>
          </div>
        </settings-panel>
        <settings-panel displayName="BusID" expanded="true">
          <attributes-panel
            .attributeRefs=${this.widgetConfig.busRefs}
            style="padding-bottom: 12px;"
            .attributeFilter="${busFilter}"
            @attribute-select=${(ev: AttributesSelectEvent) => this.onBusSelect(ev)}
          ></attributes-panel>
        </settings-panel>
      </settings-panel>
      <settings-panel displayName="settings" expanded="${true}">
        <div class="switch-row">
          <span>Parking-Icon anzeigen</span>
          <or-mwc-input
            .type=${InputType.SWITCH}
            .value=${this.widgetConfig.showParkingIcon}
            @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onShowParkingIconToggle(ev)}
          ></or-mwc-input>
          <div class="switch-row">
            <span><or-translate value="dashboard.showHelperText"></or-translate></span>
            <or-mwc-input
              .type=${InputType.SWITCH}
              .value=${this.widgetConfig.showHelperText}
              @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onHelperTextToggle(ev)}
            ></or-mwc-input>
          </div>
        </div>
      </settings-panel>
    `;
  }

  protected onBusSelect(ev: AttributesSelectEvent) {
    this.widgetConfig.busRefs = ev.detail.attributeRefs;
    if (ev.detail.attributeRefs.length === 1) {
      const asset = ev.detail.assets.find((a) => a.id === ev.detail.attributeRefs[0].id);
      if (asset) this.setDisplayName!(asset.name);
    }
    this.notifyConfigUpdate();
  }

  protected onOccupiedSelect(ev: AttributesSelectEvent) {
    this.widgetConfig.occupiedRefs = ev.detail.attributeRefs;
    if (ev.detail.attributeRefs.length === 1) {
      const asset = ev.detail.assets.find((a) => a.id === ev.detail.attributeRefs[0].id);
      if (asset) this.setDisplayName!(asset.name);
    }
    this.notifyConfigUpdate();
  }

  protected onInvertOccupiedToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.invertOccupied = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }

  protected onShowParkingIconToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showParkingIcon = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }

  protected onParkingLabelChange(ev: OrInputChangedEvent) {
    this.widgetConfig.parkingLabel = ev.detail.value as string;
    this.notifyConfigUpdate();
  }

  protected onHelperTextToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showHelperText = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }

}
