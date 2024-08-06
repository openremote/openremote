import { DashboardWidget } from "@openremote/model";
import { LitElement, TemplateResult } from "lit";
import { WidgetSettings, WidgetSettingsChangedEvent } from "./util/widget-settings";
import { WidgetManifest } from "./util/or-widget";
export declare class OrDashboardWidgetsettings extends LitElement {
    static get styles(): import("lit").CSSResult[];
    protected selectedWidget: DashboardWidget;
    protected settingsElem?: WidgetSettings;
    forceParentUpdate(changes: Map<string, any>, force?: boolean): void;
    protected render(): TemplateResult<1>;
    protected setDisplayName(name?: string): void;
    protected generateContent(widgetTypeId: string): Promise<TemplateResult>;
    protected initSettings(manifest: WidgetManifest): WidgetSettings;
    protected onWidgetConfigChange(ev: WidgetSettingsChangedEvent): void;
}
