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
import { customElement } from "lit/decorators.js";
import { html, type LitElement } from "lit";
import { styleMap } from "lit/directives/style-map.js";
import { Tabs } from "@vaadin/tabs/vaadin-tabs.js";
import { registerStyles, css } from "@vaadin/vaadin-themable-mixin/register-styles.js";
import type { OrVaadinComponent } from "./util";
import "./or-vaadin-tab";

/*
 * The Vaadin base tabs styles read the `--vaadin-tabs-*` properties set here. The tabs share the bar equally, so the
 * indicator is sized from the number of tabs and moved by the index of the selected one.
 */
registerStyles(
  "or-vaadin-tabs",
  css`
    :host {
      --_gap: var(--or-tabs-gap, 10px);
      --_padding: var(--or-tabs-padding, var(--lumo-space-xs, 4px));
      --_radius: var(--or-tabs-radius, 99px);
      --vaadin-tabs-background: var(--or-tabs-background, var(--lumo-contrast-5pct, #f2f3f2));
      --vaadin-tabs-border-radius: var(--_radius);
      --vaadin-tabs-gap: var(--_gap);
      --vaadin-tabs-padding: var(--_padding);
    }

    /* A stacked bar is narrower than it is tall, so round it as much as the horizontal bar instead of fully */
    :host([orientation="vertical"]) {
      --_radius: var(--or-tabs-radius, calc(var(--or-tab-height, var(--lumo-size-l, 44px)) / 2 + var(--_padding)));
    }

    #indicator {
      position: absolute;
      inset-block: var(--_padding);
      inset-inline-start: var(--_padding);
      width: calc((100% - 2 * var(--_padding) - (var(--_count) - 1) * var(--_gap)) / var(--_count));
      border-radius: var(--_radius);
      background: var(--or-tabs-indicator-color, var(--lumo-base-color, #ffffff));
      translate: calc(var(--_direction, 1) * var(--_index) * (100% + var(--_gap))) 0;
      transition: translate var(--or-tabs-transition-duration, 0.3s) ease-out;
    }

    :host([dir="rtl"]) #indicator {
      --_direction: -1;
    }

    :host([orientation="vertical"]) #indicator {
      inset-inline: var(--_padding);
      inset-block-end: auto;
      width: auto;
      height: calc((100% - 2 * var(--_padding) - (var(--_count) - 1) * var(--_gap)) / var(--_count));
      translate: 0 calc(var(--_index) * (100% + var(--_gap)));
    }

    @media (prefers-reduced-motion: reduce) {
      #indicator {
        transition: none;
      }
    }
  `
);

/**
 * A row of `<or-vaadin-tab>` elements in a pill-shaped bar, with an indicator that slides behind the selected tab.
 * The tabs share the bar equally. Hidden tabs are left out of the layout, but only when they are hidden before they
 * are added or the selection changes.
 *
 * @customElement "or-vaadin-tabs"
 * @slot - The `<or-vaadin-tab>` elements.
 * @attr {string} orientation - `horizontal` (default) or `vertical`.
 * @fires {CustomEvent} selected-changed - Fired when the `selected` property changes.
 * @fires {CustomEvent} items-changed - Fired when the `items` property changes.
 * @cssprop --or-tabs-background - Background of the bar.
 * @cssprop --or-tabs-indicator-color - Color of the indicator behind the selected tab.
 * @cssprop --or-tabs-padding - Space between the bar edge and the tabs.
 * @cssprop --or-tabs-gap - Space between the tabs.
 * @cssprop --or-tabs-radius - Corner radius of the bar, the indicator and the tabs. By default the tabs and the
 * indicator are full pills, and the bar is rounded to match them.
 * @cssprop --or-tabs-transition-duration - Duration of the indicator slide and the label color change.
 */
@customElement("or-vaadin-tabs")
export class OrVaadinTabs extends (Tabs as new () => Tabs & LitElement) implements OrVaadinComponent {
  static get is() {
    return "or-vaadin-tabs";
  }

  protected override render() {
    const items = this.items ?? [];
    const visible = items.filter((item) => !(item as HTMLElement).hidden);
    const selected = this.selected == null ? undefined : items[this.selected];
    const index = selected ? visible.indexOf(selected) : -1;

    return html`
      <div
        id="indicator"
        aria-hidden="true"
        ?hidden=${index < 0}
        style=${styleMap({ "--_index": `${index}`, "--_count": `${visible.length}` })}
      ></div>
      ${super.render()}
    `;
  }
}
