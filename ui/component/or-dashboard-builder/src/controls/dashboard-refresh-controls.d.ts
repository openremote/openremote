import { LitElement, PropertyValues, TemplateResult } from "lit";
import { DashboardRefreshInterval } from "@openremote/model";
export declare function intervalToMillis(interval: DashboardRefreshInterval): number | undefined;
export declare class IntervalSelectEvent extends CustomEvent<DashboardRefreshInterval> {
    static readonly NAME = "interval-select";
    constructor(interval: DashboardRefreshInterval);
}
export declare class DashboardRefreshControls extends LitElement {
    protected interval: DashboardRefreshInterval;
    protected readonly: boolean;
    protected intervalOptions: DashboardRefreshInterval[];
    protected willUpdate(changedProps: PropertyValues): void;
    protected render(): TemplateResult;
    protected onIntervalSelect(stringOptions: string[], value: string): void;
    protected getIntervalString(interval: DashboardRefreshInterval): string;
    protected getRefreshOptions(): string[];
}
