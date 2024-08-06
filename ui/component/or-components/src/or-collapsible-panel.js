var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
/*
 * Copyright 2017, OpenRemote Inc.
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
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { DefaultColor5 } from "@openremote/core";
// language=CSS
const style = css `

    :host {
        display: block;
        box-sizing: content-box;
        margin: 0;
        overflow: hidden;
        transition: margin 225ms cubic-bezier(0.4, 0, 0.2, 1), box-shadow 280ms cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        background-color: var(--or-collapisble-panel-background-color);
        border-radius: 4px;
        border-width: 1px;
        border-style: solid;
    }

    :host([hidden]) {
        display: none;
    }
    
    #header {
        display: flex;
        height: 48px;
        flex-direction: row;
        font-family: Roboto,Helvetica Neue,sans-serif;
        font-size: 15px;
        font-weight: 400;
        align-items: center;
        padding: 0 24px 0 16px;
        border-radius: inherit;
    }
    
    #header.expandable {
        cursor: pointer;
        transition: height 225ms cubic-bezier(0.4, 0, 0.2, 1);
    }
    
    #header.expanded {
        height: 64px;
    }

    #header.expanded > #indicator {
    }
    
    #header-content {
        flex: 1;
        display: flex;
        flex-direction: row;
        overflow: hidden;
    }

    #header-title, #header-description {
        display: inline-flex;
        align-items: center;
    }
    
    #header-description {
        flex-grow: 2;
    }
    
    #indicator {
        align-self: center;
        margin-right: 6px;
        margin-left: -5px;
    }
    
    #content {
        height: 0;
        visibility: hidden;
    }
    
    #content.expanded {
        height: unset;
        visibility: visible;
    }

    or-icon {
        vertical-align: middle;
        --or-icon-width: 20px;
        --or-icon-height: 20px;
        margin-right: 2px;
        margin-left: -5px;
    }
`;
let OrCollapsiblePanel = class OrCollapsiblePanel extends LitElement {
    constructor() {
        super(...arguments);
        this.expanded = false;
        this.expandable = true;
    }
    static get styles() {
        return [
            style
        ];
    }
    _onHeaderClicked(ev) {
        if (!this.expandable) {
            return;
        }
        ev.preventDefault();
        this.expanded = !this.expanded;
    }
    render() {
        return html `
            <div id="header" class="${this.expandable ? "expandable" : ""} ${this.expandable && this.expanded ? "expanded" : ""}" @click="${(ev) => this._onHeaderClicked(ev)}">
                ${this.expandable ? html `<or-icon icon="chevron-${this.expanded ? "down" : "right"}"></or-icon>` : ""}
                <span id="header-content">
                    <span id="header-title"><slot name="header"></slot></span>
                    <span id="header-description"><slot name="header-description"></slot></span>
                </span>
            </div>
            <div id="content" class="${this.expandable && this.expanded ? "expanded" : ""}">
                <slot name="content"></slot>
            </div>
        `;
    }
};
__decorate([
    property({ type: Boolean })
], OrCollapsiblePanel.prototype, "expanded", void 0);
__decorate([
    property({ type: Boolean })
], OrCollapsiblePanel.prototype, "expandable", void 0);
__decorate([
    query("#header")
], OrCollapsiblePanel.prototype, "headerElem", void 0);
OrCollapsiblePanel = __decorate([
    customElement("or-collapsible-panel")
], OrCollapsiblePanel);
export { OrCollapsiblePanel };
//# sourceMappingURL=or-collapsible-panel.js.map