/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {
    RankedTester,
    rankWith,
    and,
    ControlProps,
    mapStateToControlProps,
    mapDispatchToControlProps,
    uiTypeIs,
    formatIs
} from "@jsonforms/core";
import manager, { OREvent } from "@openremote/core";
import { Agent } from "@openremote/model";
import { JsonFormsStateContext, getTemplateWrapper, JsonFormsRendererRegistryEntry } from "@openremote/or-json-forms";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { html } from "lit";
import { i18next } from "@openremote/or-translate";
import { until } from "lit/directives/until.js";
import { showOkCancelDialog } from "@openremote/or-mwc-components/or-mwc-dialog";

/**
 * This function creates a short lived cache for loading the list of agents; this is useful when multiple instances
 * of this control are used in a single UI
 */
let agents: Agent[] | undefined;
let loadingPromise: Promise<Agent[]> | undefined;
let subscribed = false;
const timeout = 2000;

export function loadAgents(): PromiseLike<Agent[]> {

    if (agents) {
        return Promise.resolve(agents);
    }

    if (loadingPromise) {
        return loadingPromise;
    }

    if (!subscribed) {
        manager.addListener((ev: OREvent) => {
            switch (ev) {
                case OREvent.DISPLAY_REALM_CHANGED:
                    agents = undefined;
                    loadingPromise = undefined;
                    break;
            }
        });
        
        manager.events!.subscribeAssetEvents(undefined, false, (assetEvent) => {
            if (assetEvent.asset && assetEvent.asset.type!.endsWith("Agent")) {
                agents = undefined;
                loadingPromise = undefined;
            }
        });

        subscribed = true;
    }

    loadingPromise = manager.rest.api.AssetResource.queryAssets({
        realm: {
            name: manager.displayRealm
        },
        types: [
            "Agent"
        ],
        select: {
            attributes: []
        }
    })
        .then(response => response.data as Agent[])
        .then(agnts => {
            agents = agnts;
            return agnts;
        });

    return loadingPromise;
}

const agentIdTester: RankedTester = rankWith(
    6,
    and(uiTypeIs("Control"), formatIs("or-agent-id"))
);
const agentIdRenderer = (state: JsonFormsStateContext, props: ControlProps) => {
    props = {
        ...props,
        ...mapStateToControlProps({jsonforms: {...state}}, props),
        ...mapDispatchToControlProps(state.dispatch)
    };

    const onAgentChanged = (agent: Agent | undefined) => {
        props.handleChange(props.path, agent ? agent.id : undefined);
        
    };

    const loadedTemplatePromise = loadAgents().then(agents => {

        const options: [string, string][] = agents.map(agent => [agent.id!, agent.name + " (" + agent.id + ")"]);

        return html`
            <or-mwc-input .label="${i18next.t("agentId")}" required class="agent-id-picker" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onAgentChanged(agents.find((agent) => agent.id === ev.detail.value))}" type="${InputType.SELECT}" .value="${props.data}" .placeholder="${i18next.t("selectAgent")}" .options="${options}"></or-mwc-input>
        `;
    });

    const template = html`
        <style>
            .agent-id-picker {
                min-width: 300px;
                max-width: 600px;
                width: 100%;
            }
        </style>
        ${until(loadedTemplatePromise, html`<or-mwc-input class="agent-id-picker" .type="${InputType.SELECT}"></or-mwc-input>`)}
        `;

    return getTemplateWrapper(template, undefined);
};

export const agentIdRendererRegistryEntry: JsonFormsRendererRegistryEntry = {
    tester: agentIdTester,
    renderer: agentIdRenderer
};
