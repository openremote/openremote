import { css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import { RulesetUnion } from "@openremote/model";
import {
    OrRulesRuleChangedEvent
} from "./index";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog } from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import { RRule, Weekday } from 'rrule'
import moment from "moment";

@customElement("or-rule-validity")
export class OrRuleValidity extends translate(i18next)(LitElement) {


    @property({type: Object})
    public ruleset?: RulesetUnion;

    @property({type: Object})
    public rrule: RRule = new RRule({
        freq: RRule.DAILY,
        dtstart: new Date()
      });

    constructor() {
        super();
    }
    public static styles = css`
        :host {
            margin-left: 20px;
        }
    `;

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);
        if(changedProps.has("ruleset") && this.ruleset) {
            if(!this.ruleset.meta) this.ruleset.meta = {};
            if(!this.ruleset.meta["urn:openremote:rule:meta:validity"]) return;

            if(this.ruleset.meta["urn:openremote:rule:meta:validity"].recurrence) {
                this.rrule = RRule.fromString(this.ruleset.meta["urn:openremote:rule:meta:validity"].recurrence)
            }
        }
    }

    getWeekDay(weekday: string) {
        switch (weekday) {
            case "MO":
                return RRule.MO
            case "TU":
                return RRule.TU
            case "WE":
                return RRule.WE
            case "TH":
                return RRule.TH
            case "FR":
                return RRule.FR
            case "SA":
                return RRule.SA
            case "SU":
                return RRule.SU
        }
    }

    isAllDay() {
        if(this.ruleset && this.ruleset.meta && this.ruleset.meta["urn:openremote:rule:meta:validity"]) {
            if(moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).hours() === 0 && moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).minutes() === 0 
            && moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).hours() === 23 && moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).minutes() === 59) {
                return true
            }
        }
    }

    protected setRRuleValue(value: any, key?: string) {
        if(key && this.rrule && this.ruleset){
            const origOptions:any = this.rrule.origOptions
            switch (key) {
                case "all-day":
                    if(value) {
                        if(this.ruleset.meta) {
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].start = moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).startOf('day');
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].end = moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).endOf('day');
                        }
                    } else {
                        if(this.ruleset.meta) {
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].start = moment.utc();
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].end = moment.utc().add(1, 'hour');
                        }
                        
                    }
                    break;
                case "start":
                    if(this.ruleset.meta)
                        this.ruleset.meta["urn:openremote:rule:meta:validity"]['start'] = moment.utc(value).set({hour:0,minute:0,second:0,millisecond:0}).format();

                    if(this.getValidityType() === "validityRecurrence") 
                        origOptions[key] = moment.utc(value).format();
                        this.rrule = new RRule(origOptions);
                    break;
                case "end":
                    if(this.ruleset.meta)
                        this.ruleset.meta["urn:openremote:rule:meta:validity"]['end'] = moment.utc(value).set({hour:23,minute:59,second:0,millisecond:0}).format();
                    break;
                case "never-ends":
                    if(value) {
                        delete origOptions.until
                    } else {
                        origOptions.until = new Date(moment.utc().add(1, 'year').format());
                    }
                    if(this.getValidityType() === "validityRecurrence") this.rrule = new RRule(origOptions);
                    break;
                case "byweekday":
                    if(!origOptions[key]) origOptions[key] = [];
                    if(value.checked) {
                        origOptions[key].push(this.getWeekDay(value.name))
                    } else {
                        origOptions[key] = origOptions[key].filter((day:Weekday) => day !== this.getWeekDay(value.name));
                    }
                    if(this.getValidityType() === "validityRecurrence") this.rrule = new RRule(origOptions);
                    break;
                case "dtstart":
                case "until":
                    break;
                case "dtstart-time":
                    const timeParts = value.split(':');
                    origOptions["dtstart"] = moment.utc(origOptions["dtstart"]).set({hour:timeParts[0],minute:timeParts[1],second:0,millisecond:0}).format()
                    if(this.ruleset.meta)
                        this.ruleset.meta["urn:openremote:rule:meta:validity"].start = moment.utc(origOptions["dtstart"]).format();
                    if(this.getValidityType() === "validityRecurrence") this.rrule = new RRule(origOptions);
                    break;
                case "until-time":
                    const untilParts = value.split(':');
                    if(this.rrule.options.until) {
                        origOptions["until"] = moment.utc(origOptions["until"]).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).format()
                    }
                    if(this.ruleset.meta)
                        this.ruleset.meta["urn:openremote:rule:meta:validity"].end = moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).format();
                    if(this.getValidityType() === "validityRecurrence") this.rrule = new RRule(origOptions);
                    break;
            }
        }
        this.requestUpdate();
    }

    timeLabel() {
        if(this.getValidityType() === "validityAlways") {
            return i18next.t("validityAlways")
        } else if(this.ruleset && this.ruleset.meta && this.ruleset.meta["urn:openremote:rule:meta:validity"] && this.ruleset.meta["urn:openremote:rule:meta:validity"].recurrence){
            const diff = moment(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).diff(this.ruleset.meta["urn:openremote:rule:meta:validity"].start, 'days');
            let diffString = "";
            if(this.isAllDay()) {
                if(diff > 0) diffString = " "+diff+ " " + i18next.t("days") + " " + i18next.t("later")+ " ";
                return this.rrule.toText()+diffString;
            } else {
                if(diff > 0) diffString = i18next.t("fromToDays", {start: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).format("HH:mm"), end: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).format("HH:mm"), days: diff })
                if(diff === 0) diffString = i18next.t("fromTo", {start: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).format("HH:mm"), end: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).format("HH:mm") })
                return this.rrule.toText()+" "+diffString;
            } 
        } else if(this.ruleset && this.ruleset.meta && this.ruleset.meta["urn:openremote:rule:meta:validity"]){
            let format = "DD-MM-YYYY";
            if(!this.isAllDay()) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", {start: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).format(format), end: moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).format(format) })
        }
    }

    setValidityType(value: any) {
        if(!this.ruleset || !this.ruleset.meta) return
        switch (value) {
            case "validityAlways":
                this.ruleset.meta["urn:openremote:rule:meta:validity"] = null;
                break;
            case "validityPeriod":
                this.ruleset.meta["urn:openremote:rule:meta:validity"] = {
                    start: moment.utc().startOf('day'),
                    end:moment.utc().endOf('day')
                };
                break;
            case "validityRecurrence":
                this.ruleset.meta["urn:openremote:rule:meta:validity"] = {
                    start: moment.utc().startOf('day'),
                    end:moment.utc().endOf('day'),
                    recurrence: {}
                };
                break;
        }
        this.requestUpdate('ruleset')
    }

    getValidityType () {
        if(this.ruleset && this.ruleset.meta && this.ruleset.meta["urn:openremote:rule:meta:validity"]) {
            if(this.ruleset.meta["urn:openremote:rule:meta:validity"].recurrence) {
                return "validityRecurrence"
            } else {
                return "validityPeriod"

            }
        }
        return "validityAlways";
    }

    renderDialogHTML() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("radial-modal") as OrMwcDialog;
        const options = [RRule.MO.toString(), RRule.TU.toString(), RRule.WE.toString(), RRule.TH.toString(), RRule.FR.toString(), RRule.SA.toString(), RRule.SU.toString()];
        const validityTypes = ["validityAlways", "validityPeriod", "validityRecurrence"];
        const validityType = this.getValidityType()
        const selectedOptions = this.rrule.options && this.rrule.options.byweekday ? this.rrule.options.byweekday.map(day => new Weekday(day).toString()) : [];
       
        if (dialog && this.ruleset && this.ruleset.meta) {
            dialog.dialogContent = html`
                <div style="min-height: 200px; min-width: 635px; display:grid; flex-direction: row;">
                    <div class="layout horizontal">
                        <or-input .value="${validityType}" .type="${InputType.SELECT}" .options="${validityTypes}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValidityType(e.detail.value)}" ></or-input>
                    </div>

                    ${(validityType  === "validityPeriod" || validityType  === "validityRecurrence") ? html`
                        <label style="display:block; margin-top: 10px;">Period</label>
                        <div class="layout horizontal">
                            <or-input value="${moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "start")}" .label="${i18next.t("from")}"></or-input>
                            <or-input .disabled=${this.isAllDay()} .value="${moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).format("HH:mm")}" .type="${InputType.TIME}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "dtstart-time")}" .label="${i18next.t("from")}"></or-input>

                            <or-input .value="${moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).format("YYYY-MM-DD")}"  .type="${InputType.DATE}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "end")}" .label="${i18next.t("to")}"></or-input>
                            
                            <or-input .disabled=${this.isAllDay()} .value="${moment.utc(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).format("HH:mm")}" .type="${InputType.TIME}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until-time")}" .label="${i18next.t("to")}"></or-input>
                        </div>  
                        
                        <div class="layout horizontal">
                            <or-input .value=${this.isAllDay()} @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "all-day")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("all day")}"></or-input>
                        </div>
                    ` : ``}
                 
                    ${validityType  === "validityRecurrence" ? html`
                        <label style="display:block; margin-top: 10px;">Repeat occurence every</label>
                        <div class="layout horizontal">
                            <or-input .value="${selectedOptions}" .type="${InputType.CHECKBOX_LIST}" .options="${options}" .label="${i18next.t("days of the week")}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "byweekday")}" ></or-input>
                        </div>
                        
                        <label style="display:block; margin-top: 10px;">Repetition ends</label>
                        <div class="layout horizontal">                        
                            <or-input .value="${!this.rrule.options.until}"  @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "never-ends")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("never")}"></or-input>
                        </div>
                        <div class="layout horizontal">
                            <or-input ?disabled="${!this.rrule.options.until}" .value="${this.rrule.options.until ? moment.utc(this.rrule.options.until).format("YYYY-MM-DD") : moment.utc().add(1, 'year').format('YYYY-MM-DD')}"  .type="${InputType.DATE}" @or-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}" .label="${i18next.t("to")}"></or-input>
                        </div>
                    ` : ``}
                  
                    
                    
                </div>`;
        }
    }

    protected render() {
        if(!this.ruleset) return html``;

      
        // @ts-ignore

        const validityModalActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
            {
                actionName: "ok",
                default: true,
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("apply")}"></or-input>`,
                action: () => {
                    if(this.ruleset && this.ruleset.meta) {
                        if(this.getValidityType() === "validityRecurrence") {
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].recurrence = this.rrule.toString().split("RRULE:")[1]
                        }
                        if(this.getValidityType() === "validityPeriod" || this.getValidityType() === "validityRecurrence") {
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].start = moment(this.ruleset.meta["urn:openremote:rule:meta:validity"].start).valueOf()
                            this.ruleset.meta["urn:openremote:rule:meta:validity"].end = moment(this.ruleset.meta["urn:openremote:rule:meta:validity"].end).valueOf()
                        }
                        this.dispatchEvent(new OrRulesRuleChangedEvent(true));
                    }
                }
            },
        ];
       
      
        const validityModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("radial-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
                this.renderDialogHTML();

            }
        };

        this.renderDialogHTML();
        
        return html`
            <or-input .type="${InputType.BUTTON}" .label="${this.timeLabel()}" @click="${validityModalOpen}"></or-input>
            <or-mwc-dialog id="radial-modal" dialogTitle="Schedule rule activity" .dialogActions="${validityModalActions}"></or-mwc-dialog>
        `
    }
}
