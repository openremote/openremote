var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, queryAssignedElements } from "lit/decorators.js";
import { OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
// language=CSS
const style = css `
    
    :host {
        display: block;
    }

    :host([hidden]) {
        display: none;
    }
`;
/**
 * This is a form element that supports any element that has a value property
 */
// TODO: Support any form element
let OrForm = class OrForm extends LitElement {
    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        // if (this._panel) {
        //     new SimpleBar(this._panel, {
        //         autoHide: this.autoHide,
        //         // @ts-ignore
        //         forceVisible: this.forceVisible
        //     });
        // }
    }
    render() {
        return html `
            <slot></slot>
        `;
    }
    checkValidity() {
        let valid = false;
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node).forEach(orMwcInput => {
            const inputValid = orMwcInput.checkValidity();
            valid = valid && inputValid;
        });
        return valid;
    }
    reportValidity() {
        let valid = true;
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node).forEach(orMwcInput => {
            const inputValid = orMwcInput.reportValidity();
            valid = valid && inputValid;
        });
        return valid;
    }
    submit() {
        const data = {};
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node).forEach(orMwcInput => {
            data[orMwcInput.name] = orMwcInput.value;
        });
        return data;
    }
    reset() {
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node).forEach(orMwcInput => {
            orMwcInput.value = undefined;
        });
    }
};
__decorate([
    queryAssignedElements()
], OrForm.prototype, "formNodes", void 0);
OrForm = __decorate([
    customElement("or-form")
], OrForm);
export { OrForm };
//# sourceMappingURL=or-form.js.map