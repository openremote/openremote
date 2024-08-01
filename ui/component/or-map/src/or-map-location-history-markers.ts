import {LitElement, css, PropertyValues, CSSResultGroup} from "lit";
import {customElement, property} from "lit/decorators.js";
import manager, {subscribe, Util} from "@openremote/core";
import moment from "moment";
import {
    ValueDatapoint,
    GeoJSONPoint,
    SharedEvent,
    AttributeEvent,
    AssetEvent,
    AssetEventCause,
    Asset,
    AssetModelUtil
} from '@openremote/model';
import maplibregl, { MapLayerMouseEvent } from "maplibre-gl";
import type {OrMap} from "./index";
import {OrMapMarker, OrMapMarkerClickedEvent} from "./markers/or-map-marker";
import {OrMapLocationHistoryOverlay} from "./index";
@customElement("or-map-location-history-markers")
export class OrMapLocationHistoryMarkers extends OrMapMarker {


    public static NAME : string = "or-map-location-history-markers";
    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public map?: OrMap;

    private markers: maplibregl.Marker[] = [];

    private history: ValueDatapoint<GeoJSONPoint>[] | undefined;

    disconnectedCallback() {
        super.disconnectedCallback();

        for (const marker of this.markers) {
            marker.remove();
        }
    }

    async updated(changedProperties: Map<string, any>){
        // Since the element is not removed when another marker is selected,
        // but the Asset ID is changed if another asset is selected,
        // we check if the assetId has changed,
        // and if it has, remove the GeoJSON elements, and load the new Asset's layers.
        if (changedProperties.has("assetId")){
            // checks if the assetId is actually updated, because this is fired when the element is constructed.
            if(changedProperties.get("assetId") != undefined) {

            }
        }
    }

    async connectedCallback() {
        super.connectedCallback();
        console.log("created markers")
        console.log(this.assetId)
        await this._loadData();

        if(this.history == undefined) return;
        this.history!.slice(1, -1)
        for (const historyElement of this.history) {

            const el = document.createElement('historymapmarker')
            // el.style.position = "absolute"
            // el.style.zIndex = "2";
            // el.id = ""+this.assetId+historyElement.x;
            // el.className = "historyMarker"

            const time: string = moment(historyElement.x).toString();
            const popup = new maplibregl.Popup().setText(
                'Construction on the Washington Monument began in 1848.'
            );


            const marker = new maplibregl.Marker()
                .setLngLat([historyElement!.y!.coordinates![0], historyElement!.y!.coordinates![1]])
                .setPopup(popup)
                .addTo(this.map!._map!._mapGl!);

            (marker.getElement() as HTMLDivElement).addEventListener('click', (e: Event) => {
                // e.originalEvent.stopPropagation();
                e.preventDefault();
                popup.addTo(this.map!._map!._mapGl!);
                console.log(e)
            })

            this.markers.push(marker);
            console.log("added \n" + historyElement.y)
        }
    }

    async _loadData(): Promise<void> {
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
}
