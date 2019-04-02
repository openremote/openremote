import openremote, {EventCallback, OREvent} from "@openremote/core";
import {FlattenedNodesObserver} from "@polymer/polymer/lib/utils/flattened-nodes-observer.js";
import {css, customElement, html, LitElement, property, query, PropertyValues} from "lit-element";
import {LngLat, LngLatBoundsLike, LngLatLike} from "mapbox-gl";
import {MapWidget} from "./mapwidget";
import {
    OrMapMarker, OrMapMarkerEvent, OrMapMarkerChangedEvent
} from "./markers/or-map-marker";
import {getLngLat} from "./util";

export enum Type {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}

export interface ViewSettings {
    "center": LngLatLike;
    "bounds": LngLatBoundsLike;
    "zoom": number;
    "maxZoom": number;
    "minZoom": number;
    "boxZoom": boolean;
}

@customElement("or-map")
export class OrMap extends LitElement {

    public static styles = css`
        :host {
            display: block;
            overflow: hidden;
        }
        #map {            
            position: relative;
            width: 100%;
            height: 100%;
        }
        slot {
            display: none;
        }
        
        :host([hidden]) {
            display: none;
        }
        
        .leaflet-marker-icon, .mapboxgl-marker {
            pointer-events: none !important;
        }

        /* Offset margin set by leaflet-marker-icon */        
        .leaflet-marker-icon .or-map-marker {
            margin-left: 6px;
            margin-top: 6px;
        }
        
        .or-map-marker {
            position: absolute; /* This makes mapboxJS behave like mapboxGL */
            cursor: grab;
        }
        
        .or-map-marker.interactive {
            cursor: pointer;
        }
        
        .or-map-marker>*{
            pointer-events: visible;
        }
        
        .or-map-marker-default {
            position: relative;
            transform: var(--or-map-marker-transform, translate(-24px, -45px));
            --or-icon-width: var(--or-map-marker-width, 48px);
            --or-icon-height: var(--or-map-marker-height, 48px);
            --or-icon-fill-color: var(--or-map-marker-fill, #1D5632);            
        }
        
        .or-map-marker-default .marker-icon {
            position: absolute;
            left: 50%;
            top: 0px;
            --or-icon-fill-color: var(--or-map-marker-icon-fill, #FFF);
            --or-icon-width: var(--or-map-marker-icon-width, 24px);
            --or-icon-height: var(--or-map-marker-icon-height, 24px);
            transform: var(--or-map-marker-icon-transform, translate(-50%, 5px));            
        }
    `;

    @property({type: String})
    public type: Type = Type.VECTOR;

    @property({type: String, converter: {
            fromAttribute(value: string | null, type?: String): LngLatLike | undefined {
                if (!value) {
                    return;
                }

                const coords = value.split(",");
                if (coords.length !== 2) {
                    return;
                }
                const lng = Number(coords[0]);
                const lat = Number(coords[1]);
                return new LngLat(lng, lat);
            },

            toAttribute(value?: LngLatLike, type?: String): string {
                const lngLat = getLngLat(value);

                if (!lngLat) {
                    return "";
                }

                return "" + lngLat.lng + "," + lngLat.lat;
            }
        }})
    public center?: LngLatLike;

    @property({type: Number})
    public zoom?: number;

    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean = false;
    protected _observer?: FlattenedNodesObserver;

    @query("#map")
    protected _mapContainer?: HTMLElement;

    @query("slot")
    protected _slotElement?: HTMLSlotElement;

    constructor() {
        super();
        this.addEventListener(OrMapMarkerEvent.CHANGED, this._onMarkerChangedEvent);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this._observer!.disconnect();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {

        if (!openremote.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent) => {
                if (initEvent === OREvent.READY) {
                    this.loadMap();

                    openremote.removeListener(this._initCallback!);
                }
            };
            openremote.addListener(this._initCallback);
        } else {
            this.loadMap();
        }
    }

    public loadMap() {

        if (this._loaded) {
            return;
        }

        if (this._mapContainer && this._slotElement) {

            this._map = new MapWidget(this.type, this.shadowRoot!, this._mapContainer)
                .setCenter(this.center)
                .setZoom(this.zoom);
            this._map.load().then(() => {
                // Get markers from slot
                this._observer = new FlattenedNodesObserver(this._slotElement!, (info: any) => {
                    this._processNewMarkers(info.addedNodes);
                    this._processRemovedMarkers(info.removedNodes);
                });
            });
        }

        this._loaded = true;
    }

    protected _onMarkerChangedEvent(evt: OrMapMarkerChangedEvent) {
        if (this._map) {
            this._map.onMarkerChanged(evt.detail.marker, evt.detail.property);
        }
    }

    protected _processNewMarkers(nodes: Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if (node instanceof OrMapMarker) {
                this._map.addMarker(node);
            }
        });
    }

    protected _processRemovedMarkers(nodes: Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if (node instanceof OrMapMarker) {
                this._map.removeMarker(node);
            }
        });
    }

    protected render() {
        return html`
          <div id="map"></div>
          <slot></slot>
        `;
    }
}
