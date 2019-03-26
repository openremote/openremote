import {css, html, LitElement, property, customElement} from "lit-element";
import openremote from "@openremote/core";
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
            if (event.attributeState
                && event.attributeState.attributeRef!.attributeName === "targetTemperature") {
                this.targetTemperature = event.attributeState!.value;
            }
            if (event.attributeState
                && event.attributeState.attributeRef!.attributeName === "currentTemperature") {
                this.currentTemperature = event.attributeState!.value;
            }
        };

        let subscriptionId = await openremote.events!.subscribeAttributeEvents([this.assetId!], callback);
        this._subscriptionId = subscriptionId;
        console.log("Subscribed: " + subscriptionId);
    };

    unsubscribe() {
        if (this._subscriptionId) {
            openremote.events!.unsubscribe(this._subscriptionId);
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

    private raiseTargetTemperatureByControl () : void {
        let newVal = this._targetTemperature + 1;

        console.log('setting new temp',newVal);

        //use setter to update temp
        this.targetTemperature = newVal;

        //update view
        this._targetTemperature = newVal;
        const event:AttributeEvent = {
            "eventType": "attribute",
            "attributeState": {
                "attributeRef": {
                    "entityId": this.assetId,
                    "attributeName": "targetTemperature"
                },
                "value": newVal
            }
        };
        openremote.events!.sendEvent(event);
    }
    private lowerTargetTemperatureByControl () : void {
        this.targetTemperature = this._targetTemperature - 1;
    }

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
            <p>displayed: ${this._displayedLabel}</p>
            <p>current temp: ${this._currentTemperature}</p>
            <p>target temp:</p>
            <button style="display:inline" @click="${this.lowerTargetTemperatureByControl}">-</button>
            <span>${this._targetTemperature}</span>
            <button style="display:inline" @click="${this.raiseTargetTemperatureByControl}">+</button>
        `;
    }
}
