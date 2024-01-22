import {WidgetConfig} from "../util/widget-config";
import {
    Asset,
    AssetStateDuration,
    Attribute,
    AttributeRef,
    GeoJSON,
    GeoJSONPoint,
    ValueDatapoint
} from "@openremote/model";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {customElement, property} from "lit/decorators.js";
import {html, TemplateResult} from "lit";
import moment from "moment";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import manager from "@openremote/core";
import {OrMwcTableRowClickEvent, TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import "@openremote/or-map"
import { OrMap } from "@openremote/or-map";
import { until } from "lit/directives/until.js";


export interface SessionWidgetConfig extends WidgetConfig {
    period?: 'year' | 'month' | 'week' | 'day' | 'hour';
    analysisType?: AnalysisType[];
    InputRef?: AttributeRef[];
    OutputRef?: AttributeRef[];
    showTimestampControls: boolean;
    customFieldOne: string;
    customFieldTwo: number;
}

export interface AnalysisType {
    Geo_JSONPoint?: PointAnalysisTypes,
    number?: NumberAnalysisTypes
}

export enum PointAnalysisTypes {
    'Displacement', "Distance", "Average Speed", "Count", "Overview"
}
export enum NumberAnalysisTypes {
    "Difference", "Sum", "Count", "Maximum", "Minimum", "Average"
}

function getDefaultWidgetConfig(): SessionWidgetConfig {
    return {
        period: 'day',
        analysisType: [{number: NumberAnalysisTypes.Minimum}],
        showTimestampControls: false,
        customFieldOne: "default text",
        customFieldTwo: 0
    };
}

interface OverviewData {
    'startData' : any,
    'endData' : any
}

export interface TripData {
    [id: number]: number,

    'trip': ValueDatapoint<AssetStateDuration>,
    'points': ValueDatapoint<GeoJSONPoint | number>[];
    'overview'?: OverviewData
}

@customElement("session-widget")
export class SessionWidget extends OrWidget {

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: SessionWidgetConfig;

    private static geocodeUrl: string = "https://nominatim.openstreetmap.org"

    @property({type: Object})
    private sessions: TripData[] = [];

    static getManifest(): WidgetManifest {
        return {
            displayName: "Session", // name to display in widget browser
            displayIcon: "database-clock", // icon to display in widget browser. Uses <or-icon> and https://materialdesignicons.com
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: SessionWidgetConfig): OrWidget {
                return new SessionWidget(config);
            },
            getSettingsHtml(config: SessionWidgetConfig): WidgetSettings {
                return new SessionSettings(config);
            },
            getDefaultConfig(): SessionWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    public async connectedCallback(){
        super.connectedCallback();
        if (!this.isValidConfig(this.widgetConfig)) return;

        const sessionStore: TripData[] = await this._loadSessions();
        const overviewData: any[] = [];

        for (const [index, value] of this.sessions.entries()) {
            sessionStore[index] = await this._loadDatapoints(value, index)

        }

        this.sessions = sessionStore;

        console.log(this.sessions)
    }

    public async refreshContent(force: boolean) {
        await this.update(new Map());
    }

    async firstUpdated(changedProperties: Map<string, any>) {
        super.update(changedProperties);
        console.log(changedProperties);
        if (!this.isValidConfig(this.widgetConfig)) return;

        const sessionStore: TripData[] = await this._loadSessions();


        for (const [index, value] of this.sessions.entries()) {
            sessionStore[index] = await this._loadDatapoints(value, index)
        }

        this.sessions = sessionStore.sort((a, b) => b.trip.y!.startTime! - a.trip.y!.startTime!);

        console.log(this.sessions)

    }

    isValidConfig = (config: SessionWidgetConfig): boolean => {
        return config.InputRef != null && config.OutputRef != null
    }

    private tableConfig = {
        columnFilter: [],
        stickyFirstColumn: false,
        pagination: {
            enable: true
        }
    }
    private getAddressFormat(addr: NominatimResponse){
        return `${addr.address.road} ${addr.address.house_number}, ${addr.address.postcode} ${addr.address.city}, ${addr.address.country}`;
    }
    protected render(): TemplateResult {
        if (!this.isValidConfig(this.widgetConfig) || this.sessions.length <= 0) return html`
            <span>Invalid Configuration</span>`;

        //We need to get the OutputAttr's name...
        const label: string | undefined = this.asset!.attributes![this.widgetConfig!.InputRef![0].name!].meta!["LABEL"];
        const analysisType = (type: AnalysisType) => {
            return this.widgetConfig.analysisType![0].Geo_JSONPoint != undefined ? this.widgetConfig.analysisType![0].Geo_JSONPoint : this.widgetConfig.analysisType![0].number!
        }
        const analysisTypeLabel = (type: AnalysisType) => {
            return this.widgetConfig.analysisType![0].Geo_JSONPoint != undefined ? PointAnalysisTypes[this.widgetConfig.analysisType![0].Geo_JSONPoint] : NumberAnalysisTypes[this.widgetConfig.analysisType![0].number!]
        }

        const columnHeaders: string[] = ["Start Time", "End Time", "Duration"];
        this.widgetConfig.analysisType?.map((type) => columnHeaders.push(`${label ? label : this.asset!.name} - ${type}`))
        const columns: TableColumn[] = columnHeaders.map((header: string) => {
            return {
                title: header,
                isNumeric: false,
                isSortable: false,
                hideMobile: false
            }
        });
        console.log(this.sessions.map(((trip: TripData) =>{
            return trip.points.length;
        })))
        // let rowContents : TemplateResult[] =
        // this.sessions.map((trip: TripData) => {
        //     rowC
        // });



        const rows: TableRow[] = this.sessions.map((trip: TripData) => {
            const dataRows: TemplateResult[] | undefined = this.widgetConfig.analysisType?.map((type: AnalysisType) => {
                return html`${until(this.getTableElement(trip, type), html`<span>Loading...</span>`)}`
            })
            return {
                content: [
                    html`<span>${moment(trip.trip.y?.startTime).format("LLLL")}</span>`,
                    html`<span>${moment(trip.trip.y?.endTime).format("LLLL")}</span>`,
                    html`<span>${moment.duration(moment(trip.trip.y?.startTime).diff(moment(trip.trip.y?.endTime))).humanize()}</span>`,
                    ...dataRows!
                ],
                clickable: false
            }
        });

        rows.map((test: TableRow) => {
           test.content?.push()
        });

        return html`
            <or-mwc-table .columns="${columns instanceof Array ? columns : undefined}"
                          .columnsTemplate="${!(columns instanceof Array) ? columns : undefined}"
                          .rows="${rows instanceof Array ? rows : undefined}"
                          .rowsTemplate="${!(rows instanceof Array) ? rows : undefined}"
                          .config="${this.tableConfig}"
            ></or-mwc-table>
        `;
    }

    private async getTableElement(data: TripData, type: AnalysisType): Promise<TemplateResult> {
        type = {Geo_JSONPoint: PointAnalysisTypes.Overview}
        if (data.points.length == 0) return html`<span><Inv></Inv>alid data</span>`
        let array: (number | GeoJSONPoint)[] = data.points.map((d) => {return d.y!});

        if(type.Geo_JSONPoint != undefined){

            const pointArray = array as GeoJSONPoint[];
            const totalDistance: number = pointArray.reduce((sum, pointA, i, arr) => i ==0 ? 0 : sum+calculateDistance(arr[i - 1],pointA), 0)
            const duration: number = moment.duration(moment(data.trip.y?.endTime).diff(moment(data.trip.y?.startTime))).asHours()

            if(type.Geo_JSONPoint == PointAnalysisTypes["Average Speed"]) return html`${totalDistance/duration} km/h`;
            if(type.Geo_JSONPoint == PointAnalysisTypes.Displacement) return html`${calculateDistance(pointArray[0], pointArray.at(-1)!)} km`;
            if(type.Geo_JSONPoint == PointAnalysisTypes.Count) return html`${pointArray.length} points`;
            if(type.Geo_JSONPoint == PointAnalysisTypes.Distance) return html`${totalDistance} km`;
            if(type.Geo_JSONPoint == PointAnalysisTypes.Overview) {
                const lastPoint = pointArray[0];
                const firstPoint = pointArray.at(-1)!;


                const startAddress: NominatimResponse = await nominatim.reverseGeocode({lat: firstPoint.coordinates![1].toString(), lon:firstPoint.coordinates![0].toString(), zoom: 18}, SessionWidget.geocodeUrl)!
                const endAddress :NominatimResponse = await nominatim.reverseGeocode({lat: lastPoint.coordinates![1].toString(), lon:lastPoint.coordinates![0].toString(), zoom:18}, SessionWidget.geocodeUrl)!

                // return html`${startAddress.features[0].properties.address.road} ${startAddress.features[0].properties.address.house_number} --> ${endAddress.features[0].properties.address.road} ${endAddress.features[0].properties.address.house_number}`
                return html`<span>
                    ${this.getAddressFormat(startAddress)}
                </span>
                <br>
                <span>
                    <or-icon icon="car-clock"></or-icon>
                    ${totalDistance.toFixed(2)} km
                </span>
                <br>        
                <span>
                    ${this.getAddressFormat(endAddress)}
                </span>
                `
            }
        }else if (type.number != undefined){
            const numArray = array as number[];

            const sum: number = numArray.reduce((a, b) => a + b, 0);

            if(type.number == NumberAnalysisTypes.Sum || type.number == NumberAnalysisTypes.Difference) return html`<span>${sum}</span>`
            if(type.number == NumberAnalysisTypes.Average) return html`<span>${sum/numArray.length}</span>`
            if(type.number == NumberAnalysisTypes.Count) return html`<span>${numArray.length} points</span>`
            if(type.number == NumberAnalysisTypes.Maximum) return html`<span>${Math.max(...numArray)}</span>`
            if(type.number == NumberAnalysisTypes.Minimum) return html`<span>${Math.min(...numArray)}</span>`
        }else{
            return html`<span>Data Type not implemented</span>`;
        }
        return html`<span>Data Type not implemented</span>`;
    }

    private asset?: Asset = undefined;
    private async _loadSessions() : Promise<TripData[]> {
        let sessionStore: TripData[] = [];

        const getAssetData = manager.rest.api.AssetResource.get(this.widgetConfig.InputRef![0].id)
            .then((response) => {
                this.asset = response.data;
                const latestTrip  =this.asset!.attributes!["LastTripStartedAndEndedAt"]! as Attribute<AssetStateDuration>;
                if (latestTrip.value != undefined && latestTrip.timestamp != undefined){
                    // sessionStore.push({trip: {x: latestTrip.timestamp, y: latestTrip.value}, points: []})
                }
                console.log("asset data loaded")
            })
            .catch((err) => console.log(err))


        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.widgetConfig.InputRef![0].id,
            this.widgetConfig.InputRef![0].name,
            {
                type: "all",
                fromTimestamp: moment().startOf(this.widgetConfig.period as any).valueOf(),
                toTimestamp: moment().valueOf()
            }
        );
        if (response.status === 200 && response.data.length > 0){
            const responseData = response.data.length >= 10 ? response.data.slice(0,10) : response.data;
            sessionStore = responseData.map((data: ValueDatapoint<AssetStateDuration>) => {
                return {trip: data, points: [], overview: undefined}
            })
        } else {
            console.log(response.data);
        }
        return sessionStore;
    }

    private async _loadDatapoints(session: TripData, index: number) : Promise<TripData> {
        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.widgetConfig.OutputRef![0].id,
            this.widgetConfig.OutputRef![0].name,
            {
                type: "all",
                fromTimestamp: moment(session.trip.y!.startTime).valueOf(),
                toTimestamp: moment(session.trip.y!.endTime).valueOf()
            }
        );
        if (response.status === 200 && response.data.length > 0) {
            session.points = response.data
            return session;
        } else {
            session.points = [];
            return session;
        }
    }
}





