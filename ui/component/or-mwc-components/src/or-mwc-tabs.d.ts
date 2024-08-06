import { CSSResult, LitElement, TemplateResult } from "lit";
export interface OrMwcTabItem {
    name?: string;
    icon?: string;
    content?: TemplateResult | string;
}
export declare class OrMwcTabs extends LitElement {
    static get styles(): CSSResult[];
    protected index?: number;
    protected items?: OrMwcTabItem[];
    protected iconPosition?: "left" | "top";
    protected noScroll?: boolean;
    protected bgColor?: string;
    protected color?: string;
    protected styles?: CSSResult | string;
    private mdcTabBar;
    constructor();
    protected updated(changedProperties: Map<string, any>): void;
    protected render(): TemplateResult<1>;
}
