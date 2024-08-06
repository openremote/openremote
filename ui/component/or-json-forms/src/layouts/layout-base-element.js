var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { property } from "lit/decorators.js";
import { BaseElement } from "../base-element";
export class LayoutBaseElement extends BaseElement {
    constructor() {
        super(...arguments);
        this.direction = "column";
    }
    getChildProps() {
        return (this.uischema && this.uischema.elements ? this.uischema.elements : []).map((el) => {
            const props = {
                renderers: this.renderers,
                uischema: el,
                schema: this.schema,
                path: this.path
            };
            return props;
        });
    }
}
__decorate([
    property({ type: String })
], LayoutBaseElement.prototype, "direction", void 0);
//# sourceMappingURL=layout-base-element.js.map