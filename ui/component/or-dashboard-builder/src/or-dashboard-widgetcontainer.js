var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import { throttle } from "lodash";
import { style } from "./style";
import { WidgetService } from "./service/widget-service";
/* ------------------------------------ */
const elemTagName = "or-dashboard-widget-container";
let OrDashboardWidgetContainer = class OrDashboardWidgetContainer extends LitElement {
    constructor() {
        super(...arguments);
        this.loading = false;
    }
    static get styles() {
        return [style];
    }
    disconnectedCallback() {
        var _a;
        super.disconnectedCallback();
        (_a = this.resizeObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
    }
    shouldUpdate(changedProps) {
        var _a, _b, _c;
        const changed = changedProps;
        // Update config if some values in the spec are not set.
        // Useful for when migrations have taken place.
        if (this.widget) {
            const manifest = WidgetService.getManifest(this.widget.widgetTypeId);
            if (manifest) {
                this.widget.widgetConfig = WidgetService.correctToConfigSpec(manifest, this.widget.widgetConfig);
            }
        }
        // Only update widget if certain properties of widget has changed.
        // For example, when the 'gridItem' field changes, no update is needed since it doesn't apply here.
        if (changedProps.has('widget') && this.widget) {
            const oldVal = changedProps.get('widget');
            const idChanged = (oldVal === null || oldVal === void 0 ? void 0 : oldVal.id) !== ((_a = this.widget) === null || _a === void 0 ? void 0 : _a.id);
            const nameChanged = (oldVal === null || oldVal === void 0 ? void 0 : oldVal.displayName) !== ((_b = this.widget) === null || _b === void 0 ? void 0 : _b.displayName);
            const configChanged = JSON.stringify(oldVal === null || oldVal === void 0 ? void 0 : oldVal.widgetConfig) !== JSON.stringify((_c = this.widget) === null || _c === void 0 ? void 0 : _c.widgetConfig);
            if (!(idChanged || nameChanged || configChanged)) {
                changed.delete('widget');
            }
        }
        return (changed.size === 0 ? false : super.shouldUpdate(changedProps));
    }
    willUpdate(changedProps) {
        super.willUpdate(changedProps);
        if (!this.manifest && this.widget) {
            this.manifest = WidgetService.getManifest(this.widget.widgetTypeId);
        }
        // Create widget
        if (changedProps.has("widget") && this.widget) {
            this.initializeWidgetElem(this.manifest, this.widget.widgetConfig);
        }
    }
    firstUpdated(changedProps) {
        var _a;
        super.firstUpdated(changedProps);
        if (this.orWidget) {
            const containerElem = this.containerElem;
            if (containerElem) {
                (_a = this.resizeObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
                this.resizeObserver = new ResizeObserver(throttle(() => {
                    const minWidth = this.manifest.minPixelWidth || 0;
                    const minHeight = this.manifest.minPixelHeight || 0;
                    const isMinimumSize = (minWidth < containerElem.clientWidth) && (minHeight < containerElem.clientHeight);
                    this.error = (isMinimumSize ? undefined : "dashboard.widgetTooSmall");
                }, 200));
                this.resizeObserver.observe(containerElem);
            }
            else {
                console.error("gridItemElement could not be found!");
            }
        }
    }
    initializeWidgetElem(manifest, config) {
        console.debug(`Initialising ${manifest.displayName} widget..`);
        if (this.orWidget) {
            this.orWidget.remove();
        }
        this.orWidget = manifest.getContentHtml(config);
        this.orWidget.getDisplayName = () => this.widget.displayName;
        this.orWidget.getEditMode = () => this.editMode;
        this.orWidget.getWidgetLocation = () => {
            var _a, _b, _c, _d;
            return ({
                x: (_a = this.widget.gridItem) === null || _a === void 0 ? void 0 : _a.x,
                y: (_b = this.widget.gridItem) === null || _b === void 0 ? void 0 : _b.y,
                w: (_c = this.widget.gridItem) === null || _c === void 0 ? void 0 : _c.w,
                h: (_d = this.widget.gridItem) === null || _d === void 0 ? void 0 : _d.h
            });
        };
    }
    render() {
        const showHeader = !!this.widget.displayName;
        return html `
            <div id="widget-container" style="height: calc(100% - 16px); padding: 8px 16px 8px 16px; display: flex; flex-direction: column;">

                <!-- Container title -->
                ${when(showHeader, () => {
            var _a;
            return html `
                    <div style="flex: 0 0 36px; display: flex; justify-content: space-between; align-items: center;">
                        <span class="panel-title" style="width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                            ${(_a = this.widget.displayName) === null || _a === void 0 ? void 0 : _a.toUpperCase()}
                        </span>
                    </div>
                `;
        })}

                <!-- Content -->
                <div style="flex: 1; max-height: ${showHeader ? 'calc(100% - 36px)' : '100%'};">
                    ${when((!this.error && !this.loading), () => html `
                        ${this.orWidget}
                    `, () => html `<or-translate value="${this.error ? this.error : "loading"}"></or-translate>`)}
                </div>
            </div>
        `;
    }
    refreshContent(force) {
        var _a;
        (_a = this.orWidget) === null || _a === void 0 ? void 0 : _a.refreshContent(force);
    }
};
OrDashboardWidgetContainer.tagName = elemTagName;
__decorate([
    property()
], OrDashboardWidgetContainer.prototype, "widget", void 0);
__decorate([
    property()
], OrDashboardWidgetContainer.prototype, "editMode", void 0);
__decorate([
    property()
], OrDashboardWidgetContainer.prototype, "loading", void 0);
__decorate([
    state()
], OrDashboardWidgetContainer.prototype, "orWidget", void 0);
__decorate([
    state()
], OrDashboardWidgetContainer.prototype, "error", void 0);
__decorate([
    query("#widget-container")
], OrDashboardWidgetContainer.prototype, "containerElem", void 0);
OrDashboardWidgetContainer = __decorate([
    customElement(elemTagName)
], OrDashboardWidgetContainer);
export { OrDashboardWidgetContainer };
//# sourceMappingURL=or-dashboard-widgetcontainer.js.map