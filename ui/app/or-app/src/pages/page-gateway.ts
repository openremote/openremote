import {css, customElement, html, property, PropertyValues, TemplateResult, unsafeCSS} from "lit-element";
import i18next from "i18next";
import "@openremote/or-panel";
import {ConnectionStatus, GatewayConnection, GatewayConnectionStatusEvent} from "@openremote/model";
import manager, {DefaultColor1, OREvent} from "@openremote/core";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageGatewayProvider<S extends AppStateKeyed>(store: EnhancedStore<S>) {
    return {
        routes: [
            "gateway"
        ],
        pageCreator: () => {
            return new PageGateway(store);
        }
    };
}

@customElement("page-gateway")
class PageGateway<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;
                
                display: flex;
                justify-content: center;
                
                --or-panel-heading-margin: 0 0 5px 10px;
                --or-panel-background-color: var(--or-app-color1, ${unsafeCSS(DefaultColor1)});
                --or-panel-heading-font-size: large; 
            }            
            
            @media only screen and (max-width: 1080px){
                or-panel {
                    margin: 0;
                }
            }
            
            or-panel {
                position: relative;
                max-width: 1000px;
                min-width: 600px;
                margin-top: 40px;
            }
            
            #status {
                position: absolute;
                top: 15px;
                right: 25px;
                font-weight: bold;
                
            }
            
            #fields {
                display: flex;
                flex-direction: column;
                padding: 10px;
            }
            
            #fields > or-input {
                margin: 10px 0;
                width: 100%;
            }
            
            #buttons {
                text-align: right;
            }
            
            #buttons > or-input {
                margin: 10px;                
            }
        `;
    }

    @property()
    public realm?: string;

    @property()
    protected _loading = true;

    @property()
    protected _connection?: GatewayConnection;

    @property()
    protected _connectionStatus?: ConnectionStatus;

    @property()
    protected _dirty = false;

    protected _readonly = false;
    protected _eventSubscriptionId?: string;

    protected _onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.DISPLAY_REALM_CHANGED:
                this.realm = manager.displayRealm;
                break;
        }
    };

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    connectedCallback() {
        super.connectedCallback();
        this._readonly = !manager.hasRole("write:admin");
        manager.addListener(this._onManagerEvent)
        this._subscribeEvents();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._unsubscribeEvents();
        manager.removeListener(this._onManagerEvent);
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("realm")) {
            this._loadData();
        }

        return super.shouldUpdate(_changedProperties);
    }

    public updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (!this.realm) {
            this.realm = manager.displayRealm;
        }
    }

    protected render(): TemplateResult | void {
        const connection = this._connection;

        return html`
            <or-panel ?disabled="${this._loading}" .heading="${i18next.t("gatewayConnection") + " (" + this.realm + ")"}">
                <div id="status">
                    ${this._connectionStatus}
                </div>
                <div id="fields">
                    <or-input .label="${i18next.t("host")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.host : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("host", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("port")}" .type="${InputType.NUMBER}" ?disabled="${this._loading || this._readonly}" min="1" max="65536" step="1" .value="${connection ? connection.port : undefined}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("port", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("realm")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.realm : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("realm", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("clientId")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.clientId : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientId", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("clientSecret")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.clientSecret : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientSecret", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("secured")}" .type="${InputType.SWITCH}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.secured : false}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("secured", e.detail.value)}"></or-input>
                    <or-input .label="${i18next.t("disabled")}" .type="${InputType.SWITCH}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.disabled : false}" @or-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("disabled", e.detail.value)}"></or-input>
                </div>
                <div id="buttons">
                    <or-input .label="${i18next.t("reset")}" ?disabled="${!this._dirty || this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._reset()}"></or-input>                
                    <or-input .label="${i18next.t("delete")}" ?disabled="${this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._delete()}"></or-input>                
                    <or-input .label="${i18next.t("save")}" ?disabled="${!this._dirty || this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._save()}"></or-input>                
                </div>
            </or-panel>
        `;
    }

    public stateChanged(state: S) {
    }

    protected async _subscribeEvents() {
        if (manager.events) {
            this._eventSubscriptionId = await manager.events.subscribe<GatewayConnectionStatusEvent>({
                eventType: "gateway-connection-status"
            }, (ev) => this._onEvent(ev));
        }
    }

    protected _unsubscribeEvents() {
        if (this._eventSubscriptionId) {
            manager.events!.unsubscribe(this._eventSubscriptionId);
            this._eventSubscriptionId = undefined;
        }
    }

    protected async _loadData() {
        this._loading = true;
        this._connection = {secured: true};
        this._connectionStatus = null;
        const connectionResponse = await manager.rest.api.GatewayClientResource.getConnection(this.realm);
        const statusResponse = await manager.rest.api.GatewayClientResource.getConnectionStatus(this.realm);

        this._setConnection(connectionResponse.data);
        this._connectionStatus = statusResponse.data;
    }

    protected _setConnectionProperty(propName: string, value: any) {
        this._connection[propName] = value;
        this._dirty = true;
    }

    protected _reset() {
        this._loadData();
    }

    protected async _delete() {
        this._loading = true;
        const response = await manager.rest.api.GatewayClientResource.deleteConnection(this.realm);
        if (response.status !== 204) {
            // TODO: Toast message
        }
        this._loadData();
    }

    protected async _save() {
        this._loading = true;
        const response = await manager.rest.api.GatewayClientResource.setConnection(this.realm, this._connection);
        if (response.status !== 204) {
            // TODO: Toast message
        }
        this._loadData();
    }

    protected _setConnection(connection: GatewayConnection) {
        this._connection = connection || {secured: true};
        this._loading = false;
        this._dirty = false;
    }

    protected _onEvent(event: GatewayConnectionStatusEvent) {
        if (event.realm === this.realm) {
            this._connectionStatus = event.connectionStatus;
        }
    }
}
