import { css, html, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";
import { CustomWidgetConfig } from "../widgets/custom-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import "@openremote/or-icon";

const ICON_OPTIONS = ["lightbulb", "solar-power", "fan", "water-pump", "fire", "emoticon"];

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
`;

@customElement("custom-settings")
export class CustomSettings extends AssetWidgetSettings {
  protected readonly widgetConfig!: CustomWidgetConfig;
  @property({ type: Boolean }) private showMenu = false;

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
                  <span>Mapping-Liste</span>
                  <div class="mapping-list">
                    ${this.widgetConfig.valueMappings?.map(
                      (m, idx) => html`
                        <div class="mapping-item">
                          <input
                            type="text"
                            placeholder="Wert"
                            .value=${m.value}
                            @input=${(e: Event) => this.onMappingValueChange(e, idx)}
                          />
                          <input
                            type="color"
                            .value=${m.color}
                            @input=${(e: Event) => this.onMappingColorChange(e, idx)}
                          />
                          <button @click=${() => this.onRemoveMapping(idx)}>✕</button>
                        </div>
                      `
                    ) ?? html`<div>Keine Mappings definiert</div>`}
                  </div>
                </div>
                <button
                  @click=${(e: MouseEvent) => {
                    this.toggleMenu(e);
                    if (!this.showMenu) {
                      this.onAddMapping();
                    }
                  }}
                  aria-haspopup="true"
                  aria-expanded=${this.showMenu}
                >
                  Mapping hinzufügen ▼
                </button>

                <!-- 3. Konditionales Dropdown -->
                ${this.showMenu
                  ? html`
                      <ul class="dropdown" role="menu" aria-label="Mapping Optionen">
                        <li role="none">
                          <a role="menuitem" href="#" @click=${() => this._select("Option A")}>Option A</a>
                        </li>
                        <li role="none">
                          <a role="menuitem" href="#" @click=${() => this._select("Option B")}>Option B</a>
                        </li>
                        <li role="none">
                          <a role="menuitem" href="#" @click=${() => this._select("Option C")}>Option C</a>
                        </li>
                      </ul>
                    `
                  : ""}
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

  private toggleMenu(e: MouseEvent) {
    e.stopPropagation();
    this.showMenu = !this.showMenu;
  }

  private _select(choice: string) {
    console.log("Gewählt:", choice);
    this.showMenu = false;
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
}
