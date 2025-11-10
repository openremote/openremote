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
import {Frequency, RRule, Weekday, WeekdayStr} from 'rrule'
import moment from "moment";
import { Days } from "rrule/dist/esm/rrule";
import { BY_RULE_PARTS, EventTypes, MONTHS, NOT_APPLICABLE_BY_RULE_PARTS, recurrenceEnds } from "./data";
import type { RulePartKey, RuleParts, LabeledEventTypes, Frequencies } from "./types";
export { RuleParts, RulePartKey, Frequencies, LabeledEventTypes };

function range(start: number, end: number): number[] {
    return Array.from({ length: end - start + 1 }, (_, i) => start + i )
}

// TODO: use es2023
function toSpliced<T>(arr: T[], index: number, count: number): T[] {
    arr.splice(index, count);
    return arr;
}

const byWeekNoOptions = toSpliced(range(-53, 53), 53, 1);
const byYearDayOptions = toSpliced(range(-366, 366), 366, 1); // TODO: optimize option rendering
const byMonthDayOptions = toSpliced(range(-31, 31), 31, 1);
const byHourOptions = range(1, 24);
const byMinuteOrSecondsOptions = range(1, 60);

@customElement("or-calendar-event")
export class OrCalendarEvent extends translate(i18next)(LitElement) {

    @query("#radial-modal")
    protected dialog?: OrMwcDialog;

    @property()
    protected header?: string;

    @property()
    protected excludeFrequencies: Frequencies[] = [];

    @property()
    protected excludeRuleParts: RulePartKey[] = [];

    @property()
    protected calendarEvent?: CalendarEvent;

    @property()
    protected eventTypes: LabeledEventTypes = EventTypes; // TODO: what about translations?? -> reactive?

    @property()
    protected _rrule?: RRule;

    protected _parts?: RulePartKey[];

    protected _dialog?: OrMwcDialog;

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (changedProps.has("_rrule")) {
          this._parts = BY_RULE_PARTS
              .filter(p => !NOT_APPLICABLE_BY_RULE_PARTS[Frequency[this._rrule!.options.freq!] as keyof typeof Frequency]?.includes(p.toUpperCase()))
              .filter(p => !this.excludeRuleParts.includes(p))
        }

        // if (changedProps.has("ruleset") && this.ruleset) {
        //     this._validity = this.ruleset.meta ? this.ruleset.meta["validity"] as CalendarEvent : undefined;

