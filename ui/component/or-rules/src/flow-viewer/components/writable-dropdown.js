var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { PickerStyle } from "../styles/picker-styles";
let WritableDropdown = class WritableDropdown extends LitElement {
    constructor() {
        super(...arguments);
        this.options = [];
    }
    static get styles() {
        return PickerStyle;
    }
    firstUpdated() {
        if (this.options.length > 0 && !this.value) {
            this.value = this.options[0].value;
        }
        this.selectElement.value = this.value;
    }
    shouldUpdate(_changedProperties) {
        if (this.selectElement && _changedProperties.has("value")) {
            this.selectElement.value = this.value;
        }
        return super.shouldUpdate(_changedProperties);
    }
    render() {
        return html `
        <select id="select-element" @change=${(e) => this.dispatchEvent(new Event("onchange", e))} @input=${(e) => { this.dispatchEvent(new InputEvent("oninput", e)); this.value = this.selectElement.value; }}>
            ${this.options.map((o) => html `<option value="${o.value}">${o.name}</option>`)}
        </select>
        `;
    }
};
__decorate([
    property({ type: Object, reflect: true })
], WritableDropdown.prototype, "value", void 0);
__decorate([
    property({ type: Array })
], WritableDropdown.prototype, "options", void 0);
__decorate([
    query("#select-element")
], WritableDropdown.prototype, "selectElement", void 0);
WritableDropdown = __decorate([
    customElement("writable-dropdown")
], WritableDropdown);
export { WritableDropdown };
//# sourceMappingURL=writable-dropdown.js.map