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
import {customElement, property, query, state} from "lit/decorators.js";
import {CalendarEvent} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {translate,i18next} from "@openremote/or-translate";

import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {Frequency as FrequencyValue, RRule, Weekday, WeekdayStr} from 'rrule'
import moment from "moment";
import { Days } from "rrule/dist/esm/rrule";
import { BY_RRULE_PARTS, EventTypes, MONTHS, NOT_APPLICABLE_BY_RRULE_PARTS, recurrenceEnds } from "./data";
import type { RulePartKey, RuleParts, LabeledEventTypes, Frequency } from "./types";
import { when } from "lit/directives/when.js";
export { RuleParts, RulePartKey, Frequency, LabeledEventTypes };

export * from "./data";

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

export interface OrSchedulerChangedEventDetail {
    value?: CalendarEvent;
}

export class OrSchedulerChangedEvent extends CustomEvent<OrSchedulerChangedEventDetail> {

    public static readonly NAME = "or-scheduler-changed";

    constructor(value?: CalendarEvent) {
        super(OrSchedulerChangedEvent.NAME, {
            detail: { value },
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrSchedulerChangedEvent.NAME]: OrSchedulerChangedEvent;
    }
}

@customElement("or-scheduler")
export class OrScheduler extends translate(i18next)(LitElement) {

    @property()
    public calendarEvent?: CalendarEvent = this.default;

    @property()
    public default?: CalendarEvent;

    @property()
    public disabledFrequencies: Frequency[] = [];

    @property()
    public disabledRRuleParts: RulePartKey[] = [];

    @property()
    public eventType: EventTypes = EventTypes.default;

    @property()
    public eventTypes: LabeledEventTypes = EventTypes;

    @property()
    public header?: string;

    @property()
    public isAllDay = true;

    @state()
    protected _rrule?: RRule;

    @query("#radial-modal")
    protected dialog?: OrMwcDialog;

    protected _dialog?: OrMwcDialog;
    protected _byRRuleParts?: RulePartKey[];

    protected firstUpdated(_changedProps: PropertyValues) {
        if (this.calendarEvent?.start && this.calendarEvent?.end) {
            if (this.calendarEvent.recurrence) {
                this.eventType = EventTypes.recurrence;
            } else {
                this.eventType = EventTypes.period;
            }
            this.isAllDay = this.calendarEvent
                && moment(this.calendarEvent.start).isSame(moment(this.calendarEvent.start).clone().startOf("day"))
                && moment(this.calendarEvent.end).isSame(moment(this.calendarEvent.end).clone().endOf("day"));
        }
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (changedProps.has("_rrule") && this._rrule) {
            this._byRRuleParts = BY_RRULE_PARTS
                .filter(p => !NOT_APPLICABLE_BY_RRULE_PARTS[FrequencyValue[this._rrule!.options.freq] as Frequency]?.includes(p.toUpperCase()))
                .filter(p => !this.disabledRRuleParts.includes(p));
        }

        if (changedProps.has("calendarEvent")) {
            if (this.calendarEvent && this.calendarEvent.recurrence) {
                this._rrule = RRule.fromString(this.calendarEvent.recurrence);
            } else if (this.eventType === EventTypes.default && this.default?.recurrence) {
                this._rrule = RRule.fromString(this.default.recurrence);
            } else {
                this._rrule = undefined;
            }
        }
    }

    protected getRRule(): string | undefined {
        return this._rrule?.toString()?.split("RRULE:")?.[1];
    }

