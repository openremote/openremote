import { css, html, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { BatteryWidgetConfig } from "../widgets/battery-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-icon";

export type RadioMode = "relativ" | "absolut";

const styling = css`
  .input-container {
    margin-top: 8px; /* Abstand zur Textbox, wenn "Absolut" ausgewählt wird */
  }
`;

@customElement("battery-settings")
export class BatterySettings extends AssetWidgetSettings {
  // Override of widgetConfig with extended type
  protected readonly widgetConfig!: BatteryWidgetConfig;
  @property({ type: String }) mode = "relativ"; // Standardmäßig 'relativ'
  @property({ type: Number }) absoluteValue: number | null = null;

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    return html`
      <settings-panel displayName="attributes" expanded="true">
        <attributes-panel
          .attributeRefs=${this.widgetConfig.attributeRefs}
          style="padding-bottom: 12px;"
          @attribute-select=${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}
        ></attributes-panel>
      </settings-panel>
      <settings-panel displayName="Einstellungen" expanded="true">
        <div class="switch-container">
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

  protected onAttributesSelect(ev: AttributesSelectEvent) {
    this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
    if (ev.detail.attributeRefs.length === 1) {
      const asset = ev.detail.assets.find((a) => a.id === ev.detail.attributeRefs[0].id);
      if (asset) this.setDisplayName!(asset.name);
    }
    this.notifyConfigUpdate();
  }

  protected onHelperTextToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showHelperText = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }
}
