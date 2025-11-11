import {
    RankedTester,
    rankWith,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    scopeEndsWith
} from "@jsonforms/core";
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
        console.log(event, props.data)
        props.handleChange("schedule", event?.detail.value);
    };

    return getTemplateWrapper(html`
        <or-calendar-event
            .calendarEvent="${props.data as CalendarEvent}"
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
    `, undefined);
};

export const calendarEventRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: calendarEventTester,
    renderer: calendarEventRenderer
};
