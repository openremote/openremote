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
import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "./or-vaadin-numberfield";
import "./or-vaadin-textfield";
import "./or-vaadin-textarea";
import "./or-vaadin-checkbox";

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

@customElement("or-vaadin-input")
export class OrVaadinInput extends LitElement {

    static readonly FORBIDDEN_ATTRIBUTES = ["id"];

    @property({type: String})
    public type: InputType = InputType.TEXT;

    @query("#elem")
    protected _elem?: HTMLInputElement;

    protected _observer = new MutationObserver((mutations, observer) => this._onAttributeChange(mutations, observer));

    static get styles() {
        return css`
            #elem {
                width: 100%;
            }
            or-vaadin-textarea {
                max-height: 200px;
            }
        `;
    }

    connectedCallback() {
        this._observer.observe(this, { attributes: true });
        return super.connectedCallback();
    }

    disconnectedCallback() {
        this._observer.disconnect();
        return super.disconnectedCallback();
    }

    firstUpdated(_changedProps: PropertyValues) {
        for(const name of this.getAttributeNames()) {
            this._applyAttribute(name, this.getAttribute(name), this._elem);
        }
        return super.firstUpdated(_changedProps);
    }

    public get native(): HTMLInputElement | undefined {
        return this._elem;
    }

    public get nativeValue(): any {
        return this._elem?.value;
    }

    public checkValidity() {
        return this._elem?.checkValidity() ?? false;
    }

    render() {
        return this._getInputTemplate(this.type);
    }

    protected _onAttributeChange(mutations: MutationRecord[], observer: MutationObserver) {
        this.getUpdateComplete().then(() => {
            mutations
                .filter(mutation => mutation.type === "attributes" && mutation.attributeName)
                .forEach(mutation => {
                    this._applyAttribute(mutation.attributeName!, this.getAttribute(mutation.attributeName!));
                });
        });
    }

    /**
     *
     * @param type
     * @protected
     */
    protected _getInputTemplate(type: InputType): TemplateResult {
        switch (type) {
            case InputType.CHECKBOX: {
                return html`<or-vaadin-checkbox id="elem" @change=${this._onValueChange}></or-vaadin-checkbox>`;
            }
            case InputType.RADIO: {
                return html`Not supported yet.`;
            }
            case InputType.SWITCH: {
                return html`Not supported yet.`;
            }
            case InputType.NUMBER:
            case InputType.RANGE: {
                return html`<or-vaadin-numberfield id="elem" @change=${this._onValueChange}></or-vaadin-numberfield>`;
            }
            case InputType.JSON:
            case InputType.JSON_OBJECT:
            case InputType.TEXTAREA: {
                return html`<or-vaadin-textarea id="elem" @change=${this._onValueChange}></or-vaadin-textarea>`;
            }
            default: {
                return html`<or-vaadin-textfield id="elem" @change=${this._onValueChange}></or-vaadin-textfield>`;
            }
        }
    }

    protected _onValueChange(e: Event) {
        e.stopPropagation();
        if (e.defaultPrevented) return;
        const value = (e.currentTarget as HTMLInputElement)?.value ?? this.nativeValue;
        this.dispatchEvent(new CustomEvent("change", { detail: { value: value }, bubbles: true, composed: true }));
    }

    protected _applyAttribute(name: string, value?: any, elem = this._elem) {
        if(elem && !OrVaadinInput.FORBIDDEN_ATTRIBUTES.includes(name)) {
            switch (typeof value) {
                case "boolean": {
                    elem?.toggleAttribute(name, value);
                    break;
                }
                default: {
                    if(value !== null) {
                        elem?.setAttribute(name, value);
                    } else {
                        elem?.removeAttribute(name);
                    }
                    break;
                }
            }
        }
    }

}
