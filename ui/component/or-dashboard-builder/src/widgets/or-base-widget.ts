import {DashboardWidget} from "@openremote/model";
import {TemplateResult} from "lit";

export interface OrWidgetConfig {

}

export interface OrWidgetEntity {
    DISPLAY_NAME: string,
    DISPLAY_MDI_ICON: string; // https://materialdesignicons.com;
    MIN_COLUMN_WIDTH: number;
    MIN_PIXEL_WIDTH: number;
    MIN_PIXEL_HEIGHT: number;

    getDefaultConfig: (widget: DashboardWidget) => OrWidgetConfig;
    verifyConfigSpec: (widget: DashboardWidget) => OrWidgetConfig;

    getWidgetHTML: (widget: DashboardWidget, editMode: boolean, realm: string) => TemplateResult;
    getSettingsHTML: (widget: DashboardWidget, realm: string) => TemplateResult;
}
