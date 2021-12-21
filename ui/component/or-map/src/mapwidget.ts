import manager, {MapType} from "@openremote/core";
import {LngLatLike, Map as MapGL, MapboxOptions as OptionsGL, Marker as MarkerGL, Style as StyleGL, LngLat,
    MapMouseEvent,
    NavigationControl,
    GeolocateControl,
    Control,
    IControl} from "maplibre-gl";
import MaplibreGeocoder from "@maplibre/maplibre-gl-geocoder";
import '@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css';
import {debounce} from "lodash";
import {ControlPosition, OrMapClickedEvent, OrMapLoadedEvent, ViewSettings} from "./index";
import {
    OrMapMarker
} from "./markers/or-map-marker";
import {getLatLngBounds, getLngLat} from "./util";
const mapboxJsStyles = require("mapbox.js/dist/mapbox.css");
const maplibreGlStyles = require("maplibre-gl/dist/maplibre-gl.css");
const maplibreGeoCoderStyles = require("@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css");

// TODO: fix any type
const metersToPixelsAtMaxZoom = (meters:number, latitude:number) =>
    meters / 0.075 / Math.cos(latitude * Math.PI / 180);


export class MapWidget {
    protected _mapJs?: L.mapbox.Map;
    protected _mapGl?: MapGL;
    protected _type: MapType;
    protected _styleParent: Node;
    protected _mapContainer: HTMLElement;
    protected _loaded: boolean = false;
    protected _markersJs: Map<OrMapMarker, L.Marker> = new Map();
    protected _markersGl: Map<OrMapMarker, MarkerGL> = new Map();
    protected _viewSettings?: ViewSettings;
    protected _center?: LngLatLike;
    protected _zoom?: number;
    protected _showGeoCodingControl: boolean = false;
    protected _controls?: (Control | IControl | [Control | IControl, ControlPosition?])[];
    protected _clickHandlers: Map<OrMapMarker, (ev: MouseEvent) => void> = new Map();
    protected _geocoder?: any;

    constructor(type: MapType, showGeoCodingControl: boolean, styleParent: Node, mapContainer: HTMLElement) {
        this._type = type;
        this._styleParent = styleParent;
        this._mapContainer = mapContainer;
        this._showGeoCodingControl = showGeoCodingControl;
    }

    public setCenter(center?: LngLatLike): this {

        this._center = getLngLat(center);

        switch (this._type) {
            case MapType.RASTER:
                if (this._mapJs) {
                    const latLng = getLngLat(this._center) || (this._viewSettings ? getLngLat(this._viewSettings.center) : undefined);
                    if (latLng) {
                        this._mapJs.setView(latLng, undefined, {pan: {animate: false}, zoom: {animate: false}});
                    }
                }
                break;
            case MapType.VECTOR:
                if (this._mapGl && this._center) {
                    this._mapGl.setCenter(this._center);
                }
                break;
        }

        return this;
    }

    public flyTo(coordinates?:LngLatLike, zoom?: number): this {
        switch (this._type) {
            case MapType.RASTER:
                if (this._mapJs) {
                    // TODO implement fylTo
                }
                break;
            case MapType.VECTOR:
                if (!coordinates) {
                    coordinates = this._center ? this._center : this._viewSettings ? this._viewSettings.center : undefined;
                }

                if (!zoom) {
                    zoom = this._zoom ? this._zoom : this._viewSettings && this._viewSettings.zoom ? this._viewSettings.zoom : undefined;
                }

                if (this._mapGl) {
                    // Only do flyTo if it has valid LngLat value
                    if(coordinates) {
                        this._mapGl.flyTo({
                            center: coordinates,
                            zoom: zoom
                        });
                    }
                } else {
                    this._center = coordinates;
                    this._zoom = zoom;
                }
                break;
        }

        return this;
    }

    public resize(): this {

        switch (this._type) {
            case MapType.RASTER:
                if (this._mapJs) {
                }
                break;
            case MapType.VECTOR:
                if (this._mapGl) {
                    this._mapGl.resize()
                }
                break;
        }

        return this;
    }

    public setZoom(zoom?: number): this {

        this._zoom = zoom;

        switch (this._type) {
            case MapType.RASTER:
                if (this._mapJs && this._zoom) {
                    this._mapJs.setZoom(this._zoom, {animate: false});
                }
                break;
            case MapType.VECTOR:
                if (this._mapGl && this._zoom) {
                    this._mapGl.setZoom(this._zoom);
                }
                break;
        }

        return this;
    }

