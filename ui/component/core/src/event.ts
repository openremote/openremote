import manager from "./index";
import {arrayRemove, Deferred} from "./util";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AssetFilter,
    AssetsEvent,
    AttributeEvent,
    AttributeRef,
    CancelEventSubscription,
    EventRequestResponseWrapper,
    EventSubscription,
    ReadAssetsEvent,
    SharedEvent,
    TriggeredEventSubscription
} from "@openremote/model";

export enum EventProviderStatus {
    DISCONNECTED = "DISCONNECTED",
    CONNECTED = "CONNECTED",
    CONNECTING = "CONNECTING",
    RECONNECT_FAILED = "RECONNECT_FAILED"
}

export interface EventProvider {
    status: EventProviderStatus;

    connect(): Promise<boolean>;

    disconnect(): void;

    subscribeStatusChange(callback: (status: EventProviderStatus) => void): void;

    unsubscribeStatusChange(callback: (status: EventProviderStatus) => void): void;

    subscribe<T extends SharedEvent>(eventSubscription: EventSubscription<T>, callback: (event: T) => void): Promise<string>;

    unsubscribe<T extends SharedEvent>(subscriptionId: string): void;

    /**
     * Subscribe to {@link AssetEvent}s for the specified {@link Asset}s for all {@link Asset}s
     */
    subscribeAssetEvents(ids: string[] | AttributeRef[] | undefined, requestCurrentValues: boolean, callback: (event: AssetEvent) => void): Promise<string>;

    /**
     * Subscribe to {@link AttributeEvent}s for the specified {@link Asset}s and if {@link AttributeRef}s are provided
     * then the attribute names from the refs will be used for each asset ID.
     */
    subscribeAttributeEvents(ids: string[] | AttributeRef[], requestCurrentValues: boolean, callback: (event: AttributeEvent) => void): Promise<string>;

    sendEvent<T extends SharedEvent>(event: T): void;

    sendEventWithReply<T extends SharedEvent, U extends SharedEvent>(event: EventRequestResponseWrapper<T>): Promise<U>;
}

// Interface to provide a singleton implementation of EventProvider
export interface EventProviderFactory {
    getEventProvider(): EventProvider | undefined;
}

interface EventSubscriptionInfo<T extends SharedEvent> {
    eventSubscription: EventSubscription<T>;
    callback: (event: T) => void;
    deferred: Deferred<string> | null;
}

interface AssetSubscriptionInfo {
    callbacks: Map<string, (attributeEvent: AttributeEvent) => void>;
    asset?: Asset;
    promise?: Promise<void>;
}

const SUBSCRIBE_MESSAGE_PREFIX = "SUBSCRIBE:";
const SUBSCRIBED_MESSAGE_PREFIX = "SUBSCRIBED:";
const UNSUBSCRIBE_MESSAGE_PREFIX = "UNSUBSCRIBE:";
const UNAUTHORIZED_MESSAGE_PREFIX = "UNAUTHORIZED:";
const TRIGGERED_MESSAGE_PREFIX = "TRIGGERED:";
const EVENT_MESSAGE_PREFIX = "EVENT:";
const EVENT_REQUEST_RESPONSE_MESSAGE_PREFIX = "REQUESTRESPONSE:";

abstract class EventProviderImpl implements EventProvider {

    protected static MIN_RECONNECT_DELAY: number = 0;
    protected static MAX_RECONNECT_DELAY: number = 30000;
    protected _disconnectRequested: boolean = false;
    protected _reconnectDelayMillis: number = WebSocketEventProvider.MIN_RECONNECT_DELAY;
    protected _reconnectTimer: number | null = null;
    protected _status: EventProviderStatus = EventProviderStatus.DISCONNECTED;
    protected _connectingDeferred: Deferred<boolean> | null = null;
    protected _statusCallbacks: Array<(status: EventProviderStatus) => void> = [];
    protected _pendingSubscription: EventSubscriptionInfo<SharedEvent> | null = null;
    protected _queuedSubscriptions: EventSubscriptionInfo<SharedEvent>[] = [];
    protected _subscriptionMap: Map<string, EventSubscriptionInfo<SharedEvent>> = new Map();
    protected _assetEventPromise?: Promise<string>;
    protected _assetEventCallbackMap: Map<string, (e: AssetEvent) => void> = new Map();
    protected _attributeEventPromise?: Promise<string>;
    protected _attributeEventCallbackMap: Map<string, (e: AttributeEvent) => void> = new Map();
    protected _unloading = false;
    protected static _subscriptionCounter: number = 0;

