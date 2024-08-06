import { LitElement, TemplateResult } from "lit";
/**
 * A simple loading wrapper around some other content that will hide the content whilst loading property is true
 */
export declare class OrLoadingWrapper extends LitElement {
    loadingHeight?: number;
    loadDom: boolean;
    fadeContent: boolean;
    loading: boolean;
    content?: TemplateResult;
    static get styles(): import("lit").CSSResult;
    render(): TemplateResult<1>;
}
