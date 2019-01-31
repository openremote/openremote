//import rest from "@openremote/rest";
import openremote from "./index";
import {Deferred} from "./util";
import {EventSubscription, SharedEvent} from "@openremote/model";

export enum EventProviderStatus {
    DISCONNECTED = "DISCONNECTED",
    CONNECTED = "CONNECTED",
    CONNECTING = "CONNECTING"
}

export interface EventProvider {
    connect(): Promise<boolean>;

    disconnect(): void;
}

interface EventSubscriptionInfo<T extends SharedEvent> {
    eventSubscription: EventSubscription<T>;
    callback: (event: T) => void;
    deferred: Deferred<number> | null;
}

const SUBSCRIBE_MESSAGE_PREFIX = "SUBSCRIBE:";
const SUBSCRIBED_MESSAGE_PREFIX = "SUBSCRIBED:";
const UNSUBSCRIBE_MESSAGE_PREFIX = "UNSUBSCRIBE:";

abstract class EventProviderImpl implements EventProvider {

    protected static MIN_RECONNECT_DELAY: number = 0;
    protected static MAX_RECONNECT_DELAY: number = 30000;
    protected _disconnectRequested: boolean = false;
    protected _reconnectDelayMillis: number = WebSocketEventProvider.MIN_RECONNECT_DELAY;
    protected _reconnectTimer: number | null = null;
    protected _statusCallback: (status: EventProviderStatus) => void;
    protected _status: EventProviderStatus = EventProviderStatus.DISCONNECTED;
    protected _connectingDeferred: Deferred<boolean> | null = null;

    protected _pendingSubscription: EventSubscriptionInfo<SharedEvent> | null = null;
    protected _queuedSubscriptions: EventSubscriptionInfo<SharedEvent>[] = [];
    protected _subscriptionMap: { [id: number]: EventSubscriptionInfo<SharedEvent> } = {};

    protected abstract _doConnect(): Promise<boolean>;

    protected abstract _doDisconnect(): void;

    protected abstract _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<number>;

    protected abstract _doUnsubscribe(subscriptionId: number): void;

    abstract get endpointUrl(): string;

    public get status(): EventProviderStatus {
        return this._status;
    }

    protected constructor(statusCallback: (status: EventProviderStatus) => void) {
        this._statusCallback = statusCallback;
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
                    this._reconnectDelayMillis = WebSocketEventProvider.MIN_RECONNECT_DELAY;
                }

                if (connected) {
                    console.debug("Connected to event service: " + this.endpointUrl);
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

    public subscribe<T extends SharedEvent>(eventSubscription: EventSubscription<T>, callback: (event: T) => void): Promise<number> {

        let subscriptionInfo = {
            eventSubscription: eventSubscription,
            callback: callback as ((event: SharedEvent) => void),
            deferred: new Deferred<number>()
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

    public unsubscribe<T extends SharedEvent>(subscriptionId: number) {

        let callback = this._subscriptionMap[subscriptionId];
        if (callback) {
            delete this._subscriptionMap[subscriptionId];
            this._doUnsubscribe(subscriptionId);
        }
    }

    protected _processNextSubscription() {
        if (this._status != EventProviderStatus.CONNECTED || this._queuedSubscriptions.length === 0) {
            return;
        }

        setTimeout(() => {
            let subscriptionInfo = this._queuedSubscriptions.shift();
            if (subscriptionInfo) {
                this.subscribe(subscriptionInfo.eventSubscription, subscriptionInfo.callback)
                    .then(
                        (id: number) => {
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
            this._statusCallback(status);
        }, 0);
    }

    protected _onMessageReceived(messageId: string, message: any) {
        console.debug("Event provider message received");
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

    protected _onError() {
        console.debug("Event provider error");
        // Could have inconsistent state so disconnect and let consumers decide what to do and when to reconnect
        this.disconnect();
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

        this._reconnectTimer = setTimeout(() => {

            if (this._disconnectRequested) {
                return;
            }

            this.connect().then(connected => {
                if (connected) {
                    this._reconnectDelayMillis = WebSocketEventProvider.MIN_RECONNECT_DELAY;
                } else {
                    this._scheduleReconnect();
                }
            })
        }, this._reconnectDelayMillis);

        if (this._reconnectDelayMillis < WebSocketEventProvider.MAX_RECONNECT_DELAY) {
            this._reconnectDelayMillis = Math.min(WebSocketEventProvider.MAX_RECONNECT_DELAY, this._reconnectDelayMillis + 3000);
        }
    }
}

export class WebSocketEventProvider extends EventProviderImpl {

    private _endpointUrl: string;
    protected _webSocket: WebSocket | undefined = undefined;
    protected _connectDeferred: Deferred<boolean> | null = null;
    protected _subscribeDeferred: Deferred<number> | null = null;

    get endpointUrl(): string {
        return this._endpointUrl;
    }

    constructor(managerUrl: string, statusCallback: (connected: EventProviderStatus) => void) {
        super(statusCallback);

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
                this._onError();
            }
        };

        this._webSocket!.onclose = () => {
            this._webSocket = undefined;

            if (this._connectDeferred) {
                let deferred = this._connectDeferred;
                this._connectDeferred = null;
                deferred.resolve(false);
            } else {
                this._onDisconnect();
            }
        };

        this._webSocket!.onmessage = (e) => {
            let msg = e.data as string;
            if (msg && msg.startsWith(SUBSCRIBED_MESSAGE_PREFIX)) {
                let jsonStr = msg.substring(SUBSCRIBED_MESSAGE_PREFIX.length);
                let id = JSON.parse(jsonStr) as number;

                if (this._subscribeDeferred) {
                    this._subscribeDeferred.resolve(id);
                }
            } else {
                this._onMessageReceived(e.lastEventId, e.data);
            }
        };

        return this._connectDeferred.promise;
    }

    protected _doDisconnect(): void {
        this._webSocket!.close();
    }

    protected _doSubscribe<T extends SharedEvent>(subscription: EventSubscription<T>): Promise<number> {
        if (!this._webSocket) {
            return Promise.reject("Not connected");
        }

        if (this._subscribeDeferred) {
            return Promise.reject("There's already a pending subscription");
        }

        this._subscribeDeferred = new Deferred();
        this._webSocket.send(SUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(subscription));
        return this._subscribeDeferred.promise;
    }

    protected _doUnsubscribe(subscriptionId: number): void {
        if (!this._webSocket) {
            return;
        }
        this._webSocket.send(UNSUBSCRIBE_MESSAGE_PREFIX + JSON.stringify(subscriptionId));
    }
}