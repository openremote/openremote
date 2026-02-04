import manager, { DefaultColor4 } from "@openremote/core";
import maplibregl, {
    AddLayerObject,
    IControl,
    GeolocateControl,
    LngLat,
    LngLatLike,
    Map as MapGL,
    MapOptions,
    MapMouseEvent,
    Marker,
    NavigationControl,
    StyleSpecification,
    GeoJSONSourceSpecification,
    MapSourceDataEvent,
} from "maplibre-gl";
import MaplibreGeocoder from "@maplibre/maplibre-gl-geocoder";
import "@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css";
import debounce from "lodash.debounce";
import {
    ControlPosition,
    OrMapClickedEvent,
    OrMapGeocoderChangeEvent,
    OrMapLoadedEvent,
    OrMapLongPressEvent,
    ViewSettings,
    OrMapMarkersChangedEvent,
    AssetWithLocation,
} from "./index";
import { OrMapMarker } from "./markers/or-map-marker";
import { getLngLat, getMarkerIconAndColorFromAssetType, isWebglSupported } from "./util";
import { Asset, GeoJsonConfig } from "@openremote/model";
import { Feature, FeatureCollection, Geometry } from "geojson";
import { isMapboxURL, transformMapboxUrl } from "./util/mapbox-url";
import { OrClusterMarker, Slice } from "./markers/or-cluster-marker";

const maplibreGlStyles = require("maplibre-gl/dist/maplibre-gl.css");
const maplibreGeoCoderStyles = require("@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css");

export interface ClusterConfig {
    cluster: boolean,
    clusterRadius: number,
    /** Until what zoom level cluster markers are shown */
    clusterMaxZoom: number
}

const metersToPixelsAtMaxZoom = (meters: number, latitude: number) => meters / 0.075 / Math.cos(latitude * Math.PI / 180);

let pkey: string | null;

export class MapWidget {
    protected _map?: MapGL;
    protected _styleParent: Node;
    protected _mapContainer: HTMLElement;
    protected _loaded = false;
    protected _markers: Map<OrMapMarker, Marker> = new Map();
    protected _geoJsonConfig?: GeoJsonConfig;
    protected _geoJsonSources: string[] = [];
    protected _geoJsonLayers: Map<string, any> = new Map();
    protected _viewSettings?: ViewSettings;
    protected _center?: LngLatLike;
    protected _zoom?: number;
    protected _showGeoCodingControl = false;
    protected _showBoundaryBox = false;
    protected _useZoomControls = true;
    protected _showGeoJson = true;
    protected _controls?: (IControl | [IControl, ControlPosition?])[];
    protected _clickHandlers: Map<OrMapMarker, (ev: MouseEvent) => void> = new Map();
    protected _geocoder?: any;
    protected _clusterConfig?: ClusterConfig;
    protected _pointsMap: any = {
        type: "FeatureCollection",
        features: []
    };

    protected _assetTypeColors: any = {};
    protected _cachedMarkers: Record<string, Marker> = {};
    protected _markersOnScreen: Record<string, Marker> = {};
    protected _assetsOnScreen: Record<string, AssetWithLocation> = {};

    constructor(styleParent: Node, mapContainer: HTMLElement, showGeoCodingControl = false, showBoundaryBox = false, useZoomControls = true, showGeoJson = true, clusterConfig?: ClusterConfig) {
        this._styleParent = styleParent;
        this._mapContainer = mapContainer;
        this._showGeoCodingControl = showGeoCodingControl;
        this._showBoundaryBox = showBoundaryBox;
        this._useZoomControls = useZoomControls;
        this._showGeoJson = showGeoJson;
        this._clusterConfig = clusterConfig;
    }

    protected _onMove = () => this._updateMarkers();
    protected _onMoveEnd = () => this._updateMarkers();
    protected _onData = (e: MapSourceDataEvent) => {
        if (this._map && e.isSourceLoaded  && e.sourceId === "mapPoints") {
            this._map.on('move', this._onMove);
            this._map.on('moveend', this._onMoveEnd);
            this._updateMarkers();
        }
    };

