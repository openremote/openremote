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
import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query, queryAssignedElements} from "lit/decorators.js";
import {DefaultColor2} from "@openremote/core";
import {OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";

// language=CSS
const style = css`
    
    :host {
        display: block;
    }

    :host([hidden]) {
        display: none;
    }
`;

/**
 * This is a form element that supports any element that has a value property
 */
// TODO: Support any form element
@customElement("or-form")
export class OrForm extends LitElement {

    @queryAssignedElements()
    protected formNodes!: Node[];

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);



        // if (this._panel) {
        //     new SimpleBar(this._panel, {
        //         autoHide: this.autoHide,
        //         // @ts-ignore
        //         forceVisible: this.forceVisible
        //     });
        // }
    }

    render() {

        return html`
            <slot></slot>
        `;
    }

    public checkValidity(): boolean {
        let valid = false;

        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node as OrMwcInput).forEach(orMwcInput => {
            const inputValid = orMwcInput.checkValidity();
            valid = valid && inputValid;
        });

        return valid;
    }

    public reportValidity(): boolean {
        let valid = true;

        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node as OrMwcInput).forEach(orMwcInput => {
            const inputValid = orMwcInput.reportValidity();
            valid = valid && inputValid;
        });

        return valid;
    }

    public submit(): {[key: string]: any} {
        const data: {[key: string]: any} = {};
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node as OrMwcInput).forEach(orMwcInput => {
            data[orMwcInput.name! as string] = orMwcInput.value;
        });
        return data;
    }

    public reset() {
        this.formNodes.filter(node => node instanceof OrMwcInput && node.name).map(node => node as OrMwcInput).forEach(orMwcInput => {
            orMwcInput.value = undefined;
        });
    }
}
