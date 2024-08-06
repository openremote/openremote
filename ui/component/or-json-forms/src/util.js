import { composeWithUi, createDefaultValue, deriveTypes, getAjv, getData, getRenderers, getSchema, hasShowRule, isVisible, Resolve, resolveSchema, resolveSubSchemas, } from "@jsonforms/core";
import { DefaultColor5, Util } from "@openremote/core";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-components/or-ace-editor";
import { html, unsafeCSS } from "lit";
import { createRef, ref } from 'lit/directives/ref.js';
import { unknownTemplate } from "./standard-renderers";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
export function getTemplateFromProps(state, props) {
    if (!state || !props) {
        return html ``;
    }
    const renderers = props.renderers || getRenderers({ jsonforms: Object.assign({}, state) });
    const schema = props.schema;
    const uischema = props.uischema;
    let template;
    if (renderers && schema && uischema) {
        const orderedRenderers = renderers.map(r => [r, r.tester(uischema, schema)]).sort((a, b) => b[1] - a[1]);
        const renderer = orderedRenderers && orderedRenderers.length > 0 ? orderedRenderers[0] : undefined;
        if (renderer && renderer[1] !== -1) {
            template = renderer[0].renderer(state, props);
        }
        else {
            template = unknownTemplate();
        }
    }
    return template;
}
/**
 * For a given anyOf schema array this will try and extract a common const property which can be used as a discriminator
 * when creating instances
 */
export function getCombinatorInfos(schemas, rootSchema) {
    return schemas.map(schema => {
        let constProperty;
        let constValue;
        let creator;
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
                    const obj = {};
                    obj[constProp[0]] = getSchemaConst(constProp[1]);
                    return obj;
                };
                if (!titleAndDescription[0]) {
                    titleAndDescription[0] = getSchemaConst(constProp[1]);
                }
            }
            else {
                creator = () => createDefaultValue(schema);
            }
        }
        else {
            // Assume a primitive type that can be instantiated with default value creator
            creator = () => createDefaultValue(schema);
        }
        return {
            title: titleAndDescription[0],
            description: titleAndDescription[1],
            defaultValueCreator: creator,
            constProperty: constProperty,
            constValue: constValue
        };
    });
}
export function getSchemaConst(schema) {
    if (!schema) {
        return;
    }
    if (schema.const !== undefined) {
        return schema.const;
    }
    if (Array.isArray(schema.enum) && schema.enum.length === 1) {
        return schema.enum[0];
    }
}
export function getSchemaPicker(rootSchema, resolvedSchema, path, keyword, label, selectedCallback) {
    const combinatorInfos = getCombinatorInfos(resolvedSchema[keyword], rootSchema);
    const options = combinatorInfos.map((combinatorInfo, index) => [index + "", combinatorInfo.title || i18next.t("schema.title.indexedItem", { index: index })]);
    const pickerUpdater = (index) => {
        const matchedInfo = combinatorInfos[index];
        selectedCallback(matchedInfo);
    };
    const pickerLabel = label ? i18next.t("schema.anyOfPickerLabel", { label: label }) : i18next.t("type");
    return html `                
        <or-mwc-input class="any-of-picker" .label="${pickerLabel}" .type="${InputType.SELECT}" .options="${options}" @or-mwc-input-changed="${(ev) => pickerUpdater(Number(ev.detail.value))}"></or-mwc-input>
    `;
}
export function findSchemaTitleAndDescription(schema, rootSchema) {
    let title;
    if (schema.$ref) {
        title = getLabelFromScopeOrRef(schema.$ref);
        schema = Resolve.schema(rootSchema, schema.$ref);
    }
    if (schema.title) {
        return [schema.title, schema.description];
    }
    if (schema.allOf) {
        const resolvedSchema = resolveSubSchemas(schema, rootSchema, "allOf");
        const titledSchema = resolvedSchema.allOf.find((allOfSchema) => {
            return !!allOfSchema.title;
        });
        if (titledSchema) {
            return [titledSchema.title, titledSchema.description];
        }
    }
    return [i18next.t("schema.title." + title, { defaultValue: Util.camelCaseToSentenceCase(title) }), undefined];
}
function getLabelFromScopeOrRef(scopeOrRef) {
    return scopeOrRef.substr(scopeOrRef.lastIndexOf("/") + 1);
}
function getSchemaObjectProperties(schema) {
    let props = [];
    if (schema.allOf) {
        props = schema.allOf.map(schema => schema.properties ? Object.entries(schema.properties) : []).flat();
    }
    else if (schema.properties) {
        props = Object.entries(schema.properties);
    }
    return props;
}
/**
 * Copied from eclipse source code to inject global definitions into the validating schema otherwise AJV will fail
 * to compile the schema - not perfect but works for our cases
 */
