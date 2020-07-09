import {LngLat, LngLatBounds, LngLatBoundsLike, LngLatLike} from "mapbox-gl";
import L, {LatLng, LatLngBounds} from "mapbox.js";
import {Asset, AttributeType, AssetAttribute, GeoJSONPoint, Position, AttributeValueType} from "@openremote/model";
import {Util} from "@openremote/core";

export function getLngLat(lngLatLike?: LngLatLike | Asset | AssetAttribute | GeoJSONPoint): LngLat | undefined {
    if (lngLatLike) {

        if (lngLatLike instanceof LngLat) {
            return lngLatLike as LngLat;
        }

        let obj = lngLatLike as any;

        if ((obj as Asset).attributes) {
            // This is an asset
            const locationAttribute = Util.getAssetAttribute(obj as Asset, AttributeType.LOCATION.attributeName!);
            if (!locationAttribute) {
                return;
            }
            obj = locationAttribute;
        }

        if ((obj as AssetAttribute).name && (obj as AssetAttribute).type) {
            const attr = obj as AssetAttribute;
            if (attr.type !== AttributeValueType.GEO_JSON_POINT.name || !attr.value) {
                return;
            }
            obj = attr.value;
        }

        if ((obj as GeoJSONPoint).coordinates && Array.isArray(obj.coordinates)) {
            return new LngLat(obj.coordinates[0], obj.coordinates[1]);
        }

        if (obj.lng && obj.lat) {
            return new LngLat(obj.lng, obj.lat);
        }

        if (obj.lon && obj.lat) {
            return new LngLat(obj.lon, obj.lat);
        }

        if (Array.isArray(obj) && obj.length === 2) {
            return new LngLat(obj[0], obj[1]);
        }
    }
}

export function getGeoJSONPoint(lngLat: LngLat | undefined): GeoJSONPoint | null {
    if (!lngLat) {
        return null;
    }

    return {
        coordinates: ([lngLat.lng, lngLat.lat] as unknown) as Position
    }
}

export function getLngLatBounds(lngLatBoundsLike?: LngLatBoundsLike): LngLatBounds | undefined {
    if (lngLatBoundsLike) {

        if (lngLatBoundsLike instanceof LngLatBounds) {
            return lngLatBoundsLike as LngLatBounds;
        }

        const arr = lngLatBoundsLike as any[];
        if (arr.length === 2) {
            const sw = getLngLat(arr[0]);
            const ne = getLngLat(arr[1]);
            if (sw && ne) {
                return new LngLatBounds(sw, ne);
            }
        }
        
        if (arr.length === 4) {
            return new LngLatBounds([arr[0], arr[1], arr[2], arr[3]]);
        }
    }
}

export function getLatLng(lngLatLike?: LngLatLike): LatLng | undefined {
    const lngLat = getLngLat(lngLatLike);
    if (lngLat) {
        return L.latLng(lngLat.lat, lngLat.lng);
    }
}

export function getLatLngBounds(lngLatBoundsLike?: LngLatBoundsLike): LatLngBounds | undefined {
    const lngLatBounds = getLngLatBounds(lngLatBoundsLike);
    if (lngLatBounds) {
        return L.latLngBounds(getLatLng(lngLatBounds.getNorthEast())!, getLatLng(lngLatBounds.getSouthWest())!);
    }
}
