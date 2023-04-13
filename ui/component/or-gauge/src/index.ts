import {css, html, LitElement} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {Asset, AssetModelUtil, Attribute, AttributeRef} from "@openremote/model";
import { Gauge, GaugeOptions } from "gaugeJS";
import manager, { Util } from "@openremote/core";
import {i18next} from "@openremote/or-translate";
import {debounce} from "lodash";
import { getAssetDescriptorIconTemplate } from "@openremote/or-icon";

//language=css
const styling = css`
    :host {
        display: flex;
        align-items: center;
        flex-direction: column;
    }

    #chart-wrapper {
        position: relative;
        width: 100%;
        height: 100%;
        flex-direction: column;
        justify-content: center;
        align-items: center;
    }
    #chart-container {
        display: flex;
        justify-content: center;
        align-items: center;
    }

    #chart {
        width: 100%;
    }

    .chart-description {
        margin: 0 20px;
        font-size: 16px;
        z-index: 3;
    }
    .mainvalue-wrapper {
        width: 100%;
        display: flex;
        flex: 0 0 60px;
        align-items: center;
        justify-content: center;
    }
    .main-number {
        color: var(--internal-or-asset-viewer-title-text-color);
        font-size: 42px;
    }

    .main-number-icon {
        font-size: 24px;
        margin-right: 10px;
        display: flex;
    }

    .main-number-unit {
        font-size: 42px;
        color: var(--internal-or-asset-viewer-title-text-color);
        font-weight: 200;
        margin-left: 5px;
    }

    .main-number.xs, .main-number-unit.xs {
        font-size: 18px;
    }
    .main-number.s, .main-number-unit.s {
        font-size: 24px;
    }
    .main-number.m, .main-number-unit.m {
        font-size: 30px;
    }
    .main-number.l, .main-number-unit.l {
        font-size: 36px;
    }
    .main-number.xl, .main-number-unit.xl {
        font-size: 42px;
    }
    .main-number.unknown, .main-number-unit.unknown {
        font-size: unset;
    }
`
export interface OrGaugeConfig {
    attributeRef?: AttributeRef;
    thresholds?: [number, string][]
    options?: GaugeOptions;
}

@customElement("or-gauge")
export class OrGauge extends LitElement {

    static get styles() {
        return [styling]
    }

    @property({type: Object})
    public attrRef?: AttributeRef;

    @property({type: Object})
    public asset?: Asset;

    @property({type: Object})
    public assetAttribute?: [number, Attribute<any>];

    @property()
    public value?: number;

    @property()
    public decimals: number = 0;

    @property()
    public unit?: string;

    @property()
    public min?: number;

    @property()
    public max?: number;

    @property()
    public thresholds?: [number, string][];

    @property()
    public readonly config?: OrGaugeConfig;

    @property({type: String})
    public realm?: string;


    @state()
    protected loading: boolean = false;

    @state()
    private gauge?: Gauge;

    @state()
    private gaugeSize?: { width: number, height: number }

    @query("#chart")
    private _gaugeElem!: HTMLCanvasElement;

    @query("#chart-wrapper")
    private _wrapperElem!: HTMLElement;

    @query("#details-container")
    private _detailsElem!: HTMLElement;

    private resizeObserver?: ResizeObserver;


    constructor() {
        super();
        if(!this.config) {
            this.config = this.getDefaultConfig();
        }
        // Register observer when gauge size changes
        this.updateComplete.then(() => {
            this.resizeObserver = new ResizeObserver(debounce((entries: ResizeObserverEntry[]) => {
                const size = entries[0].contentRect;
                this.gaugeSize = {
                    width: size.width,
                    height: size.height
                }
                this.updateComplete.then(() => {
                    this.setupGauge(); // recreate gauge since the library is not 100% responsive.
                });
            }, 200))
            this.resizeObserver.observe(this._wrapperElem);
        })
    }

