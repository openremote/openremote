import { and, createCombinatorRenderInfos, findUISchema, hasType, isAllOfControl, isAnyOfControl, isBooleanControl, isDateControl, isDateTimeControl, isEnumControl, isIntegerControl, isNumberControl, isObjectControl, isOneOfControl, isOneOfEnumControl, isStringControl, isTimeControl, mapDispatchToControlProps, mapStateToControlProps, mapStateToControlWithDetailProps, mapStateToLayoutProps, or, rankWith, resolveSubSchemas, schemaMatches, schemaSubPathMatches, uiTypeIs } from "@jsonforms/core";
import { html } from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import { getCombinatorInfos, getLabel, getSchemaConst, getSchemaPicker, getTemplateFromProps, mapStateToCombinatorRendererProps, showJsonEditor } from "./util";
import "./layouts/layout-vertical-element";
import "./controls/control-input-element";
import "./controls/control-array-element";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { Util } from "@openremote/core";
const hasOneOfItems = (schema) => schema.oneOf !== undefined &&
    schema.oneOf.length > 0 &&
    schema.oneOf.every((entry) => {
        return getSchemaConst(entry) !== undefined;
    });
const hasEnumItems = (schema) => Array.isArray(schema.enum);
export const isEnumArray = and(uiTypeIs('Control'), and(schemaMatches(schema => hasType(schema, 'array') &&
    !Array.isArray(schema.items) &&
    schema.uniqueItems === true), schemaSubPathMatches('items', schema => {
    return hasOneOfItems(schema) || hasEnumItems(schema);
})));
export const verticalOrGroupLayoutTester = rankWith(1, or(uiTypeIs("VerticalLayout"), uiTypeIs("Group")));
export const verticalLayoutRenderer = (state, props) => {
    const contentProps = Object.assign(Object.assign(Object.assign({}, mapStateToLayoutProps({ jsonforms: Object.assign({}, state) }, props)), mapDispatchToControlProps(state.dispatch)), { label: props.label, required: props.required, errors: props.errors, minimal: props.minimal, type: props.type });
    const template = html `<or-json-forms-vertical-layout .state="${state}" .props="${contentProps}"></or-json-forms-vertical-layout>`;
    let deleteHandler;
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path || "", undefined);
        };
    }
    return getTemplateWrapper(template, deleteHandler);
};
export const constTester = rankWith(6, schemaMatches(schema => getSchemaConst(schema) !== undefined));
export const constRenderer = (state, props) => {
    // Don't render const
    return undefined;
};
export const inputControlTester = rankWith(3, or(schemaMatches(schema => Array.isArray(schema.type) && schema.type.length === 7), isStringControl, isBooleanControl, isNumberControl, isIntegerControl, isDateControl, isTimeControl, isDateTimeControl, isEnumControl, isOneOfEnumControl, isEnumArray));
export const inputControlRenderer = (state, props) => {
    const contentProps = Object.assign(Object.assign({}, mapStateToControlProps({ jsonforms: Object.assign({}, state) }, props)), mapDispatchToControlProps(state.dispatch));
    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;
    const template = html `<or-json-forms-input-control .state="${state}" .props="${contentProps}"></or-json-forms-input-control>`;
    let deleteHandler;
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path, undefined);
        };
    }
    return getTemplateWrapper(template, deleteHandler);
};
export const objectControlTester = rankWith(2, isObjectControl);
export const objectControlRenderer = (state, props) => {
    const { required, renderers, cells, uischemas, schema, label, errors, path, visible, enabled, uischema, rootSchema } = mapStateToControlWithDetailProps({ jsonforms: Object.assign({}, state) }, props);
    const detailUiSchema = findUISchema(uischemas, schema, uischema.scope, path, "VerticalLayout", uischema, rootSchema);
    const contentProps = {
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
};
export const anyOfOneOfControlTester = rankWith(4, or(isAnyOfControl, isOneOfControl));
export const anyOfOneOfControlRenderer = (state, props) => {
    const jsonFormsContext = { jsonforms: Object.assign({}, state) };
    const { required, renderers, cells, schema, label, path, errors, visible, enabled, uischema, rootSchema, data } = mapStateToControlWithDetailProps(jsonFormsContext, props);
    const keyword = schema.anyOf !== undefined ? "anyOf" : "oneOf";
    const resolvedSchema = resolveSubSchemas(schema, rootSchema, keyword);
    const resolvedProps = mapStateToCombinatorRendererProps(jsonFormsContext, props, keyword);
    const renderInfos = createCombinatorRenderInfos(resolvedSchema[keyword], rootSchema, keyword, resolvedProps.uischema || uischema, path, resolvedProps.uischemas);
    if (data !== undefined && data !== null && (resolvedProps.indexOfFittingSchema === undefined || resolvedProps.indexOfFittingSchema < 0)) {
        // Try and match the data using our own combinator info objects
        const combinatorInfos = getCombinatorInfos(resolvedSchema[keyword], rootSchema);
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
            const showJson = (ev) => {
                ev.stopPropagation();
                showJsonEditor(label, data, ((newValue) => {
                    handleChange(path || "", newValue);
                }));
            };
            return html `
                <div class="item-container no-match-container"><span>${label}:</span><b><or-translate value="validation.noSchemaMatchFound"></b><or-mwc-input .type="${InputType.BUTTON}" outlined label="json" icon="pencil" @or-mwc-input-changed="${(ev) => showJson(ev)}"></or-mwc-input></div>
            `;
        }
        else {
            // We have no data so show a schema picker
            return getSchemaPicker(rootSchema, resolvedSchema, path, keyword, props.label || label, (selectedSchema => handleChange(path, selectedSchema.defaultValueCreator())));
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
        };
    }
    const contentProps = {
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
    };
    return getTemplateFromProps(state, contentProps);
};
export const allOfControlTester = rankWith(4, isAllOfControl);
export const allOfControlRenderer = (state, props) => {
    const jsonFormsContext = { jsonforms: Object.assign({}, state) };
    const contentProps = Object.assign({}, mapStateToControlWithDetailProps(jsonFormsContext, props));
    // Merge the schemas
    const allOfSchema = resolveSubSchemas(contentProps.schema, contentProps.rootSchema, "allOf");
    contentProps.schema = allOfSchema.allOf.reduce((accumulator, value) => Util.mergeObjects(accumulator, value, false));
    // Reset the uischema scope
    contentProps.uischema.scope = "#";
    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;
    contentProps.minimal = props.minimal;
    return getTemplateFromProps(state, contentProps);
};
export const arrayControlTester = rankWith(2, schemaMatches(schema => hasType(schema, 'array') && !Array.isArray(schema.items) // we don't care about tuples
));
export const arrayControlRenderer = (state, props) => {
    const contentProps = Object.assign(Object.assign({}, mapStateToControlProps({ jsonforms: Object.assign({}, state) }, props)), mapDispatchToControlProps(state.dispatch));
    contentProps.label = props.label || contentProps.label;
    contentProps.required = !!props.required || contentProps.required;
    contentProps.minimal = props.minimal;
    const template = html `<or-json-forms-array-control .state="${state}" .props="${contentProps}"></or-json-forms-array-control>`;
    let deleteHandler;
    if (!contentProps.required && contentProps.path) {
        deleteHandler = () => {
            contentProps.handleChange(contentProps.path, undefined);
        };
    }
    return getTemplateWrapper(template, deleteHandler);
};
export function getTemplateWrapper(elementTemplate, deleteHandler) {
    const deleteTemplate = !deleteHandler ? `` : html `
                <button class="button-clear" @click="${deleteHandler}"><or-icon icon="close-circle"></or-icon></input>
            `;
    return html `
                <div class="item-container">
                    ${elementTemplate}
                    <div class="delete-container">
                        ${deleteTemplate}
                    </div>
                </div>
            `;
}
export function unknownTemplate() {
    return html `<span>No applicable renderer found!</span>`;
}
export const StandardRenderers = [
    { tester: verticalOrGroupLayoutTester, renderer: verticalLayoutRenderer },
    { tester: constTester, renderer: constRenderer },
    { tester: inputControlTester, renderer: inputControlRenderer },
    { tester: objectControlTester, renderer: objectControlRenderer },
    { tester: arrayControlTester, renderer: arrayControlRenderer },
    { tester: anyOfOneOfControlTester, renderer: anyOfOneOfControlRenderer },
    { tester: allOfControlTester, renderer: allOfControlRenderer }
];
//# sourceMappingURL=standard-renderers.js.map