var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement } from "lit";
import { property } from "lit/decorators.js";
import { style } from "../style";
export class WidgetSettingsChangedEvent extends CustomEvent {
    constructor(widgetConfig) {
        super(WidgetSettingsChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: widgetConfig
        });
    }
}
WidgetSettingsChangedEvent.NAME = "settings-changed";
export class WidgetSettings extends LitElement {
    static get styles() {
        return [style];
    }
    constructor(config) {
        super();
        this.widgetConfig = config;
    }
    // Lit lifecycle for "on every update" which triggers on every property/state change
    willUpdate(changedProps) {
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            this.dispatchEvent(new WidgetSettingsChangedEvent(this.widgetConfig));
        }
    }
    notifyConfigUpdate() {
        this.requestUpdate('widgetConfig');
    }
}
__decorate([
    property()
], WidgetSettings.prototype, "widgetConfig", void 0);
//# sourceMappingURL=widget-settings.js.map