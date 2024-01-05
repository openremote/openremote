import {
    and,
    ControlElement,
    ControlProps,
    createCombinatorRenderInfos,
    DispatchPropsOfControl,
    findUISchema,
    hasType,
    isAllOfControl,
    isAnyOfControl,
    isBooleanControl,
    isDateControl,
    isDateTimeControl,
    isEnumControl,
    isIntegerControl,
    isNumberControl,
    isObjectControl,
    isOneOfControl,
    isOneOfEnumControl,
    isStringControl,
    isTimeControl,
    JsonFormsRendererRegistryEntry,
    JsonSchema,
    mapDispatchToControlProps,
    mapStateToControlProps,
    mapStateToControlWithDetailProps,
    mapStateToLayoutProps,
    or,
    OwnPropsOfJsonFormsRenderer,
    OwnPropsOfRenderer,
    RankedTester,
    rankWith,
    RendererProps,
    resolveSubSchemas,
    schemaMatches,
    schemaSubPathMatches,
    StatePropsOfControlWithDetail,
    Tester,
    uiTypeIs
} from "@jsonforms/core";
import {html, TemplateResult} from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import {JsonFormsStateContext} from "./index";
import {
    getCombinatorInfos,
    getLabel,
    getSchemaConst, getSchemaPicker,
    getTemplateFromProps,
    mapStateToCombinatorRendererProps,
    showJsonEditor
} from "./util";
import "./layouts/layout-vertical-element";
import "./controls/control-input-element";
import "./controls/control-array-element";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {AdditionalProps} from "./base-element";
import {Util} from "@openremote/core";

const hasOneOfItems = (schema: JsonSchema): boolean =>
    schema.oneOf !== undefined &&
    schema.oneOf.length > 0 &&
    (schema.oneOf as JsonSchema[]).every((entry: JsonSchema) => {
        return getSchemaConst(entry) !== undefined;
    });

const hasEnumItems = (schema: JsonSchema): boolean =>
    Array.isArray(schema.enum);

export const isEnumArray: Tester = and(
    uiTypeIs('Control'),
    and(
        schemaMatches(
            schema =>
                hasType(schema, 'array') &&
                !Array.isArray(schema.items) &&
                schema.uniqueItems === true
        ),
        schemaSubPathMatches('items', schema => {
            return hasOneOfItems(schema) || hasEnumItems(schema);
        })
    )
);

export const verticalOrGroupLayoutTester: RankedTester = rankWith(
    1,
    or(
        uiTypeIs("VerticalLayout"),
        uiTypeIs("Group")
    )
);

export const verticalLayoutRenderer = (state: JsonFormsStateContext, props: OwnPropsOfJsonFormsRenderer & AdditionalProps) => {
    const contentProps: RendererProps & DispatchPropsOfControl & AdditionalProps = {
        ...mapStateToLayoutProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch),
        label: props.label,
        required: props.required,
        errors: props.errors,
        minimal: props.minimal,
        type: props.type
    };

    const template = html`<or-json-forms-vertical-layout .state="${state}" .props="${contentProps}"></or-json-forms-vertical-layout>`;
    let deleteHandler: undefined | (() => void);
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path || "", undefined);
        }
    }
    return getTemplateWrapper(template, deleteHandler);
};

export const constTester: RankedTester = rankWith(
    6,
    schemaMatches(schema => getSchemaConst(schema) !== undefined)
);
export const constRenderer = (state: JsonFormsStateContext, props: OwnPropsOfJsonFormsRenderer) => {
    // Don't render const
    return undefined;
};

export const inputControlTester: RankedTester = rankWith(
    3,
    or(
        schemaMatches(schema => Array.isArray(schema.type) && schema.type.length === 7),
        isStringControl,
        isBooleanControl,
        isNumberControl,
        isIntegerControl,
        isDateControl,
        isTimeControl,
        isDateTimeControl,
        isEnumControl,
        isOneOfEnumControl,
        isEnumArray,
    )
);
export const inputControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    const contentProps: ControlProps = {
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;

    const template = html`<or-json-forms-input-control .state="${state}" .props="${contentProps}"></or-json-forms-input-control>`;
    let deleteHandler: undefined | (() => void);
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path, undefined);
        }
    }
    return getTemplateWrapper(template, deleteHandler);
};

