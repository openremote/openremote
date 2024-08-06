import { DashboardWidget } from "@openremote/model";
import { WidgetConfig } from "../util/widget-config";
import { WidgetManifest } from "../util/or-widget";
export declare class WidgetService {
    static getManifest(widgetTypeId: string): WidgetManifest;
    static placeNew(widgetTypeId: string, x: number, y: number): Promise<DashboardWidget>;
    static correctToConfigSpec(manifest: WidgetManifest, widgetConfig: WidgetConfig): WidgetConfig;
}
