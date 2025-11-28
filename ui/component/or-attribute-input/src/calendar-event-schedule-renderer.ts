import {
    RankedTester,
    rankWith,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    scopeEndsWith
} from "@jsonforms/core";
import moment from "moment";
import { html } from "lit";
import { i18next } from "@openremote/or-translate";
import { JsonFormsStateContext, getTemplateWrapper, JsonFormsRendererRegistryEntry } from "@openremote/or-json-forms";
import { Frequencies, RulePartKey, LabeledEventTypes, OrCalendarEventChangedEvent } from "@openremote/or-calendar-event";
import { CalendarEvent } from "@openremote/model";
import "@openremote/or-calendar-event";

const calendarEventTester: RankedTester = rankWith(
    6,
    scopeEndsWith('schedule')
    // and(uiTypeIs("Control"), formatIs("or-calendar-event"))
);
const calendarEventRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const onCalendarEventChanged = (event: OrCalendarEventChangedEvent | undefined) => {
        props.handleChange("schedule", event?.detail.value);
    };

    let deleteHandler: undefined | (() => void);
    if (!props.required && props.path) {
        deleteHandler = () => {
            props.handleChange(props.path, undefined);
        }
    }

    return getTemplateWrapper(html`
        <or-calendar-event
            .calendarEvent="${props.data as CalendarEvent}"
            .default="${{
                start: moment(Date.now()).startOf("day").toDate().getTime(),
                end: moment(Date.now()).startOf("day").add("day").toDate().getTime(),
                recurrence: "FREQ=DAILY"
            }}"
            .header="${i18next.t("simulatorSchedule")}"
            .eventTypes="${{
                default: i18next.t("defaultSimulatorSchedule"),
                period: i18next.t("planPeriod"),
                recurrence: i18next.t("planRecurrence"),
            } as LabeledEventTypes}"
            .excludeFrequencies="${[
                // Disallowed as we cannot guarantee second accuracy in the SimulatorProtocol
                'SECONDLY'
            ] as Frequencies[]}"
            .excludeRuleParts="${[
                // Disabled for now, to reduce complexity
                'bymonth',
                'byweekno',
                'byyearday',
                'bymonthday',
                'byhour',
                'byminute',
                // Disallowed as we cannot guarantee second accuracy in the SimulatorProtocol
                'bysecond'
            ] as RulePartKey[]}"
            @or-calendar-event-changed="${onCalendarEventChanged}"
        >
        </or-calendar-event>
    `, deleteHandler);
};

export const calendarEventRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: calendarEventTester,
    renderer: calendarEventRenderer
};
