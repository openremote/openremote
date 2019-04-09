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
                this._targetTemperature = event.attributeState!.value;
                this.requestUpdate("_targetTemperature");
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
        //send event to manager
        const event:AttributeEvent = {
            "eventType": "attribute",
            "attributeState": {
                "attributeRef": {
                    "entityId": this.assetId,
                    "attributeName": "targetTemperature"
                },
                "value": newValue
            }
        };
        openremote.events!.sendEvent(event);
    };

    private raiseTargetTemperatureByControl () : void {
        this.targetTemperature = this._targetTemperature + 1;
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

    static styles = css`
        .wrapper,
        .inner-wrapper {
            align-items: center;
        }
        .wrapper {
            display: flex;
        }
        .inner-wrapper {
            display: flex;
            flex-grow: 1;
            justify-content: center;
        }
        
        .icon,
        .display-temp {
            flex-basis: 50px;
        }
        .btn {
            width: 20px;
        }
        .target-temp {
            padding: 0 20px;
            text-align: center;
        }
        .display-temp {
            text-align: right;
        }
    `;

    protected render() {
        // <p>displayed: ${this._displayedLabel}</p>
        // <p>current temp: ${this._currentTemperature}</p>
        // <p>target temp:</p>
        return html`
            <div class="wrapper">
              <div class="icon">[i]</div>
              <div class="inner-wrapper">
                <button class="btn" style="display:inline" @click="${this.lowerTargetTemperatureByControl}">-</button>
                <span class="target-temp">${this._targetTemperature}&deg;</span>
                <button class="btn" style="display:inline" @click="${this.raiseTargetTemperatureByControl}">+</button>
              </div>
              <div class="display-temp">[tmp&deg;]</div>
            </div>
        `;
    }
}
