/*
 * Copyright 2025, OpenRemote Inc.
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
import {customElement, property} from "lit/decorators.js";
import {TextArea} from "@vaadin/text-area";
import {OrVaadinComponent} from "./util";
import {type LitElement, type PropertyValues} from "lit";

// _updateHeight is defined in Vaadin's TextAreaMixin (not TextArea itself); the cast exposes it.
@customElement("or-vaadin-text-area")
export class OrVaadinTextArea extends (TextArea as new () => TextArea & LitElement & { _updateHeight(): void }) implements OrVaadinComponent {

    // Show a native vertical resize handle and stop automatic resizing: the height starts at
    // min-rows and stays fixed until the user drags it. Default is Vaadin's autoresize.
    // Touch devices get no usable native handle; the height then simply stays fixed.
    @property({type: Boolean})
    manualresize = false;

    override _onEnter(ev: KeyboardEvent) {
        // Shift+Enter inserts a newline without submitting the value.
        if (!ev.shiftKey) {
            this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        }
        return super._onEnter(ev);
    }

    // Vaadin's default _updateHeight reads scrollHeight on every value change to autoresize.
    // In manualresize mode, keep the height fixed and offer a native resize handle instead.
    override _updateHeight(): void {
        if (!this.inputElement) return;
        const inputField = this.shadowRoot?.querySelector<HTMLElement>('[part~="input-field"]');
        if (this.manualresize) {
            if (inputField) {
                // The container wraps the textarea (auto height, capped by max-rows) so a
                // resize drag grows the whole field without a second scrollbar of its own.
                inputField.style.height = "auto";
                inputField.style.overflowY = "hidden";
            }
            this.inputElement.style.overflowY = "auto";
            // Fixed height with a native vertical handle. Vaadin's own min-rows sets the
            // initial height and max-rows caps it, so no row-to-pixel math is reimplemented.
            // Coarse pointers get no handle (Vaadin's resize: none) as it is unusable on touch.
            if (window.matchMedia("(pointer: coarse)").matches) {
                this.inputElement.style.removeProperty("resize");
            } else {
                this.inputElement.style.resize = "vertical";
            }
            return;
        }
        // Autoresize: drop the manual overrides (no-ops when never set) and keep Vaadin's
        // stock behavior (scrollbar-flicker minimization, scroll-position preservation).
        this.inputElement.style.removeProperty("resize");
        this.inputElement.style.removeProperty("overflow-y");
        if (inputField) inputField.style.removeProperty("overflow-y");
        super._updateHeight();
    }

    protected override updated(changedProperties: PropertyValues): void {
        super.updated(changedProperties);
        // _updateHeight only runs on value changes; re-apply when the mode toggles.
        if (changedProperties.has("manualresize")) {
            this._updateHeight();
        }
    }
}
