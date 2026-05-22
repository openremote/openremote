/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
