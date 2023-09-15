import {css, CSSResultGroup, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import manager, {DefaultBoxShadow, subscribe, Util} from "@openremote/core";
import moment from "moment";
import {
    MapAssetCardConfig,
    MapMarkerAssetConfig, OrMap,
    OrMapMarker,
    OrMapMarkerChangedEvent,
    OrMapMarkerEventDetail
} from "./index";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AttributeEvent,
    SharedEvent,
    AssetDatapointQuery, ValueDatapoint, GeoJSONPoint
} from '@openremote/model';
import {MapWidget} from "./mapwidget";
import {GeoJSONSource} from "maplibre-gl"
@customElement("or-map-location-history-overlay")
export class OrMapLocationHistoryOverlay extends subscribe(manager)(LitElement) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public map?: OrMap;

    // @property({type: Object, attribute: true})
    public asset?: Asset;

    private SourceIds: string[] = [];
    private layerIds: string[] = [];

    private history: ValueDatapoint<GeoJSONPoint>[] | undefined;

    disconnectedCallback() {
        super.disconnectedCallback();

        this.layerIds!.reverse().forEach((layerId: string) => {
            this.map?._map?.RemoveGeoJSONLayer(layerId);
        });

        this.SourceIds.reverse().forEach((sourceId: string) => {
           this.map?._map?.RemoveGeoJSONSource(sourceId);
        });
    }

    async connectedCallback() {
        super.connectedCallback();

        await this._loadData();
        this._addGeoJSONLayer();
    }

    // updated(changedProperties: Map<string, any>) {
    //     // If assetId is undefined, remove GeoJSON
    //     //TODO: If history has changed, append the added point to the most recent points
    //         if(changedProperties.has("assetId")){
    //         if(this.assetId == undefined){
    //             for (const sourceIdsKey in this.SourceIds!) {
    //                 this.map?._map!.RemoveGeoJSONSource(sourceIdsKey);
    //             }
    //         }
    //     }
    // }



    async _loadData(): Promise<void> {

        const now = moment();

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.assetId,
            "location",
            {
                type: "all",
                fromTimestamp: now.clone().subtract(1, 'day').startOf('day').valueOf(),
                toTimestamp: now.valueOf()
            }
        );
        if (response.status === 200 && response.data.length > 0) {
            this.history = response.data;
        }
    }
    private _addGeoJSONLayer() {
        if(this.map == undefined) return;

        //grab the points we want to use

        const points : ValueDatapoint<GeoJSONPoint>[] = this.history!.slice(this.history!.length - 101, this.history!.length-1)

        function getLinesBetweenPoints  (points: ValueDatapoint<GeoJSONPoint | undefined>[]) : ([[number, number], [number, number]][]) {
            let array : [[number, number], [number, number]][] = [];
            // points.length-1
            for (let i : number = 1; i < 10; i++) {
                array.push([[points[i - 1].y!.coordinates![0], points[i - 1].y!.coordinates![1]], [points[i].y!.coordinates![0], points[i].y!.coordinates![1]]])
                // array.push(([points[i - 1].y!.coordinates![0], points[i].y!.coordinates![1]]))
            }
            return array
        }
        // function route (points: ValueDatapoint<GeoJSONPoint | undefined>[]):
        //     { type: string,
        //       properties:any,
        //       geometry:
        //           {
        //             type:string,
        //             coordinates: [GeoJSONPoint, GeoJSONPoint]
        //           }
        //     }[] {
        //
        //   let routeToBeReturned : { type: string, properties:any, geometry: {type:string, coordinates: [number, number]} }[] = [];
        //   for (let i : number = 1; i < points.length-1; i++) {
        //     routeToBeReturned.push(
        //         { type:"Feature",
        //           properties: {},
        //           geometry: {
        //             type:"LineString",
        //             coordinates: getLineCoordinates
        //           }
        //         }
        //   )
        //   }
        //
        //   return routeToBeReturned;
        // }

        const LineSource = {
            type: "geojson",
            data: {
                type: "FeatureCollection",
                features:[
                    { type:"Feature",
                        properties: {},
                        geometry: {
                            type:"MultiLineString",
                            coordinates: getLinesBetweenPoints(points)
                        }
                    }
                ]
            }
        } as any as GeoJSONSource;
        const PointsSource = {
            type: "geojson",
            data: {
                type: "FeatureCollection",
                features: points.map((point: ValueDatapoint<GeoJSONPoint>) => {
                    return {
                        "type": "Feature",
                        "properties": {},
                        "geometry": point.y!
                    }
                })
            }
        } as any as GeoJSONSource;
        // const RouteSource = {
        //     type: "geojson",
        //     data: {
        //         type: "FeatureCollection",
        //         features:[
        //             { type:"Point",
        //                 properties: {},
        //                 geometry: {
        //                     type:"LineString",
        //                     coordinates: getLinesBetweenPoints(points)
        //                 }
        //             }
        //         ]
        //     }
        // } as any as GeoJSONSource;




        const PointSourceData = this.map._map!.addGeoJSONSource(PointsSource);
        const LineSourceData = this.map._map!.addGeoJSONSource(LineSource);

        this.SourceIds!.push(PointSourceData!.sourceId, LineSourceData!.sourceId)

        this.layerIds.push(<string>this.map._map!.addGeoJSONLayer("MultiPoint", PointSourceData!.sourceId!))
        this.layerIds.push(<string>this.map._map!.addGeoJSONLayer("MultiLineString", LineSourceData!.sourceId!))


        // if(data) {
        //   this.map!._map!._mapGl!.addLayer({
        //     'id': 'test',
        //     'type': 'symbol',
        //     'source': data.sourceId,
        //     'layout': {
        //       'icon-image': 'custom-marker',
        //       // get the year from the source's "year" property
        //       'text-field': ['get', 'year'],
        //       'text-font': [
        //         'Open Sans Semibold',
        //         'Arial Unicode MS Bold'
        //       ],
        //       'text-offset': [0, 1.25],
        //       'text-anchor': 'top'
        //     }
        //   });
        // }


    }
    // protected async render() {
    //     const x = await this._loadData();
    //     const y = this._addGeoJSONLayer();
    // }


}