    // Processing changes before update, to prevent extra render
    willUpdate(changedProps: Map<string, any>) {
        if(changedProps.has('assetAttribute')) {
            const attr = this.assetAttribute![1];
            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attr.name!, this.asset!.type!);
            this.unit = Util.resolveUnits(Util.getAttributeUnits(attr, attributeDescriptor, this.asset!.type));
            this.value = attr.value != null ? attr.value : NaN;
        }
    }

    // After render took place...
    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has('value')) {
            this.gauge?.set(this.value != null ? this.value : NaN);
        }
        if(changedProperties.has('attrRef')) {
            if(this.attrRef) {
                this.loadData(this.attrRef);
            } else {
                this.assetAttribute = undefined;
                this.value = undefined;
            }
        }

        // Render gauge again if..
        if(changedProperties.has('min') && this.min != null && this.gauge) {
            this.gauge.setMinValue(this.min);
            this.gauge.set(this.value != null ? this.value : NaN);
        }
        if(changedProperties.has('max') && this.max != null && this.gauge) {
            this.gauge.maxValue = this.max;
            this.gauge.set(this.value != null ? this.value : NaN);
        }
        if(changedProperties.has('thresholds') && this.thresholds) {

            // Make staticZones out of the thresholds.
            // If below the minimum or above the maximum, set the value according to it.
            this.config!.options!.staticZones = [];
            this.thresholds.sort((x, y) => (x[0] < y[0]) ? -1 : 1).forEach(((threshold, index) => {
                const min = threshold[0];
                const max = (this.thresholds![index + 1] ? this.thresholds![index + 1][0] : this.max);
                const zone = {
                    strokeStyle: threshold[1],
                    min: ((this.min && min && this.min > min) ? this.min : ((this.max && min && this.max < min) ? this.max : min)),
                    max: ((this.max && max && this.max < max) ? this.max : ((this.min && max && this.min > max) ? this.min : max))
                };
                this.config?.options?.staticZones?.push(zone as any);
            }));

            // The lowest staticZone should ALWAYS have the minimum value, to prevent the graphic displaying incorrectly.
            if(this.min && this.config?.options?.staticZones) {
                this.config.options.staticZones[0].min = this.min;
            }

            // Applying the options we changed.
            if(this.gauge) {
                this.gauge.setOptions(this.config?.options);
            }
        }
    }

    setupGauge() {
        this.gauge = new Gauge(this._gaugeElem);
        this.gauge.setOptions(this.config?.options);
        this.gauge.maxValue = (this.max ? this.max : 100)
        this.gauge.setMinValue(this.min ? this.min : 0);
        this.gauge.animationSpeed = 1;
        this.gauge.set(this.value != null ? this.value : NaN);
        if(this.value == null && this.attrRef) {
            this.loadData(this.attrRef);
        }
    }

    getGaugeWidth(gaugeSize?: { width: number, height: number }, includeLabelHeight: boolean = true): string {
        if(!gaugeSize) {
            return "unset"
        }
        const width = gaugeSize.width;
        const height = (includeLabelHeight ? (gaugeSize.height - this._detailsElem.clientHeight) : gaugeSize.height) * 1.5;
        return Math.min(width, height) + "px";
    }
    shouldShowLabel(gaugeSize: { width: number, height: number }): boolean {
        return (gaugeSize.width > 70) && (gaugeSize.height > 100);
    }
    getLabelSize(width: number): "s" | "m" | "l" | "xl" {
        if(width < 120) {
            return "s";
        } else if(width < 240) {
            return "m";
        } else if(width < 320) {
            return "l"
        } else {
            return "xl"
        }
    }

    render() {
        const formattedVal = (this.value != null ? +this.value.toFixed(this.decimals) : NaN); // + operator prevents str return

        // Set width/height values based on size
        const showLabel = this.gaugeSize ? this.shouldShowLabel(this.gaugeSize) : true;
        const labelSize = showLabel && this.gaugeSize ? this.getLabelSize(this.gaugeSize.width) : "unknown"
        const gaugeWidth = this.getGaugeWidth(this.gaugeSize, showLabel);

        return html`
            <div style="position: relative; height: 100%; width: 100%;">
                <div id="chart-wrapper" style="display: ${this.loading ? 'none' : 'flex'};">
                    <div id="chart-container" style="flex: 0 0 0; width: ${gaugeWidth};">
                        <canvas id="chart"></canvas>
                    </div>
                    <div id="details-container">
                        ${showLabel ? html`
                            <div class="mainvalue-wrapper">
                                <span class="main-number-icon">${this.asset ? getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.asset.type)) : ""}</span>
                                <span class="main-number ${labelSize}">${formattedVal}</span>
                                <span class="main-number-unit ${labelSize}">${this.unit ? this.unit : ""}</span>
                            </div>
                        ` : undefined}
                    </div>
                </div>
            </div>
            ${this.loading ? html`
                <span>${i18next.t('loading')}</span>
            ` : undefined}
        `
    }

    async loadData(attrRef: AttributeRef) {
        const response = await manager.rest.api.AssetResource.queryAssets({ ids: [attrRef.id!] });
        const assets = response.data;
        const assetAttributes = [attrRef].map((attrRef) => {
            const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
            const asset = assetIndex >= 0 ? assets[assetIndex] : undefined;
            return asset && asset.attributes ? [assetIndex, asset.attributes[attrRef.name!]] : undefined;
        }).filter((indexAndAttr) => !!indexAndAttr) as [number, Attribute<any>][];

        this.asset = assets[0];
        this.assetAttribute = assetAttributes[0];
    }

    getDefaultConfig(): OrGaugeConfig {
        return {
            attributeRef: undefined,
            options: {
                angle: 0,
                lineWidth: 0.4,
                radiusScale: 1,
                pointer: {
                    length: 0.5,
                    strokeWidth: 0.035,
                    color: "#000000",
                },
                staticZones: [],
                limitMax: true,
                limitMin: true,
                colorStart: "#000000",
                colorStop: "#707070",
                strokeColor: "#ABCDEF",
                generateGradient: false,
                highDpiSupport: true
            }
        }
    }
}
