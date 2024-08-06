var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { createId, isControl, mapDispatchToControlProps, removeId } from "@jsonforms/core";
import { property } from "lit/decorators.js";
import { BaseElement } from "../base-element";
export class ControlBaseElement extends BaseElement {
    constructor() {
        super();
    }
    updated(_changedProperties) {
        super.updated(_changedProperties);
        if (_changedProperties.has("state")) {
            const { handleChange } = mapDispatchToControlProps(this.state.dispatch);
            this.handleChange = handleChange;
        }
    }
    shouldUpdate(changedProperties) {
        var _a;
        if (changedProperties.has("uischema")) {
            if (isControl(this.uischema)) {
                const oldSchemaValue = changedProperties.get("uischema");
                if ((oldSchemaValue === null || oldSchemaValue === void 0 ? void 0 : oldSchemaValue.scope) !== ((_a = this.uischema) === null || _a === void 0 ? void 0 : _a.scope)) {
                    if (this.id) {
                        removeId(this.id);
                    }
                    this.id = createId(this.uischema.scope);
                }
            }
        }
        return true;
    }
    disconnectedCallback() {
        if (isControl(this.uischema)) {
            removeId(this.id);
        }
    }
}
__decorate([
    property()
], ControlBaseElement.prototype, "description", void 0);
__decorate([
    property()
], ControlBaseElement.prototype, "rootSchema", void 0);
//# sourceMappingURL=control-base-element.js.map