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
import "../src/or-vaadin-toggle-group";

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
            description: "Toggle is an input field representing a binary on/off choice. It reuses the Vaadin checkbox as its base, so it supports the same states (checked, disabled, readonly, required) and events (`change`, `checked-changed`), while being exposed to assistive technology as a switch."
        }
    }
};

export const Primary: Story = {
    args: {
        label: "Toggle",
        checked: true
    }
};

/** Disabled and readonly toggles, in both the off and on state. */
export const DisabledReadonly: Story = {
    name: "Disabled & Readonly",
    parameters: {
        title: "Disabled & Readonly",
        summary: "Use `disabled` to block interaction entirely and `readonly` to show a value that cannot be edited. Both combine with `checked`.",
        docs: {
            story: {
                height: "130px"
            }
        }
    },
    render: () => html`
        <div style="display: grid; grid-template-columns: repeat(2, max-content); gap: 24px 64px; padding: 8px;">
            <or-vaadin-toggle label="Disabled, off" disabled></or-vaadin-toggle>
            <or-vaadin-toggle label="Disabled, on" checked disabled></or-vaadin-toggle>
            <or-vaadin-toggle label="Readonly, off" readonly></or-vaadin-toggle>
            <or-vaadin-toggle label="Readonly, on" checked readonly></or-vaadin-toggle>
        </div>
    `
};

/** Several toggles combined inside a toggle group, in both orientations. */
export const Groups: Story = {
    name: "Groups",
    parameters: {
        title: "Groups",
        summary: "Multiple toggles can be combined inside an `<or-vaadin-toggle-group>` to share a group label, helper text and validation. Under the theme the group is horizontal by default; use `theme=\"vertical\"` to stack them.",
        docs: {
            story: {
                height: "280px"
            }
        }
    },
    render: () => html`
        <div style="display: flex; flex-direction: column; gap: 32px; padding: 8px;">
            <or-vaadin-toggle-group label="Notifications (vertical)" theme="vertical" helper-text="Choose how you want to be notified">
                <or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>
                <or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>
                <or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>
            </or-vaadin-toggle-group>

            <or-vaadin-toggle-group label="Notifications (horizontal)" helper-text="Choose how you want to be notified">
                <or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>
                <or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>
                <or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>
            </or-vaadin-toggle-group>
        </div>
    `
};

/** Helper text gives extra context below the toggle. */
export const HelperText: Story = {
    name: "Helper text",
    parameters: {
        title: "Helper text",
        summary: "Use `helper-text` to show additional context below the toggle, for example to explain why a choice is `required`.",
        docs: {
            story: {
                height: "110px"
            }
        }
    },
    render: () => html`
        <div style="display: grid; grid-template-columns: repeat(2, max-content); gap: 24px 64px; padding: 8px;">
            <or-vaadin-toggle label="With helper text" checked helper-text="Additional context shown below the toggle"></or-vaadin-toggle>
            <or-vaadin-toggle label="Required" required helper-text="This choice is required"></or-vaadin-toggle>
        </div>
    `
};

export const examples = [DisabledReadonly, Groups, HelperText];

export {customElements, packageJson};


export default meta;
