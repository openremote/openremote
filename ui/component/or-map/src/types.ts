import { Asset, Attribute, GeoJsonConfig, WellknownAttributes } from "@openremote/model";
import { LngLat, LngLatBoundsLike, LngLatLike } from "maplibre-gl";
import { Point } from "geojson";

type MandatoryAttribute<T> = Attribute<T> & { value: T };

export interface AssetWithLocation extends Asset {
    attributes: { [index: string]: Attribute<any> } & {
        [WellknownAttributes.LOCATION]: MandatoryAttribute<Point>;
    };
}

export interface ClusterConfig {
    cluster: boolean;
    clusterRadius: number;
    /** Until what zoom level cluster markers are shown */
    clusterMaxZoom: number;
}

export type ControlPosition = "top-right" | "top-left" | "bottom-right" | "bottom-left";

export interface MapEventDetail {
    lngLat: LngLat;
    doubleClick: boolean;
}

export interface MapGeocoderEventDetail {
    geocode: any;
}

export interface ViewSettings {
    center: LngLatLike;
    bounds?: LngLatBoundsLike | null;
    zoom: number;
    maxZoom: number;
    minZoom: number;
    boxZoom: boolean;
    geocodeUrl: string;
    geoJson?: GeoJsonConfig;
}
