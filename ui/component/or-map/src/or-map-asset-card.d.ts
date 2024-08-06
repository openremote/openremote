import { CSSResultGroup, LitElement, PropertyValues, TemplateResult } from "lit";
import { Asset, SharedEvent } from "@openremote/model";
import "@openremote/or-icon";
import { MapMarkerAssetConfig } from "./markers/or-map-marker-asset";
export interface MapAssetCardTypeConfig {
    include?: string[];
    exclude?: string[];
    hideViewAsset?: boolean;
}
export interface MapAssetCardConfig {
    default?: MapAssetCardTypeConfig;
    assetTypes?: {
        [assetType: string]: MapAssetCardTypeConfig;
    };
}
export declare class OrMapAssetCardLoadAssetEvent extends CustomEvent<string> {
    static readonly NAME = "or-map-asset-card-load-asset";
    constructor(assetId: string);
}
declare global {
    export interface HTMLElementEventMap {
        [OrMapAssetCardLoadAssetEvent.NAME]: OrMapAssetCardLoadAssetEvent;
    }
}
export declare const DefaultConfig: MapAssetCardConfig;
declare const OrMapAssetCard_base: (new (...args: any[]) => {
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
}) & typeof LitElement;
export declare class OrMapAssetCard extends OrMapAssetCard_base {
    assetId?: string;
    asset?: Asset;
    config?: MapAssetCardConfig;
    markerconfig?: MapMarkerAssetConfig;
    useAssetColor: boolean;
    static get styles(): CSSResultGroup;
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    _onEvent(event: SharedEvent): void;
    protected getCardConfig(): MapAssetCardTypeConfig | undefined;
    protected render(): TemplateResult | undefined;
    protected _loadAsset(assetId: string): void;
    protected getIcon(): string | undefined;
    protected getColor(): string | undefined;
}
export {};
