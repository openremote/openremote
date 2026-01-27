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
import { EventEmitter } from "events";
import { SelectableElement } from "../components/selectable-element";
import { project } from "../components/flow-editor";

export class Input extends EventEmitter {
    public selected: SelectableElement[] = [];
    public selectables: SelectableElement[] = [];
    private keysCurrentlyHeld: string[] = [];

    constructor() {
        super();
        window.addEventListener("keydown", this.onkeydown);
        window.addEventListener("keyup", this.onkeyup);
        window.addEventListener("blur", () => {
            this.clearSelection();
            this.keysCurrentlyHeld = [];
        });
        project.addListener("cleared", () => { this.clearSelection(); });
        this.setMaxListeners(1024);
    }

    public select(element: SelectableElement, forceMultipleSelection = false) {
        if (!this.multiSelectedEnabled && !forceMultipleSelection) { this.clearSelection(); }
        if (this.selected.includes(element)) { return; }
        this.emit("selected", element);
        if (element.selected) {
            this.selected.push(element);
        }
    }

    public deselect(element: SelectableElement) {
        const index = this.selected.indexOf(element);
        if (index === -1) {
            console.warn("Attempt to deselect nonexistent node");
            return;
        }
        this.emit("deselected", element);
        if (!element.selected) {
            this.selected.splice(index, 1);
        }
    }

    public handleSelection(element: SelectableElement, neverDeselect = false) {
        if (!this.multiSelectedEnabled && this.selected.length > 1) {
            this.select(element);
        } else if (this.selected.includes(element) && !neverDeselect) {
            this.deselect(element);
        } else {
            this.select(element);
        }
    }

    public clearSelection(ignoreMultiselect = false) {
        if (this.multiSelectedEnabled && !ignoreMultiselect) { return; }
        this.selected.forEach((e) => this.emit("deselected", e));
        this.selected = [];
        this.emit("selectioncleared");
    }

    public isHeld(key: string) {
        return this.keysCurrentlyHeld.includes(key);
    }

    public get multiSelectedEnabled() {
        return this.isHeld("Shift") || this.isHeld("Control");
    }

    private onkeydown = (e: KeyboardEvent) => {
        if (this.keysCurrentlyHeld.includes(e.key)) { return; }
        this.keysCurrentlyHeld.push(e.key);
    }

    private onkeyup = (e: KeyboardEvent) => {
        const index = this.keysCurrentlyHeld.indexOf(e.key);
        if (index === -1) { return; }
        this.keysCurrentlyHeld.splice(index, 1);
    }
}