export const objectControlTester: RankedTester = rankWith(
    2,
    isObjectControl
);
export const objectControlRenderer = (state: JsonFormsStateContext, props: ControlProps & AdditionalProps) => {
    const {
        required,
        renderers,
        cells,
        uischemas,
        schema,
        label,
        errors,
        path,
        visible,
        enabled,
        uischema,
        rootSchema
    } = mapStateToControlWithDetailProps({jsonforms: {...state}}, props);

    const detailUiSchema = findUISchema(
        uischemas!,
        schema,
        uischema.scope,
        path,
        "VerticalLayout",
        uischema,
        rootSchema
    );

    const contentProps: OwnPropsOfRenderer & AdditionalProps = {
        visible: visible,
        enabled: enabled,
        schema: schema,
        uischema: detailUiSchema,
        path: path,
        renderers: renderers,
        cells: cells,
        label: props.label || getLabel(schema, rootSchema, label) || "",
        required: !!props.required || !!required,
        errors: errors,
        minimal: props.minimal
    };
    return getTemplateFromProps(state, contentProps);
}


export const anyOfOneOfControlTester: RankedTester = rankWith(
    4,
    or(
        isAnyOfControl,
        isOneOfControl
    )
);
export const anyOfOneOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps & AdditionalProps) => {

    const jsonFormsContext = {jsonforms: {...state}};

    const {
        required,
        renderers,
        cells,
        schema,
        label,
        path,
        errors,
        visible,
        enabled,
        uischema,
        rootSchema,
        data
    } = mapStateToControlWithDetailProps(jsonFormsContext, props);

    const keyword = schema.anyOf !== undefined ? "anyOf" : "oneOf";
    const resolvedSchema = resolveSubSchemas(schema, rootSchema, keyword);
    const resolvedProps = mapStateToCombinatorRendererProps(jsonFormsContext, props, keyword);

    const renderInfos = createCombinatorRenderInfos(
        resolvedSchema[keyword]!,
        rootSchema,
        keyword,
        resolvedProps.uischema || uischema,
        path,
        resolvedProps.uischemas
    );

    if (data !== undefined && data !== null && (resolvedProps.indexOfFittingSchema === undefined || resolvedProps.indexOfFittingSchema < 0)) {
        // Try and match the data using our own combinator info objects
        const combinatorInfos = getCombinatorInfos(resolvedSchema[keyword]!, rootSchema);

        const constProp = combinatorInfos.length > 0 ? combinatorInfos[0].constProperty : undefined;
        if (constProp && typeof data === "object" && data[constProp]) {
            const dataType = data[constProp];
            resolvedProps.indexOfFittingSchema = combinatorInfos.findIndex((combinatorInfo) => combinatorInfo.constValue === dataType);
        }
    }

    if (resolvedProps.indexOfFittingSchema === undefined || resolvedProps.indexOfFittingSchema < 0) {
        const { handleChange } = mapDispatchToControlProps(state.dispatch);

        if (data !== undefined && data !== null) {
            // We have data that doesn't match a schema so show invalid template
            console.warn("Cannot match " + keyword + " schema to instance data");

            const showJson = (ev: Event) => {
                ev.stopPropagation();

                showJsonEditor(label, data, ((newValue) => {
                    handleChange(path || "", newValue);
                }));
            };

            return html`
                <div class="item-container no-match-container"><span>${label}:</span><b><or-translate value="validation.noSchemaMatchFound"></b><or-mwc-input .type="${InputType.BUTTON}" outlined label="json" icon="pencil" @or-mwc-input-changed="${(ev: Event) => showJson(ev)}"></or-mwc-input></div>
            `;
        } else {
            // We have no data so show a schema picker
            return getSchemaPicker(rootSchema, resolvedSchema, path, keyword, props.label || label, (selectedSchema => handleChange(path, selectedSchema.defaultValueCreator())))
        }
    }

    // Return template for the anyOf/oneOf schema that matches the data
    const matchedSchema = renderInfos[resolvedProps.indexOfFittingSchema].schema;
    let matchedUischema = renderInfos[resolvedProps.indexOfFittingSchema].uischema;

    if (matchedSchema.allOf) {
        // Force the uischema to be a simple control so it goes through the allOf renderer
        matchedUischema = {
            type: 'Control',
            scope: "#",
            label: false
        } as ControlElement;
    }

    const contentProps: OwnPropsOfRenderer & AdditionalProps = {
        schema: matchedSchema,
        uischema: matchedUischema,
        path: path,
        renderers: renderers,
        cells: cells,
        label: props.label || getLabel(matchedSchema, rootSchema, label) || "",
        required: props.required || !!required,
        errors: errors,
        minimal: props.minimal,
        type: matchedSchema.title
    }

    return getTemplateFromProps(state, contentProps);
}

