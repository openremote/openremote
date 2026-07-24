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
