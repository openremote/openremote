import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {css, html} from "lit";
import {customElement} from "lit/decorators.js";
import {ControlBaseElement} from "./control-base-element";
import {baseStyle} from "../styles";
import {
    isBooleanControl,
    isEnumControl,
    isIntegerControl,
    isNumberControl,
    isOneOfEnumControl,
    isStringControl,
    JsonSchema
} from "@jsonforms/core";
import {isEnumArray} from "../standard-renderers";
import {getSchemaConst} from "../util";

// language=CSS
const style = css`
    or-mwc-input {
        width: 100%;
    }
`;

@customElement("or-json-forms-input-control")
export class ControlInputElement extends ControlBaseElement {

    protected inputType!: InputType;

    public static get styles() {
        return [
            baseStyle,
            style
        ];
    }

    render() {

        const uischema = this.uischema;
        const schema = this.schema;
        const format = this.schema.format;

        this.inputType = InputType.TEXT;
        let step: number | undefined;
        let min: number | undefined;
        let minLength: number | undefined;
        let max: number | undefined;
        let maxLength: number | undefined;
        let pattern: string | undefined;
        let options: [string, string][] | undefined;
        let multiple = false;
        let value: any = this.data ?? schema.default;

        if (Array.isArray(schema.type)) {
            this.inputType = InputType.JSON;
        } else if (isBooleanControl(uischema, schema, this.rootSchema)) {
            this.inputType = InputType.CHECKBOX;
        } else if (isNumberControl(uischema, schema, this.rootSchema) || isIntegerControl(uischema, schema, this.rootSchema)) {
            step = isNumberControl(uischema, schema, this.rootSchema) ? 0.1 : 1;
            this.inputType = InputType.NUMBER;
            min = schema.minimum;
            max = schema.maximum;

            step = schema.multipleOf || step;

            if (min !== undefined && max !== undefined && format === "or-range") {
                // Limit to 200 graduations
                if ((max-min)/step <= 200) {
                    this.inputType = InputType.RANGE;
                }
            }

        } else if (
            isEnumControl(uischema, schema, this.rootSchema)
            || isOneOfEnumControl(uischema, schema, this.rootSchema)
            || isEnumArray(uischema, schema, this.rootSchema)) {

            this.inputType = InputType.SELECT;

            if (isEnumControl(uischema, schema, this.rootSchema)) {
                options = schema.enum!.map(enm => {
                    return [JSON.stringify(enm), String(enm)];
                });
            } else if (isOneOfEnumControl(uischema, schema, this.rootSchema)) {
                options = (schema.oneOf as JsonSchema[]).map(s => {
                    return [JSON.stringify(s.const), String(s.const)];
                })
            } else {

                multiple = true;

                if ((schema.items! as JsonSchema).oneOf!) {
                    options = (schema.items! as JsonSchema).oneOf!.map(s => {
                        return [JSON.stringify(s.const), String(s.const)];
                    })
                } else {
                    options = (schema.items! as JsonSchema).enum!.map(enm => {
                        return [JSON.stringify(enm), String(enm)];
                    })
                }
            }

            if (multiple) {
                value = Array.isArray(value) ? value.map(v => JSON.stringify(v)) : value !== undefined ? [JSON.stringify(value)] : undefined;
            } else {
                value = value !== undefined ? JSON.stringify(value) : undefined;
            }
        } else if (isStringControl(uischema, schema, this.rootSchema)) {
            minLength = schema.minLength;
            maxLength = schema.maxLength;
            pattern = schema.pattern;

            if (format === "date-time") {
                this.inputType = InputType.DATETIME;
            } else if (format === "date") {
                this.inputType = InputType.DATE;
            } else if (format === "time") {
                this.inputType = InputType.TIME;
            } else if (format === "email") {
                this.inputType = InputType.EMAIL;
            } else if (format === "tel") {
                this.inputType = InputType.TELEPHONE;
            } else if (format === "or-multiline") {
                this.inputType = InputType.TEXTAREA;
            } else if (format === "or-password" || (schema as any).writeOnly) {
                this.inputType = InputType.PASSWORD;
            }
        }

        return html`<or-mwc-input
                .label="${this.label}"
                .type="${this.inputType}"
                .disabled="${!this.enabled}"
                .required="${!!this.required}"
                .id="${this.id}"
                .options="${options}"
                .multiple="${multiple}"
                @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.onValueChanged(e)}"
                .maxLength="${maxLength}"
                .minLength="${minLength}"
                .pattern="${pattern}"
                .validationMessage="${this.errors}"
                .step="${step}"
                .max="${max}"
                .min="${min}"
                .value="${value}"></or-mwc-input>`;
    }

    protected onValueChanged(e: OrInputChangedEvent) {
        if (this.inputType === InputType.SELECT) {
            if (Array.isArray(e.detail.value)) {
                this.handleChange(this.path!, (e.detail.value as []).map((v: string) => JSON.parse(v)));
            } else {
                this.handleChange(this.path!, JSON.parse((e.detail.value as string)));
            }
        } else {
            this.handleChange(this.path!, e.detail.value);
        }
    }
}
