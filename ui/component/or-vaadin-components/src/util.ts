/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {Util} from "@openremote/core";
import {WellknownMetaItems, WellknownValueTypes} from "@openremote/model";
import type {
    AssetDescriptor,
    Attribute,
    AttributeDescriptor,
    MetaHolder,
    NameHolder,
    NameValueHolder,
    ValueConstraint,
    ValueConstraintAllowedValues,
    ValueConstraintFuture,
    ValueConstraintFutureOrPresent,
    ValueConstraintMax,
    ValueConstraintMin,
    ValueConstraintNotBlank,
    ValueConstraintNotEmpty,
    ValueConstraintNotNull,
    ValueConstraintPast,
    ValueConstraintPastOrPresent,
    ValueConstraintPattern,
    ValueConstraintSize,
    ValueDescriptor,
    ValueDescriptorHolder,
    ValueFormat,
    ValueHolder
} from "@openremote/model";
import {html, TemplateResult} from "lit";
import {Ref, ref, createRef} from "lit/directives/ref.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {styleMap} from "lit/directives/style-map.js";
import {OrVaadinInput} from "./or-vaadin-input";

export enum InputType {
    BUTTON = "button",
    BUTTON_TOGGLE = "button-toggle",
    BUTTON_MOMENTARY = "button-momentary",
    CHECKBOX = "checkbox",
    CHECKBOX_LIST = "checkbox-list",
    COLOUR = "color",
    DATE = "date",
    DATETIME = "datetime-local",
    EMAIL = "email",
    JSON = "json",
    JSON_OBJECT = "json-object",
    MONTH = "month",
    NUMBER = "number",
    BIG_INT = "big-int",
    PASSWORD = "password",
    RADIO = "radio",
    SWITCH = "switch",
    RANGE = "range",
    TELEPHONE = "tel",
    TEXT = "text",
    TEXTAREA = "textarea",
    TIME = "time",
    URL = "url",
    WEEK = "week",
    SELECT = "select",
    LIST = "list",
    CRON = "cron",
    DURATION = "duration",
    DURATION_TIME = "duration-time",
    DURATION_PERIOD = "duration-period"
}

export interface ValueInputProviderOptions {
    label?: string;
    required?: boolean;
    readonly?: boolean;
    disabled?: boolean;
    compact?: boolean;
    rounded?: boolean;
    outlined?: boolean;
    comfortable?: boolean;
    resizeVertical?: boolean;
    inputType?: InputType;
}

export interface ValueInputProvider {
    templateFunction: ValueInputTemplateFunction;
    supportsHelperText: boolean;
    supportsLabel: boolean;
    supportsSendButton: boolean;
    validator?: () => boolean;
}

export type ValueInputTemplateFunction = ((value: any, focused: boolean, loading: boolean, sending: boolean, error: boolean, helperText: string | undefined) => TemplateResult | PromiseLike<TemplateResult>) | undefined;

export type ValueInputProviderGenerator = (assetDescriptor: AssetDescriptor | string, valueHolder: NameHolder & ValueHolder<any> | undefined, valueHolderDescriptor: ValueDescriptorHolder | undefined, valueDescriptor: ValueDescriptor
                                           , valueChangeNotifier: (value: any) => void, options: ValueInputProviderOptions) => ValueInputProvider;

function inputTypeSupportsButton(inputType: InputType): boolean {
    return inputType === InputType.NUMBER
        || inputType === InputType.BIG_INT
        || inputType === InputType.TELEPHONE
        || inputType === InputType.TEXT
        || inputType === InputType.PASSWORD
        || inputType === InputType.DATE
        || inputType === InputType.DATETIME
        || inputType === InputType.EMAIL
        || inputType === InputType.JSON
        || inputType === InputType.JSON_OBJECT
        || inputType === InputType.MONTH
        || inputType === InputType.TEXTAREA
        || inputType === InputType.TIME
        || inputType === InputType.URL
        || inputType === InputType.WEEK;
}

function inputTypeSupportsHelperText(inputType: InputType) {
    return inputTypeSupportsButton(inputType) || inputType === InputType.SELECT;
}

