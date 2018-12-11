import {LngLatBoundsLike, LngLatLike} from "mapbox-gl";

export = L
export as namespace L;

declare namespace L {
    export namespace mapbox {
        export class Map {
            constructor(element: string | Element, tilejson: object, options?: MapOptions);
        }

        export interface MapOptions {
            boxZoom?: boolean;
            center?: LngLatLike;
            maxBounds?: LngLatBoundsLike;
            maxZoom?: number;
            minZoom?: number;
        }
    }
}