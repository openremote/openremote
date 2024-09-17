import {CSSResult, LitElement, TemplateResult } from "lit";
import { property } from "lit/decorators.js";
import {WidgetConfig} from "./widget-config";
import {WidgetSettings} from "./widget-settings";

export interface WidgetManifest {
    displayName: string,
    displayIcon: string,
    minColumnWidth?: number,
    minColumnHeight?: number,
    minPixelWidth?: number,
    minPixelHeight?: number
    getContentHtml(config: WidgetConfig): OrWidget,
    getSettingsHtml(config: WidgetConfig): WidgetSettings,
    getDefaultConfig(): WidgetConfig
}

// Main OrWidget class where all widgets extend their functionality on.
// It contains several methods used for rendering by the parent component; OrDashboardWidget
export abstract class OrWidget extends LitElement {

    protected static manifest: WidgetManifest;

    @property({type: Object})
    protected readonly widgetConfig!: WidgetConfig;

    constructor(config: WidgetConfig) {
        super();
        this.widgetConfig = config;
    }

    static get styles(): CSSResult[] {
        return [];
    }

    /* --------------------------- */

    static getManifest(): WidgetManifest {
        if (!this.manifest) {
            throw new Error(`No manifest present on ${this.name}`);
        }
        return this.manifest;
    }

    // Method used for refreshing the content in a widget.
    // This can be customized to lower the performance- and/or visual impact of a refresh.
    public abstract refreshContent(force: boolean): void;

    // WebComponent lifecycle method to render HTML content
    protected abstract render(): TemplateResult;


    /* ------------------------------------- */

    public getDisplayName?: () => string | undefined;

    public getEditMode?: () => boolean;

    public getWidgetLocation?: () => { x?: number, y?: number, h?: number, w?: number }
}
