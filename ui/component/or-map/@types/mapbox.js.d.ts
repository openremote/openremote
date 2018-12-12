declare module "mapbox.js" {
    import {LngLatBoundsLike, LngLatLike} from "mapbox-gl";
    import {DivIconOptions} from "mapbox.js";

    export default L;

    type Point2d = [number, number];
    type Point3d = [number, number, number];

    export interface MapOptions {
        zoom: number,
        boxZoom?: boolean;
        center?: LngLatLike;
        maxBounds?: LngLatBoundsLike;
        maxZoom?: number;
        minZoom?: number;
    }

    export interface IconOptions {
        iconUrl?: string,
        iconRetinaUrl?: string,
        iconSize?: Point2d,
        iconAnchor?: Point2d,
        popupAnchor?: Point2d,
        tooltipAnchor?: Point2d,
        shadowUrl?: string,
        shadowRetinaUrl?: string,
        shadowSize?: Point2d,
        shadowAnchor?: Point2d,
        className?: string
    }

    export interface DivIconOptions extends IconOptions {
        html?: string,
        bgPos?: Point2d
    }

    export interface MarkerOptions {
        icon?: L.Icon;
        keyboard?: boolean;
        title?: string;
        alt?: string;
        zIndexOffset?: number;
        opacity?: number;
    }

    export namespace L {
        namespace mapbox {
            export class map {
                constructor(element: string | Element, tilejson: object, options?: MapOptions);
            }
        }

        function icon(options: IconOptions): Icon;

        function divIcon(options: DivIconOptions): DivIcon;

        function latLng(lat: number, lng: number, alt?: number): LatLng;

        function latLng(pos: Point2d | Point3d | LatLng): LatLng;

        function marker(pos: Point2d | Point3d | LatLng, options: MarkerOptions): Marker;

        export class LatLng {
            lat: number;
            lng: number;
            alt?: number;
        }

        export class Icon {

        }

        export class DivIcon extends Icon {

        }

        export class Marker extends Layer {

        }

        class Layer {
            addTo(addTo: L.mapbox.map): this;
            removeFrom(removeFrom: L.mapbox.map): this;
        }
    }
}