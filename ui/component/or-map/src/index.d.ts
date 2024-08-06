import { EventCallback } from "@openremote/core";
import { FlattenedNodesObserver } from "@polymer/polymer/lib/utils/flattened-nodes-observer.js";
import { CSSResult, LitElement, PropertyValues } from "lit";
import { IControl, LngLat, LngLatBoundsLike, LngLatLike, Map as MapGL } from "maplibre-gl";
import { MapWidget } from "./mapwidget";
import "./markers/or-map-marker";
import "./markers/or-map-marker-asset";
import { OrMapMarker, OrMapMarkerChangedEvent } from "./markers/or-map-marker";
import * as Util from "./util";
import { OrMwcInput, ValueInputProviderGenerator } from "@openremote/or-mwc-components/or-mwc-input";
import { GeoJsonConfig, MapType } from "@openremote/model";
export { Util, LngLatLike };
export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";
export { IControl } from "maplibre-gl";
export * from "./or-map-asset-card";
export interface ViewSettings {
    center: LngLatLike;
    bounds?: LngLatBoundsLike | null;
    zoom: number;
    maxZoom: number;
    minZoom: number;
    boxZoom: boolean;
    geocodeUrl: String;
    geoJson?: GeoJsonConfig;
}
export interface MapEventDetail {
    lngLat: LngLat;
    doubleClick: boolean;
}
export interface MapGeocoderEventDetail {
    geocode: any;
}
export declare class OrMapLoadedEvent extends CustomEvent<void> {
    static readonly NAME = "or-map-loaded";
    constructor();
}
export declare class OrMapClickedEvent extends CustomEvent<MapEventDetail> {
    static readonly NAME = "or-map-clicked";
    constructor(lngLat: LngLat, doubleClick?: boolean);
}
export declare class OrMapLongPressEvent extends CustomEvent<MapEventDetail> {
    static readonly NAME = "or-map-long-press";
    constructor(lngLat: LngLat);
}
export declare class OrMapGeocoderChangeEvent extends CustomEvent<MapGeocoderEventDetail> {
    static readonly NAME = "or-map-geocoder-change";
    constructor(geocode: any);
}
declare global {
    export interface HTMLElementEventMap {
        [OrMapClickedEvent.NAME]: OrMapClickedEvent;
        [OrMapLoadedEvent.NAME]: OrMapLoadedEvent;
        [OrMapLongPressEvent.NAME]: OrMapLongPressEvent;
        [OrMapGeocoderChangeEvent.NAME]: OrMapGeocoderChangeEvent;
    }
}
export type ControlPosition = "top-right" | "top-left" | "bottom-right" | "bottom-left";
export declare class CenterControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    pos?: LngLatLike;
    onAdd(map: MapGL): HTMLElement;
    onRemove(map: MapGL): void;
}
export declare class CoordinatesControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    protected input: OrMwcInput;
    protected _readonly: boolean;
    protected _value: any;
    protected _valueChangedHandler: (value: LngLat | undefined) => void;
    constructor(disabled: boolean | undefined, valueChangedHandler: (value: LngLat | undefined) => void);
    onAdd(map: MapGL): HTMLElement;
    onRemove(map: MapGL): void;
    set readonly(readonly: boolean);
    set value(value: any);
}
export declare const geoJsonPointInputTemplateProvider: ValueInputProviderGenerator;
export declare class OrMap extends LitElement {
    static styles: CSSResult;
    type: MapType;
    protected _markerStyles: string[];
    center?: LngLatLike;
    zoom?: number;
    showGeoCodingControl: boolean;
    showBoundaryBoxControl: boolean;
    useZoomControl: boolean;
    geoJson?: GeoJsonConfig;
    showGeoJson: boolean;
    boundary: string[];
    controls?: (IControl | [IControl, ControlPosition?])[];
    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean;
    protected _observer?: FlattenedNodesObserver;
    protected _markers: OrMapMarker[];
    protected _resizeObserver?: ResizeObserver;
    protected _mapContainer?: HTMLElement;
    protected _slotElement?: HTMLSlotElement;
    constructor();
    protected firstUpdated(_changedProperties: PropertyValues): void;
    get markers(): OrMapMarker[];
    connectedCallback(): void;
    disconnectedCallback(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    protected updated(changedProperties: PropertyValues): void;
    refresh(): void;
    loadMap(): void;
    resize(): void;
    flyTo(coordinates?: LngLatLike, zoom?: number): void;
    protected _onMarkerChangedEvent(evt: OrMapMarkerChangedEvent): void;
    protected _processNewMarkers(nodes: Element[]): void;
    protected _processRemovedMarkers(nodes: Element[]): void;
}
