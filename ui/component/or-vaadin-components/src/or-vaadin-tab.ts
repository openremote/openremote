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
import type { LitElement } from "lit";
import { Tab } from "@vaadin/tabs/vaadin-tab.js";
import { registerStyles, css } from "@vaadin/vaadin-themable-mixin/register-styles.js";
import type { OrVaadinComponent } from "./util";

/*
 * The Vaadin base tab styles read the `--vaadin-tab-*` properties set here. The tab has no background of its own in
 * either state, as `<or-vaadin-tabs>` draws the selected one as an indicator behind the tabs.
 */
registerStyles(
  "or-vaadin-tab",
  css`
    :host {
      --vaadin-tab-background: transparent;
      --vaadin-tab-text-color: var(--or-tab-color, var(--lumo-secondary-text-color, #646b64));
      --vaadin-tab-font-size: var(--or-tab-font-size, var(--lumo-font-size-m, 1rem));
      --vaadin-tab-font-weight: var(--or-tab-font-weight, 700);
      --vaadin-tab-line-height: var(--or-tab-line-height, 1.5);
      --vaadin-tab-padding: var(--or-tab-padding, 0 var(--lumo-space-s, 8px));
      --vaadin-tab-border-radius: var(--or-tabs-radius, 99px);

      /* Equal widths let <or-vaadin-tabs> place its indicator without measuring the tabs */
      flex: 1 1 0;
      min-width: 0;
      min-height: var(--or-tab-height, var(--lumo-size-l, 44px));
      box-sizing: border-box;
      text-align: center;
      overflow-wrap: anywhere;
      transition: color var(--or-tabs-transition-duration, 0.3s) ease-out;
    }

    /* Stacked tabs fit their labels, so the labels need room to clear the rounded ends */
    :host([orientation="vertical"]) {
      --vaadin-tab-padding: var(--or-tab-padding, 0 var(--lumo-space-m, 1rem));
      justify-content: center;
    }

    :host([selected]) {
      --vaadin-tab-background: transparent;
      --vaadin-tab-text-color: var(--or-tab-selected-color, var(--lumo-body-text-color, #1f211f));
    }

    @media (prefers-reduced-motion: reduce) {
      :host {
        transition: none;
      }
    }
  `
);

/**
 * A tab for `<or-vaadin-tabs>`. It takes an equal share of the tab bar, and wrapping its label in an `<a>` makes the
 * whole tab a link.
 *
 * @customElement "or-vaadin-tab"
 * @slot - Label content, such as text, an icon or an `<a>` for navigation.
 * @cssprop --or-tab-color - Label color when not selected.
 * @cssprop --or-tab-selected-color - Label color when selected.
 * @cssprop --or-tab-font-size - Label font size.
 * @cssprop --or-tab-font-weight - Label font weight.
 * @cssprop --or-tab-line-height - Label line height.
 * @cssprop --or-tab-height - Minimum height of the tab.
 * @cssprop --or-tab-padding - Padding of the tab.
 */
@customElement("or-vaadin-tab")
export class OrVaadinTab extends (Tab as new () => Tab & LitElement) implements OrVaadinComponent {
  static get is() {
    return "or-vaadin-tab";
  }
}
