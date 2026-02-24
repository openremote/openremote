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
import { setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers } from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import manager from "@openremote/core";
import { Frequency, INTUITIVE_NOT_APPLICABLE, type RRulePartKeys } from "../src/index";
import "../src/index";

const tagName = "or-scheduler";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-scheduler",
    component: tagName,
    args: args,
    argTypes: argTypes,
    render: storyArgs => template(storyArgs),
    excludeStories: /^[a-z].*/,
    parameters: {
        actions: {
            handles: events
        },
        docs: {
            subtitle: `<${tagName}>`,
            description: "The scheduler is a web component that implements the Recurrence Rule Standard for single/recurring events (ref rfc5545#section-3.8.5.3)."
        }
    }
};

export const Primary: Story = {
    render: (_args) => {
        // Unused frequencies
        const DISABLED_FREQUENCIES = [
            "SECONDLY"
        ] as Frequency[];
        // Unused rrule parts
        const DISABLED_RRULE_PARTS = [
            "bysecond" // Partially broken
        ] as RRulePartKeys[];
        return html`
            <or-scheduler
                open
                removable
                disableNegativeByPartValues
                .disabledFrequencies="${DISABLED_FREQUENCIES}"
                .disabledRRuleParts="${DISABLED_RRULE_PARTS}"
                .disabledByPartCombinations="${INTUITIVE_NOT_APPLICABLE}"
            ></or-scheduler>
        `;
    },
    parameters: {
        docs: {
            story: {
                height: "1000px"
            }
        }
    },
    loaders: [
        async storyArgs => ({
            orManager: await loadOrManager()
        })
    ]
};

export const examples: Story[] = [];

export { customElements, packageJson };

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

async function loadOrManager() {
    if (await manager.init({ managerUrl: "http://localhost:8080", realm: "master" })) {
        if (!manager.authenticated) {
            manager.login();
        }
        return manager;
    }
    throw new Error("Manager could not be initialized");
}


export default meta;
