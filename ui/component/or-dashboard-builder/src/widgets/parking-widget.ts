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
  :host {
    display: block;
    height: 100%;
  }
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  .widget-container {
    width: 100%;
    height: 100%;
    display: flex; /* Card zentrieren */
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
  }

  .parking-card {
    place-items: center;
    gap: 12px;
  } /* zentriert alle Grid-Items */

  .parking-info {
    text-align: center;
    font-weight: 600;
    color: #374151;

    font-size: clamp(14px, calc(18px * var(--k, 1)), 28px);

    /* angenehme Zeilenhöhe + Abstand nach unten */
    line-height: 1.2;
    margin-block-end: clamp(4px, calc(10px * var(--k, 1)), 14px);

    /* bricht lange Namen sauber um */
    hyphens: auto;
    word-break: break-word;
    padding-inline: 4px; /* kleiner Innenabstand links/rechts */
  }

  .icon-box {
    width: var(--icon-size, 56px);
    height: var(--icon-size, 56px);
    border-radius: calc(8px * var(--k, 1));
    background: #1976d2;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 0; /* kein Baseline-Gap */
    margin-block-end: 5px;
  }

  .icon-box .icon {
    display: block;
    width: calc(0.6 * var(--icon-size, 56px));
    height: calc(0.6 * var(--icon-size, 56px));
    /* Für or-icon, das über CSS-Variablen liest */
    --or-icon-width: calc(0.6 * var(--icon-size, 56px));
    --or-icon-height: calc(0.6 * var(--icon-size, 56px));
    --or-icon-fill: #ffffffff; /* Weiß */
  }

  .parking-card .status {
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    min-block-size: clamp(28px, calc(40px * var(--k, 1)), 56px);
    padding-inline: calc(14px * var(--k, 1)); /* nur seitlich */
    border-radius: calc(10px * var(--k, 1));
    font-weight: 700;
    color: #fff;
    text-align: center;
    justify-self: center; /* sicher zentriert */
    /* Breite steuern: min + prozentual, aber mit Obergrenze */
    inline-size: clamp(calc(120px * var(--k, 1)), 60%, calc(260px * var(--k, 1)));
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
    font-size: clamp(14px, calc(18px * var(--k, 1)), 28px);
  }
  .parking-card .bus .label {
    color: #6b7280;
    margin-right: calc(7px * var(--k, 1));
  }
  .parking-card .bus .value.empty {
    opacity: 0.6;
  }

  .attribute-timestamp {
    position: absolute; /* relativ zum äußeren Wrapper */
    inset-inline-start: 8px; /* links (RTL-sicher) */
    inset-block-end: 6px; /* unten */
    font-size: clamp(10px, calc(12px * var(--k, 1)), 13px);
    line-height: 1.1;
    color: #9ca3af; /* dezentes Grau */
    z-index: 1;
    pointer-events: none; /* blockiert keine Handles/Clicks */
    max-width: calc(100% - 16px); /* falls Text doch lang wird */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;

export interface ParkingWidgetConfig extends AssetWidgetConfig {
  attributeRefs: AttributeRef[];
  occupiedRefs: AttributeRef[];
  busRefs: AttributeRef[];
  invertOccupied: boolean;
  showParkingIcon: boolean;
  parkingLabel: string;
  showHelperText: boolean;
}

function getDefaultWidgetConfig(): ParkingWidgetConfig {
  return {
    attributeRefs: [],
    occupiedRefs: [],
    busRefs: [],
    invertOccupied: false,
    showParkingIcon: false,
    parkingLabel: "",
    showHelperText: false,
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

    const BASE = 240; // „Entwurfsbreite“ deiner Card; dient nur zur Normierung

    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;

        // Skalar 0.5…3.0 – je nach Platz
        const k = Math.max(0.5, Math.min(3, Math.min(width, height) / BASE));
        this.style.setProperty("--k", `${k}`);

        // Icon proportional skalieren (mit Min/Max-Klammer)
        const icon = Math.max(32, Math.min(96, 56 * k));
        this.style.setProperty("--icon-size", `${icon}px`);
      }
    });

    // Das Host-Element beobachten reicht, weil es die Kachelgröße erhält
    ro.observe(this);
    this._resizeObserver = ro;
  }

  protected render(): TemplateResult {
    const cfg = this.widgetConfig;
    const busRef = cfg.busRefs?.[0];
    const occupiedRef = cfg.occupiedRefs?.[0];

    return html`
      <div style="position: relative; height: 100%; overflow: hidden;">
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
            const timestamp: any = this.loadedAssets[0].attributes?.[busRef?.name!].timestamp;

            const canShow =
              !!asset && !!busRef && !!occupiedRef && occupiedValBase !== undefined && busVal !== undefined;

            return canShow
              ? html`
                  <div id="widget-wrapper" class="widget-container">
                    <div class="parking-card ${occupiedVal ? "occupied" : "free"}" role="status" aria-live="polite">
                      <div class="parking-info">${cfg.parkingLabel}</div>
                      ${cfg.showParkingIcon
                        ? html`
                            <div class="icon-box" aria-hidden="true">
                              <or-icon class="icon" icon="parking"></or-icon>
                            </div>
                          `
                        : null}

                      <div class="status">${occupiedVal ? "Belegt" : "Frei"}</div>
                      <div class="bus">
                        ${occupiedVal
                          ? html`<span class="label">Bus</span>
                              <span class="value ${busVal === "" ? "empty" : ""}">${busVal ?? "—"}</span>`
                          : null}
                      </div>
                    </div>
                    ${cfg.showHelperText
                      ? html`<div class="attribute-timestamp">${this.formatUpdatedAt(timestamp)}</div>`
                      : null}
                  </div>
                `
              : html``;
          }
        )}
      </div>
    `;
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
