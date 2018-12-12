import openremote, {EventCallback, OREvent} from "@openremote/core";
import {html, PolymerElement} from "@polymer/polymer";
import { FlattenedNodesObserver } from '@polymer/polymer/lib/utils/flattened-nodes-observer.js';
import {customElement, property} from '@polymer/decorators';
import rest from "@openremote/rest";
import {OrMapMarker} from "@openremote/or-map-marker";
import {Map, MapboxOptions, Style as MapboxStyle} from "mapbox-gl";
import {L} from "mapbox.js";

export enum Type {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}

/**
 * `or-map`
 * Displays a map
 *
 */
@customElement('or-map')
export class OrMap extends PolymerElement {
    protected _initCallback?: EventCallback;
    protected _map?: Map;
    protected static _mapboxGlStyle?: any;
    protected static _mapboxJsStyle?: any;
    protected _loaded: boolean = false;
    protected _observer?: FlattenedNodesObserver;

    static get template() {
        return html`
      <style>
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
      </style>
      <div id="map"></div>
      <slot id="markers-slot"></slot>
    `;
    }

    @property({type: String})
    type: Type = Type.VECTOR;

    constructor() {
        super();
    }

    _processNewMarkers(nodes) {
        nodes.forEach(function (node) {
            // node tagName should include marker to pass this check
            if(node instanceof OrMapMarker) {
                const marker = <OrMapMarker>node;
                let leafletMarket =  L.divIcon({className: 'map-marker', html: marker.html.innerHTML});
                L.marker([marker.latitude, marker.longitude], {icon:leafletMarket}).addTo(this._map);
            }
        }.bind(this));
    }

    _processRemovedMarkers(nodes) {

    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._observer.disconnect();
    }

    ready() {
        super.ready();

        if (!openremote.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent) => {
                if (initEvent === OREvent.READY) {
                    this.loadMap();
                }
            };
            openremote.addListener(this._initCallback);
        } else {
            this.loadMap();
        }
    }

    async loadMap() {

        if (this._loaded) {
            return;
        }

        if (this.shadowRoot) {
            const mapElement: HTMLElement = this.shadowRoot.getElementById('map')!;
            if (this.type === Type.RASTER) {

                if (!OrMap._mapboxJsStyle) {
                    // @ts-ignore
                    OrMap._mapboxJsStyle = await import("mapbox.js/theme/style.css");
                }

                // Add style to shadow root
                var style = document.createElement('style');
                style.id = "mapboxJsStyle";
                style.textContent = OrMap._mapboxJsStyle.default.toString();
                this.shadowRoot.appendChild(style);

                const settingsResponse = await rest.api.MapResource.getSettingsJs();
                const settings = <any> settingsResponse.data;
                const options: L.mapbox.MapOptions = {
                    zoom: <number>settings.zoom+1, // JS zoom is out by one compared to GL
                    minZoom: settings.minZoom || 0,
                    maxZoom: settings.maxZoom || 22,
                    boxZoom: settings.boxZoom || false,
                };
                if (settings.bounds) {
                    let b = settings.bounds;
                    options.maxBounds = [
                        [b[1], b[0]],
                        [b[3], b[2]],
                    ];
                }

                const m: L.mapbox.map = new L.mapbox.map(mapElement, settings, options);
                this._map = m;

            } else {
                if (!OrMap._mapboxGlStyle) {
                    // @ts-ignore
                    OrMap._mapboxGlStyle = await import("mapbox-gl/dist/mapbox-gl.css");
                }

                // Add style to shadow root
                var style = document.createElement('style');
                style.id = "mapboxGlStyle";
                style.textContent = OrMap._mapboxGlStyle.default.toString();
                this.shadowRoot.appendChild(style);

                const map: typeof import("mapbox-gl") = await import("mapbox-gl");
                const settingsResponse = await rest.api.MapResource.getSettings();
                const settings = <any> settingsResponse.data;

                const options: MapboxOptions = {
                    container: mapElement,
                    style: <MapboxStyle> settings,
                    attributionControl: true,
                    minZoom: settings.minZoom || 0,
                    maxZoom: settings.maxZoom || 0,
                    maxBounds: settings.maxBounds || null,
                    boxZoom: settings.boxZoom || false,
                    transformRequest: (url, resourceType) => {
                        return {
                            url: url,
                            headers: {'Authorization': openremote.getAuthorizationHeader()}
                        }
                    }
                };

                this._map = new map.Map(options);
            }

            // Get markers from slot
            let slotElement = this.shadowRoot.getElementById('markers-slot');
            this._observer = new FlattenedNodesObserver(slotElement, (info) => {
                this._processNewMarkers(info.addedNodes);
                this._processRemovedMarkers(info.removedNodes);
            });
        }



        this._loaded = true;
    }
}
