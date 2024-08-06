import { LitElement, PropertyValues } from "lit";
import { DashboardWidget } from "@openremote/model";
import { OrWidget, WidgetManifest } from "./util/or-widget";
import { WidgetConfig } from "./util/widget-config";
export declare class OrDashboardWidgetContainer extends LitElement {
    static tagName: string;
    protected readonly widget: DashboardWidget;
    protected readonly editMode: boolean;
    protected loading: boolean;
    protected orWidget?: OrWidget;
    protected error?: string;
    protected containerElem?: Element;
    protected resizeObserver?: ResizeObserver;
    protected manifest?: WidgetManifest;
    static get styles(): import("lit").CSSResult[];
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown>): boolean;
    willUpdate(changedProps: Map<string, any>): void;
    firstUpdated(changedProps: PropertyValues): void;
    protected initializeWidgetElem(manifest: WidgetManifest, config: WidgetConfig): void;
    protected render(): import("lit-html").TemplateResult<1>;
    refreshContent(force: boolean): void;
}
