import manager, {EventCallback} from "@openremote/core";
import {html, LitElement, PropertyValues} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {IControl, LngLat, LngLatLike} from "maplibre-gl";
import {style} from "./style";
import "./markers/or-map-marker";
import "./markers/or-map-marker-asset";
import "@openremote/or-vaadin-components/or-vaadin-text-field"
import {OrMapMarker, OrMapMarkerChangedEvent} from "./markers/or-map-marker";
import * as Util from "./util";
import {
    ValueInputProviderGenerator,
    ValueInputTemplateFunction
} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {getMarkerIconAndColorFromAssetType} from "./util";
import {i18next} from "@openremote/or-translate";
import debounce from "lodash.debounce";
import { AttributeEvent, GeoJsonConfig } from "@openremote/model";
import { CoordinatesControl, CoordinatesRegexPattern, getCoordinatesInputKeyHandler } from "./controls/coordinates";
import { AssetMap } from "./asset-map";
import { OrMapCenterControl } from "./controls/center";
import { OrMapGeolocateControl } from "./controls/geolocate";
import { ifDefined } from "lit-html/directives/if-defined.js";
import type { AssetWithLocation, ClusterConfig, ControlPosition, MapEventDetail } from "./types";

// Re-exports
export {Util, LngLatLike, LngLat};
export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";
export * from "./markers/or-cluster-marker";
export {IControl} from "maplibre-gl";
export * from "./or-map-asset-card";
export * from "./controls/legend";
export * from "./controls/preset-filter";
export * from "./controls/geocoder";
export type * from "./types";

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
        [OrMapMarkersChangedEvent.NAME]: OrMapMarkersChangedEvent;
    }
}

export const geoJsonPointInputTemplateProvider: ValueInputProviderGenerator = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {

    const disabled = !!(options && options.disabled);
    const readonly = !!(options && options.readonly);
    const compact = !!(options && options.compact);
    const centerControl = new OrMapCenterControl();

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

    // Indirection so the stable geolocate instance can call the per-render setPos.
    let _setPos: ((lngLat: LngLatLike | null) => void) | undefined;
    const controls: (IControl | [IControl, ControlPosition?])[] = [
        [centerControl, "bottom-right"],
        [coordinatesControl, "top-left"],
    ];
    if (!readonly) {
        const userLocationControl = new OrMapGeolocateControl((currentLocation: GeolocationPosition) => {
            _setPos?.(new LngLat(currentLocation.coords.longitude, currentLocation.coords.latitude));
        });
        controls.push([userLocationControl, "bottom-right"]);
    }

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

        _setPos = (lngLat: LngLatLike | null) => {
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
        const setPos = _setPos;

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
                    <or-vaadin-text-field style="width: auto;" value=${ifDefined(centerStr)} pattern=${CoordinatesRegexPattern}
                                         @keyup="${(e: KeyboardEvent) => getCoordinatesInputKeyHandler(valueChangeHandler)(e)}">
                    </or-vaadin-text-field>
                    <or-vaadin-button theme="icon" @click=${onClick}>
                        <or-icon icon="crosshairs-gps"></or-icon>
                    </or-vaadin-button>
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
    protected _map?: AssetMap;
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

    public addAsset(asset: AssetWithLocation) {
        this._map?.addAsset(asset);
    }

    public addAssets(assets: AssetWithLocation[]) {
        this._map?.addAssets(assets);
    }

    public updateAttribute(event: AttributeEvent) {
        this._map?.updateAttribute(event)
    }

    public removeAssets(ids: string[]) {
        this._map?.removeAssets(ids);
    }

    public removeAllAssets(): void {
        this._map?.removeAllAssets();
    }

    public addControl(control: IControl, position?: ControlPosition): void {
        this._map?.addControl(control, position);
    }

    public removeControl(control: IControl): void {
        this._map?.removeControl(control);
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (manager.ready) {
            this.loadMap();
        }
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
            this._map = new AssetMap(this.shadowRoot!, this._mapContainer, this.showGeoCodingControl, this.showBoundaryBoxControl, this.useZoomControl, this.showGeoJson, this.cluster)
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
                        this._map!.addMarker(node)
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

    public _removeMarker(marker: OrMapMarker) {
        this._map?.removeMarker(marker);
    }

    protected _onMarkerChangedEvent(evt: OrMapMarkerChangedEvent) {
        if (this._map) {
            this._map.onMarkerChanged(evt.detail.marker, evt.detail.property);
        }
    }
}
