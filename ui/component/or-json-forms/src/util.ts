import {
    CombinatorKeyword,
    composeWithUi,
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
    JsonSchema,
    mapStateToControlProps,
    mapStateToControlWithDetailProps,
    OwnPropsOfControl,
    OwnPropsOfRenderer,
    Resolve,
    resolveSubSchemas,
    StatePropsOfCombinator,
    StatePropsOfControl,
    StatePropsOfControlWithDetail,
} from "@jsonforms/core";
import { ErrorObject } from "ajv";
import {html, TemplateResult} from "lit";
import {JsonFormsStateContext} from "./index";

export function getTemplateFromProps<T extends OwnPropsOfRenderer>(state: JsonFormsStateContext | undefined, props: T | undefined): TemplateResult {
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
            template = html`<or-json-forms-unknown class="item-container"></or-json-forms-unknown>`;
        }
    }

    return template || html``;
}

export function toControlDetailProps(context: JsonFormsStateContext, props: OwnPropsOfControl): StatePropsOfControlWithDetail {
    return mapStateToControlWithDetailProps({ jsonforms: { ...context } }, props);
}

export function toControlProps(context: JsonFormsStateContext, props: OwnPropsOfControl): StatePropsOfControl {
    return mapStateToControlProps({ jsonforms: { ...context } }, props)
}

export interface AnyOfInfo {
    title: string;
    description: string;
    defaultValueCreator: () => any;
}

/**
 * For a given anyOf schema array this will try and extract a common const property which can be used as a discriminator
 * when creating instances
 */
export function getAnyOfInfos(anyOfSchemas: JsonSchema[], rootSchema: JsonSchema): AnyOfInfo[] {
    // Find const properties of first schema

    return anyOfSchemas.map(schema => {
        const titleAndDescription = findSchemaTitleAndDescription(schema, rootSchema);
        let creator: () => any;

        if (schema.$ref) {
            schema = Resolve.schema(rootSchema, schema.$ref);
        }

        if (Array.isArray(schema.allOf)) {
            schema = resolveSubSchemas(schema, rootSchema, "allOf");
        }

        if (deriveTypes(schema).every(type => type === "object")) {
            const props = getSchemaObjectProperties(schema);
            const constProp = props.find(([propName, propSchema]) => propSchema.const !== undefined);
            if (constProp) {
                creator = () => {
                    const obj: any = {};
                    obj[constProp[0]] = constProp[1].const;
                    return obj;
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
            defaultValueCreator: creator
        } as AnyOfInfo;
    });
}

export function findSchemaTitleAndDescription(schema: JsonSchema, rootSchema: JsonSchema): [string | undefined, string | undefined] {
    let refTitle: string | undefined;

    if (schema.$ref) {
        refTitle = schema.$ref.substr(schema.$ref.lastIndexOf("/")+1);
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

    return [refTitle, undefined];
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
 * Copied from eclipse source code as there is a bug (https://github.com/eclipsesource/jsonforms/issues/1787)
 * which means indexOfFittingSchema always returns 0 if there is only a const error
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
            const valFn = ajv.compile(_schema[keyword]![i]);
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

