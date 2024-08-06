/// <reference types="leaflet" />
import { LngLatBounds, LngLatBoundsLike, LngLatLike } from "maplibre-gl";
import { Asset, AssetDescriptor, GeoJSONPoint, ValueHolder } from "@openremote/model";
import { AttributeMarkerColoursRange, MapMarkerColours } from "./markers/or-map-marker-asset";
export declare function getLngLat(lngLatLike?: LngLatLike | Asset | ValueHolder<any> | GeoJSONPoint): {
    lng: number;
    lat: number;
} | undefined;
export declare function getGeoJSONPoint(lngLat: LngLatLike | undefined): GeoJSONPoint | undefined;
export declare function getLngLatBounds(lngLatBoundsLike?: LngLatBoundsLike): LngLatBounds | undefined;
export declare function getLatLngBounds(lngLatBoundsLike?: LngLatBoundsLike): L.LatLngBounds | undefined;
export interface OverrideConfigSettings {
    markerConfig: MapMarkerColours;
    currentValue: any;
}
export declare function getMarkerIconAndColorFromAssetType(type: AssetDescriptor | string | undefined, configOverrideSettings?: OverrideConfigSettings): {
    icon: string;
    color: string | undefined | AttributeMarkerColoursRange[];
} | undefined;