    abstract get endpointUrl(): string;

    public get status(): EventProviderStatus {
        return this._status;
    }

    public subscribeStatusChange(callback: (status: EventProviderStatus) => void): void {
        this._statusCallbacks.push(callback);
    }

    public unsubscribeStatusChange(callback: (status: EventProviderStatus) => void): void {
        arrayRemove(this._statusCallbacks, callback);
    }

    public connect(): Promise<boolean> {
        if (this._status === EventProviderStatus.CONNECTED) {
            return Promise.resolve(true);
        }

        this._disconnectRequested = false;

        if (this._connectingDeferred) {
            return this._connectingDeferred.promise;
        }

        this._onStatusChanged(EventProviderStatus.CONNECTING);

        this._connectingDeferred = new Deferred();

        this._doConnect().then((connected: boolean) => {
            if (this._connectingDeferred) {
                const deferred = this._connectingDeferred;
                this._connectingDeferred = null;

                if (this._reconnectTimer) {
                    window.clearTimeout(this._reconnectTimer);
                    this._reconnectTimer = null;
                }

                if (connected) {
                    console.debug("Connected to event service: " + this.endpointUrl);
                    this._reconnectDelayMillis = WebSocketEventProvider.MIN_RECONNECT_DELAY;
                    this._onStatusChanged(EventProviderStatus.CONNECTED);

                    window.setTimeout(() => {
                        this._onConnect();
                    }, 0);
                } else {
                    console.debug("Failed to connect to event service: " + this.endpointUrl);
                    this._onStatusChanged(EventProviderStatus.DISCONNECTED);
                }

                deferred.resolve(connected);
            }
        });

        return this._connectingDeferred.promise;
    }

    public disconnect(): void {
        console.debug("Disconnecting from event service: " + this.endpointUrl);
        if (this._disconnectRequested) {
            return;
        }
        this._disconnectRequested = true;

        if (this._reconnectTimer) {
            window.clearTimeout(this._reconnectTimer);
            this._reconnectTimer = null;
        }

        if (this.status === EventProviderStatus.DISCONNECTED) {
            return;
        }

        this._doDisconnect();
    }

    public subscribe<T extends SharedEvent>(eventSubscription: EventSubscription<T>, callback: (event: T) => void): Promise<string> {

        const subscriptionInfo: EventSubscriptionInfo<SharedEvent> = {
            eventSubscription: eventSubscription,
            callback: callback as ((event: SharedEvent) => void),
            deferred: new Deferred<string>()
        };

        if (this._pendingSubscription != null || this._status !== EventProviderStatus.CONNECTED) {
            this._queuedSubscriptions.push(subscriptionInfo);
            return subscriptionInfo.deferred!.promise;
        }

        this._pendingSubscription = subscriptionInfo;

        this._doSubscribe(eventSubscription).then(
            subscriptionId => {
                if (this._pendingSubscription) {
                    const subscription = this._pendingSubscription;
                    this._pendingSubscription = null;

                    // Store subscriptionId and callback
                    this._subscriptionMap.set(subscriptionId, subscription);

                    this._processNextSubscription();
                    const deferred = subscription.deferred;
                    subscription.deferred = null;

                    if (deferred) {
                        deferred.resolve(subscriptionId);
                    }
                }
            },
            (reason) => {
                if (this._pendingSubscription) {
                    const subscription = this._pendingSubscription;
                    this._pendingSubscription = null;
                    this._processNextSubscription();
                    const deferred = subscription.deferred;
                    subscription.deferred = null;

                    if (deferred) {
                        deferred.reject(reason);
                    }
                }
            });

        return this._pendingSubscription.deferred!.promise;
    }

