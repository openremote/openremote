import { CombinatorKeyword, ControlElement, JsonFormsState, JsonFormsSubStates, JsonSchema, OwnPropsOfControl, OwnPropsOfRenderer, StatePropsOfCombinator } from "@jsonforms/core";
import "@openremote/or-components/or-ace-editor";
import { TemplateResult } from "lit";
export declare function getTemplateFromProps<T extends OwnPropsOfRenderer>(state: JsonFormsSubStates | undefined, props: T | undefined): TemplateResult | undefined;
export interface CombinatorInfo {
    title: string;
    description: string;
    constProperty?: string;
    constValue?: any;
    defaultValueCreator: () => any;
}
/**
 * For a given anyOf schema array this will try and extract a common const property which can be used as a discriminator
 * when creating instances
 */
export declare function getCombinatorInfos(schemas: JsonSchema[], rootSchema: JsonSchema): CombinatorInfo[];
export declare function getSchemaConst(schema: JsonSchema): any;
export declare function getSchemaPicker(rootSchema: JsonSchema, resolvedSchema: JsonSchema, path: string, keyword: "anyOf" | "oneOf", label: string, selectedCallback: (selectedSchema: CombinatorInfo) => void): TemplateResult;
export declare function findSchemaTitleAndDescription(schema: JsonSchema, rootSchema: JsonSchema): [string | undefined, string | undefined];
/**
 * Copied from eclipse source code to inject global definitions into the validating schema otherwise AJV will fail
 * to compile the schema - not perfect but works for our cases
 */
export declare function mapStateToCombinatorRendererProps(state: JsonFormsState, ownProps: OwnPropsOfControl, keyword: CombinatorKeyword): StatePropsOfCombinator;
export declare function getLabel(schema: JsonSchema, rootSchema: JsonSchema, uiElementLabel?: string, uiElementScope?: string): string | undefined;
export declare function resolveSubSchemasRecursive(schema: JsonSchema, rootSchema: JsonSchema, keyword?: CombinatorKeyword): JsonSchema;
export declare const controlWithoutLabel: (scope: string) => ControlElement;
export declare const showJsonEditor: (title: string, value: any, updateCallback: (newValue: string) => void) => void;
