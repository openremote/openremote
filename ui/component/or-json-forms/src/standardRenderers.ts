import {
    and,
    computeLabel,
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
    isOneOfEnumControl,
    isStringControl,
    isTimeControl,
    JsonFormsRendererRegistryEntry,
    JsonSchema,
    Layout,
    mapDispatchToControlProps,
    mapDispatchToMultiEnumProps,
    mapStateToAllOfProps,
    mapStateToAnyOfProps,
    mapStateToControlProps,
    mapStateToLayoutProps,
    or,
    OwnPropsOfRenderer,
    RankedTester,
    rankWith,
    RendererProps,
    resolveSubSchemas,
    schemaMatches,
    schemaSubPathMatches,
    Tester,
    uiTypeIs
} from "@jsonforms/core";
import {html, TemplateResult} from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import {JsonFormsStateContext, WithRequired, WithTitle} from "./index";
import {findSchemaTitleAndDescription, getTemplateFromProps, mapStateToCombinatorRendererProps, toControlDetailProps} from "./util";
import "./layouts/layout-vertical-element";
import "./controls/control-input-element";
import "./controls/control-array-element";

const hasOneOfItems = (schema: JsonSchema): boolean =>
    schema.oneOf !== undefined &&
    schema.oneOf.length > 0 &&
    (schema.oneOf as JsonSchema[]).every((entry: JsonSchema) => {
        return entry.const !== undefined;
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

export const verticalLayoutTester: RankedTester = rankWith(
    1,
    uiTypeIs('VerticalLayout')
);

export const verticalLayoutRenderer = (state: JsonFormsStateContext, props: RendererProps & WithTitle & WithRequired) => {
    const p: RendererProps & WithTitle & WithRequired & DispatchPropsOfControl = {
        ...mapStateToLayoutProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch),
        title: props.title,
        required: props.required
    };

    const template = html`<or-json-forms-vertical-layout data-title="${p.title || ""}" .state="${state}" .props="${p}"></or-json-forms-vertical-layout>`;
    return getTemplateWrapper(template, state, p);
};

export const inputControlTester: RankedTester = rankWith(
    3,
    or(
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
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch),
        ...mapDispatchToMultiEnumProps(state.dispatch)
    };

    const template = html`<or-json-forms-input-control .state="${state}" .props="${props}"></or-json-forms-input-control>`;
    return getTemplateWrapper(template, state, props);
};

export const objectControlTester: RankedTester = rankWith(
    2,
    isObjectControl
);
export const objectControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    const {
        required,
        renderers,
        cells,
        uischemas,
        schema,
        label,
        path,
        visible,
        enabled,
        uischema,
        rootSchema
    } = toControlDetailProps(state, props);

    const detailUiSchema = findUISchema(
        uischemas!,
        schema,
        uischema.scope,
        path,
        'VerticalLayout',
        uischema,
        rootSchema
    );

    const labelStr = computeLabel(label, !!required, false);
    const contentProps: OwnPropsOfRenderer & WithTitle & WithRequired & DispatchPropsOfControl = {
        title: labelStr,
        visible: visible,
        enabled: enabled,
        schema: schema,
        uischema: detailUiSchema,
        path: path,
        handleChange: props.handleChange,
        renderers: renderers,
        cells: cells,
        required: required
    };
    return getTemplateFromProps(state, contentProps);
}

export const anyOfControlTester: RankedTester = rankWith(
    4,
    isAnyOfControl
);
export const anyOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {

    const {
        required,
        renderers,
        cells,
        uischemas,
        schema,
        label,
        path,
        visible,
        enabled,
        uischema,
        rootSchema
    } = toControlDetailProps(state, props);

    const anyOfSchema = resolveSubSchemas(schema, rootSchema, "anyOf");

    const anyOfProps = mapStateToCombinatorRendererProps({jsonforms: {...state}}, {
        schema: anyOfSchema,
        path: path,
        visible: visible,
        enabled: enabled,
        uischema: uischema,
        renderers: renderers,
        cells: cells,
        uischemas: uischemas
    }, "anyOf");

    if (anyOfProps.indexOfFittingSchema === undefined) {
        console.warn("Cannot match anyOf schema to instance data");
        return html`<or-json-forms-unknown class="item-container"></or-json-forms-unknown>`;
    }

    const anyOfRenderInfos = createCombinatorRenderInfos(
        anyOfSchema.anyOf!,
        rootSchema,
        "anyOf",
        anyOfProps.uischema || uischema,
        path,
        anyOfProps.uischemas
    );

    // Return template for the anyOf schema that matches the data
    const matchedSchema = anyOfRenderInfos[anyOfProps.indexOfFittingSchema].schema;
    let matchedUischema = anyOfRenderInfos[anyOfProps.indexOfFittingSchema].uischema;

    if (matchedSchema.allOf) {
        // Force the uischema to be a simple control so that the allOf renderer gets used
        matchedUischema = {
            type: 'Control',
            scope: "#",
            label: false
        } as ControlElement;
    }

    return getTemplateFromProps(state, {
        schema: matchedSchema,
        uischema: matchedUischema,
        path: path,
        renderers: renderers,
        cells: cells
    } as OwnPropsOfRenderer);
}

