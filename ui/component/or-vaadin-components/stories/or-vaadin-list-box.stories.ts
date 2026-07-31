/*
 * Copyright 2026, OpenRemote Inc.
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
import { setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getORStorybookHelpers } from "../../storybook-utils";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import "../src/or-vaadin-list-box";
import "../src/or-vaadin-item";
import { html } from "lit";

const tagName = "or-vaadin-list-box";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
  title: "Playground/or-vaadin-components/list-box",
  component: tagName,
  args,
  argTypes,
  render: (storyArgs) => template(storyArgs),
  excludeStories: /^[a-z].*/,
  parameters: {
    actions: {
      handles: events,
    },
    docs: {
      subtitle: `<${tagName}>`,
      description: "List Box allows the user to select one or more values from a scrollable list of items.",
    },
  },
};

export const Primary: Story = {
  render: (args) =>
    template(
      args,
      html`
        <or-vaadin-item>Item 1</or-vaadin-item>
        <or-vaadin-item>Item 2</or-vaadin-item>
        <or-vaadin-item>Item 3</or-vaadin-item>
      `
    ),
};

export const examples: Story[] = [];

export { customElements, packageJson };

export default meta;
