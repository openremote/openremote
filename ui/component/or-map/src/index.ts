import manager, {EventCallback} from "@openremote/core";
import {FlattenedNodesObserver} from "@polymer/polymer/lib/utils/flattened-nodes-observer.js";
import {CSSResult, html, LitElement, PropertyValues} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {Control, IControl, LngLat, LngLatBoundsLike, LngLatLike, Map as MapGL, GeolocateControl} from "maplibre-gl";
import {MapWidget} from "./mapwidget";
import {style} from "./style";
import "./markers/or-map-marker";
import "./markers/or-map-marker-asset";
import {OrMapMarker, OrMapMarkerChangedEvent} from "./markers/or-map-marker";
import * as Util from "./util";
import {
    InputType,
    OrMwcInput,
    ValueInputProviderGenerator,
    ValueInputTemplateFunction
} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {getMarkerIconAndColorFromAssetType} from "./util";
import {i18next} from "@openremote/or-translate";
import { debounce } from "lodash";
import {GeoJsonConfig, MapType } from "@openremote/model";

// Re-exports
export {Util, LngLatLike};
export * from "./markers/or-map-marker";
export * from "./markers/or-map-marker-asset";
export {Control, IControl} from "maplibre-gl";
export * from "./or-map-asset-card";

export interface ViewSettings {
    center: LngLatLike;
    bounds?: LngLatBoundsLike | null;
    zoom: number;
    maxZoom: number;
    minZoom: number;
    boxZoom: boolean;
    geocodeUrl: String;
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

    constructor(lngLat: LngLat, doubleClick: boolean = false) {
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

declare global {
    export interface HTMLElementEventMap {
        [OrMapClickedEvent.NAME]: OrMapClickedEvent;
        [OrMapLoadedEvent.NAME]: OrMapLoadedEvent;
        [OrMapLongPressEvent.NAME]: OrMapLongPressEvent;
        [OrMapGeocoderChangeEvent.NAME]: OrMapGeocoderChangeEvent;
    }
}

export type ControlPosition = "top-right" | "top-left" | "bottom-right" | "bottom-left";

export class CenterControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    public pos?: LngLatLike;

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");
        const button = document.createElement("button");
        button.className = "maplibregl-ctrl-compass";
        button.addEventListener("click", (ev) => map.flyTo({
            center: this.pos,
            zoom: map.getZoom()
        }));
        const buttonIcon = document.createElement("span");
        buttonIcon.className = "maplibregl-ctrl-icon";
        button.appendChild(buttonIcon);
        control.appendChild(button);
        this.elem = control;
        return control;
    }

    onRemove(map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
    }
}

const CoordinatesRegexPattern = "^[ ]*(?:Lat: )?(-?\\d+\\.?\\d*)[, ]+(?:Lng: )?(-?\\d+\\.?\\d*)[ ]*$";

function getCoordinatesInputKeyHandler(valueChangedHandler: (value: LngLat | undefined) => void) {
    return (e: KeyboardEvent) => {
        if (e.code === "Enter" || e.code === "NumpadEnter") {
            const valStr = (e.target as OrMwcInput).value as string;
            let value: LngLat | undefined = !valStr ? undefined : {} as LngLat;

            if (valStr) {
                const lngLatArr = valStr.split(/[ ,]/).filter(v => !!v);
                if (lngLatArr.length === 2) {
                    value = new LngLat(
                        Number.parseFloat(lngLatArr[0]),
                        Number.parseFloat(lngLatArr[1])
                    );
                }
            }
            valueChangedHandler(value);
        }
    };
}

export class CoordinatesControl {

    protected map?: MapGL;
    protected elem?: HTMLElement;
    protected input!: OrMwcInput;
    protected _readonly = false;
    protected _value: any;
    protected _valueChangedHandler: (value: LngLat | undefined) => void;

    constructor(disabled: boolean = false, valueChangedHandler: (value: LngLat | undefined) => void) {
        this._readonly = disabled;
        this._valueChangedHandler = valueChangedHandler;
    }

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");

        const input = new OrMwcInput();
        input.type = InputType.TEXT;
        input.outlined = true;
        input.compact = true;
        input.readonly = this._readonly;
        input.icon = "crosshairs-gps";
        input.value = this._value;
        input.pattern = CoordinatesRegexPattern;
        input.onkeyup = getCoordinatesInputKeyHandler(this._valueChangedHandler);

        control.appendChild(input);
        this.elem = control;
        this.input = input;
        return control;
    }

    onRemove(map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
    }

    public set readonly(readonly: boolean) {
        this._readonly = readonly;
        if (this.input) {
            this.input.readonly = readonly;
        }
    }

    public set value(value: any) {
        this._value = value;
        if (this.input) {
            this.input.value = value;
        }
    }
}

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

    public controls?: (Control | IControl | [Control | IControl, ControlPosition?])[];

    protected _initCallback?: EventCallback;
    protected _map?: MapWidget;
    protected _loaded: boolean = false;
    protected _observer?: FlattenedNodesObserver;
    protected _markers: OrMapMarker[] = [];

    protected _resizeObserver?: ResizeObserver;

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
        if (manager.ready) {
            this.loadMap();
        }
    }

    public get markers(): OrMapMarker[] {
        return this._markers;
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        if (this._observer) {
            this._observer.disconnect();
        }
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
            this._map = new MapWidget(this.type, this.shadowRoot!, this._mapContainer, this.showGeoCodingControl, this.showBoundaryBoxControl, this.useZoomControl, this.showGeoJson)
                .setCenter(this.center)
                .setZoom(this.zoom)
                .setControls(this.controls)
                .setGeoJson(this.geoJson);
            this._map.load().then(() => {
                // Get markers from slot
                this._observer = new FlattenedNodesObserver(this._slotElement!, (info: any) => {
                    this._processNewMarkers(info.addedNodes);
                    this._processRemovedMarkers(info.removedNodes);
                });
                this._resizeObserver?.disconnect();
                this._resizeObserver = new ResizeObserver(debounce(() => {
                    this.resize();
                }, 200));
                var container = this._mapContainer?.parentElement;
                if (container) {
                    this._resizeObserver.observe(container);
                }
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
