import { css, html, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { SliderWidgetConfig } from "../widgets/slider-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { Attribute } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-icon";

const styling = css``;

@customElement("slider-settings")
export class SliderSettings extends AssetWidgetSettings {
  // Override of widgetConfig with extended type
  protected readonly widgetConfig!: SliderWidgetConfig;

  protected render(): TemplateResult {
    return html`
      <!-- Attribute selection -->
      <settings-panel displayName="attributes" expanded="true">
        <attributes-panel
          .attributeRefs=${this.widgetConfig.attributeRefs}
          style="padding-bottom: 12px;"
          @attribute-select=${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}
        ></attributes-panel>
      </settings-panel>
      <settings-panel displayName="Feld" expanded="true">
        <or-mwc-input
          .type=${InputType.TEXT}
          .value=${this.widgetConfig.unit ?? ""}
          @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onUnitChange(e)}
          label="Einheit"
        ></or-mwc-input>
        <or-mwc-input
          .type=${InputType.NUMBER}
          .value=${this.widgetConfig.min ?? ""}
          @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onMinChange(e)}
          label="Wert von"
        ></or-mwc-input>
        <or-mwc-input
          .type=${InputType.NUMBER}
          .value=${this.widgetConfig.max ?? ""}
          @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onMaxChange(e)}
          label="Wert bis"
        ></or-mwc-input>
        <or-mwc-input
          .type=${InputType.NUMBER}
          .value=${this.widgetConfig.step ?? ""}
          @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onStepChange(e)}
          label="Schritte"
        ></or-mwc-input>
      </settings-panel>
      <settings-panel displayName="Aussehen" expanded="true">
        <span>Akzentfarbe: </span>
        <or-mwc-input
          type="${InputType.COLOUR}"
          value="${this.widgetConfig.accentColor}"
          @or-mwc-input-changed="${(event: Event) => this.onAccentColorChange(event)}"
        ></or-mwc-input>
      </settings-panel>
    `;
  }

  protected onAttributesSelect(ev: AttributesSelectEvent) {
    this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
    if (ev.detail.attributeRefs.length === 1) {
      const asset = ev.detail.assets.find((a) => a.id === ev.detail.attributeRefs[0].id);
      if (asset) this.setDisplayName!(asset.name);
    }
    this.notifyConfigUpdate();
  }

  protected onUnitChange(ev: OrInputChangedEvent) {
    this.widgetConfig.unit = ev.detail.value as string;
    this.notifyConfigUpdate();
  }

  protected onMinChange(ev: OrInputChangedEvent) {
    this.widgetConfig.min = ev.detail.value as number;
    this.notifyConfigUpdate();
  }

  protected onMaxChange(ev: OrInputChangedEvent) {
    this.widgetConfig.max = ev.detail.value as number;
    this.notifyConfigUpdate();
  }

  protected onStepChange(ev: OrInputChangedEvent) {
    this.widgetConfig.step = ev.detail.value as number;
    this.notifyConfigUpdate();
  }

  protected onAccentColorChange(ev: Event) {
    const input = ev.currentTarget as HTMLInputElement;
    this.widgetConfig.accentColor = input.value;
    this.notifyConfigUpdate();
  }
}
