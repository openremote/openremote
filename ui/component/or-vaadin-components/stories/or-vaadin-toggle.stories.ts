/*
 * Copyright 2026, OpenRemote Inc.
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
import {setCustomElementsManifest, type Meta, type StoryObj} from "@storybook/web-components";
import {getORStorybookHelpers} from "../../storybook-utils";
import customElements from "../custom-elements.json" with {type: "json"};
import packageJson from "../package.json" with {type: "json"};
import {html} from "lit";
import "../src/or-vaadin-toggle";
import "../src/or-vaadin-checkbox-group";

const tagName = "or-vaadin-toggle";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-vaadin-components/toggle",
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
            description: "Toggle is an input field representing a binary on/off choice. It reuses the Vaadin checkbox as its base, so it supports the same states (checked, disabled, readonly, indeterminate, required) and events (`change`, `checked-changed`)."
        }
    }
};

export const Primary: Story = {
    args: {
        label: "Toggle",
        checked: true,
        helperText: "Helper text shown below the toggle"
    }
};

/** Every attribute-driven visual state of the toggle in a single view. */
export const States: Story = {
    name: "States & attributes",
    parameters: {
        title: "States & attributes",
        summary: "The toggle covers the standard checkbox states. Use `checked` for the on/off value, `disabled` to block interaction, `readonly` to show a non-editable value, `indeterminate` for a mixed state, and `required` for validation. The label comes from the `label` attribute and is optional."
    },
    render: () => html`
        <div style="display: grid; grid-template-columns: repeat(2, max-content); gap: 24px 64px; padding: 8px;">
            <or-vaadin-toggle label="Off (default)"></or-vaadin-toggle>
            <or-vaadin-toggle label="On" checked></or-vaadin-toggle>
            <or-vaadin-toggle label="Disabled, off" disabled></or-vaadin-toggle>
            <or-vaadin-toggle label="Disabled, on" checked disabled></or-vaadin-toggle>
            <or-vaadin-toggle label="Readonly, off" readonly></or-vaadin-toggle>
            <or-vaadin-toggle label="Readonly, on" checked readonly></or-vaadin-toggle>
            <or-vaadin-toggle label="Indeterminate" indeterminate></or-vaadin-toggle>
            <or-vaadin-toggle label="Required" required></or-vaadin-toggle>
            <or-vaadin-toggle label="With helper text" checked helper-text="Additional context shown below the toggle"></or-vaadin-toggle>
            <or-vaadin-toggle checked title="No label"></or-vaadin-toggle>
        </div>
    `
};

/** Several toggles combined inside a checkbox group, in both orientations. */
export const Group: Story = {
    name: "Toggle group",
    parameters: {
        title: "Grouped toggles",
        summary: "Multiple toggles can be combined inside an `<or-vaadin-checkbox-group>` to share a group label, helper text and validation. The group is horizontal by default; use `theme=\"vertical\"` to stack them."
    },
    render: () => html`
        <div style="display: flex; flex-direction: column; gap: 32px; padding: 8px;">
            <or-vaadin-checkbox-group label="Notifications (vertical)" theme="vertical" helper-text="Choose how you want to be notified">
                <or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>
                <or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>
                <or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>
            </or-vaadin-checkbox-group>

            <or-vaadin-checkbox-group label="Notifications (horizontal)" helper-text="Choose how you want to be notified">
                <or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>
                <or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>
                <or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>
            </or-vaadin-checkbox-group>
        </div>
    `
};

export const examples = [States, Group];

export {customElements, packageJson};


export default meta;
