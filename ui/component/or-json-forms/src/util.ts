import {
    CombinatorKeyword,
    composeWithUi,
    ControlElement,
    ControlProps,
    createDefaultValue,
    deriveTypes,
    getAjv,
    getData,
    getRenderers,
    getSchema,
    hasShowRule,
    isVisible,
    JsonFormsRendererRegistryEntry,
    JsonFormsState,
    JsonFormsSubStates,
    JsonSchema,
    JsonSchema4,
    OwnPropsOfControl,
    OwnPropsOfRenderer,
    Resolve,
    resolveSchema,
    resolveSubSchemas,
    StatePropsOfCombinator,
} from "@jsonforms/core";
import {DefaultColor5, Util} from "@openremote/core";
import { InputType, OrInputChangedEvent, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-components/or-ace-editor";
import {OrAceEditor, OrAceEditorChangedEvent} from "@openremote/or-components/or-ace-editor";
import {html, TemplateResult, unsafeCSS} from "lit";
import {createRef, Ref, ref} from 'lit/directives/ref.js';
import {ErrorObject} from "./index";
import {unknownTemplate} from "./standard-renderers";
import {OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {AdditionalProps} from "./base-element";

export function getTemplateFromProps<T extends OwnPropsOfRenderer>(state: JsonFormsSubStates | undefined, props: T | undefined): TemplateResult | undefined {
    if (!state || !props) {
        return html``;
    }

    const renderers = props.renderers || getRenderers({jsonforms: {...state}});
    const schema = props.schema;
    const uischema = props.uischema;

    let template: TemplateResult | undefined;

    if (renderers && schema && uischema) {
        const orderedRenderers: [JsonFormsRendererRegistryEntry, number][] = renderers.map(r => [r, r.tester(uischema, schema)] as [JsonFormsRendererRegistryEntry, number]).sort((a,b) => b[1] - a[1]);
        const renderer = orderedRenderers && orderedRenderers.length > 0 ? orderedRenderers[0] : undefined;
        if (renderer && renderer[1] !== -1) {
            template = renderer[0].renderer(state, props) as TemplateResult;
        } else {
            template = unknownTemplate();
        }
    }

    return template;
}

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
export function getCombinatorInfos(schemas: JsonSchema[], rootSchema: JsonSchema): CombinatorInfo[] {

    return schemas.map(schema => {
        let constProperty: string | undefined;
        let constValue: any | undefined;
        let creator: () => any;
        const titleAndDescription = findSchemaTitleAndDescription(schema, rootSchema);

        if (schema.$ref) {
            schema = Resolve.schema(rootSchema, schema.$ref);
        }

        if (Array.isArray(schema.allOf)) {
            schema = resolveSubSchemas(schema, rootSchema, "allOf");
        }

        if (deriveTypes(schema).every(type => type === "object")) {
            const props = getSchemaObjectProperties(schema);
            const constProp = props.find(([propName, propSchema]) => getSchemaConst(propSchema) !== undefined);
            if (constProp) {
                constProperty = constProp[0];
                constValue = getSchemaConst(constProp[1]);

                creator = () => {
                    const obj: any = {};
                    obj[constProp[0]] = getSchemaConst(constProp[1]);
                    return obj;
                }

                if (!titleAndDescription[0]) {
                    titleAndDescription[0] = getSchemaConst(constProp[1]);
                }
            } else {
                creator = () => createDefaultValue(schema);
            }
        } else {
            // Assume a primitive type that can be instantiated with default value creator
            creator = () => createDefaultValue(schema);
        }

        return {
            title: titleAndDescription[0],
            description: titleAndDescription[1],
            defaultValueCreator: creator,
            constProperty: constProperty,
            constValue: constValue
        } as CombinatorInfo;
    });
}

export function getSchemaConst(schema: JsonSchema): any {
    if (!schema) {
        return;
    }

    if (schema.const !== undefined) {
        return schema.const;
    }

    if (Array.isArray(schema.enum) && schema.enum!.length === 1) {
        return schema.enum[0];
    }
}

export function getSchemaPicker(rootSchema: JsonSchema, resolvedSchema: JsonSchema, path: string, keyword: "anyOf" | "oneOf", label: string, selectedCallback: (selectedSchema: CombinatorInfo) => void): TemplateResult {
    const combinatorInfos = getCombinatorInfos(resolvedSchema[keyword]!, rootSchema);
    const options: [string, string][] = combinatorInfos.map((combinatorInfo, index) => [index+"", combinatorInfo.title || i18next.t("schema.title.indexedItem", {index: index})]);
    const pickerUpdater = (index: number) => {
        const matchedInfo = combinatorInfos[index];
        selectedCallback(matchedInfo);
    };
    const pickerLabel = label ? i18next.t("schema.anyOfPickerLabel", {label: label}) : i18next.t("type");
    return html`                
        <or-mwc-input class="any-of-picker" .label="${pickerLabel}" .type="${InputType.SELECT}" .options="${options}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => pickerUpdater(Number(ev.detail.value))}"></or-mwc-input>
    `;
}

export function findSchemaTitleAndDescription(schema: JsonSchema, rootSchema: JsonSchema): [string | undefined, string | undefined] {
    let title: string | undefined;

    if (schema.$ref) {
        title = getLabelFromScopeOrRef(schema.$ref);
        schema = Resolve.schema(rootSchema, schema.$ref);
    }

    if (schema.title) {
        return [schema.title, schema.description];
    }

    if (schema.allOf) {
        const resolvedSchema = resolveSubSchemas(schema, rootSchema, "allOf");
        const titledSchema = (resolvedSchema.allOf! as JsonSchema[]).find((allOfSchema) => {
            return !!allOfSchema.title;
        });
        if (titledSchema) {
            return [titledSchema.title, titledSchema.description];
        }
    }

    return [i18next.t("schema.title." + title, {defaultValue: Util.camelCaseToSentenceCase(title)}), undefined];
}

function getLabelFromScopeOrRef(scopeOrRef: string) {
    return scopeOrRef.substr(scopeOrRef.lastIndexOf("/")+1);
}

function getSchemaObjectProperties(schema: JsonSchema): [string, JsonSchema][] {
    let props: [string, JsonSchema][] = [];

    if (schema.allOf) {
        props = schema.allOf.map(schema => schema.properties ? Object.entries(schema.properties) : []).flat();
    } else if (schema.properties) {
        props = Object.entries(schema.properties);
    }

    return props;
}

/**
 * Copied from eclipse source code to inject global definitions into the validating schema otherwise AJV will fail
 * to compile the schema - not perfect but works for our cases
 */
export function mapStateToCombinatorRendererProps(
    state: JsonFormsState,
    ownProps: OwnPropsOfControl,
    keyword: CombinatorKeyword
): StatePropsOfCombinator {
    const { uischema } = ownProps;
    const path = composeWithUi(uischema!, ownProps.path!);
    const rootSchema = getSchema(state);
    const resolvedSchema = Resolve.schema(
        ownProps.schema || rootSchema,
        uischema!.scope,
        rootSchema
    );
    const visible: boolean =
        ownProps.visible === undefined || hasShowRule(uischema!)
            ? isVisible(uischema!, getData(state), ownProps.path!, getAjv(state))
            : ownProps.visible;
    const id = ownProps.id!;

    const data = Resolve.data(getData(state), path);

    const ajv = state.jsonforms.core!.ajv!;
    const schema = resolvedSchema || rootSchema;
    const _schema = resolveSubSchemas(schema, rootSchema, keyword);
    const structuralKeywords = [
        'required',
        'additionalProperties',
        'type',
        'enum',
        'const'
    ];
    const dataIsValid = (errors: ErrorObject[]): boolean => {
        return (
            !errors ||
            errors.length === 0 ||
            !errors.find(e => structuralKeywords.indexOf(e.keyword) !== -1)
        );
    };
    let indexOfFittingSchema: number = -1;
    // TODO instead of compiling the combinator subschemas we can compile the original schema
    // without the combinator alternatives and then revalidate and check the errors for the
    // element
    for (let i = 0; i < _schema[keyword]!.length; i++) {
        try {
            const schema = {
                definitions: rootSchema.definitions,
                ..._schema[keyword]![i]
            } as JsonSchema;
            const valFn = ajv.compile(schema);
            valFn(data);
            if (dataIsValid(valFn.errors!)) {
                indexOfFittingSchema = i;
                break;
            }
        } catch (error) {
            console.debug("Combinator subschema is not self contained, can't hand it over to AJV");
        }
    }

    return {
        data,
        path,
        schema,
        rootSchema,
        visible,
        id,
        indexOfFittingSchema,
        uischemas: state.jsonforms.uischemas!,
        uischema
    };
}

export function getLabel(schema: JsonSchema, rootSchema: JsonSchema, uiElementLabel?: string, uiElementScope?: string): string | undefined {
    if (uiElementLabel) {
        return uiElementLabel;
    }

    const titleAndDesc = findSchemaTitleAndDescription(schema, rootSchema);

    if (titleAndDesc[0]) {
        return titleAndDesc[0];
    }

    if (uiElementScope) {
        return Util.camelCaseToSentenceCase(getLabelFromScopeOrRef(uiElementScope));
    }

    return undefined;
}

export function resolveSubSchemasRecursive(
    schema: JsonSchema,
    rootSchema: JsonSchema,
    keyword?: CombinatorKeyword
): JsonSchema {
    const combinators: string[] = keyword ? [keyword] : ["allOf", "anyOf", "oneOf"];

    if (schema.$ref) {
        return resolveSubSchemasRecursive(resolveSchema(rootSchema, schema.$ref), rootSchema);
    }

    combinators.forEach((combinator) => {
        const schemas = (schema as any)[combinator] as JsonSchema[];

        if (schemas) {
            (schema as any)[combinator] = schemas.map(subSchema =>
                resolveSubSchemasRecursive(subSchema, rootSchema)
            );
        }
    });

    if (schema.items) {
        if (Array.isArray(schema.items)) {
            schema.items = (schema.items as JsonSchema4[]).map((itemSchema) => resolveSubSchemasRecursive(itemSchema, rootSchema) as JsonSchema4);
        } else {
            schema.items = resolveSubSchemasRecursive(schema.items as JsonSchema, rootSchema);
        }
    }

    if (schema.properties) {
        Object.keys(schema.properties).forEach((prop) =>
            schema.properties![prop] = resolveSubSchemasRecursive(schema.properties![prop], rootSchema)
        );
    }

    return schema;
}

export const controlWithoutLabel = (scope: string): ControlElement => ({
    type: 'Control',
    scope: scope,
    label: false
});

export const showJsonEditor = (title: string, value: any, updateCallback: (newValue: string) => void): void => {

    const editorRef: Ref<OrAceEditor> = createRef();
    const updateBtnRef: Ref<OrMwcInput> = createRef();
    const onEditorEdit = () => {
        // Disable update button whilst edit in progress
        updateBtnRef.value!.disabled = true;
    };
    const onEditorChanged = (ev: OrAceEditorChangedEvent) => {
        const valid = ev.detail.valid;
        updateBtnRef.value!.disabled = !valid;
    };

    const dialog = showDialog(new OrMwcDialog()
        .setContent(html`
                <or-ace-editor ${ref(editorRef)} @or-ace-editor-edit="${() => onEditorEdit()}" @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => onEditorChanged(ev)}" .value="${value}"></or-ace-editor>
            `)
        .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
                },
                {
                    default: true,
                    actionName: "update",
                    action: () => {
                        const editor = editorRef.value!;
                        if (editor.validate()) {
                            const data = !!editor.getValue() ? JSON.parse(editor.getValue()!) : undefined;
                            updateCallback(data);
                        }
                    },
                    content: html`<or-mwc-input ${ref(updateBtnRef)} disabled .type="${InputType.BUTTON}" label="update"></or-mwc-input>`
                }
            ])
        .setHeading(title)
        .setDismissAction(null)
        .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        width: 1024px;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                        padding: 0;
                        overflow: visible;
                        height: 60vh;
                    }
                </style>
            `));
};
