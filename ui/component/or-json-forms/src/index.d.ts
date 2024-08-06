import { LitElement, PropertyValues } from "lit";
import { ErrorObject } from "ajv";
import { CoreActions, Dispatch, JsonFormsCellRendererRegistryEntry, JsonFormsCore, JsonFormsRendererRegistryEntry, JsonFormsSubStates, JsonFormsUISchemaRegistryEntry, JsonSchema, OwnPropsOfJsonFormsRenderer, UISchemaElement } from "@jsonforms/core";
import { getTemplateWrapper, StandardRenderers } from "./standard-renderers";
import { AdditionalProps } from "./base-element";
declare global {
    interface SymbolConstructor {
        readonly observable: symbol;
    }
}
export { ErrorObject, StandardRenderers, getTemplateWrapper, JsonFormsRendererRegistryEntry, UISchemaElement };
export interface JsonFormsStateContext extends JsonFormsSubStates {
    dispatch: Dispatch<CoreActions>;
}
export declare class OrJSONForms extends LitElement implements OwnPropsOfJsonFormsRenderer, AdditionalProps {
    uischema?: UISchemaElement;
    schema?: JsonSchema;
    data: any;
    renderers?: JsonFormsRendererRegistryEntry[];
    cells?: JsonFormsCellRendererRegistryEntry[];
    onChange?: (dataAndErrors: {
        errors: ErrorObject[] | undefined;
        data: any;
    }) => void;
    config: any;
    uischemas?: JsonFormsUISchemaRegistryEntry[];
    readonly: boolean;
    label: string;
    required: boolean;
    static get styles(): import("lit").CSSResult[];
    protected core?: JsonFormsCore;
    protected contextValue?: JsonFormsSubStates;
    protected previousData: any;
    protected previousErrors: ErrorObject[];
    checkValidity(): boolean;
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    updateCore<T extends CoreActions>(coreAction: T): T;
    render(): import("lit-html").TemplateResult;
}
