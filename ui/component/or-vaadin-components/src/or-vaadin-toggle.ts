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
import {customElement} from "lit/decorators.js";
import {Checkbox} from "@vaadin/checkbox";
import {registerStyles, css} from "@vaadin/vaadin-themable-mixin/register-styles.js";
import {OrVaadinComponent} from "./util";
import {type LitElement} from "lit";

/*
 * The toggle reuses the Vaadin `<vaadin-checkbox>` as its base, so it inherits the checkbox's
 * behaviour, form participation, accessibility and `checked` / `readonly` / `disabled` states.
 * Only the visuals differ: the `checkbox` part is restyled into a pill-shaped track with a
 * sliding knob.
 *
 * The styles are applied through Vaadin's `registerStyles()` rather than a `static get styles()`
 * override, because `finalizeStyles()` injects `static` styles BEFORE the (Lumo) theme injector,
 * meaning a themed environment would override them. Styles registered via `registerStyles()` are
 * injected AFTER the theme, so they always take effect - both when the OpenRemote theme is applied
 * and when the component is used standalone (hence every value below has a hard-coded fallback
 * alongside its theme variable).
 *
 * `registerStyles()` targets a component by its `is` tag name and must run before the element is
 * finalized. It is therefore called at module load, and `is` is overridden to `or-vaadin-toggle`
 * (the base class would otherwise report `vaadin-checkbox`, causing these styles to leak onto every
 * checkbox, or not match at all).
 */
registerStyles("or-vaadin-toggle", css`
    :host {
        /* Theme variables with standalone fallbacks */
        --_or-toggle-track-width: 30px;
        --_or-toggle-track-height: 20px;
        --_or-toggle-knob-size: 14px;
        --_or-toggle-knob-inset: 3px;
        --_or-toggle-radius: var(--lumo-border-radius-l, 12px);
        --_or-toggle-track-off-color: var(--lumo-contrast-30pct, #c4c8c4);
        --_or-toggle-track-on-color: var(--lumo-primary-color, #47a942);
        --_or-toggle-knob-color: var(--lumo-base-color, #ffffff);
        --_or-toggle-gap: 6px;

        align-items: center;
        --vaadin-checkbox-gap: var(--_or-toggle-gap);
    }

    /*
     * Turn the square checkbox part into a pill-shaped track. Selectors are deliberately prefixed
     * with :host(...) to out-specify the Vaadin/Lumo base rules (e.g. the base hides the ::after
     * marker with a high-specificity ':host(:not([checked])) [part=checkbox]::after { opacity: 0 }'
     * rule, which would otherwise hide the knob in the off state).
     */
    :host [part='checkbox'] {
        box-sizing: border-box;
        width: var(--_or-toggle-track-width);
        min-width: var(--_or-toggle-track-width);
        height: var(--_or-toggle-track-height);
        border: none;
        border-radius: var(--_or-toggle-radius);
        background: var(--_or-toggle-track-off-color);
        transition: background-color 0.15s ease-in-out;
    }

    :host([checked]) [part='checkbox'],
    :host([indeterminate]) [part='checkbox'] {
        background: var(--_or-toggle-track-on-color);
    }

    :host([disabled]) {
        opacity: 0.5;
    }

    /* Replace the checkmark with a sliding knob */
    :host [part='checkbox']::after {
        content: "";
        position: absolute;
        inset: auto;
        top: 50%;
        width: var(--_or-toggle-knob-size);
        height: var(--_or-toggle-knob-size);
        min-width: 0;
        border-radius: 50%;
        background: var(--_or-toggle-knob-color);
        mask: none;
        -webkit-mask: none;
        filter: none;
        transform: translateY(-50%);
        transition: left 0.15s ease-in-out;
    }

    :host(:not([checked], [indeterminate])) [part='checkbox']::after {
        opacity: 1;
        left: var(--_or-toggle-knob-inset);
    }

    :host([checked]) [part='checkbox']::after,
    :host([indeterminate]) [part='checkbox']::after {
        opacity: 1;
        left: calc(var(--_or-toggle-track-width) - var(--_or-toggle-knob-size) - var(--_or-toggle-knob-inset));
    }

    :host([focus-ring]) [part='checkbox'] {
        outline: 2px solid var(--_or-toggle-track-on-color);
        outline-offset: 2px;
    }
`);

/**
 * A toggle (switch) component representing a binary on/off choice.
 *
 * @customElement "or-vaadin-toggle"
 * @fires {Event} change - Fired when the toggle is switched on or off by the user.
 * @fires {CustomEvent} checked-changed - Fired when the `checked` property changes.
 */
@customElement("or-vaadin-toggle")
export class OrVaadinToggle extends (Checkbox as new () => Checkbox & LitElement) implements OrVaadinComponent {

    static get is() {
        return "or-vaadin-toggle";
    }
}
