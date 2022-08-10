import {RuleCondition, SunPositionTriggerPosition} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {css, html, LitElement, PropertyValues} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import moment from "moment";
import {buttonStyle} from "../style";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {TimeTriggerType} from "../index";
import {Util} from "@openremote/core";

// language=CSS
const style = css`
    
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
    protected selectedTrigger?: TimeTriggerType;

    protected triggerOptions: TimeTriggerType[];

    constructor() {
        super();
        this.triggerOptions = [TimeTriggerType.TIME_OF_DAY, TimeTriggerType.SUNRISE, TimeTriggerType.SUNSET];
        this.selectedTrigger = this.triggerOptions[0];
    }

    protected get query() {
        return this.condition.assets!;
    }

    render() {
        const isoString = Util.cronStringToISOString(this.condition.cron!);
        const formattedTime = moment(isoString).format('HH:mm');
        return html`
            <div class="trigger-group">
                <or-mwc-input class="min-width" type="${InputType.SELECT}" .options="${this.triggerOptions}" .value="${this.selectedTrigger}" label="Trigger type"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => { this.setTrigger(ev.detail.value); }}">
                </or-mwc-input>
                ${this.selectedTrigger ? html`
                    ${this.selectedTrigger == this.triggerOptions[0] ? html`
                        <or-mwc-input class="min-width" type="${InputType.TIME}" .value="${(this.condition.cron ? formattedTime : undefined)}" label="Time of day"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => { this.setTime(ev.detail.value) }}">
                        </or-mwc-input>
                    ` : html`
                        <or-mwc-input class="min-width width" type="${InputType.NUMBER}" .value="${this.condition.sun?.offsetMins}" label="Offset in minutes"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => { this.setOffset(ev.detail.value) }}">
                        </or-mwc-input>
                    `}
                ` : undefined}
            </div>
        `
    }

    setTrigger(trigger: TimeTriggerType) {
        if(trigger && this.triggerOptions.includes(trigger)) {
            switch (trigger) {
                case TimeTriggerType.TIME_OF_DAY:
                    this.condition.sun = undefined; break;
                case TimeTriggerType.SUNRISE:
                    this.condition.cron = undefined;
                    this.condition.sun = { position: this.getPosition(trigger) };
                    break;
                    // this.condition.duration = undefined; break;
                case TimeTriggerType.SUNSET:
                    this.condition.cron = undefined;
                    this.condition.sun = { position: this.getPosition(trigger) };
                    break;
                    // this.condition.duration = undefined; break;
            }
            this.selectedTrigger = trigger;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setTime(time: string) {
        if(time) {
            const splittedTime = time.split(':');
            this.condition.cron = Util.formatCronString(undefined, undefined, undefined, splittedTime[0], splittedTime[1]);
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
    setOffset(offset: number) {
        this.condition.sun = {
            position: this.getPosition(this.selectedTrigger!),
            offsetMins: offset
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    getPosition(trigger: TimeTriggerType): SunPositionTriggerPosition | undefined {
        switch (trigger) {
            case TimeTriggerType.SUNRISE: return SunPositionTriggerPosition.SUNRISE;
            case TimeTriggerType.SUNSET: return SunPositionTriggerPosition.SUNSET;
        }
    }
}
