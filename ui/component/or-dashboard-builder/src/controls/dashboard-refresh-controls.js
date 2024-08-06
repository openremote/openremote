var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
export function intervalToMillis(interval) {
    switch (interval) {
        case "OFF" /* DashboardRefreshInterval.OFF */:
            return undefined;
        case "ONE_MIN" /* DashboardRefreshInterval.ONE_MIN */:
            return (60 * 1000);
        case "FIVE_MIN" /* DashboardRefreshInterval.FIVE_MIN */:
            return (5 * 60 * 1000);
        case "QUARTER" /* DashboardRefreshInterval.QUARTER */:
            return (15 * 60 * 1000);
        case "ONE_HOUR" /* DashboardRefreshInterval.ONE_HOUR */:
            return (60 * 60 * 1000);
        default:
            return undefined;
    }
}
export class IntervalSelectEvent extends CustomEvent {
    constructor(interval) {
        super(IntervalSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: interval
        });
    }
}
IntervalSelectEvent.NAME = "interval-select";
let DashboardRefreshControls = class DashboardRefreshControls extends LitElement {
    constructor() {
        super(...arguments);
        this.interval = "OFF" /* DashboardRefreshInterval.OFF */;
        this.readonly = true;
        // TODO: Replace this with Object.values(), after generated typescript enums support this. ('const' variable issue in model.ts)
        this.intervalOptions = ["OFF" /* DashboardRefreshInterval.OFF */, "ONE_MIN" /* DashboardRefreshInterval.ONE_MIN */, "FIVE_MIN" /* DashboardRefreshInterval.FIVE_MIN */, "QUARTER" /* DashboardRefreshInterval.QUARTER */, "ONE_HOUR" /* DashboardRefreshInterval.ONE_HOUR */];
    }
    willUpdate(changedProps) {
        super.willUpdate(changedProps);
        if (changedProps.has("interval") && this.interval !== undefined) {
            this.dispatchEvent(new IntervalSelectEvent(this.interval));
        }
    }
    render() {
        const intervalOptions = this.getRefreshOptions();
        const value = this.getIntervalString(this.interval);
        return html `
            <div style="height: 100%; display: flex; align-items: center;">
                ${when(this.readonly, () => html `
                    ${when(this.interval === "OFF" /* DashboardRefreshInterval.OFF */, () => html `
                        <or-mwc-input .type="${InputType.BUTTON}" icon="pause" disabled="true" style="height: 36px; margin-top: -12px;"></or-mwc-input>
                    `, () => html `
                        <or-mwc-input .type="${InputType.BUTTON}" label="${value}" disabled="true"></or-mwc-input>
                    `)}
                `, () => html `
                    ${getContentWithMenuTemplate(this.interval === "OFF" /* DashboardRefreshInterval.OFF */ ? html `
                                <or-mwc-input .type="${InputType.BUTTON}" icon="pause" style="height: 36px; margin-top: -12px;"></or-mwc-input>
                            ` : html `
                                <or-mwc-input .type="${InputType.BUTTON}" label="${value}"></or-mwc-input>
                            `, intervalOptions.map(o => ({ value: o })), value, (newVal) => this.onIntervalSelect(intervalOptions, newVal), undefined, false, true, true)}
                `)}
            </div>
        `;
    }
    onIntervalSelect(stringOptions, value) {
        this.interval = this.intervalOptions[stringOptions.indexOf(value)];
    }
    getIntervalString(interval) {
        return (`dashboard.interval.${interval.toLowerCase()}`);
    }
    getRefreshOptions() {
        return this.intervalOptions.map(interval => this.getIntervalString(interval));
    }
};
__decorate([
    property()
], DashboardRefreshControls.prototype, "interval", void 0);
__decorate([
    property()
], DashboardRefreshControls.prototype, "readonly", void 0);
DashboardRefreshControls = __decorate([
    customElement("dashboard-refresh-controls")
], DashboardRefreshControls);
export { DashboardRefreshControls };
//# sourceMappingURL=dashboard-refresh-controls.js.map