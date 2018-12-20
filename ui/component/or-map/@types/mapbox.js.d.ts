declare module "mapbox.js" {

    export default L;

    type Point2d = [number, number];
    type Point3d = [number, number, number];

    type LatLngLike = Point2d | Point3d | LatLng;
    type LatLngBoundsLike = [LatLngLike, LatLngLike] | LatLngBounds;

    export interface MapOptions {
        zoom?: number,
        boxZoom?: boolean;
        center?: LatLngLike;
        maxBounds?: LatLngBoundsLike;
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
        icon?: Icon;
        keyboard?: boolean;
        title?: string;
        alt?: string;
        zIndexOffset?: number;
        opacity?: number;
    }

    export class LatLng {
        private constructor();

        lat: number;
        lng: number;
        alt?: number;
    }

    export class LatLngBounds {
        private constructor();
        getSouthEast(): LatLng;
        getNorthWest(): LatLng;
        getNorthEast(): LatLng;
        getSouthWest(): LatLng;
        getCenter(): LatLng;
    }

    export class Icon {

    }

    export class DivIcon extends Icon {

    }

    export class Marker extends Layer {
        setLatLng(latLng: LatLngLike): this;
        on(event: any, func: any): this;
    }

    class Layer {
        addTo(addTo: L.mapbox.map): this;
        removeFrom(removeFrom: L.mapbox.map): this;
    }

    export namespace L {
        namespace mapbox {
            export class map {
                constructor(element: string | Element, tilejson: object, options?: MapOptions);
                getBoundsZoom(bounds: LatLngBoundsLike, inside?: boolean): number;
                setMaxZoom(zoom: number): this;
                setMinZoom(zoom: number): this;
            }
        }

        function icon(options: IconOptions): Icon;

        function divIcon(options: DivIconOptions): DivIcon;

        function latLng(lat: number, lng: number, alt?: number): LatLng;

        function latLng(pos: LatLngLike): LatLng;

        function latLngBounds(corner1: LatLngLike, corner2: LatLngLike): LatLngBounds;

        function latLngBounds(bounds: LatLngBoundsLike): LatLngBounds;

        function marker(pos: LatLngLike, options: MarkerOptions): Marker;
    }
}