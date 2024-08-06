var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { ControlBaseElement } from "./control-base-element";
import { baseStyle } from "../styles";
import { isBooleanControl, isEnumControl, isIntegerControl, isNumberControl, isOneOfEnumControl, isStringControl } from "@jsonforms/core";
import { isEnumArray } from "../standard-renderers";
// language=CSS
const style = css `
    or-mwc-input {
        width: 100%;
    }
`;
let ControlInputElement = class ControlInputElement extends ControlBaseElement {
    static get styles() {
        return [
            baseStyle,
            style
        ];
    }
    render() {
        var _a;
        const uischema = this.uischema;
        const schema = this.schema;
        const format = this.schema.format;
        this.inputType = InputType.TEXT;
        let step;
        let min;
        let minLength;
        let max;
        let maxLength;
        let pattern;
        let options;
        let multiple = false;
        let value = (_a = this.data) !== null && _a !== void 0 ? _a : schema.default;
        if (Array.isArray(schema.type)) {
            this.inputType = InputType.JSON;
        }
        else if (isBooleanControl(uischema, schema)) {
            this.inputType = InputType.CHECKBOX;
        }
        else if (isNumberControl(uischema, schema) || isIntegerControl(uischema, schema)) {
            step = isNumberControl(uischema, schema) ? 0.1 : 1;
            this.inputType = InputType.NUMBER;
            min = schema.minimum;
            max = schema.maximum;
            step = schema.multipleOf || step;
            if (min !== undefined && max !== undefined && format === "or-range") {
                // Limit to 200 graduations
                if ((max - min) / step <= 200) {
                    this.inputType = InputType.RANGE;
                }
            }
        }
        else if (isEnumControl(uischema, schema)
            || isOneOfEnumControl(uischema, schema)
            || isEnumArray(uischema, schema)) {
            this.inputType = InputType.SELECT;
            if (isEnumControl(uischema, schema)) {
                options = schema.enum.map(enm => {
                    return [JSON.stringify(enm), String(enm)];
                });
            }
            else if (isOneOfEnumControl(uischema, schema)) {
                options = schema.oneOf.map(s => {
                    return [JSON.stringify(s.const), String(s.const)];
                });
            }
            else {
                multiple = true;
                if (schema.items.oneOf) {
                    options = schema.items.oneOf.map(s => {
                        return [JSON.stringify(s.const), String(s.const)];
                    });
                }
                else {
                    options = schema.items.enum.map(enm => {
                        return [JSON.stringify(enm), String(enm)];
                    });
                }
            }
            if (multiple) {
                value = Array.isArray(value) ? value.map(v => JSON.stringify(v)) : value !== undefined ? [JSON.stringify(value)] : undefined;
            }
            else {
                value = value !== undefined ? JSON.stringify(value) : undefined;
            }
        }
        else if (isStringControl(uischema, schema)) {
            minLength = schema.minLength;
            maxLength = schema.maxLength;
            pattern = schema.pattern;
            if (format === "date-time") {
                this.inputType = InputType.DATETIME;
            }
            else if (format === "date") {
                this.inputType = InputType.DATE;
            }
            else if (format === "time") {
                this.inputType = InputType.TIME;
            }
            else if (format === "email") {
                this.inputType = InputType.EMAIL;
            }
            else if (format === "tel") {
                this.inputType = InputType.TELEPHONE;
            }
            else if (format === "or-multiline") {
                this.inputType = InputType.TEXTAREA;
            }
            else if (format === "or-password" || schema.writeOnly) {
                this.inputType = InputType.PASSWORD;
            }
        }
        return html `<or-mwc-input
                .label="${this.label}"
                .type="${this.inputType}"
                .disabled="${!this.enabled}"
                .required="${!!this.required}"
                .id="${this.id}"
                .options="${options}"
                .multiple="${multiple}"
                @or-mwc-input-changed="${(e) => this.onValueChanged(e)}"
                .maxLength="${maxLength}"
                .minLength="${minLength}"
                .pattern="${pattern}"
                .validationMessage="${this.errors}"
                .step="${step}"
                .max="${max}"
                .min="${min}"
                .value="${value}"></or-mwc-input>`;
    }
    onValueChanged(e) {
        if (this.inputType === InputType.SELECT) {
            if (Array.isArray(e.detail.value)) {
                this.handleChange(this.path, e.detail.value.map((v) => JSON.parse(v)));
            }
            else {
                this.handleChange(this.path, JSON.parse(e.detail.value));
            }
        }
        else {
            this.handleChange(this.path, e.detail.value);
        }
    }
};
ControlInputElement = __decorate([
    customElement("or-json-forms-input-control")
], ControlInputElement);
export { ControlInputElement };
//# sourceMappingURL=control-input-element.js.map