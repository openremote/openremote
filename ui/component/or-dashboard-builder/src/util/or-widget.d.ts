import { CSSResult, LitElement, TemplateResult } from "lit";
import { WidgetConfig } from "./widget-config";
import { WidgetSettings } from "./widget-settings";
export interface WidgetManifest {
    displayName: string;
    displayIcon: string;
    minColumnWidth?: number;
    minColumnHeight?: number;
    minPixelWidth?: number;
    minPixelHeight?: number;
    getContentHtml(config: WidgetConfig): OrWidget;
    getSettingsHtml(config: WidgetConfig): WidgetSettings;
    getDefaultConfig(): WidgetConfig;
}
export declare abstract class OrWidget extends LitElement {
    protected static manifest: WidgetManifest;
    protected readonly widgetConfig: WidgetConfig;
    constructor(config: WidgetConfig);
    static get styles(): CSSResult[];
    static getManifest(): WidgetManifest;
    abstract refreshContent(force: boolean): void;
    protected abstract render(): TemplateResult;
    getDisplayName?: () => string | undefined;
    getEditMode?: () => boolean;
    getWidgetLocation?: () => {
        x?: number;
        y?: number;
        h?: number;
        w?: number;
    };
}
