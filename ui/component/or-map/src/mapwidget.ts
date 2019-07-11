import openremote from "@openremote/core";
import rest from "@openremote/rest";
import {LngLatLike, Map as MapGL, MapboxOptions as OptionsGL, Marker as MarkerGL, Style as StyleGL, LngLat, MapEventType,
    MapMouseEvent} from "mapbox-gl";
import L, {Map as MapJS, MapOptions as OptionsJS, Marker as MarkerJS} from "mapbox.js";
import {Type, ViewSettings} from "./index";
import {OrMapMarker, OrMapMarkerChangedEvent, OrMapMarkerClickedEvent} from "./markers/or-map-marker";
import {getLatLng, getLatLngBounds, getLngLat, getLngLatBounds} from "./util";

// TODO were to place this
// fix any type
const metersToPixelsAtMaxZoom = (meters:number, latitude:number) =>
    meters / 0.075 / Math.cos(latitude * Math.PI / 180);

export class MapWidget {
    protected static _mapboxGlStyle?: any;
    protected static _mapboxJsStyle?: any;
    protected _mapJs?: MapJS;
    protected _mapGl?: MapGL;
    protected _type: Type;
    protected _styleParent: Node;
    protected _mapContainer: HTMLElement;
    protected _loaded: boolean = false;
    protected _markersJs: Map<OrMapMarker, MarkerJS> = new Map();
    protected _markersGl: Map<OrMapMarker, MarkerGL> = new Map();
    protected _viewSettings?: ViewSettings;
    protected _center?: LngLat;
    protected _zoom?: number;
    protected _clickHandlers: Map<OrMapMarker, (ev: MouseEvent) => void> = new Map();

    constructor(type: Type, styleParent: Node, mapContainer: HTMLElement) {
        this._type = type;
        this._styleParent = styleParent;
        this._mapContainer = mapContainer;
    }

    public setCenter(center?: LngLatLike): this {

        this._center = getLngLat(center);

        switch (this._type) {
            case Type.RASTER:
                if (this._mapJs) {
                    const latLng = getLatLng(this._center) || (this._viewSettings ? getLatLng(this._viewSettings.center) : undefined);
                    if (latLng) {
                        this._mapJs.setView(latLng, undefined, {pan: {animate: false}, zoom: {animate: false}});
                    }
                }
                break;
            case Type.VECTOR:
                if (this._mapGl && this._center) {
                    this._mapGl.setCenter(this._center);
                }
                break;
        }

        return this;
    }

    public flyTo(LngLat:LngLatLike): this {
        switch (this._type) {
            case Type.RASTER:
                if (this._mapJs) {
                    //TODO implement fylTo
                }
                break;
            case Type.VECTOR:
                if (this._mapGl && LngLat) {
                    this._mapGl.flyTo({
                        center: LngLat,
                        zoom: 15
                    });
                }
                break;
        }

        return this;
    }

    public setZoom(zoom?: number): this {

        this._zoom = zoom;

        switch (this._type) {
            case Type.RASTER:
                if (this._mapJs && this._zoom) {
                    this._mapJs.setZoom(this._zoom, {animate: false});
                }
                break;
            case Type.VECTOR:
                if (this._mapGl && this._zoom) {
                    this._mapGl.setZoom(this._zoom);
                }
                break;
        }

        return this;
    }

