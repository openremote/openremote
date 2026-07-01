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
import {OrVaadinComponent} from "./util";
import {css, type LitElement} from "lit";

type WithLit<T> = T & typeof LitElement;

/**
 * A toggle (switch) component representing a binary on/off choice.
 *
 * Vaadin does not ship a native toggle/switch web component, so this extends the
 * Vaadin {@link Checkbox} - inheriting its `checked`, `disabled`, `readonly` and
 * `label` behaviour, form participation, and `change` / `checked-changed` events -
 * and restyles the `checkbox` part into a pill-shaped track with a sliding knob to
 * match the OpenRemote design system.
 *
 * @customElement "or-vaadin-toggle"
 * @fires {Event} change - Fired when the toggle is switched on or off by the user.
 * @fires {CustomEvent} checked-changed - Fired when the `checked` property changes.
 */
@customElement("or-vaadin-toggle")
export class OrVaadinToggle extends (Checkbox as new () => Checkbox & LitElement) implements OrVaadinComponent {

    static get styles() {
        return [
            (Checkbox as WithLit<typeof Checkbox>).styles,
            css`
                :host {
                    --_track-width: 30px;
                    --_track-height: 20px;
                    --_knob-size: 14px;
                    /* Vertical/horizontal inset of the knob within the track */
                    --_knob-inset: 3px;
                    align-items: center;
                    /* Gap between the track and the label */
                    --vaadin-checkbox-gap: 6px;
                }

                /* Turn the square checkbox part into a pill-shaped track */
                [part='checkbox'] {
                    width: var(--_track-width);
                    min-width: var(--_track-width);
                    height: var(--_track-height);
                    border: none;
                    border-radius: var(--lumo-border-radius-l, 12px);
                    background: var(--lumo-contrast-30pct, #c4c8c4);
                    transition: background-color 0.15s ease-in-out;
                }

                :host([checked]) [part='checkbox'] {
                    background: var(--lumo-primary-color, #47a942);
                }

                :host([disabled]) [part='checkbox'] {
                    opacity: 0.5;
                }

                /* The sliding knob (overrides the inherited checkmark) */
                [part='checkbox']::after,
                :host(:not([checked], [indeterminate])) [part='checkbox']::after {
                    content: "";
                    position: absolute;
                    inset: auto;
                    top: 50%;
                    left: var(--_knob-inset);
                    width: var(--_knob-size);
                    height: var(--_knob-size);
                    border-radius: 50%;
                    background: var(--lumo-base-color, #ffffff);
                    mask: none;
                    filter: none;
                    opacity: 1;
                    transform: translateY(-50%);
                    transition: left 0.15s ease-in-out;
                }

                :host([checked]) [part='checkbox']::after,
                :host([indeterminate]) [part='checkbox']::after {
                    left: calc(var(--_track-width) - var(--_knob-size) - var(--_knob-inset));
                }
            `
        ];
    }
}
