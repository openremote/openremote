import { LitElement } from "lit";
export declare class OrCollapsiblePanel extends LitElement {
    static get styles(): import("lit").CSSResult[];
    expanded: boolean;
    expandable: boolean;
    protected headerElem: HTMLDivElement;
    protected _onHeaderClicked(ev: MouseEvent): void;
    render(): import("lit-html").TemplateResult<1>;
}
