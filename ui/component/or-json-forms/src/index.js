var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { Actions, configReducer, coreReducer, createAjv, generateDefaultUISchema, generateJsonSchema, mapStateToJsonFormsRendererProps, setConfig } from "@jsonforms/core";
import { getTemplateWrapper, StandardRenderers } from "./standard-renderers";
import { getLabel, getTemplateFromProps } from "./util";
import { baseStyle } from "./styles";
import { Util } from "@openremote/core";
export { StandardRenderers, getTemplateWrapper };
// language=CSS
const styles = css `
    .delete-container {
        width: 0;
    }

    .item-container {
        margin: 0; /* Remove inherited margin */
    }
`;
let OrJSONForms = class OrJSONForms extends LitElement {
    constructor() {
        super(...arguments);
        this.renderers = StandardRenderers;
        this.readonly = false;
        this.required = false;
        this.previousErrors = [];
    }
    static get styles() {
        return [
            baseStyle,
            styles
        ];
    }
    checkValidity() {
        return this.previousErrors.length === 0;
    }
    shouldUpdate(_changedProperties) {
        super.shouldUpdate(_changedProperties);
        if (!this.schema) {
            this.schema = this.data !== undefined ? generateJsonSchema(this.data) : {};
        }
        if (!this.uischema) {
            this.uischema = generateDefaultUISchema(this.schema);
        }
        if (!this.core) {
            this.core = {
                ajv: createAjv({ useDefaults: true, validateFormats: false }),
                data: {},
                schema: this.schema,
                uischema: this.uischema
            };
            this.updateCore(Actions.init(this.data, this.schema, this.uischema));
            this.config = configReducer(undefined, setConfig(this.config));
        }
        if (_changedProperties.has("data") || _changedProperties.has("schema") || _changedProperties.has("uischema")) {
            this.updateCore(Actions.updateCore(this.data, this.schema, this.uischema));
        }
        if (!this.contextValue || _changedProperties.has("core") || _changedProperties.has("renderers") || _changedProperties.has("cells") || _changedProperties.has("config") || _changedProperties.has("readonly")) {
            this.contextValue = {
                core: this.core,
                renderers: this.renderers,
                cells: this.cells,
                config: this.config,
                uischemas: this.uischemas,
                readonly: this.readonly,
                dispatch: (action) => this.updateCore(action)
            };
        }
        if (_changedProperties.has("core")) {
            const data = this.core.data;
            const errors = this.core.errors;
            if (this.onChange && (!Util.objectsEqual(data, this.previousData, true) || (errors && !Util.objectsEqual(errors, this.previousErrors, true)))) {
                this.previousErrors = errors || [];
                this.previousData = data;
                this.onChange({ data: data, errors: errors });
            }
        }
        return true;
    }
    updateCore(coreAction) {
        const coreState = coreReducer(this.core, coreAction);
        if (coreState !== this.core) {
            this.core = coreState;
        }
        return coreAction;
    }
    render() {
        if (!this.contextValue) {
            return html ``;
        }
        const props = Object.assign(Object.assign({}, mapStateToJsonFormsRendererProps({ jsonforms: Object.assign({}, this.contextValue) }, this)), { label: getLabel(this.schema, this.uischema, this.label, undefined) || "", required: this.required });
        return getTemplateFromProps(this.contextValue, props) || html ``;
    }
};
__decorate([
    property({ type: Object })
], OrJSONForms.prototype, "uischema", void 0);
__decorate([
    property({ type: Object })
], OrJSONForms.prototype, "schema", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrJSONForms.prototype, "data", void 0);
__decorate([
    property({ type: Array })
], OrJSONForms.prototype, "renderers", void 0);
__decorate([
    property({ type: Array })
], OrJSONForms.prototype, "cells", void 0);
__decorate([
    property({ type: String, attribute: false })
], OrJSONForms.prototype, "onChange", void 0);
__decorate([
    property({ type: String, attribute: false })
], OrJSONForms.prototype, "config", void 0);
__decorate([
    property({ type: Array })
], OrJSONForms.prototype, "uischemas", void 0);
__decorate([
    property({ type: Boolean })
], OrJSONForms.prototype, "readonly", void 0);
__decorate([
    property({ type: String })
], OrJSONForms.prototype, "label", void 0);
__decorate([
    property({ type: Boolean })
], OrJSONForms.prototype, "required", void 0);
__decorate([
    state()
], OrJSONForms.prototype, "core", void 0);
__decorate([
    state()
], OrJSONForms.prototype, "contextValue", void 0);
OrJSONForms = __decorate([
    customElement("or-json-forms")
], OrJSONForms);
export { OrJSONForms };
//# sourceMappingURL=index.js.map