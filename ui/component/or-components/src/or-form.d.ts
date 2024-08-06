import { LitElement, PropertyValues } from "lit";
/**
 * This is a form element that supports any element that has a value property
 */
export declare class OrForm extends LitElement {
    protected formNodes: Node[];
    protected firstUpdated(_changedProperties: PropertyValues): void;
    render(): import("lit-html").TemplateResult<1>;
    checkValidity(): boolean;
    reportValidity(): boolean;
    submit(): {
        [key: string]: any;
    };
    reset(): void;
}
