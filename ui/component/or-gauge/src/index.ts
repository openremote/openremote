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

    .chart-wrapper {
        position: relative;
        /*flex: 1;*/
        width: 100%;
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

    @property()
    private mainValueSize: "xs" | "s" | "m" | "l" | "xl" = "m";


    @state()
    protected loading: boolean = false;

    @state()
    private gauge?: Gauge;

    @query("#chart")
    private _gaugeElem!: HTMLCanvasElement;

    private resizeObserver?: ResizeObserver;


    constructor() {
        super();
        if(!this.config) {
            this.config = this.getDefaultConfig();
        }
        this.updateComplete.then(() => {
            this.resizeObserver = new ResizeObserver(debounce((entries: ResizeObserverEntry[]) => {
                this.setupGauge(); // recreate gauge since the library is not 100% responsive.
                const gaugeSize = entries[0].devicePixelContentBoxSize[0].blockSize; // since gauge elem width == value elem width
                this.setLabelSize(gaugeSize);
            }, 200))
            this.resizeObserver.observe(this.shadowRoot!.getElementById('chart')!);
        })
    }

    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has('value')) {
            this.gauge?.set(this.value ? this.value : NaN);
        }
        if(changedProperties.has('assetAttribute')) {
            const attr = this.assetAttribute![1];
            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attr.name!, this.asset!.type!);
            this.unit = Util.resolveUnits(Util.getAttributeUnits(attr, attributeDescriptor, this.asset!.type));
            this.value = attr.value ? attr.value : NaN;
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
        if(changedProperties.has('min') && this.min) {
            this.gauge?.setMinValue(this.min);
            this.gauge?.set(this.value ? this.value : NaN);
        }
        if(changedProperties.has('max') && this.max && this.gauge) {
            this.gauge.maxValue = this.max;
            this.gauge?.set(this.value ? this.value : NaN);
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
        this.gauge.set(this.value ? this.value : NaN);
        if(!this.value && this.attrRef) {
            this.loadData(this.attrRef);
        }
    }

    setLabelSize(blockSize: number) {
        if(blockSize < 60) {
            this.mainValueSize = "s";
        } else if(blockSize < 100) {
            this.mainValueSize = "m";
        } else if(blockSize < 200) {
            this.mainValueSize = "l"
        } else {
            this.mainValueSize = "xl"
        }
    }

    render() {
        const formattedVal = (this.value ? +this.value.toFixed(this.decimals) : NaN); // + operator prevents str return
        return html`
            <div style="position: relative; height: 100%; width: 100%;">
                <div class="chart-wrapper" style="display: ${this.loading ? 'none' : 'flex'}; flex-direction: column; justify-content: center; height: 100%;">
                    <div style="flex: 0 0 0;">
                        <canvas id="chart"></canvas>
                    </div>
                    <div>
                        <div class="mainvalue-wrapper">
                            <span class="main-number-icon">${this.asset ? getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.asset.type)) : ""}</span>
                            <span class="main-number ${this.mainValueSize}">${formattedVal}</span>
                            <span class="main-number-unit ${this.mainValueSize}">${this.unit ? this.unit : ""}</span>
                        </div>
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

    // Wait until function that waits until a boolean returns differently
    waitUntil(conditionFunction: any) {
        const poll = (resolve: any) => {
            if(conditionFunction()) resolve();
            else setTimeout((_: any) => poll(resolve), 400);
        }
        return new Promise(poll);
    }
}
