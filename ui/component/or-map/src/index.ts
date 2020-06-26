import manager, {EventCallback, OREvent, MapType} from "@openremote/core";
import {FlattenedNodesObserver} from "@polymer/polymer/lib/utils/flattened-nodes-observer.js";
import {CSSResult, customElement, html, LitElement, property, PropertyValues, query} from "lit-element";
import {LngLat, LngLatBoundsLike, LngLatLike} from "mapbox-gl";
import {MapWidget} from "./mapwidget";
import {style} from "./style";
import {OrMapMarker, OrMapMarkerChangedEvent} from "./markers/or-map-marker";
import * as Util from "./util";

// Re-exports
export {Util, LngLat};
export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";

export interface ViewSettings {
    "center": LngLatLike;
    "bounds": LngLatBoundsLike;
    "zoom": number;
    "maxZoom": number;
    "minZoom": number;
    "boxZoom": boolean;
}

export interface MapEventDetail {
    lngLat: LngLat;
}

export class OrMapLoadedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-map-loaded";

    constructor() {
        super(OrMapLoadedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapClickedEvent extends CustomEvent<MapEventDetail> {

    public static readonly NAME = "or-map-clicked";

    constructor(lngLat: LngLat) {
        super(OrMapClickedEvent.NAME, {
            detail: {
                lngLat: lngLat
            },
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapClickedEvent.NAME]: OrMapClickedEvent;
        [OrMapLoadedEvent.NAME]: OrMapLoadedEvent;
    }
}

@customElement("or-map")
export class OrMap extends LitElement {

    public static styles = style;

    @property({type: String})
    public type: MapType = manager.mapType;

    protected _markerStyles: string[] = [];

    @property({type: String, converter: {
            fromAttribute(value: string | null, type?: String): LngLatLike | undefined {
                if (!value) {
                    return;
                }

                const coords = value.split(",");
                if (coords.length !== 2) {
                    return;
                }
                const lng = Number(coords[0]);
                const lat = Number(coords[1]);
                return new LngLat(lng, lat);
            },

            toAttribute(value?: LngLatLike, type?: String): string {
                const lngLat = Util.getLngLat(value);

                if (!lngLat) {
                    return "";
                }

                return "" + lngLat.lng + "," + lngLat.lat;
            }
        }})
    public center?: LngLatLike;

    @property({type: Number})
    public zoom?: number;

    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean = false;
    protected _observer?: FlattenedNodesObserver;
    protected _markers: OrMapMarker[] = [];

    @query("#map")
    protected _mapContainer?: HTMLElement;

    @query("slot")
    protected _slotElement?: HTMLSlotElement;

    constructor() {
        super();
        this.addEventListener(OrMapMarkerChangedEvent.NAME, this._onMarkerChangedEvent);
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        manager.addListener(this.onManagerEvent);
        if (manager.ready) {
            this.loadMap();
        }
    }

    
    protected onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.READY:
                if (!manager.ready) {
                    this.loadMap();
                }
                break;
            case OREvent.DISPLAY_REALM_CHANGED:
                if(this._map){
                    this._map.loadViewSettings().then(()=> {
                        if(!this._map) return
                        this._map.setCenter()
                        this._map.flyTo();
                    });
                }
                break;
        }
    }

    public get markers(): OrMapMarker[] {
        return this._markers;
    }


    public disconnectedCallback() {
        super.disconnectedCallback();
        this._observer!.disconnect();
        manager.removeListener(this.onManagerEvent);
    }

    protected render() {
        return html`
            <div id="container">
                <div id="map"></div>
                <slot></slot>
                <a id="openremote" href="https://openremote.io/" target="_blank">
                    <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAABGdBTUEAAYagMeiWXwAAAAJiS0dEAP+Hj8y/AAAESUlEQVRIx5XVbWzdZRnH8c//f07POe36sK7SrYXZjYGzbBokOqd4QtKATdnMDFFSkoFzmWGRQOAFSoYnsBSzxBdmGsN4Y7LEFwZkUdHpDoja/AnJjBvp1sm2upW5PtAV+zBS+7Tz//tiLe0emPF+d9/39fvmunJf9+8K3HBF1drFzurVn5+5XkTwPwBNOt1s0pCjDnnT+Xzy/wOa5jaXnLLfLwzlF0WENwaYckyvUTHS1tnjl+6JFqk+JoMOqqy2JqjPVpXP1k7U1+Ty8paBPk97NX/pYwEdgZUetNkdlirDrHGnyv6y5r3lm4M2WVzwpJfz8XUBHTntnrJO6qqLWE9u/125zBNq0WebN/PXAjqq/cBOVWDasCEsd5MsmEzt/3xP+S6fwNsezPdfBejI6fCEDMa95oAT/t31pVxDQ6p6oy2WYSb18w0D2V3Klez2w3y8CNAR2G6vShxXUCxMkXLvS7Y6E/5+3emaJ92JqezzG+8M2nHW/flTi59xladU4phtfluYgnsDWUt8Nv7+8UfO73UUuenvDLxuCKu0RQt90BFo14wxzzpamD8uExtHSsu5HSP7XMCtZ5uTPyO0SdVCBlXahHjNG4WFrGY9Y4tXzODL7zb7NYJS64eHzWK92xYAa9yBKa8Wphf0xaQ4XOz0qF9JhMnXh//mIm4dnDSMOusWALepwQUnrm2t4pi9hrGyP+ccloxV6kOZFemoWi2mOpclaQycqGlt9R8XHS/GixinnVWvbDpjDEEpNpdnWrtd+HvZWzMQT9xjj1iXzUau6MPS9X9NKOeTmqzPpZWwfMkEKnza2ivimqxCKZjQa9BMmFI2D+gxibql4z7EiobYOSy1o7V6Xt1aYacGvD/7lse1+GZ9t0Zc8kHaGcOa1K6o+FePL1iy7K7wYHy70FZa9+qVWOm7tgslfpecKcy46GS0xXKM6g6d14VU+RfTnRJ8Y223w8j4tkMOOeR1j6nAMT8tzkCUcvlbn3ImbJn0RyWC+1af1Iv6ukcbf+aIRKDR3b5ipVCiy+PenaupWRsSfzCWim0ftUmdiqrJwWLpbmk3196UfXG0X6NKIWKDXva0I0UQZT2nRaDfc/mhgCj0PS9ImZzefbg5fliIvuTA++/0ZaYDTDqqpzhn6lHoW36iSuLHnslfCiBqdMBGDI6/0LUhfkgGiWFbC29c+epRaJMX3YJuD+R75l15wG4DaKh5dsPJsj0GXLaawavkWY/MyUcU/JOPHCkK7fAjNZiIf/PeX/vWx1814muF0Y/EKWs95mFVuKhgX352EYAoY5vnNSDRF/9p/MgHfQ2dldNIqbPeJm2aBBix20vzg26RpUUpLfb43FxZU4YMmEJGoxXKQeIfCg4uzMkrTDVitZ0ecst1B05i0Cv26Vk8H68JjFKabXa/Zkul5w5Lxp120EHdlyu/AQCiQI1P+YxaGcwY1+20kasnM/wXCa5/Ik1hKTEAAAAldEVYdGRhdGU6Y3JlYXRlADIwMTktMDgtMjBUMTU6MTc6NTUrMDA6MDCwJSdSAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDE5LTA4LTIwVDE1OjE3OjU1KzAwOjAwwXif7gAAAABJRU5ErkJggg==" />
                </a>
            </div>
        `;
    }

    protected updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (changedProperties.has("center") || changedProperties.has("zoom")) {
            this.flyTo(this.center, this.zoom);
        }
    }

    public reloadMap() {
        this._loaded = false;
        this.loadMap();
    }

    public loadMap() {

        if (this._loaded) {
            return;
        }

        if (this._mapContainer && this._slotElement) {
            this._map = new MapWidget(this.type, this.shadowRoot!, this._mapContainer)
                .setCenter(this.center)
                .setZoom(this.zoom);
            this._map.load().then(() => {
                // Get markers from slot
                this._observer = new FlattenedNodesObserver(this._slotElement!, (info: any) => {
                    this._processNewMarkers(info.addedNodes);
                    this._processRemovedMarkers(info.removedNodes);
                });
            });
        }

        this._loaded = true;
    }

    public flyTo(coordinates?: LngLatLike, zoom?: number) {
        if (this._map) {
            this._map.flyTo(coordinates, zoom);
        }
    }

    protected _onMarkerChangedEvent(evt: OrMapMarkerChangedEvent) {
        if (this._map) {
            this._map.onMarkerChanged(evt.detail.marker, evt.detail.property);
        }
    }

    protected _processNewMarkers(nodes: Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if (node instanceof OrMapMarker) {

                this._markers.push(node);

                // Add styles of marker class to the shadow root if not already added
                const className = node.constructor.name;
                if (this._markerStyles.indexOf(className) < 0) {
                    const styles = (node.constructor as any).styles;
                    let stylesArr: CSSResult[] = [];

                    if (styles) {
                        if (!Array.isArray(styles)) {
                            stylesArr.push(styles as CSSResult);
                        } else {
                            stylesArr = styles as CSSResult[];
                        }

                        stylesArr.forEach((styleItem) => {
                            const styleElem = document.createElement("style");
                            styleElem.textContent = String(styleItem.toString());
                            if (this._mapContainer!.children.length > 0) {
                                this._mapContainer!.insertBefore(styleElem, this._mapContainer!.children[0]);
                            } else {
                                this._mapContainer!.appendChild(styleElem);
                            }
                        });
                    }

                    this._markerStyles.push(className);
                }

                this._map.addMarker(node);
            }
        });
    }

    protected _processRemovedMarkers(nodes: Element[]) {
        nodes.forEach((node) => {
            if (!this._map) {
                return;
            }

            if (node instanceof OrMapMarker) {
                const i = this._markers.indexOf(node);
                if (i >= 0) {
                    this._markers.splice(i, 1);
                }
                this._map.removeMarker(node);
            }
        });
    }
}

export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";
