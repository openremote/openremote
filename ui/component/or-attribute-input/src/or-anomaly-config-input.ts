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
import moment from "moment";



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
    expanded: boolean = true;
    @property({type: Boolean})
    updateBool: boolean = false;
    @property({type:Object})
    public template!: TemplateResult
    @property({type:Number})
    public selectedIndex: number = 0;
    @property({type: String, attribute: false})
    public onChange?: (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}) => void;


    render() {
        return this.draw()
    }

    protected draw(){
        const uiSchema: any = {
            type: "Control",
            scope: "#"
        };
        let schema: any;
        const jsonFormsInput: Ref<OrJSONForms> = createRef();

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

        const onChanged = (dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) => {
            let valid = true
            const newConfig: AnomalyDetectionConfigurationUnion = dataAndErrors.data
            if (newConfig) {

                if (!newConfig.timespan || !newConfig.type || !newConfig.minimumDatapoints || !newConfig.deviation || newConfig.onOff == undefined) valid = false;
                if(!moment.duration(newConfig.timespan))valid=false;
                if (!Util.objectsEqual(newConfig, this.anomalyDetectionConfigObject!.methods![this.selectedIndex]) && valid) {
                    this.anomalyDetectionConfigObject!.methods![this.selectedIndex] = newConfig;
                    if (jsonFormsInput.value) {
                        jsonFormsInput.value.data = newConfig;
                    }
                }
            }
            if (this.onChange ) {
                this.onChange({data: this.anomalyDetectionConfigObject, errors: []});
            }
            this.updateBool = !this.updateBool;
            if(valid)this.draw();

        }
        const doLoad = async (con: AnomalyDetectionConfigObject) => {
            if (jsonFormsInput.value) {
                jsonFormsInput.value.data = con.methods![this.selectedIndex];
            }
        }
        window.setTimeout(() => doLoad(this.anomalyDetectionConfigObject as AnomalyDetectionConfigObject), 0);

        return html`
            <style>
                    .test {
                        display: flex;
                        flex-direction: column;
                        width: 100%;
                    }
                </style>
                <div class="test">
                    <or-collapsible-panel style="width: 100%">
                        <span slot="header">
                            Anomaly Detection Custom
                        </span>
                        <div class="test" slot="content">
                            <div style="display: flex; padding: 0 16pt; margin-left: 10pt">
                                ${this.anomalyDetectionConfigObject!.methods?.map((m) => {
            const i = this.anomalyDetectionConfigObject!.methods?.indexOf(this.anomalyDetectionConfigObject!.methods?.find(x => x === m)!)!;
            return html`
                                <div .style="z-index: 10; ${i === this.selectedIndex ? "border-style:outset;border-bottom-style: solid;border-bottom-color: white;": ""}">
                                    <or-mwc-input type="button" .label="${m.type}" @or-mwc-input-changed="${() =>{ this.selectedIndex = i; this.draw(); this.updateBool = !this.updateBool}}" ></or-mwc-input>
                                    <or-mwc-input type="button" icon="delete" @or-mwc-input-changed="${() =>{ this.removeMethod(i); this.draw();}}" ></or-mwc-input>
                                </div>
                            `
        })}
                                <or-mwc-input type="button" icon="plus" @or-mwc-input-changed="${() =>{this.addMethod(); this.draw();}}" ></or-mwc-input>
                            </div >
                            <or-collapsible-panel style="margin: 16pt; margin-top: 0;" expanded="${true}">
                                <span slot="header">
                                    ${this.anomalyDetectionConfigObject.methods![this.selectedIndex].type}
                                </span>
                                <div class="test" slot="content">
                                    <or-json-forms style="padding: 0 16pt;"  .renderers="${StandardRenderers}" ${ref(jsonFormsInput)}
                                        .disabled="${false}" .readonly="${false}" .label="Config"
                                        .schema="${schema}" label="Anomaly Detection Json forms" .uischema="${uiSchema}"
                                        .onChange="${onChanged}" .props="test" .minimal="${true}"></or-json-forms>
                                    <or-anomaly-config-chart style="display: flex; padding: 0 16pt; width: auto;"
                                        .timePresetKey="${this.updateBool}" .panelName="${this.selectedIndex}" .anomalyConfig="${this.anomalyDetectionConfigObject ? this.anomalyDetectionConfigObject.methods![this.selectedIndex] : undefined}" .attributeRef="${attributeRef}" >
                                    </or-anomaly-config-chart>
                                </div>
                            </or-collapsible-panel>
                        </div>
                    </or-collapsible-panel>
                </div>
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
            con = {type:"global", onOff:false, deviation:20, minimumDatapoints:2, timespan:"PT20M"  }
            this.anomalyDetectionConfigObject.methods![i] = con;
            this.selectedIndex = i;
            this.requestUpdate();
        }
    }
    protected removeMethod(index:number) {
        if(this.anomalyDetectionConfigObject){
            const obj = this.anomalyDetectionConfigObject;
            this.anomalyDetectionConfigObject.methods?.splice(index,1);
            this.selectedIndex = 0;
            console.log(this.anomalyDetectionConfigObject.methods![index])
            this.requestUpdate();
        }
    }

}
