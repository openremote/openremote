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
// Plain JS on purpose: the component-test bundle compiles .ts files against ui/test's tsconfig
// (rootDir ui/test), so TS component sources outside that dir fail the build.
import { css, html } from "lit";
import { OrElement } from "@openremote/or-element";

/** Renders a heading without defining any styles of its own. */
export class UnstyledElement extends OrElement {
  render() {
    return html`<h4>Heading</h4>`;
  }
}
customElements.define("test-unstyled-element", UnstyledElement);

/** Renders a heading with own styles that add to (underline) and override (font-weight) the shared ones. */
export class StyledElement extends OrElement {
  static styles = css`
    h4 {
      font-weight: 400;
      text-decoration: underline;
    }
  `;

  render() {
    return html`<h4>Heading</h4>`;
  }
}
customElements.define("test-styled-element", StyledElement);

/** Counts the lifecycle callbacks the platform delivers, without overriding connectedMoveCallback. */
export class LifecycleElement extends OrElement {
  constructor() {
    super();
    this.counts = { connected: 0, disconnected: 0 };
  }

  connectedCallback() {
    super.connectedCallback();
    this.counts.connected++;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.counts.disconnected++;
  }

  render() {
    return html`<h4>Lifecycle</h4>`;
  }

  /**
   * Relocates this element to a second parent, either as a move or as a plain re-insertion.
   * @param how "move" for moveBefore, "reinsert" for appendChild
   * @returns whether moveBefore exists, and the callback counts after relocating
   */
  async relocate(how) {
    const from = this.parentElement;
    const to = document.createElement("div");
    from.parentElement.appendChild(to);
    await this.updateComplete;

    const supported = typeof to.moveBefore === "function";
    if (how === "move" && supported) {
      to.moveBefore(this, null);
    } else {
      to.appendChild(this);
    }
    await new Promise((resolve) => setTimeout(resolve, 0));

    return { supported, ...this.counts };
  }
}
customElements.define("test-lifecycle-element", LifecycleElement);

/** Reproduces how a grid re-orders its children: the wrapper div moves, the component rides along. */
export class NestedMoveFixture extends HTMLElement {
  /**
   * Builds parent > [itemA > component, itemB], then relocates itemA to the end of the same parent.
   * @param how "move" for moveBefore, "reinsert" for appendChild
   * @returns whether moveBefore exists, and the component callback counts after relocating
   */
  async run(how) {
    const parent = document.createElement("div");
    this.appendChild(parent);

    const itemA = document.createElement("div");
    const itemB = document.createElement("div");
    parent.append(itemA, itemB);

    const component = document.createElement("test-lifecycle-element");
    itemA.appendChild(component);
    while (!(await component.updateComplete)) {
      /* keep waiting */
    }

    const supported = typeof parent.moveBefore === "function";
    if (how === "move" && supported) {
      parent.moveBefore(itemA, null);
    } else {
      parent.appendChild(itemA);
    }
    await new Promise((resolve) => setTimeout(resolve, 0));

    return { supported, order: Array.from(parent.children).indexOf(itemA), ...component.counts };
  }
}
customElements.define("nested-move-fixture", NestedMoveFixture);
