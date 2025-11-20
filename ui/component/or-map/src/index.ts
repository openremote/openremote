import manager, {EventCallback} from "@openremote/core";
import {html, LitElement, PropertyValues} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {IControl, LngLat, LngLatBoundsLike, LngLatLike, GeolocateControl} from "maplibre-gl";
import {ClusterConfig, MapWidget} from "./mapwidget";
import {style} from "./style";
import "./markers/or-map-marker";
import "./markers/or-map-marker-asset";
import {OrMapMarker, OrMapMarkerChangedEvent} from "./markers/or-map-marker";
import * as Util from "./util";
import {
    InputType,
    ValueInputProviderGenerator,
    ValueInputTemplateFunction
} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {getMarkerIconAndColorFromAssetType} from "./util";
import {i18next} from "@openremote/or-translate";
import { debounce } from "lodash";
import { Asset, Attribute, GeoJsonConfig, GeoJSONPoint, MapType, WellknownAttributes } from "@openremote/model";
import { CoordinatesControl, CoordinatesRegexPattern, getCoordinatesInputKeyHandler } from "./controls/coordinates";
import { CenterControl } from "./controls/center";

// Re-exports
export {Util, LngLatLike, LngLat, ClusterConfig};
export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";
export * from "./markers/or-cluster-marker";
export {IControl} from "maplibre-gl";
export * from "./or-map-asset-card";
export * from "./or-map-legend";

export interface AssetWithLocation extends Asset {
    attributes: { [index: string]: Attribute<any> } & {
        [WellknownAttributes.LOCATION]: Attribute<GeoJSONPoint>
    };
}

export interface ViewSettings {
    center: LngLatLike;
    bounds?: LngLatBoundsLike | null;
    zoom: number;
    maxZoom: number;
    minZoom: number;
    boxZoom: boolean;
    geocodeUrl: string;
    geoJson?: GeoJsonConfig
}

export interface MapEventDetail {
    lngLat: LngLat;
    doubleClick: boolean;
}
export interface MapGeocoderEventDetail {
    geocode: any;
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