    public async unsubscribe<T extends SharedEvent>(subscriptionId: string): Promise<void> {

        if (this._subscriptionMap.delete(subscriptionId)) {
            this._doUnsubscribe(subscriptionId);
            return;
        }

        if (this._assetEventCallbackMap.delete(subscriptionId)) {
            if (this._assetEventPromise && this._assetEventCallbackMap.size === 0) {
                // Remove the asset subscription
                const assetEventSubscriptionId = await this._assetEventPromise;
                this._assetEventPromise = undefined;
                this.unsubscribe(assetEventSubscriptionId);
            }
            return;
        }

        if (this._attributeEventCallbackMap.delete(subscriptionId)) {
            if (this._attributeEventPromise && this._attributeEventCallbackMap.size === 0) {
                // Remove the attribute subscription
                const attributeEventSubscriptionId = await this._attributeEventPromise;
                this._attributeEventPromise = undefined;
                this.unsubscribe(attributeEventSubscriptionId);
            }
            return;
        }
    }

    public sendEvent<T extends SharedEvent>(event: T): void {

        if (this._status === EventProviderStatus.CONNECTED) {
            this._doSend(event);
        }
    }

    public sendEventWithReply<T extends SharedEvent, U extends SharedEvent>(event: EventRequestResponseWrapper<T>): Promise<U> {
        if (this._status !== EventProviderStatus.CONNECTED) {
            return Promise.reject("Not connected");
        }
        return this._doSendWithReply(event);
    }

    public async subscribeAssetEvents(ids: string[] | AttributeRef[] | undefined, requestCurrentValues: boolean, callback: (event: AssetEvent) => void): Promise<string> {

        const isAttributeRef = ids && typeof ids[0] !== "string";
        const assetIds = isAttributeRef ? (ids as AttributeRef[]).map((ref) => ref.id!) : ids as string[] | undefined;
        const subscriptionId = "AssetEvent" + EventProviderImpl._subscriptionCounter++;

        // If not already done then create a single global subscription for asset events and filter for each callback
        if (!this._assetEventPromise) {

            let assetFilter: AssetFilter | undefined;

            if (!manager.authenticated) {
                // Need to set the filter realm when anonymous
                assetFilter = {
                    filterType: "asset",
                    realm: manager.displayRealm
                }
            }

            const subscription: EventSubscription<AssetEvent> = {
                eventType: "asset",
                filter: assetFilter
            };
            // if (assetIds) {
            //     subscription.filter = {
            //         filterType: "asset",
            //         assetIds: assetIds
            //     };
            // }
            this._assetEventPromise = this.subscribe(subscription, (event) => {
                this._assetEventCallbackMap.forEach((callback) => callback(event));
            });
        }

        const eventFilter = (e: AssetEvent) => {
            const assetId = e.asset!.id!;

            if (assetIds) {
                if (assetIds.find((id => assetId === id))) {
                    callback(e);
                }
            } else {
                const realm = e.asset!.realm!;
                if (realm === manager.displayRealm) {
                    callback(e);
                }
            }
        };

        this._assetEventCallbackMap.set(subscriptionId, eventFilter);

        return this._assetEventPromise.then(() => {

            try {
                // Get the current state of the assets if requested
                if (assetIds && requestCurrentValues) {
                    const readRequest: EventRequestResponseWrapper<ReadAssetsEvent> = {
                        messageId: "read-assets:" + assetIds.join(",") + ":" + subscriptionId,
                        event: {
                            eventType: "read-assets",
                            assetQuery: {
                                ids: assetIds
                            }
                        }
                    }
                    this.sendEventWithReply(readRequest)
                        .then((e: SharedEvent) => {
                            const assetsEvent = e as AssetsEvent;
                            // Check subscription still exists
                            if (!assetsEvent.assets || !this._assetEventCallbackMap.has(subscriptionId)) {
                                return;
                            }

                            assetsEvent.assets.forEach((asset) => {
                                const assetEvent: AssetEvent = {
                                    eventType: "asset",
                                    asset: asset,
                                    cause: AssetEventCause.READ
                                };
                                callback(assetEvent);
                            });
                        });
                }
            } catch (e) {
                console.error("Failed to subscribe to asset events for assets: " + ids);
                if (subscriptionId) {
                    this.unsubscribe(subscriptionId);
                }
                throw e;
            }

            return subscriptionId;
        });
    }

