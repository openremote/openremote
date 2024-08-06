import { CSSResultGroup, LitElement, PropertyValues } from "lit";
export declare class OrMapMarkerChangedEvent extends CustomEvent<OrMapMarkerChangedEventDetail> {
    static readonly NAME = "or-map-marker-changed";
    constructor(marker: OrMapMarker, prop: string);
}
export declare class OrMapMarkerClickedEvent extends CustomEvent<OrMapMarkerEventDetail> {
    static readonly NAME = "or-map-marker-clicked";
    constructor(marker: OrMapMarker);
}
export interface OrMapMarkerEventDetail {
    marker: OrMapMarker;
}
export interface OrMapMarkerChangedEventDetail extends OrMapMarkerEventDetail {
    property: string;
}
export interface TemplateOptions {
    displayValue?: string;
    direction?: string;
}
declare global {
    export interface HTMLElementEventMap {
        [OrMapMarkerChangedEvent.NAME]: OrMapMarkerChangedEvent;
        [OrMapMarkerClickedEvent.NAME]: OrMapMarkerClickedEvent;
    }
}
/**
 * Base class for all map markers.
 *
 * This component doesn't directly render anything instead it generates DOM that can be added to the map component
 */
export declare class OrMapMarker extends LitElement {
    static get styles(): CSSResultGroup;
    protected static _defaultTemplate: (icon: string | undefined, options?: TemplateOptions) => string;
    lat?: number;
    lng?: number;
    radius?: number;
    displayValue?: string;
    direction?: string;
    visible: boolean;
    icon?: string;
    color?: string;
    activeColor?: string;
    interactive: boolean;
    active: boolean;
    _actualMarkerElement?: HTMLDivElement;
    protected _slot?: HTMLSlotElement;
    get markerContainer(): HTMLDivElement | undefined;
    _onClick(e: MouseEvent): void;
    _createMarkerElement(): HTMLDivElement;
    /**
     * Override in sub types to customise the look of the marker. If undefined returned then a default marker will
     * be used instead.
     */
    createMarkerContent(): HTMLElement | undefined;
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    protected updateVisibility(container?: HTMLDivElement): void;
    protected getColor(): string | undefined;
    protected getActiveColor(): string | undefined;
    protected updateColor(container?: HTMLDivElement): void;
    protected updateActive(container?: HTMLDivElement): void;
    protected updateInteractive(container?: HTMLDivElement): void;
    protected refreshMarkerContent(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    protected _raisePropertyChange(prop: string): void;
    protected addMarkerClassNames(markerElement: HTMLElement): void;
    protected addMarkerContainerClassNames(markerContainer: HTMLElement): void;
    protected createDefaultMarkerContent(): HTMLElement;
    hasPosition(): boolean;
}
