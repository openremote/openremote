import {
    RankedTester,
    rankWith,
    and,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    uiTypeIs,
    formatIs,
    scopeEndsWith
} from "@jsonforms/core";
import { JsonFormsStateContext, getTemplateWrapper, JsonFormsRendererRegistryEntry } from "@openremote/or-json-forms";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { html } from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-schedular";
import { i18next } from "@openremote/or-translate";
import { until } from "lit/directives/until.js";


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
    console.log(props.data)
    return getTemplateWrapper(html`<or-schedular></or-schedular>`, undefined);
};

export const calendarEventRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: calendarEventTester,
    renderer: calendarEventRenderer
};
