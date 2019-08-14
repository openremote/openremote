import openremote from "./index";
import {arrayRemove, Deferred} from "./util";
import {
    AttributeEvent,
    CancelEventSubscription,
    EventSubscription,
    RenewEventSubscriptions,
    SharedEvent,
    ReadAssetAttributesEvent,
    AssetEvent,
    ReadAssetEvent,
    TriggeredEventSubscription
} from "@openremote/model";

export enum EventProviderStatus {
    DISCONNECTED = "DISCONNECTED",
    CONNECTED = "CONNECTED",
    CONNECTING = "CONNECTING"
}

export interface EventProvider {
    status: EventProviderStatus;

    connect(): Promise<boolean>;

    disconnect(): void;

    subscribeStatusChange(callback: (status: EventProviderStatus) => void): void;

    unsubscribeStatusChange(callback: (status: EventProviderStatus) => void): void;

    subscribe<T extends SharedEvent>(eventSubscription: EventSubscription<T>, callback: (event: T) => void): Promise<string>;

    unsubscribe<T extends SharedEvent>(subscriptionId: string): void;

    subscribeAssetEvents(assetIds: string[] | null, callback: (event: AssetEvent) => void): Promise<string>;

    subscribeAttributeEvents(assetIds: string[], requestCurrentValues: boolean, callback: (event: AttributeEvent) => void): Promise<string>;

    sendEvent<T extends SharedEvent>(event: T): void;
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

const SUBSCRIBE_MESSAGE_PREFIX = "SUBSCRIBE:";
const SUBSCRIBED_MESSAGE_PREFIX = "SUBSCRIBED:";
const UNSUBSCRIBE_MESSAGE_PREFIX = "UNSUBSCRIBE:";
const RENEW_MESSAGE_PREFIX = "RENEW:";
const UNAUTHORIZED_MESSAGE_PREFIX = "UNAUTHORIZED:";
const EVENT_MESSAGE_PREFIX = "EVENT:";

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
    protected _subscriptionMap: { [id: string]: EventSubscriptionInfo<SharedEvent> } = {};

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
                let deferred = this._connectingDeferred;
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

        let subscriptionInfo = {
            eventSubscription: eventSubscription,
            callback: callback as ((event: SharedEvent) => void),
            deferred: new Deferred<string>()
        };

        if (this._pendingSubscription != null || this._status !== EventProviderStatus.CONNECTED) {
            this._queuedSubscriptions.push(subscriptionInfo);
            return subscriptionInfo.deferred.promise;
        }

        this._pendingSubscription = subscriptionInfo;

        this._doSubscribe(eventSubscription).then(
            subscriptionId => {
                if (this._pendingSubscription) {
                    let subscriptionInfo = this._pendingSubscription;
                    this._pendingSubscription = null;

                    // Store subscriptionId and callback
                    this._subscriptionMap[subscriptionId] = subscriptionInfo;

                    this._processNextSubscription();
                    let deferred = subscriptionInfo.deferred;
                    subscriptionInfo.deferred = null;

                    if (deferred) {
                        deferred.resolve(subscriptionId);
                    }
                }
            },
            reason => {
                if (this._pendingSubscription) {
                    let subscriptionInfo = this._pendingSubscription;
                    this._pendingSubscription = null;
                    this._processNextSubscription();
                    let deferred = subscriptionInfo.deferred;
                    subscriptionInfo.deferred = null;

                    if (deferred) {
                        deferred.reject(reason);
                    }
                }
            });

