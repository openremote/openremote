/// <reference types="mapbox" />
/// <reference types="leaflet" />
import { IControl, LngLat, LngLatLike, Map as MapGL, Marker as MarkerGL, GeoJSONSourceSpecification } from "maplibre-gl";
import "@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css";
import { ControlPosition, ViewSettings } from "./index";
import { OrMapMarker } from "./markers/or-map-marker";
import { GeoJsonConfig, MapType } from "@openremote/model";
import { Feature, FeatureCollection } from "geojson";
export declare class MapWidget {
    protected _mapJs?: L.mapbox.Map;
    protected _mapGl?: MapGL;
    protected _type: MapType;
    protected _styleParent: Node;
    protected _mapContainer: HTMLElement;
    protected _loaded: boolean;
    protected _markersJs: Map<OrMapMarker, L.Marker>;
    protected _markersGl: Map<OrMapMarker, MarkerGL>;
    protected _geoJsonConfig?: GeoJsonConfig;
    protected _geoJsonSources: string[];
    protected _geoJsonLayers: Map<string, any>;
    protected _viewSettings?: ViewSettings;
    protected _center?: LngLatLike;
    protected _zoom?: number;
    protected _showGeoCodingControl: boolean;
    protected _showBoundaryBox: boolean;
    protected _useZoomControls: boolean;
    protected _showGeoJson: boolean;
    protected _controls?: (IControl | [IControl, ControlPosition?])[];
    protected _clickHandlers: Map<OrMapMarker, (ev: MouseEvent) => void>;
    protected _geocoder?: any;
    constructor(type: MapType, styleParent: Node, mapContainer: HTMLElement, showGeoCodingControl?: boolean, showBoundaryBox?: boolean, useZoomControls?: boolean, showGeoJson?: boolean);
    setCenter(center?: LngLatLike): this;
    flyTo(coordinates?: LngLatLike, zoom?: number): this;
    resize(): this;
    setZoom(zoom?: number): this;
    setControls(controls?: (IControl | [IControl, ControlPosition?])[]): this;
    setGeoJson(geoJsonConfig?: GeoJsonConfig): this;
    loadViewSettings(): Promise<any>;
    load(): Promise<void>;
    protected styleLoaded(): Promise<void>;
    unload(): void;
    protected _onMapClick(lngLat: LngLat, doubleClicked?: boolean): void;
    protected loadGeoJSON(geoJsonConfig?: GeoJsonConfig): Promise<void>;
    groupSourcesByGeometryType(sources: FeatureCollection): Map<string, Feature[]> | undefined;
    addGeoJSONSource(source: GeoJSONSourceSpecification): {
        source: GeoJSONSourceSpecification;
        sourceId: string;
    } | undefined;
    addGeoJSONLayer(typeString: string, sourceId: string): void;
    addMarker(marker: OrMapMarker): void;
    removeMarker(marker: OrMapMarker): void;
    onMarkerChanged(marker: OrMapMarker, prop: string): void;
    protected _updateMarkerPosition(marker: OrMapMarker): void;
    protected _updateMarkerElement(marker: OrMapMarker, doAdd: boolean): void;
    protected _removeMarkerRadius(marker: OrMapMarker): void;
    protected _createMarkerRadius(marker: OrMapMarker): void;
    createBoundaryBox(boundsArray?: string[]): void;
    protected _addMarkerClickHandler(marker: OrMapMarker, elem: HTMLElement): void;
    protected _removeMarkerClickHandler(marker: OrMapMarker, elem: HTMLElement): void;
    protected _forwardGeocode(config: any): Promise<{
        features: {
            type: string;
            geometry: {
                type: string;
                coordinates: any[];
            };
            place_name: any;
            properties: any;
            text: any;
            place_type: string[];
            center: any[];
        }[];
    }>;
    _reverseGeocode(config: {
        lat: number;
        lon: number;
    }): Promise<{
        features: {
            type: string;
            geometry: {
                type: string;
                coordinates: any[];
            };
            place_name: any;
            properties: any;
            text: any;
            place_type: string[];
            center: any[];
        }[];
    }>;
    protected _initLongPressEvent(): void;
    protected _onLongPress(lngLat: LngLat): void;
    protected _onGeocodeChange(geocode: any): void;
}