    public setCenter(center?: LngLatLike): this {
        this._center = getLngLat(center);
        if (this._map && this._center) {
            this._map.setCenter(this._center);
        }
        return this;
    }

    public flyTo(coordinates?:LngLatLike, zoom?: number): this {
        if (!coordinates) {
            coordinates = this._center ? this._center : this._viewSettings ? this._viewSettings.center : undefined;
        }

        if (!zoom) {
            zoom = this._zoom ? this._zoom : this._viewSettings && this._viewSettings.zoom ? this._viewSettings.zoom : undefined;
        }

        if (this._map) {
            // Only do flyTo if it has valid LngLat value
            if (coordinates) {
                this._map.flyTo({
                    center: coordinates,
                    zoom: zoom
                });
            }
        } else {
            this._center = coordinates;
            this._zoom = zoom;
        }

        return this;
    }

    public resize(): this {
        if (this._map) {
            this._map.resize()
        }
        return this;
    }

    public setZoom(zoom?: number): this {
        this._zoom = zoom;
        if (this._map && this._zoom) {
            this._map.setZoom(this._zoom);
        }
        return this;
    }

    public setControls(controls?: (IControl | [IControl, ControlPosition?])[]): this {
        this._controls = controls;
        if (this._map) {
            if (this._controls) {
                this._controls.forEach((control) => {
                    if (Array.isArray(control)) {
                        const controlAndPosition: [IControl, ControlPosition?] = control;
                        this._map!.addControl(controlAndPosition[0], controlAndPosition[1]);
                    } else {
                        this._map!.addControl(control);
                    }
                });
            } else {
                // Add zoom and rotation controls to the map
                this._map.addControl(new NavigationControl());
            }
        }
        return this;
    }

    public setGeoJson(geoJsonConfig?: GeoJsonConfig): this {
        this._geoJsonConfig = geoJsonConfig;
        if (this._map) {
            if (this._geoJsonConfig) {
                this._loadGeoJSON(this._geoJsonConfig);
            } else {
                this._loadGeoJSON(this._viewSettings?.geoJson);
            }
        }
        return this;
    }

    public async loadViewSettings() {
        const response = await manager.rest.api.MapResource.getSettings();
        const settings = response.data as any;

        if (settings.override) {
          return settings.override
        }

        // Load options for current realm or fallback to default if exist
        const realmName = manager.displayRealm || "default";
        this._viewSettings = settings.options ? settings.options[realmName] ? settings.options[realmName] : settings.options.default : null;

        if (this._viewSettings) {

            // If Map was already present, so only ran during updates such as realm switches
            if (this._map) {
                this._map.setMinZoom(this._viewSettings.minZoom);
                this._map.setMaxZoom(this._viewSettings.maxZoom);
                if (this._viewSettings.bounds){
                    this._map.setMaxBounds(this._viewSettings.bounds);
                }
                // Unload all GeoJSON that is present, and load new layers if present
                if(this._geoJsonConfig) {
                    await this._loadGeoJSON(this._geoJsonConfig);
                } else {
                    await this._loadGeoJSON(this._viewSettings?.geoJson);
                }
            }
            if (!this._center) {
                this.setCenter(this._viewSettings.center);
            } else {
                this.setCenter(this._center);
            }
        }

        return settings;
    }

