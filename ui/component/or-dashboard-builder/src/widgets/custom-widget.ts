// File: custom-widget.ts
import { css, html, PropertyValues, TemplateResult } from "lit";
import { customElement, state, query } from "lit/decorators.js";
import { throttle } from "lodash";
import { WidgetManifest } from "../util/or-widget";
import { OrAssetWidget } from "../util/or-asset-widget";
import { AssetWidgetConfig } from "../util/widget-config";
import { CustomSettings } from "../settings/custom-settings";
import "@openremote/or-icon";
import "@openremote/or-attribute-input";
import type { ValueMappingUnion, TextMapping } from "../settings/custom-settings";

export interface CustomWidgetConfig extends AssetWidgetConfig {
  customFieldTwo: number;
  readonly: boolean;
  showHelperText: boolean;
  showVariable: boolean;
  showValue: boolean;
  icon?: string;
  showIcon: boolean;
  valueMappings: ValueMappingUnion[];
  textMappings: TextMapping[];
  variableLabel?: string;
  iconColor: string;
}

function getDefaultWidgetConfig(): CustomWidgetConfig {
  return {
    attributeRefs: [],
    customFieldTwo: 0,
    readonly: true,
    showHelperText: true,
    showVariable: true,
    showValue: true,
    icon: "lightbulb",
    showIcon: true,
    valueMappings: [],
    textMappings: [],
    variableLabel: "",
    iconColor: "inherit"
  };
}

const styling = css`
  :host {
    display: block;
    width: 100%;
    height: 100%;
    --icon-size: 48px;
  }

  /* Wrapper für das ganze Widget */
  #widget-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center; /* Icon+Input mittig */
    text-align: center;
    justify-content: center;
    box-sizing: border-box;
    overflow: hidden;
    padding: 8px;
  }

  /* Icon-Area */
  .icon-container {
    margin-bottom: 8px;
  }
  .icon-container or-icon {
    --or-icon-width: var(--icon-size);
    --or-icon-height: var(--icon-size);
  }

  .attribute-value {
    font-size: 1.5rem;
    font-weight: 600;
    flex-shrink: 0;
  }

  /* Error-Text */
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  .attribute-timestamp {
    font-size: 12px;
    color: #777777ff;
  }
`;

@customElement("custom-widget")
export class CustomWidget extends OrAssetWidget {
  protected readonly widgetConfig!: CustomWidgetConfig;

  @state() private _loading = false;
  @state() protected _error?: string;

  @query("#widget-wrapper") private _wrapper?: HTMLElement;
  private _resizeObserver?: ResizeObserver;

  static getManifest(): WidgetManifest {
    return {
      displayName: "Custom widget",
      displayIcon: "emoticon",
      minColumnWidth: 1,
      minColumnHeight: 1,
      getContentHtml: (cfg: CustomWidgetConfig) => new CustomWidget(cfg),
      getSettingsHtml: (cfg: CustomWidgetConfig) => new CustomSettings(cfg),
      getDefaultConfig: () => getDefaultWidgetConfig(),
    };
  }

  static get styles() {
    return [...super.styles, styling];
  }

  //  Beim ersten Mounten: ResizeObserver für Attribute‑Input-Workaround
  protected willUpdate(changed: PropertyValues) {
    // erst Routinen vom Parent ausführen (z.B. Grid‑Resize‑Observer)
    super.willUpdate(changed);

    // Sobald widgetConfig (und damit attributeRefs) geändert wird:
    if (changed.has("widgetConfig") && this.widgetConfig.attributeRefs.length > 0) {
      this._loading = true;
      this._error = undefined;
      this.fetchAssets(this.widgetConfig.attributeRefs)
        .then((assets) => {
          this.loadedAssets = assets;
        })
        .catch((e) => {
          this._error = e.message;
        })
        .finally(() => {
          this._loading = false;
        });
    }

    // Deinen Resize‐Observer fürs Input‑Widget weiterlaufen lassen
    if (!this._resizeObserver && this._wrapper) {
      this._resizeObserver = new ResizeObserver(throttle(() => window.dispatchEvent(new Event("resize")), 200));
      this._resizeObserver.observe(this._wrapper);
    }

    return true; // oder super.willUpdate(...) retour geben, je nach Basis‑Implementierung
  }

  //  Daten (Assets) neu laden, wenn sich attributeRefs ändern
  public refreshContent(force: boolean): void {
    const refs = this.widgetConfig.attributeRefs;
    if (!refs || refs.length === 0) {
      this._error = "noAttributesConnected";
      this._loading = false;
      return;
    }
    this._loading = true;
    this._error = undefined;

    this.fetchAssets(refs)
      .then((assets) => {
        this.loadedAssets = assets;
      })
      .catch((e) => {
        this._error = e.message;
      })
      .finally(() => {
        this._loading = false;
      });
  }