    public setControls(controls?: (Control | IControl | [Control | IControl, ControlPosition?])[]): this {
        this._controls = controls;
        if (this._mapGl) {
            if (this._controls) {
                this._controls.forEach((control) => {
                    if (Array.isArray(control)) {
                        const controlAndPosition: [Control | IControl, ControlPosition?] = control;
                        this._mapGl!.addControl(controlAndPosition[0], controlAndPosition[1]);
                    } else {
                        this._mapGl!.addControl(control);
                    }
                });
            } else {
                // Add zoom and rotation controls to the map
                this._mapGl.addControl(new NavigationControl());
            }
        }
        return this;
    }

    public async loadViewSettings() {

        let settingsResponse;
        if (this._type === MapType.RASTER) {
            settingsResponse = await manager.rest.api.MapResource.getSettingsJs();
        } else {
            settingsResponse = await manager.rest.api.MapResource.getSettings();
        }
        const settings = settingsResponse.data as any;

        // Load options for current realm or fallback to default if exist
        const realmName = manager.displayRealm || "default";
        this._viewSettings = settings.options ? settings.options[realmName] ? settings.options[realmName] : settings.options.default : null;

        if (this._viewSettings) {
            if (this._mapGl) {
                this._mapGl.setMinZoom(this._viewSettings.minZoom);
                this._mapGl.setMaxZoom(this._viewSettings.maxZoom);
                this._mapGl.setMaxBounds(this._viewSettings.bounds);
            }
            this.setCenter(this._viewSettings.center);
        }

        return settings;
    }

    public async load(): Promise<void> {
        if (this._loaded) {
            return;
        }

        if (this._type === MapType.RASTER) {

            // Add style to shadow root
            const style = document.createElement("style");
            style.id = "mapboxJsStyle";
            style.textContent = mapboxJsStyles;
            this._styleParent.appendChild(style);
            const settings = await this.loadViewSettings();

            let options: L.mapbox.MapOptions | undefined;
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
                    const lngLat = getLngLat(this._viewSettings.center);
                    options.center = lngLat ? L.latLng(lngLat.lat, lngLat.lng) : undefined;
                }
                if (this._center) {
                    const lngLat = getLngLat(this._center);
                    options.center = lngLat ? L.latLng(lngLat.lat, lngLat.lng) : undefined;
                }
                if (this._zoom) {
                    options.zoom = this._zoom + 1;
                }
            }

            this._mapJs = L.mapbox.map(this._mapContainer, settings, options);

            this._mapJs.on("click", (e: any)=> {
                this._onMapClick(e.latlng);
            });

