import "@openremote/or-chart"
import {ChartViewConfig, OrChart, OrChartEvent} from "@openremote/or-chart"
import {
    Chart,
    TimeUnit,
    ChartDataset,
    ChartConfiguration,
    ScatterDataPoint,
    TimeScaleOptions
} from "chart.js";
import "@openremote/or-mwc-components/or-mwc-input";
import { customElement, property } from "lit/decorators.js";
import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {Console, DefaultColor4, manager, Util} from "@openremote/core";
import moment from "moment";
import {throttle} from "lodash";
import {AnnotationOptions} from "chartjs-plugin-annotation";
import {
    AnomalyDetectionConfigObject, AnomalyDetectionConfiguration, Asset,
    AssetDatapointQueryUnion,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef,
    DatapointInterval, ValueDatapoint
} from "@openremote/model";
import {i18next, translate} from "@openremote/or-translate";
import "@openremote/or-components/or-collapsible-panel"
import {GenericAxiosResponse} from "@openremote/rest";



@customElement("or-anomaly-config-input")
export class OrAnomalyConfigChart extends translate(i18next)(LitElement) {

    @property({type: Object})
    public anomalyDetectionConfigObject?: AnomalyDetectionConfigObject = undefined;
    @property({type: Object})
    public attributeRef?: AttributeRef = undefined;
    @property({type: Object})
    public datapointQuery!: AssetDatapointQueryUnion;
    @property({type: Number})
    public timespan?: Number = undefined;


    render() {

        console.log("test")
        if(!this.anomalyDetectionConfigObject || !this.attributeRef)return html``;
        const attributeRef = this.attributeRef;
        const newconfig = this.anomalyDetectionConfigObject;
        let i = 0;
        let index = i;

        return html`
                    <div>
                        <div style= "display:flex; width: 100%; height: 50%">
                            <or-anomaly-config-chart style="display: flex" .attributeRef="${attributeRef}" .panelName="${i}" .timePresetKey="${i}" .anomalyConfig="${newconfig.methods![i]}"></or-anomaly-config-chart>
                        </div>
                        <div style="display: flex; padding-left: 16pt">
                        ${newconfig.methods?.map((m) => {
            const i = newconfig.methods?.indexOf(newconfig.methods?.find(x => x === m)!)!;
            return html`
                                <div .style="z-index: 10; ${i === index ? "border-style:outset;border-bottom-style: solid;border-bottom-color: white;": ""}">
                                    <or-mwc-input type="button" .label="${m.type}" @or-mwc-input-changed="${() => 7}" ></or-mwc-input>
                                </div>
                            `
        })}
                        </div styl>
                            <div style="padding-left: 16pt">
                                ${newconfig.methods?.map((m) => {
            const i = newconfig.methods?.indexOf(newconfig.methods?.find(x => x === m)!)!;
            if(newconfig.methods){
                return html`
                                        <div class="columnDiv" .style="visibility: ${i === index ? "block": "hidden"}; position: absolute; width: 95%; z-index:1; margin-top:-2pt; padding-left:2pt; border-style: outset;">
                                            <or-mwc-input type="number" label="deviation" .value=${newconfig.methods[i].deviation} style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="number" label="minimumDatapoints" .value="${newconfig.methods[i].minimumDatapoints}" style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="text" label="timespan" .value="${newconfig.methods[i].timespan}" style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="button" label="Test"  @or-mwc-input-changed="${() => 7}" style="padding:10px 10px 16px 0;"></or-mwc-input>
                                        </div>
                                        `
            }
        })}
                            </div>
                    </div>
                `
    }
}
