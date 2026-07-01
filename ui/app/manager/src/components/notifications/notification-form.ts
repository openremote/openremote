import {css, html, LitElement, unsafeCSS} from "lit";
import {globals} from "@openremote/theme";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import { i18next } from "@openremote/or-translate";
import manager, {Util} from "@openremote/core";
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
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import "@openremote/or-asset-tree";
import {OrVaadinSelect, SelectItem} from "@openremote/or-vaadin-components/or-vaadin-select";
// or-vaadin-checkbox-group registers the native vaadin-checkbox used for its children
import "@openremote/or-vaadin-components/or-vaadin-checkbox-group";
import "@openremote/or-vaadin-components/or-vaadin-text-field";
import "@openremote/or-vaadin-components/or-vaadin-text-area";

export type NotificationMessage = PushNotificationMessage | EmailNotificationMessage;

interface TargetOption {
    label: string;
    value: string;
}

export class NotificationFormChangedEvent extends CustomEvent<void> {
    static readonly NAME = "notification-form-changed";

    constructor() {
        super(NotificationFormChangedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

@customElement("notification-form")
export class NotificationForm extends LitElement {
    static styles = css`
        ${unsafeCSS(globals)}
        :host {
            height: 76vh;
            display: block;
        }

        .form-container {
            height: 100%;
        }

        or-vaadin-select,
        or-vaadin-checkbox-group,
        or-vaadin-text-field,
        or-vaadin-text-area {
            width: 100%;
            margin-bottom: 16px;
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
            gap: var(--lumo-space-m);
            height: 100%;
        }

        .formGridContainer-readonly {
            position: relative;
            display: grid;
            grid-template-columns: 1fr 2fr;
            grid-template-areas:
            "targetContainer messageContentContainer"
            "propContainer actionButtonContainer";
            gap: var(--lumo-space-m);
            height: 100%;
        }

        [class*="formGridContainer"] > * {
            background-color: white;
            border-radius: var(--lumo-border-radius-m);
            box-sizing: border-box;
            padding: var(--lumo-space-l);
        }

        .targetContainer {
            grid-area: targetContainer;
            position: absolute;
            width: 100%;
            height: 100%;
        }

        .messageContentContainer {
            grid-area: messageContentContainer;
            height: 100%;
        }

        .actionButtonContainer {
            grid-area: actionButtonContainer;
        }

        .propContainer {
            grid-area: propContainer;
        }

        .target-area {
            flex: 1 1 auto;
            overflow: auto;
        }

        h4 {
            margin: 0;
            padding-bottom: var(--lumo-space-m);
            font-size: var(--lumo-font-size-l);
            font-weight: 600;
            line-height: 125.303%;
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
    protected _targetOptions: TargetOption[] = [];

    @state()
    protected _selectedAssetIds: string[] = [];

    @property({type: Boolean})
    public disabled = false;

    @property({type: Boolean})
    public readonly = false;

    @property({type: Object})
    public notification?: SentNotification;

    // The realm to load target data for; supplied by the page from the store so it stays in sync with the app realm
    @property({type: String})
    public realm?: string;

    async firstUpdated() {
        // In readonly (details) mode the form only displays an existing notification's stored values; the
        // asset/user/realm option lists are only used by the editable target selectors, so skip loading them.
        if (this.readonly) {
            return;
        }

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
                label: asset.name,
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
        // Reload target data when the realm changes; skip the initial assignment (handled by firstUpdated) and
        // readonly mode (the details view doesn't use the option lists)
        if (!this.readonly && changedProps.has('realm') && changedProps.get('realm') !== undefined) {
            this._reloadTargetData();
        }
        // Notify listeners (e.g. the create dialog) so they can re-evaluate form validity
        if (!this.readonly) {
            this.dispatchEvent(new NotificationFormChangedEvent());
        }
    }

    /**
     * Resets the form to its initial empty state. Called when the create dialog is (re)opened, since the form
     * instance persists between opens. Target data isn't reloaded here — that's driven by the realm property.
     */
    public reset() {
        this._message = {type: "push"};
        this._targets = [];
        this._selectedAssetIds = [];
        const canReadAssets = manager.hasRole("read:assets") || manager.hasRole("read:admin");
        return this._onTargetTypeChanged(canReadAssets ? NotificationTargetType.ASSET : NotificationTargetType.USER);
    }

    /** Drops cached realm-specific target data and rebuilds the options for the current target type. */
    protected _reloadTargetData() {
        this._users = undefined;
        this._assets = undefined;
        this._realms = undefined;
        return this._onTargetTypeChanged(this._targetType);
    }

    protected async _loadUsers(): Promise<User[]> {
        if (!manager.hasRole("read:users") && !manager.hasRole("read:admin")) {
            return [];
        }
        try {
            const response = await manager.rest.api.UserResource.query({
                realmPredicate: { name: this.realm ?? manager.displayRealm },
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
                realm: { name: this.realm ?? manager.displayRealm }
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

    protected async _onTargetTypeChanged(type: NotificationTargetType) {
        if (!type) return;

        this._targetType = type;
        this._targetOptions = [];

        switch (type) {
            case NotificationTargetType.USER:
                if (!this._users) {
                    await this._loadUsers();
                }
                this._targetOptions = (this._users || []).map(user => ({
                    label: user.username,
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
                    label: realm,
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
            await this._onTargetTypeChanged(this._targetType);
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
                <h4><or-translate value="properties"></or-translate></h4>
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
                    <h4 style="flex:0 0 auto;"><or-translate value="notifications.target"></or-translate></h4>
                    ${this._renderReadOnlyField("notifications.targetType", this._normalizeValue(this._targetType))}
                    ${this._renderReadOnlyField("notifications.target", targetDisplay)}
                </div>
            `;
        }

        const allowedTargetTypes: SelectItem[] = [];
        if (manager.hasRole("read:assets") || manager.hasRole("read:admin")) {
            allowedTargetTypes.push({label: i18next.t("asset_plural"), value: NotificationTargetType.ASSET});
        }
        if (manager.hasRole("read:users") || manager.hasRole("read:admin")) {
            allowedTargetTypes.unshift({label: i18next.t("user_plural"), value: NotificationTargetType.USER});
        }
        if (!manager.isRestrictedUser() && manager.hasRole("read:admin")) {
            allowedTargetTypes.push({label: i18next.t("realm_plural"), value: NotificationTargetType.REALM});
        }

        return html`
            <div class="targetContainer">
                <h4 style="flex:0 0 auto;"><or-translate value="notifications.target"></or-translate></h4>
                <or-vaadin-select
                        style="flex:0 0 auto;"
                        id="targetType"
                        required
                        ?disabled="${inputDisabled || allowedTargetTypes.length === 1}"
                        .items="${allowedTargetTypes}"
                        value="${this._targetType}"
                        @change="${(ev: Event) => this._onTargetTypeChanged((ev.currentTarget as OrVaadinSelect).value as NotificationTargetType)}">
                    <or-translate slot="label" value="notifications.targetType"></or-translate>
                </or-vaadin-select>

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
                                <or-vaadin-checkbox-group
                                        id="target"
                                        required
                                        theme="vertical"
                                        ?disabled="${inputDisabled}"
                                        .value="${this._targets}"
                                        @value-changed="${(ev: CustomEvent) => {
                                            // Guard against the value-changed re-firing when we re-bind .value, which would recurse
                                            if (!Util.objectsEqual(ev.detail.value, this._targets, true)) {
                                                this._targets = [...ev.detail.value];
                                            }
                                        }}">
                                    <or-translate slot="label" value="notifications.target"></or-translate>
                                    ${this._targetOptions
                                            .sort(Util.sortByString((option: TargetOption) => option.label))
                                            .map(option => html`
                                                <vaadin-checkbox value="${option.value}" label="${option.label}"></vaadin-checkbox>
                                            `)}
                                </or-vaadin-checkbox-group>
                            `
                    )}
                </div>
            </div>
        `;
    }

    protected _renderMessageContentContainer(inputDisabled: boolean) {
        const messageTypeOptions: SelectItem[] = [
            {label: i18next.t("notifications.types.push"), value: "push"},
            {label: i18next.t("notifications.types.email"), value: "email"}
        ];

        return html`
            <div class="messageContentContainer">
                <h4><or-translate value="content"></or-translate></h4>
                <or-vaadin-select
                        id="messageType"
                        required
                        ?disabled="${inputDisabled}"
                        .items="${messageTypeOptions}"
                        value="${this._message.type}"
                        @change="${(ev: Event) => this._onMessageTypeChanged((ev.currentTarget as OrVaadinSelect).value as NotificationMessage["type"])}">
                    <or-translate slot="label" value="type"></or-translate>
                </or-vaadin-select>
                ${this._message.type === "push"
                        ? this._renderPushContent(this._message as PushNotificationMessage, inputDisabled)
                        : this._renderEmailContent(this._message as EmailNotificationMessage, inputDisabled)}
            </div>
        `;
    }

    protected _renderPushContent(message: PushNotificationMessage, inputDisabled: boolean) {
        return html`
            <or-vaadin-text-field
                    id="notificationTitle"
                    required
                    ?readonly="${inputDisabled}"
                    value="${message.title || ''}"
                    @change="${(ev: Event) => this._updateMessage({title: (ev.currentTarget as HTMLInputElement).value})}">
                <or-translate slot="label" value="title"></or-translate>
            </or-vaadin-text-field>

