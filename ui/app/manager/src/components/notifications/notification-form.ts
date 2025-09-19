import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import manager, {DefaultColor3, Util} from "@openremote/core";
import {Asset, PushNotificationMessage, SentNotification, User} from "@openremote/model";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {live} from "lit/directives/live.js";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import "@openremote/or-asset-tree";
import {ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";

export interface NotificationFormData {
    name: string;
    title: string;
    body: string;
    priority: string;
    targetType: string;
    targets: string[]; // The IDs used for sending messages.
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
            height: 76vh;
            display: block;
            min-width: 1024px;
        }

        .form-container {
            display: flex;
            flex-direction: column;
            gap: 16px;
            height: 100%;
            padding: 0 16px;
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

        or-asset-tree {
            flex: 0 0 auto;
        }

        /* Grid styles */

        .formGridContainer {
            position: relative;
            display: grid;
            grid-template-columns: 1fr 2fr;
            grid-template-areas:
            "targetContainer messageContentContainer"
            "targetContainer actionButtonContainer";
            gap: 8px;
            height: 100%;
        }

        .formGridContainer-readonly {
            position: relative;
            display: grid;
            grid-template-columns: 1fr 2fr;
            grid-template-areas:
            "targetContainer messageContentContainer"
            "propContainer actionButtonContainer";
            gap: 8px;
            height: 100%;
        }

        .targetContainer {
            grid-area: targetContainer;
            position: absolute;
            display: flex;
            flex-direction: column;
            z-index: 3;
            height: 100%;
            width: 100%;
        }

        .messageContentContainer {
            grid-area: messageContentContainer;
            display: flex;
            flex-direction: column;
            height: 100%;
        }

        .actionButtonContainer {
            grid-area: actionButtonContainer;
        }

        .propContainer {
            grid-area: propContainer;
        }

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

        .target-area {
            flex: 1 1 auto;
            z-index: 1;
            overflow: auto;
        }

        or-mwc-input {
            position: relative;
            z-index: 2;
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
    }

    connectedCallback() {
        super.connectedCallback();
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
    protected formData: NotificationFormData = {
        name: '',
        title: '',
        body: '',
        priority: 'Normal',
        targetType: 'Asset',
        targets: [''],
        actionUrl: '',
        openButtonText: '',
        closeButtonText: ''
    };

    @state()
    protected _users?: User[];

    @state()
    protected _assets?: Asset[];

    @state()
    protected _realms?: string[];

    @state()
    protected _targetOptions: { text: string, value: string }[] = [];

    @state()
    protected _selectedAssetIds: string[] = [];

    @property({type: Boolean})
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

    protected _onFieldChanged(field: keyof NotificationFormData, value: any) {
        this.formData = {
            ...this.formData,
            [field]: value
        }
    }

    protected async _loadUsers(): Promise<User[]> {
        try {
            const response = await manager.rest.api.UserResource.query({
                realmPredicate: {name: manager.displayRealm}
            });
            // filter out keycloak service account
            return this._users = response.data.filter((u) => u.username !== 'manager-keycloak');
        } catch (error) {
            console.error("Failed to load users:", error);
            showSnackbar(undefined, i18next.t("loadingUsersFailed"));
            return [];
        }
    }

    protected async _loadAssets(): Promise<Asset[]> {
        try {
            const response = await manager.rest.api.AssetResource.queryAssets({
                realm: {name: manager.displayRealm}
            });
            this._assets = response.data;
            return response.data;
        } catch (error) {
            console.error("Failed to load assets:", error);
            showSnackbar(undefined, i18next.t("loadingAssetsFailed"));
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
            showSnackbar(undefined, i18next.t("loadingRealmsFailed"));
            return [];
        }
    }

    protected async _onTargetTypeChanged(e: OrInputChangedEvent) {
        const type = e.detail.value;
        if (!type) return;

        this.formData.targetType = type;
        this._targetOptions = [];

        switch (type) {
            case "User":
                if (!this._users) {
                    await this._loadUsers();
                }
                this._targetOptions = (this._users || []).map(user => ({
                    text: user.username,
                    value: user.id
                }));
                break;

            case "Asset":
                if (!this._assets) {
                    await this._loadAssets();
                }
                break;

            case "Realm":
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

    protected _onAssetSelectionChanged(e: OrAssetTreeSelectionEvent): void {
        this._selectedAssetIds = e.detail.newNodes.map(node => node.asset.id);
        this._onFieldChanged('targets', this._selectedAssetIds);
    }

    private _normalizeValue(value: string): string {
        let normalizedValue = '';
        if (value) {
            normalizedValue = value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
        }
        return normalizedValue;
    }

    private async _populateFormFromNotification() {
        if (!this.notification) return;

        const pushMessage = this.notification?.message as PushNotificationMessage;
        if (!pushMessage) return;

        const normalizedPriority = this._normalizeValue(pushMessage.priority || 'NORMAL');


        this.formData = {
            name: this.notification.name || '',
            title: pushMessage.title,
            body: pushMessage.body || '',
            priority: normalizedPriority,
            targetType: this.notification.target || '',
            targets: [this.notification.targetId] || [],
            actionUrl: pushMessage.action?.url,
            openButtonText: pushMessage.buttons?.[0]?.title,
            closeButtonText: pushMessage.buttons?.[1]?.title,
        };

        if (this.readonly) {
            this.formData = {
                ...this.formData,
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

        // Load target options based on type
        if (this.readonly) {
            await this._onTargetTypeChanged({
                detail: {value: this.formData.targetType}
            } as OrInputChangedEvent);
        }

        await this.requestUpdate();
    }

    public getFormData(): NotificationFormData | null {
        if (this.readonly) {
            return this.formData;
        }

        // for create mode
        const form = this.shadowRoot!;
        const inputs = {
            title: form.querySelector<OrMwcInput>("#notificationTitle"),
            body: form.querySelector<OrMwcInput>("#notificationBody"),
            priority: form.querySelector<OrMwcInput>("#notificationPriority"),
            targetType: form.querySelector<OrMwcInput>("#targetType"),
            targets: form.querySelector<OrMwcInput>("#target"),

            actionUrl: form.querySelector<OrMwcInput>("#actionUrl"),
            openButton: form.querySelector<OrMwcInput>("#openButtonText"),
            closeButton: form.querySelector<OrMwcInput>("#closeButtonText")
        };

        if (!inputs.title?.value || !inputs.body?.value) {
            return null;
        }

        return {
            name: inputs.title.value,
            title: inputs.title.value,
            body: inputs.body.value,
            priority: inputs.priority?.value || this.formData.priority || "Normal",
            targetType: inputs.targetType?.value || this.formData.targetType,
            targets: inputs.targets?.value || this.formData.targets,
            actionUrl: inputs.actionUrl?.value || '',
            openButtonText: inputs.openButton?.value || '',
            closeButtonText: inputs.closeButton?.value || ''
        };
    }


    protected render() {
        const inputDisabled = this.disabled || this.readonly;

        return html`
            <div class="form-container">
                <div class="${this.readonly ? "formGridContainer-readonly" : "formGridContainer"}">
                    ${this._renderTargetContainer(inputDisabled)}
                    ${this._renderMessageContentContainer(inputDisabled)}
                    ${this._renderActionButtonContainer(inputDisabled)}
                    ${this.readonly ? this._renderPropertiesContainer() : ''}
                </div>
            </div>
        `;
    }

    protected _renderPropertiesContainer() {
        return html`
            <div class="propContainer">
                <h5>${i18next.t("properties")}</h5>
                ${this._renderReadOnlyField("source", this._normalizeValue(this.formData.source))}
                ${this._renderReadOnlyField("status", this.formData.status)}
                ${this._renderReadOnlyField("sent", this.formData.sent)}
                ${this._renderReadOnlyField("delivered", this.formData.delivered)}
            </div>
        `;
    }

    // TODO: Make the Target field similar to the cell in the table: icon with asset/user/realm name instead of ID
    protected _renderTargetContainer(inputDisabled: boolean) {
        if (inputDisabled) {
            return html`
                <div class="targetContainer">
                    <h5 style="flex:0 0 auto;">${i18next.t("notifications.target")}</h5>
                    ${this._renderReadOnlyField("notifications.targetType", this._normalizeValue(this.formData.targetType))}
                    ${this._renderReadOnlyField("notifications.target", this._normalizeValue(this.formData.targets[0]))}
                </div>
            `;
        }

        return html`
            <div class="targetContainer">
                <h5 style="flex:0 0 auto;">${i18next.t("notifications.target")}</h5>
                <or-mwc-input
                        style="flex:0 0 auto;"
                        label="${i18next.t("notifications.targetType")}"
                        type="${InputType.SELECT}"
                        .options="${["Asset", "User", "Realm"]}"
                        ?disabled="${inputDisabled}"
                        required
                        id="targetType"
                        .value="${this.formData.targetType}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this._onFieldChanged('targetType', e.detail.value);
                            this._onTargetTypeChanged(e);
                        }}">
                </or-mwc-input>

                <div class="target-area">
                    ${when(this.formData.targetType === "Asset",
                            () => html`
                                <or-asset-tree
                                        id="asset-selector"
                                        .selectedIds="${this._selectedAssetIds}"
                                        .showSortBtn="${false}"
                                        expandNodes
                                        checkboxes
                                        @or-asset-tree-selection="${(e: OrAssetTreeSelectionEvent) => this._onAssetSelectionChanged(e)}"
                                ></or-asset-tree>
                            `,
                            () => html`
                                <or-mwc-list
                                        label="${i18next.t("notifications.target")}"
                                        type="${ListType.MULTI_CHECKBOX}"
                                        ?disabled="${inputDisabled || !this._targetOptions}"
                                        required
                                        id="target"
                                        .values="${this.formData.targets}"
                                        .listItems="${this._targetOptions.sort(Util.sortByString(option => option.text))}"
                                        @or-mwc-list-changed="${(e: OrMwcListChangedEvent) => {
                                            this._onFieldChanged('targets', e.detail.map(id => id.value))
                                        }}">
                                </or-mwc-list>
                            `
                    )}
                </div>
            </div>
        `;
    }

    protected _renderMessageContentContainer(inputDisabled: boolean) {
        return html`
            <div class="messageContentContainer">
                <h5>${i18next.t("Content")}</h5>
                <or-mwc-input
                        label="${i18next.t('title')}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="notificationTitle"
                        required
                        .value="${live(this.formData.title || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                                this._onFieldChanged('title', e.detail.value)}"
                ></or-mwc-input>

                <or-mwc-input
                        label="${i18next.t('body')}"
                        type="${InputType.TEXTAREA}"
                        style="display: flex; flex: 1; --mdc-text-field-height: 100%;"
                        rows="4"
                        ?readonly="${inputDisabled}"
                        id="notificationBody"
                        required
                        .value="${live(this.formData.body || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                                this._onFieldChanged('body', e.detail.value)}"
                ></or-mwc-input>
            </div>
        `;

    }

    protected _renderActionButtonContainer(inputDisabled: boolean) {
        return html`
            <div class="actionButtonContainer">
                <h5>${i18next.t("Actions")}</h5>
                <or-mwc-input
                        label="${i18next.t("notifications.urlToVisit")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="actionUrl"
                        .value="${this.formData.actionUrl || ''}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onFieldChanged('actionUrl', e.detail.value)}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t("notifications.openButtonText")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="openButtonText"
                        .value="${this.formData.openButtonText || ''}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onFieldChanged('openButtonText', e.detail.value)}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t("notifications.closeButtonText")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="closeButtonText"
                        .value="${this.formData.closeButtonText || ''}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onFieldChanged('closeButtonText', e.detail.value)}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t('priority')}"
                        type="${InputType.SELECT}"
                        .options="${['Normal', 'High']}"
                        ?readonly="${inputDisabled}"
                        id="notificationPriority"
                        .value="${live(this.formData.priority || 'Normal')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                                this._onFieldChanged('priority', e.detail.value)}"
                ></or-mwc-input>
            </div>
        `;
    }

    protected _renderReadOnlyField(label: string, value: string | string[] | undefined) {
        return html`
            <or-mwc-input
                    label="${i18next.t(label)}"
                    type="${InputType.TEXT}"
                    .value="${value || '-'}"
                    ?readonly="${true}">
            </or-mwc-input>
        `;
    }
}
