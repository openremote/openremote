import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import i18next from "i18next";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {createRef, Ref, ref} from "lit/directives/ref.js";
import "@openremote/or-components/or-panel";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {AttributeDescriptor, AttributePredicate, ClientRole, ConnectionStatus, GatewayAttributeFilter, GatewayConnection, GatewayConnectionStatusEvent, LogicGroupOperator, GatewayAssetSyncRule} from "@openremote/model";
import manager, {DefaultColor1, DefaultColor3} from "@openremote/core";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {OrAssetTypeAttributePicker, AssetTypeAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import "@openremote/or-components/or-ace-editor";
import moment from "moment";
import {OrAceEditor} from "@openremote/or-components/or-ace-editor";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

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
                display: flex;
                width: calc(100% - 40px);
                max-width: 1360px;
                padding: 0px 20px;
                flex-direction: row;
                align-items: center;
                justify-content: space-between;
                margin: 8px auto;
            }
            
            #title div {
                display: flex;
                align-items: center;
            }
            
            #title > div > span {
                font-size: 18px;
                font-weight: bold;
            }

            #title > div > or-icon {
                --or-icon-width: 20px;
                --or-icon-height: 20px;
                margin-right: 10px;
                margin-left: 14px;
            }

            or-panel {
                position: relative;
                width: calc(100% - 40px);
                max-width: 1360px;
                margin: 0 auto 16px;
                --or-panel-heading-text-transform: uppercase;
            }

            .gateway-status-header {
                position: absolute;
                top: 18px;
                right: 25px;
                font-weight: bold;
                display: flex;
            }

            #gateway-content {
                display: flex;
                flex-wrap: wrap;
                padding: 10px;
                gap: 40px;
            }

            .gateway-column {
                flex: 1;
                flex-basis: 45%;
                min-width: 350px;
                display: flex;
                flex-direction: column;
                gap: 20px;
            }

            .gateway-sharing-control {
                display: flex;
                flex-direction: column;
                gap: 10px;
            }

            .gateway-sharing-control-child {
                margin-left: 20px;
                display: flex;
                gap: 10px;
                align-items: center;
            }

            #gateway-footer {
                margin-top: 40px;
                display: flex;
                justify-content: space-between;
            }

            #gateway-footer > div {
                display: flex;
                gap: 10px;
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

            @media only screen and (max-width: 1200px) {
                #gateway-content {
                    gap: 20px;
                }
            }
        `;
    }

    @state()
    protected realm?: string;

    @state()
    protected _loading = false;

    @state()
    protected _connection?: GatewayConnection;

    @state()
    protected _connectionStatus?: ConnectionStatus;

    @state()
    protected _dirty = false;

    @state()
    protected _invalid = false;

    protected _readonly = false;
    protected _eventSubscriptionId?: string;
    protected _intervalMin?: number;

    get name(): string {
        return "gatewayConnection";
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
        const disabled = this._loading || this._readonly;

        return html`
            <div id="wrapper">
                
                ${until(this._getTitleTemplate(connection, this._connectionStatus, disabled))}
                
                <or-panel ?disabled="${disabled}" .heading="${i18next.t("gateway.connectionDetails")}">
                    ${when(this._connectionStatus, () => html`
                        <div class="gateway-status-header">
                            <or-translate value="status" style="font-weight: normal;"></or-translate>:
                            <span style="margin-left: 10px;">${this._connectionStatus}</span>
                        </div>
                    `)}
                    ${until(this._getContentTemplate(() => this._getSettingsColumns(connection, disabled)))}
                </or-panel>
                
                <or-panel ?disabled="${disabled}" heading="${i18next.t("gateway.dataSharing")}">
                    <div class="gateway-status-header">
                        <or-mwc-input .type="${InputType.BUTTON}" label="JSON" outlined icon="pencil"
                                      @or-mwc-input-changed="${() => this._openConnectionJSONEditor(connection)}"
                        ></or-mwc-input>
                    </div>
                    ${until(this._getContentTemplate(() => this._getDataSharingColumns(connection, this._isDataSharingCustom(connection), disabled)))}
                </or-panel>                
     
                <or-panel ?disabled="${disabled}" heading="${i18next.t("gateway.assetSyncRules")}">
                    ${until(this._getContentTemplate(() => this._getAssetSyncRulesColumns(connection, disabled)))}
                </or-panel>
            </div>
        `;
    }

    protected async _getTitleTemplate(connection?: GatewayConnection, _status?: ConnectionStatus, disabled = true): Promise<TemplateResult> {
        return html`
            <div id="title">
                <div>
                    <or-icon icon="cloud"></or-icon>
                    <span>${i18next.t("gatewayConnection")}</span>
                </div>
                <div style="gap: 20px;">
                    <div>
                        <or-translate value="enabled"></or-translate>
                        <or-mwc-input .type="${InputType.SWITCH}" .value="${!connection.disabled}" ?disabled="${disabled}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("disabled", !e.detail.value)}"
                        ></or-mwc-input>
                    </div>
                    <or-mwc-input label="save" ?disabled="${!this._dirty || !this._invalid || disabled}" .type="${InputType.BUTTON}" raised @click="${() => this._save()}"></or-mwc-input>
                </div>
            </div>
        `;
    }

    protected async _getContentTemplate(content: () => Promise<TemplateResult>): Promise<TemplateResult> {
        return html`
            <div id="gateway-content">
                ${until(content(), html`<or-loading></or-loading>`)}
            </div>
        `;
    }

    /**
     * Returns an HTML {@link TemplateResult} with controls to configure the {@link GatewayConnection}.
     * Settings like host, port, realm, clientId, clientSecret, and more.
     */
    protected async _getSettingsColumns(connection: GatewayConnection, disabled = true): Promise<TemplateResult> {
        return html`
            <div id="gateway-column-1" class="gateway-column">
                <div></div>
                <or-mwc-input id="gateway-host" .label="${i18next.t("host")}" required .type="${InputType.TEXT}" ?disabled="${disabled}" .value="${connection?.host}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("host", e.detail.value)}"
                ></or-mwc-input>
                <or-mwc-input id="gateway-port" .label="${i18next.t("port")}" .type="${InputType.NUMBER}"
                              ?disabled="${disabled}" min="1" max="65536" step="1" .value="${connection?.port}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("port", e.detail.value)}"
                ></or-mwc-input>
                <or-mwc-input id="gateway-realm" .label="${i18next.t("realm")}" required .type="${InputType.TEXT}" ?disabled="${disabled}" .value="${connection?.realm}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("realm", e.detail.value)}"
                ></or-mwc-input>
                <div></div>
            </div>
            <div id="gateway-column-2" class="gateway-column">
                <div></div>
                <or-mwc-input id="gateway-clientid" .label="${i18next.t("clientId")}" required .type="${InputType.TEXT}" ?disabled="${disabled}" .value="${connection?.clientId}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientId", e.detail.value)}"
                ></or-mwc-input>
                <or-mwc-input id="gateway-clientsecret" .label="${i18next.t("clientSecret")}" required .type="${InputType.TEXT}" ?disabled="${disabled}" .value="${connection?.clientSecret}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("clientSecret", e.detail.value)}"
                ></or-mwc-input>
                <or-mwc-input id="gateway-secured" .label="${i18next.t("secured")}" .type="${InputType.CHECKBOX}" style="height: 56px;" ?disabled="${disabled}" .value="${connection?.secured || false}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("secured", e.detail.value)}"
                ></or-mwc-input>
            </div>
        `;
    }

    /**
     * Returns an HTML {@link TemplateResult} with the controls to limit the rate of {@link AttributeEvent}s in the {@link GatewayConnection}.
     * Think of an attribute picker, and a number control to define the interval (in minutes).
     */
    protected async _getDataSharingColumns(connection: GatewayConnection, isCustom = false, disabled = true): Promise<TemplateResult> {
        const controlsDisabled = isCustom || disabled;
        const filterChecked = connection.attributeFilters?.find(filter => filter.matcher !== undefined);
        const filterDisabled = controlsDisabled || !filterChecked;
        const interval = connection.attributeFilters?.[0]?.duration ? moment.duration(connection.attributeFilters[0].duration).get("minutes") : undefined;
        const intervalDisabled = controlsDisabled || interval === undefined;
        const attrAmountArr = connection.attributeFilters?.map(filter => filter.matcher?.attributes?.items?.length || 0);
        const attrAmount = attrAmountArr?.reduce((a, b) => a + b, 0);
        const controlStyling = controlsDisabled ? "--mdc-theme-text-primary-on-background: lightgray; color: lightgray" : undefined;
        return html`
            <div id="gateway-column-3" class="gateway-column">
                ${when(isCustom, () => html`
                    <or-translate value="gateway.limit_sharing_is_custom_error"></or-translate>
                `, () => html`
                    <div></div>
                `)}
                <div class="gateway-sharing-control" style="${controlStyling}">
                    <or-mwc-input .label="${i18next.t("gateway.limit_sharing_attribute")}" .type="${InputType.CHECKBOX}"
                                  ?disabled="${controlsDisabled}" .value="${controlsDisabled ? undefined : filterChecked}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onLimitAttributesCheck(e)}"
                    ></or-mwc-input>
                    <div class="gateway-sharing-control-child">
                        <or-mwc-input .type="${InputType.BUTTON}" raised ?disabled="${filterDisabled}"
                                      label="${attrAmount || 0} ${i18next.t("gateway.limit_sharing_attribute_selected")}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onLimitAttributesButtonClick(e)}"
                        ></or-mwc-input>
                    </div>
                </div>
                <div class="gateway-sharing-control"  style="${controlStyling}">
                    <or-mwc-input .label="${i18next.t("gateway.limit_sharing_rate")}" .type="${InputType.CHECKBOX}"
                                  ?disabled="${controlsDisabled}" .value="${!controlsDisabled && interval !== undefined}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onAttributesIntervalUpdate(e.detail.value ? 1 : undefined)}"
                    ></or-mwc-input>
                    <div class="gateway-sharing-control-child">
                        <or-mwc-input .type="${InputType.NUMBER}" compact outlined ?disabled="${intervalDisabled}" .value="${controlsDisabled ? undefined : interval}" style="width: 84px;"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onAttributesIntervalUpdate(e.detail.value)}"
                        ></or-mwc-input>
                        <or-translate value="gateway.limit_sharing_rate_suffix"></or-translate>
                    </div>
                </div>
            </div>
        `;
    }

    protected async _getAssetSyncRulesColumns(connection: GatewayConnection, disabled = true): Promise<TemplateResult> {
        const controlStyling = disabled ? "--mdc-theme-text-primary-on-background: lightgray; color: lightgray" : undefined;
        return html`
            <div id="gateway-column-4" class="gateway-column">
                <div class="gateway-sharing-control"  style="${controlStyling}">
                    <or-mwc-input .label="${i18next.t("gateway.assetSyncRulesEnable")}" .type="${InputType.CHECKBOX}"
                                  ?disabled="${disabled}" .value="${!!connection.assetSyncRules}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onAssetSyncRulesToggle(!!e.detail.value)}"
                    ></or-mwc-input>
                    <div class="gateway-sharing-control-child">
                        <or-mwc-input .type="${InputType.JSON_OBJECT}" ?disabled="${disabled || !connection.assetSyncRules}" 
                                      .value="${connection?.assetSyncRules}"
                                      resizevertical
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._setConnectionProperty("assetSyncRules", e.detail.value)}"
                                      .label="${i18next.t('gateway.assetSyncRulesInput')}" style="width: 100%;"></or-mwc-input>
                        <or-translate value=""></or-translate>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * HTML callback for checking the "limit data sharing by attribute" checkbox.
     */
    protected _onLimitAttributesCheck(ev: OrInputChangedEvent) {
        const attrFilters = this._connection.attributeFilters;
        if(attrFilters?.length > 0) {
            attrFilters?.forEach(filter => {
                if(ev.detail.value) {
                    filter.matcher = {};
                } else {
                    delete filter.matcher;
                    delete filter.skipAlways;
                }
            });
            this._updateAttributeFilters(attrFilters);
        } else {
            this._updateAttributeFilters(ev.detail.value ? [{ matcher: {} }] as GatewayAttributeFilter[] : undefined);
        }
    }

    /**
     * HTML callback for clicking the "X attributes selected" button.
     * Here, it opens an attribute picker and handles its callback.
     */
    protected _onLimitAttributesButtonClick(_ev: OrInputChangedEvent) {
        const selectedAttrs = this._getAttrDescriptorMapFromFilters(this._connection.attributeFilters);
        const dialog = showDialog(new OrAssetTypeAttributePicker().setSelectedAttributes(selectedAttrs).setMultiSelect(true));
        dialog.addEventListener(AssetTypeAttributePickerPickedEvent.NAME, (ev) => {

            const duration = this._intervalMin ? moment.duration(this._intervalMin, "minutes") : undefined;
            const assetTypeMap = ev.detail as Map<string, AttributeDescriptor[]>;
            const filters = Array.from(assetTypeMap.entries()).map(entry => ({
                duration: duration?.toISOString(),
                matcher: {
                    types: [entry[0]] as string[],
                    attributes: {
                        items: entry[1]?.map((attr: AttributeDescriptor) => ({
                            name: {predicateType: "string", value: attr.name}
                        })),
                        operator: LogicGroupOperator.OR
                    }
                }
            }) as GatewayAttributeFilter);

            // By default, all other attributes will NOT be shared (they're blocked)
            filters.push({
                skipAlways: true
            });

            this._updateAttributeFilters(filters);
        });
    }

    /**
     * HTML callback for when the "minutes interval between attribute values" updates.
     */
    protected _onAttributesIntervalUpdate(value?: number) {
        this._intervalMin = value;
        const duration = this._intervalMin ? moment.duration(this._intervalMin, "minutes") : undefined;
        const attributeFilters = this._connection.attributeFilters || [];
        if(attributeFilters.length > 0) {

            // Update all existing filters to the new duration
            attributeFilters?.forEach(filter => {
                if(duration) {
                    const isSkipAlwaysItem = Object.keys(filter).length === 1 && filter.skipAlways;
                    if(!isSkipAlwaysItem) {
                        filter.duration = duration?.toISOString();
                    }
                } else {
                    delete filter.duration;
                }
            });

        } else if(duration) {
            attributeFilters.push({
                duration: duration.toISOString()
            });
        }
        this._updateAttributeFilters(attributeFilters);
    }

    protected _onAssetSyncRulesToggle(enabled: boolean) {
        if (!enabled) {
            this._setConnectionProperty("assetSyncRules", undefined);
        } else {
            this._setConnectionProperty("assetSyncRules", this._getDefaultGatewayAssetSyncRules());
        }
    }

    /**
     * HTML callback for when the GatewayAssetSyncRules updates.
     */
    protected _onAssetSyncRulesUpdated(syncRules?: { [index: string]: GatewayAssetSyncRule }) {
        this._setConnectionProperty("assetSyncRules", syncRules);
    }

    protected  _getDefaultGatewayAssetSyncRules(): { [index: string]: GatewayAssetSyncRule }  {
        return {
            "*" : {
                excludeAttributeMeta: {
                    "*": [
                        "accessPublicRead",
                        "accessPublicWrite",
                        "accessRestrictedRead",
                        "accessRestrictedWrite"
                    ]
                },
                addAttributeMeta: {
                    "*": {
                        "storeDataPoints": true
                    }
                },
                excludeAttributes: [
                    "notes"
                ]
            }
        }
    }

    /**
     * Internal function that returns a map of {@link AttributeDescriptor[]}, based on the {@link GatewayAttributeFilter}.
     * Mainly meant for the attribute-picker, as it uses AttributeDescriptors instead.
     */
    protected _getAttrDescriptorMapFromFilters(attrFilters?: GatewayAttributeFilter[]): Map<string, AttributeDescriptor[]> {
        const map = new Map<string, AttributeDescriptor[]>();
        if(!attrFilters) {
            return map;
        }
        attrFilters.forEach(filter => {
            const key = filter.matcher?.types?.[0] as string | undefined;
            const values = (filter.matcher?.attributes?.items as any)?.map((attr: AttributePredicate) => ({
                name: attr.name.value
            }));
            if(key && values) {
                map.set(key, values);
            }
        });
        return map;
    }

    protected async _loadData() {
        this._loading = true;
        this._connection = {secured: true};
        this._connectionStatus = null;
        const connectionResponse = await manager.rest.api.GatewayClientResource.getConnection(this.realm);
        const statusResponse = await manager.rest.api.GatewayClientResource.getConnectionStatus(this.realm);

        this._setConnection(connectionResponse.data);
        this._connectionStatus = statusResponse.data;
        this._loading = false;
    }

    protected _updateAttributeFilters(attributeFilters: GatewayAttributeFilter[]) {
        attributeFilters = attributeFilters?.filter(filter => Object.keys(filter).length > 0); // clear unset keys and empty objects
        console.debug("Updating attributeFilters to", attributeFilters);
        this._setConnectionProperty("attributeFilters", attributeFilters);
    }

    protected _setConnectionProperty(propName: string, value: any) {
        this._connection[propName] = value;
        this._dirty = true;
        this._invalid = this._isValid();
        this.requestUpdate("_connection");
    }

    protected async _save() {
        this._loading = true;
        return manager.rest.api.GatewayClientResource.setConnection(this.realm, this._connection).then(response => {
            if(response.status === 204) {
                this._loadData();
            } else {
                showSnackbar(undefined, i18next.t("errorOccurred"));
            }
        }).catch(() => {
            showSnackbar(undefined, i18next.t("errorOccurred"));
        }).finally(() => {
            this._loading = false;
        });
    }

    protected _setConnection(connection: GatewayConnection) {
        this._connection = connection || {secured: true};
        this._dirty = false;
    }

    protected _onEvent(event: GatewayConnectionStatusEvent) {
        if (event.realm === this.realm) {
            this._connectionStatus = event.connectionStatus;
        }
    }

    /**
     * Internal function that verifies whether the Data Sharing configuration is customized through JSON.
     * (by checking the amount of object keys, and whether it is valid at all)
     */
    protected _isDataSharingCustom(connection?: GatewayConnection): boolean {
        if(!connection?.attributeFilters) {
            return false;
        }
        if(!Array.isArray(connection.attributeFilters)) {
            return true;
        }
        if(connection.attributeFilters.length === 0) {
            return false;
        }
        const excludedKeys = ['duration', 'matcher', 'durationParsedMillis', 'skipAlways'];
        const customFilter = connection.attributeFilters.find(filter => {
            const unknownKeys = Object.keys(filter).filter(key => !excludedKeys.includes(key));
            return unknownKeys.length > 0;
        });
        return !!customFilter;

    }

    protected _isValid(): boolean {
        if(!this._connection.host) {
            console.warn("Interconnect form can't be submitted: Host is not valid.");
            return false;
        }
        if(!this._connection.realm) {
            console.warn("Interconnect form can't be submitted: Realm name must be set.")
            return false;
        }
        if(!this._connection.clientId) {
            console.warn("Interconnect form can't be submitted: Client ID must be set.")
            return false;
        }
        if(!this._connection.clientSecret) {
            console.warn("Interconnect form can't be submitted: Client secret must be set.")
            return false;
        }
        return true;
    }

    /**
     * Function that opens the JSON editor for customizing the {@link GatewayAttributeFilter}s manually.
     * The changed are only applied once the save button is pressed.
     */
    protected _openConnectionJSONEditor(connection?: GatewayConnection) {
        const editorRef: Ref<OrAceEditor> = createRef();
        showDialog(new OrMwcDialog()
            .setHeading("JSON Editor")
            .setContent(html`
                <or-ace-editor ${ref(editorRef)} .value="${connection?.attributeFilters}" style="height: 60vh; width: 1024px;"></or-ace-editor>
            `)
            .setActions([
                { actionName: "cancel", content: "cancel"},
                { actionName: "save", content: "save", action: () => {
                    const editor = editorRef.value;
                    if(!editor.validate()) {
                        console.warn("JSON was not valid");
                        showSnackbar(undefined, i18next.t('errorOccurred'));
                        return;
                    }
                    try {
                        let parsed: GatewayAttributeFilter[] | undefined;
                        if(editor.getValue().length > 0) {

                            parsed = JSON.parse(editor.getValue());

                            // Verify if the JSON is an array. If so; simply accept the format.
                            if(!Array.isArray(parsed)) {
                                console.warn("Could not parse JSON to GatewayAttributeFilter[], as it was not an array.");
                                showSnackbar(undefined, i18next.t("errorOccurred"));
                                return;
                            }
                        }
                        this._updateAttributeFilters(parsed);

                    } catch (e) {
                        console.error(e);
                        showSnackbar(undefined, i18next.t("errorOccurred"));
                    }
                }}
            ])
        );
    }
}
