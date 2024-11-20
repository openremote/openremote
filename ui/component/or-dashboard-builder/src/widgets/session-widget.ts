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
import * as nominatim from "../util/nominatim-browser"
import {NominatimResponse} from "../util/nominatim-browser";


export interface SessionWidgetConfig extends WidgetConfig {
    period?: 'year' | 'month' | 'week' | 'day' | 'hour';
    analysisType?: AnalysisType[];
    InputRef?: {ref: AttributeRef[], type?: string };
    OutputRef?: {ref: AttributeRef[], type?: string };
    showTimestampControls: boolean;
    customFieldOne: string;
    customFieldTwo: number;
}

export interface AnalysisType {
    Geo_JSONPoint?: PointAnalysisTypes,
    number?: NumberAnalysisTypes
}

export enum PointAnalysisTypes {
    DISPLACEMENT = "Displacement", DISTANCE = "Distance", AVERAGE_SPEED = "Average Speed", COUNT = "Count", OVERVIEW = "Overview"
}
export enum NumberAnalysisTypes {
    DIFFERENCE = "Difference", SUM = "Sum", COUNT = "Count", MAXIMUM = "Maximum", MINIMUM = "Minimum", AVERAGE = "Average"
}

function getDefaultWidgetConfig(): SessionWidgetConfig {
    return {
        period: 'day',
        analysisType: [{number: NumberAnalysisTypes.MINIMUM}],
        showTimestampControls: false,
        InputRef: {ref: [], type: undefined},
        OutputRef: {ref: [], type: undefined},
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
    }

    public async refreshContent(force: boolean) {
        await this.update(new Map());
    }

    async firstUpdated(changedProperties: Map<string, any>) {
        super.update(changedProperties);
        if (!this.isValidConfig(this.widgetConfig)) return;

        const sessionStore: TripData[] = await this._loadSessions();


        for (const [index, value] of this.sessions.entries()) {
            sessionStore[index] = await this._loadDatapoints(value, index)
        }

        this.sessions = sessionStore.sort((a, b) => b.trip.y!.startTime! - a.trip.y!.startTime!);
    }

    isValidConfig = (config: SessionWidgetConfig): boolean => {
        return config.InputRef?.ref.length == 1 && config.InputRef.type != undefined
            && config.OutputRef?.ref.length == 1 && config.OutputRef.type != undefined
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
        if (!this.isValidConfig(this.widgetConfig)) return html`<span>Invalid Configuration</span>`;
        if(this.sessions.length <= 0) return html`<span>No Asset State Duration data-points found. Please generate some, or change the session attribute.</span>`

        //We need to get the OutputAttr's name...
        const label: string | undefined = this.asset!.attributes![this.widgetConfig!.InputRef!.ref[0].name!].meta!["LABEL"];
        const analysisType = (type: AnalysisType) => {
            return this.widgetConfig.analysisType![0].Geo_JSONPoint != undefined ? this.widgetConfig.analysisType![0].Geo_JSONPoint : this.widgetConfig.analysisType![0].number!
        }
        const analysisTypeLabel = (type: AnalysisType) : string => {
            return this.widgetConfig.analysisType![0].Geo_JSONPoint != undefined ? this.widgetConfig.analysisType![0].Geo_JSONPoint! as PointAnalysisTypes : this.widgetConfig.analysisType![0].number! as NumberAnalysisTypes
        }

        const columnHeaders: string[] = ["Start Time", "End Time", "Duration"];
        this.widgetConfig.analysisType?.map((type) => {
            const typeString = type.Geo_JSONPoint == undefined ? type.number?.toString() : type.Geo_JSONPoint.toString();
            columnHeaders.push(`${label ? label : this.asset!.name} - ${typeString + ""}`)
        })
        const columns: TableColumn[] = columnHeaders.map((header: string) => {
            return {
                title: header,
                isNumeric: false,
                isSortable: false,
                hideMobile: false
            }
        });
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
        // type = {Geo_JSONPoint: PointAnalysisTypes.Overview}
        // if (data.points.length == 0) return html`<span>Invalid data</span>`
        let array: (number | GeoJSONPoint)[] = data.points.map((d) => {return d.y!});

        if(type.Geo_JSONPoint != undefined){

            const parsedType: PointAnalysisTypes = type.Geo_JSONPoint as PointAnalysisTypes;

            const pointArray = array as GeoJSONPoint[];
            const totalDistance: number = pointArray.reduce((sum, pointA, i, arr) => i ==0 ? 0 : sum+calculateDistance(arr[i - 1],pointA), 0)
            const duration: number = moment.duration(moment(data.trip.y?.endTime).diff(moment(data.trip.y?.startTime))).asHours()

            if(parsedType == PointAnalysisTypes.AVERAGE_SPEED) return html`${totalDistance/duration} km/h`;
            if(parsedType == PointAnalysisTypes.DISPLACEMENT) return html`${calculateDistance(pointArray[0], pointArray.at(-1)!)} km`;
            if(parsedType == PointAnalysisTypes.COUNT) return html`${pointArray.length} points`;
            if(parsedType == PointAnalysisTypes.DISTANCE) return html`${totalDistance} km`;
            if(parsedType == PointAnalysisTypes.OVERVIEW) {
                if(!(pointArray.length >= 2)) return html`Less than two points between the two timestamps`;
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
        }
        else if (type.number != undefined){
            const parsedType: NumberAnalysisTypes = type.number as NumberAnalysisTypes;

            const numArray = array as number[];

            const sum: number = numArray.reduce((a, b) => a + b, 0);

            if(parsedType == NumberAnalysisTypes.SUM || parsedType == NumberAnalysisTypes.DIFFERENCE) return html`<span>${sum}</span>`
            if(parsedType == NumberAnalysisTypes.AVERAGE) return html`<span>${sum/numArray.length}</span>`
            if(parsedType == NumberAnalysisTypes.COUNT) return html`<span>${numArray.length} points</span>`
            if(parsedType == NumberAnalysisTypes.MAXIMUM) return html`<span>${Math.max(...numArray)}</span>`
            if(parsedType == NumberAnalysisTypes.MINIMUM) return html`<span>${Math.min(...numArray)}</span>`
        }
        else{
            return html`<span>Data Type not implemented</span>`;
        }
        return html`<span>Error</span>`;
    }

    private asset?: Asset = undefined;
    private async _loadSessions() : Promise<TripData[]> {
        let sessionStore: TripData[] = [];

        manager.rest.api.AssetResource.queryAssets({ids: [this.widgetConfig.InputRef!.ref[0].id!]})
            .then((response: { data: Asset[]; }) => {
                this.asset = response.data[0];
                const latestTrip  =this.asset!.attributes!["LastTripStartedAndEndedAt"]! as Attribute<AssetStateDuration>;
                if (latestTrip.value != undefined && latestTrip.timestamp != undefined){
                    // sessionStore.push({trip: {x: latestTrip.timestamp, y: latestTrip.value}, points: []})
                }
            })
            .catch((err) => console.log(err))


        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.widgetConfig.InputRef!.ref[0].id,
            this.widgetConfig.InputRef!.ref[0].name,
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
            this.widgetConfig.OutputRef!.ref[0].id,
            this.widgetConfig.OutputRef!.ref[0].name,
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
    private inputAttrType: string | undefined = this.widgetConfig.InputRef != undefined ? this.widgetConfig.InputRef!.type : undefined;
    @property({type: String})
    private outputAttrType: string | undefined = this.widgetConfig.OutputRef != undefined ? this.widgetConfig.OutputRef!.type : undefined;


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
        return html`
            <div>
                <!-- Attribute selector -->
                <settings-panel displayName="Session Attribute" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.InputRef?.ref}" onlyDataAttrs="${false}"
                                      .attributeFilter="${attributeInputFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onInputAttributeSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <settings-panel displayName="Output Attribute" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.OutputRef?.ref}" onlyDataAttrs="${false}"
                                      .attributeFilter="${attributeOutputFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onOutputAttributeSelect(ev)}"
                    ></attributes-panel>
                    ${this.widgetConfig.OutputRef?.type! != undefined ? html`
                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" 
                                      .options="${this.analysisTypes.get(this.widgetConfig.OutputRef?.type!)}"
                                      .value="${this.widgetConfig.analysisType![0].number == undefined 
                                              ? this.widgetConfig.analysisType![0].number 
                                              : this.widgetConfig.analysisType![0].Geo_JSONPoint}" 
                                  label="Analysis Type"
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

    private asset? :Asset;

    protected onInputAttributeSelect(ev: AttributesSelectEvent) {
        if(ev.detail.attributeRefs.length == 1){
            let type: string = ev.detail.assets[0]!.attributes![ev.detail.attributeRefs[0].name!].type!;
            // let type : string = this.getType(ev.detail.attributeRefs[0])!
            this.widgetConfig.InputRef = {
                ref: ev.detail.attributeRefs as AttributeRef[],
                type: type
            }
        }else {
            this.widgetConfig.InputRef = {
                ref: ev.detail.attributeRefs as AttributeRef[],
                type: undefined
            }
        }
        this.notifyConfigUpdate();
    }

    protected onOutputAttributeSelect(ev: AttributesSelectEvent) {
        if(ev.detail.attributeRefs.length == 1){
            let type: string = ev.detail.assets[0]!.attributes![ev.detail.attributeRefs[0].name!].type!;
            // let type: string = this.getType(ev.detail.attributeRefs![0])!;
          this.widgetConfig.OutputRef = {
                ref: ev.detail.attributeRefs as AttributeRef[],
                type: type
            }
        }else {
            this.widgetConfig.OutputRef = {
                ref: ev.detail.attributeRefs as AttributeRef[],
                type: undefined
            }
        }
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
        if(this.widgetConfig.OutputRef?.type == "GEO_JSONPoint"){
            this.widgetConfig.analysisType = [{Geo_JSONPoint: ev.detail.value as PointAnalysisTypes}]
        }
        if(this.widgetConfig.OutputRef?.type == "number"){
            this.widgetConfig.analysisType = [{number: ev.detail.value as NumberAnalysisTypes}]
        }
        this.notifyConfigUpdate();
    }

    private getType(ref: AttributeRef): string | undefined {
        manager.rest.api.AssetResource.queryAssets({
            ids: [ref.id!]
        })
            .then((response) => {
                let attr = response.data[0].attributes![ref.name!]
                if(attr != undefined) return attr.type!;
            })
            .catch((err) => console.log(err))
        return undefined;
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
