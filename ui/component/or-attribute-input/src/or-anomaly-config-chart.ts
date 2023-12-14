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
import { html, PropertyValues} from "lit";
import {Console, DefaultColor4, manager, Util} from "@openremote/core";
import moment from "moment";
import {throttle} from "lodash";
import {AnnotationOptions} from "chartjs-plugin-annotation";
import {
    AnomalyDetectionConfigObject,
    AnomalyDetectionConfiguration,
    AnomalyDetectionConfigurationChange,
    AnomalyDetectionConfigurationGlobal,
    Asset,
    AssetDatapointQueryUnion,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef,
    DatapointInterval,
    ValueDatapoint
} from "@openremote/model";
import {i18next, translate} from "@openremote/or-translate";
import "@openremote/or-components/or-collapsible-panel"
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";



@customElement("or-anomaly-config-chart")
export class OrAnomalyConfigChart extends OrChart {

    @property({type: Object})
    public attributeRef?: AttributeRef = undefined;
    @property({type: Object})
    public anomalyConfig?: AnomalyDetectionConfiguration = undefined;
    @property({type: Number})
    public timespan?: Number = undefined;
    @property({type: String})
    private errorMessage = ""
    @property({type: Boolean})
    private canRefresh = false


    protected willUpdate(changedProps: Map<string, any>) {
        return super.willUpdate(changedProps);
    }

    protected async _loadData() {
        let timespan=0
        if(this._loading || !this.anomalyConfig || !this.canRefresh){
            return
        }
        if(this.anomalyConfig.onOff == undefined || !this.anomalyConfig.type || !this.anomalyConfig.deviation){
            return;
        }
        if(this.anomalyConfig.type==="global"){
            if((this.anomalyConfig as AnomalyDetectionConfigurationChange).timespan === "" || !(this.anomalyConfig as AnomalyDetectionConfigurationGlobal).timespan
                || !(this.anomalyConfig as AnomalyDetectionConfigurationGlobal).minimumDatapoints ||!/^P(\d+Y)?(\d+M)?(\d+W)?(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?$/.test((this.anomalyConfig as AnomalyDetectionConfigurationGlobal).timespan as string)){
                this.errorMessage = i18next.t("anomalyDetection.invalidConfiguration");
                return
            }
        }else if(this.anomalyConfig.type==="change"){
            if((this.anomalyConfig as AnomalyDetectionConfigurationChange).timespan === "" || !(this.anomalyConfig as AnomalyDetectionConfigurationChange).timespan
                || !(this.anomalyConfig as AnomalyDetectionConfigurationChange).minimumDatapoints || !/^P(\d+Y)?(\d+M)?(\d+W)?(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?$/.test((this.anomalyConfig as AnomalyDetectionConfigurationChange).timespan as string)){
                this.errorMessage = i18next.t("anomalyDetection.invalidConfiguration");
                return
            }
        }
        if(this.attributeRef){
            this._loading = true
            const query = {
                ids: [this.attributeRef.id],
                select: {
                    attributes: [
                        this.attributeRef.name
                    ]
                },
            } as AssetQuery;
            try {
                const response = await manager.rest.api.AssetResource.queryAssets(query);
                this.assets = response.data || [];
                if(this.attributeRef.name){
                    if(this.assets[0].attributes){
                        this.assetAttributes[0] = [0,this.assets[0].attributes[this.attributeRef.name]]
                    }
                }
            } catch (e) {
                console.error("Failed to get assets requested in settings", e);
            }


        }

        if (this._data || !this.assetAttributes || !this.assets || (this.assets.length === 0 && !this.dataProvider) || (this.assetAttributes.length === 0 && !this.dataProvider)) {
            this._loading = false
            return;
        }
        this._loading = true;

        this._startOfPeriod = 0;
        this._endOfPeriod = 0;
        if(this.anomalyConfig.type === "global" || this.anomalyConfig.type === "change"){
            timespan =  moment.duration((this.anomalyConfig as AnomalyDetectionConfigurationGlobal | AnomalyDetectionConfigurationChange).timespan).asMilliseconds()
            this._startOfPeriod = Date.now() - timespan * 5;
            this._endOfPeriod = Date.now();
        }else if(this.anomalyConfig.type === "forecast"){
            timespan = 1000*60*20;
            this._startOfPeriod = Date.now() - timespan * 2;
            this._endOfPeriod = Date.now() + timespan;
        }

        this.datapointQuery = {
            type: "all",
            fromTimestamp: Date.now()- timespan * 5,
            toTimestamp: Date.now()
        }


        const diffInHours = (this._endOfPeriod - this._startOfPeriod) / 1000 / 60 / 60;
        const intervalArr = this._getInterval(diffInHours);

        const stepSize: number = intervalArr[0];
        const interval: DatapointInterval = intervalArr[1];

        const lowerCaseInterval = interval.toLowerCase();
        this._timeUnits =  lowerCaseInterval as TimeUnit;
        this._stepSize = stepSize;

        const data: ChartDataset<"line", ScatterDataPoint[]>[] = [];
        let promises;

        if(this.dataProvider) {
            await this.dataProvider(this._startOfPeriod, this._endOfPeriod, (interval.toString() as TimeUnit), stepSize).then((dataset) => {
                dataset.forEach((set) => { data.push(set); });
            });
        } else {
            promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

                const asset = this.assets[assetIndex];
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
                const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                const unit = Util.resolveUnits(Util.getAttributeUnits(attribute, descriptors[0], asset.type));
                const colourIndex = index % this.colors.length;


                //limits anomaly data
                let datasets = await this.getAnomalyLimits(asset,attribute,this.datapointQuery)
                    datasets = datasets as ChartDataset<"line",ScatterDataPoint[]>[];
                    if(datasets.length !== 0){
                        let dataset = datasets[2];
                        (dataset as any).assetId = asset.id;
                        (dataset as any).attrName = attribute.name;
                        (dataset as any).unit = unit;
                        dataset = datasets[0];
                        data.push(dataset);
                        dataset = datasets[1];
                        data.push(dataset);
                        for (let i = 2; i < datasets.length; i=i+2){
                            dataset = datasets[i];
                            data.push(dataset);
                            dataset = datasets[i+1];
                            data.push(dataset);
                        }
                    }
            });
        }

        if(promises) {
            await Promise.all(promises);
        }
        if(data[0] && data[0].data.length !== 0){
            this._data = data;
        }
        this._loading = false;

    }

