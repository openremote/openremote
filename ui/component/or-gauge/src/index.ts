import {css, html, LitElement} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {Attribute, AttributeRef} from "@openremote/model";
import { Gauge, GaugeOptions } from 'gaugejs';
import manager from "@openremote/core";
import {i18next} from "@openremote/or-translate";

//language=css
const styling = css`
    :host {
        display: flex;
        align-items: center;
        flex-direction: column;
    }

    .chart-wrapper {
        position: relative;
        flex: 1;
        width: 100%;
    }

    #chart {
        width: 100%;
    }

    k2-chart-value {
        position: absolute;
        background-color: white;
        bottom: -10px;
        left: 0;
        right: 0;
        font-size: 32px;
        font-weight: bold;
        text-align: center;
        border-radius: 50%;
        margin: auto;
        height: 90px;
        width: 90px;
        line-height: 90px;
    }

    .chart-value.green {
        color: var(--k2-green-color);
    }

    .chart-value.yellow {
        color: var(--k2-yellow-color);
    }

    .chart-value.red {
        color: var(--k2-red-color);
    }

    .chart-description {
        margin: 0 20px;
        font-size: 16px;
        z-index: 3;
    }
`
export interface OrGaugeConfig {
    attributeRef?: AttributeRef;
    thresholds?: OrGaugeThreshold[]
    options?: GaugeOptions;
}
export interface OrGaugeThreshold {
    strokeStyle: number;
    min: number;
    max: number;
}

@customElement("or-gauge")
export class OrGauge extends LitElement {

    static get styles() {
        return [styling]
    }

    @property({type: Object})
    public attrRef?: AttributeRef;

    @property({type: Object})
    public attribute?: Attribute<any>;

    @property()
    public value?: number;

    @property()
    public readonly config?: OrGaugeConfig;

    @property({type: String})
    public realm?: string;


    @state()
    protected loading: boolean = false;

    @state()
    private gauge?: Gauge;

    @query("#chart")
    private _gaugeElem!: HTMLCanvasElement;


    constructor() {
        super();
        console.error("Constructor of or-gauge.");
        if(!this.config) {
            this.config = this.getDefaultConfig();
        }
    }
    firstUpdated(_changedProperties: Map<string, any>) {
        console.error("firstUpdated on or-gauge");
        if(!this.gauge) {
            console.error(this._gaugeElem);
            this.gauge = new Gauge(this._gaugeElem);
            this.gauge.setOptions(this.config?.options);
            this.gauge.maxValue = 100;
            this.gauge.setMinValue(0);
            this.gauge.animationSpeed = 1;
            this.gauge.set(this.value ? this.value : NaN);
            if(!this.value && this.attrRef) {
                this.loadData(this.attrRef);
            }
        }
    }
    updated(changedProperties: Map<string, any>) {
        console.error(changedProperties);
        if(changedProperties.has('value')) {
            this.gauge?.set(this.value ? this.value : NaN);
        }
        if(changedProperties.has('attribute')) {
            this.gauge?.set(this.attribute?.value ? this.attribute.value : NaN)
        }
        const reloadData = changedProperties.has("assetAttributes") || changedProperties.has("realm");
        if(reloadData) {
            console.error("Reloading data..");
            this.loadData(this.attrRef!);
        }
    }

    render() {
        console.error("Rendering or-gauge..");
        return html`
            <div class="chart-wrapper" style="display: ${this.loading ? 'none' : 'initial'}">
                <canvas id="chart"></canvas>
                <span>${this.value}</span>
                <span>${this.attribute?.name} ${this.attribute?.type}</span>
                <span>${this.attribute?.timestamp}</span>
            </div>
            ${this.loading ? html`
                <span>${i18next.t('loading')}</span>
            ` : undefined}
        `
    }

    async loadData(attrRef: AttributeRef) {
        console.error(attrRef);
        const response = await manager.rest.api.AssetResource.queryAssets({ ids: [attrRef.id!] });
        console.error(response);
        const attributes = response.data[0].attributes;
        if(attributes) {
            if(attributes.hasOwnProperty(attrRef.name!)) {
                this.attribute = attributes[attrRef.name!];
                this.value = attributes[attrRef.name!].value;
            }
        }
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
                limitMax: true,
                limitMin: false,
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
            else setTimeout(_ => poll(resolve), 400);
        }
        return new Promise(poll);
    }
}
