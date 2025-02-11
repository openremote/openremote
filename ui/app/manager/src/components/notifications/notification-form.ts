import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { OrMwcInput, InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import manager, { DefaultColor3 } from "@openremote/core";
import { User, Asset, SentNotification, PushNotificationMessage } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { live } from "lit/directives/live.js";

export interface NotificationFormData {
    name: string;
    title: string;
    body: string;
    priority: string;
    targetType: string;
    target: string;
    // actions
    actionUrl?: string;
    openButtonText?: string;
    closeButtonText?: string;
    // fields shown in readonly mode
    source?: string;
    sourceId?: string;
    status?: string;
    sent?: string;
    delivered?: string;
}


@customElement("notification-form")
export class NotificationForm extends LitElement {
    static styles = css`
        :host {
            display: block;
            width: 100%;
        }
        
        .form-container {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .select-container {
        display: flex;
        flex-direction: column;
        gap: 4px;

        }

        select {
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 16px;
            background-color: white;
        }

        select:disabled {
            background-color: #f5f5f5;
            cursor: not-allowed;
        }

        label {
            font-size: 14px;
            color: #666;
        }

        or-mwc-input {
            width: 100%;
            margin-bottom: 12px;
        }

        /* Grid styles */

        .formGridContainer {
            display: grid; 
            grid-template-columns: 2fr 1fr;  
            grid-template-areas: 
                "targetContainer actionButtonContainer"
                "messageContentContainer actionButtonContainer"
                "messageContentContainer propContainer"
                "messageContentContainer propContainer"; 
            gap: 8px 8px;
            min-height: 100%;
            }

        .targetContainer { grid-area: targetContainer; }
        .messageContentContainer { 
            grid-area: messageContentContainer; 
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .actionButtonContainer { grid-area: actionButtonContainer; }
        .propContainer { grid-area: propContainer; }

        .section-title {
            text-transform: uppercase;
            font-weight: bolder;
            color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
            line-height: 1em;
            /*margin-bottom: 10px;*/
            margin-top: 0;
            flex: 0 0 auto;
            letter-spacing: 0.025em;
            display: flex;
            align-items: center;
            min-height: 36px;
        }

        h5 {
            margin-top: 12px; 
            margin-bottom: 6px;
        }

        :host([readonly]) or-mwc-input {
            --mdc-text-field-fill-color: var (--or-app-color2);
            pointer-events: none;
        }

        :host([readonly]) .form-container {
            opacity: 0.9;
        }
    `;

    constructor() {
        super();
        this._selectedTargetType = "ASSET";
    }
    
    connectedCallback() {
        super.connectedCallback();
        console.log("NotificationForm connected");
    }

    async firstUpdated() {
        // load assets first since it's default
        await this._loadAssets();

        // set initial target options
        this._targetOptions = (this._assets || []).map(asset => ({
            text: asset.name,
            value: asset.id
        }));

        await Promise.all([
            this._loadUsers(),
            this._loadRealms()
        ]);
    }

    @state()
    protected _users?: User[];

    @state()
    protected _assets?: Asset[];

    @state()
    protected _realms?: string[];

    @state()
    protected _selectedTargetType?: string;

    @state()
    protected _targetOptions: { text: string, value: string }[] = [];

    @state()
    protected _selectedTarget?: string;

    @state()
    protected _actionUrl?: string;

    @state()
    protected _openButtonText?: string;
 
    @state()
    protected _closeButtonText?: string;

    //TO DO: visual element for openInBrowser check
    @state()
    protected _openInBrowser?: boolean;

    @state()
    protected _formDataFields: Partial<NotificationFormData> = {};

    @property({ type: Boolean })
    public disabled = false;

    @property({type: Boolean})
    public readonly = false;

    @property({type: Object})
    public notification?: SentNotification;

    

    updated(changedProps: Map<string, any>) {
        if (changedProps.has('notification') && this.notification) {
            this._populateFormFromNotification()
        }
    }

    protected async _loadUsers(): Promise<User[]> {
        try {
            const response = await manager.rest.api.UserResource.query({
                realmPredicate: { name: manager.displayRealm }
            });
            // filter out keycloak service account
            return this._users = response.data.filter((u) => u.username !== 'manager-keycloak');
        } catch (error) {
            console.error("Failed to load users:", error);
            showSnackbar(undefined, i18next.t("Loading users failed"));
            return [];
        }
    }

    protected async _loadAssets(): Promise<Asset[]> {
        try {
            const response = await manager.rest.api.AssetResource.queryAssets({
                realm: { name: manager.displayRealm }
            });
            this._assets = response.data;
            return response.data;
        } catch (error) {
            console.error("Failed to load assets:", error);
            showSnackbar(undefined, i18next.t("Loading assets failed"));
            return [];
        }
    }

    protected async _loadRealms(): Promise<string[]> {
        try {
            const response = await manager.rest.api.RealmResource.getAccessible();
            this._realms = response.data.map(realm => realm.name);
            return this._realms;
        } catch (error) {
            console.error("Failed to load realms:", error);
            showSnackbar(undefined, i18next.t("Loading realms failed"));
            return [];
        }
    }

    protected async _onActionUrlChanged(e: OrInputChangedEvent) {
        this._actionUrl = e.detail.value;
        await this.requestUpdate();
    }

    protected async _onButtonTextChanged(e: OrInputChangedEvent, btnText: string) {
        btnText = e.detail.value;
        await this.requestUpdate();
    }

    protected async _onTargetSelected(e: OrInputChangedEvent) {
        if (!e.detail || !e.detail.value) return;

        this._selectedTarget = e.detail.value;
        await this.requestUpdate();
    }
    
    protected async _onTargetTypeChanged(e: OrInputChangedEvent) {
        const type = e.detail.value;
        if (!type) return;

        this._selectedTargetType = type;
        this._targetOptions = [];
        
        switch (type) {
            case "USER":
                if (!this._users) {
                    await this._loadUsers();
                }
                this._targetOptions = (this._users || []).map(user => ({
                    text: user.username,
                    value: user.id
                }));
                break;
                
            case "ASSET":
                if (!this._assets) {
                    await this._loadAssets();
                }
                this._targetOptions = (this._assets || []).map(asset => ({
                    text: asset.name,
                    value: asset.id
                }));
                if (this._targetOptions.length > 0) {
                    this._selectedTarget = this._targetOptions[0].value;
                }
                break;

            case "REALM":
                if (!this._realms) {
                    await this._loadRealms();
                }
                this._targetOptions = (this._realms || []).map(realm => ({
                    text: realm,
                    value: realm
                }));
                break;
        }
        
        await this.requestUpdate();
    }

    private _onFieldChanged(fieldId: string, value: any) {
        this._formDataFields = {
            ...this._formDataFields,
            [fieldId]: value
        };
        this.requestUpdate();
    }

    private async _populateFormFromNotification() {
        if (!this.notification) return;

        const pushMessage = this.notification?.message as PushNotificationMessage;
        if (!pushMessage) return;
    
        // Set target type and load corresponding options
        this._selectedTargetType = this.notification.target;
        this._selectedTarget = this.notification.targetId;
        const normalizedPriority = (pushMessage.priority || 'NORMAL').charAt(0).toUpperCase() + 
        (pushMessage.priority || 'NORMAL').slice(1).toLowerCase();
    
        // Load target options based on type
        if (this.readonly) {
            await this._onTargetTypeChanged({
                detail: { value: this._selectedTargetType }
            } as OrInputChangedEvent);
        }
    
        this._formDataFields = {
            title: pushMessage.title,
            body: pushMessage.body,
            priority: normalizedPriority,
            targetType: this.notification.target,
            target: this.notification.targetId,
            actionUrl: pushMessage.action?.url,
            openButtonText: pushMessage.buttons?.[0]?.title,
            closeButtonText: pushMessage.buttons?.[1]?.title,
        };
    
        if (this.readonly) {
            this._formDataFields = {
                ...this._formDataFields,
                source: this.notification.sourceId ? 
                    `${this.notification.source}, ${this.notification.sourceId}` : 
                    this.notification.source,
                status: this.notification.deliveredOn ? 
                    i18next.t("delivered") : 
                    i18next.t("pending"),
                sent: this.notification.sentOn ? 
                    new Date(this.notification.sentOn).toLocaleString() : 
                    '-',
                delivered: this.notification.deliveredOn ? 
                    new Date(this.notification.deliveredOn).toLocaleString() : 
                    '-'
            };
        }
    
        await this.requestUpdate();
    }

    public getFormData(): NotificationFormData | null {
        if (this.readonly && this._formDataFields) {
            return this._formDataFields as NotificationFormData;
        }

        // for create mode
        const form = this.shadowRoot!;
        const inputs = {

            title: form.querySelector<OrMwcInput>("#notificationTitle"),
            body: form.querySelector<OrMwcInput>("#notificationBody"),
            priority: form.querySelector<OrMwcInput>("#notificationPriority"),
            targetType: form.querySelector<OrMwcInput>("#targetType"),
            target: form.querySelector<OrMwcInput>("#target"),

            actionUrl: form.querySelector<OrMwcInput>("#actionUrl"),
            openButton: form.querySelector<OrMwcInput>("#openButtonText"),
            closeButton: form.querySelector<OrMwcInput>("#closeButtonText")
        };

        if ( !inputs.title?.value || !inputs.body?.value) {
            return null;
        }

        return {
            name: inputs.title.value,
            title: inputs.title.value,
            body: inputs.body.value,
            priority: inputs.priority?.value || "Normal",
            targetType: inputs.targetType?.value || this._selectedTargetType!,
            target: inputs.target?.value || this._selectedTarget!,
            actionUrl: inputs.actionUrl?.value || this._actionUrl,
            openButtonText: inputs.openButton?.value || this._openButtonText,
            closeButtonText: inputs.closeButton?.value || this._closeButtonText
        };
    }

    protected render() {
        const inputDisabled = this.disabled || this.readonly;        

        return html`
            <div class="form-container">

                <div class="formGridContainer">
                    <div class="targetContainer">
                        <h5>${i18next.t("Target")}</h5>
                         <or-mwc-input 
                            label="${i18next.t("Target type")}"
                            type="${InputType.SELECT}"
                            .options="${["USER", "ASSET", "REALM"]}"
                            ?disabled="${inputDisabled}"
                            required
                            id="targetType"
                            .value="${this._selectedTargetType}"
                            @or-mwc-input-changed="${this._onTargetTypeChanged}">
                        </or-mwc-input>

                        <or-mwc-input 
                            label="${i18next.t("Target")}"
                            type="${InputType.SELECT}"
                            ?disabled="${inputDisabled || !this._targetOptions}"
                            required
                            id="target"
                            .value="${this._selectedTarget}"
                            .options="${this._targetOptions.map(o => [o.value, o.text])}"
                            .searchProvider="${(search?: string) => {
                            // Filter options based on search text
                            if (!search) return Promise.resolve(this._targetOptions.map(o => [o.value, o.text]));
                            return Promise.resolve(
                                this._targetOptions
                                    .filter(o => o.text.toLowerCase().includes(search.toLowerCase()))
                                    .map(o => [o.value, o.text])
                            );
                        }}"
                        searchLabel="Search targets"
                            @or-mwc-input-changed="${this._onTargetSelected}">
                        </or-mwc-input>

                    </div>
               
                <div class="messageContentContainer">
                <h5>${i18next.t("Content")}</h5>
                    <or-mwc-input 
                        label="${i18next.t('Title')}"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="notificationTitle"
                        required
                        .value="${live(this._formDataFields.title || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => 
                            this._onFieldChanged('title', e.detail.value)}"
                    ></or-mwc-input>

                     <or-mwc-input 
                        label="${i18next.t('Body')}"
                        type="${InputType.TEXTAREA}"
                        style="display: flex; flex: 1; --mdc-text-field-height: 100%;"
                        rows="4"
                        ?disabled="${inputDisabled}"
                        id="notificationBody"
                        required
                        .value="${live(this._formDataFields.body || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => 
                            this._onFieldChanged('body', e.detail.value)}"
                    ></or-mwc-input>
                </div>

                <div class="actionButtonContainer">
                <h5>${i18next.t("Actions")}</h5>
                    <or-mwc-input 
                        label="${i18next.t("URL to visit (optional)")}"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="actionUrl"
                        .value="${this.readonly ? this._formDataFields.actionUrl : this._actionUrl}"
                        @or-mwc-input-changed="${this._onActionUrlChanged}">
                    </or-mwc-input>

                    <or-mwc-input 
                        label="${i18next.t("Open button text (optional)")}"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="openButtonText"
                        .value="${this.readonly? this._formDataFields.openButtonText : this._openButtonText}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onButtonTextChanged(e, this._openButtonText)}">
                    </or-mwc-input>

                    <or-mwc-input 
                        label="${i18next.t("Close button text (optional)")}"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="closeButtonText"
                        .value="${this.readonly? this._formDataFields.closeButtonText : this._closeButtonText}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onButtonTextChanged(e, this._closeButtonText)}">
                    </or-mwc-input>


                    <or-mwc-input 
                        label="${i18next.t('Priority')}"
                        type="${InputType.SELECT}"
                        .options="${['Normal', 'High']}"
                        ?disabled="${inputDisabled}"
                        id="notificationPriority"
                        .value="${live(this._formDataFields.priority || 'Normal')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => 
                            this._onFieldChanged('priority', e.detail.value)}"
                    ></or-mwc-input>
            </div>
               
                <div class="propContainer">
                    ${this.readonly ? html`
                        <div class="propContainer">
                        <h5>${i18next.t("Properties")}</h5>
                            ${this._getReadOnlyField("Source", this._formDataFields.source)}
                            ${this._getReadOnlyField("Status", this._formDataFields.status)}
                            ${this._getReadOnlyField("Sent", this._formDataFields.sent)}
                            ${this._getReadOnlyField("Delivered", this._formDataFields.delivered)}
                        </div>
                    ` : ''}
                </div>
                </div>
            </div>
        `;
    }

    protected _getReadOnlyField(label: string, value: string | undefined) {
        return html`
            <or-mwc-input 
                label="${i18next.t(label)}"
                type="${InputType.TEXT}"
                .value="${value || '-'}"
                ?disabled="${true}">
            </or-mwc-input>
        `;
    }

}