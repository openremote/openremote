import { Dashboard, DashboardScalingPreset, DashboardScreenPreset } from "@openremote/model";
import { LitElement } from "lit";
export declare class OrDashboardBoardsettings extends LitElement {
    protected readonly dashboard: Dashboard;
    protected readonly showPerms?: boolean;
    static get styles(): import("lit").CSSResult[];
    forceParentUpdate(force?: boolean): void;
    private setViewAccess;
    private setEditAccess;
    private setBreakpoint;
    private setRefreshInterval;
    protected render(): import("lit-html").TemplateResult<1>;
    setToMobilePreset(): void;
    scalingPresetTemplate(screenPresets: DashboardScreenPreset[], scalingPresets: {
        key: DashboardScalingPreset;
        value: string;
    }[]): import("lit-html").TemplateResult<1>;
    screenPresetTemplate(screenPresets: DashboardScreenPreset[], customLabels?: string[]): import("lit-html").TemplateResult<1>;
}
