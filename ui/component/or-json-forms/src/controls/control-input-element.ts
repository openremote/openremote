/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { type InputOption, InputType } from "@openremote/or-vaadin-components/util";
import { OrVaadinInput } from "@openremote/or-vaadin-components/or-vaadin-input";
import type { OrVaadinComboBox } from "@openremote/or-vaadin-components/or-vaadin-combo-box";
import type { OrVaadinMultiSelectComboBox } from "@openremote/or-vaadin-components/or-vaadin-multi-select-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-multi-select-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-date-picker";
import "@openremote/or-vaadin-components/or-vaadin-date-time-picker";
import "@openremote/or-vaadin-components/or-vaadin-time-picker";
import { css, html, type TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { ControlBaseElement } from "./control-base-element";
import { baseStyle } from "../styles";
import {
  isBooleanControl,
  isEnumControl,
  isIntegerControl,
  isNumberControl,
  isOneOfEnumControl,
  isStringControl,
  type JsonSchema,
} from "@jsonforms/core";
import { isEnumArray } from "../standard-renderers";

let defaultTz: string;

// language=CSS
const style = css`
  or-vaadin-input {
    display: block;
  }

  or-vaadin-input,
  or-vaadin-combo-box,
  or-vaadin-multi-select-combo-box,
  or-vaadin-date-picker,
  or-vaadin-date-time-picker,
  or-vaadin-time-picker {
    width: 100%;
  }
`;

@customElement("or-json-forms-input-control")
export class ControlInputElement extends ControlBaseElement {
  protected inputType!: InputType;

  public static get styles() {
    return [baseStyle, style];
  }

  render() {
    const uischema = this.uischema;
    const schema = this.schema;
    const format = this.schema.format;
    const context = { rootSchema: this.rootSchema, config: this.config };

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
    let searchable = false;

    if (Array.isArray(schema.type)) {
      this.inputType = InputType.JSON;
    } else if (isBooleanControl(uischema, schema, context)) {
      this.inputType = InputType.CHECKBOX;
    } else if (isNumberControl(uischema, schema, context) || isIntegerControl(uischema, schema, context)) {
      step = isNumberControl(uischema, schema, context) ? 0.1 : 1;
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
    } else if (
      isEnumControl(uischema, schema, context) ||
      isOneOfEnumControl(uischema, schema, context) ||
      isEnumArray(uischema, schema, context)
    ) {
      this.inputType = InputType.SELECT;

      if (isEnumControl(uischema, schema, context)) {
        options = schema.enum!.map((enm) => {
          return [JSON.stringify(enm), String(enm)];
        });
      } else if (isOneOfEnumControl(uischema, schema, context)) {
        options = (schema.oneOf as JsonSchema[]).map((s) => {
          return [JSON.stringify(s.const), String(s.const)];
        });
      } else {
        multiple = true;

        if ((schema.items! as JsonSchema).oneOf!) {
          options = (schema.items! as JsonSchema).oneOf!.map((s) => {
            return [JSON.stringify(s.const), String(s.const)];
          });
        } else {
          options = (schema.items! as JsonSchema).enum!.map((enm) => {
            return [JSON.stringify(enm), String(enm)];
          });
        }
      }
    } else if (isStringControl(uischema, schema, context)) {
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
      } else if (format === "timezone") {
        this.inputType = InputType.SELECT;
        options = Intl.supportedValuesOf("timeZone").map((z) => [JSON.stringify(z), z]);
        if (!(defaultTz && value)) {
          defaultTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
          this.handleChange(this.path, defaultTz);
        }
        // There are hundreds of zones to pick from, so the list needs to be filterable
        searchable = true;
      }
    }

    // Option values are JSON so that every enum type round-trips through the string an option carries
    if (this.inputType === InputType.SELECT) {
      if (multiple) {
        const values = Array.isArray(value) ? value : [value];
        value = values.filter((v) => v !== undefined).map((v) => JSON.stringify(v));
      } else {
        value = value === undefined ? undefined : JSON.stringify(value);
      }
    }

    if (multiple || searchable) {
      return this.getOptionsTemplate(options, value, multiple);
    }

    if ([InputType.DATE, InputType.DATETIME, InputType.TIME].includes(this.inputType)) {
      return this.getPickerTemplate(value);
    }

    const isCheckbox = this.inputType === InputType.CHECKBOX;
    const displayValue = OrVaadinInput.JSON_TYPES.includes(this.inputType)
      ? OrVaadinInput.stringifyJson(value)
      : (value ?? undefined);
    return html`<or-vaadin-input
      .id="${this.id}"
      type="${this.inputType}"
      label="${ifDefined(this.label || undefined)}"
      value="${ifDefined(isCheckbox ? undefined : displayValue)}"
      ?checked="${isCheckbox && !!value}"
      ?disabled="${!this.enabled}"
      ?required="${!!this.required}"
      .items="${options?.map(([optionValue, optionLabel]) => ({ value: optionValue, label: optionLabel }))}"
      minlength="${ifDefined(minLength)}"
      maxlength="${ifDefined(maxLength)}"
      pattern="${ifDefined(pattern)}"
      error-message="${ifDefined(this.errors || undefined)}"
      step="${ifDefined(step)}"
      min="${ifDefined(min)}"
      max="${ifDefined(max)}"
      @change="${(e: Event) => this.onVaadinValueChanged(e)}"
    ></or-vaadin-input>`;
  }

  /**
   * Template for an enum, which a combo box renders so that long lists can be filtered and several values picked.
   */
  protected getOptionsTemplate(options: [string, string][] | undefined, value: any, multiple: boolean): TemplateResult {
    const items: InputOption[] | undefined = options?.map(([optionValue, optionLabel]) => ({
      value: optionValue,
      label: optionLabel,
    }));

    if (multiple) {
      const selected: string[] = value ?? [];
      return html`<or-vaadin-multi-select-combo-box
        .items="${items}"
        .selectedItems="${items?.filter((item) => selected.includes(item.value))}"
        label="${ifDefined(this.label || undefined)}"
        error-message="${ifDefined(this.errors || undefined)}"
        ?disabled="${!this.enabled}"
        ?required="${!!this.required}"
        @change="${(e: Event) => this.onOptionsChanged(e)}"
      ></or-vaadin-multi-select-combo-box>`;
    }

    return html`<or-vaadin-combo-box
      .items="${items}"
      .value="${value ?? ""}"
      label="${ifDefined(this.label || undefined)}"
      error-message="${ifDefined(this.errors || undefined)}"
      ?disabled="${!this.enabled}"
      ?required="${!!this.required}"
      @change="${(e: Event) => this.onOptionChanged(e)}"
    ></or-vaadin-combo-box>`;
  }

  /**
   * Template for a date, time or date and time, which the pickers read and write in the same format the
   * matching JSON schema format describes.
   */
  protected getPickerTemplate(value: any): TemplateResult {
    const label = this.label || undefined;
    const errorMessage = this.errors || undefined;
    const disabled = !this.enabled;
    const required = !!this.required;
    const onChange = (e: Event) =>
      this.handleChange(this.path!, (e.currentTarget as HTMLInputElement).value || undefined);

    switch (this.inputType) {
      case InputType.DATE: {
        return html`<or-vaadin-date-picker
          value="${ifDefined(value)}"
          label="${ifDefined(label)}"
          error-message="${ifDefined(errorMessage)}"
          ?disabled="${disabled}"
          ?required="${required}"
          @change="${onChange}"
        ></or-vaadin-date-picker>`;
      }
      case InputType.TIME: {
        return html`<or-vaadin-time-picker
          value="${ifDefined(value)}"
          label="${ifDefined(label)}"
          error-message="${ifDefined(errorMessage)}"
          ?disabled="${disabled}"
          ?required="${required}"
          @change="${onChange}"
        ></or-vaadin-time-picker>`;
      }
      default: {
        return html`<or-vaadin-date-time-picker
          value="${ifDefined(value)}"
          label="${ifDefined(label)}"
          error-message="${ifDefined(errorMessage)}"
          ?disabled="${disabled}"
          ?required="${required}"
          @change="${onChange}"
        ></or-vaadin-date-time-picker>`;
      }
    }
  }

  /**
   * Vaadin inputs expose their value as a string, so it is parsed back into the type the schema expects.
   */
  protected onVaadinValueChanged(e: Event) {
    const input = e.currentTarget as OrVaadinInput;
    const value = input.nativeValue;
    const empty = value === undefined || value === "";

    switch (this.inputType) {
      case InputType.SELECT: {
        this.handleChange(this.path!, empty ? undefined : JSON.parse(value));
        break;
      }
      case InputType.NUMBER:
      case InputType.RANGE: {
        this.handleChange(this.path!, empty ? undefined : Number(value));
        break;
      }
      case InputType.JSON:
      case InputType.JSON_OBJECT: {
        // Text that does not parse has no value, so keep what is there and let the field show it is invalid
        if (input.checkValidity()) {
          this.handleChange(this.path!, value);
        }
        break;
      }
      default: {
        this.handleChange(this.path!, value);
      }
    }
  }

  /** Reads the single option a combo box holds, whose value is the JSON of the schema value. */
  protected onOptionChanged(e: Event) {
    const value = (e.currentTarget as OrVaadinComboBox).value;
    this.handleChange(this.path!, value ? JSON.parse(value) : undefined);
  }

  /** Reads the options a multi select combo box holds, whose values are the JSON of the schema values. */
  protected onOptionsChanged(e: Event) {
    const items = (e.currentTarget as OrVaadinMultiSelectComboBox).selectedItems as InputOption[];
    this.handleChange(
      this.path!,
      items.map((item) => JSON.parse(item.value))
    );
  }
}
