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
import {ByWeekday, Frequency, Options, RRule, Weekday, WeekdayStr} from 'rrule'
import moment from "moment";
import { Days } from "rrule/dist/esm/rrule";
import { getMonthDays, isLeapYear, MONTH_DAYS } from "rrule/dist/esm/dateutil";

/**
 * Supported recurrence rule parts in evaluation order:
 * - `interval`
 * - `freq`
 * - `bymonth`
 * - `byweekno`
 * - `byyearday`
 * - `bymonthday`
 * - `byweekday` = `byday` in {@link https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10|rfc5545#section-3.3.10}.
 * - `byhour`
 * - `byminute`
 * - `bysecond`
 * - `count`
 * - `until`
 * @todo wkst not implemented
 *
 * Ignored
 * - | 'byeaster'// Not applicable, introduced by {@link https://labix.org/python-dateutil/#head-a65103993a21b717f6702063f3717e6e75b4ba66|python-dateutil}.
 * - | 'bynmonthday' // Not specified
 * - | 'bynweekday' // Not specified
 * - | 'bysetpos' // Too complex for the time being
 * - | 'dtstart' // Not applicable CalendarEvent already specifies start
 * - | 'tzid' // Not part of the recurrence rule parts
 *
 * @see {@link RRule} and {@link https://labix.org/python-dateutil/#head-a65103993a21b717f6702063f3717e6e75b4ba66|python-dateutil}.
 */
export type RuleParts = Pick<Options,
    | 'interval'
    | 'freq' // Must exist (should default to DAILY?)
    | 'bymonth'
    | 'byweekno'
    | 'byyearday'
    | 'bymonthday'
    | 'byweekday'
    | 'byhour'
    | 'byminute'
    | 'bysecond'
    | 'count'
    | 'until'
>;
enum EventTypes { default = 'default', period = 'period', recurrence = 'recurrence' }
export type LabeledEventTypes = Record<EventTypes, string>
export type RulePartKey = keyof RuleParts

// Evaluation order: BYMONTH, BYWEEKNO, BYYEARDAY, BYMONTHDAY, BYDAY, BYHOUR, BYMINUTE and BYSECOND.
// As per https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10 page 44
const BY_RULE_PARTS = [
    "bymonth",
    "byweekno",
    "byyearday",
    "bymonthday",
    "byweekday",
    "byhour",
    "byminute",
    "bysecond",
] as const

// As per https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10 page 44
const NOT_APPLICABLE_BY_RULE_PARTS = {
    "SECONDLY":	["BYWEEKNO"],
    "MINUTELY":	["BYWEEKNO"],
    "HOURLY":	["BYWEEKNO"],
    "DAILY":	["BYWEEKNO", "BYYEARDAY"],
    "WEEKLY":	["BYWEEKNO", "BYYEARDAY", "BYMONTHDAY"],
    "MONTHLY":	["BYWEEKNO", "BYYEARDAY"]
} as Record<keyof typeof Frequency, string[]>

// January (31 days)
// February (28 days in a common year and 29 days in leap years)
// March (31 days)
// April (30 days)
// May (31 days)
// June (30 days)
// July (31 days)
// August (31 days)
// September (30 days)
// October (31 days)
// November (30 days)
// December (31 days)
const MONTHS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
]

@customElement("or-calendar-event")
export class OrCalendarEvent extends translate(i18next)(LitElement) {

    @query("#radial-modal")
    protected dialog?: OrMwcDialog;

    @property()
    protected _calendar?: CalendarEvent;

    @property()
    protected _rrule?: RRule;

    @property()
    protected _excludeRuleParts?: RulePartKey[];// observedAttributes -> make reactive

    @property()
    protected _eventTypes: LabeledEventTypes = EventTypes; // TODO: what about translations?? -> reactive?

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

    getWeekDay(weekday: WeekdayStr): ByWeekday | undefined {
        return RRule[weekday]
    }

    isAllDay() {
        return this._calendar && moment(this._calendar.start).hours() === 0 && moment(this._calendar.start).minutes() === 0
            && moment(this._calendar.end).hours() === 23 && moment(this._calendar.end).minutes() === 59;
    }

