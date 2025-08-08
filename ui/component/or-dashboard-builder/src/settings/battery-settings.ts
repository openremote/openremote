import { css, html, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { BatteryWidgetConfig } from "../widgets/battery-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import "@openremote/or-icon";

@customElement("battery-settings")
export class BatterySettings extends AssetWidgetSettings {
  // Override of widgetConfig with extended type
  protected readonly widgetConfig!: BatteryWidgetConfig;
  @property({ type: String }) mode = 'relativ'; // Standardmäßig 'relativ'
  @property({ type: Number }) absoluteValue: number | null = null;

  protected render(): TemplateResult {
    return html`
      <settings-panel displayName="attributes" expanded="true">
        <attributes-panel
          .attributeRefs=${this.widgetConfig.attributeRefs}
          style="padding-bottom: 12px;"
          @attribute-select=${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}
        ></attributes-panel>
      </settings-panel>
      <div class="settings-container">
        <label>
          <input
            type="radio"
            name="mode"
            value="relativ"
            .checked=${this.mode === "relativ"}
            @change=${this.handleRadioChange}
          />
          Relativ (Nimmt den Wert von attribute.value)
        </label>
        <label>
          <input
            type="radio"
            name="mode"
            value="absolut"
            .checked=${this.mode === "absolut"}
            @change=${this.handleRadioChange}
          />
          Absolut (Benutzerdefinierte Zahl eingeben)
        </label>

        ${this.mode === "absolut"
          ? html` <div class="input-container">
              <label for="absoluteValue">Absolute Zahl:</label>
              <input
                id="absoluteValue"
                type="number"
                .value=${this.absoluteValue || ""}
                @input=${this.handleAbsoluteValueChange}
              />
            </div>`
          : ""}
      </div>
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

  // Event-Handler für die Änderung der Radio-Buttons
  handleRadioChange(event: Event) {
    const radio = event.target as HTMLInputElement;
    this.mode = radio.value;
    if (this.mode === 'relativ') {
      this.absoluteValue = null; // Reset der absoluten Zahl, wenn relativ gewählt wird
    }
  }

  // Event-Handler für die Eingabe der absoluten Zahl
  handleAbsoluteValueChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.absoluteValue = Number(input.value);
  }
}
