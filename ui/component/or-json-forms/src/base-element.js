var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement } from "lit";
import { property } from "lit/decorators.js";
export class BaseElement extends LitElement {
    set props(props) {
        delete props.id;
        Object.assign(this, props);
    }
}
__decorate([
    property({ type: Object })
], BaseElement.prototype, "state", void 0);
__decorate([
    property({ type: Object })
], BaseElement.prototype, "uischema", void 0);
__decorate([
    property({ type: Object })
], BaseElement.prototype, "schema", void 0);
__decorate([
    property({ type: String, attribute: false })
], BaseElement.prototype, "data", void 0);
__decorate([
    property({ type: Array })
], BaseElement.prototype, "renderers", void 0);
__decorate([
    property({ type: Array })
], BaseElement.prototype, "cells", void 0);
__decorate([
    property({ type: String, attribute: false })
], BaseElement.prototype, "config", void 0);
__decorate([
    property({ type: Array })
], BaseElement.prototype, "uischemas", void 0);
__decorate([
    property({ type: Boolean })
], BaseElement.prototype, "enabled", void 0);
__decorate([
    property({ type: Boolean })
], BaseElement.prototype, "visible", void 0);
__decorate([
    property({ type: String })
], BaseElement.prototype, "path", void 0);
__decorate([
    property({ type: String })
], BaseElement.prototype, "label", void 0);
__decorate([
    property({ type: Boolean })
], BaseElement.prototype, "required", void 0);
__decorate([
    property()
], BaseElement.prototype, "errors", void 0);
//# sourceMappingURL=base-element.js.map