import {LitElement, css, PropertyValues, CSSResultGroup, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import manager, {subscribe, Util} from "@openremote/core";
import "moment";
import {Asset, AssetStateDuration, GeoJSONPoint, ValueDatapoint} from "@openremote/model";
import moment from "moment";
import {OrMap} from "../index";
export interface VehicleTripProp{
    startLocation: GeoJSONPoint | undefined,
    endLocation:GeoJSONPoint | undefined,
    Duration: AssetStateDuration | undefined

}

@customElement("or-map-asset-card-trip-section")
export class OrMapAssetCardTripSection extends subscribe(manager)(LitElement) {


    public static NAME : string = "or-map-asset-card-trip-section";
    @property({type: String})
    public assetId?: string;
    // @property({type: Object})
    // public map?: OrMap;

    private tripHistory: ValueDatapoint<AssetStateDuration>[] | undefined;

    private tripDurationInMinutes: number | undefined;

    private trips: Map<ValueDatapoint<AssetStateDuration>, ValueDatapoint<number>[]> | undefined = new Map();

    disconnectedCallback() {
        super.disconnectedCallback();

    }

    async updated(changedProperties: Map<string, any>){
    }

    async connectedCallback() {
        super.connectedCallback();
        // let addressFeature = await this.map?._map!.getAddress({lat: 51.915890531254405, lon: 4.483153763225772});

        await this._loadData();

        if(this.tripHistory != undefined){

            let latestTripDates = this.tripHistory![0];

            this.trips = await this._loadTrips(this.tripHistory!);
            // let startTime = moment.unix((latestTripDates.y!.endTime!)/1000)
            // let endTime = moment.unix((latestTripDates.y!.startTime!)/1000)
            //
            // this.tripDurationInMinutes =  startTime.diff(endTime, "seconds");
        }




    }

    async _loadData(): Promise<void> {

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.assetId,
            "LastTripStartedAndEndedAt",
            {
                type: "all",
                fromTimestamp: moment().clone().startOf('day').valueOf(),
                toTimestamp: moment().valueOf()
            }
        );
        if (response.status === 200 && response.data.length > 0) {

            if(response.data.length > 10){
                this.tripHistory = response.data.slice(0,10)
            }else{
                this.tripHistory = response.data;
            }
            // console.log(this.tripHistory)
        }
    }

    render(){

        return html`
            <i>You can find the last 5 trips and their info in the console log.</i>
        `
    }

    private async _loadTrips(trips: ValueDatapoint<AssetStateDuration>[]) : Promise<Map<ValueDatapoint<AssetStateDuration>, ValueDatapoint<number>[]>> {

        let myMap = new Map<ValueDatapoint<AssetStateDuration>, ValueDatapoint<number>[]>();

        for (const trip of trips) {
            if(trip.y != undefined){

                const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
                    this.assetId,
                    "66",
                    {
                        type: "all",
                        fromTimestamp: moment.unix((trip.y.startTime!)/1000).subtract(1, 'second').valueOf(),
                        toTimestamp: moment.unix((trip.y.endTime!)/1000).add(1, 'second').valueOf()
                    }
                );
                if (response.status === 200 && response.data.length > 0) {
                    myMap.set(trip, response.data);
                }else{
                    myMap.set(trip, [])
                    console.log(response.request.baseURL);
                }
            }
        }
        console.log(`Trip History for ${this.assetId}:`)
        this.tripHistory!.reverse().map((i) => console.log(`
        ${moment.unix(i.y!.startTime!/1000).format("ddd, h:mm:ss")} --> \
        ${moment.unix(i.y!.endTime!/1000).diff(moment.unix(i.y!.startTime!/1000), "seconds")} sec -->\
        ${moment.unix(i.y!.endTime!/1000).format("ddd, h:mm:ss")} |
        ${myMap.get(i)!.length} points
        `))

        return Promise.resolve(myMap);
    }
}
