import {
    RankedTester,
    rankWith,
    isStringControl,
    or,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps
} from "@jsonforms/core";
import { JsonFormsStateContext, getTemplateWrapper, JsonFormsRendererRegistryEntry } from "@openremote/or-json-forms";
import { html } from "lit";

const agentIdTester: RankedTester = rankWith(
    6,
    or(
        isStringControl,
        (uischema, schema) => {
            return schema.format === "or-agent-id";
        }
    )
);
const agentIdRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const template = html`<div>AGENT PICKER</div>`;
    return getTemplateWrapper(template, state, props);
};

export const agentIdRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: agentIdTester,
    renderer: agentIdRenderer
};
