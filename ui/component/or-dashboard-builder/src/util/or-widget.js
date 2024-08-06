var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement } from "lit";
import { property } from "lit/decorators.js";
// Main OrWidget class where all widgets extend their functionality on.
// It contains several methods used for rendering by the parent component; OrDashboardWidget
export class OrWidget extends LitElement {
    constructor(config) {
        super();
        this.widgetConfig = config;
    }
    static get styles() {
        return [];
    }
    /* --------------------------- */
    static getManifest() {
        if (!this.manifest) {
            throw new Error(`No manifest present on ${this.name}`);
        }
        return this.manifest;
    }
}
__decorate([
    property({ type: Object })
], OrWidget.prototype, "widgetConfig", void 0);
//# sourceMappingURL=or-widget.js.map