    constructor(lngLat: LngLat, doubleClick = false) {
        super(OrMapClickedEvent.NAME, {
            detail: {
                doubleClick: doubleClick,
                lngLat: lngLat
            },
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapLongPressEvent extends CustomEvent<MapEventDetail> {

    public static readonly NAME = "or-map-long-press";

    constructor(lngLat: LngLat) {
        super(OrMapLongPressEvent.NAME, {
            detail: {
                doubleClick: false,
                lngLat: lngLat
            },
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapGeocoderChangeEvent extends CustomEvent<MapGeocoderEventDetail> {

    public static readonly NAME = "or-map-geocoder-change";

    constructor(geocode: any) {
        super(OrMapGeocoderChangeEvent.NAME, {
            detail: {
                geocode
            },
            bubbles: true,
            composed: true
        });
    }
}

export class OrMapMarkersChangedEvent extends CustomEvent<AssetWithLocation[]> {

    public static readonly NAME = "or-map-markers-changed";

    constructor(assets: AssetWithLocation[]) {
        super(OrMapMarkersChangedEvent.NAME, {
            detail: assets,
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapClickedEvent.NAME]: OrMapClickedEvent;
        [OrMapLoadedEvent.NAME]: OrMapLoadedEvent;
        [OrMapLongPressEvent.NAME]: OrMapLongPressEvent;
        [OrMapGeocoderChangeEvent.NAME]: OrMapGeocoderChangeEvent;
        [OrMapMarkersChangedEvent.NAME]: OrMapMarkersChangedEvent;
    }
}

export type ControlPosition = "top-right" | "top-left" | "bottom-right" | "bottom-left";

export const geoJsonPointInputTemplateProvider: ValueInputProviderGenerator = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {

    const disabled = !!(options && options.disabled);
    const readonly = !!(options && options.readonly);
    const compact = !!(options && options.compact);
    const comfortable = !!(options && options.comfortable);
    const centerControl = new CenterControl();

    const valueChangeHandler = (value: LngLatLike | undefined) => {
        if (!valueChangeNotifier) {
            return;
        }
        if (value !== undefined) {
            valueChangeNotifier({
                value: value ? Util.getGeoJSONPoint(value) : null
            });
        } else {
            valueChangeNotifier(undefined);
        }
    };

    const coordinatesControl = new CoordinatesControl(disabled, valueChangeHandler);
    let pos: { lng: number, lat: number } | null | undefined;

    const templateFunction: ValueInputTemplateFunction = (value, focused, loading, sending, error, helperText) => {
        let center: number[] | undefined;

        if (value) {
            pos = Util.getLngLat(value);
            center = pos ? Object.values(pos) : undefined;
        }

        const centerStr = center ? center.join(", ") : undefined;
        centerControl.pos = pos || undefined;
        coordinatesControl.readonly = disabled || readonly || sending || loading;
        coordinatesControl.value = centerStr;

        const iconAndColor = getMarkerIconAndColorFromAssetType(assetDescriptor);

        let dialog: OrMwcDialog | undefined;

        const setPos = (lngLat: LngLatLike | null) => {
            if (readonly || disabled) {
                return;
            }

            pos = lngLat ? Util.getLngLat(lngLat) : null;

            if (dialog) {
                // We're in compact mode modal
                const marker = dialog.shadowRoot!.getElementById("geo-json-point-marker") as OrMapMarker;
                marker.lng = pos ? pos.lng : undefined;
                marker.lat = pos ? pos.lat : undefined;
                center = pos ? Object.values(pos) : undefined;
                const centerStr = center ? center.join(", ") : undefined;
                coordinatesControl.value = centerStr;
            } else {
                valueChangeHandler(pos as LngLatLike);
            }
        };

        const controls = [[centerControl, "bottom-left"], [coordinatesControl, "top-right"]]

        if (!readonly) {

            const userLocationControl = new GeolocateControl({
                positionOptions: {
                    enableHighAccuracy: true
                },
                showAccuracyCircle: false,
                showUserLocation: false
            });
        
            userLocationControl.on('geolocate', (currentLocation: GeolocationPosition) => {
                setPos(new LngLat(currentLocation.coords.longitude, currentLocation.coords.latitude));
            });
            userLocationControl.on('outofmaxbounds', (currentLocation: GeolocationPosition) => {
                setPos(new LngLat(currentLocation.coords.longitude, currentLocation.coords.latitude));
            });
            controls.push([userLocationControl, "bottom-left"]);
        }

        let content = html`
            <or-map id="geo-json-point-map" class="or-map" @or-map-long-press="${(ev: OrMapLongPressEvent) => {setPos(ev.detail.lngLat);}}" .center="${center}" .controls="${controls}" .showGeoCodingControl=${!readonly}>
                <or-map-marker id="geo-json-point-marker" active .lng="${pos ? pos.lng : undefined}" .lat="${pos ? pos.lat : undefined}" .icon="${iconAndColor ? iconAndColor.icon : undefined}" .activeColor="${iconAndColor ? "#" + iconAndColor.color : undefined}" .colour="${iconAndColor ? "#" + iconAndColor.color : undefined}"></or-map-marker>
            </or-map>
            <span class="long-press-msg">${i18next.t('longPressSetLoc')}</span>
        `;

        if (compact) {
            const mapContent = content;


            const onClick = () => {
                dialog = showDialog(
                    new OrMwcDialog()
                        .setContent(mapContent)
                        .setStyles(html`
                            <style>
                                .dialog-container {
                                    flex-direction: column !important;
                                }
                                .dialog-container .long-press-msg {
                                    display: block;
                                    text-align: center;
                                }
                                or-map {
                                    width: 600px !important;
                                    min-height: 600px !important;
                                }
                            </style>
                        `)
                        .setActions([
                            {
                                actionName: "none",
                                content: "none",
                                action: () => {
                                    setPos(null);
                                    valueChangeHandler(pos as LngLatLike);
                                }
                            },
                            {
                                actionName: "ok",
                                content: "ok",
                                action: () => {
                                    valueChangeHandler(pos as LngLatLike);
                                }
                            },
                            {
                                default: true,
                                actionName: "cancel",
                                content: "cancel"
                            }
                        ]));
            };

            content = html`
                <style>
                    #geo-json-point-input-compact-wrapper {
                        display: table-cell;
                    }
                    #geo-json-point-input-compact-wrapper > * {
                        vertical-align: middle;
                    }
                </style>
                <div id="geo-json-point-input-compact-wrapper">
                    <or-mwc-input style="width: auto;" .comfortable="${comfortable}" .type="${InputType.TEXT}" .value="${centerStr}" .pattern="${CoordinatesRegexPattern}" @keyup="${(e: KeyboardEvent) => getCoordinatesInputKeyHandler(valueChangeHandler)(e)}"></or-mwc-input>
                    <or-mwc-input style="width: auto;" .type="${InputType.BUTTON}" compact icon="crosshairs-gps" @or-mwc-input-changed="${onClick}"></or-mwc-input>
                </div>
            `;
        }

        return content;
    };

    return {
        templateFunction: templateFunction,
        supportsHelperText: false,
        supportsLabel: false,
        supportsSendButton: false,
        validator: () => !pos || (pos.lat !== undefined && pos.lat !== null && pos.lng !== undefined && pos.lng !== null)
    };
}

@customElement("or-map")
export class OrMap extends LitElement {

    public static styles = style;

    @property({type: String})
    public type: MapType = manager.mapType;

    @property({type: Object})
    public cluster?: ClusterConfig;

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

    @property({type: Boolean})
    public showGeoCodingControl: boolean = false;

    @property({type: Boolean})
    public showBoundaryBoxControl: boolean = false;

    @property({type: Boolean})
    public useZoomControl: boolean = true;

    @property({type: Object})
    public geoJson?: GeoJsonConfig;

    @property({type: Boolean})
    public showGeoJson: boolean = true;

    @property({type: Array})
    public boundary: string[] = [];

    public controls?: (IControl | [IControl, ControlPosition?])[];

    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean = false;

    protected _resizeObserver?: ResizeObserver;

    @query("#map")
    protected _mapContainer?: HTMLElement;

    @query("slot")
    protected _slotElement?: HTMLSlotElement;

    constructor() {
        super();
        this.addEventListener(OrMapMarkerChangedEvent.NAME, this._onMarkerChangedEvent);
    }

    public addAssetMarker(asset: AssetWithLocation) {
        const coordinates = asset?.attributes?.location.value;
        if (!coordinates?.coordinates) return;
        this._map?.addAssetMarker(asset.id ?? '', asset.name ?? '', asset.type ?? '', coordinates.coordinates[0], coordinates.coordinates[1], asset);
    }

    public cleanUpAssetMarkers(): void {
        this._map?.cleanUpAssetMarkers();
    }

    public async reload() {
        await this._map?.load();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (manager.ready) {
            this.loadMap();
        }
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        if(this._resizeObserver) {
            this._resizeObserver.disconnect();
        }
        // Clean up of internal resources associated with the map
        this._map?.unload();
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
        if (changedProperties.has("boundary") && this.showBoundaryBoxControl){
            this._map?.createBoundaryBox(this.boundary)
        }
    }

    public refresh() {
        if (this._map) {
            this._map.loadViewSettings().then(() => {
                if (!this._map) return;
                this._map.setCenter();
                this._map.flyTo();
            });
        }
    }

    public loadMap() {
        if (this._loaded) {
            return;
        }

        if (this._mapContainer && this._slotElement) {
            this._map = new MapWidget(this.type, this.shadowRoot!, this._mapContainer, this.showGeoCodingControl, this.showBoundaryBoxControl, this.useZoomControl, this.showGeoJson, this.cluster)
                .setCenter(this.center)
                .setZoom(this.zoom)
                .setControls(this.controls)
                .setGeoJson(this.geoJson);

            this._map.build().then(() => {
                this._resizeObserver?.disconnect();
                this._resizeObserver = new ResizeObserver(debounce(() => {
                    this.resize();
                }, 200));
                const container = this._mapContainer?.parentElement;
                if (container) {
                    this._resizeObserver.observe(container);
                }
                this._slotElement!.assignedNodes({ flatten: true }).forEach((node) => {
                    if (node instanceof OrMapMarker) {
                      const marker = node;
                      this._map!.addMarker(marker)
                    }
                })
            });
        }

        this._loaded = true;
    }

    public resize() {
        if (this._map) {
            this._map.resize();
        }
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
}
