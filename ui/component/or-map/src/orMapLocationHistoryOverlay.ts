import {LitElement, html, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import manager, {subscribe} from "@openremote/core";
import moment from "moment";
import {OrMap} from "./index";
import {
    ValueDatapoint, GeoJSONPoint
} from '@openremote/model';
import {GeoJSONSource, MapLayerMouseEvent} from "maplibre-gl"
import "./or-map-location-history-markers"
import maplibregl from "maplibre-gl";
@customElement("or-map-location-history-overlay")
export class OrMapLocationHistoryOverlay extends subscribe(manager)(LitElement) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public map?: OrMap;

    private SourceIds: string[] = [];
    private layerIds: string[] = [];

    private history: ValueDatapoint<GeoJSONPoint>[] | undefined;

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeLayers();
    }

    async updated(changedProperties: Map<string, any>){
        // Since the element is not removed when another marker is selected,
        // but the Asset ID is changed if another asset is selected,
        // we check if the assetId has changed,
        // and if it has, remove the GeoJSON elements, and load the new Asset's layers.
        if (changedProperties.has("assetId")){
            // checks if the assetId is actually updated, because this is fired when the element is constructed.
            if(changedProperties.get("assetId") == undefined) {
                this.removeLayers();

                await this._loadData();
                this._addGeoJSONLayer();
            }else{
                this.removeLayers();
            }
        }
    }

    async connectedCallback() {
        super.connectedCallback();

        if (this.assetId == undefined || this.map == undefined) return

        await this._loadData();
        this._addGeoJSONLayer();


    }

    render(){
        if (this.assetId == undefined || this.map == undefined) return html``;
        return html`
        <or-map-location-history-markers id="${this.assetId}" .assetId="${this.assetId}" .map="${this.map}"></or-map-location-history-markers>
        `;
    }



    async _loadData(): Promise<void> {

        if(this.assetId == null || this.map == undefined) return;
        console.log("loaded")

        const now = moment();

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            this.assetId,
            "location",
            {
                type: "all",
                fromTimestamp: now.clone().startOf('day').valueOf(),
                toTimestamp: now.valueOf()
            }
        );
        if (response.status === 200 && response.data.length > 0) {
            this.history = response.data;
        }
    }
    private _addGeoJSONLayer() {
        if(this.map == undefined) return;
        if(this.assetId == undefined) return;
        if (this.history?.length == 0 || this.history == undefined) return;
        //grab the points we want to use

        const points : ValueDatapoint<GeoJSONPoint>[] = this.history!

        function getLinesBetweenPoints  (points: (ValueDatapoint<GeoJSONPoint | undefined>)[]) : ([[number, number], [number, number]][]) {
            let array : [[number, number], [number, number]][] = [];
            // points.length-1
            for (let i : number = 1; i < points.length; i++){
                array.push(([[points[i - 1].y!.coordinates![0], points[i - 1].y!.coordinates![1]], [points[i].y!.coordinates![0], points[i].y!.coordinates![1]]]))
            }
            return array
        }


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





        const PointSourceData = this.map._map!.addGeoJSONSource(PointsSource);
        const LineSourceData = this.map._map!.addGeoJSONSource(LineSource);

        this.SourceIds!.push(LineSourceData!.sourceId)
        this.SourceIds!.push(PointSourceData!.sourceId)

        const lineLayerId:string = (<string>this.map._map!.addGeoJSONLayer("MultiLineString", LineSourceData!.sourceId!))
        this.layerIds.push(lineLayerId);


        // const pointLayerId:string =(<string>this.map._map!.addGeoJSONLayer("MultiPoint", PointSourceData!.sourceId!))
        // this.layerIds.push(pointLayerId)
        // this.map!._map!._mapGl!.on('click', pointLayerId, (e: MapLayerMouseEvent) => {
        //     e.originalEvent.stopPropagation();
        //     e.preventDefault();
        //     const popup = new maplibregl.Popup()
        //         .setLngLat(e.lngLat)
        //         .setText("Location of time "+e.features![0].id)
        //         .addTo(this.map!._map!._mapGl!)
        //     console.log(e)
        // })
    }


    private removeLayers() {
        console.log("unloaded")
        this.history = [];

        this.layerIds.reverse().forEach((layerId: string) => {
            this.map?._map?.RemoveGeoJSONLayer(layerId);
        });
        this.layerIds = [];

        this.SourceIds.reverse().forEach((sourceId: string) => {
            this.map?._map?.RemoveGeoJSONSource(sourceId);
        });
        this.SourceIds = [];
    }
}
