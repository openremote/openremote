import { LitElement, PropertyValues } from "lit";
import { FileInfo } from "@openremote/model";
import "./or-loading-indicator";
export declare class OrFileUploader extends LitElement {
    readonly src: string;
    readonly title: string;
    readonly accept: string;
    private loading;
    private files;
    get Files(): FileInfo[];
    static get styles(): import("lit").CSSResult;
    protected firstUpdated(_changedProperties: PropertyValues): void;
    _onChange(e: any): Promise<void>;
    render(): import("lit-html").TemplateResult<1>;
}
