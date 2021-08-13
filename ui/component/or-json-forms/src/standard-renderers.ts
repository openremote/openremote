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
    Tester,
    uiTypeIs
} from "@jsonforms/core";
import {html, TemplateResult} from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import {JsonFormsStateContext} from "./index";
import {getCombinatorInfos, getLabel, getTemplateFromProps, mapStateToCombinatorRendererProps} from "./util";
import "./layouts/layout-vertical-element";
import "./controls/control-input-element";
import "./controls/control-array-element";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {WithLabelAndRequired} from "./base-element";
import {Util} from "@openremote/core";

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

export const verticalOrGroupLayoutTester: RankedTester = rankWith(
    1,
    or(
        uiTypeIs("VerticalLayout"),
        uiTypeIs("Group")
    )
);

export const verticalLayoutRenderer = (state: JsonFormsStateContext, props: OwnPropsOfJsonFormsRenderer & WithLabelAndRequired) => {
    const contentProps: RendererProps & DispatchPropsOfControl & WithLabelAndRequired = {
        ...mapStateToLayoutProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch),
        label: props.label,
        required: props.required
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

    const contentProps: OwnPropsOfRenderer & WithLabelAndRequired = {
        visible: visible,
        enabled: enabled,
        schema: schema,
        uischema: detailUiSchema,
        path: path,
        renderers: renderers,
        cells: cells,
        label: props.label || getLabel(schema, rootSchema, label) || "",
        required: !!props.required || !!required
    };
    return getTemplateFromProps(state, contentProps);
}


export const anyOfControlTester: RankedTester = rankWith(
    4,
    isAnyOfControl
);
export const anyOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {

    const jsonFormsContext = {jsonforms: {...state}};

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
    } = mapStateToControlWithDetailProps(jsonFormsContext, props);

    const anyOfSchema = resolveSubSchemas(schema, rootSchema, "anyOf");
    const anyOfProps = mapStateToCombinatorRendererProps(jsonFormsContext, props, "anyOf");

    const anyOfRenderInfos = createCombinatorRenderInfos(
        anyOfSchema.anyOf!,
        rootSchema,
        "anyOf",
        anyOfProps.uischema || uischema,
        path,
        anyOfProps.uischemas
    );

    if (anyOfProps.indexOfFittingSchema === undefined || anyOfProps.indexOfFittingSchema < 0) {
        if (props.data !== undefined) {
            // We have data that doesn't match a schema so show invalid template
            console.warn("Cannot match anyOf schema to instance data");
            return invalidTemplate();
        } else {
            // We have no data so show a schema picker
            const { handleChange } = mapDispatchToControlProps(state.dispatch);
            const combinatorInfos = getCombinatorInfos(anyOfSchema.anyOf!, rootSchema);
            const options: [string, string][] = combinatorInfos.map((combinatorInfo, index) => [index+"", combinatorInfo.title || i18next.t("Item ") + (index+1)]);
            const pickerUpdater = (index: number) => {
                const matchedInfo = combinatorInfos[index];
                handleChange(path, matchedInfo.defaultValueCreator());
            };
            return html`
                <or-mwc-input .type="${InputType.SELECT}" .options="${options}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => pickerUpdater(Number(ev.detail.value))}"></or-mwc-input>
            `;
        }
    }

    // Return template for the anyOf schema that matches the data
    const matchedSchema = anyOfRenderInfos[anyOfProps.indexOfFittingSchema].schema;
    let matchedUischema = anyOfRenderInfos[anyOfProps.indexOfFittingSchema].uischema;

    if (matchedSchema.allOf) {
        // Force the uischema to be a simple control so it goes through the allOf renderer
        matchedUischema = {
            type: 'Control',
            scope: "#",
            label: false
        } as ControlElement;
    }

    const contentProps: OwnPropsOfRenderer & WithLabelAndRequired = {
        schema: matchedSchema,
        uischema: matchedUischema,
        path: path,
        renderers: renderers,
        cells: cells,
        label: props.label || getLabel(matchedSchema, rootSchema, label) || "",
        required: props.required || !!required
    }

    return getTemplateFromProps(state, contentProps);
}

export const allOfControlTester: RankedTester = rankWith(
    4,
    isAllOfControl
);
export const allOfControlRenderer = (state: JsonFormsStateContext, props: ControlProps & WithLabelAndRequired) => {
    const jsonFormsContext = {jsonforms: {...state}};
    const contentProps = {
        ...mapStateToControlWithDetailProps(jsonFormsContext, props)
    };

    // Merge the schemas
    const allOfSchema = resolveSubSchemas(contentProps.schema, contentProps.rootSchema, "allOf");
    contentProps.schema = (allOfSchema.allOf! as JsonSchema[]).reduce((accumulator, value) => Util.mergeObjects(accumulator, value, false));
    // Reset the uischema scope
    contentProps.uischema.scope = "#";

    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;

    return getTemplateFromProps(state, contentProps);
}


export const arrayControlTester: RankedTester = rankWith(
    2,
    schemaMatches(
        schema => hasType(schema, 'array') && !Array.isArray(schema.items) // we don't care about tuples
    )
);
export const arrayControlRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    const contentProps: ControlProps = {
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;

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

export function invalidTemplate() {
    return html`<span>No applicable renderer found!</span>`;
}

export function unknownTemplate() {
    return html`<span>No applicable renderer found!</span>`;
}

export const StandardRenderers: JsonFormsRendererRegistryEntry[] = [

    {tester: verticalOrGroupLayoutTester, renderer: verticalLayoutRenderer},
    {tester: inputControlTester, renderer: inputControlRenderer},
    {tester: objectControlTester, renderer: objectControlRenderer},
    {tester: arrayControlTester, renderer: arrayControlRenderer},
    {tester: anyOfControlTester, renderer: anyOfControlRenderer},
    {tester: allOfControlTester, renderer: allOfControlRenderer}
];