        //     if (this._validity && this._validity.recurrence) {
        //         this._rrule = RRule.fromString(this._validity.recurrence);
        //     } else {
        //         this._rrule = undefined;
        //     }
        // }
    }

    isAllDay() {
        return this.calendarEvent && moment(this.calendarEvent.start).hours() === 0 && moment(this.calendarEvent.start).minutes() === 0
            && moment(this.calendarEvent.end).hours() === 23 && moment(this.calendarEvent.end).minutes() === 59;
    }

    protected setRRuleValue(value: any, key: keyof RuleParts | "all-day" | "start" | "end" | "recurrence-ends" | "dtstart-time" | "until-time") {
        let origOptions = this._rrule ? this._rrule.origOptions : undefined;
        const calendarEvent = this.calendarEvent!;

        if (key === "interval" || key === "freq" || key.startsWith("by")) {
          if (key === "byweekday") {
            origOptions!.byweekday = (value as WeekdayStr[]).map(d => RRule[d]);
            if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
          } else {
            origOptions![key as keyof RuleParts] = value;
            if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
          }
        } else {
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
              if (newStartDate.isValid()) {
                calendarEvent.start = newStartDate.set({ hour: 0, minute: 0, second: 0, millisecond: 0 }).toDate().getTime();
                if (this.getEventType() === EventTypes.recurrence) {
                  origOptions!.dtstart = newStartDate.toDate();
                  this._rrule = new RRule(origOptions);
                }
              }
              break;
            case "end":
              const newEndDate = moment(value);
              if (newEndDate.isValid()) {
                calendarEvent.end = newEndDate.set({ hour: 23, minute: 59, second: 0, millisecond: 0 }).toDate().getTime();
              }
              break;
            case "recurrence-ends":
              if (origOptions!.until) delete origOptions!.until
              if (origOptions!.count) delete origOptions!.count
              if (value === "until") {
                origOptions!.until = moment().add(1, 'year').toDate();
              } else if (value === "count") {
                origOptions!.count = 1;
              }
              if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
            case "until":
              if (origOptions!.count) delete origOptions!.count
              const newDate = moment(value);
              origOptions!.until = new Date(moment(origOptions!.until).set({ year: newDate.year(), month: newDate.month(), date: newDate.date() }).format());
              if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
            case "count":
              if (origOptions!.until) delete origOptions!.until
              origOptions!.count = value;
              if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
            case "dtstart-time":
              const timeParts = value.split(':');
              if (origOptions) {
                origOptions!.dtstart = moment(origOptions.dtstart).set({ hour: timeParts[0], minute: timeParts[1], second: 0, millisecond: 0 }).toDate();
              } else {
                origOptions = new RRule({
                  dtstart: moment(this.calendarEvent!.start).set({ hour: timeParts[0], minute: timeParts[1], second: 0, millisecond: 0 }).toDate()
                }).origOptions;
              }
              calendarEvent.start = moment(origOptions!.dtstart).toDate().getTime();
              if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
            case "until-time":
              const untilParts = value.split(':');
              if (this._rrule && this._rrule.options.until) {
                if (origOptions) {
                  origOptions!.until = moment(origOptions.until).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate();
                } else {
                  origOptions = new RRule({
                    until: moment(this.calendarEvent!.end).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate()
                  }).origOptions;
                }
              }
              calendarEvent.end = moment(calendarEvent.end).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate().getTime();
              if (this.getEventType() === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
          }
        }
        this.calendarEvent = {...calendarEvent};
        this._dialog!.requestUpdate();
        console.log("setRRuleValue", this._rrule?.toString())
    }

    timeLabel(): string | undefined {
        if (this.getEventType() === EventTypes.default) {
            return this.eventTypes.default;
        } else if (this.calendarEvent && this._rrule) {
            const calendarEvent = this.calendarEvent;
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
        } else if (this.calendarEvent) {
            let format = "DD-MM-YYYY";
            if(!this.isAllDay()) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", {start: moment(this.calendarEvent.start).format(format), end: moment(this.calendarEvent.end).format(format) })
        }
    }

    setCalendarEventType(value: any) {
        switch (value) {
            case EventTypes.default:
                // delete this.ruleset.meta["calendarEvent"];
                this.calendarEvent = undefined;
                this._rrule = undefined;
                break;
            case EventTypes.period:
                this.calendarEvent = {
                    start: moment().startOf("day").toDate().getTime(),
                    end: moment().endOf("day").toDate().getTime()
                };
                this._rrule = undefined;
                break;
            case EventTypes.recurrence:
                if (!this.calendarEvent) {
                    this.calendarEvent = {
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
        if (this.calendarEvent) {
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
            .setHeading(this.header)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }

                    #dialog-content {
                        overflow: visible;
                        background-color: #f5f5f5;
                    }

                    .mdc-dialog .mdc-dialog__content {
                        padding: 8px 16px !important;
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

                    .section {
                        background-color: white;
                        margin-top: 10px;
                        padding: 8px 16px;
                        border-radius: 4px;
                    }
                    .section > * {
                        margin: 8px 0;
                    }
                    .section:first-child {
                        margin-top: 0;
                    }
                    .section:last-child {
                        margin-bottom: 0;
                    }

                    .title {
                        display: block;
                        font-weight: bold;
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
        const calendar = this.calendarEvent;

        return html`
            <div style="min-width: 635px; display:grid; flex-direction: row;">
                <div class="section">
                    <label class="title"><or-translate value="typeOfSchedule"></or-translate></label>
                    <div class="layout horizontal">
                        <or-mwc-input style="min-width: 280px;"
                                    .value="${eventType}"
                                    .type="${InputType.SELECT}"
                                    .options="${Object.entries(this.eventTypes)}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setCalendarEventType(e.detail.value)}"></or-mwc-input>
                    </div>
                </div>
                ${eventType === EventTypes.recurrence ? this.getRepeat() : ``}
                ${calendar && (eventType === EventTypes.period || eventType === EventTypes.recurrence) ? this.getPeriod(calendar) : ``}
                ${eventType === EventTypes.recurrence ? this.getRecurrenceEnds() : ``}
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
            <div class="section">
                <label class="title"><or-translate value="repeatEvery"></or-translate></label>
                <div class="layout horizontal" style="display: flex; gap: 8px;">
                        <or-mwc-input style="width: 60px;"
                                    min="1"
                                    max="9"
                                    .value="${interval}"
                                    .type="${InputType.NUMBER}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "interval")}"></or-mwc-input>
                        <or-mwc-input style="flex: 1;"
                                    .value="${frequency.toString()}"
                                    .type="${InputType.SELECT}"
                                    .options="${Object.entries(Frequency).filter(([_, v]) => typeof v !== "number" && !this.excludeFrequencies.includes(v as Frequencies))}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "freq")}"></or-mwc-input>
                </div>
                <div>${this.getByRulePart("bymonth", InputType.CHECKBOX_LIST, Object.entries(MONTHS))}</div>
                <div>
                    ${this.getByRulePart("byweekno", InputType.SELECT, byWeekNoOptions)}
                    ${this.getByRulePart("byyearday", InputType.SELECT, byYearDayOptions)}
                    ${this.getByRulePart("bymonthday", InputType.SELECT, byMonthDayOptions)}
                </div>
                <div>${this.getByRulePart("byweekday", InputType.CHECKBOX_LIST, Object.keys(Days))}</div>
                <div>
                    ${this.getByRulePart("byhour", InputType.SELECT, byHourOptions)}
                    ${this.getByRulePart("byminute", InputType.SELECT, byMinuteOrSecondsOptions)}
                    ${this.getByRulePart("bysecond", InputType.SELECT, byMinuteOrSecondsOptions)}
                </div>
            </div>`;
    }

    protected getByRulePart<T>(part: RulePartKey, type: InputType, options: T[]): TemplateResult | undefined {
        if (!this._parts?.includes(part)) {
          return undefined
        }

        const value = part === "byweekday"
            ? this._rrule?.options?.byweekday?.map(d => new Weekday(d).toString()) 
            : this._rrule?.options[part];

        return html`<or-mwc-input style="min-width: 30%"
                                  .value="${value ?? []}"
                                  .type="${type}"
                                  .options="${options}"
                                  .label="${part}"
                                  multiple
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                this.setRRuleValue(e.detail.value, part);
            }}"></or-mwc-input>`;
    }

    /**
     * @todo limit by freq length
     * @param calendar 
     * @returns 
     */
    protected getPeriod(calendar: CalendarEvent): TemplateResult {
        return html`
            <div class="section">
                <label class="title"><or-translate value="period"></or-translate></label>
                <div style="display: flex; justify-content: space-between; gap: 8px;" class="layout horizontal">
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
            </div>`;
    }

    /**
     * Applicable rule parts
     * - `until`
     * - `count`
     * @returns 
     */
    protected getRecurrenceEnds(): TemplateResult {
        const when = (this._rrule?.options?.until && "until") || (this._rrule?.options?.count && "count") || "never";
        return html`
            <div class="section">
                <label class="title"><or-translate value="recurrenceEnds"></or-translate></label>
                <div style="display: flex; justify-content: space-between; gap: 8px;" class="layout horizontal">
                    <or-mwc-input style="flex: 1" 
                                  .label="${i18next.t(when)}"
                                  .value="${this._rrule?.options?.until ?? this._rrule?.options?.count ?? "never"}"
                                  .type="${InputType.SELECT}"
                                  .options="${Object.entries(recurrenceEnds)}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "recurrence-ends")}">
                    </or-mwc-input>
                    ${this._rrule?.options.until ? html`<or-mwc-input style="min-width: 50%" .value="${this._rrule!.options.until ? moment(this._rrule!.options.until).format("YYYY-MM-DD") : moment().add(1, 'year').format('YYYY-MM-DD')}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}"></or-mwc-input>`: undefined}
                    ${this._rrule?.options.count ? html`<or-mwc-input style="min-width: 50%" .value="${this._rrule.options.count}" min="1" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "count")}"></or-mwc-input>`: undefined}
                </div>
            </div>`;
    }
}
