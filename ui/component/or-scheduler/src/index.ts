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
import { customElement, property, query, state } from "lit/decorators.js";
import { Util } from "@openremote/core";
import { CalendarEvent } from "@openremote/model";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-vaadin-components/or-vaadin-checkbox";
import "@openremote/or-vaadin-components/or-vaadin-checkbox-group";
import "@openremote/or-vaadin-components/or-vaadin-date-picker";
import "@openremote/or-vaadin-components/or-vaadin-date-time-picker";
import "@openremote/or-vaadin-components/or-vaadin-dialog";
import "@openremote/or-vaadin-components/or-vaadin-icon";
import "@openremote/or-vaadin-components/or-vaadin-number-field";
import "@openremote/or-vaadin-components/or-vaadin-radio-group";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-multi-select-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-time-picker";
import "@openremote/or-translate";
import { translate, i18next } from "@openremote/or-translate";
import { InputType } from "@openremote/or-vaadin-components/util";
import { dialogRenderer, dialogHeaderRenderer, dialogFooterRenderer, OrVaadinDialog } from "@openremote/or-vaadin-components/or-vaadin-dialog";
import { Frequency as FrequencyValue, RRule, Weekday, WeekdayStr } from "rrule";
import moment from "moment";
import { BY_RRULE_PARTS, EventTypes, FREQUENCIES, MONTHS, NOT_APPLICABLE_BY_RRULE_PARTS, rruleEnds, WEEKDAYS } from "./util";
import type { RRulePartKeys, RRuleParts, LabeledEventTypes, Frequency } from "./types";
import { when } from "lit/directives/when.js";

export { RRuleParts, RRulePartKeys, Frequency, LabeledEventTypes };
export * from "./util";

type PartKeys = RRulePartKeys | "start" | "end" | "start-time" | "end-time" | "all-day"| "recurrence-ends";

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

export interface OrSchedulerRemovedEventDetail {
    value?: CalendarEvent;
}

export class OrSchedulerRemovedEvent extends CustomEvent<OrSchedulerRemovedEventDetail> {

    public static readonly NAME = "or-scheduler-removed";

