import {
    RankedTester,
    rankWith,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    and,
    isObjectControl,
    schemaMatches,
} from "@jsonforms/core";
import { html } from "lit";
import { i18next } from "@openremote/or-translate";
import { CalendarEvent } from "@openremote/model";
import { JsonFormsStateContext, getTemplateWrapper, JsonFormsRendererRegistryEntry } from "@openremote/or-json-forms";
import { Frequency, RulePartKey, LabeledEventTypes, OrSchedulerChangedEvent } from "@openremote/or-scheduler";
import "@openremote/or-scheduler";

const schedulerTester: RankedTester = rankWith(
    6,
    and(isObjectControl, schemaMatches((schema) => schema.format === "or-scheduler"))
);
const schedulerRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    // Match default replay schedule (when schedule isn't defined)
    const now = Date.now()
    const dayInMillis = 86400_000
    const millisSinceStartOfDay = now % dayInMillis
    const defaultEvent = {
        start: now - millisSinceStartOfDay,
        end: now - millisSinceStartOfDay + dayInMillis - 1,
        recurrence: "FREQ=DAILY"
    }
    const tzOffset = new Date().getTimezoneOffset() * 60000;

    // Init the schedule field with the default value
    if (!Object.keys(props.data).length) {
        props.handleChange(props.path, defaultEvent);
    }

    const onSchedulerChanged = (event: OrSchedulerChangedEvent | undefined) => {
        const calEvent = event?.detail.value;
        if (calEvent?.start && calEvent?.end) {
            props.handleChange(props.path, {
                ...calEvent,
                start: calEvent.start - tzOffset,
                end: calEvent.end - tzOffset,
            });
        }
    };

    let deleteHandler: undefined | (() => void);
    if (!props.required && props.path) {
        deleteHandler = () => {
            props.handleChange(props.path, undefined);
        }
    }

    const { start, end, recurrence } = props.data as CalendarEvent;
    return getTemplateWrapper(html`
        <or-scheduler
            .calendarEvent="${{
                start: start ? start + tzOffset : start,
                end: end ? end + tzOffset : end,
                recurrence,
            }}"
            .default="${defaultEvent}"
            .header="${i18next.t("simulatorSchedule")}"
            .eventTypes="${{
                default: i18next.t("defaultSimulatorSchedule"),
                period: i18next.t("planPeriod"),
                recurrence: i18next.t("planRecurrence"),
            } as LabeledEventTypes}"
            disabledFrequencies="${[
                // Disallowed as we cannot guarantee second accuracy in the SimulatorProtocol
                'SECONDLY'
            ] as Frequency[]}"
            disabledRRuleParts="${[
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
