import { css, html, PropertyValues, TemplateResult } from "lit";
import { customElement, state, query } from "lit/decorators.js";
import { WidgetManifest } from "../util/or-widget";
import { OrAssetWidget } from "../util/or-asset-widget";
import { AssetWidgetConfig } from "../util/widget-config";
import { SliderSettings } from "../settings/slider-settings";
import { WidgetSettings } from "../util/widget-settings";
import { AttributeRef } from "@openremote/model";
import { throttle } from "lodash";
import { when } from "lit/directives/when.js";

const styling = css`
  :host {
    display: block;
    border-radius: 16px;
    overflow: clip;
    position: relative;
    overflow: hidden;
    height: 100%;
  }
  #widget-wrapper {
    padding: 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 10px;
    border-radius: 16px;
    background: var(--or-surface, #fff);
  }

  .display {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 6px;
    font-weight: 700;
    letter-spacing: 0.2px;
    font-size: clamp(24px, 6.5vw, 32px);
  }
  .display .unit {
    font-weight: 700;
  }

  .slider-block {
    margin-top: 6px;
  }

  /* Native <input type="range"> Styling (WebKit + Firefox) */
  .range {
    width: 100%;
    height: 24px;
    background: transparent;
  }
  .range,
  .range::-webkit-slider-runnable-track {
    -webkit-appearance: none;
  }
  .range:focus {
    outline: none;
  }

  /* Track */
  .range::-webkit-slider-runnable-track {
    height: 4px;
    border-radius: 2px;
    background: rgba(0, 0, 0, 0.15);
  }
  .range::-moz-range-track {
    height: 4px;
    border-radius: 2px;
    background: rgba(0, 0, 0, 0.15);
  }

  /* Thumb */
  .range::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #111;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
    margin-top: -7px; /* centers thumb on 4px track */
    cursor: pointer;
  }
  .range::-moz-range-thumb {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #111;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
    cursor: pointer;
    border: none;
  }

  .range-scale {
    display: flex;
    justify-content: space-between;
    margin-top: 6px;
    font-size: var(--or-font-size-s, 12px);
    opacity: 0.7;
  }

  .controls {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-top: 10px;
  }
  .btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 10px 0;
    border-radius: 12px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    background: #fff;
    box-shadow: var(--or-shadow-s, 0 1px 3px rgba(0, 0, 0, 0.1));
    font-size: 15px;
    line-height: 1;
    cursor: pointer;
    user-select: none;
  }
  .btn:active {
    transform: translateY(1px);
  }

  .btn.primary {
    background: var(--or-primary, #111);
    color: #fff;
    border-color: transparent;
  }
  .btn[disabled] {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .submit {
    grid-column: 1 / -1;
  }
`;

export interface SliderWidgetConfig extends AssetWidgetConfig {
  attributeRefs: AttributeRef[];
  sliderValue: number;

  // Anzeige / Verhalten
  min?: number;
  max?: number;
  step?: number;
  unit?: string; // z. B. "°C" oder "%"
  decimals?: number; // Nachkommastellen für Soll/Ist
  accentColor: string;
}

function getDefaultWidgetConfig(): SliderWidgetConfig {
  return {
    attributeRefs: [],
    sliderValue: 21.5,
    min: 5,
    max: 28,
    step: 0.5,
    unit: "°C",
    decimals: 1,
    accentColor: "inherit",
  };
}

@customElement("slider-widget")
export class SliderWidget extends OrAssetWidget {
  protected widgetConfig!: SliderWidgetConfig;

  @state() protected _loading = false;
  @query("#widget-wrapper") private _wrapper?: HTMLElement;
  private _resizeObserver?: ResizeObserver;

  static getManifest(): WidgetManifest {
    return {
      displayName: "Slider",
      displayIcon: "tune-variant",
      minColumnWidth: 2,
      minColumnHeight: 2,
      getContentHtml: (config: SliderWidgetConfig): OrAssetWidget => new SliderWidget(config),
      getSettingsHtml: (config: SliderWidgetConfig): WidgetSettings => new SliderSettings(config),
      getDefaultConfig: (): SliderWidgetConfig => getDefaultWidgetConfig(),
    };
  }

  static get styles() {
    return [...super.styles, styling];
  }

  protected willUpdate(changed: PropertyValues) {
    const attributeRefs = this.widgetConfig.attributeRefs;
    if (attributeRefs.length === 0) {
      this._error = "noAttributesConnected";
    } else if (!this.isAttributeRefLoaded(attributeRefs[0])) {
      this.loadAssets(attributeRefs);
    }
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

  public refreshContent(_force: boolean): void {
    this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as SliderWidgetConfig;
  }

  disconnectedCallback(): void {
    this._resizeObserver?.disconnect();
    super.disconnectedCallback();
  }

  private _fmt(n: number): string {
    const decimals = this.widgetConfig.decimals ?? 0;
    return new Intl.NumberFormat(navigator.language || "de-DE", {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(n);
  }

  protected render(): TemplateResult {
    const cfg = this.widgetConfig;
    const min = cfg.min ?? 0;
    const max = cfg.max ?? 100;
    const step = cfg.step ?? 1;
    const value = cfg.sliderValue;

    return html`
      <div id="widget-wrapper">
        ${when(
          this._loading || this._error,
          () =>
            this._loading
              ? html`<or-loading-indicator></or-loading-indicator>`
              : html`<or-translate id="error-txt" .value="${this._error}"></or-translate>`,
          () => html`
            <!-- Top: Sollwert -->
            <div class="display">
              <or-icon icon="${"thermometer"}"></or-icon>
              <span class="value">${this._fmt(value)}</span>
              <span class="unit">${cfg.unit ?? ""}</span>
            </div>

            <!-- Slider + Skala -->
            <div class="slider-block">
              <input
                class="range"
                type="range"
                aria-label="Sollwert"
                .value=${String(value)}
                .min=${String(min)}
                .max=${String(max)}
                .step=${String(step)}
                @input=${this.onSliderInput}
              />
              <div class="range-scale">
                <span>${this._fmt(min)}${cfg.unit ?? ""}</span>
                <span>${this._fmt(max)}${cfg.unit ?? ""}</span>
              </div>
            </div>

            <!-- Buttons -->
            <div class="controls">
              <button class="btn" @click=${this.onDeduct}>−</button>
              <button class="btn" @click=${this.onAdd}>+</button>
              <button class="btn primary submit" @click=${this.onSubmit}>Submit</button>
            </div>
          `
        )}
      </div>
    `;
  }

  protected onSliderInput = (ev: Event) => {
    const el = ev.target as HTMLInputElement;
    const next = Number(el.value);
    if (Number.isFinite(next)) {
      this.widgetConfig = { ...this.widgetConfig, sliderValue: next };
    }
  };

  protected onAdd = () => {
    const { sliderValue, step = 1, max = 100 } = this.widgetConfig;
    const next = Math.min(max, (sliderValue ?? 0) + step);
    this.widgetConfig = { ...this.widgetConfig, sliderValue: Number(next.toFixed(6)) };
  };

  protected onDeduct = () => {
    const { sliderValue, step = 1, min = 0 } = this.widgetConfig;
    const next = Math.max(min, (sliderValue ?? 0) - step);
    this.widgetConfig = { ...this.widgetConfig, sliderValue: Number(next.toFixed(6)) };
  };

  protected onSubmit() {}
}
