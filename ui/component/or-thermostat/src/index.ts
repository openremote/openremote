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

        let callback: (event: AttributeEvent) => void = (event) => {
            console.log("Event Received:" + JSON.stringify(event, null, 2));
            if (event
                && event.ref!.attributeName === "targetTemperature") {
                this.targetTemperature = event.value;
            }
            if (event
                && event.ref!.attributeName === "currentTemperature") {
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
        let oldValue = this._displayedLabel;
        this._displayedLabel = newValue;
        this.requestUpdate("_displayedLabel", oldValue);
    };

    private set currentTemperature (newValue:number) {
        let oldValue = this._currentTemperature;
        this._currentTemperature = newValue;
        this.requestUpdate("_currentTemperature", oldValue);
    };

    private set targetTemperature (newValue:number) {
        let oldValue = this._targetTemperature;
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
