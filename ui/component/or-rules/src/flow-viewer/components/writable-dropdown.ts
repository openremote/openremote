import { LitElement, html, PropertyValues } from "lit";
import {customElement, property, query} from "lit/decorators.js";
import { PickerStyle } from "../styles/picker-styles";

@customElement("writable-dropdown")
export class WritableDropdown extends LitElement {
    @property({ type: Object, reflect: true }) public value?: any;
    @property({ type: Array }) public options: { value: any, name: string }[] = [];

    @query("#select-element") public selectElement!: HTMLSelectElement;

    public static get styles() {
        return PickerStyle;
    }

    protected firstUpdated() {
        if (this.options.length > 0 && !this.value) {
            this.value = this.options[0].value;
        }
        this.selectElement.value = this.value;
    }

    protected shouldUpdate(_changedProperties: PropertyValues) {
        if (this.selectElement && _changedProperties.has("value")) {
            this.selectElement.value = this.value;
        }

        return super.shouldUpdate(_changedProperties);
    }

    protected render() {
        return html`
        <select id="select-element" @change=${(e: Event) => this.dispatchEvent(new Event("onchange", e))} @input=${(e: InputEvent) => { this.dispatchEvent(new InputEvent("oninput", e)); this.value = this.selectElement.value; }}>
            ${this.options.map((o) => html`<option value="${o.value}">${o.name}</option>`)}
        </select>
        `;
    }
}