export function mapStateToCombinatorRendererProps(state, ownProps, keyword) {
    const { uischema } = ownProps;
    const path = composeWithUi(uischema, ownProps.path);
    const rootSchema = getSchema(state);
    const resolvedSchema = Resolve.schema(ownProps.schema || rootSchema, uischema.scope, rootSchema);
    const visible = ownProps.visible === undefined || hasShowRule(uischema)
        ? isVisible(uischema, getData(state), ownProps.path, getAjv(state))
        : ownProps.visible;
    const id = ownProps.id;
    const data = Resolve.data(getData(state), path);
    const ajv = state.jsonforms.core.ajv;
    const schema = resolvedSchema || rootSchema;
    const _schema = resolveSubSchemas(schema, rootSchema, keyword);
    const structuralKeywords = [
        'required',
        'additionalProperties',
        'type',
        'enum',
        'const'
    ];
    const dataIsValid = (errors) => {
        return (!errors ||
            errors.length === 0 ||
            !errors.find(e => structuralKeywords.indexOf(e.keyword) !== -1));
    };
    let indexOfFittingSchema = -1;
    // TODO instead of compiling the combinator subschemas we can compile the original schema
    // without the combinator alternatives and then revalidate and check the errors for the
    // element
    for (let i = 0; i < _schema[keyword].length; i++) {
        try {
            const schema = Object.assign({ definitions: rootSchema.definitions }, _schema[keyword][i]);
            const valFn = ajv.compile(schema);
            valFn(data);
            if (dataIsValid(valFn.errors)) {
                indexOfFittingSchema = i;
                break;
            }
        }
        catch (error) {
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
        uischemas: state.jsonforms.uischemas,
        uischema
    };
}
export function getLabel(schema, rootSchema, uiElementLabel, uiElementScope) {
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
export function resolveSubSchemasRecursive(schema, rootSchema, keyword) {
    const combinators = keyword ? [keyword] : ["allOf", "anyOf", "oneOf"];
    if (schema.$ref) {
        return resolveSubSchemasRecursive(resolveSchema(rootSchema, schema.$ref), rootSchema);
    }
    combinators.forEach((combinator) => {
        const schemas = schema[combinator];
        if (schemas) {
            schema[combinator] = schemas.map(subSchema => resolveSubSchemasRecursive(subSchema, rootSchema));
        }
    });
    if (schema.items) {
        if (Array.isArray(schema.items)) {
            schema.items = schema.items.map((itemSchema) => resolveSubSchemasRecursive(itemSchema, rootSchema));
        }
        else {
            schema.items = resolveSubSchemasRecursive(schema.items, rootSchema);
        }
    }
    if (schema.properties) {
        Object.keys(schema.properties).forEach((prop) => schema.properties[prop] = resolveSubSchemasRecursive(schema.properties[prop], rootSchema));
    }
    return schema;
}
export const controlWithoutLabel = (scope) => ({
    type: 'Control',
    scope: scope,
    label: false
});
export const showJsonEditor = (title, value, updateCallback) => {
    const editorRef = createRef();
    const updateBtnRef = createRef();
    const onEditorEdit = () => {
        // Disable update button whilst edit in progress
        updateBtnRef.value.disabled = true;
    };
    const onEditorChanged = (ev) => {
        const valid = ev.detail.valid;
        updateBtnRef.value.disabled = !valid;
    };
    const dialog = showDialog(new OrMwcDialog()
        .setContent(html `
                <or-ace-editor ${ref(editorRef)} @or-ace-editor-edit="${() => onEditorEdit()}" @or-ace-editor-changed="${(ev) => onEditorChanged(ev)}" .value="${value}"></or-ace-editor>
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
                const editor = editorRef.value;
                if (editor.validate()) {
                    const data = !!editor.getValue() ? JSON.parse(editor.getValue()) : undefined;
                    updateCallback(data);
                }
            },
            content: html `<or-mwc-input ${ref(updateBtnRef)} disabled .type="${InputType.BUTTON}" label="update"></or-mwc-input>`
        }
    ])
        .setHeading(title)
        .setDismissAction(null)
        .setStyles(html `
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
//# sourceMappingURL=util.js.map