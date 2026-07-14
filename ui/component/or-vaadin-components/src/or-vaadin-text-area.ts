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

    // Vaadin's default _updateHeight reads scrollHeight on every value change — O(content lines).
    // When --or-text-area-height is set, size via CSS variable (O(1)) instead.
    _updateHeight(): void {
        if (!this.inputElement) return;
        const h = window.getComputedStyle(this).getPropertyValue("--or-text-area-height").trim();
        if (h) {
            // Expand the vaadin-input-container to the target height and suppress its scrollbar.
            // Lumo's default container height is smaller than h, which would cause a second scrollbar.
            const inputField = this.shadowRoot?.querySelector<HTMLElement>('[part~="input-field"]');
            if (inputField) {
                inputField.style.height = h;
                inputField.style.overflowY = "hidden";
            }
            // height:100% resolves to the container's content box (h minus its padding),
            // so the textarea fits exactly without a separate padding calculation.
            this.inputElement.style.height = "100%";
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
