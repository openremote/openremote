import { LitElement, PropertyValues } from "lit";
export declare class WritableDropdown extends LitElement {
    value?: any;
    options: {
        value: any;
        name: string;
    }[];
    selectElement: HTMLSelectElement;
    static get styles(): import("lit").CSSResult;
    protected firstUpdated(): void;
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    protected render(): import("lit-html").TemplateResult<1>;
}