    /**
     * Build the map based on the map config.
     */
    public async build(): Promise<void> {
        if (this._loaded) {
            return;
        }

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

        const options: MapOptions = {
            attributionControl: {compact: true},
            container: this._mapContainer,
            style: settings as StyleSpecification,
            transformRequest: (url, resourceType) => {
                if (!pkey) {
                    pkey = new URL(url).searchParams.get("access_token") || ''
                }
                if (isMapboxURL(url)) {
                    return transformMapboxUrl(url, pkey, resourceType)
                }
                // Cross-domain tile servers usually have the following headers specified "access-control-allow-methods	GET", "access-control-allow-origin *", "allow GET,HEAD". The "Access-Control-Request-Headers: Authorization" may not be set e.g. with Mapbox tile servers. The CORS preflight request (OPTION) will in this case fail if the "authorization" header is being requested cross-domain. The only headers allowed are so called "simple request" headers, see https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#simple_requests.
                const headers = new URL(window.origin).hostname === new URL(url).hostname 
                    ? {Authorization: manager.getAuthorizationHeader()} : {}
                return {
                    headers,
                    url
                };
            }
        };

        if (this._viewSettings) {
            if (this._useZoomControls){
                options.maxZoom = this._viewSettings.maxZoom;
                options.minZoom = this._viewSettings.minZoom;
            }
            if (this._viewSettings.bounds && !this._showBoundaryBox){
                options.maxBounds = this._viewSettings.bounds;
            }

            options.boxZoom = this._viewSettings.boxZoom;
            options.zoom = this._viewSettings.zoom;
            options.center = this._viewSettings.center;
        }

        this._center = this._center || (this._viewSettings ? this._viewSettings.center : undefined);
        options.center = this._center;

        if (this._zoom) {
            options.zoom = this._zoom;
        }

        // Firefox headless mode does not support webgl, see https://bugzilla.mozilla.org/show_bug.cgi?id=1375585
        if (!isWebglSupported()) {
          console.warn("WebGL is not supported in this environment. The map cannot be initialized.");
          return;
        }

        this._map = new map.Map(options);

        await this._styleLoaded();

        this._map.on("click", (e: MapMouseEvent) => {
            this._onMapClick(e.lngLat);
        });

        this._map.on("dblclick", (e: MapMouseEvent) => {
            this._onMapClick(e.lngLat, true);
        });

        if (this._showGeoCodingControl && this._viewSettings && this._viewSettings.geocodeUrl) {
            this._geocoder = new MaplibreGeocoder({forwardGeocode: this._forwardGeocode.bind(this), reverseGeocode: this._reverseGeocode }, { maplibregl, showResultsWhileTyping: true });
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
            this._map!.addControl(this._geocoder, 'top-left');

            // There's no callback parameter in the options of the MaplibreGeocoder,
            // so this is how we get the selected result.
            this._geocoder._inputEl.addEventListener("change", () => {
                var selected = this._geocoder._typeahead.selected;
                this._onGeocodeChange(selected);
            });
        }

        // Add custom controls
        if (this._controls) {
            this._controls.forEach((control) => {
                if (Array.isArray(control)) {
                    const controlAndPosition: [IControl, ControlPosition?] = control;
                    this._map!.addControl(controlAndPosition[0], controlAndPosition[1]);
                } else {
                    this._map!.addControl(control);
                }
            });
        } else {
            // Add zoom and rotation controls to the map
            this._map.addControl(new NavigationControl());
            // Add current location controls to the map
            this._map.addControl(new GeolocateControl({
                positionOptions: {
                    enableHighAccuracy: true
                },
                showAccuracyCircle: true,
                showUserLocation: true
            }));
        }

        // Unload all GeoJSON that is present, and load new layers if present
        if(this._geoJsonConfig) {
            await this._loadGeoJSON(this._geoJsonConfig);
        } else {
            await this._loadGeoJSON(this._viewSettings?.geoJson);
        }

        this._initLongPressEvent();
        this._map.on("load", async () => await this.load());

        this._mapContainer.dispatchEvent(new OrMapLoadedEvent());
        this._loaded = true;
        this.createBoundaryBox()
    }

    protected _styleLoaded(): Promise<void> {
        return new Promise(resolve => {
            if (this._map) {
                this._map.once('style.load', resolve);
            }
        });
    }