    protected async getAnomalyLimits(asset: Asset, attribute:Attribute<any>,query:AssetDatapointQueryUnion): Promise<ChartDataset<"line", ScatterDataPoint[]>[]>{
        let datasets : ChartDataset<"line", ScatterDataPoint[]>[] = [];
        let response: GenericAxiosResponse<ValueDatapoint<any>[][]>;
        let minData : ChartDataset<"line", ScatterDataPoint[]> ={
            borderColor: DefaultColor4 + "80",
            backgroundColor: DefaultColor4 + "80",
            label: "min",
            pointRadius: 0,
            animation: false,
            fill: false,
            data: [],
        };
        let anomalyDataset: ChartDataset<"line", ScatterDataPoint[]> ={
            pointStyle: "cross",
            label: "anomalies",
            animation: false,
            pointRadius :10,
            pointRotation: 45,
            pointBorderWidth: 2,
            backgroundColor: "#00000000",
            borderColor: "#00000000",
            pointBorderColor: "#be0000",
            pointBackgroundColor: "#be0000",
            data:[]
        };

        let maxData : ChartDataset<"line", ScatterDataPoint[]> ={
            borderColor: DefaultColor4 + "80",
            backgroundColor: DefaultColor4 + "80",
            label: "max",
            animation: false,
            pointRadius: 0,
            fill: 3,
            data: [],
        };
        let dataset: ChartDataset<"line", ScatterDataPoint[]> = {
            borderColor: "#2844cc",
            backgroundColor: "#2844cc",
            label: "data",
            animation: false,
            pointRadius: 2,
            segment:{
                borderDash: (ctx => {
                    return  ctx.p0.parsed.x > Date.now()? [2, 4] : undefined
                })
            },
            fill: false,
            data: [],
        };
        if(this.anomalyConfig){
            response = await manager.rest.api.AnomalyDetectionResource.getAnomalyDatapointLimits(asset.id, attribute.name, this.anomalyConfig);
            if (response.status === 200) {
                if(response.data.length === 0){
                    this.errorMessage = i18next.t("anomalyDetection.noDatapointsSaved");
                    return datasets;
                }else if(response.data.length === 1){
                    this.errorMessage = i18next.t("anomalyDetection.notEnoughDatapointsSaved");
                    return datasets;
                }
                dataset.data = response.data[2].filter(value => value !== null && value !== undefined) as ScatterDataPoint[];
                anomalyDataset.data = response.data[3].filter(value => value !== null) as ScatterDataPoint[];
                let limitSectionCount = 0;
                while(response.data[1].length > 1) {
                    let minimaxdata = response.data[1].slice(response.data[1].findIndex(d => d !== null),response.data[1].length);
                    let index = minimaxdata.findIndex(d => d === null);
                    minimaxdata = minimaxdata.slice(0,index=== -1? minimaxdata.length:index);
                    maxData.data = minimaxdata.filter(value => value !== null && value !== undefined) as ScatterDataPoint[];
                    datasets.push(JSON.parse(JSON.stringify(maxData)));
                    response.data[1] =  response.data[1].splice(response.data[1].findIndex(d => d !== null) + minimaxdata.length, response.data[1].length);
                    limitSectionCount++
                }
                let i = 1
                datasets.forEach(ds =>{
                    ds.fill = limitSectionCount +i
                    i++;
                })
                datasets.push(dataset);

                while(response.data[0].length > 1){
                    let minimindata = response.data[0].slice(response.data[0].findIndex(d => d !== null),response.data[0].length);
                    let index = minimindata.findIndex(d => d === null);
                    minimindata = minimindata.slice(0,index=== -1? minimindata.length:index);
                    minData.data = minimindata.filter(value => value !== null && value !== undefined) as ScatterDataPoint[];
                    datasets.push(JSON.parse(JSON.stringify(minData)));
                    response.data[0] = response.data[0].splice(response.data[0].findIndex(d => d !== null) + minimindata.length, response.data[0].length);
                }
                datasets.push(anomalyDataset);
            }else if(response.status === 204){
                this.errorMessage = i18next.t("anomalyDetection.invalidConfiguration");
            }


        }
        return datasets;
    }

    render() {
        const disabled = this._loading;
        return html`
                    <div id="chart-container" style="display: flex; ">
                        ${disabled ? html`
                        <div style="position: absolute; height: 100%; width: 100%;">
                            <or-loading-indicator ?overlay="false"></or-loading-indicator>
                        </div>
                    ` : undefined}
                        ${this._data != undefined ? html`
                            <canvas id="chart" style="visibility: ${disabled ? 'hidden' : 'visible'}"></canvas>
                        `: html`
                            <p>${this.errorMessage}</p>
                        ` }
                        
                    </div>
        `;
    }
}