        return this._pendingSubscription.deferred!.promise;
    }

    public unsubscribe<T extends SharedEvent>(subscriptionId: string) {

        let callback = this._subscriptionMap[subscriptionId];
        if (callback) {
            delete this._subscriptionMap[subscriptionId];
            this._doUnsubscribe(subscriptionId);
        }
    }

    public sendEvent<T extends SharedEvent>(event: T): void {
        this._doSend(event);
    }

    public async subscribeAssetEvents(assetIds: string[] | null, callback: (event: AssetEvent) => void): Promise<string> {
        let subscription: EventSubscription<AssetEvent> = {
            eventType: "asset"
        };

        if (assetIds && assetIds.length > 0) {
            subscription.filter = {
                filterType: "asset-id",
                assetIds: assetIds
            };
        }

        let subscriptionId: string | null = null;

        try {
            subscriptionId = await this.subscribe(subscription, callback);

            // Get the current state of each asset
            if (assetIds) {
                assetIds.forEach(assetId => {
                    let readEvent: ReadAssetEvent = {
                        assetId: assetId,
                        eventType: "read-asset",
                        subscriptionId: subscriptionId!
                    };
                    this.sendEvent(readEvent);
                });
            }
        } catch (e) {
            console.error("Failed to subscribe to asset events for assets: " + assetIds);
            if (subscriptionId) {
                this.unsubscribe(subscriptionId);
            }
            throw e;
        }

        return subscriptionId!;
    }

    public async subscribeAttributeEvents(assetIds: string[], requestCurrentValues: boolean, callback: (event: AttributeEvent) => void): Promise<string> {
        let subscription: EventSubscription<AttributeEvent> = {
            eventType: "attribute",
            filter: {
                filterType: "asset-id",
                assetIds: assetIds
            }
        };

        let subscriptionId: string | null = null;

        try {
            subscriptionId = await this.subscribe(subscription, callback);

            // Get the current value of each assets attributes
            if (requestCurrentValues) {
                assetIds.forEach(assetId => {
                    let readEvent: ReadAssetAttributesEvent = {
                        assetId: assetId,
                        eventType: "read-asset-attributes",
                        subscriptionId: subscriptionId!
                    };
                    this.sendEvent(readEvent);
                });
            }
        } catch (e) {
            console.error("Failed to subscribe to attribute events for assets: " + assetIds);
            if (subscriptionId) {
                this.unsubscribe(subscriptionId);
            }
            throw e;
        }

        return subscriptionId!;
    }

    protected _processNextSubscription() {
        if (this._status !== EventProviderStatus.CONNECTED || this._queuedSubscriptions.length === 0) {
            return;
        }

        setTimeout(() => {
            let subscriptionInfo = this._queuedSubscriptions.shift();
            if (subscriptionInfo) {
                this.subscribe(subscriptionInfo.eventSubscription, subscriptionInfo.callback)
                    .then(
                        (id: string) => {
                            let deferred = subscriptionInfo!.deferred;
                            subscriptionInfo!.deferred = null;

                            if (deferred) {
                                deferred.resolve(id);
                            }
                        }, reason => {
                            let deferred = subscriptionInfo!.deferred;
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
        window.setTimeout(() => {
            this._statusCallbacks.forEach((cb) => cb(status));
        }, 0);
    }

    protected _onMessageReceived(subscriptionId: string, event: SharedEvent) {
        let subscriptionInfo = this._subscriptionMap[subscriptionId];

        if (subscriptionInfo) {
            subscriptionInfo.callback(event);
        }
    }

    protected _onConnect() {
        console.debug("Event provider connected: " + this.constructor.name);

        if (Object.keys(this._subscriptionMap).length > 0) {
            for (const subscriptionId in this._subscriptionMap) {
                if (this._subscriptionMap.hasOwnProperty(subscriptionId)) {
                    this._queuedSubscriptions.unshift(this._subscriptionMap[subscriptionId]);
                }
            }
            this._subscriptionMap = {};
        }

        this._processNextSubscription();
    }

    protected _onDisconnect() {
        console.debug("Event provider disconnected");
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
                    this._scheduleReconnect();
                }
            }).catch(error => {
                this._scheduleReconnect();
            });
        }, this._reconnectDelayMillis);

        if (this._reconnectDelayMillis < WebSocketEventProvider.MAX_RECONNECT_DELAY) {
            this._reconnectDelayMillis = Math.min(WebSocketEventProvider.MAX_RECONNECT_DELAY, this._reconnectDelayMillis + 3000);
        }
    }

    protected abstract _doSend<T extends SharedEvent>(event: T): void;

    protected abstract _doConnect(): Promise<boolean>;

    protected abstract _doDisconnect(): void;

    protected abstract _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<string>;

    protected abstract _doUnsubscribe(subscriptionId: string): void;
}

export class WebSocketEventProvider extends EventProviderImpl {

    protected static _subscriptionCounter: number = 1;
    protected static _subscriptionRenewalMillis = 150 * 1000;

    private readonly _endpointUrl: string;
    protected _webSocket: WebSocket | undefined = undefined;
    protected _connectDeferred: Deferred<boolean> | null = null;
    protected _subscribeDeferred: Deferred<string> | null = null;
    protected _renewalTimer: number | null = null;

    get endpointUrl(): string {
        return this._endpointUrl;
    }

