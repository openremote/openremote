import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import i18next from "i18next";
import "@openremote/or-components/or-panel";
import {ClientRole, ConnectionStatus, GatewayConnection, GatewayConnectionStatusEvent} from "@openremote/model";
import manager, {DefaultColor1, DefaultColor3, OREvent} from "@openremote/core";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";

export function pageGatewayProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "gateway",
        routes: [
            "gateway"
        ],
        pageCreator: () => {
            return new PageGateway(store);
        }
    };
}

@customElement("page-gateway")
export class PageGateway extends Page<AppStateKeyed>  {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;
                
                display: flex;
                justify-content: center;
                
                --or-panel-heading-min-height: 0px;
                --or-panel-heading-margin: 4px 0 0 10px;
                --or-panel-background-color: var(--or-app-color1, ${unsafeCSS(DefaultColor1)});
                --or-panel-heading-font-size: 14px; 
                --or-panel-padding: 14px;
            }            
            
            #wrapper {  
                height: 100%;
                width: 100%;
                display: flex;
                flex-direction: column;
                overflow: auto;
            }
                
            #title {
                margin: 20px auto 0;
                font-size: 18px;
                font-weight: bold;
                max-width: 1000px;
                min-width: 600px;
                color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
            }

            #title > or-icon {
                margin-right: 10px;
                margin-left: 14px;
            }
            
            or-panel {
                position: relative;
                max-width: 1000px;
                min-width: 600px;
                margin: 20px auto;
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
            
            #fields > or-mwc-input {
                margin: 10px 0;
                width: 100%;
            }
            
            #buttons {
                text-align: right;
            }
            
            #buttons > or-mwc-input {
                margin: 10px;                
            }

            @media only screen and (max-width: 780px){
                :host { 
                    --or-panel-border-radius: 0;
                }
                or-panel {
                    width: 100%;
                    min-width: auto;
                }

                #title {
                    width: 100%;
                    min-width: auto;
                }
            }
        `;
    }

    @state()
    protected realm?: string;

    @state()
    protected _loading = true;

    @state()
    protected _connection?: GatewayConnection;

    @state()
    protected _connectionStatus?: ConnectionStatus;

    @property()
    protected _dirty = false;

    protected _readonly = false;
    protected _eventSubscriptionId?: string;

    get name(): string {
        return "gatewayConnection";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    connectedCallback() {
        super.connectedCallback();
        this._readonly = !manager.hasRole(ClientRole.WRITE_ADMIN);
        this._subscribeEvents();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._unsubscribeEvents();
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
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="cloud"></or-icon>${i18next.t("gatewayConnection")}
                </div>
                <or-panel ?disabled="${this._loading}" .heading="${i18next.t("connectionDetails")}">
                    <div id="status">
                        ${this._connectionStatus}
                    </div>
                    <div id="fields">
                        <or-mwc-input .label="${i18next.t("host")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.host : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("host", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("port")}" .type="${InputType.NUMBER}" ?disabled="${this._loading || this._readonly}" min="1" max="65536" step="1" .value="${connection ? connection.port : undefined}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("port", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("realm")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.realm : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("realm", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("clientId")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.clientId : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientId", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("clientSecret")}" .type="${InputType.TEXT}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.clientSecret : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientSecret", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("secured")}" .type="${InputType.SWITCH}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.secured : false}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("secured", e.detail.value)}"></or-mwc-input>
                        <or-mwc-input .label="${i18next.t("disabled")}" .type="${InputType.SWITCH}" ?disabled="${this._loading || this._readonly}" .value="${connection ? connection.disabled : false}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("disabled", e.detail.value)}"></or-mwc-input>
                    </div>
                    <div id="buttons">
                        <or-mwc-input label="reset" ?disabled="${!this._dirty || this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._reset()}"></or-mwc-input>                
                        <or-mwc-input label="delete" ?disabled="${this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._delete()}"></or-mwc-input>                
                        <or-mwc-input label="save" ?disabled="${!this._dirty || this._loading || this._readonly}" .type="${InputType.BUTTON}" raised @click="${() => this._save()}"></or-mwc-input>                
                    </div>
                </or-panel>
            </div>
        `;
    }

    public stateChanged(state: AppStateKeyed) {
        this.realm = state.app.realm;
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
