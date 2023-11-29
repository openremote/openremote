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
    public selectedIndex: number = -1;
    @property({type: String, attribute: false})
    public onChange?: (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}, update: boolean) => void;


    render() {
        return this.draw()
    }

    protected draw(){
        const uiSchema: any = {
            type: "Control",
            scope: "#"
        };
        let schemaChangeGlobal: any;
        let schemaForecast: any;
        const jsonFormsInput: Ref<OrJSONForms> = createRef();
        if(this.selectedIndex == -1){
            this.selectedIndex = this.anomalyDetectionConfigObject!.methods!.length > 0 ? 0:-1;
        }
        schemaChangeGlobal = JSON.parse("{\n" +
            "  \"type\": \"object\",\n" +
            "  \"title\": \"Global\",\n" +
            "  \"properties\": {\n" +
            "    \"onOff\": {\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"type\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\n" +
            "        \"global\",\n" +
            "        \"change\",\n" +
            "        \"forecast\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"deviation\": {\n" +
            "      \"type\": \"integer\"\n" +
            "    },\n" +
            "    \"minimumDatapoints\": {\n" +
            "      \"type\": \"integer\"\n" +
            "    },\n" +
            "    \"timespan\": {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  }\n" +
            "}");
        schemaForecast = JSON.parse("{\n" +
            "  \"type\": \"object\",\n" +
            "  \"title\": \"Forecast\",\n" +
            "  \"properties\": {\n" +
            "    \"onOff\": {\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"type\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\n" +
            "        \"global\",\n" +
            "        \"change\",\n" +
            "        \"forecast\"\n" +
            "      ],\n" +
            "      \"default\":\"forecast\"\n" +
            "    },\n" +
            "    \"deviation\": {\n" +
            "      \"type\": \"integer\"\n" +
            "    }\n" +
            "  }\n" +
            "}")

        if(!this.anomalyDetectionConfigObject || !this.attributeRef)return html``;
        const attributeRef = this.attributeRef;

        const onChanged = (dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) => {
            let valid = true
            const newConfig: AnomalyDetectionConfigurationUnion = dataAndErrors.data
            if (newConfig) {

                if ( !newConfig.type || !newConfig.deviation || newConfig.onOff == undefined) valid = false;
                if(newConfig.type === "global"){
                   if(!/^P(\d+Y)?(\d+M)?(\d+W)?(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?$/.test((newConfig as AnomalyDetectionConfigurationGlobal).timespan as string))valid = false
                }
                if (!Util.objectsEqual(newConfig, this.anomalyDetectionConfigObject!.methods![this.selectedIndex]) && valid) {
                    this.anomalyDetectionConfigObject!.methods![this.selectedIndex] = newConfig;
                    if (jsonFormsInput.value) {
                        jsonFormsInput.value.data = newConfig;
                    }
                    if (this.onChange && valid) {
                        this.onChange({data: this.anomalyDetectionConfigObject, errors: []},true);
                    }
                }
            }
            this.updateBool = !this.updateBool;
            if(valid)this.draw();

        }
        const doLoad = async (con: AnomalyDetectionConfigObject) => {
            if (jsonFormsInput.value) {
                if(con.methods![this.selectedIndex]){
                    jsonFormsInput.value.data = con.methods![this.selectedIndex];
                }
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
                                <div .style="z-index: 10; margin:0 2pt; border-top-right-radius:4pt; border-top-left-radius:4pt; border-color:lightgray; border-style:solid; border-width:thin; ${i === this.selectedIndex ? "border-bottom-color: white;": "border-bottom-style: none;"}">
                                    <or-mwc-input type="button" .label="${m.name}" @or-mwc-input-changed="${() =>{ this.selectedIndex = i; this.draw(); this.updateBool = !this.updateBool}}" ></or-mwc-input>
                                </div>
                            `
        })}
                                <or-mwc-input type="button" icon="plus" @or-mwc-input-changed="${() =>{this.addMethod(); this.draw();}}" ></or-mwc-input>
                            </div >
                            <div style="margin: 16pt; padding: 16pt; padding-top: 0; margin-top: -1px; border-style: solid; border-color: lightgray; border-width: thin; border-radius: 4pt;">
                                <div style="display: flex;  justify-content: space-between; width: 100%">
                                    ${html`<or-mwc-input type="text" .value="${this.anomalyDetectionConfigObject.methods![this.selectedIndex].name}" @or-mwc-input-changed="${() =>{this.updateName("test");}}"></or-mwc-input>`}
                                    <p>${this.selectedIndex==-1 ? "No methods Created" : this.anomalyDetectionConfigObject.methods![this.selectedIndex].type}</p>
                                    <or-mwc-input .style="visibility: ${this.selectedIndex == -1? 'hidden':'visible'};" type="button" icon="delete" @or-mwc-input-changed="${() =>{ this.removeMethod(this.selectedIndex); this.draw();}}" ></or-mwc-input>
                                </div>
                                <div .style="visibility: ${this.selectedIndex == -1? 'hidden':'visible'};" class="test" slot="content" >
                                    <or-json-forms  .renderers="${StandardRenderers}" ${ref(jsonFormsInput)}
                                        .disabled="${false}" .readonly="${false}" .label="Config"
                                        .schema="${this.anomalyDetectionConfigObject.methods![this.selectedIndex].type === "forecast"? schemaForecast : schemaChangeGlobal}" label="Anomaly Detection Json forms" .uischema="${uiSchema}"
                                        .onChange="${onChanged}" .props="test" .minimal="${true}"></or-json-forms>
                                    <div>
                                        <or-mwc-input type="switch" .value></or-mwc-input>
                                    </div>
                                    <or-anomaly-config-chart style="display: flex; width: auto;"
                                        .timePresetKey="${this.updateBool}" .panelName="${this.selectedIndex}" .anomalyConfig="${this.anomalyDetectionConfigObject ? this.anomalyDetectionConfigObject.methods![this.selectedIndex] : undefined}" .attributeRef="${attributeRef}" >
                                    </or-anomaly-config-chart>
                                </div>
                            </div>
                        </div>
                    </or-collapsible-panel>
                </div>
                `
    }
    protected addMethod() {
        if(this.anomalyDetectionConfigObject){
            const obj = this.anomalyDetectionConfigObject;

            const i = this.anomalyDetectionConfigObject.methods ? this.anomalyDetectionConfigObject.methods.length: 0
            let con : AnomalyDetectionConfigurationGlobal;
            con = {type:"global", onOff:false, deviation:20, minimumDatapoints:2, timespan:"PT20M"  }
            this.anomalyDetectionConfigObject.methods![i] = con;
            this.selectedIndex = i;
            this.draw();
        }
    }
    protected removeMethod(index:number) {
        if(this.anomalyDetectionConfigObject){
            this.anomalyDetectionConfigObject.methods?.splice(index,1);
            this.anomalyDetectionConfigObject.methods!.length > 0 ? this.selectedIndex = 0 : this.selectedIndex= -1;
            this.draw();
        }
    }
    protected updateName(e:any){
        if(this.anomalyDetectionConfigObject && this.anomalyDetectionConfigObject.methods){
            this.anomalyDetectionConfigObject.methods[this.selectedIndex].name = e.toString()
            this.draw();
        }
    }

}