export const allOfControlTester: RankedTester = rankWith(
    4,
    isAllOfControl
);
export const allOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {

    const {
        required,
        renderers,
        cells,
        uischemas,
        schema,
        label,
        path,
        visible,
        enabled,
        uischema,
        rootSchema
    } = toControlDetailProps(state, props);

    const allOfSchema = resolveSubSchemas(schema, rootSchema, 'allOf');

    const allOfProps = mapStateToCombinatorRendererProps({jsonforms: {...state}}, {
        schema: allOfSchema,
        path: path,
        visible: visible,
        enabled: enabled,
        uischema: uischema,
        renderers: renderers,
        cells: cells,
        uischemas: uischemas
    }, "allOf");

    const allOfRenderInfos = createCombinatorRenderInfos(
        allOfSchema.allOf!,
        rootSchema,
        "allOf",
        allOfProps.uischema || uischema,
        path,
        allOfProps.uischemas
    );

    if (allOfRenderInfos.every(renderInfo => renderInfo.uischema.type === "VerticalLayout")) {
        const titleAndDescription = findSchemaTitleAndDescription(allOfSchema, rootSchema);

        // Combine into a single layout
        (allOfRenderInfos[0].uischema as Layout).elements = allOfRenderInfos.map(renderInfo => (renderInfo.uischema as Layout).elements).flat();
        allOfRenderInfos[0].schema.title = titleAndDescription[0];
        allOfRenderInfos[0].schema.description = titleAndDescription[1];

        return getTemplateFromProps(state, {
            schema: allOfRenderInfos[0].schema,
            uischema: allOfRenderInfos[0].uischema,
            path: path,
            renderers: renderers,
            cells: cells
        } as OwnPropsOfRenderer);
    }

    return allOfRenderInfos.map(renderInfo => getTemplateFromProps(state, {
        schema: renderInfo.schema,
        uischema: renderInfo.uischema,
        path: path,
        renderers: renderers,
        cells: cells
    } as OwnPropsOfRenderer));
}


export const arrayControlTester: RankedTester = rankWith(
    2,
    schemaMatches(
        schema => hasType(schema, 'array') && !Array.isArray(schema.items) // we don't care about tuples
    )
);
export const arrayControlRenderer = (state: JsonFormsStateContext, props: ControlProps & WithTitle) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const template = html`<or-json-forms-array-control .state="${state}" .props="${props}"></or-json-forms-array-control>`;
    return getTemplateWrapper(template, state, props);
};


export function getTemplateWrapper(elementTemplate: TemplateResult, state: JsonFormsStateContext, props: RendererProps & WithRequired): TemplateResult {

    const { handleChange } = mapDispatchToControlProps(state.dispatch);

    const deleteTemplate = props.required || !props.path ? `` : html`
                <button class="button-clear" @click="${() => handleChange(props.path, undefined)}"><or-icon icon="close-circle"></or-icon></input>
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

export const StandardRenderers: JsonFormsRendererRegistryEntry[] = [

    {tester: verticalLayoutTester, renderer: verticalLayoutRenderer},
    {tester: inputControlTester, renderer: inputControlRenderer},
    {tester: objectControlTester, renderer: objectControlRenderer},
    {tester: arrayControlTester, renderer: arrayControlRenderer},
    {tester: anyOfControlTester, renderer: anyOfControlRenderer},
    {tester: allOfControlTester, renderer: allOfControlRenderer}
];
