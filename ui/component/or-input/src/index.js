var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement, property } from 'lit-element';
class OrRulesEditor extends LitElement {
    constructor() {
        super();
        // default value in minutes
        this.value = 0;
        // default value in minutes
        this.current = 0;
        // maxRange in minutes
        this.maxRange = 360;
        // minRange in minutes
        this.minRange = 0;
        // Steps in minutes
        this.step = 5;
    }
    render() {
        return html `
              <h1>hi</h1>
        `;
    }
}
__decorate([
    property({ type: Function })
], OrRulesEditor.prototype, "onChange", void 0);
__decorate([
    property({ type: Number })
], OrRulesEditor.prototype, "value", void 0);
__decorate([
    property({ type: Number })
], OrRulesEditor.prototype, "current", void 0);
__decorate([
    property({ type: Number })
], OrRulesEditor.prototype, "maxRange", void 0);
__decorate([
    property({ type: Number })
], OrRulesEditor.prototype, "minRange", void 0);
__decorate([
    property({ type: Number })
], OrRulesEditor.prototype, "step", void 0);
window.customElements.define('or-rules-editor', OrRulesEditor);
//# sourceMappingURL=index.js.map