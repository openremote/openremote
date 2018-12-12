import {html, PolymerElement} from "@polymer/polymer";

/**
 * Base class for all map markers
 */
export abstract class OrMapMarker extends PolymerElement {
    protected _lat: number = 0;
    protected _lng: number = 0;
    protected _html?: HTMLElement = undefined;

    get latitude(): number {
        return this._lat;
    }

    get longitude(): number {
        return this._lng;
    }

    get html(): HTMLElement | undefined {
        return this._html;
    }
}