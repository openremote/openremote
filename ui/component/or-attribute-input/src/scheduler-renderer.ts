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
import { Frequencies, RulePartKey, LabeledEventTypes, OrSchedulerChangedEvent, EventTypes } from "@openremote/or-scheduler";
import { CalendarEvent } from "@openremote/model";
import "@openremote/or-scheduler";

const schedulerTester: RankedTester = rankWith(
    6,
    scopeEndsWith('schedule')
    // and(uiTypeIs("Control"), formatIs("or-scheduler"))
);
const schedulerRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const onSchedulerChanged = (event: OrSchedulerChangedEvent | undefined) => {
        props.handleChange("schedule", event?.detail.value);
    };

    let deleteHandler: undefined | (() => void);
    if (!props.required && props.path) {
        deleteHandler = () => {
            props.handleChange(props.path, undefined);
        }
    }

    return getTemplateWrapper(html`
        <or-scheduler
            .calendarEvent="${props.data as CalendarEvent}"
            .default="${{
                start: moment().startOf("day").add(moment().utcOffset(), "m").toDate().getTime(),
                end: moment().startOf("day").add("day").add(moment().utcOffset(), "m").toDate().getTime(),
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
            @or-scheduler-changed="${onSchedulerChanged}"
        >
        </or-scheduler>
    `, deleteHandler);
};

export const schedulerRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: schedulerTester,
    renderer: schedulerRenderer
};
