import { PropertyValues } from "lit";
import { OrMapMarker } from "./or-map-marker";
import { Asset, GeoJSONPoint, SharedEvent } from "@openremote/model";
export type MapMarkerConfig = {
    attributeName: string;
    showLabel?: boolean;
    showUnits?: boolean;
    hideDirection?: boolean;
    colours?: MapMarkerColours;
};
export type MapMarkerColours = AttributeMarkerColours | RangeAttributeMarkerColours;
export type MapMarkerAssetConfig = {
    [assetType: string]: MapMarkerConfig;
};
export type AttributeMarkerColours = {
    type: "string" | "boolean";
    [value: string]: string;
};
export type RangeAttributeMarkerColours = {
    type: "range";
    ranges: AttributeMarkerColoursRange[];
};
export type AttributeMarkerColoursRange = {
    min: number;
    colour: string;
};
export declare function getMarkerConfigForAssetType(config: MapMarkerAssetConfig | undefined, assetType: string | undefined): MapMarkerConfig | undefined;
export declare function getMarkerConfigAttributeName(config: MapMarkerAssetConfig | undefined, assetType: string | undefined): string | undefined;
declare const OrMapMarkerAsset_base: (new (...args: any[]) => {
    _connectRequested: boolean;
    _subscriptionIds?: string[] | undefined;
    _assetIds?: string[] | undefined;
    _attributeRefs?: import("@openremote/model").AttributeRef[] | undefined;
    _status: import("@openremote/core").EventProviderStatus;
    _statusCallback: (status: import("@openremote/core").EventProviderStatus) => void;
    connectedCallback(): void;
    disconnectedCallback(): void;
    connectEvents(): void;
    disconnectEvents(): void;
    _doConnect(): Promise<void>;
    readonly eventsConnected: boolean;
    _onEventProviderStatusChanged(status: import("@openremote/core").EventProviderStatus): void;
    _onEventsConnect(): void;
    _onEventsDisconnect(): void;
    _addEventSubscriptions(): Promise<void>;
    _removeEventSubscriptions(): void;
    _refreshEventSubscriptions(): void;
    assetIds: string[] | undefined;
    attributeRefs: import("@openremote/model").AttributeRef[] | undefined;
    _sendEvent(event: SharedEvent): void;
    _sendEventWithReply<U extends SharedEvent, V extends SharedEvent>(event: import("@openremote/model").EventRequestResponseWrapper<U>): Promise<V>;
    onEventsConnect(): void;
    onEventsDisconnect(): void;
    _onEvent(event: SharedEvent): void;
    requestUpdate(name?: PropertyKey | undefined, oldValue?: unknown): void;
    readonly isConnected: boolean;
}) & typeof OrMapMarker;
export declare class OrMapMarkerAsset extends OrMapMarkerAsset_base {
    assetId?: string;
    asset?: Asset;
    config?: MapMarkerAssetConfig;
    assetTypeAsIcon: boolean;
    constructor();
    protected markerColor?: string;
    protected set type(type: string | undefined);
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    /**
     * This will only get called when assetId is set; if asset is set then it is expected that attribute changes are
     * handled outside this component and the asset should be replaced when attributes change that require the marker
     * to re-render
     */
    _onEvent(event: SharedEvent): void;
    protected onAssetChanged(asset?: Asset): Promise<void>;
    protected _updateLocation(location: GeoJSONPoint | null): void;
    protected getColor(): string | undefined;
    protected getActiveColor(): string | undefined;
}
export {};
