import { LngLat, LngLatBounds } from "maplibre-gl";
import { AssetModelUtil } from "@openremote/model";
export function getLngLat(lngLatLike) {
    if (!lngLatLike) {
        return;
    }
    if (lngLatLike instanceof LngLat) {
        return lngLatLike;
    }
    if (lngLatLike.lat) {
        if (lngLatLike.lon) {
            return { lng: lngLatLike.lon, lat: lngLatLike.lat };
        }
        return { lng: lngLatLike.lng, lat: lngLatLike.lat };
    }
    if (Array.isArray(lngLatLike)) {
        return { lng: lngLatLike[0], lat: lngLatLike[1] };
    }
    if (lngLatLike.coordinates && Array.isArray(lngLatLike.coordinates) && lngLatLike.coordinates.length >= 2) {
        return getLngLat(lngLatLike.coordinates);
    }
    if (lngLatLike.attributes) {
        // This is an asset
        const locationAttribute = lngLatLike.attributes["location" /* WellknownAttributes.LOCATION */];
        if (!locationAttribute) {
            return;
        }
        if (locationAttribute.value) {
            return getLngLat(locationAttribute.value);
        }
        return;
    }
    if (lngLatLike.value) {
        return getLngLat(lngLatLike.value);
    }
}
export function getGeoJSONPoint(lngLat) {
    if (!lngLat) {
        return;
    }
    return Array.isArray(lngLat) ? { type: "Point", coordinates: lngLat } : { type: "Point", coordinates: [lngLat.hasOwnProperty("lng") ? lngLat.lng : lngLat.lon, lngLat.lat] };
}
export function getLngLatBounds(lngLatBoundsLike) {
    if (lngLatBoundsLike) {
        if (lngLatBoundsLike instanceof LngLatBounds) {
            return lngLatBoundsLike;
        }
        const arr = lngLatBoundsLike;
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
export function getLatLngBounds(lngLatBoundsLike) {
    const lngLatBounds = getLngLatBounds(lngLatBoundsLike);
    if (lngLatBounds) {
        return L.latLngBounds(lngLatBounds.getNorthEast(), lngLatBounds.getSouthWest());
    }
}
export function getMarkerIconAndColorFromAssetType(type, configOverrideSettings) {
    if (!type) {
        return;
    }
    const descriptor = typeof (type) === "string" ? AssetModelUtil.getAssetDescriptor(type) : type;
    const icon = descriptor && descriptor.icon ? descriptor.icon : "help-circle";
    let colour = descriptor && descriptor.colour ? descriptor.colour : undefined;
    if (configOverrideSettings) {
        if (configOverrideSettings.markerConfig) {
            const colourConfig = configOverrideSettings.markerConfig;
            const attrVal = configOverrideSettings.currentValue;
            if (colourConfig.type === "range" && colourConfig.ranges && typeof attrVal === "number") {
                const ranges = colourConfig.ranges;
                // see in what range the attrVal fits and if not, what the setting for the highest range is
                const colourFromRange = ranges.sort((a, b) => b.min - a.min).find(r => attrVal >= r.min) || ranges.reduce((a, b) => (b.min > a.min) ? a : b);
                colour = colourFromRange.colour || undefined;
            }
            else if (colourConfig.type === "boolean") {
                const value = !!attrVal + "";
                colour = colourConfig[value];
            }
            else if (colourConfig.type === "string") {
                const value = attrVal + "";
                colour = colourConfig[value];
            }
        }
        // todo icon override
    }
    return {
        color: colour,
        icon: icon
    };
}
//# sourceMappingURL=util.js.map