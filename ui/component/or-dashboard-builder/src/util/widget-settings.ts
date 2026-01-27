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
import {LitElement, PropertyValues, TemplateResult } from "lit";
import { property } from "lit/decorators.js";
import {WidgetConfig} from "./widget-config";
import {style} from "../style";

export class WidgetSettingsChangedEvent extends CustomEvent<WidgetConfig> {

    public static readonly NAME = "settings-changed";

    constructor(widgetConfig: WidgetConfig) {
        super(WidgetSettingsChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: widgetConfig
        });
    }

}

export abstract class WidgetSettings extends LitElement {

    @property()
    protected readonly widgetConfig: WidgetConfig;

    static get styles() {
        return [style];
    }

    protected abstract render(): TemplateResult

    constructor(config: WidgetConfig) {
        super();
        this.widgetConfig = config;
    }


    // Lit lifecycle for "on every update" which triggers on every property/state change
    protected willUpdate(changedProps: PropertyValues) {
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.dispatchEvent(new WidgetSettingsChangedEvent(this.widgetConfig));
        }
    }

    protected notifyConfigUpdate() {
        this.requestUpdate('widgetConfig');
    }


    /* ----------------------------- */

    public getDisplayName?: () => string | undefined;

    public setDisplayName?: (name?: string) => void;

    public getEditMode?: () => boolean;

    public getWidgetLocation?: () => { x?: number, y?: number, h?: number, w?: number }
}
