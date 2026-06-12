import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import manager, {DefaultColor3, Util} from "@openremote/core";
import {
    Asset,
    EmailNotificationMessage,
    Notification,
    NotificationTargetType,
    PushNotificationButton,
    PushNotificationMessage,
    PushNotificationMessageMessagePriority,
    SentNotification,
    User
} from "@openremote/model";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {live} from "lit/directives/live.js";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import "@openremote/or-asset-tree";
import {ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";

export type NotificationMessage = PushNotificationMessage | EmailNotificationMessage;

@customElement("notification-form")
export class NotificationForm extends LitElement {
    static styles = css`
        :host {
            height: 76vh;
            display: block;
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

        /* Lift the focused input above its siblings so open select menus aren't overlapped by the fields below */
        or-mwc-input:focus-within {
            z-index: 4;
        }

        h5 {
            margin-top: 12px;
            margin-bottom: 6px;
        }

        :host([readonly]) or-mwc-input {
            --mdc-text-field-fill-color: var(--or-app-color2);
            pointer-events: none;
        }

        :host([readonly]) .form-container {
            opacity: 0.9;
        }
    `;

    @state()
    protected _message: NotificationMessage = {type: "push"};

    @state()
    protected _targetType: NotificationTargetType = NotificationTargetType.ASSET;

    @state()
    protected _targets: string[] = [];

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

    async firstUpdated() {
        const canReadAssets = manager.hasRole("read:assets") || manager.hasRole("read:admin");

        const promises = [];
        if (canReadAssets) {
            promises.push(this._loadAssets());
        }
        if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            promises.push(this._loadUsers());
        }
        if (manager.hasRole("read:admin")) {
            promises.push(this._loadRealms());
        }
        await Promise.all(promises);

        if (canReadAssets) {
            this._targetOptions = (this._assets || []).map(asset => ({
                text: asset.name,
                value: asset.id
            }));
        } else if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            this._targetType = NotificationTargetType.USER;
        }
    }

    updated(changedProps: Map<string, any>) {
        if (changedProps.has('notification') && this.notification) {
            this._populateFromNotification()
        }
    }

    protected async _loadUsers(): Promise<User[]> {
        if (!manager.hasRole("read:users") && !manager.hasRole("read:admin")) {
            return [];
        }
        try {
            const response = await manager.rest.api.UserResource.query({
                realmPredicate: { name: manager.displayRealm },
                serviceUsers: false
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
        if (!manager.hasRole("read:assets") && !manager.hasRole("read:admin")) {
            return [];
        }
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

        this._targetType = type;
        this._targetOptions = [];

        switch (type) {
            case NotificationTargetType.USER:
                if (!this._users) {
                    await this._loadUsers();
                }
                this._targetOptions = (this._users || []).map(user => ({
                    text: user.username,
                    value: user.id
                }));
                break;

            case NotificationTargetType.ASSET:
                if (!this._assets) {
                    await this._loadAssets();
                }
                break;

            case NotificationTargetType.REALM:
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
        this._targets = this._selectedAssetIds;
    }

    protected _onMessageTypeChanged(type: NotificationMessage["type"]) {
        if (type === this._message.type) return;
        this._message = {type} as NotificationMessage;
    }

    protected _updateMessage(props: Partial<NotificationMessage>) {
        this._message = {...this._message, ...props} as NotificationMessage;
    }

    private _normalizeValue(value: string): string {
        let normalizedValue = '';
        if (value) {
            normalizedValue = value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
        }
        return normalizedValue;
    }

    private async _populateFromNotification() {
        if (!this.notification) return;

        this._message = (this.notification.message || {type: "push"}) as NotificationMessage;
        this._targetType = this.notification.target || NotificationTargetType.ASSET;
        this._targets = this.notification.targetId ? [this.notification.targetId] : [];

        // Load target options based on type
        if (this.readonly) {
            await this._onTargetTypeChanged({
                detail: {value: this._targetType}
            } as OrInputChangedEvent);
        }

        await this.requestUpdate();
    }

    /**
     * Builds a {@link Notification} from the current form state or returns null when required fields are missing.
     */
    public getNotification(): Notification | null {
        const name = this._message.type === "email"
            ? (this._message as EmailNotificationMessage).subject
            : (this._message as PushNotificationMessage).title;
        const body = this._message.type === "email"
            ? (this._message as EmailNotificationMessage).html
            : (this._message as PushNotificationMessage).body;

        if (!name || !body || !this._targetType || this._targets.length === 0) {
            return null;
        }

        let message = this._message;
        if (message.type === "push") {
            const push = {...message} as PushNotificationMessage;
            // Drop empty button slots and attach the action to the open button (index 0)
            const buttons = (push.buttons || []).filter(button => button?.title);
            if (buttons.length > 0) {
                if (push.action) {
                    buttons[0] = {...buttons[0], action: push.action};
                }
                push.buttons = buttons;
            } else {
                delete push.buttons;
            }
            message = push;
        }

        return {
            name,
            message,
            targets: this._targets.map(id => ({
                id,
                type: this._targetType
            }))
        };
    }

    protected render() {
        const inputDisabled = this.disabled || this.readonly;

        return html`
            <div class="form-container">
                <div class="${this.readonly ? "formGridContainer-readonly" : "formGridContainer"}">
                    ${this._renderTargetContainer(inputDisabled)}
                    ${this._renderMessageContentContainer(inputDisabled)}
                    ${this._message.type === "push" ? this._renderActionButtonContainer(inputDisabled) : ''}
                    ${this.readonly ? this._renderPropertiesContainer() : ''}
                </div>
            </div>
        `;
    }

    protected _renderPropertiesContainer() {
        if (!this.notification) return '';

        const canReadUsers = manager.hasRole("read:users") || manager.hasRole("read:admin");
        const source = (canReadUsers && this.notification.sourceId) ?
            `${this.notification.source}, ${this.notification.sourceId}` :
            this.notification.source;
        const status = this.notification.deliveredOn ? i18next.t("delivered") : i18next.t("pending");
        const sent = this.notification.sentOn ? new Date(this.notification.sentOn).toLocaleString() : '-';
        const delivered = this.notification.deliveredOn ? new Date(this.notification.deliveredOn).toLocaleString() : '-';

        return html`
            <div class="propContainer">
                <h5>${i18next.t("properties")}</h5>
                ${this._renderReadOnlyField("source", this._normalizeValue(source))}
                ${this._renderReadOnlyField("status", status)}
                ${this._renderReadOnlyField("sent", sent)}
                ${this._renderReadOnlyField("delivered", delivered)}
            </div>
        `;
    }

    // TODO: Make the Target field similar to the cell in the table: icon with asset/user/realm name instead of ID
    protected _renderTargetContainer(inputDisabled: boolean) {
        if (inputDisabled) {
            const canSeeTargetId = manager.hasRole("read:admin")
                || (this._targetType === NotificationTargetType.USER && manager.hasRole("read:users"))
                || (this._targetType === NotificationTargetType.ASSET && manager.hasRole("read:assets"));
            const targetDisplay = canSeeTargetId ? this._normalizeValue(this._targets[0]) : '-';
            return html`
                <div class="targetContainer">
                    <h5 style="flex:0 0 auto;">${i18next.t("notifications.target")}</h5>
                    ${this._renderReadOnlyField("notifications.targetType", this._normalizeValue(this._targetType))}
                    ${this._renderReadOnlyField("notifications.target", targetDisplay)}
                </div>
            `;
        }

        const allowedTargetTypes: [NotificationTargetType, string][] = [];
        if (manager.hasRole("read:assets") || manager.hasRole("read:admin")) {
            allowedTargetTypes.push([NotificationTargetType.ASSET, i18next.t("asset_plural")]);
        }
        if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            allowedTargetTypes.unshift([NotificationTargetType.USER, i18next.t("user_plural")]);
        }
        if (!manager.isRestrictedUser() && manager.hasRole("read:admin")) {
            allowedTargetTypes.push([NotificationTargetType.REALM, i18next.t("realm_plural")]);
        }

        return html`
            <div class="targetContainer">
                <h5 style="flex:0 0 auto;">${i18next.t("notifications.target")}</h5>
                <or-mwc-input
                        style="flex:0 0 auto;"
                        label="${i18next.t("notifications.targetType")}"
                        type="${InputType.SELECT}"
                        .options="${allowedTargetTypes}"
                        ?disabled="${inputDisabled}"
                        required
                        id="targetType"
                        .value="${this._targetType}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onTargetTypeChanged(e)}">
                </or-mwc-input>

                <div class="target-area">
                    ${when(this._targetType === NotificationTargetType.ASSET,
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
                                        .values="${this._targets}"
                                        .listItems="${this._targetOptions.sort(Util.sortByString(option => option.text))}"
                                        @or-mwc-list-changed="${(e: OrMwcListChangedEvent) => {
                                            this._targets = e.detail.map(item => item.value);
                                        }}">
                                </or-mwc-list>
                            `
                    )}
                </div>
            </div>
        `;
    }

    protected _renderMessageContentContainer(inputDisabled: boolean) {
        const messageTypeOptions: [NotificationMessage["type"], string][] = [
            ["push", i18next.t("notifications.types.push")],
            ["email", i18next.t("notifications.types.email")]
        ];

        return html`
            <div class="messageContentContainer">
                <h5>${i18next.t("content")}</h5>
                <or-mwc-input
                        label="${i18next.t("type")}"
                        type="${InputType.SELECT}"
                        .options="${messageTypeOptions}"
                        ?disabled="${inputDisabled}"
                        required
                        id="messageType"
                        .value="${this._message.type}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onMessageTypeChanged(e.detail.value)}">
                </or-mwc-input>
                ${this._message.type === "push"
                        ? this._renderPushContent(this._message as PushNotificationMessage, inputDisabled)
                        : this._renderEmailContent(this._message as EmailNotificationMessage, inputDisabled)}
            </div>
        `;
    }

    protected _renderPushContent(message: PushNotificationMessage, inputDisabled: boolean) {
        return html`
            <or-mwc-input
                    label="${i18next.t('title')}"
                    type="${InputType.TEXT}"
                    ?readonly="${inputDisabled}"
                    id="notificationTitle"
                    required
                    .value="${live(message.title || '')}"
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                            this._updateMessage({title: e.detail.value})}"
            ></or-mwc-input>

            <or-mwc-input
                    label="${i18next.t('body')}"
                    type="${InputType.TEXTAREA}"
                    style="display: flex; flex: 1; --mdc-text-field-height: 100%;"
                    rows="4"
                    ?readonly="${inputDisabled}"
                    id="notificationBody"
                    required
                    .value="${live(message.body || '')}"
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                            this._updateMessage({body: e.detail.value})}"
            ></or-mwc-input>
        `;
    }

    protected _renderEmailContent(message: EmailNotificationMessage, inputDisabled: boolean) {
        return html`
            <or-mwc-input
                    label="${i18next.t('subject')}"
                    type="${InputType.TEXT}"
                    ?readonly="${inputDisabled}"
                    id="notificationSubject"
                    required
                    .value="${live(message.subject || '')}"
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                            this._updateMessage({subject: e.detail.value})}"
            ></or-mwc-input>

            <or-mwc-input
                    label="${i18next.t('body')}"
                    type="${InputType.TEXTAREA}"
                    style="display: flex; flex: 1; --mdc-text-field-height: 100%;"
                    rows="4"
                    ?readonly="${inputDisabled}"
                    id="notificationEmailBody"
                    required
                    .value="${live(message.html || '')}"
                    @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                            this._updateMessage({html: e.detail.value})}"
            ></or-mwc-input>
        `;
    }

    protected _renderActionButtonContainer(inputDisabled: boolean) {
        const message = this._message as PushNotificationMessage;

        return html`
            <div class="actionButtonContainer">
                <h5>${i18next.t("actions")}</h5>
                <or-mwc-input
                        label="${i18next.t("notifications.urlToVisit")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="actionUrl"
                        .value="${live(message.action?.url || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                                this._updateMessage({action: e.detail.value ? {url: e.detail.value, openInBrowser: true} : undefined})}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t("notifications.openButtonText")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="openButtonText"
                        .value="${live(message.buttons?.[0]?.title || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._updateButton(0, e.detail.value)}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t("notifications.closeButtonText")}"
                        type="${InputType.TEXT}"
                        ?readonly="${inputDisabled}"
                        id="closeButtonText"
                        .value="${live(message.buttons?.[1]?.title || '')}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._updateButton(1, e.detail.value)}">
                </or-mwc-input>

                <or-mwc-input
                        label="${i18next.t('priority')}"
                        type="${InputType.SELECT}"
                        .options="${[
                            [PushNotificationMessageMessagePriority.NORMAL, i18next.t('normal')],
                            [PushNotificationMessageMessagePriority.HIGH, i18next.t('high')]
                        ]}"
                        ?readonly="${inputDisabled}"
                        id="notificationPriority"
                        .value="${live(message.priority || PushNotificationMessageMessagePriority.NORMAL)}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) =>
                                this._updateMessage({priority: e.detail.value})}"
                ></or-mwc-input>
            </div>
        `;
    }

    protected _updateButton(index: number, title?: string) {
        const message = this._message as PushNotificationMessage;
        const buttons: (PushNotificationButton | undefined)[] = [...(message.buttons || [])];
        buttons[index] = title ? {title} : undefined;
        this._updateMessage({buttons: buttons as PushNotificationButton[]});
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
