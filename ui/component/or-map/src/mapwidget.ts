import L, {MapOptions, Marker} from "mapbox.js";
import {Type} from "./index";
import rest from "@openremote/rest";
import openremote from "@openremote/core";
import {Map as MapGL, MapboxOptions, Style as MapboxStyle, Marker as MarkerGL} from "mapbox-gl";
import {OrMapMarker} from "./markers/or-map-marker";

export class MapWidget {
    protected _mapJs?: L.mapbox.map;
    protected _mapGl?: MapGL;
    protected static _mapboxGlStyle?: any;
    protected static _mapboxJsStyle?: any;
    protected _type: Type;
    protected _styleParent: Node;
    protected _mapContainer: HTMLElement;
    protected _loaded: boolean = false;
    protected _markersJs?: Map<OrMapMarker, Marker>;
    protected _markersGl?: Map<OrMapMarker, MarkerGL>;

    constructor(type: Type, styleParent: Node, mapContainer: HTMLElement) {
        this._type = type;
        this._styleParent = styleParent;
        this._mapContainer = mapContainer;

        switch(type) {
            case Type.RASTER:
                this._markersJs = new Map<OrMapMarker, Marker>();
                break;
            case Type.VECTOR:
                this._markersGl = new Map<OrMapMarker, MarkerGL>();
                break;
        }
    }

    async load(): Promise<void> {
        if (this._loaded) {
            return;
        }

        this._loaded = true;

        if (this._type === Type.RASTER) {

            if (!MapWidget._mapboxJsStyle) {
                // @ts-ignore
                MapWidget._mapboxJsStyle = await import("mapbox.js/dist/mapbox.css");
            }

            // Add style to shadow root
            var style = document.createElement('style');
            style.id = "mapboxJsStyle";
            style.textContent = MapWidget._mapboxJsStyle.default.toString();
            this._styleParent.appendChild(style);
            const settingsResponse = await rest.api.MapResource.getSettingsJs();
            const settings = <any> settingsResponse.data;

            // Load options for current realm or fallback to default if exist
            let settingOptions : any = settings.options ? settings.options[openremote.getRealm() || "default"] ? settings.options[openremote.getRealm() || "default"] : settings.options.default : null;
            let options: MapOptions | undefined;
            if (settingOptions) {
                options = {};

                // JS zoom is out by one compared to GL
                options.zoom = settingOptions.zoom ? settingOptions.zoom + 1 : undefined;
                options.maxZoom = settingOptions.maxZoom;
                options.minZoom = settingOptions.minZoom;
                options.boxZoom = settingOptions.boxZoom;

                // JS uses lat then lng unlike GL
                if (settingOptions.bounds) {
                    let b = settingOptions.bounds;
                    options.maxBounds = [
                        [b[1], b[0]],
                        [b[3], b[2]],
                    ];
                }
                if (settingOptions.center) {
                    let c = settingOptions.center;
                    options.center = [
                        c[1], c[0]
                    ];
                }
            }

            this._mapJs = new L.mapbox.map(this._mapContainer, settings, options);

            if (options && options.maxBounds) {
                const minZoom = this._mapJs.getBoundsZoom(options.maxBounds, true);
                if (!options.minZoom || options.minZoom < minZoom) {
                    this._mapJs.setMinZoom(minZoom);
                }
            }

        } else {
            if (!MapWidget._mapboxGlStyle) {
                // @ts-ignore
                MapWidget._mapboxGlStyle = await import("mapbox-gl/dist/mapbox-gl.css");
            }

            // Add style to shadow root
            const style = document.createElement('style');
            style.id = "mapboxGlStyle";
            style.textContent = MapWidget._mapboxGlStyle.default.toString();
            this._styleParent.appendChild(style);

            const map: typeof import("mapbox-gl") = await import("mapbox-gl");
            const settingsResponse = await rest.api.MapResource.getSettings();
            const settings = <any> settingsResponse.data;

            // Load options for current realm or fallback to default if exist
            let settingOptions : any = settings.options ? settings.options[openremote.getRealm() || "default"] ? settings.options[openremote.getRealm() || "default"] : settings.options.default : null;
            let options: MapboxOptions = {
                container: this._mapContainer,
                style: <MapboxStyle> settings,
                attributionControl: true,
                transformRequest: (url, resourceType) => {
                    return {
                        url: url,
                        headers: {'Authorization': openremote.getAuthorizationHeader()}
                    }
                }
            };

            if (settingOptions) {
                options.minZoom = settingOptions.minZoom;
                options.maxZoom = settingOptions.maxZoom;
                options.maxBounds = settingOptions.bounds;
                options.boxZoom = settingOptions.boxZoom;
                options.zoom = settingOptions.zoom;
                options.center = settingOptions.center;
            }

            this._mapGl = new map.Map(options);
        }
    }

    addMarker(marker: OrMapMarker) {
        if (marker._ele) {
            switch (this._type) {
                case Type.RASTER:
                    let icon = L.divIcon({className: 'map-marker', html: marker._ele.outerHTML});
                    let m: Marker = L.marker([marker.lat, marker.lng], {icon: icon});

                    m.addTo(this._mapJs!);

                    if(marker._onClick) {
                        m.on('click', marker._onClick);
                    }

                    this._markersJs!.set(marker, m);
                    break;
                case Type.VECTOR:
                    new MarkerGL(marker._ele)
                        .setLngLat([marker.lng, marker.lat])
                        .addTo(this._mapGl!);
                    break;
            }
        }
    }

    removeMarker(marker: OrMapMarker) {
        switch (this._type) {
            case Type.RASTER:
                let m: Marker | undefined = this._markersJs!.get(marker);
                if (m) {
                    this._markersJs!.delete(marker);
                    m.removeFrom(this._mapJs!);
                }
                break;
            case Type.VECTOR:
                break;
        }
    }

    updateMarkerPosition(marker: OrMapMarker) {
        switch (this._type) {
            case Type.RASTER:
                let m: Marker | undefined = this._markersJs!.get(marker);
                if (m) {
                    m.setLatLng([marker.lat, marker.lng]);
                }
                break;
            case Type.VECTOR:
                break;
        }
    }
}