    public async load(): Promise<void> {
        if (this._loaded) {
            return;
        }

        if (this._type === Type.RASTER) {

            if (!MapWidget._mapboxJsStyle) {
                // @ts-ignore
                MapWidget._mapboxJsStyle = await import(/* webpackChunkName: "mapbox-js-css" */ "mapbox.js/dist/mapbox.css");
            }

            // Add style to shadow root
            const style = document.createElement("style");
            style.id = "mapboxJsStyle";
            style.textContent = MapWidget._mapboxJsStyle.default.toString();
            this._styleParent.appendChild(style);
            const settingsResponse = await rest.api.MapResource.getSettingsJs();
            const settings = settingsResponse.data as any;

            // Load options for current realm or fallback to default if exist
            this._viewSettings = settings.options ? settings.options[openremote.getRealm() || "default"] ? settings.options[openremote.getRealm() || "default"] : settings.options.default : null;
            let options: OptionsJS | undefined;
            if (this._viewSettings) {
                options = {};

                // JS zoom is out compared to GL
                options.zoom = this._viewSettings.zoom ? this._viewSettings.zoom + 1 : undefined;
                options.maxZoom = this._viewSettings.maxZoom ? this._viewSettings.maxZoom - 1 : undefined;
                options.minZoom = this._viewSettings.minZoom ? this._viewSettings.minZoom + 1 : undefined;
                options.boxZoom = this._viewSettings.boxZoom;

                // JS uses lat then lng unlike GL
                if (this._viewSettings.bounds) {
                    options.maxBounds = getLatLngBounds(this._viewSettings.bounds);
                }
                if (this._viewSettings.center) {
                    options.center = getLatLng(this._viewSettings.center);
                }
                if (this._center) {
                    options.center = getLatLng(this._center);
                }
            }

            this._mapJs = L.mapbox.map(this._mapContainer, settings, options);

            if (options && options.maxBounds) {
                const minZoom = this._mapJs.getBoundsZoom(options.maxBounds, true);
                if (!options.minZoom || options.minZoom < minZoom) {
                    this._mapJs.setMinZoom(minZoom);
                }
            }

        } else {
            if (!MapWidget._mapboxGlStyle) {
                // @ts-ignore
                MapWidget._mapboxGlStyle = await import(/* webpackChunkName: "mapbox-gl-css" */ "mapbox-gl/dist/mapbox-gl.css");
            }

            // Add style to shadow root
            const style = document.createElement("style");
            style.id = "mapboxGlStyle";
            style.textContent = MapWidget._mapboxGlStyle.default.toString();
            this._styleParent.appendChild(style);

            const map: typeof import("mapbox-gl") = await import(/* webpackChunkName: "mapbox-gl" */ "mapbox-gl");
            const settingsResponse = await rest.api.MapResource.getSettings();
            const settings = settingsResponse.data as any;

            // Load options for current realm or fallback to default if exist
            this._viewSettings = settings.options ? settings.options[openremote.getRealm() || "default"] ? settings.options[openremote.getRealm() || "default"] : settings.options.default : null;
            const options: OptionsGL = {
                attributionControl: true,
                container: this._mapContainer,
                style: settings as StyleGL,
                transformRequest: (url, resourceType) => {
                    return {
                        headers: {Authorization: openremote.getAuthorizationHeader()},
                        url
                    };
                }
            };

            if (this._viewSettings) {
                options.minZoom = this._viewSettings.minZoom;
                options.maxZoom = this._viewSettings.maxZoom;
                options.maxBounds = this._viewSettings.bounds;
                options.boxZoom = this._viewSettings.boxZoom;
                options.zoom = this._viewSettings.zoom;
                options.center = this._viewSettings.center;
            }
            if (this._center) {
                options.center = this._center;
            }

            this._mapGl = new map.Map(options);
        }

        this._loaded = true;
    }

    public addMarker(marker: OrMapMarker) {
        if (marker.lat && marker.lng) {
            this._updateMarkerElement(marker, true);
        }
    }

    public removeMarker(marker: OrMapMarker) {
        this._updateMarkerElement(marker, false);
    }

    public onMarkerChanged(marker: OrMapMarker, prop: string) {
        if (!this._loaded) {
            return;
        }

        switch (prop) {
            case "lat":
            case "lng":
                if (marker.lat && marker.lng) {
                    if (marker._actualMarkerElement) {
                        this._updateMarkerPosition(marker);
                        this._createMarkerRadius(marker);
                    } else {
                        this._updateMarkerElement(marker, true);

                    }
                } else if (marker._actualMarkerElement) {
                    this._updateMarkerElement(marker, false);

                }
                break;
        }
    }