    constructor(value?: CalendarEvent) {
        super(OrSchedulerRemovedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrSchedulerChangedEvent.NAME]: OrSchedulerChangedEvent;
        [OrSchedulerRemovedEvent.NAME]: OrSchedulerRemovedEvent;
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
    public disabledRRuleParts: RRulePartKeys[] = [];

    @property({ type: String })
    public header = "scheduleActivity";

    @property({ type: Boolean })
    public isAllDay = true;

    @property({ type: Boolean })
    public removable = false;

    @property({ type: Object })
    public schedule?: CalendarEvent = this.defaultSchedule;

    @property()
    public timezoneOffset = 0;

    @state()
    protected _count = 1;

    @state()
    protected _ends: keyof typeof rruleEnds = "never";

    @state()
    protected _normalizedSchedule = this._applyTimezoneOffset(this.schedule);

    @state()
    protected _rrule?: RRule;

    @query("#scheduler")
    protected _dialog!: OrVaadinDialog;

    protected _byRRuleParts?: RRulePartKeys[];
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
            this._normalizedSchedule = this._applyTimezoneOffset(this.schedule);
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
     * because the timezone is configurable.
     * @param rrule The recurrence rule to normalize.
     * @returns String representation of the defined Recurrence Rule
     */
    protected _getRRule(rrule = this._rrule): string | undefined {
        return rrule?.toString()?.split("RRULE:")?.[1]?.replace(/(UNTIL=\d+T\d+)Z/, "$1");
    }

    protected _setRRuleValue(value: any, key: PartKeys) {
        let origOptions = this._rrule?.origOptions;
        const calendarEvent = this._normalizedSchedule!;

        if (key === "interval" || key === "freq" || key.startsWith("by")) {
            if (key === "byweekday") {
                origOptions!.byweekday = (value as WeekdayStr[]).map(d => RRule[d]);
            } else {
                origOptions![key as RRulePartKeys] = value;
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
        this._normalizedSchedule.recurrence = this._getRRule();
    }

    protected _timeLabel(): string | undefined {
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

    protected _setCalendarEventType(event: any) {
        const value = event.target.value;
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
    }

    /**
     * Apply the timezone offset to the calendarEvent
     * @param schedule The schedule for which to add/substract the offset
     * @param substract Whether to substract the offset
     * @returns The transformed calendar event
     */
    protected _applyTimezoneOffset(schedule?: CalendarEvent, substract = false): CalendarEvent | undefined {
        if (schedule) {
            const offset = (substract ? -this.timezoneOffset : this.timezoneOffset);
            let { start, end, recurrence } = { ...schedule };
            if (start) start += offset;
            if (end) end += offset;
            if (recurrence && RRule.fromString(recurrence).origOptions.until) {
                const origOptions = RRule.fromString(recurrence).origOptions;
                origOptions.until = new Date(Number(origOptions.until) + offset)
                recurrence = this._getRRule(new RRule(origOptions))
            }
            return { start, end, recurrence }
        }
    }

    protected render() {
        const timeLabel = this._timeLabel();
        return html`
            <or-vaadin-button @click="${() => this._dialog!.open()}">${timeLabel?.charAt(0).toUpperCase()}${timeLabel?.slice(1)}</or-vaadin-button>
            <or-vaadin-dialog id="scheduler" header-title="${i18next.t(this.header)}" @closed="${this._onClose}"
                ${dialogHeaderRenderer(this._getDialogHeader, [])}
                ${dialogRenderer(this._getDialogContent, [
                    this.defaultSchedule,
                    this.defaultEventTypeLabel,
                    this.disabledFrequencies,
                    this.disabledRRuleParts,
                    this.header,
                    this.isAllDay,
                    this.schedule,
                    this.timezoneOffset,
                    this._count,
                    this._ends,
                    this._normalizedSchedule,
                    this._rrule,
                ])}
                ${dialogFooterRenderer(this._getDialogFooter, [])}
            ></or-vaadin-dialog>
        `;
    }

    protected _getDialogHeader(): TemplateResult {
        return html`
            <vaadin-button theme="tertiary" @click="${this._onClose}">
                <vaadin-icon icon="lumo:cross"></vaadin-icon>
            </vaadin-button>
        `;
    }

    protected _getDialogContent(): TemplateResult {
        const calendar = this._normalizedSchedule;
        return html`
            <style>
                or-vaadin-dialog::part(content) {
                    width: 600px;
                }

                vaadin-checkbox {
                    font-weight: 600;
                }

                .period {
                    display: flex;
                    flex: 1;
                    gap: 2px;
                }

                .section {
                    background-color: white;
                    border-radius: var(--lumo-border-radius-m);
                    display: flex;
                    flex-direction: column;
                    padding: var(--lumo-space-l);
                }

                .title {
                    padding-bottom: var(--lumo-space-m);
                    font-size: var(--lumo-font-size-l);
                    font-style: normal;
                    font-weight: 600;
                    line-height: 125.303%; /* 22.554px */
                }

                .label {
                    margin-left: 10px;
                }
                or-vaadin-number-field[disabled] + .label {
                    color: var(--lumo-contrast-20pct);
                    user-select: revert;
                    -webkit-user-select: none;
                }

                @media only screen and (min-width: 768px) {
                    or-vaadin-date-picker[combined] {
                        --vaadin-input-field-top-end-radius: 0;
                        --vaadin-input-field-bottom-end-radius: 0;
                    }
                    or-vaadin-time-picker {
                        --vaadin-input-field-top-start-radius: 0;
                        --vaadin-input-field-bottom-start-radius: 0;
                    }
                }

                @media only screen and (max-width: 768px) {
                    .period {
                        flex-wrap: wrap;
                    }
                }
            </style>
            <div style="display: flex; flex-direction: column; gap: var(--lumo-space-m); max-width: 85vw">
                <div id="event-type" class="section">
                    <label class="title"><or-translate value="schedule.type"></or-translate></label>
                    <div style="display: flex">
                        <or-vaadin-select style="flex: 1" .value="${this._eventType}" .items="${Object.entries(this._eventTypes).map(([k,v]) => ({ value: k, label: v }))}"
                            @change="${this._setCalendarEventType}">
                        </or-vaadin-select>
                    </div>
                </div>
                ${calendar && (this._eventType === EventTypes.period || this._eventType === EventTypes.recurrence) ? this._getPeriodTemplate(calendar) : ``}
                ${this._eventType === EventTypes.recurrence ? this._getRepeatTemplate() : ``}
                ${this._eventType === EventTypes.recurrence ? this._getEndsTemplate() : ``}
            </div>`;
    }

    protected _getDialogFooter(): TemplateResult {
        return html`
            ${when(this.removable, () => html`
                <or-vaadin-button style="background-color: unset; margin-right: auto" theme="error" @click="${this._onDelete}">
                    <or-icon icon="or:trash" style="--or-icon-fill: white"></or-icon>
                    <or-translate value="schedule.delete"></or-translate>
                </or-vaadin-button>`
            )}
            <or-vaadin-button theme="primary" @click="${this._onSave}"><or-translate value="schedule.save"></or-translate></or-vaadin-button>
        `;
    }

    protected _onClose() {
        this._dialog!.close();
    }

    protected _onDelete() {
        this._dialog!.close();
        this.dispatchEvent(new OrSchedulerRemovedEvent());
    }

    protected _onSave() {
        if (this._normalizedSchedule && this.isAllDay) {
            this._normalizedSchedule.start = moment(this._normalizedSchedule.start).startOf("day").toDate().getTime();
            this._normalizedSchedule.end = moment(this._normalizedSchedule.end).startOf("day").endOf("day").toDate().getTime();
        }
        if (this._eventType === EventTypes.default) {
            delete this._normalizedSchedule;
        } else if (this._eventType === EventTypes.recurrence) {
            this._normalizedSchedule!.recurrence = this._getRRule();
        }
        const schedule = this._applyTimezoneOffset(this._normalizedSchedule ?? this.defaultSchedule, true);
        this.dispatchEvent(new OrSchedulerChangedEvent(schedule));
        this._dialog!.close();
    }

    protected _onPartChange(part: PartKeys, valueProp: "checked" | "value" | "detail", value?: any) {
        return (e: any) => {
            if (valueProp === "detail") {
                if (!Util.objectsEqual(e.detail.value, value, true)) {
                    this._setRRuleValue(e.detail.value, part);
                }
                return;
            }
            this._setRRuleValue(e.target[valueProp], part);
        }
    }

    /**
     * Check if this frequency is allowed
     * @param freq The frequency to check
     * @returns Whether the frequency is allowed
     */
    protected _isAllowedFrequency(freq: Frequency) {
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
    protected _getRepeatTemplate(): TemplateResult {
        const interval = this._rrule?.origOptions.interval ?? 1;
        const frequency = this._rrule?.origOptions.freq ?? FrequencyValue.DAILY;
        const frequencies = Object.entries(FREQUENCIES)
            .map(([k, v]) => ({ value: String(FrequencyValue[k as Frequency]), label: i18next.t(v, { count: interval }) }))
            .filter(({ value }) => this._isAllowedFrequency(FrequencyValue[value as Frequency] as any))

        return html`
            <div id="recurrence" class="section">
                <label class="title"><or-translate value="schedule.repeat"></or-translate></label>
                <div style="display: flex; gap: 8px; margin-bottom: var(--lumo-space-l)">
                    ${when(!this.disabledRRuleParts?.includes("interval"), () => html`
                        <or-vaadin-number-field min="1" max="9" step-buttons-visible style="width: 106px" .value="${interval}"
                            @change="${this._onPartChange("interval", "value")}">
                        </or-vaadin-number-field>
                    `)}
                    <or-vaadin-select style="flex: 1;" .value="${frequency.toString()}" .items="${frequencies}"
                        @change="${this._onPartChange("freq", "value")}">
                    </or-vaadin-select>
                </div>
                <label class="title"><or-translate value="schedule.repeatOn"></or-translate></label>
                <div>${this._getByRulePart("bymonth", InputType.CHECKBOX_LIST, Object.entries(MONTHS))}</div>
                <div>
                    ${this._getByRulePart("byweekno", InputType.SELECT, byWeekNoOptions)}
                    ${this._getByRulePart("byyearday", InputType.SELECT, byYearDayOptions)}
                    ${this._getByRulePart("bymonthday", InputType.SELECT, byMonthDayOptions)}
                </div>
                <div>${this._getByRulePart("byweekday", InputType.CHECKBOX_LIST, Object.entries(WEEKDAYS))}</div>
                <div>
                    ${this._getByRulePart("byhour", InputType.SELECT, byHourOptions)}
                    ${this._getByRulePart("byminute", InputType.SELECT, byMinuteOrSecondsOptions)}
                    ${this._getByRulePart("bysecond", InputType.SELECT, byMinuteOrSecondsOptions)}
                </div>
            </div>`;
    }

    /**
     * Displays the BY_XXX RRule part fields if allowed by the frequency
     *
     * @returns The specified BY_XXX RRule part field or undefined if not applicable
     */
    protected _getByRulePart<T extends number | [string, string]>(part: RRulePartKeys, type: InputType, options: T[]): TemplateResult | undefined {
        if (!this._byRRuleParts?.includes(part)) {
            return undefined
        }

        const value = (part === "byweekday"
            ? (this._rrule?.origOptions?.byweekday as Weekday[])?.map(String)
            : this._rrule?.origOptions[part]
        ) ?? [];

        return type === InputType.CHECKBOX_LIST ? html`
          <or-vaadin-checkbox-group .value="${value}" @value-changed="${this._onPartChange(part, "detail", value)}" theme="button">
                ${(options as [string, string][]).map(([value, label]) => html`<vaadin-checkbox value="${value}" label="${i18next.t(label).slice(0, 3)}"></vaadin-checkbox>`)}
            </or-vaadin-checkbox-group>
        ` : html`
            <or-vaadin-multi-select-combo-box .label="${i18next.t(part)}" .items="${options}" @change="${this._onPartChange(part, "value")}"></or-vaadin-multi-select-combo-box>
        `;
    }

    /**
     * Displays the fields that define when and how long the particular event is
     *
     * @param calendar The specified calendar event
     * @returns The periods allDay and from/to date and time fields
     */
    protected _getPeriodTemplate(calendar: CalendarEvent): TemplateResult {
        return html`
            <div id="period" class="section">
                <label class="title"><or-translate value="period"></or-translate></label>
                <div style="display: flex; gap: 8px">
                    <div class="period">
                        <or-vaadin-date-picker style="text-transform: capitalize" ?combined="${!this.isAllDay}" .value="${moment(calendar.start).format("YYYY-MM-DD")}"
                            @change="${this._onPartChange("start", "value")}" label="${i18next.t("from")}"> </or-vaadin-date-picker>
                        <or-vaadin-time-picker style="margin-top: auto" ?hidden=${this.isAllDay} .value="${moment(calendar.start).format("HH:mm")}"
                            @change="${this._onPartChange("start-time", "value")}">
                        </or-vaadin-time-picker>
                    </div>
                    <div class="period">
                        <or-vaadin-date-picker style="text-transform: capitalize" ?combined="${!this.isAllDay}" .value="${moment(calendar.end).format("YYYY-MM-DD")}"
                            @change="${this._onPartChange("end", "value")}" label="${i18next.t("to")}">
                        </or-vaadin-date-picker>
                        <or-vaadin-time-picker style="margin-top: auto" ?hidden=${this.isAllDay} .value="${moment(calendar.end).format("HH:mm")}"
                            @change="${this._onPartChange("end-time", "value")}">
                        </or-vaadin-time-picker>
                    </div>
                </div>
                <or-vaadin-checkbox .checked=${this.isAllDay} @change="${this._onPartChange("all-day", "checked")}" .label="${i18next.t("allDay")}"></or-vaadin-checkbox>
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
    protected _getEndsTemplate(): TemplateResult {
        return html`
            <div id="recurrence-ends" class="section">
                <label class="title"><or-translate value="schedule._ends"></or-translate></label>
                <div style="display: flex; gap: 8px;">
                    <or-vaadin-radio-group style="padding-right: 10px" .value="${this._ends}" theme="vertical"
                        @change="${this._onPartChange("recurrence-ends", "value")}">
                        ${Object.entries(rruleEnds)
                                .filter(([k]) => !this.disabledRRuleParts?.includes(k as RRulePartKeys))
                                .map(([k, v]) => html`
                                    <vaadin-radio-button style="margin: 6px 0" value="${k}" label="${i18next.t(v)}"></vaadin-radio-button>
                        `)}
                    </or-vaadin-radio-group>
                    <div style="display: flex; flex-direction: column; flex: 1; ">
                        ${when(!this.disabledRRuleParts.includes("until"), () => html`
                            <or-vaadin-date-time-picker style="margin-top: auto; padding: var(--lumo-space-s) 0" ?disabled="${this._ends !== "until"}"
                                .value="${moment(this._until).format("YYYY-MM-DD HH:mm")}"
                                @change="${this._onPartChange("until", "value")}">
                            </or-vaadin-date-time-picker>`
                        )}
                        ${when(!this.disabledRRuleParts.includes("count"), () => html`<div style="display: inline-flex">
                            <or-vaadin-number-field ?disabled="${this._ends !== "count"}" min="1" style="width: 120px"
                                step-buttons-visible
                                .value="${this._count}"
                                @change="${this._onPartChange("count", "value")}">
                            </or-vaadin-number-field><or-translate class="label" value="schedule.count"
                                ?disabled="${this._ends !== "count"}" .options="${{ count: +this._count }}">
                            </or-translate>
                            </div>`
                        )}
                    </div>
                </div>
            </div>`;
    }
}
