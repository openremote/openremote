import {LngLat, LngLatBounds, LngLatBoundsLike, LngLatLike} from "mapbox-gl";
import L, {LatLng, LatLngBounds} from "mapbox.js";

export function getLngLat(lngLatLike?: LngLatLike): LngLat | undefined {
    if (lngLatLike) {

        if (lngLatLike instanceof LngLat) {
            return lngLatLike as LngLat;
        }

        const obj = lngLatLike as object;

        if (obj.hasOwnProperty("lng")) {
            const lngLatObj = obj as { lng: number; lat: number; };
            return new LngLat(lngLatObj.lng, lngLatObj.lat);
        }

        if (obj.hasOwnProperty("lon")) {
            const lonLatObj = obj as { lon: number; lat: number; };
            return new LngLat(lonLatObj.lon, lonLatObj.lat);
        }
        const lonLatArr = obj as number[];

        if (lonLatArr.length === 2) {
            return new LngLat(lonLatArr[0], lonLatArr[1]);
        }
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