    /**
     * Load map sources, layers and events
     */
    public async load() {
        if (!this._map || !this._loaded) {
            console.warn("MapLibre Map not initialized!");
            return;
        }

        if (this._map.getSource('mapPoints')) {
            if (this._map.getLayer('unclustered-point')) {
                this._map.removeLayer('unclustered-point');
            }
            if (this._map.getLayer('clusters')) {
                this._map.removeLayer('clusters');
            }
            if (this._map.getLayer('cluster-count')) {
                this._map.removeLayer('cluster-count');
            }
            this._map.removeSource('mapPoints');
        }

        this._map.addSource('mapPoints', {
            'type': 'geojson',
            'cluster': this._clusterConfig?.cluster ?? true,
            'clusterRadius': this._clusterConfig?.clusterRadius ?? 180,
            'clusterMaxZoom': this._clusterConfig?.clusterMaxZoom ?? 17,
            'data': this._pointsMap,
            'clusterProperties': Object.fromEntries(Object.keys(this._assetTypeColors).map(t => [t,["+", ["case", ["==", ["get", "assetType"], t], 1, 0]]]))
        });

        if (!this._map.getLayer('unclustered-point')) {
            this._map.addLayer({
                id: 'unclustered-point',
                type: 'circle',
                source: 'mapPoints',
                filter: ['!', ['has', 'point_count']],
                paint: { 'circle-radius': 0 }
            });
        }

        this._map.on("data", this._onData);
    }

    // Clean up of internal resources associated with the map.
    // Normally used during disconnectedCallback
    public unload() {
        if (this._map) {
            this._map.remove();
            this._map = undefined;
        }
    }

    protected _onMapClick(lngLat: LngLat, doubleClicked: boolean = false) {
        this._mapContainer.dispatchEvent(new OrMapClickedEvent(lngLat, doubleClicked));
    }

    protected async _loadGeoJSON(geoJsonConfig?: GeoJsonConfig) {

        // Remove old layers
        if(this._geoJsonLayers.size > 0) {
            this._geoJsonLayers.forEach((layer, layerId) => this._map!.removeLayer(layerId));
            this._geoJsonLayers = new Map();
        }
        // Remove old sources
        if(this._geoJsonSources.length > 0) {
            this._geoJsonSources.forEach((sourceId) => this._map!.removeSource(sourceId));
            this._geoJsonSources = [];
        }

        // Add new ones if present
        if (this._showGeoJson && geoJsonConfig) {

            // If array of features (most of the GeoJSONs use this)
            if (geoJsonConfig.source.type == "FeatureCollection") {
                const groupedSources = this.groupSourcesByGeometryType(geoJsonConfig.source);
                groupedSources?.forEach((features, type) => {
                    const newSource = {
                        type: "geojson",
                        data: {
                            type: "FeatureCollection",
                            features: features
                        }
                    } as any as GeoJSONSourceSpecification;
                    const sourceInfo = this.addGeoJSONSource(newSource);
                    if(sourceInfo) {
                        this.addGeoJSONLayer(type, sourceInfo.sourceId);
                    }
                })

                // Or only 1 feature is added
            } else if (geoJsonConfig.source.type == "Feature") {
                const sourceInfo = this.addGeoJSONSource(geoJsonConfig.source);
                if(sourceInfo) {
                    this.addGeoJSONLayer(sourceInfo.source.type, sourceInfo.sourceId);
                }
            } else {
                console.error("Could not create layer since source type is neither 'FeatureCollection' nor 'Feature'.")
            }
        }
    }

    public groupSourcesByGeometryType(sources: FeatureCollection): Map<string, Feature[]> | undefined {
        const groupedSources: Map<string, Feature[]> = new Map();
        sources.features.forEach((feature) => {
            const sources = groupedSources.get(feature.geometry.type) ?? [];
            sources.push(feature);
            groupedSources.set(feature.geometry.type, sources);
        })
        return groupedSources;
    }