    public async subscribeAttributeEvents(ids: string[] | AttributeRef[] | undefined, requestCurrentValues: boolean, callback: (event: AttributeEvent) => void): Promise<string> {

        const isAttributeRef = ids && typeof ids[0] !== "string";
        const assetIds = isAttributeRef ? (ids as AttributeRef[]).map((ref) => ref.id!) : ids as string[] | undefined;
        const attributes = isAttributeRef ? ids as AttributeRef[] : undefined;
        const subscriptionId = "AttributeEvent" + EventProviderImpl._subscriptionCounter++;

        // If not already done then create a single global subscription for attribute events and filter for each callback
        if (!this._attributeEventPromise) {

            let assetFilter: AssetFilter | undefined;

            if (!manager.authenticated) {
                // Need to set the filter realm when anonymous
                assetFilter = {
                    filterType: "asset",
                    realm: manager.displayRealm
                }
            }

            const subscription: EventSubscription<AttributeEvent> = {
                eventType: "attribute",
                filter: assetFilter
            };
            this._attributeEventPromise = this.subscribe(subscription, (event) => {
                this._attributeEventCallbackMap.forEach((callback) => callback(event));
            });
        }

        // Build a filter to only respond to the callback for the requested attributes
        const eventFilter = (e: AttributeEvent) => {
            const eventRef = e.ref!;

            if (isAttributeRef) {
                (ids as AttributeRef[]).forEach((ref: AttributeRef) => {
                    if (eventRef.id === ref.id && eventRef.name === ref.name) {
                        callback(e);
                    }
                });
            } else if (assetIds) {
                if (assetIds.find((id => eventRef.id === id))) {
                    callback(e);
                }
            } else {
                const realm = e.realm!;
                if (realm === manager.displayRealm) {
                    callback(e);
                }
            }
        };

        this._attributeEventCallbackMap.set(subscriptionId, eventFilter);

        return this._attributeEventPromise.then(() => {

            try {
                // Get the current state of the attributes if requested
                if (requestCurrentValues && assetIds) {
                    // Just request the whole asset(s) and let the event filter do the work
                    const readRequest: EventRequestResponseWrapper<ReadAssetsEvent> = {
                        messageId: "read-assets:" + assetIds.join(",") + ":" + subscriptionId,
                        event: {
                            eventType: "read-assets",
                            assetQuery: {
                                ids: assetIds
                            }
                        }
                    }
                    this.sendEventWithReply(readRequest)
                        .then((e: SharedEvent) => {
                            const assetsEvent = e as AssetsEvent;
                            // Check subscription still exists
                            if (!assetsEvent.assets || !this._attributeEventCallbackMap.has(subscriptionId)) {
                                return;
                            }

                            assetsEvent.assets.forEach((asset) => {
                                if (asset.attributes) {
                                    Object.entries(asset.attributes!).forEach(([attributeName, attr]) => {
                                        if (!attributes || attributes.find((ref) => ref.id === asset!.id && ref.name === attributeName)) {
                                            eventFilter({
                                                eventType: "attribute",
                                                timestamp: attr.timestamp,
                                                value: attr.value,
                                                ref: {
                                                    id: asset.id,
                                                    name: attributeName
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        });
                }
            } catch (e) {
                console.error("Failed to subscribe to asset events for assets: " + ids);
                if (subscriptionId) {
                    this.unsubscribe(subscriptionId);
                }
                throw e;
            }

            return subscriptionId;
        });
    }

    protected _processNextSubscription() {
        if (this._status !== EventProviderStatus.CONNECTED || this._queuedSubscriptions.length === 0) {
            return;
        }

        setTimeout(() => {
            const subscriptionInfo = this._queuedSubscriptions.shift();
            if (subscriptionInfo) {
                this.subscribe(subscriptionInfo.eventSubscription, subscriptionInfo.callback)
                    .then(
                        (id: string) => {
                            const deferred = subscriptionInfo!.deferred;
                            subscriptionInfo!.deferred = null;

                            if (deferred) {
                                deferred.resolve(id);
                            }
                        }, reason => {
                            const deferred = subscriptionInfo!.deferred;
                            subscriptionInfo!.deferred = null;

                            if (deferred) {
                                deferred.reject(reason);
                            }
                        });
            }
        }, 0);
    }

    private _onStatusChanged(status: EventProviderStatus) {
        if (status === this._status) {
            return;
        }

        console.debug("Event provider status changed: " + status);

        this._status = status;
        if(!this._unloading) {
            window.setTimeout(() => {
                this._statusCallbacks.forEach((cb) => cb(status));
            }, 0);
        }
    }

    protected _onMessageReceived(subscriptionId: string, event: SharedEvent) {
        const subscriptionInfo = this._subscriptionMap.get(subscriptionId);

        if (subscriptionInfo) {
            subscriptionInfo.callback(event);
        }
    }

    protected _onConnect() {
        if (Object.keys(this._subscriptionMap).length > 0) {
            for (const subscriptionId in this._subscriptionMap) {
                if (this._subscriptionMap.has(subscriptionId)) {
                    this._queuedSubscriptions.unshift(this._subscriptionMap.get(subscriptionId)!);
                }
            }
            this._subscriptionMap.clear();
        }

        this._processNextSubscription();
    }

    protected _onDisconnect() {
        this._onStatusChanged(EventProviderStatus.DISCONNECTED);
        if (this._pendingSubscription) {
            this._queuedSubscriptions.unshift(this._pendingSubscription);
            this._pendingSubscription = null;
        }

        this._scheduleReconnect();
    }

    protected _scheduleReconnect() {
        if (this._reconnectTimer) {
            return;
        }

        if (this._disconnectRequested) {
            return;
        }

        console.debug("Event provider scheduling reconnect in " + this._reconnectDelayMillis + "ms");

        this._reconnectTimer = window.setTimeout(() => {

            if (this._disconnectRequested) {
                return;
            }

            this.connect().then(connected => {
                if (!connected) {
                    this._onStatusChanged(EventProviderStatus.RECONNECT_FAILED);
                    this._scheduleReconnect();
                }
            }).catch(error => {
                this._onStatusChanged(EventProviderStatus.RECONNECT_FAILED);
                this._scheduleReconnect();
            });
        }, this._reconnectDelayMillis);

        if (this._reconnectDelayMillis < WebSocketEventProvider.MAX_RECONNECT_DELAY) {
            this._reconnectDelayMillis = Math.min(WebSocketEventProvider.MAX_RECONNECT_DELAY, this._reconnectDelayMillis + 3000);
        }
    }

    protected abstract _doSend<T extends SharedEvent>(event: T): void;

    protected abstract _doSendWithReply<T extends SharedEvent, U extends SharedEvent>(event: EventRequestResponseWrapper<T>): Promise<U>;

    protected abstract _doConnect(): Promise<boolean>;

    protected abstract _doDisconnect(): void;

    protected abstract _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<string>;

    protected abstract _doUnsubscribe(subscriptionId: string): void;
}

export class WebSocketEventProvider extends EventProviderImpl {

    protected static _subscriptionCounter: number = 1;

    private readonly _endpointUrl: string;
    protected _webSocket: WebSocket | undefined = undefined;
    protected _connectDeferred: Deferred<boolean> | null = null;
    protected _subscribeDeferred: Deferred<string> | null = null;
    protected _repliesDeferred: Map<string, Deferred<SharedEvent>> = new Map<string, Deferred<SharedEvent>>();

    get endpointUrl(): string {
        return this._endpointUrl;
    }

    constructor(managerUrl: string) {
        super();

        this._endpointUrl = (managerUrl.startsWith("https:") ? "wss" : "ws") + "://" + managerUrl.substr(managerUrl.indexOf("://") + 3) + "/websocket/events";

        // Close socket on unload/refresh of page
        window.addEventListener("beforeunload", () => {
            this._unloading = true;
            this.disconnect();
        });
    }

    protected _doConnect(): Promise<boolean> {
        let authorisedUrl = this._endpointUrl + "?Realm=" + manager.config.realm;

        if (manager.authenticated) {
            authorisedUrl += "&Authorization=" + manager.getAuthorizationHeader();
        }

        this._webSocket = new WebSocket(authorisedUrl);
        this._connectDeferred = new Deferred();

        if(manager.isTokenExpired()) {
            this._connectDeferred.resolve(false);
        }

        this._webSocket!.onopen = () => {
            if (this._connectDeferred) {
                const deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(true);
            }
        };

        this._webSocket!.onerror = (err) => {
            if (this._connectDeferred) {
                const deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(false);
            } else {
                console.debug("Event provider error");
                // Could have inconsistent state so disconnect and let consumers decide what to do and when to reconnect
                this._beforeDisconnect();
            }
        };

        this._webSocket!.onclose = () => {
            this._webSocket = undefined;

            if (this._connectDeferred) {
                const deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(false);
            } else {
                this._beforeDisconnect();
            }
        };

        this._webSocket!.onmessage = (e) => {
            const msg = e.data as string;

            if (msg && msg.startsWith(SUBSCRIBED_MESSAGE_PREFIX)) {
                const jsonStr = msg.substring(SUBSCRIBED_MESSAGE_PREFIX.length);
                const subscription = JSON.parse(jsonStr) as EventSubscription<SharedEvent>;
                const deferred = this._subscribeDeferred;
                this._subscribeDeferred = null;
                if (deferred) {
                    deferred.resolve(subscription.subscriptionId!);
                }
            } else if (msg.startsWith(UNAUTHORIZED_MESSAGE_PREFIX)) {
                const jsonStr = msg.substring(UNAUTHORIZED_MESSAGE_PREFIX.length);
                const subscription = JSON.parse(jsonStr) as EventSubscription<SharedEvent>;
                const deferred = this._subscribeDeferred;
                this._subscribeDeferred = null;
                if (deferred) {
                    console.warn("Unauthorized event subscription: " + JSON.stringify(subscription, null, 2));
                    deferred.reject("Unauthorized");
                }
            } else if (msg.startsWith(TRIGGERED_MESSAGE_PREFIX)) {
                const str = msg.substring(TRIGGERED_MESSAGE_PREFIX.length);
                const triggered = JSON.parse(str) as TriggeredEventSubscription<SharedEvent>;
                if (triggered.events) {
                    triggered.events.forEach((event) => {
                        this._onMessageReceived(triggered.subscriptionId!, event);
                    });
                }
            } else if (msg.startsWith(EVENT_REQUEST_RESPONSE_MESSAGE_PREFIX)) {
                const str = msg.substring(EVENT_REQUEST_RESPONSE_MESSAGE_PREFIX.length);
                const event = JSON.parse(str) as EventRequestResponseWrapper<SharedEvent>;
                if (event.messageId && event.event) {
                    const deferred = this._repliesDeferred.get(event.messageId!);
                    this._repliesDeferred.delete(event.messageId!);
                    if (deferred) {
                        deferred.resolve(event.event);
                    }
                }
            }
        };

        return this._connectDeferred.promise;
    }

    protected _beforeDisconnect(): void {
        this._onDisconnect();
    }

    protected _doDisconnect(): void {
        this._webSocket?.close();
        this._subscribeDeferred = null;
        this._repliesDeferred.clear();
    }

    protected _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<string> {
        if (!this._webSocket) {
            return Promise.reject("Not connected");
        }

        if (this._subscribeDeferred) {
            return Promise.reject("There's already a pending subscription");
        }

        this._subscribeDeferred = new Deferred();
        if (!subscription.subscriptionId) {
            subscription.subscriptionId = WebSocketEventProvider._subscriptionCounter++ + "";
        }
        this._webSocket.send(SUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(subscription));
        return this._subscribeDeferred.promise;
    }

    protected _doUnsubscribe(subscriptionId: string): void {
        if (!this._webSocket) {
            return;
        }
        const cancelSubscription: CancelEventSubscription = {
            subscriptionId: subscriptionId
        };
        this._webSocket.send(UNSUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(cancelSubscription));
    }

    protected _doSend<T extends SharedEvent>(event: T): void {
        const message = EVENT_MESSAGE_PREFIX + JSON.stringify(event);
        this._webSocket!.send(message);
    }

    protected _doSendWithReply<T extends SharedEvent, U extends SharedEvent>(event: EventRequestResponseWrapper<T>): Promise<U> {

        if (!event.messageId) {
            event.messageId = (new Date().getTime() + (Math.random() * 10)).toString(10);
        }

        if (this._repliesDeferred.has(event.messageId)) {
            return Promise.reject("There's already a pending send and reply with this ID");
        }

        const deferred = new Deferred<SharedEvent>();
        this._repliesDeferred.set(event.messageId, deferred);
        this._webSocket!.send(EVENT_REQUEST_RESPONSE_MESSAGE_PREFIX + JSON.stringify(event));
        return (deferred as unknown as Deferred<U>).promise;
    }
}
