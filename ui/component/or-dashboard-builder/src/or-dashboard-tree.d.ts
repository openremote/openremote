import { LitElement, PropertyValues } from 'lit';
import '@openremote/or-icon';
import { Dashboard } from '@openremote/model';
import '@openremote/or-mwc-components/or-mwc-menu';
export declare class OrDashboardTree extends LitElement {
    static get styles(): import("lit").CSSResult[];
    protected dashboards?: Dashboard[];
    protected selected?: Dashboard;
    protected readonly realm: string;
    protected readonly userId: string;
    protected readonly readonly = true;
    protected readonly hasChanged = false;
    protected showControls: boolean;
    shouldUpdate(changedProperties: PropertyValues): boolean;
    updated(changedProperties: PropertyValues): void;
    private getAllDashboards;
    private selectDashboard;
    private createDashboard;
    protected duplicateDashboard(dashboard: Dashboard): void;
    protected _doDuplicateDashboard(dashboard: Dashboard): void;
    private deleteDashboard;
    protected onDashboardClick(dashboardId: string): void;
    protected showDiscardChangesModal(): Promise<boolean>;
    protected render(): import("lit-html").TemplateResult<1>;
}