    public addGeoJSONSource(source: GeoJSONSourceSpecification): { source: GeoJSONSourceSpecification, sourceId: string } | undefined {
        if (!this._map) {
            console.error("mapGl instance not found!"); return;
        }
        const id = Date.now() + "-" + (this._geoJsonSources.length + 1);
        this._map.addSource(id, source)
        this._geoJsonSources.push(id);
        return {
            source: source,
            sourceId: id
        }
    }

    public addGeoJSONLayer(typeString: string, sourceId: string) {
        if (!this._map) {
            console.error("mapGl instance not found!"); return;
        }

        const type = typeString as "Point" | "MultiPoint" | "LineString" | "MultiLineString" | "Polygon" | "MultiPolygon" | "GeometryCollection"
        const layerId = sourceId + "-" + type;

        // If layer is not added yet
        if( this._geoJsonLayers.get(layerId) == undefined) {

            // Get realm color by getting value from CSS
            let realmColor: string = getComputedStyle(this._mapContainer).getPropertyValue('--or-app-color4');
            if(realmColor == undefined || realmColor.length == 0) {
                realmColor = DefaultColor4;
            }

            let layer = {
                id: layerId,
                source: sourceId
            } as any;

            // Set styling based on type
            switch (type) {
                case "Point":
                case "MultiPoint": {
                    layer.type = "circle";
                    layer.paint = {
                        'circle-radius': 12,
                        'circle-color': realmColor
                    }
                    this._geoJsonLayers.set(layerId, layer);
                    this._map.addLayer(layer);
                    break;
                }
                case "LineString":
                case "MultiLineString": {
                    layer.type = "line";
                    layer.paint = {
                        'line-color': realmColor,
                        'line-width': 4
                    };
                    this._geoJsonLayers.set(layerId, layer);
                    this._map.addLayer(layer);
                    break;
                }
                case "Polygon":
                case "MultiPolygon": {
                    layer.type = "fill";
                    layer.paint = {
                        'fill-color': realmColor,
                        'fill-opacity': 0.3
                    };
                    this._geoJsonLayers.set(layerId, layer);
                    this._map.addLayer(layer);

                    // Add extra layer with outline
                    const outlineId = layerId + "-outline";
                    const outlineLayer = {
                        id: outlineId,
                        source: sourceId,
                        type: "line",
                        paint: {
                            'line-color': realmColor,
                            'line-width': 2
                        },
                    } as AddLayerObject
                    this._geoJsonLayers.set(outlineId, outlineLayer);
                    this._map.addLayer(outlineLayer);
                    break;
                }
                case "GeometryCollection": {
                    console.error("GeometryCollection GeoJSON is not implemented yet!");
                    return;
                }
            }
        }
    }

    public addMarker(marker: OrMapMarker) {
        if (marker.hasPosition()) {
            this._updateMarkerElement(marker, true);
        }
    }

    protected _updateMarkers() {
        if (!this._map) return;

        const newMarkers: Record<string, Marker> = {};
        const features = this._map.querySourceFeatures('mapPoints');

        // Asset markers
        for (const feature of features) {
            if (!feature.properties.id) continue;
            const id: string = feature.properties.id;
            const geometry = feature.geometry as Geometry & { coordinates: LngLatLike };
            const coords = geometry.coordinates;

            let marker = this._cachedMarkers[id]
            if (!marker) { 
                const placeholder = document.createElement("div");
                marker = this._cachedMarkers[id] = new Marker({ element: placeholder }).setLngLat(coords);
            }
            newMarkers[id] = marker;

            if (!this._markersOnScreen[id]) {
                marker.addTo(this._map);
                this._assetsOnScreen[id] = JSON.parse(feature.properties.asset);
            };
        }

        // Cluster markers
        for (const feature of features) {
            if (!feature.properties.cluster) continue;
            const id: number = feature.properties.cluster_id;
            const geometry = feature.geometry as Geometry & { coordinates: [number, number] };
            const [lng, lat] = geometry.coordinates;

            let marker = this._cachedMarkers[id];
            if (!marker) {
                const slices: Slice[] = Object.entries(feature.properties)
                    .filter(([k]) => this._assetTypeColors.hasOwnProperty(k))
                    .map(([type, count]) => [type, this._assetTypeColors[type], count]);

                marker = this._cachedMarkers[id] = new Marker({
                    element: new OrClusterMarker(slices, id, lng, lat, this._map),
                }).setLngLat([lng, lat]);
            }
            newMarkers[id] = marker;

            if (!this._markersOnScreen[id]) marker.addTo(this._map);
        }

        for (const id in this._markersOnScreen) {
            const marker = newMarkers[id];
            if (!marker
              || marker._element instanceof OrClusterMarker && !marker._element.hasTypes(Object.keys(this._assetTypeColors))
            ) {
                this._markersOnScreen[id].remove();
                delete this._assetsOnScreen[id];
            }
        }
        this._markersOnScreen = newMarkers;
        this._mapContainer.dispatchEvent(new OrMapMarkersChangedEvent(Object.values(this._assetsOnScreen)));
    }

