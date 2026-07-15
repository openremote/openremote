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

@customElement("or-vaadin-text-area")
export class OrVaadinTextArea extends (TextArea as new () => TextArea & LitElement & { _updateHeight(): void }) implements OrVaadinComponent {

    // Enables the native resize handle in fixed-height mode (--or-text-area-height).
    // Irrelevant in the default auto-grow mode, where the height always tracks the content.
    @property({type: Boolean})
    resize = true;

    private _fixedHeightApplied = false;

    override _onEnter(ev: KeyboardEvent) {
        // Shift+Enter inserts a newline without submitting the value.
        if (!ev.shiftKey) {
            this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        }
        return super._onEnter(ev);
    }

    // Vaadin's default _updateHeight reads scrollHeight on every value change — O(content lines).
    // When --or-text-area-height is set, size via CSS variable (O(1)) instead.
    override _updateHeight(): void {
        if (!this.inputElement) return;
        const h = window.getComputedStyle(this).getPropertyValue("--or-text-area-height").trim();
        if (h) {
            const inputField = this.shadowRoot?.querySelector<HTMLElement>('[part~="input-field"]');
            if (inputField) {
                // The container wraps the textarea (auto height) so a native resize
                // drag on the textarea grows the whole field, and it never shows a
                // second scrollbar of its own.
                inputField.style.height = "auto";
                inputField.style.overflowY = "hidden";
            }
            // Set the initial height only: a resize drag on the textarea writes this
            // same inline property, so later value changes must not snap it back.
            if (!this._fixedHeightApplied) {
                this.inputElement.style.height = h;
                this._fixedHeightApplied = true;
            }
            this.inputElement.style.overflowY = "auto";
            // Fixed height disables auto-grow, so the textarea offers the native
            // resize handle by default (inline style beats Vaadin's ::slotted()
            // resize: none). min-height keeps drags from shrinking below default.
            // Only an explicit false disables, so undefined behaves like the default.
            if (this.resize !== false) {
                this.inputElement.style.resize = "vertical";
                this.inputElement.style.minHeight = h;
            } else {
                this.inputElement.style.resize = "none";
                this.inputElement.style.minHeight = "";
            }
            return;
        }
        // Without the CSS variable, keep Vaadin's stock auto-grow behavior
        // (scrollbar-flicker minimization, scroll-position preservation) so
        // other or-vaadin-text-area users are unaffected by this override.
        super._updateHeight();
    }

    protected override updated(changedProperties: PropertyValues): void {
        super.updated(changedProperties);
        // _updateHeight only runs on value changes; re-apply when the option toggles.
        if (changedProperties.has("resize")) {
            this._updateHeight();
        }
    }
}
