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
}

function getDefaultWidgetConfig(): BatteryWidgetConfig {
  return {
    attributeRefs: [],
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

    return super.willUpdate(changed); // oder super.willUpdate(...) retour geben, je nach Basis‑Implementierung
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

  //  Daten (Assets) neu laden, wenn sich attributeRefs ändern
  public refreshContent(force: boolean): void {
    this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as BatteryWidgetConfig;
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

    console.log("Error:", this._error);

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
              </div>
            `;
          })
      )}
    `;
  }

  private getBatteryIcon(batteryStatus: number): { iconName: string; iconColour: string } {
    let iconName = "battery-high";
    let iconColour = "green";

    if (batteryStatus < 5) {
      iconName = "battery-alert-variant-outline";
      iconColour = "red";
    } else if (batteryStatus < 33) {
      iconName = "battery-low";
      iconColour = "red";
    } else if (batteryStatus < 66) {
      iconName = "battery-medium";
      iconColour = "orange";
    }

    return { iconName, iconColour };
  }
}