    public addAssetMarker(assetId: string, assetName: string, assetType: string, long: number, lat: number, asset: Asset) {
        this._assetTypeColors[assetType] = getMarkerIconAndColorFromAssetType(assetType)?.color;
        this._pointsMap.features.push({
            type: 'Feature',
            properties: {
                name: assetName,
                id: assetId,
                assetType: assetType,
                asset: asset
            },
            geometry: {
                type: "Point",
                coordinates: [ long, lat ]
            }
        });
    }

    public cleanUpAssetMarkers(): void {
        this._assetTypeColors = {};
        this._pointsMap = {
            type: "FeatureCollection",
            features: []
        };
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
                if (marker.hasPosition()) {
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
        const mGl: Marker | undefined = this._markers.get(marker);
        mGl?.setLngLat([marker.lng!, marker.lat!]);
        this._createMarkerRadius(marker);
    }

    protected _updateMarkerElement(marker: OrMapMarker, doAdd: boolean) {
        let mGl = this._markers.get(marker);
        if (mGl) {
            marker._actualMarkerElement = undefined;
            this._removeMarkerClickHandler(marker, mGl.getElement());
            mGl.remove();
            this._markers.delete(marker);
        }

        if (doAdd) {
            const elem = marker._createMarkerElement();

            if (elem) {
                mGl = new Marker({
                    element: elem,
                    anchor: "top-left"
                })
                    .setLngLat([marker.lng!, marker.lat!])
                    .addTo(this._map!);

                this._markers.set(marker, mGl);

                marker._actualMarkerElement = mGl.getElement() as HTMLDivElement;

                if (marker.interactive) {
                    this._addMarkerClickHandler(marker, mGl.getElement());
                }
            }
            if (marker.radius) {
                this._createMarkerRadius(marker);
            }
        }
    }

    protected _removeMarkerRadius(marker:OrMapMarker){
        if (this._map && this._loaded && marker.radius && marker.lat && marker.lng) {
            if (this._map.getSource('circleData')) {
                this._map.removeLayer('marker-radius-circle');
                this._map.removeSource('circleData');
            }
        }
    }

    protected _createMarkerRadius(marker:OrMapMarker){
        if (this._map && this._loaded && marker.radius && marker.lat && marker.lng){

            this._removeMarkerRadius(marker);

            this._map.addSource('circleData', {
                type: 'geojson',
                data: {
                    type: 'FeatureCollection',
                    features: [{
                        type: "Feature",
                        geometry: {
                            "type": "Point",
                            "coordinates": [marker.lng, marker.lat]
                        },
                        properties: {}
                    }]
                }
            });

            this._map.addLayer({
                "id": "marker-radius-circle",
                "type": "circle",
                "source": "circleData",
                "paint": {
                    "circle-radius": [
                        "interpolate",
                        ["exponential", 2],
                        ["zoom"],
                        0, 0, 
                        20, metersToPixelsAtMaxZoom(marker.radius, marker.lat)
                    ],
                    "circle-color": "red",
                    "circle-opacity": 0.3,
                    "circle-pitch-alignment": "map" // Keeps the circle flat against the map surface
                }
            });
        }
    }

    public createBoundaryBox(boundsArray: string[] = []){
        if (this._map && this._loaded && this._showBoundaryBox && this._viewSettings?.bounds) {

            if (this._map.getSource('bounds')) {
                this._map.removeLayer('bounds');
                this._map.removeSource('bounds');
            }

            if (boundsArray.length !== 4){
                boundsArray = this._viewSettings?.bounds.toString().split(",")
            }
            var req = [
                [
                    [boundsArray[0], boundsArray[3]],
                    [boundsArray[2], boundsArray[3]],
                    [boundsArray[2], boundsArray[1]],
                    [boundsArray[0], boundsArray[1]],
                    [boundsArray[0], boundsArray[3]]
                ]
            ]
            this._map.fitBounds([
                parseFloat(boundsArray[0]) + .01,
                parseFloat(boundsArray[1]) - .01,
                parseFloat(boundsArray[2]) - .01,
                parseFloat(boundsArray[3]) + .01,
            ])
            this._map.addSource('bounds', {
                'type': 'geojson',
                'data': {
                    'type': 'Feature',
                    'properties': {},
                    'geometry': {
                        'type': 'Polygon',
                        // @ts-ignore
                        'coordinates': req
                    }
                }
            });

            this._map.addLayer({
                'id': 'bounds',
                'type': 'fill',
                'source': 'bounds',
                'paint': {
                    'fill-color': '#FF0000',
                    'fill-opacity': .4
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
            let request =  this._viewSettings!.geocodeUrl + '/search?q=' + config.query + '&format=geojson&polygon_geojson=1&addressdetails=1';
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

    public async _reverseGeocode(config: {lat: number, lon:number}) {
            const features = [];
            try {
                let request =  this._viewSettings!.geocodeUrl + '/reverse?lat=' + config.lat + '&lon='+config.lon+'&format=geojson&polygon_geojson=1&addressdetails=1';
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
                console.error(`Failed to reverseGeocode with error: ${e}`);
            }

            return {
                features: features
            };
        }

    protected _initLongPressEvent() {
        if (this._map) {
            let pressTimeout: NodeJS.Timeout | null; 
            let pos: LngLat;
            let clearTimeoutFunc = () => { if (pressTimeout) clearTimeout(pressTimeout); pressTimeout = null; };

            this._map.on('touchstart', (e) => {
                if (e.originalEvent.touches.length > 1) {
                    return;
                }
                pos = e.lngLat;
                pressTimeout = setTimeout(() => {
                    this._onLongPress(pos!);
                }, 500);
            });

            this._map.on('mousedown', (e) => {
                if (!pressTimeout) {
                    pos = e.lngLat;
                    pressTimeout = setTimeout(() => {
                        this._onLongPress(pos!);
                        pressTimeout = null;
                    }, 500);
                }
            });
           
            this._map.on('dragstart', clearTimeoutFunc);
            this._map.on('mouseup', clearTimeoutFunc);
            this._map.on('touchend', clearTimeoutFunc);
            this._map.on('touchcancel', clearTimeoutFunc);
            this._map.on('touchmove', clearTimeoutFunc);
            this._map.on('moveend', clearTimeoutFunc);
            this._map.on('gesturestart', clearTimeoutFunc);
            this._map.on('gesturechange', clearTimeoutFunc);
            this._map.on('gestureend', clearTimeoutFunc);
        }
    };

    protected _onLongPress(lngLat: LngLat) {
        this._mapContainer.dispatchEvent(new OrMapLongPressEvent(lngLat));
    }

    protected _onGeocodeChange(geocode:any) {
        this._mapContainer.dispatchEvent(new OrMapGeocoderChangeEvent(geocode));
    }
}