// Settings element
// This can be placed in a seperate file if preferred.
@customElement("session-settings")
export class SessionSettings extends WidgetSettings {

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: SessionWidgetConfig;

    @property({type: String})
    private inputAttrType: string | undefined = "";
    @property({type: String})
    private outputAttrType: string | undefined = "";


    public analysisTypes : Map<String, string[]> = new Map;
    private analysisTypeAction: string | undefined;

    public connectedCallback(){
        super.connectedCallback();
        this.analysisTypes.set("GEO_JSONPoint", Object.values(PointAnalysisTypes) as string[])
        this.analysisTypes.set("number", Object.values(NumberAnalysisTypes) as string[])
    }

    protected render(): TemplateResult {
        const searchProvider: (search?: string) => Promise<[any, string][]> = async (search) => {
            return search ? this.analysisTypes.get(this.outputAttrType!)!.filter(o => o.toLowerCase().includes(search.toLowerCase())) : this.analysisTypes.get(this.outputAttrType!)! as any;
        };
        const attributeOutputFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
            this.outputAttrType = attr.type!
            return ["GEO_JSONPoint", "number"].includes(attr.type!)
        };
        const attributeInputFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
            this.inputAttrType = attr.type!
            return ["AssetStateDuration"].includes(attr.type!)
        };
        console.log("rendering")
        return html`
            <div>
                <!-- Attribute selector -->
                <settings-panel displayName="Session Attribute" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.InputRef}" onlyDataAttrs="${false}"
                                      .attributeFilter="${attributeInputFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onInputAttributeSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <settings-panel displayName="Output Attribute" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.OutputRef}" onlyDataAttrs="${false}"
                                      .attributeFilter="${attributeOutputFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onOutputAttributeSelect(ev)}"
                    ></attributes-panel>
                    ${this.outputAttrType != undefined ? html`
                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .disabled="${this.outputAttrType == undefined}"
                                      .options="${this.analysisTypes.get(this.outputAttrType)}"
                                      .value="${this.analysisTypeAction}" label="Analysis Type"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onAnalysisSelect(ev)}"
                        ></or-mwc-input>
                        
                        <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .disabled="${this.outputAttrType == undefined}"
                                      .options="${this.analysisTypes.get(this.outputAttrType)}"
                                      .value="${this.analysisTypeAction}" label="Analysis Type"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onAnalysisSelect(ev)}"
                        ></or-mwc-input>
                    ` : html`<span>Disabled</span>`}
                    
                </settings-panel>

                <!-- Display settings -->
                <settings-panel displayName="${i18next.t('display')}" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <or-mwc-input .type="${InputType.SELECT}"  style="width: 100%;"
                                      .options="${['year', 'month', 'week', 'day', 'hour']}"
                                      .value="${this.widgetConfig.period}" label="${i18next.t('timeframe')}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeframeSelect(ev)}"
                        ></or-mwc-input>
                        <div class="switchMwcInputContainer">
                            <span>${i18next.t('dashboard.allowTimerangeSelect')}</span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;"
                                          .value="${this.widgetConfig.showTimestampControls}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeframeToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- -->
            </div>
        `;
    }

    protected onButtonClick() {
        this.widgetConfig.customFieldOne = "custom text" + moment();
        this.notifyConfigUpdate();
    }

    protected onInputAttributeSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.InputRef = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected onOutputAttributeSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.OutputRef = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected onTimeframeSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.period = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onTimeframeToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showTimestampControls = ev.detail.value;
        this.notifyConfigUpdate();
    }

    private onAnalysisSelect(ev: OrInputChangedEvent) {
        if(this.outputAttrType == "GEO_JSONPoint"){
            this.widgetConfig.analysisType = [{Geo_JSONPoint: ev.detail.value}]
        }
        if(this.outputAttrType == "number"){
            this.widgetConfig.analysisType = [{number: ev.detail.value}]
        }
        console.log(this.widgetConfig.analysisType)
        this.notifyConfigUpdate();
    }
}

export function toRadians(degrees: number): number {
    return degrees * Math.PI / 180;
}

// This is bad programming, but I can't really add a new library here, so I'm just using the actual math for
// generating the distance between GeoJSONPoints.
export function calculateDistance(point1: GeoJSONPoint, point2: GeoJSONPoint): number {
    if(point1 == undefined || point2 == undefined) return 0;
    const R = 6371; // Radius of the Earth in kilometers

    const lat1 = toRadians(point1.coordinates![1]);
    const lat2 = toRadians(point2.coordinates![1]);
    const deltaLat = toRadians(point2.coordinates![1] - point1.coordinates![1]);
    const deltaLon = toRadians(point2.coordinates![0] - point1.coordinates![0]);

    const a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
        Math.cos(lat1) * Math.cos(lat2) *
        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    return R * c; // Distance in kilometers
}
