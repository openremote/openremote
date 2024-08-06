import { LitElement, TemplateResult } from "lit";
export declare class SettingsPanel extends LitElement {
    protected expanded: boolean;
    protected displayName?: string;
    static get styles(): import("lit").CSSResult[];
    protected render(): TemplateResult;
    toggle(state?: boolean): void;
    protected generateHeader(expanded: boolean, title?: string): Promise<TemplateResult>;
}
