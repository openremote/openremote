/* tslint:disable:no-empty */
import {EventProviderFactory, EventProviderStatus} from "./event";
import {objectsEqual} from "./util";
import {AttributeRef, SharedEvent, EventRequestResponseWrapper} from "@openremote/model";

declare type Constructor<T = {}> = new (...args: any[]) => T;

interface CustomElement {
    connectedCallback?(): void;
    disconnectedCallback?(): void;
    readonly isConnected: boolean;
}

export const subscribe = (eventProviderFactory: EventProviderFactory) => <T extends Constructor<CustomElement>>(base: T) =>

    class extends base {

        public _connectRequested = false;
        public _subscriptionIds?: string[];
        public _assetIds?: string[];
        public _attributeRefs?: AttributeRef[];
        public _status: EventProviderStatus = EventProviderStatus.DISCONNECTED;
        public _statusCallback = (status: EventProviderStatus) => this._onEventProviderStatusChanged(status);

        connectedCallback() {
            if (super.connectedCallback) {
                super.connectedCallback();
            }

            this.connectEvents();
        }

        disconnectedCallback() {
            this.disconnectEvents();

            if (super.disconnectedCallback) {
                super.disconnectedCallback();
            }
        }

        public connectEvents() {
            if (!eventProviderFactory.getEventProvider()) {
                console.log("No event provider available so cannot subscribe");
                return;
            }
            if (this._connectRequested) {
                return;
            }

            this._connectRequested = true;
            eventProviderFactory.getEventProvider()!.subscribeStatusChange(this._statusCallback);
            this._doConnect();
        }

        public disconnectEvents() {
            if (!this._connectRequested) {
                return;
            }
            this._connectRequested = false;
            eventProviderFactory.getEventProvider()!.unsubscribeStatusChange(this._statusCallback);
            this._onEventsDisconnect();
        }

        public async _doConnect() {
            if (!this.eventsConnected) {
                return;
            }

            this._onEventsConnect();
        }

        public get eventsConnected() {
            return this._connectRequested && eventProviderFactory.getEventProvider()!.status === EventProviderStatus.CONNECTED;
        }

        public _onEventProviderStatusChanged(status: EventProviderStatus) {
            switch (status) {
                case EventProviderStatus.DISCONNECTED:
                    this._onEventsDisconnect();
                    break;
                case EventProviderStatus.CONNECTED:
                    this._doConnect();
                    break;
            }
        }

        public _onEventsConnect() {
            this._addEventSubscriptions();
            this.onEventsConnect();
        }

        public _onEventsDisconnect() {
            this._removeEventSubscriptions();
            this.onEventsDisconnect();
        }

        /**
         * Defaults to subscribe to attribute events and optionally asset events, override in subclasses to do
         * custom subscriptions.
         * Returns the subscription IDs of any created subscriptions
         */
        public async _addEventSubscriptions(): Promise<string[]> {
            const isAttributes = !!this._attributeRefs;
            const ids: string[] | AttributeRef[] | undefined = this._attributeRefs ? this._attributeRefs : this._assetIds;
            const subscriptions = [];

            if (ids && ids.length > 0) {
                if (!isAttributes) {
                    subscriptions.push(await eventProviderFactory.getEventProvider()!.subscribeAssetEvents(ids, true, (event) => this._onEvent(event)));
                }
                subscriptions.push(await eventProviderFactory.getEventProvider()!.subscribeAttributeEvents(ids, isAttributes, (event) => this._onEvent(event)));
            }

            return subscriptions;
        }

        public _removeEventSubscriptions() {
            if (!this._subscriptionIds) {
                return;
            }

            this._subscriptionIds.forEach((subscriptionId) => {
                eventProviderFactory.getEventProvider()!.unsubscribe(subscriptionId);
            });

            this._subscriptionIds = undefined;
        }

        public _refreshEventSubscriptions() {
            this._removeEventSubscriptions();
            this._addEventSubscriptions();
        }

        public set assetIds(assetIds: string[] | undefined) {
            if (objectsEqual(this._assetIds, assetIds)) {
                return;
            }

            this._assetIds = assetIds;
            this._refreshEventSubscriptions();
        }

        public get assetIds() {
            return this._assetIds;
        }

        public set attributeRefs(attributes: AttributeRef[] | undefined) {
            if (objectsEqual(this._attributeRefs, attributes)) {
                return;
            }

            this._attributeRefs = attributes;
            this._refreshEventSubscriptions();
        }

        public _sendEvent(event: SharedEvent) {
            eventProviderFactory.getEventProvider()!.sendEvent(event);
        }

        public _sendEventWithReply<U extends SharedEvent, V extends SharedEvent>(event: EventRequestResponseWrapper<U>): Promise<V> {
            return eventProviderFactory.getEventProvider()!.sendEventWithReply(event);
        }

        // noinspection JSUnusedLocalSymbols
        public onEventsConnect() {}

        // noinspection JSUnusedLocalSymbols
        public onEventsDisconnect() {}

        // noinspection JSUnusedLocalSymbols
        public _onEvent(event: SharedEvent) {}
    };
