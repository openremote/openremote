import { LitElement } from "lit";
import { JsonFormsCellRendererRegistryEntry, JsonFormsRendererRegistryEntry, JsonFormsUISchemaRegistryEntry, JsonSchema, OwnPropsOfRenderer, UISchemaElement } from '@jsonforms/core';
import { JsonFormsStateContext } from "./index";
/**
 * Adds additional props for layouts which are normally only available for controls
 */
export interface AdditionalProps {
    label?: string;
    required?: boolean;
    errors?: string;
    minimal?: boolean;
    type?: string;
}
export declare abstract class BaseElement<T extends UISchemaElement, P extends OwnPropsOfRenderer> extends LitElement implements OwnPropsOfRenderer, AdditionalProps {
    state: JsonFormsStateContext;
    uischema: T;
    schema: JsonSchema;
    data: any;
    renderers?: JsonFormsRendererRegistryEntry[];
    cells?: JsonFormsCellRendererRegistryEntry[];
    config: any;
    uischemas?: JsonFormsUISchemaRegistryEntry[];
    enabled: boolean;
    visible: boolean;
    path: string;
    label: string;
    required: boolean;
    errors: string;
    set props(props: P);
}