  // Cleanup
  disconnectedCallback(): void {
    this._resizeObserver?.disconnect();
    super.disconnectedCallback();
  }

  // Responsive Icon-Größe
  protected firstUpdated(changed: PropertyValues): void {
    super.firstUpdated(changed);
    let lastSize = 0;
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        const newSize = Math.min(width, height) * 0.4;
        // Nur bei echtem Unterschied (z.B. >0.5px) updaten
        if (Math.abs(newSize - lastSize) > 0.5) {
          lastSize = newSize;
          // Asynchron setzen, um den Resize‑Loop zu unterbrechen
          requestAnimationFrame(() => {
            this.style.setProperty("--icon-size", `${newSize}px`);
          });
        }
      }
    });
    ro.observe(this);
    this._resizeObserver = ro;
  }

  protected render(): TemplateResult {
    const cfg = this.widgetConfig;
    const asset = this.loadedAssets[0];
    const attrRef = cfg.attributeRefs[0];
    const attribute: any = asset?.attributes?.[attrRef?.name!];

    if (!attribute) {
      return html`<div id="error-txt">Kein Attribut ausgewählt</div>`;
    }

    const rawStr = String(attribute.value);
    const epoch = this.formatUpdatedAt(attribute.timestamp);

    // → Hier iconColor holen
    const iconColor = this.applyMappings(rawStr);

    const displayValue = this.applyTextMappings(rawStr);

    return html`
      <div id="widget-wrapper" class="widget-container">
        ${cfg.showIcon
          ? html`<div class="icon-container">
              <or-icon icon="${cfg.icon}" style="color:${iconColor}"></or-icon>
            </div>`
          : null}
        <div class="info">
          ${cfg.showVariable
            ? html`<div class="attribute-name">
                ${cfg.variableLabel && cfg.variableLabel.length ? cfg.variableLabel : attribute.name}:
              </div>`
            : null}
          ${cfg.showValue ? html`<div class="attribute-value">${displayValue}</div>` : null}
        </div>
        ${cfg.showHelperText ? html`<div class="attribute-timestamp">${epoch}</div>` : null}
      </div>
    `;
  }

  private applyTextMappings(raw: string): string {
    for (const tm of this.widgetConfig.textMappings ?? []) {
      if (tm.from === raw) {
        return tm.to;
      }
    }
    return raw;
  }

  // → nun: Rückgabewert string
  private applyMappings(rawStr: string): string {
    let iconColor = this.widgetConfig.iconColor;

    for (const m of this.widgetConfig.valueMappings ?? []) {
      switch (m.type) {
        case "value":
          if (m.value === rawStr) {
            iconColor = m.color;
            break;
          }
          continue;

        case "range":
          const num = parseFloat(rawStr);
          if (!isNaN(num) && num >= m.min && num <= m.max) {
            iconColor = m.color;
            break;
          }
          continue;
      }
      if (iconColor !== "inherit") {
        break;
      }
    }

    return iconColor;
  }

  private formatUpdatedAt(epoch: number): string {
    const now = new Date();
    const date = new Date(epoch < 1e12 ? epoch * 1000 : epoch); // Sekunden oder ms erkennen
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 10) {
      return "Aktualisiert gerade eben";
    }
    if (diffMin < 1) {
      return `Aktualisiert vor ${diffSec} ${diffSec === 1 ? "Sekunde" : "Sekunden"}`;
    }
    if (diffHour < 1) {
      return `Aktualisiert vor ${diffMin} ${diffMin === 1 ? "Minute" : "Minuten"}`;
    }

    // Stunden (weniger als 24h)
    if (diffHour < 24) {
      return `Aktualisiert vor ${diffHour} ${diffHour === 1 ? "Stunde" : "Stunden"}`;
    }

    // gestern (zwischen 24 und 48 Stunden)
    if (diffDay === 1) {
      const time = date.toLocaleTimeString("de-DE", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });
      return `Aktualisiert gestern um ${time}`;
    }

    // innerhalb der letzten Woche (ab 2 Tagen bis <7)
    if (diffDay < 7) {
      return `Aktualisiert vor ${diffDay} ${diffDay === 1 ? "Tag" : "Tagen"}`;
    }

    // älter: absolute Darstellung
    const parts = new Intl.DateTimeFormat("de-DE", {
      weekday: "short",
      day: "numeric",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).formatToParts(date);

    const get = (type: string) => parts.find((p) => p.type === type)?.value ?? "";

    let weekday = get("weekday");
    if (!weekday.endsWith(".")) weekday += ".";
    const day = get("day");
    const month = get("month");
    const year = get("year");
    const hour = get("hour");
    const minute = get("minute");

    return `Aktualisiert am: ${weekday} ${day}. ${month} ${year} ${hour}:${minute}`;
  }
}
