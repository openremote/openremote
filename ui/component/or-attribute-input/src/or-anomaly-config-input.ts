import "@openremote/or-chart"
import "@openremote/or-mwc-components/or-mwc-input";
import {OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input"
import { customElement, property } from "lit/decorators.js";
import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {Console, DefaultColor4, manager, Util} from "@openremote/core";
import {
    Alarm,
    AnomalyDetectionConfigObject,
    AnomalyDetectionConfiguration, AnomalyDetectionConfigurationChange,
    AnomalyDetectionConfigurationGlobal,
    AnomalyDetectionConfigurationUnion,
    Asset,
    AssetDatapointQueryUnion,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef,
    DatapointInterval, AlarmConfig, User, UserQuery,
    ValueDatapoint
} from "@openremote/model";
import {ErrorObject, OrJSONForms, StandardRenderers} from "@openremote/or-json-forms";
import "@openremote/or-json-forms";
import {showJsonEditor} from "@openremote/or-json-forms";
import {i18next, translate} from "@openremote/or-translate";
import "@openremote/or-components/or-collapsible-panel"
import {createRef, Ref, ref} from 'lit/directives/ref.js';


//language=css
const styling = css`
    .test {
        display: flex;
        flex-direction: column;
        width: 100%;
    }
    .item{
        display: flex;
        width: 100%;
    }
`
@customElement("or-anomaly-config-input")
export class OrAnomalyConfigInput extends translate(i18next)(LitElement) {

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
    @property({type: Boolean})
    updateBool: boolean = false;
    @property()
    public users: User[] = [];
    @property({type:Object})
    public template!: TemplateResult
    @property({type:Number})
    public selectedIndex: number = -1;
    @property({type:Number})
    public previousConfig?: AnomalyDetectionConfigurationUnion = undefined;
    @property({type: String, attribute: false})
    public onChange?: (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}, update: boolean) => void;


    async connectedCallback(): Promise<void> {
        await this.loadUsers();
        super.connectedCallback();
    }
    render() {
        return this.draw()
    }

    protected draw(){
        console.log("draw")
        const uiSchema: any = {
            type: "Control",
            scope: "#"
        };
        const jsonFormsInput: Ref<OrJSONForms> = createRef();
        const options: {value: string | undefined, label: string | undefined}[] = [{value:"",label:"" }]
        const types = [{value:"global", label:"Range"},{value:"change", label:"Change"},{value:"forecast", label:"Forecast"}]
        this.users.map((u) => {
            return { value: u.id, label: u.username };
        }).forEach(U => options.push(U));
        if(this.selectedIndex == -1){
            this.selectedIndex = this.anomalyDetectionConfigObject!.methods!.length > 0 ? 0:-1;
        }

        if(!this.anomalyDetectionConfigObject || !this.attributeRef || !this.anomalyDetectionConfigObject.methods)return html``;
        const attributeRef = this.attributeRef;
        const showJson = (ev: Event) => {
            ev.stopPropagation();
            showJsonEditor(`${i18next.t("anomalyDetection.configuration")}`, this.anomalyDetectionConfigObject, ((newValue: any) => {
                this.updateConfigObject(newValue as AnomalyDetectionConfigObject);
            }));
        };

        const onChanged = (dataAndErrors: { errors: ErrorObject[] | undefined, data: any }) => {
            let newconfig =dataAndErrors.data as AnomalyDetectionConfigurationUnion
            this.updateData(newconfig)
            if (jsonFormsInput.value) {
                if(newconfig){
                    jsonFormsInput.value.data = newconfig;
                }
            }
        }
        const onCollapseToggled = (expanded: boolean) => {
            this.expanded = expanded;
            if(expanded){
                this.updateBool = !this.updateBool
            }
        }
        const doLoad = async (con: AnomalyDetectionConfigObject) => {
            if (jsonFormsInput.value) {
                if(con.methods![this.selectedIndex]){
                    jsonFormsInput.value.data = con.methods![this.selectedIndex];
                }
            }
        }
        const DetectionConfig = (method: AnomalyDetectionConfigurationUnion) =>{
            return html`
                <div style="display: flex; flex-direction: column;">
                    <or-mwc-input type="checkbox" .label="${i18next.t("active")}" .value="${method.onOff}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"onOff")}"></or-mwc-input>
                    <or-mwc-input style="padding-top: 10px;"  required="true" type="select" .label="${i18next.t("type")}" .options="${types.map((obj) => obj.label) || undefined}"
                                  .value="${method.type ? types.filter((obj) => obj.value === this.anomalyDetectionConfigObject!.methods![this.selectedIndex].type).map((obj) => obj.label)[0] : ""}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      e.detail.value = types.filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0]
                                      this.updateProperty(e,"type");
                                  }}"></or-mwc-input>
                    ${DetectionMethodConfig(method)}
                    <or-mwc-input type="button" style="padding-top: 10px;" outlined icon="test-tube" .label="${i18next.t("anomalyDetection.testMethod")}"
                                    @or-mwc-input-changed="${() => this.updateGraph()}" ></or-mwc-input>
                </div>
                
            `;
        }
        const DetectionMethodConfig = (method: AnomalyDetectionConfigurationUnion) =>{
            switch (method.type){
                case "change":
                    return html`
                        <or-mwc-input style="padding-top: 10px;" required="true" type="number" .label="${i18next.t("anomalyDetection.deviationChange")}" .value="${method.deviation}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"deviation")}"></or-mwc-input>
                        <or-mwc-input style="padding-top: 10px;" required="true" type="text" .label="${i18next.t("anomalyDetection.minimumTimespan")}" .value="${method.timespan}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"timespan")}"></or-mwc-input>
                    `;
                case "global":
                    return html`
                        <or-mwc-input style="padding-top: 10px;" required="true" type="number" .label="${i18next.t("anomalyDetection.deviationGlobal")}" .value="${method.deviation}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"deviation")}"></or-mwc-input>
                        <or-mwc-input style="padding-top: 10px;" required="true" type="text" .label="${i18next.t("anomalyDetection.minimumTimespan")}" .value="${method.timespan}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"timespan")}"></or-mwc-input>
                    `;
                case "forecast":
                    return html`
                        <or-mwc-input style="padding-top: 10px;" required="true" type="number" .label="${i18next.t("anomalyDetection.deviationForecast")}" .value="${method.deviation}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"deviation")}"></or-mwc-input>
                    `;
                default:
                    return html`
                        
                    `;
            }
        }
        window.setTimeout(() => doLoad(this.anomalyDetectionConfigObject as AnomalyDetectionConfigObject), 0);

        return html`
            <div class="test">
                <or-collapsible-panel style="width: 100%" .onChange="${onCollapseToggled}">
                    <div slot="header" style="width: 80%">
                            <span>
                            ${i18next.t("anomalyDetection.")}
                            </span>
                    </div>
                    <div slot="header-description" style="display: flex; flex-direction: row-reverse; width: 100%">
                        <or-mwc-input type="button" outlined .label="${i18next.t("json")}" icon="pencil" @click="${(ev: Event) => {ev.stopPropagation();}}"  @or-mwc-input-changed="${(ev: Event) => showJson(ev)}"></or-mwc-input>
                    </div>

                    <div class="test" slot="content">
                        <div style="display: flex; padding: 0 16pt; margin-left: 10pt">
                            ${this.anomalyDetectionConfigObject.methods.map((m) => {
                                const i = this.anomalyDetectionConfigObject!.methods?.indexOf(this.anomalyDetectionConfigObject!.methods?.find(x => x === m)!)!;
                                return html`
                                    <div .style="z-index: 10; margin:0 2pt; border-top-right-radius:4pt; border-top-left-radius:4pt; border-color:lightgray; border-style:solid; border-width:thin; ${i === this.selectedIndex ? "border-bottom-color: white;": "border-bottom-style: none;"}">
                                        <or-mwc-input type="button" .label="${m.name}" @or-mwc-input-changed="${() =>{ this.selectedIndex = i; this.updateGraph();}}" ></or-mwc-input>
                                    </div>
                                `
                            })}
                            <or-mwc-input type="button" label="${i18next.t("anomalyDetection.addMethod")}" icon="plus" @or-mwc-input-changed="${() =>{this.addMethod();}}" ></or-mwc-input>
                        </div >
                        ${this.selectedIndex == -1? html`
                            <p style="padding-left:16pt ">${i18next.t("anomalyDetection.noMethodsCreated")}</p>
                        `: html`
                            <div style="margin: 16pt; padding: 16pt; margin-top: -1px; border-style: solid; border-color: lightgray; border-width: thin; border-radius: 4pt;">
                                <div style="display: flex;  justify-content: space-between; width: 100%">
                                    <div class="item" style="justify-content: left">
                                        <or-mwc-input  .label="${i18next.t("name")}" type="text" .value="${this.anomalyDetectionConfigObject!.methods![this.selectedIndex].name!}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"name")}"></or-mwc-input>
                                    </div>

                                    <div class="item" style="justify-content: right">
                                        <or-mwc-input .style="visibility: ${this.selectedIndex == -1? 'hidden':'visible'};" type="button" icon="close-circle" @or-mwc-input-changed="${() =>{ this.removeMethod(this.selectedIndex);}}" ></or-mwc-input>
                                    </div>
                                </div>
                                <div .style="visibility: ${this.selectedIndex == -1? 'hidden':'visible'};" class="test" slot="content" >
                                    <div style="display: flex; justify-content: space-between; flex-direction: row;">
                                        <div style="width: 45%">
                                            <p>${i18next.t("anomalyDetection.detectionMethod")}</p>
                                            ${DetectionConfig(this.anomalyDetectionConfigObject.methods[this.selectedIndex])}
                                        </div>
                                        <div style="width: 45%;  display: flex; flex-direction: column;">
                                            <p>${i18next.t("alarm.")}</p>
                                            ${this.anomalyDetectionConfigObject.methods.map((m) => {
                                                const i = this.anomalyDetectionConfigObject!.methods?.indexOf(this.anomalyDetectionConfigObject!.methods?.find(x => x === m)!)!;
                                                if(i === this.selectedIndex) return html`
                                                        <or-mwc-input .readonly="${!m.onOff}" type="checkbox" .label="${i18next.t("active")}" .value="${m.onOff? m.alarmOnOff: false}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"alarmOnOff")}"></or-mwc-input>
                                            <or-mwc-input .required="${m.alarmOnOff}" style="padding-top: 10px;" .value="${m.alarm?.severity?m.alarm?.severity:""}" type="select" .options="${["LOW","MEDIUM","HIGH"]}" label="${i18next.t("alarm.severity")}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"alarm.severity")}"></or-mwc-input>
                                            <or-mwc-input style="padding-top: 10px;" .label="${i18next.t("alarm.assignee")}" placeholder=" " type="select"
                                                          .options="${options.map((obj) => obj.label) || undefined}"
                                                          .value="${m.alarm!.assigneeId ? options.filter((obj) => obj.value === this.anomalyDetectionConfigObject!.methods![this.selectedIndex].alarm!.assigneeId).map((obj) => obj.label)[0] : ""}"
                                                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                                    e.detail.value = options.filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0]
                                                    this.updateProperty(e,"alarm.assigneeId");
                                                }}"></or-mwc-input>
                                            <or-mwc-input style="padding-top: 10px;" .value="${m.alarm?.content?m.alarm?.content:""}" type="textarea"  label="${i18next.t("alarm.content")}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.updateProperty(e,"alarm.content")}"></or-mwc-input>
                                `
                                            })}
                                            
                                        </div>
                                    </div>
                                    <or-anomaly-config-chart style="display: flex; width: auto;"
                                                             .timePresetKey="${this.updateBool}"
                                                             .panelName="${this.selectedIndex}"
                                                             .anomalyConfig="${this.anomalyDetectionConfigObject ? this.anomalyDetectionConfigObject.methods![this.selectedIndex] : undefined}" .attributeRef="${attributeRef}"
                                                             .canRefresh="${this.expanded}">
                                    </or-anomaly-config-chart>
                                </div>
                            </div>
                        `}
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
            con = {name:"Method "+ (this.anomalyDetectionConfigObject.methods!.length +1 ),type:"global", onOff:true, deviation:10, minimumDatapoints:2, timespan:"PT20M", alarm:{content:"%ASSET_NAME%\n%ATTRIBUTE_NAME%\n%METHOD_TYPE%", assigneeId:undefined},alarmOnOff:false }
            this.anomalyDetectionConfigObject.methods![i] = con;
            this.selectedIndex = i;
            this.updateGraph();
        }
    }
    protected removeMethod(index:number) {
        if(this.anomalyDetectionConfigObject){
            this.anomalyDetectionConfigObject.methods?.splice(index,1);
            this.selectedIndex--;
            this.updateGraph();
        }
    }
    protected updateProperty(e:OrInputChangedEvent, prop:string){
        if(this.anomalyDetectionConfigObject && this.anomalyDetectionConfigObject.methods){
            let newconfig:AnomalyDetectionConfigurationUnion = JSON.parse(JSON.stringify(this.anomalyDetectionConfigObject.methods[this.selectedIndex]));
            if(prop.includes("alarm.")){
                if(typeof newconfig.alarm![prop as keyof AlarmConfig] === typeof e.detail.value || typeof undefined){
                    //can't dynamicly set value of property using a string in typescript
                    // @ts-ignore
                    newconfig.alarm[prop.split("alarm.")[1]] = e.detail.value;
                }
            }else{
                if(typeof newconfig[prop as keyof AnomalyDetectionConfigurationUnion] === typeof e.detail.value || typeof undefined){
                    //can't dynamicly set value of property using a string in typescript
                    // @ts-ignore
                    newconfig[prop] = e.detail.value;
                }
            }
            this.updateData(newconfig);
        }
    }
    protected updateGraph(){
        // test if updated values are influential on drawing the graph and if so update that data
        if(this.previousConfig != undefined){
            // copy non impactfull properties to only compare the values which would change the graph
            this.previousConfig.name = this.anomalyDetectionConfigObject!.methods![this.selectedIndex].name;
            this.previousConfig.alarm = this.anomalyDetectionConfigObject!.methods![this.selectedIndex].alarm;
            this.previousConfig.alarmOnOff = this.anomalyDetectionConfigObject!.methods![this.selectedIndex].alarmOnOff;
            this.previousConfig.onOff = this.anomalyDetectionConfigObject!.methods![this.selectedIndex].onOff;
            if(!Util.objectsEqual(this.previousConfig, this.anomalyDetectionConfigObject!.methods![this.selectedIndex])){
                this.updateBool = !this.updateBool;
            }
        }else{
            this.updateBool = !this.updateBool;
        }
        this.previousConfig = JSON.parse(JSON.stringify( this.anomalyDetectionConfigObject!.methods![this.selectedIndex]))
    }

    protected updateData(newConfig:AnomalyDetectionConfigurationUnion){
        let valid = true;
        let update = false;
        if (newConfig) {
            let config = JSON.parse(JSON.stringify(this.anomalyDetectionConfigObject));
            config.methods[this.selectedIndex] =  newConfig
            this.updateConfigObject(config)
        }
    }
    protected updateConfigObject(newConfig:AnomalyDetectionConfigObject){
        let valid = true;
        if(newConfig && newConfig.methods && newConfig.methods[0]){
            let index = 1;
            newConfig.methods.forEach(method =>{
                for(let i = index; i < newConfig.methods!.length;i++){
                    if( method.name === newConfig.methods![i].name) valid = false;
                }

                index++
                if ( !method.type || !method.deviation || method.onOff === undefined || method.alarmOnOff === undefined || !method.alarm  || (method.alarmOnOff? !method.alarm!.severity:false) || !method.name || method.name === "") valid = false;
                method.alarm!.realm = manager.getRealm();
                if(method.type === "global"){
                    if(!this.testValid((method as AnomalyDetectionConfigurationGlobal).minimumDatapoints,(method as AnomalyDetectionConfigurationGlobal).timespan as string)){
                        valid = false;
                    }
                }else if(method.type === "change"){
                    if(!this.testValid((method as AnomalyDetectionConfigurationChange).minimumDatapoints,(method as AnomalyDetectionConfigurationChange).timespan as string)){
                        valid = false;
                    }
                }
            })
            if (!Util.objectsEqual(newConfig, this.anomalyDetectionConfigObject)) {
                this.anomalyDetectionConfigObject = newConfig;
                if (this.onChange) {
                    this.onChange({data: this.anomalyDetectionConfigObject, errors: []},valid);
                }
            }
        }
    }
    protected testValid(minimumDatapoints:number|undefined, timespan:string|undefined):boolean{
        if(!timespan || timespan === "" || !/^P(\d+Y)?(\d+M)?(\d+W)?(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?$/.test(timespan)){
            return false;
        }else if(!minimumDatapoints || minimumDatapoints < 1){
            minimumDatapoints = 2;
            return false;
        }
        return true;
    }
    protected async loadUsers() {
        const usersResponse = await manager.rest.api.UserResource.query({
            realmPredicate: { name: manager.displayRealm },
        } as UserQuery);

        if (usersResponse.status !== 200) {
            return;
        }

        this.users = usersResponse.data.filter((user) => user.enabled && !user.serviceAccount);
    }

}
