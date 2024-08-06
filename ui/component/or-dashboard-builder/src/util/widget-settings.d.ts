import { LitElement, PropertyValues, TemplateResult } from "lit";
import { WidgetConfig } from "./widget-config";
export declare class WidgetSettingsChangedEvent extends CustomEvent<WidgetConfig> {
    static readonly NAME = "settings-changed";
    constructor(widgetConfig: WidgetConfig);
}
export declare abstract class WidgetSettings extends LitElement {
    protected readonly widgetConfig: WidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected abstract render(): TemplateResult;
    constructor(config: WidgetConfig);
    protected willUpdate(changedProps: PropertyValues): void;
    protected notifyConfigUpdate(): void;
    getDisplayName?: () => string | undefined;
    setDisplayName?: (name?: string) => void;
    getEditMode?: () => boolean;
    getWidgetLocation?: () => {
        x?: number;
        y?: number;
        h?: number;
        w?: number;
    };
}
