var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import moment from "moment";
import { buttonStyle } from "../style";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import { TimeTriggerType } from "../index";
import { Util } from "@openremote/core";
import { OrMwcDialogOpenedEvent } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrMapClickedEvent } from "@openremote/or-map";
import { i18next } from "@openremote/or-translate";
// language=CSS
const style = css `
    
    ${buttonStyle}
    
    :host {
        display: block;
    }
    
    .trigger-group {
        flex-grow: 1;
        display: flex;
        align-items: center;
        flex-direction: row;
        flex-wrap: wrap;
    }
    .min-width {
        min-width: 200px;
    }
    .width {
        width: 200px;
    }
    
    .trigger-group > * {
        margin: 10px 3px 6px 3px;
    }
`;
let OrRuleTriggerQuery = class OrRuleTriggerQuery extends LitElement {
    static get styles() {
        return style;
    }
    constructor() {
        super();
        this.readonly = false;
        this.triggerOptions = [];
        Object.values(TimeTriggerType).forEach((type) => { this.triggerOptions.push({ key: type, value: this.triggerToString(type) }); });
        this.getSunPositions().forEach((opt) => { this.triggerOptions.push({ key: opt, value: this.triggerToString(opt) }); });
        this.selectedTrigger = this.triggerOptions[0];
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initMap);
    }
    updated(changedProperties) {
        if (changedProperties.has('condition')) {
            if (this.condition.cron) {
                this.selectedTrigger = { key: TimeTriggerType.TIME_OF_DAY, value: this.triggerToString(TimeTriggerType.TIME_OF_DAY) };
            }
            else if (this.condition.sun) {
                this.selectedTrigger = { key: this.condition.sun.position, value: this.triggerToString(this.condition.sun.position) };
            }
        }
    }
    /* ---------------------- */
    initMap() {
        const modal = this.shadowRoot.getElementById('map-modal');
        if (!modal)
            return;
        const map = modal.shadowRoot.querySelector('.or-map');
        if (map) {
            map.addEventListener(OrMapClickedEvent.NAME, (evt) => {
                const lngLat = evt.detail.lngLat;
                this.setLocation({ type: 'Point', coordinates: [lngLat.lat, lngLat.lng] });
                const latElement = modal.shadowRoot.querySelector('.location-lat');
                const lngElement = modal.shadowRoot.querySelector('.location-lng');
                latElement.value = lngLat.lat;
                lngElement.value = lngLat.lng;
            });
        }
    }
    renderDialogHTML(point) {
        const dialog = this.shadowRoot.getElementById("map-modal");
        if (dialog) {
            dialog.content = html `
                <div style="display:grid">
                    <or-map class="or-map" type="VECTOR" style="border: 1px solid #d5d5d5; height: 400px; min-width: 300px; margin-bottom: 20px;">
                        ${(point && point.coordinates) ? html `
                            <or-map-marker class="or-map-marker" active color="#FF0000" icon="white-balance-sunny" lat="${point.coordinates[0]}" lng="${point.coordinates[1]}"></or-map-marker>
                        ` : undefined}
                    </or-map>
                    <div class="layout horizontal">
                        <input hidden class="location-lng"  required placeholder=" " type="text" .value="${point && point.coordinates ? point.coordinates[0] : null}" />
                        <input hidden class="location-lat" required placeholder=" " type="text" .value="${point && point.coordinates ? point.coordinates[1] : null}" />
                    </div>
                </div>
            `;
        }
    }
    /* ----------------------------- */
    render() {
        var _a, _b;
        const modalActions = [
            { actionName: "close", default: true, content: html `<or-mwc-input class="button" .type="${InputType.BUTTON}" label="close"></or-mwc-input>`, action: () => { } }
        ];
        const onMapModalOpen = () => {
            var _a;
            const dialog = this.shadowRoot.getElementById("map-modal");
            if (dialog) {
                dialog.dismissAction = null;
                dialog.open();
                this.renderDialogHTML((_a = this.condition.sun) === null || _a === void 0 ? void 0 : _a.location);
            }
        };
        // Render dialog on every update (for example when changing location in the modal itself)
        this.renderDialogHTML((_a = this.condition.sun) === null || _a === void 0 ? void 0 : _a.location);
        return html `
            <div class="trigger-group">
                <or-mwc-input class="min-width" type="${InputType.SELECT}" .options="${this.triggerOptions.map((t) => t.value)}" .value="${this.selectedTrigger.value}" label="${i18next.t('triggerType')}"
                              @or-mwc-input-changed="${(ev) => { this.setTrigger(this.triggerOptions.find((t) => t.value == ev.detail.value)); }}">
                </or-mwc-input>
                ${this.selectedTrigger ? html `
                    ${this.selectedTrigger.key == this.triggerOptions[0].key ? html `
                        <or-mwc-input class="min-width" type="${InputType.TIME}" .value="${(this.condition.cron ? moment(Util.cronStringToISOString(this.condition.cron, true)).format('HH:mm') : undefined)}" label="${i18next.t('timeOfDay')}"
                                      @or-mwc-input-changed="${(ev) => { this.setTime(ev.detail.value); }}">
                        </or-mwc-input>
                    ` : html `
                        <or-mwc-input class="min-width width" type="${InputType.NUMBER}" .value="${(_b = this.condition.sun) === null || _b === void 0 ? void 0 : _b.offsetMins}" label="${i18next.t('offsetInMinutes')}"
                                      @or-mwc-input-changed="${(ev) => { this.setOffset(ev.detail.value); }}">
                        </or-mwc-input>
                        <or-mwc-input type="${InputType.BUTTON}" class="min-width" @or-mwc-input-changed="${onMapModalOpen}" label="location"></or-mwc-input>
                        <or-mwc-dialog id="map-modal" heading="${i18next.t('pickLocation')}" .actions="${modalActions}"></or-mwc-dialog>
                    `}
                ` : undefined}
            </div>
        `;
    }
    /* ---------------------------------------------------- */
    // Getters/setters of the file
    setTrigger(trigger) {
        if (trigger) {
            if (trigger.key == TimeTriggerType.TIME_OF_DAY) {
                this.condition.sun = undefined;
            }
            else if (this.getSunPositions().includes(trigger.key)) {
                this.condition.cron = undefined;
                if (this.getSunPositions().includes(this.selectedTrigger.key)) {
                    this.condition.sun = { position: trigger.key, offsetMins: this.condition.sun.offsetMins, location: this.condition.sun.location };
                }
                else {
                    this.condition.sun = { position: trigger.key, offsetMins: 0 };
                }
            }
            this.selectedTrigger = trigger;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setTime(time) {
        if (time) {
            const splittedTime = time.split(':');
            const date = new Date();
            date.setHours(Number(splittedTime[0]));
            date.setMinutes(Number(splittedTime[1]));
            this.condition.cron = Util.formatCronString(undefined, undefined, undefined, date.getUTCHours().toString(), date.getUTCMinutes().toString());
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setOffset(offset) {
        var _a, _b;
        this.condition.sun = {
            position: (_a = this.condition.sun) === null || _a === void 0 ? void 0 : _a.position,
            location: (_b = this.condition.sun) === null || _b === void 0 ? void 0 : _b.location,
            offsetMins: offset
        };
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    setLocation(point) {
        var _a, _b;
        this.condition.sun = {
            position: (_a = this.condition.sun) === null || _a === void 0 ? void 0 : _a.position,
            location: point,
            offsetMins: (_b = this.condition.sun) === null || _b === void 0 ? void 0 : _b.offsetMins
        };
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    getSunPositions() {
        return [
            "TWILIGHT_MORNING_CIVIL" /* SunPositionTriggerPosition.TWILIGHT_MORNING_CIVIL */,
            "SUNRISE" /* SunPositionTriggerPosition.SUNRISE */, "SUNSET" /* SunPositionTriggerPosition.SUNSET */,
            "TWILIGHT_EVENING_CIVIL" /* SunPositionTriggerPosition.TWILIGHT_EVENING_CIVIL */
        ];
    }
    /* --------------------------------- */
    // Utility stuff
    triggerToString(position) {
        if (position == TimeTriggerType.TIME_OF_DAY) {
            return i18next.t("timeOfDay");
        }
        else {
            return i18next.t(position.toLowerCase());
        }
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleTriggerQuery.prototype, "condition", void 0);
__decorate([
    property()
], OrRuleTriggerQuery.prototype, "readonly", void 0);
__decorate([
    state()
], OrRuleTriggerQuery.prototype, "selectedTrigger", void 0);
__decorate([
    state()
], OrRuleTriggerQuery.prototype, "triggerOptions", void 0);
OrRuleTriggerQuery = __decorate([
    customElement("or-rule-trigger-query")
], OrRuleTriggerQuery);
export { OrRuleTriggerQuery };
//# sourceMappingURL=or-rule-trigger-query.js.map