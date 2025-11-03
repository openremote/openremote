/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {CalendarEvent} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {translate,i18next} from "@openremote/or-translate";

import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {ByWeekday, RRule, Weekday} from 'rrule'
import moment from "moment";

@customElement("or-schedular")
export class OrSchedular extends translate(i18next)(LitElement) {

    @query("#radial-modal")
    protected dialog?: OrMwcDialog;

    @property()
    protected _calendar?: CalendarEvent;

    @property()
    protected _rrule?: RRule;

    protected _dialog?: OrMwcDialog;

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        // if (changedProps.has("ruleset") && this.ruleset) {
        //     this._validity = this.ruleset.meta ? this.ruleset.meta["validity"] as CalendarEvent : undefined;

        //     if (this._validity && this._validity.recurrence) {
        //         this._rrule = RRule.fromString(this._validity.recurrence);
        //     } else {
        //         this._rrule = undefined;
        //     }
        // }
    }

    getWeekDay(weekday: string): ByWeekday | undefined {
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
        return this._calendar && moment(this._calendar.start).hours() === 0 && moment(this._calendar.start).minutes() === 0
            && moment(this._calendar.end).hours() === 23 && moment(this._calendar.end).minutes() === 59;
    }

    protected setRRuleValue(value: any, key: string) {
        let origOptions = this._rrule ? this._rrule.origOptions : undefined;
        const validity = this._calendar!;

        switch (key) {
            case "all-day":
                if (value) {
                    validity.start = moment(validity.start).startOf("day").toDate().getTime();
                    validity.end = moment(validity.end).endOf("day").toDate().getTime();
                } else {
                    validity.start = moment().toDate().getTime();
                    validity.end = moment().add(1, 'hour').toDate().getTime();
                }
                break;
            case "start":
                const newStartDate = moment(value);
                if(newStartDate.isValid()) {
                    validity.start = newStartDate.set({hour:0,minute:0,second:0,millisecond:0}).toDate().getTime();
                    if (this.getEventTypes() === "recurrence") {
                        origOptions!.dtstart = newStartDate.toDate();
                        this._rrule = new RRule(origOptions);
                    }
                }
                break;
            case "end":
                const newEndDate = moment(value);
                if(newEndDate.isValid()) {
                    validity.end = newEndDate.set({hour:23,minute:59,second:0,millisecond:0}).toDate().getTime();
                }
                break;
            case "never-ends":
                if (value) {
                    delete origOptions!.until
                } else {
                    origOptions!.until = moment().add(1, 'year').toDate();
                }
                if (this.getEventTypes() === "recurrence") this._rrule = new RRule(origOptions);
                break;
            case "byweekday":
                if (!origOptions!.byweekday) origOptions!.byweekday = [];
                if (!Array.isArray(origOptions!.byweekday)) origOptions!.byweekday = [origOptions!.byweekday as ByWeekday];
                const newDays: string[] = value;
                origOptions!.byweekday = [];
                newDays.forEach((d: any) => {
                    const weekDay = this.getWeekDay(d);
                    if (weekDay) {
                        (origOptions!.byweekday! as ByWeekday[]).push(weekDay);
                    }
                });
                if (this.getEventTypes() === "recurrence") this._rrule = new RRule(origOptions);
                break;
            case "until":
                if (this._rrule!.options.until) {
                    const newDate = moment(value)
                    origOptions!.until = new Date(moment(origOptions!.until).set({year: newDate.year(), month: newDate.month(), date: newDate.date()}).format())
                }
                if (this.getEventTypes() === "recurrence") this._rrule = new RRule(origOptions);
                break;
            case "dtstart-time":
                const timeParts = value.split(':');
                if (origOptions) {
                    origOptions!.dtstart = moment(origOptions.dtstart).set({hour:timeParts[0],minute:timeParts[1],second:0,millisecond:0}).toDate();
                } else {
                    origOptions = new RRule({
                        dtstart: moment(this._calendar!.start).set({hour:timeParts[0],minute:timeParts[1],second:0,millisecond:0}).toDate()
                    }).origOptions;
                }
                validity.start = moment(origOptions!.dtstart).toDate().getTime();
                if (this.getEventTypes() === "recurrence") this._rrule = new RRule(origOptions);
                break;
            case "until-time":
                const untilParts = value.split(':');
                if (this._rrule && this._rrule.options.until) {
                    if (origOptions) {
                        origOptions!.until = moment(origOptions.until).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).toDate();
                    } else {
                        origOptions = new RRule({
                            until: moment(this._calendar!.end).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).toDate()
                        }).origOptions;
                    }
                }
                validity.end = moment(validity.end).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).toDate().getTime();
                if(this.getEventTypes() === "recurrence") this._rrule = new RRule(origOptions);
                break;
        }
        this._calendar = {...validity};
        this._dialog!.requestUpdate();
    }

    timeLabel() {
        if (this.getEventTypes() === "always") {
            return i18next.t("always");
        } else if (this._calendar && this._rrule) {
            const validity = this._calendar;
            const diff = moment(validity.end).diff(validity.start, "days");
            let diffString = "";
            if (this.isAllDay()) {
                if(diff > 0) diffString = " "+i18next.t('forDays', {days: diff});
                return this._rrule.toText() + diffString;
            } else {
                if(diff > 0) diffString = i18next.t("fromToDays", {start: moment(validity.start).format("HH:mm"), end: moment(validity.end).format("HH:mm"), days: diff })
                if(diff === 0) diffString = i18next.t("fromTo", {start: moment(validity.start).format("HH:mm"), end: moment(validity.end).format("HH:mm") })
                return this._rrule.toText() + " " + diffString;
            } 
        } else if (this._calendar) {
            let format = "DD-MM-YYYY";
            if(!this.isAllDay()) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", {start: moment(this._calendar.start).format(format), end: moment(this._calendar.end).format(format) })
        }
    }

    setValidityType(value: any) {
        console.log(this._calendar, this.getEventTypes())

        switch (value) {
            case "always":
                // delete this.ruleset.meta["validity"];
                this._calendar = undefined;
                this._rrule = undefined;
                break;
            case "period":
                this._calendar = {
                    start: moment().startOf("day").toDate().getTime(),
                    end: moment().endOf("day").toDate().getTime()
                };
                this._rrule = undefined;
                break;
            case "recurrence":
                if (!this._calendar) {
                    this._calendar = {
                        start: moment().startOf("day").toDate().getTime(),
                        end: moment().endOf("day").toDate().getTime()
                    };
                }
                this._rrule = new RRule({
                    freq: RRule.DAILY,
                    dtstart: new Date()
                });
                break;
        }
        this._dialog!.requestUpdate();
    }

    getEventTypes () {
        if(this._calendar) {
            if (this._rrule) {
                return "recurrence";
            } else {
                return "period";
            }
        }
        return "always";
    }

    protected render() {
        // if(!this.ruleset) return html``;

        return html`
            <or-mwc-input outlined .type="${InputType.BUTTON}" label="${this.timeLabel()}" @or-mwc-input-changed="${() => this.showDialog()}"></or-mwc-input>
        `;
    }

    protected showDialog() {
        this._dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("scheduleRuleActivity"))
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }

                    #dialog-content {
                        overflow: visible;
                    }

                    @media only screen and (max-width: 1279px) {
                        .mdc-dialog__surface {
                            overflow-x: auto !important;
                            overflow-y: auto !important;
                        }

                        #dialog-content {
                            min-height: 230px;
                            overflow: auto;
                        }
                    }
                </style>`)
            .setActions([
                {
                    actionName: "cancel",
                    content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                    action: () => {
                        this._dialog = undefined;
                    }
                },
                {
                    actionName: "ok",
                    default: true,
                    content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="apply"></or-mwc-input>`,
                    action: () => {
                        // if (this.ruleset && this.ruleset.meta) {
                        //     if (this.getValidityType() === "always") {
                        //         delete this.ruleset.meta["validity"];
                        //     } else {
                        //         if (this.getValidityType() === "recurrence") {
                        //             this._validity!.recurrence = this._rrule!.toString().split("RRULE:")[1];
                        //         }
                        //         this.ruleset.meta["validity"] = this._validity;
                        //     }
                        //     this.dispatchEvent(new OrRulesRuleChangedEvent(true));
                        //     this._dialog = undefined;
                        // }
                    }
                },
            ])
            .setContent(() => this.getDialogContent())
            .setDismissAction(null));
    }

    protected getDialogContent(): TemplateResult {
        const options = [RRule.MO.toString(), RRule.TU.toString(), RRule.WE.toString(), RRule.TH.toString(), RRule.FR.toString(), RRule.SA.toString(), RRule.SU.toString()];
        const eventTypes = ["always", "period", "recurrence"];
        const eventType = this.getEventTypes();
        const selectedOptions = this._rrule && this._rrule.options && this._rrule.options.byweekday ? this._rrule.options.byweekday.map(day => new Weekday(day).toString()) : [];
        const calendar = this._calendar;

        return html`
            <div style="min-width: 635px; display:grid; flex-direction: row;">
                <div class="layout horizontal">
                    <or-mwc-input style="min-width: 280px;" .value="${eventType}" .type="${InputType.SELECT}" .options="${eventTypes}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setValidityType(e.detail.value)}"></or-mwc-input>
                </div>

                ${calendar && (eventType  === "period" || eventType  === "recurrence") ? html`
                    <label style="display:block; margin-top: 20px;"><or-translate value="period"></or-translate></label>
                    <div style="display: flex; justify-content: space-between;" class="layout horizontal">
                        <div> 
                            <or-mwc-input value="${moment(calendar.start).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "start")}" .label="${i18next.t("from")}"></or-mwc-input>
                            <or-mwc-input .disabled=${this.isAllDay()} .value="${moment(calendar.start).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "dtstart-time")}" .label="${i18next.t("from")}"></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .value="${moment(calendar.end).format("YYYY-MM-DD")}"  .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "end")}" .label="${i18next.t("to")}"></or-mwc-input>
                            <or-mwc-input .disabled=${this.isAllDay()} .value="${moment(calendar.end).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until-time")}" .label="${i18next.t("to")}"></or-mwc-input>
                        </div>
                    </div>  
                    
                    <div class="layout horizontal">
                        <or-mwc-input .value=${this.isAllDay()} @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "all-day")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("allDay")}"></or-mwc-input>
                    </div>
                ` : ``}
             
                ${eventType  === "recurrence" ? html`
                    <label style="display: block; margin-top: 20px;"><or-translate value="repeatOccurrenceEvery"></or-translate></label>
                    <div class="layout horizontal">
                        <or-mwc-input .value="${selectedOptions}" 
                                      .type="${InputType.CHECKBOX_LIST}" 
                                      .options="${options}" 
                                      .label="${i18next.t("daysOfTheWeek")}" 
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => { 
                                          this.setRRuleValue(e.detail.value, "byweekday"); 
                                        }}" ></or-mwc-input>
                    </div>

                    <label style="display:block; margin-top: 20px;"><or-translate value="repetitionEnds"></or-translate></label>
                    <div class="layout horizontal">
                        <or-mwc-input .value="${!this._rrule!.options.until}"  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "never-ends")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("never")}"></or-mwc-input>
                    </div>
                    <div class="layout horizontal">
                        <or-mwc-input ?disabled="${!this._rrule!.options.until}" .value="${this._rrule!.options.until ? moment(this._rrule!.options.until).format("YYYY-MM-DD") : moment().add(1, 'year').format('YYYY-MM-DD')}"  .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}" .label="${i18next.t("to")}"></or-mwc-input>
                    </div>
                ` : ``}
            </div>`;
    }
}
