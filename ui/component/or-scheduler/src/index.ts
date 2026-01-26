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
import { html, LitElement, PropertyValues, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { CalendarEvent } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { translate, i18next } from "@openremote/or-translate";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { Frequency as FrequencyValue, RRule, Weekday, WeekdayStr } from "rrule";
import moment from "moment";
import { Days } from "rrule/dist/esm/rrule";
import { BY_RRULE_PARTS, EventTypes, FREQUENCIES, MONTHS, NOT_APPLICABLE_BY_RRULE_PARTS, rruleEnds } from "./util";
import type { RulePartKey, RuleParts, LabeledEventTypes, Frequency } from "./types";
import { when } from "lit/directives/when.js";
export { RuleParts, RulePartKey, Frequency, LabeledEventTypes };

export * from "./util";

function range(start: number, end: number): number[] {
    return Array.from({ length: end - start + 1 }, (_, i) => start + i )
}

// TODO: use es2023
function toSpliced<T>(arr: T[], index: number, count: number): T[] {
    const shallowCopy = [...arr];
    shallowCopy.splice(index, count);
    return shallowCopy;
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

    @property({ type: Object })
    public defaultSchedule?: CalendarEvent;

    @property({ type: String })
    public defaultEventTypeLabel = "default";

    @property({ type: Array })
    public disabledFrequencies: Frequency[] = [];

    @property({ type: Array })
    public disabledRRuleParts: RulePartKey[] = [];

    @property({ type: String })
    public header = "scheduleActivity";

    @property({ type: Boolean })
    public isAllDay = true;

    @property({ type: Object })
    public schedule?: CalendarEvent = this.defaultSchedule;

    @property()
    public timezoneOffset = 0;

    @state()
    protected _ends: keyof typeof rruleEnds = "never";

    @state()
    protected _normalizedSchedule = this.applyTimezoneOffset(this.schedule);

    @state()
    protected _rrule?: RRule;

    protected _byRRuleParts?: RulePartKey[];
    protected _count = 1;
    protected _dialog?: OrMwcDialog;
    protected _eventType: EventTypes = EventTypes.default;
    protected _eventTypes: LabeledEventTypes = EventTypes;
    protected _until = moment().toDate();

    protected firstUpdated(_changedProps: PropertyValues) {
        this._eventTypes = {
            default: i18next.t(this.defaultEventTypeLabel),
            period: i18next.t("planPeriod"),
            recurrence: i18next.t("planRecurrence")
        };
        if (this._normalizedSchedule?.start && this._normalizedSchedule?.end) {
            if (this._normalizedSchedule.recurrence) {
                this._eventType = EventTypes.recurrence;
            } else {
                this._eventType = EventTypes.period;
            }
            this.isAllDay = this._normalizedSchedule
                && moment(this._normalizedSchedule.start).isSame(moment(this._normalizedSchedule.start).clone().startOf("day"))
                && moment(this._normalizedSchedule.end).isSame(moment(this._normalizedSchedule.end).clone().endOf("day"));
        }
        if (this._normalizedSchedule?.recurrence) {
            const origOptions = RRule.fromString(this._normalizedSchedule.recurrence).origOptions;
            if (origOptions.until) {
                this._until = moment(origOptions.until).toDate();
            } else if (origOptions.count) {
                this._count = origOptions.count;
            }
        }
    }

    protected willUpdate(changedProps: PropertyValues) {
        if (changedProps.has("schedule")) {
            this._normalizedSchedule = this.applyTimezoneOffset(this.schedule);
        }
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (changedProps.has("_rrule") && this._rrule) {
            this._byRRuleParts = BY_RRULE_PARTS
                .filter(p => !NOT_APPLICABLE_BY_RRULE_PARTS[FrequencyValue[this._rrule!.options.freq] as Frequency]?.includes(p.toUpperCase()))
                .filter(p => !this.disabledRRuleParts.includes(p));
            this._ends = (this._rrule?.origOptions?.until && "until") || (this._rrule?.origOptions?.count && "count") || "never";
        }

        if (changedProps.has("_normalizedSchedule")) {
            if (this._normalizedSchedule?.recurrence) {
                this._rrule = RRule.fromString(this._normalizedSchedule.recurrence);
            } else if (this._eventType === EventTypes.default && this.defaultSchedule?.recurrence) {
                this._rrule = RRule.fromString(this.defaultSchedule.recurrence);
            } else {
                this._rrule = undefined;
            }
        }
    }

    /**
     * Converts the recurrence rule to string and normalizes it.
     * 
     * The UTC timezone offset 'Z' for the UNTIL rule part is removed,
     * because the backend uses a `LocalDateTime` object to compare.
     * @param rrule The recurrence rule to normalize.
     * @returns String representation of the defined Recurrence Rule
     */
    protected getRRule(rrule = this._rrule): string | undefined {
        return rrule?.toString()?.split("RRULE:")?.[1]?.replace(/(UNTIL=\d+T\d+)Z/, "$1");
    }

    protected setRRuleValue(value: any, key: keyof RuleParts | "start" | "end" | "start-time" | "end-time" | "all-day" | "recurrence-ends") {
        let origOptions = this._rrule?.origOptions;
        const calendarEvent = this._normalizedSchedule!;

        if (key === "interval" || key === "freq" || key.startsWith("by")) {
            if (key === "byweekday") {
                origOptions!.byweekday = (value as WeekdayStr[]).map(d => RRule[d]);
            } else {
                origOptions![key as keyof RuleParts] = value;
            }
            if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
        } else if (key === "start" && origOptions) {
            const [year, month, date] = value.split("-").map(Number);
            origOptions.dtstart = moment(calendarEvent.start).set({ year, month: month - 1, date }).toDate();
            calendarEvent.start = origOptions.dtstart.getTime();
            if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
        } else if (key === "end") {
            const [year, month, date] = value.split("-").map(Number);
            calendarEvent.end = moment(calendarEvent.end).set({ year, month: month - 1, date }).toDate().getTime();
        } else if (key === "start-time") {
            const [hour, minute] = value.split(':');
            if (origOptions) {
                origOptions.dtstart = moment(calendarEvent.start).set({ hour, minute, second: 0, millisecond: 0 }).toDate();
            } else {
                origOptions = new RRule({
                    dtstart: moment(calendarEvent.start).set({ hour, minute, second: 0, millisecond: 0 }).toDate()
                }).origOptions;
            }
            calendarEvent.start = moment(origOptions.dtstart).toDate().getTime();
            if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
        } else if (key === "end-time") {
            const [hour, minute] = value.split(':');
            calendarEvent.end = moment(calendarEvent.end).set({ hour, minute, second: 0, millisecond: 0 }).toDate().getTime();
        } else {
          switch (key) {
            case "all-day":
                this.isAllDay = value;
                break;
            case "recurrence-ends":
                if (value === "until") {
                    origOptions!.until = this._until;
                    delete origOptions!.count;
                } else if (value === "count") {
                    origOptions!.count = this._count;
                    delete origOptions!.until;
                } else {
                    delete origOptions!.count;
                    delete origOptions!.until;
                }
                this._ends = value;
                if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "until":
                this._until = new Date(value);
                origOptions!.until = this._until;
                if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
            case "count":
                this._count = value;
                origOptions!.count = this._count;
                if (this._eventType === EventTypes.recurrence) this._rrule = new RRule(origOptions);
                break;
          }
        }

        this._normalizedSchedule = { ...calendarEvent };
        this._dialog!.requestUpdate();
        this._normalizedSchedule.recurrence = this.getRRule();
    }

    protected timeLabel(): string | undefined {
        if (this._eventType === EventTypes.default) {
            return i18next.t(this.defaultEventTypeLabel);
        } if (this._normalizedSchedule && this._rrule) {
            const calendarEvent = this._normalizedSchedule;
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
        } else if (this._normalizedSchedule) {
            let format = "DD-MM-YYYY";
            if(!this.isAllDay) format = "DD-MM-YYYY HH:mm";
            return i18next.t("activeFromTo", { start: moment(this._normalizedSchedule.start).format(format), end: moment(this._normalizedSchedule.end).format(format) });
        }
    }

    protected setCalendarEventType(value: any) {
        switch (value) {
            case EventTypes.default:
                this._normalizedSchedule = this.defaultSchedule;
                this._rrule = RRule.fromString(this.defaultSchedule?.recurrence ?? "") || undefined;
                break;
            case EventTypes.period:
                this._normalizedSchedule = {
                    start: this._normalizedSchedule?.start ?? moment().startOf("day").toDate().getTime(),
                    end: this._normalizedSchedule?.end ?? moment().startOf("day").endOf("day").toDate().getTime()
                };
                this._rrule = undefined;
                break;
            case EventTypes.recurrence:
                this._normalizedSchedule = {
                    start: this._normalizedSchedule?.start ?? moment().startOf("day").toDate().getTime(),
                    end: this._normalizedSchedule?.end ?? moment().startOf("day").endOf("day").toDate().getTime(),
                    recurrence: "FREQ=DAILY"
                };
                break;
        }
        this._eventType = value;
        this._dialog!.requestUpdate();
    }

    /**
     * Apply the timezone offset to the calendarEvent
     * @param schedule The schedule for which to add/substract the offset
     * @param substract Whether to substract the offset
     * @returns The transformed calendar event
     */
    protected applyTimezoneOffset(schedule?: CalendarEvent, substract = false): CalendarEvent | undefined {
        if (schedule) {
            const offset = (substract ? -this.timezoneOffset : this.timezoneOffset);
            let { start, end, recurrence } = { ...schedule };
            if (start) start += offset;
            if (end) end += offset;
            if (recurrence && RRule.fromString(recurrence).origOptions.until) {
                const origOptions = RRule.fromString(recurrence).origOptions;
                origOptions.until = new Date(Number(origOptions.until) + offset)
                recurrence = this.getRRule(new RRule(origOptions))
            }
            return { start, end, recurrence }
        }
    }

    protected render() {
        return html`
            <or-mwc-input outlined .type="${InputType.BUTTON}" label="${this.timeLabel()}" @or-mwc-input-changed="${() => this.showDialog()}"></or-mwc-input>
        `;
    }

    protected showDialog() {
        this._dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t(this.header))
            .setStyles(html`
                <style>
                    #dialog-content {
                        max-height: 100vh;
                        overflow: auto;
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
                        if (this._normalizedSchedule && this.isAllDay) {
                            this._normalizedSchedule.start = moment(this._normalizedSchedule.start).startOf("day").toDate().getTime();
                            this._normalizedSchedule.end = moment(this._normalizedSchedule.end).startOf("day").endOf("day").toDate().getTime();
                        }
                        if (this._eventType === EventTypes.default) {
                            delete this._normalizedSchedule;
                        } else if (this._eventType === EventTypes.recurrence) {
                            this._normalizedSchedule!.recurrence = this.getRRule();
                        }
                        const schedule = this.applyTimezoneOffset(this._normalizedSchedule ?? this.defaultSchedule, true);
                        this.dispatchEvent(new OrSchedulerChangedEvent(schedule));
                        this._dialog = undefined;
                    }
                },
            ])
            .setContent(() => this.getDialogContent())
            .setDismissAction(null));
    }

    protected getDialogContent(): TemplateResult {
        const calendar = this._normalizedSchedule;

        return html`
            <div style="min-width: 635px; display:grid; flex-direction: row;">
                <div id="event-type" class="section">
                    <label class="title"><or-translate value="schedule.type"></or-translate></label>
                    <div class="layout horizontal">
                        <or-mwc-input style="width: 100%"
                                    .value="${this._eventType}"
                                    .type="${InputType.SELECT}"
                                    .options="${Object.entries(this._eventTypes)}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setCalendarEventType(e.detail.value)}"></or-mwc-input>
                    </div>
                </div>
                ${this._eventType === EventTypes.recurrence ? this.getRepeatTemplate() : ``}
                ${calendar && (this._eventType === EventTypes.period || this._eventType === EventTypes.recurrence) ? this.getPeriodTemplate(calendar) : ``}
                ${this._eventType === EventTypes.recurrence ? this.getEndsTemplate() : ``}
            </div>`;
    }

    /**
     * Check if this frequency is allowed
     * @param freq The frequency to check
     * @returns Whether the frequency is allowed
     */
    protected isAllowedFrequency(freq: Frequency) {
        // Secondly is disabled because it's not possible to configure the period on a seconds basis
        // with the current period fields
        return freq !== "SECONDLY" && !this.disabledFrequencies.includes(freq);
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
    protected getRepeatTemplate(): TemplateResult {
        const interval = this._rrule?.origOptions.interval ?? 1;
        const frequency = this._rrule?.origOptions.freq ?? FrequencyValue.DAILY;
        const frequencies = Object.entries(FREQUENCIES)
            .map(([k,v]) => [String(FrequencyValue[k as Frequency]), i18next.t(v, { count: interval })])
            .filter(([k]) => this.isAllowedFrequency(FrequencyValue[k as Frequency] as any))

        return html`
            <div id="recurrence" class="section">
                <label class="title"><or-translate value="schedule.repeat"></or-translate></label>
                <div class="layout horizontal" style="display: flex; gap: 8px;">
                    ${when(!this.disabledRRuleParts?.includes("interval"), () => html`
                        <or-mwc-input style="width: 60px;" min="1" max="9" .value="${interval}" .type="${InputType.NUMBER}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "interval")}"></or-mwc-input>
                    `)}
                    <or-mwc-input style="flex: 1;" .value="${frequency.toString()}" .type="${InputType.SELECT}" .options="${frequencies}"
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
                        <or-mwc-input .disabled=${this.isAllDay} .value="${moment(calendar.start).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "start-time")}" .label="${i18next.t("from")}"></or-mwc-input>
                    </div>
                    <div>
                        <or-mwc-input .value="${moment(calendar.end).format("YYYY-MM-DD")}" .type="${InputType.DATE}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "end")}" .label="${i18next.t("to")}"></or-mwc-input>
                        <or-mwc-input .disabled=${this.isAllDay} .value="${moment(calendar.end).format("HH:mm")}" .type="${InputType.TIME}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "end-time")}" .label="${i18next.t("to")}"></or-mwc-input>
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
    protected getEndsTemplate(): TemplateResult {
        return html`
            <div id="recurrence-ends" class="section">
                <label class="title"><or-translate value="schedule._ends"></or-translate></label>
                <div style="display: flex; gap: 8px;" class="layout horizontal">
                    <or-mwc-input style="padding-right: 10px" .type="${InputType.RADIO}"
                        .value="${this._ends}"
                        .options="${Object.entries(rruleEnds).filter(([k]) => !this.disabledRRuleParts?.includes(k as RulePartKey))}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "recurrence-ends")}">
                    </or-mwc-input>
                    <div style="display: flex; flex-direction: column-reverse; flex: 1">
                        ${when(!this.disabledRRuleParts.includes("count"), () => html`
                            <or-mwc-input ?disabled="${this._ends !== "count"}" min="1" .type="${InputType.NUMBER}"
                                .value="${this._count}"
                                .label="${i18next.t("schedule.count", { count: this._count })}"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "count")}">
                            </or-mwc-input>`
                        )}
                        ${when(!this.disabledRRuleParts.includes("until"), () => html`
                            <or-mwc-input ?disabled="${this._ends !== "until"}" .type="${InputType.DATETIME}"
                                .value="${moment(this._until).format("YYYY-MM-DD HH:mm")}"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setRRuleValue(e.detail.value, "until")}">
                            </or-mwc-input>`
                        )}
                    </div>
                </div>
            </div>`;
    }
}