            <or-vaadin-text-area
                    id="notificationBody"
                    required
                    style="flex: 1;"
                    min-rows="4"
                    ?readonly="${inputDisabled}"
                    value="${message.body || ''}"
                    @change="${(ev: Event) => this._updateMessage({body: (ev.currentTarget as HTMLInputElement).value})}">
                <or-translate slot="label" value="body"></or-translate>
            </or-vaadin-text-area>
        `;
    }

    protected _renderEmailContent(message: EmailNotificationMessage, inputDisabled: boolean) {
        return html`
            <or-vaadin-text-field
                    id="notificationSubject"
                    required
                    ?readonly="${inputDisabled}"
                    value="${message.subject || ''}"
                    @change="${(ev: Event) => this._updateMessage({subject: (ev.currentTarget as HTMLInputElement).value})}">
                <or-translate slot="label" value="subject"></or-translate>
            </or-vaadin-text-field>

            <or-vaadin-text-area
                    id="notificationEmailBody"
                    required
                    style="flex: 1;"
                    min-rows="4"
                    ?readonly="${inputDisabled}"
                    value="${message.html || ''}"
                    @change="${(ev: Event) => this._updateMessage({html: (ev.currentTarget as HTMLInputElement).value})}">
                <or-translate slot="label" value="body"></or-translate>
            </or-vaadin-text-area>
        `;
    }

    protected _renderActionButtonContainer(inputDisabled: boolean) {
        const message = this._message as PushNotificationMessage;
        const priorityOptions: SelectItem[] = [
            {label: i18next.t('normal'), value: PushNotificationMessageMessagePriority.NORMAL},
            {label: i18next.t('high'), value: PushNotificationMessageMessagePriority.HIGH}
        ];

        return html`
            <div class="actionButtonContainer">
                <h4><or-translate value="actions"></or-translate></h4>
                <or-vaadin-text-field
                        id="actionUrl"
                        ?readonly="${inputDisabled}"
                        value="${message.action?.url || ''}"
                        @change="${(ev: Event) => {
                            const url = (ev.currentTarget as HTMLInputElement).value;
                            this._updateMessage({action: url ? {url, openInBrowser: true} : undefined});
                        }}">
                    <or-translate slot="label" value="openWebsiteUrl"></or-translate>
                </or-vaadin-text-field>

                <or-vaadin-text-field
                        id="openButtonText"
                        ?readonly="${inputDisabled}"
                        value="${message.buttons?.[0]?.title || ''}"
                        @change="${(ev: Event) => this._updateButton(0, (ev.currentTarget as HTMLInputElement).value)}">
                    <or-translate slot="label" value="buttonTextConfirm"></or-translate>
                </or-vaadin-text-field>

                <or-vaadin-text-field
                        id="closeButtonText"
                        ?readonly="${inputDisabled}"
                        value="${message.buttons?.[1]?.title || ''}"
                        @change="${(ev: Event) => this._updateButton(1, (ev.currentTarget as HTMLInputElement).value)}">
                    <or-translate slot="label" value="buttonTextDecline"></or-translate>
                </or-vaadin-text-field>

                <or-vaadin-select
                        id="notificationPriority"
                        ?disabled="${inputDisabled}"
                        .items="${priorityOptions}"
                        value="${message.priority || PushNotificationMessageMessagePriority.NORMAL}"
                        @change="${(ev: Event) => this._updateMessage({priority: (ev.currentTarget as OrVaadinSelect).value as PushNotificationMessageMessagePriority})}">
                    <or-translate slot="label" value="priority"></or-translate>
                </or-vaadin-select>
            </div>
        `;
    }

    protected _updateButton(index: number, title?: string) {
        const message = this._message as PushNotificationMessage;
        const buttons: (PushNotificationButton | undefined)[] = [...(message.buttons || [])];
        buttons[index] = title ? {title} : undefined;
        this._updateMessage({buttons: buttons as PushNotificationButton[]});
    }

    protected _renderReadOnlyField(label: string, value: string | undefined) {
        return html`
            <or-vaadin-text-field readonly value="${value || '-'}">
                <or-translate slot="label" value="${label}"></or-translate>
            </or-vaadin-text-field>
        `;
    }
}
