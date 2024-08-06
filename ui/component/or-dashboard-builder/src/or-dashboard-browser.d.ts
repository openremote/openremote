import { LitElement } from "lit";
export declare class OrDashboardBrowser extends LitElement {
    private sidebarGrid?;
    private backgroundGrid?;
    static get styles(): import("lit").CSSResult[];
    constructor();
    protected renderGrid(): void;
    protected render(): import("lit-html").TemplateResult<1>;
}
