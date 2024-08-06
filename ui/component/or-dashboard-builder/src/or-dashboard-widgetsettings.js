var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { style } from './style';
import { i18next } from "@openremote/or-translate";
import { until } from "lit/directives/until.js";
import { WidgetService } from "./service/widget-service";
import { WidgetSettingsChangedEvent } from "./util/widget-settings";
import { guard } from "lit/directives/guard.js";
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");
/* ------------------------------------ */
let OrDashboardWidgetsettings = class OrDashboardWidgetsettings extends LitElement {
    static get styles() {
        return [unsafeCSS(tableStyle), style];
    }
    /* ------------------------------------ */
    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes, force = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('update', { detail: { changes: changes, force: force } }));
    }
    render() {
        var _a;
        return html `
            <div style="padding: 12px;">
                <div>
                    <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;" .value="${(_a = this.selectedWidget) === null || _a === void 0 ? void 0 : _a.displayName}" label="${i18next.t('name')}"
                                  @or-mwc-input-changed="${(event) => this.setDisplayName(event.detail.value)}"
                    ></or-mwc-input>
                </div>
            </div>
            <div>
                ${guard([this.selectedWidget], () => html `
                    ${until(this.generateContent(this.selectedWidget.widgetTypeId), html `Loading...`)}
                `)}
            </div>
        `;
    }
    setDisplayName(name) {
        this.selectedWidget.displayName = name;
        this.forceParentUpdate(new Map([['widget', this.selectedWidget]]));
    }
    generateContent(widgetTypeId) {
        return __awaiter(this, void 0, void 0, function* () {
            if (!this.settingsElem || this.settingsElem.id !== this.selectedWidget.id) {
                const manifest = WidgetService.getManifest(widgetTypeId);
                this.settingsElem = this.initSettings(manifest);
            }
            return html `
            ${this.settingsElem}
        `;
        });
    }
    initSettings(manifest) {
        const settingsElem = manifest.getSettingsHtml(this.selectedWidget.widgetConfig);
        settingsElem.id = this.selectedWidget.id;
        settingsElem.getDisplayName = () => this.selectedWidget.displayName;
        settingsElem.setDisplayName = (name) => this.setDisplayName(name);
        settingsElem.getEditMode = () => true;
        settingsElem.getWidgetLocation = () => {
            var _a, _b, _c, _d;
            return ({
                x: (_a = this.selectedWidget.gridItem) === null || _a === void 0 ? void 0 : _a.x,
                y: (_b = this.selectedWidget.gridItem) === null || _b === void 0 ? void 0 : _b.y,
                w: (_c = this.selectedWidget.gridItem) === null || _c === void 0 ? void 0 : _c.w,
                h: (_d = this.selectedWidget.gridItem) === null || _d === void 0 ? void 0 : _d.h
            });
        };
        settingsElem.addEventListener(WidgetSettingsChangedEvent.NAME, (ev) => this.onWidgetConfigChange(ev));
        return settingsElem;
    }
    onWidgetConfigChange(ev) {
        this.selectedWidget.widgetConfig = ev.detail;
        this.forceParentUpdate(new Map([['widget', this.selectedWidget]]));
    }
};
__decorate([
    property({ type: Object })
], OrDashboardWidgetsettings.prototype, "selectedWidget", void 0);
__decorate([
    state()
], OrDashboardWidgetsettings.prototype, "settingsElem", void 0);
OrDashboardWidgetsettings = __decorate([
    customElement("or-dashboard-widgetsettings")
], OrDashboardWidgetsettings);
export { OrDashboardWidgetsettings };
//# sourceMappingURL=or-dashboard-widgetsettings.js.map