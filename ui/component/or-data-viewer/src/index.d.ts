import { LitElement, PropertyValues, TemplateResult } from "lit";
import "@openremote/or-chart";
import "@openremote/or-translate";
import "@openremote/or-components/or-panel";
import { OrChartConfig } from "@openremote/or-chart";
import { Asset, Attribute } from "@openremote/model";
import "@openremote/or-attribute-card";
export type PanelType = "chart" | "kpi";
export interface DefaultAssets {
    assetId?: string;
    attributes?: string[];
}
export interface PanelConfig {
    type?: PanelType;
    hide?: boolean;
    hideOnMobile?: boolean;
    defaults?: DefaultAssets[];
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: {
        [style: string]: string;
    };
    fieldStyles?: {
        [field: string]: {
            [style: string]: string;
        };
    };
}
export interface DataViewerConfig {
    panels: {
        [name: string]: PanelConfig;
    };
    viewerStyles?: {
        [style: string]: string;
    };
    propertyViewProvider?: (property: string, value: any, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute<any>, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (attributes: Attribute<any>[], panelName: string, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    chartConfig?: OrChartConfig;
}
export declare class OrDataViewerRenderCompleteEvent extends CustomEvent<void> {
    static readonly NAME = "or-data-viewer-render-complete-event";
    constructor();
}
export declare class OrDataViewerConfigInvalidEvent extends CustomEvent<void> {
    static readonly NAME = "or-data-viewer-config-invalid-event";
    constructor();
}
declare global {
    export interface HTMLElementEventMap {
        [OrDataViewerRenderCompleteEvent.NAME]: OrDataViewerRenderCompleteEvent;
        [OrDataViewerConfigInvalidEvent.NAME]: OrDataViewerConfigInvalidEvent;
    }
}
declare const OrDataViewer_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrDataViewer extends OrDataViewer_base {
    static get styles(): import("lit").CSSResult[];
    static DEFAULT_PANEL_TYPE: PanelType;
    static DEFAULT_CONFIG: DataViewerConfig;
    static generateGrid(shadowRoot: ShadowRoot | null): void;
    config?: DataViewerConfig;
    protected _assets?: Asset[];
    protected _loading: boolean;
    realm?: string;
    protected _resizeHandler: () => void;
    constructor();
    connectedCallback(): void;
    disconnectedCallback(): void;
    refresh(): void;
    getPanel(name: string, panelConfig: PanelConfig): TemplateResult;
    getPanelContent(panelName: string, panelConfig: PanelConfig): TemplateResult | undefined;
    protected render(): TemplateResult<1>;
    protected renderConfig(): TemplateResult[];
    protected updated(_changedProperties: PropertyValues): void;
}
export {};
