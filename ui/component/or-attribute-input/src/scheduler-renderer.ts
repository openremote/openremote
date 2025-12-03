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
import { Frequency, RulePartKey, LabeledEventTypes, OrSchedulerChangedEvent } from "@openremote/or-scheduler";
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

    const now = Date.now()
    const dayInMillis = 86400_000
    const offset = now % dayInMillis
    const defaultEvent = {
        start: now - offset,
        end: now - offset + dayInMillis - 1,
        recurrence: "FREQ=DAILY"
    }

    if (!Object.keys(props.data).length) {
        props.handleChange(props.path, defaultEvent);
    }

    const onSchedulerChanged = (event: OrSchedulerChangedEvent | undefined) => {
        props.handleChange(props.path, event?.detail.value);
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