    protected setRRuleValue(value: any, key: keyof RuleParts | "all-day" | "start" | "end" | "recurrence-ends" | "dtstart-time" | "until-time") {
        let origOptions = this._rrule?.origOptions!;
        const calendarEvent = this.calendarEvent!;

        if (key === "interval" || key === "freq" || key.startsWith("by")) {
            if (key === "byweekday") {
                origOptions.byweekday = (value as WeekdayStr[]).map(d => RRule[d]);
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
            } else {
                origOptions[key as keyof RuleParts] = value;
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
            }
        } else {
          switch (key) {
            case "all-day":
                this.isAllDay = value;
                break;
            case "start":
                const newStartDate = moment(value);
                if (newStartDate.isValid()) {
                    calendarEvent.start = newStartDate.set({ hour: 0, minute: 0, second: 0, millisecond: 0 }).toDate().getTime();
                    if (this.eventType === EventTypes.recurrence) {
                        origOptions.dtstart = newStartDate.toDate();
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
                if (origOptions.until) delete origOptions.until;
                if (origOptions.count) delete origOptions.count;
                if (value === "until") {
                    origOptions.until = moment().add(1, 'year').toDate();
                } else if (value === "count") {
                    origOptions.count = 1;
                }
              if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
              break;
            case "until":
                if (origOptions.count) delete origOptions.count;
                const newDate = moment(value);
                origOptions.until = new Date(moment(origOptions.until).set({ year: newDate.year(), month: newDate.month(), date: newDate.date() }).format());
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "count":
                if (origOptions.until) delete origOptions.until;
                origOptions.count = value;
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "dtstart-time":
                const timeParts = value.split(':');
                if (origOptions) {
                    origOptions.dtstart = moment(origOptions.dtstart).set({ hour: timeParts[0], minute: timeParts[1], second: 0, millisecond: 0 }).toDate();
                } else {
                    origOptions = new RRule({
                        dtstart: moment(calendarEvent.start).set({ hour: timeParts[0], minute: timeParts[1], second: 0, millisecond: 0 }).toDate()
                    }).origOptions;
                }
                calendarEvent.start = moment(origOptions.dtstart).toDate().getTime();
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "until-time":
                const untilParts = value.split(':');
                if (this._rrule && this._rrule.options.until) {
                    if (origOptions) {
                        origOptions.until = moment(origOptions.until).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate();
                    } else {
                        origOptions = new RRule({
                            until: moment(calendarEvent.end).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate()
                        }).origOptions;
                    }
                }
                calendarEvent.end = moment(calendarEvent.end).set({ hour: untilParts[0], minute: untilParts[1], second: 0, millisecond: 0 }).toDate().getTime();
                if (this.eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
          }
        }

        this.calendarEvent = { ...calendarEvent };
        this._dialog!.requestUpdate();
        this.calendarEvent.recurrence = this.getRRule();
    }

    protected timeLabel(): string | undefined {
        if (this.eventType === EventTypes.default) {
            return this.eventTypes.default;
        } if (this.calendarEvent && this._rrule) {
            const calendarEvent = this.calendarEvent;
            const diff = moment(calendarEvent.end).diff(calendarEvent.start, "days");
            let diffString = "";
            if (this.isAllDay) {
                if(diff > 0) diffString = " "+i18next.t('forDays', {days: diff});
                return this._rrule.toText() + diffString;
            } else {
                if (diff > 0) diffString = i18next.t("fromToDays", { start: moment(calendarEvent.start).format("HH:mm"), end: moment(calendarEvent.end).format("HH:mm"), days: diff });
                if (diff === 0) diffString = i18next.t("fromTo", { start: moment(calendarEvent.start).format("HH:mm"), end: moment(calendarEvent.end).format("HH:mm") });
                return this._rrule.toText() + " " + diffString;
            }
        } else if (this.calendarEvent) {
            let format = "DD-MM-YYYY";
            if(!this.isAllDay) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", { start: moment(this.calendarEvent.start).format(format), end: moment(this.calendarEvent.end).format(format) });
        }
    }

    protected setCalendarEventType(value: any) {
        switch (value) {
            case EventTypes.default:
                this.calendarEvent = this.default;
                this._rrule = RRule.fromString(this.default?.recurrence ?? "") || undefined;
                break;
            case EventTypes.period:
                this.calendarEvent = {
                    start: this.calendarEvent?.start ?? moment().startOf("day").toDate().getTime(),
                    end: this.calendarEvent?.end ?? moment().startOf("day").endOf("day").toDate().getTime()
                };
                this._rrule = undefined;
                break;
            case EventTypes.recurrence:
                this.calendarEvent = {
                    start: this.calendarEvent?.start ?? moment().startOf("day").toDate().getTime(),
                    end: this.calendarEvent?.end ?? moment().startOf("day").endOf("day").toDate().getTime(),
                    recurrence: "FREQ=DAILY"
                };
                break;
        }
        this.eventType = value;
        this._dialog!.requestUpdate();
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
                        if (this.calendarEvent && this.isAllDay) {
                            this.calendarEvent.start = moment(this.calendarEvent.start).startOf("day").toDate().getTime();
                            this.calendarEvent.end = moment(this.calendarEvent.end).startOf("day").endOf("day").toDate().getTime();
                        }
                        if (this.eventType === EventTypes.default) {
                            delete this.calendarEvent;
                        } else if (this.eventType === EventTypes.recurrence) {
                            this.calendarEvent!.recurrence = this.getRRule();
                        }
                        this.dispatchEvent(new OrSchedulerChangedEvent(this.calendarEvent ?? this.default));
                        this._dialog = undefined;
                    }
                },
            ])
            .setContent(() => this.getDialogContent())
            .setDismissAction(null));
    }

    protected getDialogContent(): TemplateResult {
        const calendar = this.calendarEvent;

        return html`
            <div style="min-width: 635px; display:grid; flex-direction: row;">
                <div id="event-type" class="section">
                    <label class="title"><or-translate value="typeOfSchedule"></or-translate></label>
                    <div class="layout horizontal">
                        <or-mwc-input style="min-width: 280px;"
                                    .value="${this.eventType}"
                                    .type="${InputType.SELECT}"
                                    .options="${Object.entries(this.eventTypes)}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setCalendarEventType(e.detail.value)}"></or-mwc-input>
                    </div>
                </div>
                ${this.eventType === EventTypes.recurrence ? this.getRecurrenceTemplate() : ``}
                ${calendar && (this.eventType === EventTypes.period || this.eventType === EventTypes.recurrence) ? this.getPeriodTemplate(calendar) : ``}
                ${this.eventType === EventTypes.recurrence ? this.getRecurrenceEndsTemplate() : ``}
            </div>`;
    }

    /**
     * Displays the interval, frequency and BY_XXX RRule part fields
     * 
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
     * @returns Inputs for the applicable rule parts
     */
    protected getRecurrenceTemplate(): TemplateResult {
        const interval = (this._rrule && this._rrule.options && this._rrule.options.interval) ?? 1;
        const frequency = (this._rrule && this._rrule.options && this._rrule.options.freq) ?? FrequencyValue.DAILY;

        return html`
            <div id="recurrence" class="section">
                <label class="title"><or-translate value="repeatEvery"></or-translate></label>
                <div class="layout horizontal" style="display: flex; gap: 8px;">
                    ${when(!this.disabledRRuleParts?.includes("interval"), () => html`
                        <or-mwc-input style="width: 60px;"
                                    min="1"
                                    max="9"
                                    .value="${interval}"
                                    .type="${InputType.NUMBER}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "interval")}"></or-mwc-input>
                    `)}
                    <or-mwc-input style="flex: 1;"
                                .value="${frequency.toString()}"
                                .type="${InputType.SELECT}"
                                .options="${Object.entries(FrequencyValue).filter(([_, v]) => typeof v !== "number" && !this.disabledFrequencies.includes(v as Frequency))}"
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

    /**
     * Displays the BY_XXX RRule part fields if allowed by the frequency
     * 
     * @returns The specified BY_XXX RRule part field or undefined if not applicable
     */
    protected getByRulePart<T>(part: RulePartKey, type: InputType, options: T[]): TemplateResult | undefined {
        if (!this._byRRuleParts?.includes(part)) {
            return undefined
        }

        const value = part === "byweekday"
            ? (this._rrule?.origOptions?.byweekday as Weekday[])?.map(String)
            : this._rrule?.origOptions[part];

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
     * Displays the fields that define when and how long the particular event is
     * 
     * @param calendar The specified calendar event
     * @returns The periods allDay and from/to date and time fields
     */
    protected getPeriodTemplate(calendar: CalendarEvent): TemplateResult {
        return html`
            <div id="period" class="section">
                <label class="title"><or-translate value="period"></or-translate></label>
                <div style="display: flex; gap: 8px;" class="layout horizontal">
                    <div>
                        <or-mwc-input value="${moment(calendar.start).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "start")}" .label="${i18next.t("from")}"></or-mwc-input>
                        <or-mwc-input .disabled=${this.isAllDay} .value="${moment(calendar.start).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "dtstart-time")}" .label="${i18next.t("from")}"></or-mwc-input>
                    </div>
                    <div>
                        <or-mwc-input .value="${moment(calendar.end).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "end")}" .label="${i18next.t("to")}"></or-mwc-input>
                        <or-mwc-input .disabled=${this.isAllDay} .value="${moment(calendar.end).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until-time")}" .label="${i18next.t("to")}"></or-mwc-input>
                    </div>
                </div>

                <div class="layout horizontal">
                    <or-mwc-input .value=${this.isAllDay} @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "all-day")}"  .type="${InputType.CHECKBOX}" .label="${i18next.t("allDay")}"></or-mwc-input>
                </div>
            </div>`;
    }

    /**
     * Displays the fields that define how a recurring event should end
     * 
     * Applicable rule parts
     * - `until`
     * - `count`
     * @returns The recurrence ends fields
     */
    protected getRecurrenceEndsTemplate(): TemplateResult {
        const when = (this._rrule?.options?.until && "until") || (this._rrule?.options?.count && "count") || "never";
        return html`
            <div id="recurrence-ends" class="section">
                <label class="title"><or-translate value="recurrenceEnds"></or-translate></label>
                <div style="display: flex; gap: 8px;" class="layout horizontal">
                    <or-mwc-input style="flex: 1"
                                  .label="${i18next.t(when)}"
                                  .value="${this._rrule?.options?.until ?? this._rrule?.options?.count ?? "never"}"
                                  .type="${InputType.SELECT}"
                                  .options="${Object.entries(recurrenceEnds).filter(([k]) => !this.disabledRRuleParts?.includes(k as RulePartKey))}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "recurrence-ends")}">
                    </or-mwc-input>
                    ${this._rrule?.options.until ? html`<or-mwc-input style="min-width: 50%" .value="${moment(this._rrule.options.until).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}"></or-mwc-input>`: undefined}
                    ${this._rrule?.options.count ? html`<or-mwc-input style="min-width: 50%" .value="${this._rrule.options.count}" min="1" .type="${InputType.NUMBER}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "count")}"></or-mwc-input>`: undefined}
                </div>
            </div>`;
    }
}
