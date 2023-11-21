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
    AnomalyDetectionConfigObject,
    AnomalyDetectionConfiguration,
    AnomalyDetectionConfigurationGlobal,
    AnomalyDetectionConfigurationUnion,
    Asset,
    AssetDatapointQueryUnion,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef,
    DatapointInterval,
    ValueDatapoint
} from "@openremote/model";
import {ErrorObject, OrJSONForms, StandardRenderers} from "@openremote/or-json-forms";
import {i18next, translate} from "@openremote/or-translate";
import "@openremote/or-components/or-collapsible-panel"
import {GenericAxiosResponse} from "@openremote/rest";
import {createRef, Ref, ref} from 'lit/directives/ref.js';



@customElement("or-anomaly-config-input")
export class OrAnomalyConfigChart extends translate(i18next)(LitElement) {

    @property({type: Object})
    public anomalyDetectionConfigObject?: AnomalyDetectionConfigObject = undefined;
    @property({type: Object})
    public attributeRef?: AttributeRef = undefined;
    @property({type: Object})
    public datapointQuery!: AssetDatapointQueryUnion;
    @property({type: Number})
    public timespan?: number = undefined;
    @property({type: Boolean})
    expanded: boolean = false;
    @property({type:Object})
    public template!: TemplateResult
    @property({type:Number})
    public selectedIndex: number = 0;


    render() {
        return this.draw(this.selectedIndex)
    }

