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
import {setCustomElementsManifest, type Meta, type StoryObj} from "@storybook/web-components";
import {getORStorybookHelpers} from "../../storybook-utils";
import customElements from "../custom-elements.json" with {type: "json"};
import packageJson from "../package.json" with {type: "json"};
import {html} from "lit";
import "../src/or-vaadin-button";

const tagName = "or-vaadin-button";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-vaadin-components/button",
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
            description: "The Button component allows users to perform actions. It comes in several different style variants and supports icons as well as text labels."
        }
    }
};

export const Primary: Story = {
    render: args => template(args, html`<span>Button</span>`)
};

const themeVariants = [
    { theme: "primary" },
    { theme: "secondary" },
    { theme: "tertiary" }
];

export const Themes: Story = {
    name: "Themes example",
    parameters: {
        title: "Using different variants / themes",
        summary: "You can change the theme of the button using the `theme` HTML attribute:"
    },
    render: () => html`
        ${template(themeVariants[0], html`<span>Primary</span>`)}
        ${template(themeVariants[1], html`<span>Secondary</span>`)}
        ${template(themeVariants[2], html`<span>Tertiary</span>`)}
    `
};

export const examples = [Themes];

export {customElements, packageJson};


export default meta;
