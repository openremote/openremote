import openremote, {EventCallback, OREvent} from "@openremote/core";
import {FlattenedNodesObserver} from "@polymer/polymer/lib/utils/flattened-nodes-observer.js";
import {css, customElement, html, LitElement, property, query, PropertyValues, CSSResult} from "lit-element";
import {LngLat, LngLatBoundsLike, LngLatLike} from "mapbox-gl";
import {MapWidget} from "./mapwidget";
import {
    OrMapMarker, OrMapMarkerEvent, OrMapMarkerChangedEvent, MarkerStyle
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

    public static styles = [
        MarkerStyle,
        css`
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
    `];

    @property({type: String})
    public type: Type = Type.VECTOR;

    protected _markerStyles: string[] = [];

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
    protected _markers: OrMapMarker[] = [];

    @query("#map")
    protected _mapContainer?: HTMLElement;

    @query("slot")
    protected _slotElement?: HTMLSlotElement;

    constructor() {
        super();
        this.addEventListener(OrMapMarkerEvent.CHANGED, this._onMarkerChangedEvent);
    }

    public get markers(): OrMapMarker[] {
        return this._markers;
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

                this._markers.push(node);

                // Add styles of marker class to the shadow root if not already added
                const className = node.constructor.name;
                if (this._markerStyles.indexOf(className) < 0) {
                    const styles = (node.constructor as any).styles;
                    let stylesArr: CSSResult[] = [];

                    if (styles) {
                        if (!Array.isArray(styles)) {
                            stylesArr.push(styles as CSSResult);
                        } else {
                            stylesArr = styles as CSSResult[];
                        }

                        stylesArr.forEach((styleItem) => {
                            const styleElem = document.createElement("style");
                            styleElem.textContent = styleItem.toString();
                            if (this._mapContainer!.children.length > 0) {
                                this._mapContainer!.insertBefore(styleElem, this._mapContainer!.children[0]);
                            } else {
                                this._mapContainer!.appendChild(styleElem);
                            }
                        })
                    }

                    this._markerStyles.push(className);
                }

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
                const i = this._markers.indexOf(node);
                if (i >= 0) {
                    this._markers.splice(i, 1);
                }
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
