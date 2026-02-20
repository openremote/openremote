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

export interface OrVaadinComponent {
    getAttributeNames(): string[];
}

/**
 * Returns whether the {@link InputType} should show a "send" button within the attribute input UI.
 * Some input types have internal mechanics for updating attributes, which is why they should return `false`.
 * Generic input types, like a text field, "support a send button", so should return `true`
 */
export function inputTypeSupportsSendButton(inputType: InputType): boolean {
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

export function inputTypeSupportsHelperText(inputType: InputType) {
    return inputTypeSupportsSendButton(inputType) || inputType === InputType.SELECT;
}

export function inputTypeSupportsLabel(inputType: InputType) {
    return inputTypeSupportsHelperText(inputType) || inputType === InputType.CHECKBOX || inputType === InputType.BUTTON_MOMENTARY;
}
