import { LitElement, TemplateResult } from "lit";
import "./or-dashboard-widgetcontainer";
import { DashboardGridItem, DashboardScreenPreset, DashboardTemplate, DashboardWidget } from "@openremote/model";
import "@openremote/or-components/or-loading-indicator";
import { GridItemHTMLElement, GridStack, GridStackNode } from "gridstack";
export interface DashboardGridNode extends GridStackNode {
    widgetTypeId: string;
}
export interface DashboardPreviewSize {
    displayName: string;
    width?: number;
    height?: number;
}
export declare class OrDashboardPreview extends LitElement {
    set template(newValue: DashboardTemplate);
    private _template?;
    get template(): DashboardTemplate;
    protected readonly dashboardId?: string;
    protected realm?: string;
    protected selectedWidget: DashboardWidget | undefined;
    protected editMode: boolean;
    protected readonly: boolean;
    protected previewWidth?: string;
    protected previewHeight?: string;
    protected previewZoom: number;
    protected previewSize?: DashboardPreviewSize;
    protected availablePreviewSizes?: DashboardPreviewSize[];
    protected fullscreen: boolean;
    protected latestChanges?: {
        changedKeys: string[];
        oldValue: DashboardTemplate;
        newValue: DashboardTemplate;
    };
    protected activePreset?: DashboardScreenPreset;
    private rerenderActive;
    private isLoading;
    protected grid?: GridStack;
    protected latestDragWidgetStart?: Date;
    constructor();
    static get styles(): import("lit").CSSResult[];
    shouldUpdate(changedProperties: Map<PropertyKey, unknown>): boolean;
    updated(changedProperties: Map<string, any>): void;
    setupGrid(recreate: boolean, force?: boolean): Promise<void>;
    refreshPreview(): void;
    refreshWidgets(): void;
    protected selectGridItem(gridItem: GridItemHTMLElement): void;
    protected deselectGridItem(gridItem: GridItemHTMLElement): void;
    protected deselectGridItems(gridItems: GridItemHTMLElement[]): void;
    protected onGridItemClick(gridItem: DashboardGridItem | undefined): void;
    protected onFitToScreenClick(): void;
    protected isPreviewVisible(): boolean;
    protected render(): TemplateResult<1>;
    protected getGridstackColumns(grid: GridStack | undefined): number | undefined;
    protected isExtraLargeGrid(): boolean;
    private cachedGridstackCSS;
    protected applyCustomGridstackGridCSS(columns: number): TemplateResult;
    protected resizeObserver?: ResizeObserver;
    protected previousObserverEntry?: ResizeObserverEntry;
    disconnectedCallback(): void;
    protected setupResizeObserver(element: Element): ResizeObserver;
    protected resizeObserverCallback: ResizeObserverCallback;
    protected _onGridResize(): void;
    protected processTemplateChanges(changes: {
        changedKeys: string[];
        oldValue: DashboardTemplate;
        newValue: DashboardTemplate;
    }): void;
    protected waitUntil(conditionFunction: any): Promise<unknown>;
    protected onWidgetDrop(_ev: Event, _prevWidget: any, newWidget: DashboardGridNode | undefined): void;
}
