declare module 'mapbox.js' {
    import {LngLatBoundsLike, LngLatLike} from "mapbox-gl";

    export namespace L {
        export namespace mapbox {
            export class map {
                constructor(element: string | Element, tilejson: object, options?: MapOptions);
            }

            export interface MapOptions {
                zoom: number,
                boxZoom?: boolean;
                center?: LngLatLike;
                maxBounds?: LngLatBoundsLike;
                maxZoom?: number;
                minZoom?: number;
            }
        }
    }
}