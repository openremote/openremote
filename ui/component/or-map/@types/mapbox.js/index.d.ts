/* tslint:disable:class-name */
declare module "mapbox.js" {

    export type Point2d = [number, number];
    export type Point3d = [number, number, number];

    export type LatLngLike = Point2d | Point3d | LatLng;
    export type LatLngBoundsLike = [LatLngLike, LatLngLike] | LatLngBounds;

    export default L;

    export class LatLng {
        public alt?: number;
        public lat: number;
        public lng: number;
        private constructor(lat: number, lng: number, alt?: number);
    }

    export class LatLngBounds {
        private constructor();
        public getSouthEast(): LatLng;
        public getNorthWest(): LatLng;
        public getNorthEast(): LatLng;
        public getSouthWest(): LatLng;
        public getCenter(): LatLng;
    }

    export class Map {
        private constructor();
        public getBoundsZoom(bounds: LatLngBoundsLike, inside?: boolean): number;
        public setMaxZoom(zoom: number): this;
        public setMinZoom(zoom: number): this;
        public on(type: string, func: any): this;
        public getContainer(): HTMLElement;
        public setView(center: LatLngLike, zoom?: number, options?: ZoomPanOptions): this;
        public setZoom(zoom: number, options?: ZoomOptions): this;
    }

    export interface MapOptions {
        zoom?: number;
        boxZoom?: boolean;
        center?: LatLngLike;
        maxBounds?: LatLngBoundsLike;
        maxZoom?: number;
        minZoom?: number;
    }

    export interface IconOptions {
        iconUrl?: string;
        iconRetinaUrl?: string;
        iconSize?: Point;
        iconAnchor?: Point;
        popupAnchor?: Point;
        tooltipAnchor?: Point;
        shadowUrl?: string;
        shadowRetinaUrl?: string;
        shadowSize?: Point;
        shadowAnchor?: Point;
        className?: string;
    }

    export interface DivIconOptions extends IconOptions {
        html?: string;
        bgPos?: Point;
    }

    export interface MarkerOptions {
        icon?: Icon;
        keyboard?: boolean;
        title?: string;
        alt?: string;
        zIndexOffset?: number;
        opacity?: number;
        clickable?: boolean;
    }

    export interface ZoomOptions {
        animate?: boolean;
    }

    export interface PanOptions extends ZoomOptions {
        duration?: number;
        easeLinearity?: number;
        noMoveStart?: boolean;
    }

    export interface ZoomPanOptions extends ZoomOptions {
        reset?: boolean;
        pan?: PanOptions;
        zoom?: ZoomOptions;
    }

    export interface Icon {

    }

    export interface DivIcon extends Icon {

    }

    export interface Point {
        x: number;
        y: number;
    }

    export class Marker extends Layer {
        public setLatLng(latLng: LatLngLike): this;
        public on(event: any, func: any): this;
        public off(event: any, func?: any): this;
        public getElement(): HTMLElement;
    }

    class Layer {
        public addTo(addTo: Map): this;
        public removeFrom(removeFrom: Map): this;
    }

    namespace L {

        namespace mapbox {
            function map(element: string | Element, tilejson: object, options?: MapOptions): Map;
        }

        function icon(options: IconOptions): Icon;

        function divIcon(options: DivIconOptions): DivIcon;

        function point(x: number, y: number): Point;

        function latLng(lat: number, lng: number, alt?: number): LatLng;

        function latLng(pos: LatLngLike): LatLng;

        function latLngBounds(corner1: LatLngLike, corner2: LatLngLike): LatLngBounds;

        function latLngBounds(bounds: LatLngBoundsLike): LatLngBounds;

        function marker(pos: LatLngLike, options: MarkerOptions): Marker;
    }
}