    protected setRRuleValue(value: any, key: keyof RuleParts | "all-day" | "start" | "end" | "never-ends" | "dtstart-time" | "until-time") {
        let origOptions = this._rrule ? this._rrule.origOptions : undefined;
        const calendarEvent = this._calendar!;

        switch (key) {
            case "all-day":
                if (value) {
                    calendarEvent.start = moment(calendarEvent.start).startOf("day").toDate().getTime();
                    calendarEvent.end = moment(calendarEvent.end).endOf("day").toDate().getTime();
                } else {
                    calendarEvent.start = moment().toDate().getTime();
                    calendarEvent.end = moment().add(1, 'hour').toDate().getTime();
                }
                break;
            case "start":
                const newStartDate = moment(value);
                if(newStartDate.isValid()) {
                    calendarEvent.start = newStartDate.set({hour:0,minute:0,second:0,millisecond:0}).toDate().getTime();
                    if (this.getEventType() === EventTypes.recurrence) {
                        origOptions!.dtstart = newStartDate.toDate();
                        this._rrule = new RRule(origOptions);
                    }
                }
                break;
            case "end":
                const newEndDate = moment(value);
                if(newEndDate.isValid()) {
                    calendarEvent.end = newEndDate.set({hour:23,minute:59,second:0,millisecond:0}).toDate().getTime();
                }
                break;
            case "never-ends":
                if (value) {
                    delete origOptions!.until
                } else {
                    origOptions!.until = moment().add(1, 'year').toDate();
                }
                if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "byweekday":
                if (!origOptions!.byweekday) origOptions!.byweekday = [];
                if (!Array.isArray(origOptions!.byweekday)) origOptions!.byweekday = [origOptions!.byweekday as ByWeekday];
                const newDays: WeekdayStr[] = value;
                origOptions!.byweekday = [];
                newDays.forEach((d) => {
                    const weekDay = this.getWeekDay(d);
                    if (weekDay) {
                        (origOptions!.byweekday! as ByWeekday[]).push(weekDay);
                    }
                });
                if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "until":
                if (this._rrule!.options.until) {
                    const newDate = moment(value)
                    origOptions!.until = new Date(moment(origOptions!.until).set({year: newDate.year(), month: newDate.month(), date: newDate.date()}).format())
                }
                if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
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
                calendarEvent.start = moment(origOptions!.dtstart).toDate().getTime();
                if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
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
                calendarEvent.end = moment(calendarEvent.end).set({hour:untilParts[0],minute:untilParts[1],second:0,millisecond:0}).toDate().getTime();
                if(this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
        }
        this._calendar = {...calendarEvent};
        this._dialog!.requestUpdate();
    }

    timeLabel() {
        if (this.getEventType() === EventTypes.default) {
            return i18next.t(EventTypes.default);
        } else if (this._calendar && this._rrule) {
            const calendarEvent = this._calendar;
            const diff = moment(calendarEvent.end).diff(calendarEvent.start, "days");
            let diffString = "";
            if (this.isAllDay()) {
                if(diff > 0) diffString = " "+i18next.t('forDays', {days: diff});
                return this._rrule.toText() + diffString;
            } else {
                if(diff > 0) diffString = i18next.t("fromToDays", {start: moment(calendarEvent.start).format("HH:mm"), end: moment(calendarEvent.end).format("HH:mm"), days: diff })
                if(diff === 0) diffString = i18next.t("fromTo", {start: moment(calendarEvent.start).format("HH:mm"), end: moment(calendarEvent.end).format("HH:mm") })
                return this._rrule.toText() + " " + diffString;
            } 
        } else if (this._calendar) {
            let format = "DD-MM-YYYY";
            if(!this.isAllDay()) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", {start: moment(this._calendar.start).format(format), end: moment(this._calendar.end).format(format) })
        }
    }

    setCalendarEventType(value: any) {
        console.log(this._calendar, this.getEventType())

        switch (value) {
            case EventTypes.default:
                // delete this.ruleset.meta["calendarEvent"];
                this._calendar = undefined;
                this._rrule = undefined;
                break;
            case EventTypes.period:
                this._calendar = {
                    start: moment().startOf("day").toDate().getTime(),
                    end: moment().endOf("day").toDate().getTime()
                };
                this._rrule = undefined;
                break;
            case EventTypes.recurrence:
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

    getEventType() {
        if(this._calendar) {
            if (this._rrule) {
                return EventTypes.recurrence;
            } else {
                return EventTypes.period;
            }
        }
        return EventTypes.default;
    }

    protected render() {
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
                        //     if (this.getcalendarEventType() === EventTypes.default) {
                        //         delete this.ruleset.meta["calendarEvent"];
                        //     } else {
                        //         if (this.getcalendarEventType() === EventTypes.recurrence) {
                        //             this._calendarEvent!.recurrence = this._rrule!.toString().split("RRULE:")[1];
                        //         }
                        //         this.ruleset.meta["calendarEvent"] = this._calendarEvent;
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
        const eventType = this.getEventType();
        const calendar = this._calendar;

        return html`
            <div style="min-width: 635px; display:grid; flex-direction: row;">
                <div class="layout horizontal">
                    <or-mwc-input style="min-width: 280px;" 
                                  .value="${eventType}" 
                                  .type="${InputType.SELECT}" 
                                  .options="${Object.entries(this._eventTypes)}" 
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setCalendarEventType(e.detail.value)}"></or-mwc-input>
                </div>

                ${eventType === EventTypes.recurrence ? this.getRepeat() : ``}
                ${calendar && (eventType === EventTypes.period || eventType === EventTypes.recurrence) ? this.getPeriod(calendar) : ``}
                ${eventType === EventTypes.recurrence ? this.getRepetitionEnds() : ``}
            </div>`;
    }

    /**
     * Applicable rule parts
     * - `interval`
     * - `freq`
     * - `bysecond`
     * - `byminute`
     * - `byhour`
     * - `byweekday`
     * - `bymonthday`
     * - `byyearday`
     * - `byweekno`
     * - `bymonth`
     * 
     * Evaluation order: BYMONTH, BYWEEKNO, BYYEARDAY, BYMONTHDAY, BYDAY, BYHOUR, BYMINUTE and BYSECOND.
     * 
     * @returns 
     */
    protected getRepeat(): TemplateResult {
      const interval = (this._rrule && this._rrule.options && this._rrule.options.interval) ?? 1;
      const frequency = (this._rrule && this._rrule.options && this._rrule.options.freq) ?? Frequency.DAILY;

      return html`
          <label style="display: block; margin-top: 20px;"><or-translate value="repeatEvery"></or-translate></label>
          <div class="layout horizontal">
              <or-mwc-input style="width: 10%" 
                            .value="${interval}" 
                            .type="${InputType.NUMBER}"
                            min="1"
                            max="9"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "interval")}"></or-mwc-input>
              <or-mwc-input style="width: 70%"
                            .value="${frequency}"
                            .type="${InputType.SELECT}"
                            .options="${Object.keys(Frequency).filter(f => isNaN(Number(f)))}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "freq")}"></or-mwc-input>
          </div>

          <div class="layout horizontal">
              ${BY_RULE_PARTS
                  .filter(p => !NOT_APPLICABLE_BY_RULE_PARTS[frequency.toString() as keyof typeof Frequency]?.includes(p))
                  .filter(p => !this._excludeRuleParts?.includes(p))
                  .map(part => {
                      switch (part) {
                          case "bymonth":   return html`${this.getByRulePart(part, InputType.CHECKBOX_LIST, MONTHS)}`  
                          case "byweekno":  return html`${this.getByRulePart(part, InputType.SELECT, [])}`  
                          case "byyearday": return html`${this.getByRulePart(part, InputType.SELECT, Array.from(Array(isLeapYear(new Date(this._calendar!.start!).getFullYear()) ? 366 : 365).keys()))}`  
                          case "bymonthday":return html`${this.getByRulePart(part, InputType.SELECT, [])}`  
                          case "byweekday": return html`${this.getByRulePart(part, InputType.CHECKBOX_LIST, Object.entries(Days))}`  
                          case "byhour":    return html`<br>${this.getByRulePart(part, InputType.SELECT, Array.from(Array(24).keys()))}`  
                          case "byminute":  return html`${this.getByRulePart(part, InputType.SELECT, Array.from(Array(60).keys()))}`  
                          case "bysecond":  return html`${this.getByRulePart(part, InputType.SELECT, Array.from(Array(60).keys()))}`  
                      }

                      return html`<br>${part}`
                  })
              }
          </div>`
    }

    protected getByRulePart<T>(part: RulePartKey, type: InputType, options: T[]): TemplateResult {
        return html`<br><or-mwc-input style="width: 100%;"
                                      .value="${this._rrule?.options[part]}"
                                      .type="${type}"
                                      .options="${options}"
                                      .label="${i18next.t(part)}"
                                      .multiple="${true}";
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          this.setRRuleValue(e.detail.value, part);
                                      }}"></or-mwc-input>`
    }

    /**
     * @todo limit by freq length
     * @param calendar 
     * @returns 
     */
    protected getPeriod(calendar: CalendarEvent): TemplateResult {
        return html`
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
            </div>`
    }

    /**
     * Applicable rule parts
     * - `until`
     * - `count`
     * @returns 
     */
    protected getRepetitionEnds(): TemplateResult {
        return html`
            <label style="display:block; margin-top: 20px;"><or-translate value="repetitionEnds"></or-translate></label>
            <div class="layout horizontal">
                <or-mwc-input .value="${!this._rrule!.options.until}"  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "never-ends")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("never")}"></or-mwc-input>
            </div>
            <div class="layout horizontal">
                <or-mwc-input ?disabled="${!this._rrule!.options.until}" .value="${this._rrule!.options.until ? moment(this._rrule!.options.until).format("YYYY-MM-DD") : moment().add(1, 'year').format('YYYY-MM-DD')}"  .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}" .label="${i18next.t("to")}"></or-mwc-input>
            </div>`
    }
}
