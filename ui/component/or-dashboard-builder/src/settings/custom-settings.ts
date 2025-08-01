import { css, html, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";
import { CustomWidgetConfig } from "../widgets/custom-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import "@openremote/or-icon";

const ICON_OPTIONS = ["lightbulb", "solar-power", "fan", "water-pump", "fire", "emoticon"];
export type ValueMapping = { value: string; color: string; type?: string };

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

  .mapping-actions {
    position: relative; /* Bezug für das absolute Dropdown */
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

@customElement("custom-settings")
export class CustomSettings extends AssetWidgetSettings {
  protected readonly widgetConfig!: CustomWidgetConfig;
  @property({ type: Boolean }) private showMenu = false;
  @property({ type: String }) private selectedOption: string | null = null;

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
            ? html`<div class="field">
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
              </div>`
            : null}
        </settings-panel>

        <!-- Mapping settings (only if icon is shown) -->
        ${this.widgetConfig.showIcon
          ? html`
              <settings-panel displayName="Value Mapping" expanded>
                <div class="field">
                  <div class="mapping-list">
                    ${this.widgetConfig.valueMappings && this.widgetConfig.valueMappings.length
                      ? Array.from(this.groupedMappings.entries()).map(
                          ([type, entries]) => html`
                            <div class="mapping-group">
                              <div class="group-title">${type === "Allgemein" ? "Mapping" : `Mapping für ${type}`}</div>
                              ${entries.map(
                                ({ mapping, idx }) => html`
                                  <div class="mapping-item">
                                    <input
                                      type="text"
                                      placeholder="Wert"
                                      .value=${mapping.value}
                                      @input=${(e: Event) => this.onMappingValueChange(e, idx)}
                                    />
                                    <input
                                      type="color"
                                      .value=${mapping.color}
                                      @input=${(e: Event) => this.onMappingColorChange(e, idx)}
                                    />
                                    <button @click=${() => this.onRemoveMapping(idx)}>✕</button>
                                  </div>
                                `
                              )}
                            </div>
                          `
                        )
                      : html`<div>Keine Mappings definiert</div>`}
                  </div>
                </div>

                <div class="mapping-actions">
                  <button
                    @click=${(e: MouseEvent) => {
                      e.stopPropagation();
                      this.showMenu = !this.showMenu;
                    }}
                    aria-haspopup="true"
                    aria-expanded=${this.showMenu}
                  >
                    Mapping hinzufügen ▼
                  </button>

                  ${this.showMenu
                    ? html`
                        <div class="dropdown-content" role="menu" aria-label="Mapping Optionen">
                          ${[
                            {
                              title: "Value",
                              subtitle: "Match a specific text value",
                              action: () => this._onOptionSelected("value"),
                            },
                            {
                              title: "Range",
                              subtitle: "Match a numerical range of values",
                              action: () => this._onOptionSelected("range"),
                            },
                            {
                              title: "Regex",
                              subtitle: "Match a regular expression with replacement",
                              action: () => this._onOptionSelected("regex"),
                            },
                            {
                              title: "Special",
                              subtitle: "Match on null, NaN, boolean and empty values",
                              action: () => this._onOptionSelected("special"),
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
        <settings-panel displayName="settings" expanded="true">
          <div class="field">
            <!-- Toggle helper text -->
            <div class="switch-container">
              <span><or-translate value="dashboard.showHelperText"></or-translate></span>
              <or-mwc-input
                .type=${InputType.SWITCH}
                style="margin: 0 -10px;"
                .value=${this.widgetConfig.showHelperText}
                @or-mwc-input-changed=${(ev: OrInputChangedEvent) => this.onHelperTextToggle(ev)}
              ></or-mwc-input>
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

  private onAddMapping() {
    this.widgetConfig.valueMappings = [...(this.widgetConfig.valueMappings || []), { value: "", color: "#000000" }];
    this.notifyConfigUpdate();
    this.showMenu = false;
  }

  private onRemoveMapping(idx: number) {
    this.widgetConfig.valueMappings = this.widgetConfig.valueMappings!.filter((_, i) => i !== idx);
    this.notifyConfigUpdate();
  }

  private onMappingValueChange(e: Event, idx: number) {
    const val = (e.target as HTMLInputElement).value;
    this.widgetConfig.valueMappings![idx].value = val;
    this.notifyConfigUpdate();
  }

  private onMappingColorChange(e: Event, idx: number) {
    const col = (e.target as HTMLInputElement).value;
    this.widgetConfig.valueMappings![idx].color = col;
    this.notifyConfigUpdate();
  }

  private _onOptionSelected(option: string) {
    this.selectedOption = option;
    this._applyOption(option);
    this.showMenu = false;
  }

  private _applyOption(option: string) {
    switch (option) {
      case "Option 1":
        this.addMapping({ value: "A-default", color: "#ff0000", type: option });
        break;
      default:
        this.onAddMapping();
    }
  }

  private addMapping(newMap: { value: string; color: string; type?: string }) {
    // Falls deine valueMappings noch kein "type" kennt, kannst du dein Interface erweitern.
    this.widgetConfig.valueMappings = [...(this.widgetConfig.valueMappings || []), newMap as any];
    this.notifyConfigUpdate();
  }

  private get groupedMappings(): Map<string, Array<{ mapping: ValueMapping; idx: number }>> {
    const map = new Map<string, Array<{ mapping: ValueMapping; idx: number }>>();
    (this.widgetConfig.valueMappings || []).forEach((m, idx) => {
      const key = m.type || "Allgemein";
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)!.push({ mapping: m, idx });
    });
    return map;
  }
}
