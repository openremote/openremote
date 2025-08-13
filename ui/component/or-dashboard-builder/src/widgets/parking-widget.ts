import { css, html, PropertyValues, TemplateResult } from "lit";
import { customElement, state, query } from "lit/decorators.js";
import { WidgetManifest } from "../util/or-widget";
import { OrAssetWidget } from "../util/or-asset-widget";
import { AssetWidgetConfig } from "../util/widget-config";
import { ParkingSettings } from "../settings/parking-settings";
import "@openremote/or-icon";
import "@openremote/or-attribute-input";
import { WidgetSettings } from "../util/widget-settings";
import { AttributeRef } from "@openremote/model";
import { throttle } from "lodash";
import { when } from "lit/directives/when.js";

const styling = css`
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  .parking-card {
    width: 240px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.06);
    padding: 16px 18px 14px;
    display: grid;
    grid-template-rows: auto auto auto;
    gap: 10px;
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, Ubuntu, "Helvetica Neue", Arial, sans-serif;
  }

  .parking-icon {
    --or-icon-width: 56px;
    --or-icon-height: 56px;
    --or-icon-fill: #ffffffff; /* Farbe des Icons */
    border-radius: 8px;
    background: #1976d2;
    justify-self: center;
  }

  .parking-card .status {
    border-radius: 10px;
    padding: 10px 14px;
    font-weight: 700;
    text-align: center;
    color: #fff;
  }
  .parking-card.free .status {
    background: #2ecc71;
  }
  .parking-card.occupied .status {
    background: #e74c3c;
  }

  .parking-card .bus {
    text-align: center;
    color: #6b7280;
    font-size: 18px;
  }
  .parking-card .bus .label {
    color: #6b7280;
    margin-right: 6px;
  }
  .parking-card .bus .value.empty {
    opacity: 0.6;
  }
`;

export interface ParkingWidgetConfig extends AssetWidgetConfig {
  attributeRefs: AttributeRef[];
  occupiedRefs: AttributeRef[];
  busRefs: AttributeRef[];
  invertOccupied: boolean;
}

function getDefaultWidgetConfig(): ParkingWidgetConfig {
  return {
    attributeRefs: [],
    occupiedRefs: [],
    busRefs: [],
    invertOccupied: false,
  };
}

@customElement("parking-widget")
export class ParkingWidget extends OrAssetWidget {
  // Override of widgetConfig with extended type
  protected widgetConfig!: ParkingWidgetConfig;

  @state()
  protected _loading = false;

  @query("#widget-wrapper") private _wrapper?: HTMLElement;
  private _resizeObserver?: ResizeObserver;

  static getManifest(): WidgetManifest {
    return {
      displayName: "Parking", // name to display in widget browser
      displayIcon: "parking", // icon to display in widget browser. Uses <or-icon> and https://materialdesignicons.com
      getContentHtml(config: ParkingWidgetConfig): OrAssetWidget {
        return new ParkingWidget(config);
      },
      getSettingsHtml(config: ParkingWidgetConfig): WidgetSettings {
        return new ParkingSettings(config);
      },
      getDefaultConfig(): ParkingWidgetConfig {
        return getDefaultWidgetConfig();
      },
    };
  }

  static get styles() {
    return [...super.styles, styling];
  }

  //  Beim ersten Mounten: ResizeObserver für Attribute‑Input-Workaround
  protected willUpdate(changed: PropertyValues) {
    const occupiedRefs = this.widgetConfig.occupiedRefs ?? [];
    const busRefs = this.widgetConfig.busRefs ?? [];

    if (occupiedRefs.length === 0 && busRefs.length === 0) {
      // gar nichts verbunden
      this._error = "noAttributesConnected";
    } else if (occupiedRefs.length === 0 || busRefs.length === 0) {
      // genau eins fehlt → gezielte Fehlermeldung, KEIN Laden
      this._error =
        occupiedRefs.length === 0
          ? "missingOccupiedAttribute" // „Bitte auch das Belegt-Attribut verbinden“
          : "missingBusAttribute"; // „Bitte auch das Bus-Attribut verbinden“
    } else {
      // beide vorhanden → Fehler zurücksetzen und ggf. laden
      this._error = undefined;

      const refsToLoad = [...occupiedRefs, ...busRefs].filter((r) => !this.isAttributeRefLoaded(r));
      if (refsToLoad.length > 0) {
        this.loadAssets(refsToLoad);
      }
    }

    // dein Resize-Observer bleibt
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
        const existing = new Map((this.loadedAssets ?? []).map((a: any) => [a.id, a]));
        for (const a of assets) existing.set(a.id, a);
        this.loadedAssets = Array.from(existing.values());
      })
      .catch((e) => {
        this._error = e.message;
      })
      .finally(() => {
        this._loading = false;
      });
  }

  private _getAttrValue(asset: any, ref?: AttributeRef) {
    const raw = asset?.attributes?.[ref?.name ?? ""];
    return raw?.value !== undefined ? raw.value : raw; // deckt {value: ...} und Direktwerte ab
  }

  //  Daten (Assets) neu laden, wenn sich attributeRefs ändern
  public refreshContent(force: boolean): void {
    this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as ParkingWidgetConfig;
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
    const busRef = cfg.busRefs?.[0];
    const occupiedRef = cfg.occupiedRefs?.[0];

    return html`
      ${when(
        this._loading || this._error,
        // Loading / Error
        () => {
          if (this._loading) {
            return html`<or-loading-indicator></or-loading-indicator>`;
          } else {
            return html`<or-translate id="error-txt" .value="${this._error}"></or-translate>`;
          }
        },
        // Content
        () => {
          const assets = this.loadedAssets ?? [];
          const asset =
            assets.find(
              (a: any) =>
                a?.attributes?.[busRef?.name ?? ""] !== undefined &&
                a?.attributes?.[occupiedRef?.name ?? ""] !== undefined
            ) ?? assets[0];

          const occupiedValBase = this._getAttrValue(asset, occupiedRef);
          const occupiedVal = this.widgetConfig.invertOccupied ? !occupiedValBase : occupiedValBase;

          const busVal = this._getAttrValue(asset, busRef);

          const canShow = !!asset && !!busRef && !!occupiedRef && occupiedValBase !== undefined && busVal !== undefined;

          return canShow
            ? html`
                <div class="parking-card ${occupiedVal ? "occupied" : "free"}" role="status" aria-live="polite">
                  <or-icon class="parking-icon" icon="parking"></or-icon>
                  <div class="status">${occupiedVal ? "Belegt" : "Frei"}</div>
                  <div class="bus">
                    ${occupiedVal
                      ? html`<span class="label">Bus</span>
                          <span class="value ${busVal === "" ? "empty" : ""}">${busVal ?? "—"}</span>`
                      : null}
                  </div>
                </div>
              `
            : html``;
        }
      )}
    `;
  }
}