    protected draw(index:number){
        const uiSchema: any = {
            type: "Control",
            scope: "#"
        };
        let schema: any;
        const jsonFormsInput: Ref<OrJSONForms> = createRef();
        console.log("draw")

        schema= JSON.parse("{\n" +
            "  \"type\": \"object\",\n" +
            "  \"title\": \"Anomaly Detection Methods\",\n" +
            "  \"required\": [\n" +
            "    \"type\",\n" +
            "    \"deviation\",\n" +
            "    \"minimumDatapoints\",\n" +
            "    \"timespan\",\n" +
            "    \"onOff\"\n" +
            "  ],\n" +
            "  \"properties\": {\n" +
            "    \"onOff\": {\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"type\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\n" +
            "        \"global\",\n" +
            "        \"change\",\n" +
            "        \"timespan\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"deviation\": {\n" +
            "      \"title\": \"Deviation (0-200)\",\n" +
            "      \"type\": \"integer\"\n" +
            "    },\n" +
            "    \"minimumDatapoints\": {\n" +
            "      \"type\": \"integer\"\n" +
            "    },\n" +
            "    \"timespan\": {\n" +
            "      \"title\": \"Minimum Timespan (in hours)\",\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  }\n" +
            "}");

        if(!this.anomalyDetectionConfigObject || !this.attributeRef)return html``;
        const attributeRef = this.attributeRef;
        const forms = jsonFormsInput.value;

        if(jsonFormsInput.value){
            jsonFormsInput.value!.schema = schema;
            jsonFormsInput.value!.data =  this.anomalyDetectionConfigObject.methods![this.selectedIndex] as AnomalyDetectionConfigurationGlobal;
            console.log(jsonFormsInput.value!.data)
        }
        const onChanged = (dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) => {
            let valid = true
            const newConfig: AnomalyDetectionConfigurationGlobal = dataAndErrors.data
            if (newConfig) {

                if (!newConfig.timespan || !newConfig.type || !newConfig.minimumDatapoints || !newConfig.deviation || newConfig.onOff == undefined) valid = false;
                if (!Util.objectsEqual(newConfig, this.anomalyDetectionConfigObject!.methods![this.selectedIndex]) && valid) {
                    this.anomalyDetectionConfigObject!.methods![this.selectedIndex] = newConfig;
                    if (jsonFormsInput.value) {
                        jsonFormsInput.value.data = newConfig
                    }
                }
            }
            console.log(jsonFormsInput.value!.data)
        }
        return html`
            <or-collapsible-panel style="width: 100%" expanded="${this.expanded}">
                <span slot="header">
                    Anomaly Detection Custom
                </span>
                    <div slot="content">
                        
                        <div style="display: flex; padding-left: 16pt">
                        ${this.anomalyDetectionConfigObject!.methods?.map((m) => {
            const i = this.anomalyDetectionConfigObject!.methods?.indexOf(this.anomalyDetectionConfigObject!.methods?.find(x => x === m)!)!;
            return html`
                                <div .style="z-index: 10; ${i === index ? "border-style:outset;border-bottom-style: solid;border-bottom-color: white;": ""}">
                                    <or-mwc-input type="button" .label="${m.type}" @or-mwc-input-changed="${() =>{ this.selectedIndex = i; this.requestUpdate()}}" ></or-mwc-input>
                                </div>
                            `
        })}
                            <or-mwc-input type="button" icon="plus" @or-mwc-input-changed="${() =>{this.addMethod()}}" ></or-mwc-input>
                        </div >
                        <div style="padding-left: 16pt">
                                ${this.anomalyDetectionConfigObject!.methods?.map((m) => {
            const i = this.anomalyDetectionConfigObject!.methods?.indexOf(this.anomalyDetectionConfigObject!.methods?.find(x => x === m)!)!;
            if(this.anomalyDetectionConfigObject && this.anomalyDetectionConfigObject.methods && i === index){
                return html`
                    <or-json-forms .renderers="${StandardRenderers}" ${ref(jsonFormsInput)}
                            .disabled="${false}" .readonly="${false}" .label="Config"
                            .schema="${schema}" label="Anomaly Detection Json forms" .uischema="${uiSchema}"
                                   .onChange="${onChanged}" ></or-json-forms>
                                        <div class="columnDiv" style="visibility: visible; width: 95%; z-index:1; margin-top:-2pt; padding-left:2pt; border-style: outset;">
                                            <or-mwc-input type="number" label="deviation" @or-mwc-input-changed="${(e : CustomEvent) => this._onAttributeModified(i,e,this.anomalyDetectionConfigObject!.methods![i].deviation)}" .value=${this.anomalyDetectionConfigObject.methods[i].deviation} style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="number" label="minimumDatapoints" @or-mwc-input-changed="${(e : Event) =>{ this.selectedIndex = i; this.requestUpdate()}}" .value="${this.anomalyDetectionConfigObject.methods[i].minimumDatapoints}" style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="text" label="timespan" @or-mwc-input-changed="${(e : Event) =>{ this.selectedIndex = i; this.requestUpdate()}}" .value="${this.anomalyDetectionConfigObject.methods[i].timespan}" style="padding: 10px 10px 16px 0;"></or-mwc-input>
                                            <or-mwc-input type="button" label="Test"  @or-mwc-input-changed="${() =>{ this.selectedIndex = i; this.requestUpdate()}}" style="padding:10px 10px 16px 0;"></or-mwc-input>
                                        </div>
                                        `
            }
        })}
                        </div>
                        <div style= "display:flex; width: 100%; height: 50%">
                            <or-anomaly-config-chart style="display: flex" .attributeRef="${attributeRef}" .panelName="${index}" .timePresetKey="${index}" .anomalyConfig="${this.anomalyDetectionConfigObject!.methods![index]}"></or-anomaly-config-chart>
                        </div>
                    </div>
                </or-collapsible-panel>
                `
    }

    protected _onAttributeModified(i: number, newValue: any, obj:any) {
        if(this.anomalyDetectionConfigObject && this.anomalyDetectionConfigObject.methods){
            obj = newValue
        }
        this.requestUpdate();

    }
    protected addMethod() {
        if(this.anomalyDetectionConfigObject){
            const obj = this.anomalyDetectionConfigObject;

            const i = this.anomalyDetectionConfigObject.methods ? this.anomalyDetectionConfigObject.methods.length: 0
            let con : AnomalyDetectionConfigurationGlobal;
            con = {type:"global", onOff:false, deviation:20, minimumDatapoints:1, timespan:"PT20M"  }
            this.anomalyDetectionConfigObject.methods![i] = con;

            console.log(this.anomalyDetectionConfigObject.methods![i])
            this.requestUpdate();
        }


    }

}