    constructor(managerUrl: string) {
        super();

        this._endpointUrl = (managerUrl.startsWith("https:") ? "wss" : "ws") + "://" + managerUrl.substr(managerUrl.indexOf("://") + 3) + "/websocket/events";

        // Close socket on unload/refresh of page
        window.addEventListener("beforeunload", () => {
            this.disconnect();
        });
    }

    protected _doConnect(): Promise<boolean> {
        let authorisedUrl = this._endpointUrl + "?Auth-Realm=" + openremote.config.realm;

        if (openremote.authenticated) {
            authorisedUrl += "&Authorization=" + openremote.getAuthorizationHeader();
        }

        this._webSocket = new WebSocket(authorisedUrl);
        this._connectDeferred = new Deferred();

        this._webSocket!.onopen = () => {
            if (this._connectDeferred) {
                let deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(true);
            }
        };

        this._webSocket!.onerror = () => {
            if (this._connectDeferred) {
                let deferred = this._connectDeferred;
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
                let deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(false);
            } else {
                this._beforeDisconnect();
            }
        };

        this._webSocket!.onmessage = (e) => {
            let msg = e.data as string;
            if (msg && msg.startsWith(SUBSCRIBED_MESSAGE_PREFIX)) {
                let jsonStr = msg.substring(SUBSCRIBED_MESSAGE_PREFIX.length);
                let subscription = JSON.parse(jsonStr) as EventSubscription<SharedEvent>;

                // Create a renewal timer if not done so
                if (!this._renewalTimer) {
                    setInterval(() => {
                        this._doRenewal();
                    }, WebSocketEventProvider._subscriptionRenewalMillis);
                }

                let deferred = this._subscribeDeferred;
                this._subscribeDeferred = null;
                if (deferred) {
                    deferred.resolve(subscription.subscriptionId);
                }
            } else if (msg.startsWith(UNAUTHORIZED_MESSAGE_PREFIX)) {
                let jsonStr = msg.substring(UNAUTHORIZED_MESSAGE_PREFIX.length);
                let subscription = JSON.parse(jsonStr) as EventSubscription<SharedEvent>;
                let deferred = this._subscribeDeferred;
                this._subscribeDeferred = null;
                if (deferred) {
                    console.warn("Unauthorized event subscription: " + subscription);
                    deferred.reject("Unauthorized");
                }
            } else if (msg.startsWith(EVENT_MESSAGE_PREFIX)) {
                let str = msg.substring(EVENT_MESSAGE_PREFIX.length);
                let triggered = JSON.parse(str) as TriggeredEventSubscription;
                if (triggered.events) {
                    triggered.events.forEach(event => {
                        this._onMessageReceived(triggered.subscriptionId!, event);
                    });
                }
            }
        };

        return this._connectDeferred.promise;
    }

    protected _beforeDisconnect(): void {
        if (this._renewalTimer != null) {
            clearInterval(this._renewalTimer);
            this._renewalTimer = null;
        }
        this._onDisconnect();
    }

    protected _doDisconnect(): void {
        this._webSocket!.close();
    }

    protected _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<string> {
        if (!this._webSocket) {
            return Promise.reject("Not connected");
        }

        if (this._subscribeDeferred) {
            return Promise.reject("There's already a pending subscription");
        }

        this._subscribeDeferred = new Deferred();
        subscription.subscriptionId = WebSocketEventProvider._subscriptionCounter++ + "";
        this._webSocket.send(SUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(subscription));
        return this._subscribeDeferred.promise;
    }

    protected _doUnsubscribe(subscriptionId: string): void {
        if (!this._webSocket) {
            return;
        }
        let cancelSubscription: CancelEventSubscription<SharedEvent> = {
            subscriptionId: subscriptionId
        };
        this._webSocket.send(UNSUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(cancelSubscription));
        if (Object.keys(this._subscriptionMap).length == 0 && this._renewalTimer) {
            clearInterval(this._renewalTimer);
        }
    }

    protected _doRenewal() {
        if (!this._webSocket) {
            return;
        }

        let renewSubscriptions: RenewEventSubscriptions = {
            subscriptionIds: Object.keys(this._subscriptionMap)
        };
        this._webSocket.send(RENEW_MESSAGE_PREFIX + JSON.stringify(renewSubscriptions));
    }

    protected _doSend<T extends SharedEvent>(event: T): void {
        if (!this._webSocket) {
            return;
        }

        let message = EVENT_MESSAGE_PREFIX + JSON.stringify(event);
        this._webSocket.send(message);
    }
}