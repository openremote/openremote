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
import { LitElement } from "lit";
import {property} from "lit/decorators.js";
import { input } from "./flow-editor";
import { i18next, translate } from "@openremote/or-translate";

export class SelectableElement extends translate(i18next)(LitElement) {
    public get selected() {
        return this.isSelected;
    }

    public get handle() {
        return this.selectableHandle;
    }

    @property({ type: Boolean }) private isSelected = false;
    @property({ attribute: false }) private selectableHandle!: HTMLElement;

    public disconnectedCallback() {
        super.disconnectedCallback();
        if (this.selected) {
            input.selected.splice(input.selected.indexOf(this), 1);
        }
        this.isSelected = false;
        input.removeListener("selected", this.onSelected);
        input.removeListener("deselected", this.onDeselected);
        input.selectables.splice(input.selectables.indexOf(this), 1);
    }

    public setHandle(element: HTMLElement) {
        if (this.selectableHandle) {
            this.selectableHandle.removeEventListener("mousedown", this.handleSelection);
        }
        element.addEventListener("mousedown", this.handleSelection);
        this.selectableHandle = element;
    }

    protected firstUpdated() {
        this.setHandle(this);
        input.selectables.push(this);
        input.addListener("selected", this.onSelected);
        input.addListener("deselected", this.onDeselected);
    }

    private readonly onSelected = (e: HTMLElement) => {
        if (e === this) {
            this.isSelected = true;
        }
    }

    private readonly onDeselected = (e: HTMLElement) => {
        if (e === this) {
            this.isSelected = false;
        }
    }

    private readonly handleSelection = (event: MouseEvent) => {
        if (event.buttons === 1) {
            input.handleSelection(this);
            event.stopPropagation();
        } else if (event.buttons === 2) {
            input.handleSelection(this, true);
        }
    }
}
