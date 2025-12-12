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
import { html } from "lit";
import { CalendarEvent } from "@openremote/model";
import { 
    JsonFormsStateContext,
    getTemplateWrapper,
    JsonFormsRendererRegistryEntry,
    RankedTester,
    rankWith,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    and,
    isObjectControl,
    schemaMatches,
} from "@openremote/or-json-forms";
import { Frequency, RulePartKey, OrSchedulerChangedEvent } from "@openremote/or-scheduler";
import "@openremote/or-scheduler";

const DISABLED_FREQUENCIES = [
    // Disallowed as we cannot guarantee second accuracy in the SimulatorProtocol
    'SECONDLY'
] as Frequency[]

const DISABLED_RRULE_PARTS = [
    // Disabled for now, to reduce complexity
    'bymonth',
    'byweekno',
    'byyearday',
    'bymonthday',
    'byhour',
    'byminute',
    // Disallowed as we cannot guarantee second accuracy in the SimulatorProtocol
    'bysecond'
] as RulePartKey[]

const schedulerTester: RankedTester = rankWith(
    6,
    and(isObjectControl, schemaMatches((schema) => schema.format === "simulator-schedule"))
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
        end: now - millisSinceStartOfDay + dayInMillis,
        recurrence: "FREQ=DAILY"
    }

    // Init the schedule field with the default value
    if (!Object.keys(props.data).length) {
        props.handleChange(props.path, defaultEvent);
    }

    const onSchedulerChanged = (event: OrSchedulerChangedEvent | undefined) => {
        const calEvent = event?.detail.value;
        if (calEvent?.start && calEvent?.end) {
            props.handleChange(props.path, calEvent);
        }
    };

    let deleteHandler: undefined | (() => void);
    if (!props.required && props.path) {
        deleteHandler = () => {
            props.handleChange(props.path, undefined);
        }
    }

    return getTemplateWrapper(html`
        <or-scheduler
            header="scheduleSimulatorActivity"
            defaultEventTypeLabel="defaultSimulatorSchedule"
            .defaultSchedule="${defaultEvent}"
            .disabledFrequencies="${DISABLED_FREQUENCIES}"
            .disabledRRuleParts="${DISABLED_RRULE_PARTS}"
            .schedule="${props.data}"
            .timezoneOffset="${new Date().getTimezoneOffset() * 60000}"
            @or-scheduler-changed="${onSchedulerChanged}"
        >
        </or-scheduler>
    `, deleteHandler);
};

export const schedulerRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: schedulerTester,
    renderer: schedulerRenderer
};
