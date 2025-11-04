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
import { Frequencies, RulePartKey, LabeledEventTypes } from "@openremote/or-calendar-event";
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

    // const onAgentChanged = (agent: Agent | undefined) => {
    //     if (!agents) {
    //         return;
    //     }

    //     if (agent) {
    //         const newAgentDescriptor = AssetModelUtil.getAssetDescriptor(agent.type) as AgentDescriptor;
    //         if (newAgentDescriptor) {
    //             props.handleChange("", {
    //               id: agent.id,
    //               type: newAgentDescriptor.agentLinkType
    //             });
    //         }
    //     }
    // };

    // const loadedTemplatePromise = loadAgents().then(agents => {

    //     const options: [string, string][] = agents.map(agent => [agent.id!, agent.name + " (" + agent.id + ")"]);

    //     return html`
    //         <or-mwc-input .label="${i18next.t("agentId")}" required class="agent-id-picker" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onAgentChanged(agents.find((agent) => agent.id === ev.detail.value))}" type="${InputType.SELECT}" .value="${props.data}" .placeholder="${i18next.t("selectAgent")}" .options="${options}"></or-mwc-input>
    //     `;
    // });

    // const template = html`
    //     <style>
    //         .agent-id-picker {
    //             min-width: 300px;
    //             max-width: 600px;
    //             width: 100%;
    //         }
    //     </style>
    //     ${until(loadedTemplatePromise, html`<or-mwc-input class="agent-id-picker" .type="${InputType.SELECT}"></or-mwc-input>`)}
    //     `;
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
            ] as RulePartKey[]}">
        </or-calendar-event>
    `, undefined);
};

export const calendarEventRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: calendarEventTester,
    renderer: calendarEventRenderer
};
