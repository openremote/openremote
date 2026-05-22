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
import {css, html, LitElement, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {DefaultColor2, DefaultColor3} from "@openremote/core";
import {when} from "lit/directives/when.js";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const simpleBarStyle = require("simplebar/dist/simplebar.css");
const elevationStyle = require("@material/elevation/dist/mdc.elevation.css");

// language=CSS
const style = css`
    
    :host {
        --internal-or-panel-background-color: var(--or-panel-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-panel-padding: var(--or-panel-padding, 10px);
        --internal-or-panel-border: var(--or-panel-border, 1px solid #e5e5e5);
        --internal-or-panel-border-radius: var(--or-panel-border-radius, 5px);
        --internal-or-panel-heading-margin: var(--or-panel-heading-margin, 0 0 10px 7px);
        --internal-or-panel-heading-min-height: var(--or-panel-heading-min-height, 36px);
        --internal-or-panel-heading-color: var(--or-panel-heading-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-panel-heading-font-size: var(--or-panel-heading-font-size, larger);
        --internal-or-panel-heading-font-weight: var(--or-panel-heading-font-weight, bolder);
        --internal-or-panel-heading-text-transform: var(--or-panel-heading-text-transform, unset);
        display: block;
    }

    :host([hidden]) {
        display: none;
    }
        
    #wrapper {
        height: 100%;
        flex: 1;
    }
    
    #panel {
        padding: var(--internal-or-panel-padding);
        background-color: var(--internal-or-panel-background-color);
        border: var(--internal-or-panel-border);
        border-radius: var(--internal-or-panel-border-radius);
    }
    
    #heading {
        display: flex;
        align-items: center;
        margin: var(--internal-or-panel-heading-margin);
        min-height: var(--internal-or-panel-heading-min-height);
        font-size: var(--internal-or-panel-heading-font-size);
        font-weight: var(--internal-or-panel-heading-font-weight);
        color: var(--internal-or-panel-heading-color);
        text-transform: var(--internal-or-panel-heading-text-transform);
    }
`;

@customElement("or-panel")
export class OrPanel extends LitElement {

    static get styles() {
        return [
            css `${unsafeCSS(elevationStyle)}`,
            css `${unsafeCSS(simpleBarStyle)}`,
            style
        ];
    }

    @property({type: Number})
    zLevel?: number;

    @property({type: String})
    public heading?: string | TemplateResult;

    @query("#panel")
    protected _panel!: HTMLDivElement;

    render() {
        return html`
            <div id="wrapper">
                <div id="panel">
                    ${this.heading ? html`
                        ${when(!(typeof this.heading === 'string'), () => html`
                            ${this.heading}
                        `, () => html`
                            <div id="heading">
                                <span>${this.heading}</span>
                            </div>
                        `)}
                    ` : ``}
                    <slot></slot>
                </div>
            </div>
        `;
    }
}
