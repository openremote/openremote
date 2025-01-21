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

        /* Grid styles */

        .formGridContainer {
            display: grid; 
            grid-template-columns: 2fr 1fr; 
            grid-template-rows: 1fr 1fr 1fr 1fr; 
            gap: 8px 8px;  
            grid-template-areas: 
                "targetContainer actionButtonContainer"
                "messageContentContainer actionButtonContainer"
                "messageContentContainer propContainer"
                "messageContentContainer propContainer"; 
            }

        .targetContainer { grid-area: targetContainer; }
        .messageContentContainer { grid-area: messageContentContainer; }
        .actionButtonContainer { grid-area: actionButtonContainer; }
        .propContainer { grid-area: propContainer; }

        .panel-title {
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

    //TO DO ADD CHECKBOX HERE 
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
            this._users = response.data.filter(user => user.enabled && !user.serviceAccount);
            return this._users;
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

    private _populateFormFromNotification() {
        if (!this.notification) return; 

        const pushMessage = this.notification?.message as PushNotificationMessage;
        if (!pushMessage) return; 

        this._formDataFields = {
            title: pushMessage.title, 
            body: pushMessage.body,
            priority: pushMessage.priority || 'NORMAL',
            targetType: this.notification.targetId,
            actionUrl: pushMessage.action?.url,
            openButtonText: pushMessage.buttons?.[0]?.title,
            closeButtonText: pushMessage.buttons?.[1]?.title,
        };
        
        if (this.readonly) {
            this._formDataFields = {
                ...this._formDataFields,
                source: `${this.notification.source}, ${this.notification.sourceId ? `,${this.notification.sourceId}` : ''}`,
                status: this.notification.deliveredOn ? i18next.t("delivered") : i18next.t("pending"),
                sent: this.notification.sentOn ? new Date(this.notification.sentOn).toLocaleString() : '-',
                delivered: this.notification.deliveredOn ? new Date(this.notification.deliveredOn).toLocaleString() : '-'
            };
        }

        this.requestUpdate();
    }

    public getFormData(): NotificationFormData | null {
        if (this.readonly && this._formDataFields) {
            return this._formDataFields as NotificationFormData;
        }

        // for create mode
        const form = this.shadowRoot!;
        const inputs = {
            // name: form.querySelector<OrMwcInput>("#notificationName"),
            title: form.querySelector<OrMwcInput>("#notificationTitle"),
            body: form.querySelector<OrMwcInput>("#notificationBody"),
            priority: form.querySelector<OrMwcInput>("#notificationPriority"),
            targetType: form.querySelector<OrMwcInput>("#targetType"),
            target: form.querySelector<OrMwcInput>("#target"),
            // adding btns & url
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
            priority: inputs.priority?.value || "NORMAL",
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
                        <div class="section-title">${i18next.t("Target")}</div>
                         <or-mwc-input 
                            label="${i18next.t("Target type")}"
                            type="${InputType.SELECT}"
                            .options="${["USER", "ASSET", "REALM"]}"
                            ?disabled="${inputDisabled}"
                            required
                            style="width: 100%;"
                            id="targetType"
                            .value="${this._selectedTargetType}"
                            @or-mwc-input-changed="${this._onTargetTypeChanged}">
                        </or-mwc-input>
                        <p></p>
                        <or-mwc-input 
                            label="${i18next.t("Target")}"
                            type="${InputType.SELECT}"
                            
                            style="width: 100%;"
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
                <div class="section-title">${i18next.t("Content")}</div>
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

                    <p></p>

                     <or-mwc-input 
                        label="${i18next.t('Body')}"
                        type="${InputType.TEXTAREA}"
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
                    <div class="section-title">${i18next.t("Actions")}</div>
                    <or-mwc-input 
                        label="${i18next.t("URL to visit (optional)")}"
                        style="width: 100%;"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="actionUrl"
                        .value="${this._actionUrl}"
                        @or-mwc-input-changed="${this._onActionUrlChanged}">
                    </or-mwc-input>

                    <p></p>

                    <or-mwc-input 
                        label="${i18next.t("Open button text (optional)")}"
                        style="width: 100%;"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="openButtonText"
                        .value="${this._openButtonText}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onButtonTextChanged(e, this._openButtonText)}">
                    </or-mwc-input>

                    <p></p>

                    <or-mwc-input 
                        label="${i18next.t("Close button text (optional)")}"
                        style="width: 100%;"
                        type="${InputType.TEXT}"
                        ?disabled="${inputDisabled}"
                        id="closeButtonText"
                        .value="${this._closeButtonText}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onButtonTextChanged(e, this._closeButtonText)}">
                    </or-mwc-input>

                    <p></p>
                    <or-mwc-input 
                        label="${i18next.t('Priority')}"
                        type="${InputType.SELECT}"
                        .options="${['NORMAL', 'HIGH']}"
                        ?disabled="${inputDisabled}"
                        id="notificationPriority"
                        .value="${live(this._formDataFields.priority || 'NORMAL')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => 
                            this._onFieldChanged('priority', e.detail.value)}"
                    ></or-mwc-input>
            </div>
               
                <div class="propContainer">
                    ${this.readonly ? html`
                        <div class="propContainer">
                            <div class="section-title">${i18next.t("Properties")}</div>
                            ${this._getReadOnlyField("Source", this._formDataFields.source)}
                            <p></p>
                            ${this._getReadOnlyField("Status", this._formDataFields.status)}
                            <p></p>
                            ${this._getReadOnlyField("Sent", this._formDataFields.sent)}
                            <p></p>
                            ${this._getReadOnlyField("Delivered", this._formDataFields.delivered)}
                        </div>
                    ` : ''}
                </div>
                </div>
            </div>
        `;
    }

    protected _getField(label: string, inputType: InputType, isDisabled: boolean, id: string, rows: number, isRequired: boolean) {
        return html`
            <or-mwc-input 
                    label="${i18next.t(label)}"
                    type="${inputType}"
                    rows="${rows}"
                    style="width: 100%;"
                    ?disabled="${isDisabled}"
                    ?required="${isRequired}"
                    id="${id}">
                </or-mwc-input>
        `;
    }

    protected _getReadOnlyField(label: string, value: string | undefined) {
        return html`
            <or-mwc-input 
                label="${i18next.t(label)}"
                type="${InputType.TEXT}"
                .value="${value || '-'}"
                ?disabled="${true}"
                style="width: 100%;">
            </or-mwc-input>
        `;
    }

}