            if (options && options.maxBounds) {
                const minZoom = this._mapJs.getBoundsZoom(options.maxBounds, true);
                if (!options.minZoom || options.minZoom < minZoom) {
                    (this._mapJs as any).setMinZoom(minZoom);
                }
            }
        } else {
            // Add style to shadow root
            let style = document.createElement("style");
            style.id = "maplibreGlStyle";
            style.textContent = maplibreGlStyles;
            this._styleParent.appendChild(style);

            style = document.createElement("style");
            style.id = "maplibreGeoCoderStyles";
            style.textContent = maplibreGeoCoderStyles;
            this._styleParent.appendChild(style);

            const map: typeof import("maplibre-gl") = await import(/* webpackChunkName: "maplibre-gl" */ "maplibre-gl");
            const settings = await this.loadViewSettings();
                
            const options: OptionsGL = {
                attributionControl: true,
                container: this._mapContainer,
                style: settings as StyleGL,
                transformRequest: (url, resourceType) => {
                    return {
                        headers: {Authorization: manager.getAuthorizationHeader()},
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

            this._center = this._center || (this._viewSettings ? this._viewSettings.center : undefined);
            options.center = this._center;

            if (this._zoom) {
                options.zoom = this._zoom;
            }

            this._mapGl = new map.Map(options);

            await this.styleLoaded();

            this._mapGl.on("click", (e: MapMouseEvent) => {
                this._onMapClick(e.lngLat);
            });

            this._mapGl.on("dblclick", (e: MapMouseEvent) => {
                this._onMapClick(e.lngLat, true);
            });

            // Add custom controls
            if (this._controls) {
                this._controls.forEach((control) => {
                    if (Array.isArray(control)) {
                        const controlAndPosition: [Control | IControl, ControlPosition?] = control;
                        this._mapGl!.addControl(controlAndPosition[0], controlAndPosition[1]);
                    } else {
                        this._mapGl!.addControl(control);
                    }
                });
            } else {
                // Add zoom and rotation controls to the map
                this._mapGl.addControl(new NavigationControl());
                // Add current location controls to the map
                this._mapGl.addControl(new GeolocateControl({
                    positionOptions: {
                        enableHighAccuracy: true
                    },
                    showAccuracyCircle: true,
                    showUserLocation: true
                }));
            }

            if (this._showGeoCodingControl && this._viewSettings && this._viewSettings.geoCodeUrl) {
                this._geocoder = new MaplibreGeocoder({forwardGeocode: this._forwardGeocode.bind(this), reverseGeocode: this._reverseGeocode }, { marker: false, showResultsWhileTyping: true });
                // Override the _onKeyDown function from MaplibreGeocoder which has a bug getting the value from the input element
                this._geocoder._onKeyDown = debounce((e: KeyboardEvent) => {
                    var ESC_KEY_CODE = 27,
                    TAB_KEY_CODE = 9;
              
                  if (e.keyCode === ESC_KEY_CODE && this._geocoder.options.clearAndBlurOnEsc) {
                    this._geocoder._clear(e);
                    return this._geocoder._inputEl.blur();
                  }
              
                  // if target has shadowRoot, then get the actual active element inside the shadowRoot
                  var value = this._geocoder._inputEl.value || e.key;
              
                  if (!value) {
                    this._geocoder.fresh = true;
                    // the user has removed all the text
                    if (e.keyCode !== TAB_KEY_CODE) this._geocoder.clear(e);
                    return (this._geocoder._clearEl.style.display = "none");
                  }
              
                  // TAB, ESC, LEFT, RIGHT, UP, DOWN
                  if (
                    e.metaKey ||
                    [TAB_KEY_CODE, ESC_KEY_CODE, 37, 39, 38, 40].indexOf(e.keyCode) !== -1
                  )
                    return;
              
                  // ENTER
                  if (e.keyCode === 13) {
                    if (!this._geocoder.options.showResultsWhileTyping) {
                      if (!this._geocoder._typeahead.list.selectingListItem)
                      this._geocoder._geocode(value);
                    } else {
                      if (this._geocoder.options.showResultMarkers) {
                        this._geocoder._fitBoundsForMarkers();
                      }
                      this._geocoder._inputEl.value = this._geocoder._typeahead.query;
                      this._geocoder.lastSelected = null;
                      this._geocoder._typeahead.selected = null;
                      return;
                    }
                  }
              
                  if (
                    value.length >= this._geocoder.options.minLength &&
                    this._geocoder.options.showResultsWhileTyping
                  ) {
                    this._geocoder._geocode(value);
                  }
                }, 300);
                this._mapGl!.addControl(this._geocoder);

                // There's no callback parameter in the options of the MaplibreGeocoder,
                // so this is how we get the selected result.
                this._geocoder._inputEl.addEventListener("change", () => {
                    var selected = this._geocoder._typeahead.selected;
                    if (selected) {
                        // Set marker by calling _onMapClick and doubleClicked set to true
                        this._onMapClick(selected.center, true);
                    }
                });
            }
        }

        this._mapContainer.dispatchEvent(new OrMapLoadedEvent());
        this._loaded = true;
    }

    protected styleLoaded(): Promise<void> {
        return new Promise(resolve => {
            if (this._mapGl) {

                this._mapGl.once('style.load', () => {
                    resolve();
                });
            }
        });
    }

    protected _onMapClick(lngLat: LngLat, doubleClicked: boolean = false) {
        this._mapContainer.dispatchEvent(new OrMapClickedEvent(lngLat, doubleClicked));
    }

    public addMarker(marker: OrMapMarker) {
        if (marker.lat && marker.lng) {
            this._updateMarkerElement(marker, true);
        }
    }

    public removeMarker(marker: OrMapMarker) {
        this._removeMarkerRadius(marker);
        this._updateMarkerElement(marker, false);
    }

    public onMarkerChanged(marker: OrMapMarker, prop: string) {
        if (!this._loaded) {
            return;
        }

        switch (prop) {
            case "lat":
            case "lng":
            case "radius":
                if (marker.lat && marker.lng) {
                    if (marker._actualMarkerElement) {
                        this._updateMarkerPosition(marker);
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
            case MapType.RASTER:
                const m: L.Marker | undefined = this._markersJs.get(marker);
                if (m) {
                    m.setLatLng([marker.lat!, marker.lng!]);
                }
                break;
            case MapType.VECTOR:
                const mGl: MarkerGL | undefined = this._markersGl.get(marker);
                if (mGl) {
                    mGl.setLngLat([marker.lng!, marker.lat!]);
                }
                break;
        }
        this._createMarkerRadius(marker);
    }

    protected _updateMarkerElement(marker: OrMapMarker, doAdd: boolean) {

        switch (this._type) {
            case MapType.RASTER:
                let m = this._markersJs.get(marker);
                if (m) {
                    this._removeMarkerClickHandler(marker, marker.markerContainer as HTMLElement);
                    marker._actualMarkerElement = undefined;
                    (m as any).removeFrom(this._mapJs!);
                    this._markersJs.delete(marker);
                }

                if (doAdd) {
                    const elem = marker._createMarkerElement();
                    if (elem) {
                        const icon = L.divIcon({html: elem.outerHTML, className: "or-marker-raster"});
                        m = L.marker([marker.lat!, marker.lng!], {icon: icon, clickable: marker.interactive});
                        m.addTo(this._mapJs!);
                        marker._actualMarkerElement = (m as any).getElement() ? (m as any).getElement().firstElementChild as HTMLDivElement : undefined;
                        if (marker.interactive) {
                            this._addMarkerClickHandler(marker, marker.markerContainer as HTMLElement);
                        }

                        this._markersJs.set(marker, m);
                    }
                    if(marker.radius) {
                        this._createMarkerRadius(marker);
                    }
                }

                break;
            case MapType.VECTOR:
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
                    if(marker.radius) {
                        this._createMarkerRadius(marker);
                    }
                }


                break;
        }
    }

    protected _removeMarkerRadius(marker:OrMapMarker){

        if(this._mapGl && this._loaded && marker.radius && marker.lat && marker.lng) {

            if (this._mapGl.getSource('circleData')) {
                this._mapGl.removeLayer('marker-radius-circle');
                this._mapGl.removeSource('circleData');
            }
        }

    }

    protected _createMarkerRadius(marker:OrMapMarker){
        if(this._mapGl && this._loaded && marker.radius && marker.lat && marker.lng){

              this._removeMarkerRadius(marker);

                this._mapGl.addSource('circleData', {
                    type: 'geojson',
                    data: {
                        type: 'FeatureCollection',
                        features: [{
                            type: "Feature",
                            geometry: {
                                "type": "Point",
                                "coordinates": [marker.lng, marker.lat]
                            },
                            properties: {
                                "title": "You Found Me",
                            }
                        }]
                    }
                });

                this._mapGl.addLayer({
                    "id": "marker-radius-circle",
                    "type": "circle",
                    "source": "circleData",
                    "paint": {
                        "circle-radius": {
                            stops: [
                                [0, 0],
                                [20, metersToPixelsAtMaxZoom(marker.radius, marker.lat)]
                            ],
                            base: 2
                        },
                        "circle-color": "red",
                        "circle-opacity": 0.3
                    }
                });
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

    protected async _forwardGeocode(config: any) {
        const features = [];
        try {
            let request =  this._viewSettings!.geoCodeUrl + '/search?q=' + config.query + '&format=geojson&polygon_geojson=1&addressdetails=1';
            const response = await fetch(request);
            const geojson = await response.json();
            for (let feature of geojson.features) {
                let center = [feature.bbox[0] + (feature.bbox[2] - feature.bbox[0]) / 2, feature.bbox[1] + (feature.bbox[3] - feature.bbox[1]) / 2 ];
                let point = {
                    type: 'Feature',
                    geometry: {
                        type: 'Point',
                        coordinates: center
                    },
                    place_name: feature.properties.display_name,
                    properties: feature.properties,
                    text: feature.properties.display_name,
                    place_type: ['place'],
                    center: center
                };
                features.push(point);
            }
        } catch (e) {
            console.error(`Failed to forwardGeocode with error: ${e}`);
        }

        return {
            features: features
        };
    }

    protected async _reverseGeocode(config: any) {

    }
}
