import { LitElement, TemplateResult } from "lit";
export declare class OrPanel extends LitElement {
    static get styles(): import("lit").CSSResult[];
    zLevel?: number;
    heading?: string | TemplateResult;
    protected _panel: HTMLDivElement;
    render(): TemplateResult<1>;
}