    protected _updateMarkerPosition(marker: OrMapMarker) {
        switch (this._type) {
            case Type.RASTER:
                const m: MarkerJS | undefined = this._markersJs.get(marker);
                if (m) {
                    m.setLatLng([marker.lat!, marker.lng!]);
                }
                break;
            case Type.VECTOR:
                const mGl: MarkerGL | undefined = this._markersGl.get(marker);
                if (mGl) {
                    mGl.setLngLat([marker.lng!, marker.lat!]);
                }
                break;
        }
    }

    protected _updateMarkerElement(marker: OrMapMarker, doAdd: boolean) {

        switch (this._type) {
            case Type.RASTER:
                let m = this._markersJs.get(marker);
                if (m) {
                    this._removeMarkerClickHandler(marker, marker.markerContainer as HTMLElement);
                    marker._actualMarkerElement = undefined;
                    m.removeFrom(this._mapJs!);
                    this._markersJs.delete(marker);
                }

                if (doAdd) {
                    const elem = marker._createMarkerElement();
                    if (elem) {
                        const icon = L.divIcon({html: elem.outerHTML, className: "or-marker-raster"});
                        m = L.marker([marker.lat!, marker.lng!], {icon: icon, clickable: marker.interactive});
                        m.addTo(this._mapJs!);
                        marker._actualMarkerElement = m.getElement() ? m.getElement().firstElementChild as HTMLDivElement : undefined;
                        if (marker.interactive) {
                            this._addMarkerClickHandler(marker, marker.markerContainer as HTMLElement);
                        }

                        this._markersJs.set(marker, m);
                    }
                }
                break;
            case Type.VECTOR:
                let mGl = this._markersGl.get(marker);
                if (mGl) {
                    marker._actualMarkerElement = undefined;
                    this._removeMarkerClickHandler(marker, mGl.getElement());
                    mGl.remove();
                    this._markersGl.delete(marker);
                }

                if (doAdd) {
                    const elem = marker._createMarkerElement();

                    if (elem) {
                        mGl = new MarkerGL({
                            element: elem,
                            anchor: "top-left"
                        })
                            .setLngLat([marker.lng!, marker.lat!])
                            .addTo(this._mapGl!);

                        this._markersGl.set(marker, mGl);

                        marker._actualMarkerElement = mGl.getElement() as HTMLDivElement;

                        if (marker.interactive) {
                            this._addMarkerClickHandler(marker, mGl.getElement());
                        }

                    }
                }
                break;
        }
    }

    protected _createMarkerRadius(marker:OrMapMarker){
        if(this._mapGl && marker.radius && marker.lat && marker.lng){
            // this._mapGl.addSource('circleData', {
            //     type: 'geojson',
            //     data: {
            //         type: 'FeatureCollection',
            //         features: [{
            //             type: "Feature",
            //             geometry: {
            //                 "type": "Point",
            //                 "coordinates": [marker.lng, marker.lat]
            //             },
            //             properties: {
            //                 "title": "You Found Me",
            //             }
            //         }]
            //     }
            // });
            //
            // this._mapGl.addLayer({
            //     "id":"marker-radius-circle",
            //     "type": "circle",
            //     "source": "circleData",
            //     "paint": {
            //         "circle-radius": {
            //             stops: [
            //                 [5, 1],
            //                 [15, 1024]
            //             ],
            //             base: 2
            //         },
            //         "circle-color": "red",
            //         "circle-opacity": 0.6
            //     }
            // });
        }
    }

    protected _addMarkerClickHandler(marker: OrMapMarker, elem: HTMLElement) {
        if (elem) {
            const handler = (ev: MouseEvent) => {
                ev.stopPropagation();
                marker._onClick(ev);
            };
            this._clickHandlers.set(marker, handler);
            elem.addEventListener("click", handler);
        }
    }

    protected _removeMarkerClickHandler(marker: OrMapMarker, elem: HTMLElement) {
        const handler = this._clickHandlers.get(marker);
        if (handler && elem) {
            elem.removeEventListener("click", handler);
            this._clickHandlers.delete(marker);
        }
    }
}
