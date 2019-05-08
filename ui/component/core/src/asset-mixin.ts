/* tslint:disable:no-empty */
import {EventProviderFactory, EventProviderStatus} from "./event";
import {arraysEqual} from "./util";
import {AttributeEvent, AssetEvent, AssetEventCause} from "@openremote/model";

declare type Constructor<T = {}> = new (...args: any[]) => T;

export const subscribe = (eventProviderFactory: EventProviderFactory) => <T extends Constructor>(base: T) => {

    return class extends base {

        public _connectRequested = false;
        public _assetSubscriptionId?: string;
        public _attributeSubscriptionId?: string;
        public _assetIds?: string[];
        public _statusCallback = (status: EventProviderStatus) => this._onEventProviderStatusChanged(status);

        public connectedCallback() {
            // @ts-ignore
            if (super.connectedCallback) {
                // @ts-ignore
                super.connectedCallback();
            }

            this.connect();
        }

        public disconnectedCallback() {
            this.disconnect();

            // @ts-ignore
            if (super.disconnectedCallback) {
                // @ts-ignore
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

            if (!this._assetIds || this._assetIds.length === 0) {
                return;
            }

            if (eventProviderFactory.getEventProvider()!.status !== EventProviderStatus.CONNECTED) {
                return;
            }

            this._assetSubscriptionId = await eventProviderFactory.getEventProvider()!.subscribeAssetEvents(this._assetIds, (event) => this._onAssetEvent(event));
            this._attributeSubscriptionId = await eventProviderFactory.getEventProvider()!.subscribeAttributeEvents(this._assetIds, false, (event) => this._onAttributeEvent(event));
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
        }

        public set assetIds(assetIds: string[] | undefined) {
            if (arraysEqual(this._assetIds, assetIds)) {
                return;
            }

            this._assetIds = assetIds;
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
            if (!this._connectRequested || !this._assetSubscriptionId) {
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

        // noinspection JSUnusedLocalSymbols
        public onAttributeEvent(event: AttributeEvent) {}

        // noinspection JSUnusedLocalSymbols
        public onAssetEvent(event: AssetEvent) {}
    };
};
