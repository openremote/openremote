import {GeoJSONPoint, RuleCondition, SunPositionTriggerPosition} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {css, html, LitElement, PropertyValues} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import moment from "moment";
import {buttonStyle} from "../style";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {TimeTriggerType} from "../index";
import {Util} from "@openremote/core";
import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrMap, OrMapClickedEvent} from "@openremote/or-map";
import {i18next} from "@openremote/or-translate";
import {OrVaadinSelect} from "@openremote/or-vaadin-components/or-vaadin-select";
import { when } from "lit/directives/when.js";
import { ifDefined } from "lit/directives/if-defined.js";
import {OrVaadinTimePicker} from "@openremote/or-vaadin-components/or-vaadin-time-picker";

// language=CSS
const style = css`
    
    ${buttonStyle}
    
    :host {
        display: block;
    }
    
    .trigger-group {
        flex-grow: 1;
        display: flex;
        align-items: baseline;
        flex-direction: row;
        flex-wrap: wrap;
    }
    .min-width {
        flex: 0 0 200px;
    }
    .width {
        width: 200px;
    }
    
    .trigger-group > * {
        margin: 10px 3px 6px 3px;
    }
`;

interface TimeTrigger {
    key: TimeTriggerType | SunPositionTriggerPosition,
    value: string
}

