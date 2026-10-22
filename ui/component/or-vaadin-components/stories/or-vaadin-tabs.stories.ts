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
import customElements from "../build/custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import { html } from "lit";
import "../src/or-vaadin-tabs";

const tagName = "or-vaadin-tabs";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const pages = [
  { name: "Status", content: "Everything is running normally. The last reading came in a minute ago." },
  { name: "Mode", content: "The asset runs automatically and follows its schedule." },
  { name: "Settings", content: "Change the name, location and schedule of the asset." },
];

/** Tabs for the pages, linked to the panels with the same id prefix. */
const pageTabs = (prefix: string) =>
  pages.map(
    ({ name }) =>
      html`<or-vaadin-tab id="${prefix}-${name.toLowerCase()}-tab" aria-controls="${prefix}-${name.toLowerCase()}"
        >${name}</or-vaadin-tab
      >`
  );

/** Panels with the content of the pages, of which only the selected one is shown. */
const pagePanels = (prefix: string, selected: number) =>
  pages.map(
    ({ name, content }, index) =>
      html`<p
        id="${prefix}-${name.toLowerCase()}"
        role="tabpanel"
        aria-labelledby="${prefix}-${name.toLowerCase()}-tab"
        ?hidden=${index !== selected}
      >
        ${content}
      </p>`
  );

/** Shows the panel of the selected tab, out of the panels next to the tabs. */
const showPanel = (event: CustomEvent<{ value: number }>) => {
  const panels = (event.target as Element).parentElement?.querySelectorAll<HTMLElement>('[role="tabpanel"]');
  panels?.forEach((panel, index) => (panel.hidden = index !== event.detail.value));
};

const meta: Meta = {
  title: "Playground/or-vaadin-components/tabs",
  component: tagName,
  args,
  argTypes,
  render: (storyArgs) =>
    template(storyArgs, html`${pages.map(({ name }) => html`<or-vaadin-tab>${name}</or-vaadin-tab>`)}`),
  excludeStories: /^[a-z].*/,
  parameters: {
    actions: {
      handles: events,
    },
    docs: {
      subtitle: `<${tagName}>`,
      description:
        "Tabs is a more mobile-friendly alternative to `<vaadin-tabs>` for switching between views or pages. The `<or-vaadin-tab>` elements share a pill-shaped bar equally, and an indicator slides behind the selected one. Selection works as in `<vaadin-tabs>`, through the `selected` index and the `selected-changed` event, and the colors, sizes and spacing are set through `--or-tabs-*` and `--or-tab-*` properties.",
    },
  },
};

export const Primary: Story = {
  args: {
    selected: 1,
  },
};

/** Tabs that switch the content below them. */
export const Content: Story = {
  name: "Content",
  parameters: {
    title: "Content",
    summary: "Listen to `selected-changed` to show the content of the selected tab.",
    docs: {
      story: {
        height: "160px",
      },
    },
  },
  render: () => html`
    <div>
      <or-vaadin-tabs selected="0" @selected-changed=${showPanel}> ${pageTabs("content")} </or-vaadin-tabs>
      ${pagePanels("content", 0)}
    </div>
  `,
};

/** Tabs stacked vertically beside their content. */
export const Vertical: Story = {
  name: "Vertical",
  parameters: {
    title: "Vertical",
    summary:
      'Set `orientation="vertical"` to stack the tabs, and the indicator then slides vertically. The content can then sit beside the tabs.',
    docs: {
      story: {
        height: "200px",
      },
    },
  },
  render: () => html`
    <div style="display: flex; align-items: start; gap: var(--lumo-space-l);">
      <or-vaadin-tabs orientation="vertical" selected="2" @selected-changed=${showPanel}>
        ${pageTabs("vertical")}
      </or-vaadin-tabs>
      ${pagePanels("vertical", 2)}
    </div>
  `,
};

export const examples = [Content, Vertical];

export { customElements, packageJson };

export default meta;
