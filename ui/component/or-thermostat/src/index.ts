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
import {css, html, LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";
import manager from "@openremote/core";
import {AttributeEvent} from "@openremote/model";

@customElement("or-thermostat")
class OrThermostat extends LitElement {

    @property({type: Boolean})
    private _drawerOpened = false;

    @property({type: String})
    private assetId = undefined;

    @property({type: String})
    private label = undefined;

    private _connected : boolean = false;
    private _subscriptionId : string | undefined = undefined;
    private _displayedLabel : string | undefined = undefined;
    private _currentTemperature : number = 0;
    private _targetTemperature : number = 0;

    async subscribe() {

        if (this._subscriptionId) {
            return;
        }

        const callback: (event: AttributeEvent) => void = (event) => {
            console.log("Event Received:" + JSON.stringify(event, null, 2));
            if (event
                && event.ref!.name === "targetTemperature") {
                this.targetTemperature = event.value;
            }
            if (event
                && event.ref!.name === "currentTemperature") {
                this.currentTemperature = event.value;
            }
        };

        const subscriptionId = await manager.events!.subscribeAttributeEvents([this.assetId!], false, callback);
        this._subscriptionId = subscriptionId;
        console.log("Subscribed: " + subscriptionId);
    };

    unsubscribe() {
        if (this._subscriptionId) {
            manager.events!.unsubscribe(this._subscriptionId);
            this._subscriptionId = undefined;
        }
    }

    static styles = css`
    `;

    private set displayedLabel (newValue:string) {
        const oldValue = this._displayedLabel;
        this._displayedLabel = newValue;
        this.requestUpdate("_displayedLabel", oldValue);
    };

    private set currentTemperature (newValue:number) {
        const oldValue = this._currentTemperature;
        this._currentTemperature = newValue;
        this.requestUpdate("_currentTemperature", oldValue);
    };

    private set targetTemperature (newValue:number) {
        const oldValue = this._targetTemperature;
        this._targetTemperature = newValue;
        this.requestUpdate("_targetTemperature", oldValue);
    };

    connectedCallback() {
        this._connected = true;
        if (this.assetId) {
            this.subscribe();
        }
        super.connectedCallback();
    }

    disconnectedCallback() {
        this._connected = false;
        this.unsubscribe();
        super.disconnectedCallback();
    }

    protected render() {
        return html`
            <p>${this._displayedLabel}</p>
            <p>${this._currentTemperature}</p>
            <p>${this._targetTemperature}</p>
        `;
    }
}
