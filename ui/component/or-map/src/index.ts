import openremote, {EventCallback, OREvent} from "@openremote/core";
import {html, PolymerElement} from "@polymer/polymer";
import { FlattenedNodesObserver } from '@polymer/polymer/lib/utils/flattened-nodes-observer.js';
import {customElement, property} from '@polymer/decorators';
import {OrMapMarker} from "../markers/or-map-marker";

import {MapWidget} from "./mapwidget";

export enum Type {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}

/**
 * `or-map`
 * Displays a map
 *
 */
@customElement('or-map')
export class OrMap extends PolymerElement {
    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean = false;
    protected _observer?: FlattenedNodesObserver;

    static get template() {
        return html`
      <style>
        :host {
            display: block;
            overflow: hidden;
        }
        #map {            
            position: relative;
            width: 100%;
            height: 100%;
        }
        slot {
            display: none;
        }
      </style>
      <div id="map"></div>
      <slot id="markers-slot"></slot>
    `;
    }

    @property({type: String})
    type: Type = Type.VECTOR;

    constructor() {
        super();
    }

    _processNewMarkers(nodes : Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if(node instanceof OrMapMarker) {
                this._map.addMarker(node);
            }
        });
    }

    _processRemovedMarkers(nodes : Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if(node instanceof OrMapMarker) {
                this._map.removeMarker(node);
            }
        });
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._observer!.disconnect();
    }

    ready() {
        super.ready();

        if (!openremote.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent) => {
                if (initEvent === OREvent.READY) {
                    this.loadMap();
                }
            };
            openremote.addListener(this._initCallback);
        } else {
            this.loadMap();
        }
    }

    loadMap() {

        if (this._loaded) {
            return;
        }

        if (this.shadowRoot) {
            const mapElement: HTMLElement = this.shadowRoot.getElementById('map')!;

            this._map = new MapWidget(this.type, this.shadowRoot, mapElement);
            this._map.load().then(() => {
                // Get markers from slot
                let slotElement = this.shadowRoot!.getElementById('markers-slot');
                this._observer = new FlattenedNodesObserver(slotElement!, (info) => {
                    this._processNewMarkers(info.addedNodes);
                    this._processRemovedMarkers(info.removedNodes);
                });
            });
        }

        this._loaded = true;
    }
}
