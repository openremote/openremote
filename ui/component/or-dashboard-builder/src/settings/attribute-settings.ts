import { css, html, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { AttributeWidgetConfig } from "../widgets/attribute-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import "@openremote/or-icon";

const ICON_OPTIONS = [
  "lightbulb",
  "solar-power",
  "fan",
  "water-pump",
  "fire",
  "emoticon",
  "door",
  "water",
  "alert",
  "lightning-bolt",
];
type MappingOption = "value" | "range" | "special";
type ValueMapping = { value: string; color: string; type: "value" };
type RangeMapping = { min: number; max: number; color: string; type: "range" };

export type ValueMappingUnion = ValueMapping | RangeMapping;
export type TextMapping = { from: string; to: string };

const styling = css`
  :host {
    display: block;
  }
  .field {
    margin: 8px 0;
  }
  .icon-picker {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }
  .icon-option {
    cursor: pointer;
    padding: 4px;
    border: 2px solid transparent;
    border-radius: 4px;
    transition: border-color 0.2s;
  }
  .icon-option[selected] {
    border-color: var(--or-color-primary);
  }

  .mapping-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .mapping-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .mapping-item input[type="text"] {
    flex: 1;
  }

  .mapping-item input[type="number"] {
    width: 7ch;
    min-width: 0;
    box-sizing: border-box;
  }

  .mapping-item input[type="text"] {
    width: 6ch;
    min-width: 0;
    box-sizing: border-box;
  }

  .error-message {
    color: #b00020; 
    font-size: 0.8rem;
    margin-top: 0.25rem;
    /* optional: links einrücken, damit sie unter den Inputs sitzt */
    margin-left: 2.5rem;
  }

  .mapping-actions {
    position: relative; 
    display: inline-block;
  }

  .dropdown-content {
    position: absolute;
    top: calc(100% + 4px);
    left: 0;
    display: flex;
    flex-direction: column;
    padding: 4px 0;
    background: #fff;
    border: 1px solid #ccc;
    border-radius: 8px;
    min-width: 260px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    z-index: 100;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  }

  .dropdown-item {
    background: none;
    border: none;
    width: 100%;
    padding: 10px 16px;
    text-align: left;
    display: flex;
    flex-direction: column;
    gap: 2px;
    cursor: pointer;
    font: inherit;
    color: #222;
  }

  .dropdown-item:not(:last-child) {
    border-bottom: 1px solid #e6e6e6;
  }

  .item-title {
    font-size: 14px;
    font-weight: 600;
    line-height: 1.2;
  }

  .item-subtitle {
    font-size: 12px;
    line-height: 1.2;
    color: #6f6f6f;
  }

  .dropdown-item:hover,
  .dropdown-item:focus {
    background: #f5f5f8;
    outline: none;
  }
`;

@customElement("attribute-settings")
export class AttributeSettings extends AssetWidgetSettings {
  protected readonly widgetConfig!: AttributeWidgetConfig;
  @property({ type: Boolean }) private showMenu = false;

  @state() private rangeErrors: Record<number, string> = {};

  static get styles() {
    return [...super.styles, styling];
  }

  protected render(): TemplateResult {
    return html`
      <div>
        <!-- Attribute selection -->
        <settings-panel displayName="attributes" expanded="true">
          <attributes-panel
            .attributeRefs=${this.widgetConfig.attributeRefs}
            style="padding-bottom: 12px;"
            @attribute-select=${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}
          ></attributes-panel>
          <div class="field">
            <or-mwc-input
              .type=${InputType.TEXT}
              .value=${this.widgetConfig.variableLabel ?? ""}
              @or-mwc-input-changed=${(e: OrInputChangedEvent) => this.onVariableLabelChange(e)}
              label="Neuer Name der Variable"
            ></or-mwc-input>
          </div>
        </settings-panel>

        <!-- Icon visibility toggle -->
        <settings-panel displayName="Icon Einstellung" expanded="true">
          <div class="field">
            <span>Icon anzeigen</span>
            <or-mwc-input
              .type=${InputType.SWITCH}
              .value=${this.widgetConfig.showIcon}
              @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onShowIconToggle(ev)}
            ></or-mwc-input>
          </div>

          ${this.widgetConfig.showIcon
            ? html`
                <div class="field">
                  <span>Basisfarbe: </span>
                  <or-mwc-input
                    type="${InputType.COLOUR}"
                    value="${this.widgetConfig.iconColor}"
                    @or-mwc-input-changed="${(event: Event) => this.onIconColorChange(event)}"
                  ></or-mwc-input>
                </div>

                <div class="field">
                  <span>Icon auswählen</span>
                  <div class="icon-picker">
                    ${ICON_OPTIONS.map(
                      (iconName) => html`
                        <div
                          class="icon-option"
                          ?selected=${this.widgetConfig.icon === iconName}
                          @click=${() => this.onIconPicked(iconName)}
                        >
                          <or-icon icon=${iconName} style="--or-icon-width:32px; --or-icon-height:32px;"></or-icon>
                        </div>
                      `
                    )}
                  </div>
                </div>
              `
            : null}
        </settings-panel>

        <!-- Mapping settings (only if icon is shown) -->
        ${this.widgetConfig.showIcon
          ? html`
              <settings-panel displayName="Value Mapping (Icon)" expanded>
                <div class="field">
                  <div class="mapping-list">
                    ${this.widgetConfig.valueMappings && this.widgetConfig.valueMappings.length
                      ? Array.from(this.groupedMappings.entries()).map(
                          ([type, entries]) => html`
                            <div class="mapping-group">
                              <div class="group-title">${type === "Allgemein" ? "Mapping" : `Mapping für ${type}`}</div>
                              ${entries.map(
                                ({ mapping, idx }: { mapping: ValueMappingUnion; idx: number }) => html`
                                  <div class="mapping-item">
                                    <!-- je nach Typ anderes UI -->
                                    ${mapping.type === "value"
                                      ? html`
                                          <input
                                            type="text"
                                            placeholder="Wert"
                                            .value=${(mapping as ValueMapping).value}
                                            @input=${(e: Event) => this.onMappingValueChange(e, idx)}
                                          />
                                        `
                                      : null}
                                    ${mapping.type === "range"
                                      ? html`
                                          <div class="range-inputs">
                                            <label>
                                              Von
                                              <input
                                                type="number"
                                                .value=${(mapping as RangeMapping).min}
                                                @input=${(e: Event) => this.onRangeChange("min", e, idx)}
                                              />
                                            </label>
                                            <label>
                                              Bis
                                              <input
                                                type="number"
                                                .value=${(mapping as RangeMapping).max}
                                                @input=${(e: Event) => this.onRangeChange("max", e, idx)}
                                              />
                                            </label>
                                          </div>
                                        `
                                      : null}

                                    <!-- Farbpicker immer -->
                                    <or-mwc-input
                                      type="${InputType.COLOUR}"
                                      value="${(mapping as any).color}"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) =>
                                        this.onMappingColorChange(event, idx)}"
                                    ></or-mwc-input>
                                    <or-mwc-input
                                      .type=${InputType.BUTTON}
                                      icon="delete-outline"
                                      @or-mwc-input-changed=${() => this.onRemoveTextMapping(idx)}
                                    ></or-mwc-input>
                                  </div>

                                  ${this.rangeErrors[idx]
                                    ? html`<div class="error-message">${this.rangeErrors[idx]}</div>`
                                    : null}
                                `
                              )}
                            </div>
                          `
                        )
                      : html`<div>Keine Mappings definiert</div>`}
                  </div>
                </div>

                <div class="mapping-actions">
                  <or-mwc-input
                    .type=${InputType.BUTTON}
                    label="Mapping hinzufügen"
                    style="margin-top: 8px;"
                    icon="triangle-down"
                    @or-mwc-input-changed=${() => (this.showMenu = !this.showMenu)}
                  ></or-mwc-input>
                  ${this.showMenu
                    ? html`
                        <div class="dropdown-content" role="menu" aria-label="Mapping Optionen">
                          ${[
                            {
                              title: "Value",
                              subtitle: "Match a specific value",
                              action: () => this._onOptionSelected("value"),
                            },
                            {
                              title: "Range",
                              subtitle: "Match a numerical range of values",
                              action: () => this._onOptionSelected("range"),
                            },
                          ].map(
                            (opt) => html`
                              <button
                                class="dropdown-item"
                                role="menuitem"
                                @click=${() => {
                                  opt.action();
                                }}
                              >
                                <div class="item-title">${opt.title}</div>
                                <div class="item-subtitle">${opt.subtitle}</div>
                              </button>
                            `
                          )}
                        </div>
                      `
                    : null}
                </div>
              </settings-panel>
            `
          : null}

        <!-- Other settings -->
        <settings-panel displayName="Text Mapping" expanded>
          <div class="field">
            <div class="mapping-list">
              ${this.widgetConfig.textMappings.length
                ? this.widgetConfig.textMappings.map(
                    (m, idx) => html`
                      <div class="mapping-item">
                        <input
                          type="text"
                          placeholder="Original"
                          .value=${m.from}
                          @input=${(e: Event) => this.onTextFromChange(e, idx)}
                        />
                        <input
                          type="text"
                          placeholder="Ersetzen durch"
                          .value=${m.to}
                          @input=${(e: Event) => this.onTextToChange(e, idx)}
                        />
                        <or-mwc-input
                          .type=${InputType.BUTTON}
                          icon="delete-outline"
                          @or-mwc-input-changed=${() => this.onRemoveTextMapping(idx)}
                        ></or-mwc-input>
                      </div>
                    `
                  )
                : html`<div>Keine Text-Mappings definiert</div>`}
            </div>
          </div>

          <div class="mapping-actions">
            <or-mwc-input
              .type=${InputType.BUTTON}
              style="margin-top: 8px;"
              label="Text-Mapping"
              icon="plus"
              @or-mwc-input-changed=${() => this.addTextMapping()}
            ></or-mwc-input>
          </div>
        </settings-panel>

        <settings-panel displayName="settings" expanded="true">
          <div class="field">
            <!-- Toggle helper text -->
            <div class="switch-container">
              <div class="switch-row">
                <span><or-translate value="dashboard.showHelperText"></or-translate></span>
                <or-mwc-input
                  .type=${InputType.SWITCH}
                  .value=${this.widgetConfig.showHelperText}
                  @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onHelperTextToggle(ev)}
                ></or-mwc-input>
              </div>

              <div class="switch-row">
                <span>Variable anzeigen</span>
                <or-mwc-input
                  .type=${InputType.SWITCH}
                  .value=${this.widgetConfig.showVariable}
                  @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onShowVariableToggle(ev)}
                ></or-mwc-input>
              </div>

              <div class="switch-row">
                <span>Value anzeigen</span>
                <or-mwc-input
                  .type=${InputType.SWITCH}
                  .value=${this.widgetConfig.showValue}
                  @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onShowValueToggle(ev)}
                ></or-mwc-input>
              </div>
            </div>
          </div>
        </settings-panel>
      </div>
    `;
  }

  private onIconPicked(iconName: string) {
    this.widgetConfig.icon = iconName;
    this.notifyConfigUpdate();
  }

  private onShowIconToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showIcon = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }

  private onShowValueToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showValue = ev.detail.value as boolean;
    this.notifyConfigUpdate();
  }

  private onShowVariableToggle(ev: OrInputChangedEvent) {
    this.widgetConfig.showVariable = ev.detail.value as boolean;
    this.notifyConfigUpdate();
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

  private onRemoveMapping(idx: number) {
    this.widgetConfig.valueMappings = this.widgetConfig.valueMappings!.filter((_, i) => i !== idx);
    this.notifyConfigUpdate();
  }

  private _onOptionSelected(option: MappingOption) {
    this._applyOption(option);
    this.showMenu = false;
  }

  private _applyOption(option: MappingOption) {
    this.addMapping(option);
  }

  private onTextFromChange(e: Event, idx: number) {
    const input = e.currentTarget as HTMLInputElement;
    this.widgetConfig.textMappings[idx].from = input.value;
    this.notifyConfigUpdate();
  }

  private onTextToChange(e: Event, idx: number) {
    const input = e.currentTarget as HTMLInputElement;
    this.widgetConfig.textMappings[idx].to = input.value;
    this.notifyConfigUpdate();
  }

  private addTextMapping() {
    this.widgetConfig.textMappings.push({ from: "", to: "" });
    this.notifyConfigUpdate();
  }

  private onRemoveTextMapping(idx: number) {
    this.widgetConfig.textMappings.splice(idx, 1);
    this.notifyConfigUpdate();
  }

  private onVariableLabelChange(e: Event) {
    const input = e.currentTarget as HTMLInputElement;
    this.widgetConfig.variableLabel = input.value;
    this.notifyConfigUpdate();
  }

  private onIconColorChange(e: Event) {
    const input = e.currentTarget as HTMLInputElement;
    this.widgetConfig.iconColor = input.value;
    this.notifyConfigUpdate();
  }

  private addMapping(option: "value" | "range" | "regex" | "special") {
    // vorhandene Mappings nehmen oder leeres Array
    const arr = this.widgetConfig.valueMappings ?? [];

    // je nach Option korrekt typisiertes Mapping anlegen
    let newMapping: ValueMappingUnion;
    switch (option) {
      case "value":
        newMapping = {
          type: "value",
          value: "",
          color: "#000000",
        };
        break;

      case "range":
        newMapping = {
          type: "range",
          min: 0,
          max: 0,
          color: "#000000",
        };
        break;

      default:
        return;
    }

    // ins Config-Array pushen und Config updaten
    this.widgetConfig.valueMappings = [...arr, newMapping];
    this.notifyConfigUpdate();
  }

  private get groupedMappings(): Map<string, Array<{ mapping: ValueMappingUnion; idx: number }>> {
    const map = new Map<string, Array<{ mapping: ValueMappingUnion; idx: number }>>();

    (this.widgetConfig.valueMappings ?? []).forEach((m, idx) => {
      const key = m.type;

      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)!.push({ mapping: m, idx });
    });

    return map;
  }

  private onMappingValueChange(e: Event, idx: number) {
    const input = e.currentTarget as HTMLInputElement;
    const mappings = this.widgetConfig.valueMappings ?? [];
    const m = mappings[idx];

    if (m.type === "value") {
      m.value = input.value;
      this.notifyConfigUpdate();
    }
  }

  private onRangeChange(field: "min" | "max", e: Event, idx: number) {
    const arr = this.widgetConfig.valueMappings;
    if (!arr) return;

    const mapping = arr[idx];
    // Guard: wenn’s kein Range-Mapping ist, raus
    if (mapping.type !== "range") return;

    const input = e.currentTarget as HTMLInputElement;
    const num = parseFloat(input.value);
    if (isNaN(num)) return;

    // explizit pro Feld …
    if (field === "min") {
      mapping.min = num;
    } else {
      mapping.max = num;
    }

    this.validateRangeMappings();
    this.requestUpdate();
  }

  private validateRangeMappings() {
    const errors: Record<number, string> = {};

    // Sammle alle Range-Mappings
    const ranges = this.widgetConfig.valueMappings
      .map((m, i) => (m.type === "range" ? { idx: i, min: m.min, max: m.max } : null))
      .filter((x): x is { idx: number; min: number; max: number } => x !== null);

    // 1. Einzel-Validation: min <= max
    for (const { idx, min, max } of ranges) {
      if (min > max) {
        errors[idx] = "Ungültiges Mapping: Min darf nicht größer als Max sein";
      }
    }

    // 2. Paar-Validation: Überlappungen
    for (let i = 0; i < ranges.length; i++) {
      for (let j = i + 1; j < ranges.length; j++) {
        const a = ranges[i],
          b = ranges[j];
        const overlap = a.min <= b.max && b.min <= a.max;
        if (overlap) {
          const msg = "Ungültiges Mapping: Überschneidet sich mit anderem Bereich";
          errors[a.idx] = errors[a.idx] ? errors[a.idx] + "; Überschneidung" : msg;
          errors[b.idx] = errors[b.idx] ? errors[b.idx] + "; Überschneidung" : msg;
        }
      }
    }

    this.rangeErrors = errors;
  }

  private onMappingColorChange(e: Event, idx: number) {
    const input = e.currentTarget as HTMLInputElement;
    const mappings = this.widgetConfig.valueMappings ?? [];
    const m = mappings[idx];

    // Farbe existiert in allen Mapping-Typen
    m.color = input.value;
    this.notifyConfigUpdate();
  }
}