@customElement("or-rule-trigger-query")
export class OrRuleTriggerQuery extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public condition!: RuleCondition;

    @property()
    public readonly: boolean = false;

    /* ---------- */

    @state()
    protected selectedTrigger: TimeTrigger;

    @state()
    protected triggerOptions: TimeTrigger[];

    constructor() {
        super();
        this.triggerOptions = [];
        Object.values(TimeTriggerType).forEach((type) => { this.triggerOptions.push({ key: type, value: this.triggerToString(type)}); });
        this.getSunPositions().forEach((opt) => { this.triggerOptions.push({ key: opt, value: this.triggerToString(opt)}); });
        this.selectedTrigger = this.triggerOptions[0];

        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initMap);
    }

    updated(changedProperties: PropertyValues) {
        if(changedProperties.has('condition')) {
            if(this.condition.cron) { this.selectedTrigger = { key: TimeTriggerType.TIME_OF_DAY, value: this.triggerToString(TimeTriggerType.TIME_OF_DAY) }}
            else if(this.condition.sun) { this.selectedTrigger = { key: this.condition.sun.position!, value: this.triggerToString(this.condition.sun.position!) }}
        }
    }

    /* ---------------------- */

    initMap() {
        const modal = this.shadowRoot!.getElementById('map-modal');
        if (!modal) return;

        const map = modal.shadowRoot!.querySelector('.or-map') as OrMap;
        if (map) {
            map.addEventListener(OrMapClickedEvent.NAME, (evt: CustomEvent) => {
                const lngLat: any = evt.detail.lngLat;
                this.setLocation({ type: 'Point', coordinates: [lngLat.lat, lngLat.lng] });
                const latElement = modal.shadowRoot!.querySelector('.location-lat') as HTMLInputElement;
                const lngElement = modal.shadowRoot!.querySelector('.location-lng') as HTMLInputElement;
                latElement.value = lngLat.lat;
                lngElement.value = lngLat.lng;
            });
        }
    }

    renderDialogHTML(point: GeoJSONPoint | undefined) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("map-modal") as OrMwcDialog;
        if (dialog) {
            dialog.content = html`
                <div style="display:grid">
                    <or-map class="or-map" type="VECTOR" style="border: 1px solid #d5d5d5; height: 400px; min-width: 300px; margin-bottom: 20px;">
                        ${(point && point.coordinates) ? html`
                            <or-map-marker class="or-map-marker" active color="#FF0000" icon="white-balance-sunny" lat="${point.coordinates[0]}" lng="${point.coordinates[1]}"></or-map-marker>
                        `: undefined}
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
        const modalActions: DialogAction[] = [
            { actionName: "close", default: true, content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="close"></or-mwc-input>`, action: () => {} }
        ];
        const onMapModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("map-modal") as OrMwcDialog;
            if(dialog) {
                dialog.dismissAction = null;
                dialog.open();
                this.renderDialogHTML(this.condition.sun?.location)
            }
        }
        // Render dialog on every update (for example when changing location in the modal itself)
        this.renderDialogHTML(this.condition.sun?.location);

        return html`
            <div class="trigger-group">
                <or-vaadin-select class="min-width" value=${this.selectedTrigger.key}
                                  .items=${this.triggerOptions.map(t => ({value: t.key, label: t.value}))}
                                  @change=${(ev: Event) => this.setTrigger(this.triggerOptions.find(o => o.key === ((ev.currentTarget as OrVaadinSelect).value))!)}>
                    <or-translate slot="label" value="triggerType"></or-translate>
                </or-vaadin-select>
                ${when(this.selectedTrigger, () => html`
                    ${this.selectedTrigger.key == this.triggerOptions[0].key ? html`
                        <!-- Time picker -->
                        <or-vaadin-time-picker class="min-width" step="900" auto-open-disabled
                                               value=${ifDefined(this.condition.cron ? moment(Util.cronStringToISOString(this.condition.cron, true)).format("HH:mm:ss") : undefined)}
                                               @change=${(ev: Event) => this.setTime((ev.currentTarget as OrVaadinTimePicker).value)}>
                            <or-translate slot="label" value="timeOfDay"></or-translate>
                        </or-vaadin-time-picker>
                    ` : html`
                        <!-- Sun position selection -->
                        <or-vaadin-number-field class="min-width width" value=${this.condition.sun?.offsetMins} min="0"
                                                @change=${(ev: Event) => {
                                                    const elem = ev.currentTarget as HTMLInputElement;
                                                    if(elem.checkValidity()) this.setOffset(Number(elem.value))
                                                }}>
                            <or-translate slot="label" value="offsetInMinutes"></or-translate>
                        </or-vaadin-number-field>
                        <or-vaadin-button class="min-width" @click=${() => onMapModalOpen()}>
                            <or-translate value="location"></or-translate>
                        </or-vaadin-button>
                        <or-mwc-dialog id="map-modal" heading="${i18next.t('pickLocation')}" .actions="${modalActions}"></or-mwc-dialog>
                    `}
                `)}
            </div>
        `
    }






    /* ---------------------------------------------------- */


    // Getters/setters of the file

    setTrigger(trigger: TimeTrigger) {
        if(trigger) {
            if(trigger.key == TimeTriggerType.TIME_OF_DAY) {
                this.condition.sun = undefined;
            } else if(this.getSunPositions().includes(trigger.key)) {
                this.condition.cron = undefined;
                if(this.getSunPositions().includes(this.selectedTrigger.key as SunPositionTriggerPosition)) {
                    this.condition.sun = { position: trigger.key, offsetMins: this.condition.sun!.offsetMins, location: this.condition.sun!.location };
                } else {
                    this.condition.sun = { position: trigger.key, offsetMins: 0 };
                }
            }
            this.selectedTrigger = trigger;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setTime(time: string) {
        if(time) {
            const splittedTime = time.split(':');
            const date = new Date();
            date.setHours(Number(splittedTime[0]));
            date.setMinutes(Number(splittedTime[1]));
            console.debug(date.toString());
            this.condition.cron = Util.formatCronString(undefined, undefined, undefined, date.getUTCHours().toString(), date.getUTCMinutes().toString());
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setOffset(offset: number) {
        this.condition.sun = {
            position: this.condition.sun?.position,
            location: this.condition.sun?.location,
            offsetMins: offset
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    setLocation(point: GeoJSONPoint) {
        this.condition.sun = {
            position: this.condition.sun?.position,
            location: point,
            offsetMins: this.condition.sun?.offsetMins
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    getSunPositions(): SunPositionTriggerPosition[] {
        return [
            SunPositionTriggerPosition.TWILIGHT_MORNING_CIVIL,
            SunPositionTriggerPosition.SUNRISE, SunPositionTriggerPosition.SUNSET,
            SunPositionTriggerPosition.TWILIGHT_EVENING_CIVIL
        ];
    }




    /* --------------------------------- */

    // Utility stuff

    triggerToString(position: TimeTriggerType | SunPositionTriggerPosition): string {
        if(position == TimeTriggerType.TIME_OF_DAY) { return i18next.t("timeOfDay"); }
        else {
            return i18next.t(position.toLowerCase());
        }
    }
}
