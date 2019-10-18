/* tslint:disable:no-empty */
import {EventProviderFactory, EventProviderStatus} from "./event";
import {arraysEqual} from "./util";
import {AssetEvent, AttributeEvent, AttributeRef} from "@openremote/model";

declare type Constructor<T = {}> = new (...args: any[]) => T;

interface CustomElement {
    connectedCallback?(): void;
    disconnectedCallback?(): void;
    readonly isConnected: boolean;
}

export const subscribe = (eventProviderFactory: EventProviderFactory) => <T extends Constructor<CustomElement>>(base: T) =>

    class extends base {

        public _connectRequested = false;
        public _assetSubscriptionId?: string;
        public _attributeSubscriptionId?: string;
        public _assetIds?: string[];
        public _attributeRefs?: AttributeRef[];
        public _statusCallback = (status: EventProviderStatus) => this._onEventProviderStatusChanged(status);

        connectedCallback() {
            if (super.connectedCallback) {
                super.connectedCallback();
            }

            this.connect();
        }

        disconnectedCallback() {
            this.disconnect();

            if (super.disconnectedCallback) {
                super.disconnectedCallback();
            }
        }

        public connect() {
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

        public async _doConnect() {
            if (!this._connectRequested || this._attributeSubscriptionId) {
                return;
            }

            if (eventProviderFactory.getEventProvider()!.status !== EventProviderStatus.CONNECTED) {
                return;
            }

            const isAttributes = !!this._attributeRefs;
            const ids: string[] | AttributeRef[] | undefined = this._attributeRefs ? this._attributeRefs : this._assetIds;

            if (ids && ids.length > 0) {
                if (!isAttributes) {
                    this._assetSubscriptionId = await eventProviderFactory.getEventProvider()!.subscribeAssetEvents(ids, true, (event) => this._onAssetEvent(event));
                }
                this._attributeSubscriptionId = await eventProviderFactory.getEventProvider()!.subscribeAttributeEvents(ids, isAttributes, (event) => this._onAttributeEvent(event));
            }
            this.onStatusChange(EventProviderStatus.CONNECTED);
        }

        public disconnect() {
            if (!this._connectRequested) {
                return;
            }
            this._connectRequested = false;
            eventProviderFactory.getEventProvider()!.unsubscribeStatusChange(this._statusCallback);
            this._doDisconnect();
        }

        public _doDisconnect() {
            if (!this._assetSubscriptionId) {
                return;
            }

            eventProviderFactory.getEventProvider()!.unsubscribe(this._assetSubscriptionId);
            eventProviderFactory.getEventProvider()!.unsubscribe(this._attributeSubscriptionId!);
            this._assetSubscriptionId = undefined;
            this._attributeSubscriptionId = undefined;
            this.onStatusChange(EventProviderStatus.DISCONNECTED);
        }

        public set assetIds(assetIds: string[] | undefined) {
            if (arraysEqual(this._assetIds, assetIds)) {
                return;
            }

            this._assetIds = assetIds;
            this._doDisconnect();
            this._doConnect();
        }

        public set attributeRefs(attributes: AttributeRef[] | undefined) {
            if (arraysEqual(this._attributeRefs, attributes)) {
                return;
            }

            this._attributeRefs = attributes;
            this._doDisconnect();
            this._doConnect();
        }

        public _onEventProviderStatusChanged(status: EventProviderStatus) {
            switch (status) {
                case EventProviderStatus.DISCONNECTED:
                    this._doDisconnect();
                    break;
                case EventProviderStatus.CONNECTED:
                    this._doConnect();
                    break;
            }
        }

        public _onAttributeEvent(event: AttributeEvent) {
            if (!this._connectRequested || !this._attributeSubscriptionId) {
                return;
            }

            this.onAttributeEvent(event);
        }

        public _onAssetEvent(event: AssetEvent) {
            if (!this._connectRequested || !this._assetSubscriptionId) {
                return;
            }

            this.onAssetEvent(event);
        }

        public _sendEvent(event: AttributeEvent) {
            if (eventProviderFactory.getEventProvider()!.status === EventProviderStatus.CONNECTED) {
                eventProviderFactory.getEventProvider()!.sendEvent(event);
            }
        }

        // noinspection JSUnusedLocalSymbols
        public onStatusChange(status: EventProviderStatus) {}

        // noinspection JSUnusedLocalSymbols
        public onAttributeEvent(event: AttributeEvent) {}

        // noinspection JSUnusedLocalSymbols
        public onAssetEvent(event: AssetEvent) {}
    };
