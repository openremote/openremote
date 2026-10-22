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
import { styleMap } from "lit/directives/style-map.js";
import "../src/or-vaadin-tabs";
import type { OrVaadinTabs } from "../src/or-vaadin-tabs";

const tagName = "or-vaadin-tabs";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const pages = [
  { name: "Status", content: "Everything is running normally. The last reading came in a minute ago." },
  { name: "Mode", content: "The asset runs automatically and follows its schedule." },
  { name: "Settings", content: "Change the name, location and schedule of the asset." },
];

/** Tabs for the pages, linked to the panels with the given id prefix. */
const pageTabs = (prefix: string, tabPrefix = prefix) =>
  pages.map(
    ({ name }) =>
      html`<vaadin-tab id="${tabPrefix}-${name.toLowerCase()}-tab" aria-controls="${prefix}-${name.toLowerCase()}"
        >${name}</vaadin-tab
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

/** Selects the tab in every bar of the `data-tab-group` and shows its panel. */
const showPanel = (event: CustomEvent<{ value: number }>) => {
  const group = (event.target as Element).closest("[data-tab-group]");
  group?.querySelectorAll<OrVaadinTabs>("or-vaadin-tabs").forEach((tabs) => (tabs.selected = event.detail.value));
  group
    ?.querySelectorAll<HTMLElement>('[role="tabpanel"]')
    .forEach((panel, index) => (panel.hidden = index !== event.detail.value));
};

/** Tabs for the pages, without content. */
const plainTabs = () => pages.map(({ name }) => html`<vaadin-tab>${name}</vaadin-tab>`);

/** An example with a caption naming what it sets. */
const captioned = (caption: string, example: unknown) => html`
  <figure style="margin: 0;">
    <figcaption
      style="margin-bottom: var(--lumo-space-s); color: var(--lumo-secondary-text-color); font-size: var(--lumo-font-size-s);"
    >
      ${caption}
    </figcaption>
    ${example}
  </figure>
`;

/** More tabs than a narrow bar fits, so that it overflows. */
const overflowTabs = () =>
  ["First", "Second", "Third", "Fourth", "Fifth", "Sixth"].map((name) => html`<vaadin-tab>${name} tab</vaadin-tab>`);

/** Tabs with the given variant, captioned with it. */
const themed = (variant: string, tabs = plainTabs, style = "") =>
  captioned(
    `theme="${variant}"`,
    html`<or-vaadin-tabs theme=${variant} selected="0" style=${style}>${tabs()}</or-vaadin-tabs>`
  );

/** Tabs without a variant above tabs with the given one, so that the difference shows. */
const compared = (variant: string) => html`
  <div style="display: flex; flex-direction: column; gap: var(--lumo-space-l);">
    ${captioned("Default", html`<or-vaadin-tabs selected="0">${plainTabs()}</or-vaadin-tabs>`)} ${themed(variant)}
  </div>
`;

const meta: Meta = {
  title: "Playground/or-vaadin-components/tabs",
  component: tagName,
  args,
  argTypes,
  render: (storyArgs) => template(storyArgs, html`${plainTabs()}`),
  excludeStories: /^[a-z].*/,
  parameters: {
    actions: {
      handles: events,
    },
    docs: {
      subtitle: `<${tagName}>`,
      description:
        'Tabs switch between views or pages. They have the Lumo look and variants of `<vaadin-tabs>`. Set `theme="sliding"` for a more mobile-friendly bar with a contrasting background, where the `<vaadin-tab>` elements share the width equally and an indicator slides behind the selected one. Selection works as in `<vaadin-tabs>`, through the `selected` index and the `selected-changed` event, and the colors, sizes, spacing, rounding and timing of the sliding variant are set through `--or-tabs-*` and `--or-tab-*` properties.',
    },
  },
};

export const Primary: Story = {
  args: {
    selected: 0,
  },
};

/** Tabs stacked vertically beside their content. */
export const Vertical: Story = {
  name: "Vertical",
  parameters: {
    title: "Vertical",
    summary:
      'Set `orientation="vertical"` to stack the tabs, so the content can sit beside them. Listen to `selected-changed` to show the content of the selected tab.',
    docs: {
      story: {
        height: "170px",
      },
    },
  },
  render: () => html`
    <div data-tab-group style="display: flex; align-items: start; gap: var(--lumo-space-l);">
      <or-vaadin-tabs orientation="vertical" selected="0" @selected-changed=${showPanel}>
        ${pageTabs("vertical")}
      </or-vaadin-tabs>
      ${pagePanels("vertical", 0)}
    </div>
  `,
};

/** Tabs that share the width of the bar equally. */
export const EqualWidth: Story = {
  name: "Equal width",
  parameters: {
    title: "Equal width",
    summary:
      'The Lumo `theme="equal-width-tabs"` variant makes the tabs share the width of the bar equally. Sliding tabs always do this.',
    docs: {
      story: {
        height: "210px",
      },
    },
  },
  render: () => compared("equal-width-tabs"),
};

/** Tabs centered in the bar. */
export const Centered: Story = {
  name: "Centered",
  parameters: {
    title: "Centered",
    summary: 'The Lumo `theme="centered"` variant centers the tabs in the bar.',
    docs: {
      story: {
        height: "210px",
      },
    },
  },
  render: () => compared("centered"),
};

/** Smaller tabs. */
export const Small: Story = {
  name: "Small",
  parameters: {
    title: "Small",
    summary: 'The Lumo `theme="small"` variant makes the tabs smaller, for where space is limited.',
    docs: {
      story: {
        height: "210px",
      },
    },
  },
  render: () => compared("small"),
};

/** Tabs with as little styling as possible. */
export const Minimal: Story = {
  name: "Minimal",
  parameters: {
    title: "Minimal",
    summary:
      'The Lumo `theme="minimal"` variant reduces the styling to a minimum, without the line along the bar and the underline below the selected tab.',
    docs: {
      story: {
        height: "210px",
      },
    },
  },
  render: () => compared("minimal"),
};

/** An overflowing bar with and without its scroll buttons. */
export const ScrollButtons: Story = {
  name: "Scroll buttons",
  parameters: {
    title: "Scroll buttons",
    summary:
      'A bar that overflows shows scroll buttons. The Lumo `theme="hide-scroll-buttons"` variant hides them and fades out the overflowing ends instead, and the tabs can still be scrolled. The `theme="show-scroll-buttons"` variant keeps them on touch devices, which hide them by default in other themes. Lumo shows them whenever the bar overflows, so here it looks like the default.',
    docs: {
      story: {
        height: "210px",
      },
    },
  },
  render: () => html`
    <div style="display: flex; flex-direction: column; gap: var(--lumo-space-l);">
      ${themed("hide-scroll-buttons", overflowTabs, "max-width: 360px;")}
      ${themed("show-scroll-buttons", overflowTabs, "max-width: 360px;")}
    </div>
  `,
};

/** The sliding variant, horizontally and vertically. */
export const Sliding: Story = {
  name: "Sliding",
  parameters: {
    title: "Sliding",
    summary:
      'The sliding variant is a more mobile-friendly alternative to the default tabs, for switching between a few views on a phone. Set `theme="sliding"` for a bar with a contrasting background, where the tabs share its width as large touch targets and an indicator slides behind the selected one. With `orientation="vertical"` the indicator slides vertically. The two bars here share one set of content, as selecting a tab in one sets `selected` on the other from its `selected-changed` event.',
    docs: {
      story: {
        height: "260px",
      },
    },
  },
  render: () => html`
    <div data-tab-group style="display: flex; flex-direction: column; gap: var(--lumo-space-m);">
      <or-vaadin-tabs theme="sliding" selected="0" @selected-changed=${showPanel}>
        ${pageTabs("sliding")}
      </or-vaadin-tabs>
      <div style="display: flex; align-items: start; gap: var(--lumo-space-l);">
        <or-vaadin-tabs theme="sliding" orientation="vertical" selected="0" @selected-changed=${showPanel}>
          ${pageTabs("sliding", "sliding-vertical")}
        </or-vaadin-tabs>
        ${pagePanels("sliding", 0)}
      </div>
    </div>
  `,
};

const roundings = [
  { caption: "0px", radius: "0px" },
  { caption: "3px", radius: "3px" },
  { caption: "Default, the Lumo medium radius", radius: undefined },
  { caption: "12px", radius: "12px" },
  { caption: "99px, a full pill", radius: "99px" },
];

/** The sliding variant with a range of `--or-tabs-radius` values. */
export const Rounding: Story = {
  name: "Rounding",
  parameters: {
    title: "Rounding",
    summary:
      "`--or-tabs-radius` rounds the tabs and the indicator, up to a full pill, and the corners of the bar follow it around the padding. The rounding is the same in either `orientation`.",
    docs: {
      story: {
        height: "760px",
      },
    },
  },
  render: () => html`
    <div style="display: flex; flex-direction: column; gap: var(--lumo-space-l);">
      ${roundings.map(({ caption, radius }) =>
        captioned(
          caption,
          html`<or-vaadin-tabs theme="sliding" selected="1" style=${styleMap({ "--or-tabs-radius": radius })}
            >${plainTabs()}</or-vaadin-tabs
          >`
        )
      )}
      <div style="display: flex; flex-wrap: wrap; gap: var(--lumo-space-l);">
        ${roundings.map(({ caption, radius }) =>
          captioned(
            caption,
            html`<or-vaadin-tabs
              theme="sliding"
              orientation="vertical"
              selected="1"
              style=${styleMap({ "--or-tabs-radius": radius })}
              >${plainTabs()}</or-vaadin-tabs
            >`
          )
        )}
      </div>
    </div>
  `,
};

export const examples = [Vertical, EqualWidth, Centered, Small, Minimal, ScrollButtons, Sliding, Rounding];

export { customElements, packageJson };

export default meta;
