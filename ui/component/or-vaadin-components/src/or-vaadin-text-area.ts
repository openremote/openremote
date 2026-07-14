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
import {customElement} from "lit/decorators.js";
import {TextArea} from "@vaadin/text-area";
import {OrVaadinComponent} from "./util";
import {type LitElement} from "lit";

@customElement("or-vaadin-text-area")
export class OrVaadinTextArea extends (TextArea as new () => TextArea & LitElement) implements OrVaadinComponent {

    override _onEnter(ev: KeyboardEvent) {
        this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        return super._onEnter(ev);
    }

    // Vaadin's default _updateHeight reads inputElement.scrollHeight on every value change,
    // which is an O(content lines) layout reflow — catastrophic for large datasets.
    // When --or-text-area-height is set on the host, use it directly (O(1) CSS variable
    // read, no layout). Without it, fall back to a minimal auto-grow so other text areas
    // still work normally.
    _updateHeight(): void {
        if (!this.inputElement) return;
        const h = window.getComputedStyle(this).getPropertyValue("--or-text-area-height").trim();
        if (h) {
            // Set the same height on both the vaadin-input-container (part="input-field")
            // and the inner textarea. Without this, Lumo's default height on the container
            // is smaller than 'h', the textarea overflows it, and a second scrollbar appears.
            // Inline style always wins over Lumo's CSS.
            const inputField = this.shadowRoot?.querySelector<HTMLElement>('[part~="input-field"]');
            if (inputField) {
                inputField.style.height = h;
                inputField.style.overflowY = "hidden";
                const cs = window.getComputedStyle(inputField);
                const paddingV = (parseFloat(cs.paddingTop) || 0) + (parseFloat(cs.paddingBottom) || 0);
                this.inputElement.style.height = `${parseFloat(h) - paddingV}px`;
            } else {
                this.inputElement.style.height = h;
            }
            this.inputElement.style.overflowY = "auto";
            return;
        }
        // Auto-grow fallback for text areas without a fixed height.
        this.inputElement.style.height = "auto";
        const sh = this.inputElement.scrollHeight;
        if (sh > 0) {
            this.inputElement.style.height = `${sh}px`;
        }
    }
}