function inputTypeSupportsLabel(inputType: InputType) {
    return inputTypeSupportsHelperText(inputType) || inputType === InputType.CHECKBOX || inputType === InputType.BUTTON_MOMENTARY;
}

export const getValueHolderInputTemplateProvider: ValueInputProviderGenerator = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {

    let inputType: InputType | undefined = options.inputType;
    let step: number | undefined;
    let pattern: string | undefined;
    let min: any;
    let max: any;
    let multiple: any;
    let required: boolean | undefined;
    let selectOptions: [string, string][] | undefined;
    let valueConverter: (v: any) => any | undefined;
    const styles = {} as any;

    const assetType = typeof assetDescriptor === "string" ? assetDescriptor : assetDescriptor.name;
    const constraints: ValueConstraint[] = (valueHolder && ((valueHolder as MetaHolder).meta) || (valueDescriptor && (valueDescriptor as MetaHolder).meta) ? Util.getAttributeValueConstraints(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType) : Util.getMetaValueConstraints(valueHolder as NameValueHolder<any>, valueHolderDescriptor as AttributeDescriptor, assetType)) || [];
    const format: ValueFormat | undefined = (valueHolder && ((valueHolder as MetaHolder).meta) || (valueDescriptor && (valueDescriptor as MetaHolder).meta) ? Util.getAttributeValueFormat(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType) : Util.getMetaValueFormat(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType));

    // Determine input type
    if (!inputType) {
        switch (valueDescriptor.name) {
            case WellknownValueTypes.TEXT:
            case WellknownValueTypes.EMAIL:
            case WellknownValueTypes.UUID:
            case WellknownValueTypes.ASSETID:
            case WellknownValueTypes.HOSTORIPADDRESS:
            case WellknownValueTypes.IPADDRESS:
                inputType = Util.getMetaValue(WellknownMetaItems.MULTILINE, valueHolder, valueHolderDescriptor) === true ? InputType.TEXTAREA : InputType.TEXT;
                break;
            case WellknownValueTypes.BOOLEAN:
                if (format && format.asNumber) {
                    inputType = InputType.NUMBER;
                    step = 1;
                    min = 0;
                    max = 1;
                    valueConverter = (v) => !!v;
                    break;
                }
                if (format && (format.asOnOff || format.asOpenClosed)) {
                    inputType = InputType.SWITCH;
                } else {
                    inputType = InputType.CHECKBOX;
                }

                if (format && format.asMomentary || (Util.getMetaValue(WellknownMetaItems.MOMENTARY, valueHolder, valueHolderDescriptor) === true) ) {
                    inputType = InputType.BUTTON_MOMENTARY;
                }
                break;
            case WellknownValueTypes.BIGNUMBER:
            case WellknownValueTypes.NUMBER:
            case WellknownValueTypes.POSITIVEINTEGER:
            case WellknownValueTypes.POSITIVENUMBER:
            case WellknownValueTypes.LONG:
            case WellknownValueTypes.INTEGER:
            case WellknownValueTypes.BYTE:
            case WellknownValueTypes.INTEGERBYTE:
            case WellknownValueTypes.DIRECTION:
            case WellknownValueTypes.TCPIPPORTNUMBER:
                if (valueDescriptor.name === WellknownValueTypes.BYTE || valueDescriptor.name === WellknownValueTypes.INTEGERBYTE) {
                    min = 0;
                    max = 255;
                    step = 1;
                } else if (valueDescriptor.name === WellknownValueTypes.INTEGER || valueDescriptor.name === WellknownValueTypes.LONG) {
                    step = 1;
                }
                if (format && format.asDate) {
                    inputType = InputType.DATETIME;
                } else if (format && format.asBoolean) {
                    inputType = InputType.CHECKBOX;
                    valueConverter = (v) => v ? 1 : 0;
                } else if (format && format.asSlider) {
                    inputType = InputType.RANGE;
                } else {
                    inputType = InputType.NUMBER;
                }
                break;
            case WellknownValueTypes.BIGINTEGER:
                inputType = InputType.BIG_INT;
                step = 1;
                break;
            case WellknownValueTypes.COLOURRGB:
                inputType = InputType.COLOUR;
                break;
            case WellknownValueTypes.DATEANDTIME:
            case WellknownValueTypes.TIMESTAMP:
            case WellknownValueTypes.TIMESTAMPISO8601:
                inputType = InputType.DATETIME;
                break;
            case WellknownValueTypes.CRONEXPRESSION:
                inputType = InputType.CRON;
                break;
            case WellknownValueTypes.TIMEDURATIONISO8601:
                inputType = InputType.DURATION_TIME;
                break;
            case WellknownValueTypes.PERIODDURATIONISO8601:
                inputType = InputType.DURATION_PERIOD;
                break;
            case WellknownValueTypes.TIMEANDPERIODDURATIONISO8601:
                inputType = InputType.DURATION;
                break;
            case WellknownValueTypes.JSONOBJECT:
                inputType = InputType.JSON_OBJECT;
                break;
        }

        if (valueDescriptor.arrayDimensions && valueDescriptor.arrayDimensions > 0) {
            inputType = InputType.JSON;
        }
    }

    if (!inputType) {
        switch (valueDescriptor.jsonType) {
            case "number":
            case "bigint":
                inputType = InputType.NUMBER;
                break;
            case "boolean":
                inputType = InputType.CHECKBOX;
                break;
            case "string":
                inputType = InputType.TEXT;
                break;
            case "date":
                inputType = InputType.DATETIME;
                break;
        }
    }

    if (!inputType) {
        inputType = InputType.JSON;
    }

    // Apply any constraints
    const sizeConstraint = constraints && constraints.find(c => c.type === "size") as ValueConstraintSize;
    const patternConstraint = constraints && constraints.find(c => c.type === "pattern") as ValueConstraintPattern;
    const minConstraint = constraints && constraints.find(c => c.type === "min") as ValueConstraintMin;
    const maxConstraint = constraints && constraints.find(c => c.type === "max") as ValueConstraintMax;
    const allowedValuesConstraint = constraints && constraints.find(c => c.type === "allowedValues") as ValueConstraintAllowedValues;
    const pastConstraint = constraints && constraints.find(c => c.type === "past") as ValueConstraintPast;
    const pastOrPresentConstraint = constraints && constraints.find(c => c.type === "pastOrPresent") as ValueConstraintPastOrPresent;
    const futureConstraint = constraints && constraints.find(c => c.type === "future") as ValueConstraintFuture;
    const futureOrPresentConstraint = constraints && constraints.find(c => c.type === "futureOrPresent") as ValueConstraintFutureOrPresent;
    const notEmptyConstraint = constraints && constraints.find(c => c.type === "notEmpty") as ValueConstraintNotEmpty;
    const notBlankConstraint = constraints && constraints.find(c => c.type === "notBlank") as ValueConstraintNotBlank;
    const notNullConstraint = constraints && constraints.find(c => c.type === "notNull") as ValueConstraintNotNull;

    if (sizeConstraint) {
        min = sizeConstraint.min;
        max = sizeConstraint.max;
    }
    if (sizeConstraint) {
        min = sizeConstraint.min;
        max = sizeConstraint.max;
    }
    if (minConstraint) {
        min = minConstraint.min;
    }
    if (maxConstraint) {
        max = maxConstraint.max;
    }
    if (patternConstraint) {
        pattern = patternConstraint.regexp;
    }
    if (notNullConstraint) {
        required = true;
    }
    if (notBlankConstraint && !pattern) {
        pattern = "\\S+";
    } else if (notEmptyConstraint && !pattern) {
        pattern = ".+";
    }
    if (allowedValuesConstraint && allowedValuesConstraint.allowedValues) {
        const allowedLabels = allowedValuesConstraint.allowedValueNames && allowedValuesConstraint.allowedValueNames.length === allowedValuesConstraint.allowedValues.length ? allowedValuesConstraint.allowedValueNames : undefined;
        selectOptions = allowedValuesConstraint.allowedValues.map((v, i) => {
            let label = allowedLabels ? allowedLabels[i] : "" + v;
            label = Util.getAllowedValueLabel(label)!;
            return [v, label || "" + v];
        });
        inputType = InputType.SELECT;

        if (valueDescriptor.arrayDimensions && valueDescriptor.arrayDimensions > 0) {
            multiple = true;
        }
    }

    if (inputType === InputType.DATETIME) {
        if (pastConstraint || pastOrPresentConstraint) {
            min = undefined;
            max = new Date();
        } else if (futureConstraint || futureOrPresentConstraint) {
            min = new Date();
            max = undefined;
        }

        // Refine the input type based on formatting
        if (format) {
            if (format.timeStyle && !format.dateStyle) {
                inputType = InputType.TIME;
            } else if (format.dateStyle && !format.timeStyle) {
                inputType = InputType.DATE;
            }
        }
    }

    if (inputType === InputType.NUMBER && format && format.resolution) {
        step = format.resolution;
    }

    if (inputType === InputType.COLOUR) {
        styles.marginLeft = "24px"
    }

    const supportsHelperText = inputTypeSupportsHelperText(inputType);
    const supportsLabel = inputTypeSupportsLabel(inputType);
    const supportsSendButton = inputTypeSupportsButton(inputType);
    const readonly = options.readonly;
    required = required || options.required;
    const comfortable = options.comfortable;
    const resizeVertical = options.resizeVertical;
    const inputRef: Ref<OrVaadinInput> = createRef();

    const templateFunction: ValueInputTemplateFunction = (value, focused, loading, sending, error, helperText) => {

        const disabled = options.disabled || loading || sending;
        const label = supportsLabel ? options.label : undefined;
        const inputStyle = styles ? styleMap(styles) : undefined;

        if(inputType && OrVaadinInput.TEMPLATES.has(inputType)) {
            return html`
                <or-vaadin-input ${ref(inputRef)} id="input" style="${ifDefined(inputStyle)}" type=${ifDefined(inputType)}
                                 label=${ifDefined(label)} value=${ifDefined(value)} pattern=${ifDefined(pattern)}
                                 min=${ifDefined(min)} max=${ifDefined(max)} format=${ifDefined(format)}
                                 ?autofocus=${focused} ?required=${required} ?multiple=${multiple}
                                 ?comfortable=${comfortable} ?readonly=${readonly} ?disabled=${disabled}
                                 options=${ifDefined(selectOptions)} step=${ifDefined(step)}
                                 helper-text="${ifDefined(helperText)}" ?resizeVertical="${resizeVertical}"
                                 ?rounded="${options.rounded}" ?outlined="${options.outlined}"
                                 @change="${(e: Event) => {
                                     e.stopPropagation();
                                     const elem = e.currentTarget as HTMLInputElement | undefined;
                                     if (elem?.checkValidity()) {
                                         const elemValue = JSON.parse(elem.value);
                                         valueChangeNotifier(valueConverter ? valueConverter(elemValue) : elemValue);
                                     }
                                 }}"
                ></or-vaadin-input>
            `;
        } else {
            // Fallback code to use deprecated or-mwc-input. This should be removed in the future, once all input types are supported and mapped.
            // Be aware: this function only creates the template, so it does not import or-mwc-input by default.
            return html`
                <or-mwc-input ${ref(inputRef)} id="input" style="${styleMap(styles)}" .type="${inputType}" .label="${label}" .value="${value}" .pattern="${pattern}"
                              .min="${min}" .max="${max}" .format="${format}" .focused="${focused}" .required="${required}" .multiple="${multiple}"
                              .options="${selectOptions}" .comfortable="${comfortable}" .readonly="${readonly}" .disabled="${disabled}" .step="${step}"
                              .helperText="${helperText}" .helperPersistent="${true}" .resizeVertical="${resizeVertical}"
                              .rounded="${options.rounded}" .outlined="${options.outlined}"
                              @or-mwc-input-changed="${(e: CustomEvent) => {
                                  e.stopPropagation();
                                  valueChangeNotifier(valueConverter ? valueConverter(e.detail.value) : e.detail.value);
                              }}"
                ></or-mwc-input>
            `;
        }
    };

    return {
        templateFunction: templateFunction,
        supportsHelperText: supportsHelperText,
        supportsSendButton: supportsSendButton,
        supportsLabel: supportsLabel,
        validator: () => {
            if (!inputRef.value) {
                return false;
            }
            return inputRef.value.checkValidity();
        }
    };
}
