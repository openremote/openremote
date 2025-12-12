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
import {InputType} from "./util";
import {OrVaadinCheckbox} from "./or-vaadin-checkbox";
import "./or-vaadin-checkbox";
import "./or-vaadin-numberfield";
import "./or-vaadin-passwordfield";
import "./or-vaadin-textfield";
import "./or-vaadin-textarea";
import "./or-vaadin-select";

/**
 * Function to register properties (get/setter) of a CustomElement list to the child Vaadin element.
 * When the Vaadin element does not recognize the HTML attribute or class property, it is ignored.
 * @param constructors - List of CustomElements to scan for available properties
 */
function registerProperties(constructors: (CustomElementConstructor | undefined)[]) {
    constructors
        .filter(constr => !!constr)
        .map(contr => contr as typeof LitElement)
        .forEach(constr => constr.observedAttributes
            .filter(attr => !OrVaadinInput.elementProperties.has(attr))
            .forEach(attr => OrVaadinInput.createProperty(attr, constr.getPropertyOptions(attr)))
        );
}

/**
 * Custom element that wraps various Vaadin input components based on the specified `type`.
 * Provides a unified interface for interacting with different types of inputs.
 *
 * @customElement "or-vaadin-input"
 */
@customElement("or-vaadin-input")
export class OrVaadinInput extends LitElement {

    /**
     * Static list of Vaadin component classes to scan through during attribute changes.
     * It's important that the complete list of CustomElements is defined here.
     */
    public static readonly VAADIN_CLASSES: (CustomElementConstructor | undefined)[] = [
        customElements.get("or-vaadin-numberfield"),
        customElements.get("or-vaadin-passwordfield"),
        customElements.get("or-vaadin-select"),
        customElements.get("or-vaadin-textarea"),
        customElements.get("or-vaadin-textfield")
    ];

    /**
     * Static map of template functions that are used to render the Vaadin components.
     * For example, {@link InputType.CHECKBOX} generates a `<or-vaadin-checkbox>` element.
     * The map value contains a function, with an {@link onChange} callback parameter for handling events.
     */
    public static readonly TEMPLATES = new Map<InputType, (onChange: (ev: Event) => void) => TemplateResult>([
        [InputType.CHECKBOX, OrVaadinInput.getCheckboxTemplate],
        [InputType.BIG_INT, OrVaadinInput.getNumberFieldTemplate],
        [InputType.NUMBER, OrVaadinInput.getNumberFieldTemplate],
        [InputType.TEXTAREA, OrVaadinInput.getTextAreaTemplate],
        [InputType.TEXT, OrVaadinInput.getTextFieldTemplate],
        [InputType.PASSWORD, OrVaadinInput.getPasswordFieldTemplate],
        [InputType.SELECT, OrVaadinInput.getSelectTemplate]
    ]);

    /**
     * List of forbidden attributes that are not processed nor "bubbled down" to the child Vaadin component.
     * So, `<or-vaadin-input id="myId" type="text">`, wouldn't render `<or-vaadin-textfield id="myId">`, but `<or-vaadin-textfield>`.
     */
    public static readonly FORBIDDEN_ATTRIBUTES = ["id"];

    /**
     * The input type that determines which Vaadin component to render.
     * Refer to {@link InputType} and {@link OrVaadinInput.TEMPLATES} for the supported types.
     */
    @property({type: String})
    public type: InputType = InputType.TEXT;

    @query("#elem")
    protected _elem?: HTMLInputElement;

    /**
     * Internal observer for tracking attribute updates on the root element.
     * @protected
     */
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

    updated(changedProps: PropertyValues) {
        changedProps.forEach((_, key) => this._onPropertyChange(String(key)));
        return super.updated(changedProps);
    }

    firstUpdated(_changedProps: PropertyValues) {
        for(const name of this.getAttributeNames()) {
            this._applyAttribute(name, this.getAttribute(name), this._elem);
        }
        return super.firstUpdated(_changedProps);
    }

    /**
     * Returns the native Vaadin input element.
     */
    public get native(): HTMLInputElement | undefined {
        return this._elem;
    }

    /**
     * Returns the value of the native Vaadin input element.
     */
    public get nativeValue(): any {
        switch (this.type) {
            case InputType.CHECKBOX: {
                return (this._elem as OrVaadinCheckbox | undefined)?.checked;
            }
            default: {
                return this._elem?.value;
            }
        }
    }

    /**
     * Function that returns a boolean which indicates if the element meets any constraint validation rules applied to it.
     * Refer to https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/checkValidity for more information.
     */
    public checkValidity(): boolean {
        return this._elem?.checkValidity() ?? false;
    }

    render() {
        return OrVaadinInput.TEMPLATES.get(this.type)?.(this._onValueChange) ?? html`Not supported yet.`;
    }

    /**
     * Internal {@link MutationCallback} function for handling attribute changes from an {@link MutationObserver}.
     * @param mutations - Array of mutations occurring in a single event
     * @param _observer - The observer causing the {@link MutationCallback}
     * @protected
     */
    protected _onAttributeChange(mutations: MutationRecord[], _observer: MutationObserver) {
        this.getUpdateComplete().then(() => {
            mutations
                .filter(mutation => mutation.type === "attributes" && mutation.attributeName)
                .forEach(mutation => {
                    this._applyAttribute(mutation.attributeName!, this.getAttribute(mutation.attributeName!), this._elem);
                });
        });
    }

    /**
     * Internal function for handling property changes during the Lit lifecycle.
     * @param name - Property key that was changed
     * @param newValue - (optional) the new value to apply
     * @protected
     */
    protected _onPropertyChange(name: string, newValue?: any) {
        newValue ??= (this as Record<string, unknown>)[String(name)];
        this._applyAttribute(name, JSON.stringify(newValue), this._elem);
    }

    /**
     * Internal callback function when a user changes the value inside the Vaadin element.
     * Normally this is caused by an `@change` event.
     * @param ev Event that occurred
     * @protected
     */
    protected _onValueChange(ev: Event) {
        ev.stopPropagation();
        if (ev.defaultPrevented) return;
        this.dispatchEvent(new CustomEvent("change", { bubbles: true }));
    }

    /**
     * Internal function to apply an attribute to the child Vaadin element.
     * @param name - Attribute name
     * @param value - Value to apply
     * @param elem - Optional {@link HTMLInputElement} to apply the change to
     * @protected
     */
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

    public static getCheckboxTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-checkbox id="elem" @change=${onChange}></or-vaadin-checkbox>`;
    }

    public static getNumberFieldTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-numberfield id="elem" @change=${onChange}></or-vaadin-numberfield>`;
    }

    public static getTextAreaTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-textarea id="elem" @change=${onChange}></or-vaadin-textarea>`;
    }

    public static getTextFieldTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-textfield id="elem" @change=${onChange}></or-vaadin-textfield>`;
    }

    public static getPasswordFieldTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-passwordfield id="elem" @change=${onChange}></or-vaadin-passwordfield>`;
    }

    public static getSelectTemplate(onChange?: (e: Event) => void) {
        return html`<or-vaadin-select id="elem" @change=${onChange}></or-vaadin-select>`;
    }
}

// Before the class is initialized, register the property fields of the underlying Vaadin classes
registerProperties(OrVaadinInput.VAADIN_CLASSES);
