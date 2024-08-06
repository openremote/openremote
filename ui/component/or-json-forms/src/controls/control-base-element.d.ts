import { ControlElement, ControlProps, JsonSchema, OwnPropsOfControl } from "@jsonforms/core";
import { PropertyValues } from "lit";
import { BaseElement } from "../base-element";
export declare abstract class ControlBaseElement extends BaseElement<ControlElement, ControlProps> implements OwnPropsOfControl, ControlProps {
    description?: string | undefined;
    rootSchema: JsonSchema;
    handleChange: (path: string, data: any) => void;
    constructor();
    updated(_changedProperties: PropertyValues): void;
    shouldUpdate(changedProperties: PropertyValues): boolean;
    disconnectedCallback(): void;
}