export const allOfControlTester: RankedTester = rankWith(
    4,
    isAllOfControl
);
export const allOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps & AdditionalProps) => {
    const jsonFormsContext = {jsonforms: {...state}};
    const contentProps: StatePropsOfControlWithDetail & AdditionalProps = {
        ...mapStateToControlWithDetailProps(jsonFormsContext, props)
    };

    // Merge the schemas
    const allOfSchema = resolveSubSchemas(contentProps.schema, contentProps.rootSchema, "allOf");
    contentProps.schema = (allOfSchema.allOf! as JsonSchema[]).reduce((accumulator, value) => Util.mergeObjects(accumulator, value, false));
    // Reset the uischema scope
    contentProps.uischema.scope = "#";

    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;
    contentProps.minimal = props.minimal;

    return getTemplateFromProps(state, contentProps);
}


export const arrayControlTester: RankedTester = rankWith(
    2,
    schemaMatches(
        schema => hasType(schema, 'array') && !Array.isArray(schema.items) // we don't care about tuples
    )
);
export const arrayControlRenderer = (state: JsonFormsStateContext, props: ControlProps & AdditionalProps) => {
    const contentProps: ControlProps & AdditionalProps = {
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;
    contentProps.minimal = props.minimal;

    const template = html`<or-json-forms-array-control .state="${state}" .props="${contentProps}"></or-json-forms-array-control>`;
    let deleteHandler: undefined | (() => void);
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path, undefined);
        }
    }
    return getTemplateWrapper(template, deleteHandler);
};


export function getTemplateWrapper(elementTemplate: TemplateResult, deleteHandler?: () => void): TemplateResult {

    const deleteTemplate = !deleteHandler ? `` : html`
                <button class="button-clear" @click="${deleteHandler}"><or-icon icon="close-circle"></or-icon></input>
            `;
    return html`
                <div class="item-container">
                    ${elementTemplate}
                    <div class="delete-container">
                        ${deleteTemplate}
                    </div>
                </div>
            `;
}

export function unknownTemplate() {
    return html`<span>No applicable renderer found!</span>`;
}

export const StandardRenderers: JsonFormsRendererRegistryEntry[] = [

    {tester: verticalOrGroupLayoutTester, renderer: verticalLayoutRenderer},
    {tester: constTester, renderer: constRenderer},
    {tester: inputControlTester, renderer: inputControlRenderer},
    {tester: objectControlTester, renderer: objectControlRenderer},
    {tester: arrayControlTester, renderer: arrayControlRenderer},
    {tester: anyOfOneOfControlTester, renderer: anyOfOneOfControlRenderer},
    {tester: allOfControlTester, renderer: allOfControlRenderer}
];
