import { css, html, PropertyValues, TemplateResult } from "lit";
import { customElement, state, query } from "lit/decorators.js";
import { WidgetManifest } from "../util/or-widget";
import { OrAssetWidget } from "../util/or-asset-widget";
import { AssetWidgetConfig } from "../util/widget-config";
import { BatterySettings } from "../settings/battery-settings";
import "@openremote/or-icon";
import "@openremote/or-attribute-input";
import { WidgetSettings } from "../util/widget-settings";
import { AttributeRef } from "@openremote/model";
import { throttle } from "lodash";
import { when } from "lit/directives/when.js";

export interface BatteryWidgetConfig extends AssetWidgetConfig {
  attributeRefs: AttributeRef[];
  showHelperText: boolean;
}

function getDefaultWidgetConfig(): BatteryWidgetConfig {
  return {
    attributeRefs: [],
    showHelperText: true,
  };
}

const styling = css`
  :host {
    display: block;
    width: 100%;
    height: 100%;
  }

  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  /* Wrapper für das ganze Widget */
  #widget-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center; 
    text-align: center;
    justify-content: center;
    box-sizing: border-box;
    overflow: hidden;
    padding: 8px;
    position: relative; 
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

  .attribute-timestamp {
    position: absolute; /* <— WICHTIG */
    left: 8px; /* unten links */
    bottom: 8px;
    font-size: 10px;
    color: #9ca3af;
    text-align: left;
    pointer-events: none; 
  }
`;

@customElement("battery-widget")
export class BatteryWidget extends OrAssetWidget {
  // Override of widgetConfig with extended type
  protected widgetConfig!: BatteryWidgetConfig;
  @state()
  protected _loading = false;

  @query("#widget-wrapper") private _wrapper?: HTMLElement;
  private _resizeObserver?: ResizeObserver;

  static getManifest(): WidgetManifest {
    return {
      displayName: "Battery", // name to display in widget browser
      displayIcon: "battery-high", // icon to display in widget browser. Uses <or-icon> and https://materialdesignicons.com
      minColumnWidth: 1,
      minColumnHeight: 1,
      getContentHtml(config: BatteryWidgetConfig): OrAssetWidget {
        return new BatteryWidget(config);
      },
      getSettingsHtml(config: BatteryWidgetConfig): WidgetSettings {
        return new BatterySettings(config);
      },
      getDefaultConfig(): BatteryWidgetConfig {
        return getDefaultWidgetConfig();
      },
    };
  }

  static get styles() {
    return [...super.styles, styling];
  }

  //  Beim ersten Mounten: ResizeObserver für Attribute‑Input-Workaround
  protected willUpdate(changed: PropertyValues) {
    const attributeRefs = this.widgetConfig.attributeRefs;
    if (attributeRefs.length === 0) {
      this._error = "noAttributesConnected";
    } else if (attributeRefs.length > 0 && !this.isAttributeRefLoaded(attributeRefs[0])) {
      this.loadAssets(attributeRefs);
    }

    // Deinen Resize‐Observer fürs Input‑Widget weiterlaufen lassen
    if (!this._resizeObserver && this._wrapper) {
      this._resizeObserver = new ResizeObserver(throttle(() => window.dispatchEvent(new Event("resize")), 200));
      this._resizeObserver.observe(this._wrapper);
    }

    return super.willUpdate(changed); 
  }

  protected loadAssets(attributeRefs: AttributeRef[]) {
    if (attributeRefs.length === 0) {
      this._error = "noAttributesConnected";
      return;
    }
    this._loading = true;
    this._error = undefined;
    this.fetchAssets(attributeRefs)
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


  public refreshContent(force: boolean): void {
    this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as BatteryWidgetConfig;
  }

  // Cleanup
  disconnectedCallback(): void {
    this._resizeObserver?.disconnect();
    super.disconnectedCallback();
  }

  protected firstUpdated(changed: PropertyValues): void {
    super.firstUpdated(changed);
    let lastSize = 0;
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        const newSize = Math.min(width, height) * 0.4;
        if (Math.abs(newSize - lastSize) > 0.5) {
          lastSize = newSize;
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

    return html`
      ${when(
        this._loading || this._error,
        () => {
          if (this._loading) {
            return html`<or-loading-indicator></or-loading-indicator>`;
          } else {
            return html`<or-translate id="error-txt" .value="${this._error}"></or-translate>`;
          }
        },
        () =>
          when(cfg.attributeRefs.length > 0 && attribute && this.loadedAssets && this.loadedAssets.length > 0, () => {
            return html`
              <div id="widget-wrapper">
                <div class="icon-container">
                  <or-icon
                    icon="${this.getBatteryIcon(attribute.value).iconName}"
                    style="color: ${this.getBatteryIcon(attribute.value).iconColour}"
                  ></or-icon>
                </div>
                <div class="attribute-value">${attribute.value}%</div>
                ${cfg.showHelperText
                  ? html`<div class="attribute-timestamp">${this.formatUpdatedAt(attribute.timestamp)}</div>`
                  : null}
              </div>
            `;
          })
      )}
    `;
  }

  private getBatteryIcon(batteryStatus: number): { iconName: string; iconColour: string } {
    let iconName = "battery-high";
    let iconColour = "green";

    if (batteryStatus <= 5) {
      iconName = "battery-alert-variant-outline";
      iconColour = "red";
    } else if (batteryStatus <= 33) {
      iconName = "battery-low";
      iconColour = "red";
    } else if (batteryStatus <= 66) {
      iconName = "battery-medium";
      iconColour = "orange";
    }

    return { iconName, iconColour };
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
