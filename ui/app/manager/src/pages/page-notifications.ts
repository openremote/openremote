import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {
    Notification,
    NotificationSendResult,
    SentNotification,
    EmailNotificationMessage,
    PushNotificationMessage,
    AbstractNotificationMessage,
    NotificationSource,
    Asset,
    User,
    PushNotificationMessageMessagePriority,
    PushNotificationMessageTargetType,
    NotificationTargetType,
    RepeatFrequency
} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor4} from "@openremote/core";
import i18next from "i18next";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {getNotificationsRoute} from "../routes";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {guard} from "lit/directives/guard.js";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAssetTreeRequestSelectionEvent, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {
    OrMwcTable,
    OrMwcTableRowClickEvent,
    OrMwcTableRowSelectEvent
} from "@openremote/or-mwc-components/or-mwc-table";

class LocalNotificationManager {
    private static readonly STORAGE_KEY = 'local_notifications';

    static getNotifications(): SentNotification[] {
        //get item from local storage
        // if stored return parsed version of the stored notiff
        const stored = localStorage.getItem(this.STORAGE_KEY);
        if (stored) {
            return JSON.parse(stored);
        }
        return [];
    }

    static saveNotifications(notifications: SentNotification[]) {
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(notifications));
    }

    static addNotification(notification: SentNotification) {
        const notifications = this.getNotifications();

        const SentNotification: SentNotification = {
            id: Date.now(), // use timestamp as id for now
            name: notification.name, 
            type: notification.message.type,
            message: notification.message,
            sentOn: Date.now(), 
            target: notification.target || NotificationTargetType.USER,
            targetId: notification.targetId || "",
            source: NotificationSource.CLIENT,
            sourceId: "local", 
            acknowledgedOn: undefined,
            deliveredOn: undefined
        };

        notifications.unshift(SentNotification);
        this.saveNotifications(notifications);

        return SentNotification;

    }

    static deleteNotification(id: number) {
        const notifications = this.getNotifications().filter(n => n.id !== id);
        this.saveNotifications(notifications);
    }

    static markAsDelivered (id: number) {
        const notifications = this.getNotifications().map(n => {
            if (n.id == id) {
                return {...n, deliveredOn: Date.now()}; //TODO: come back for the delivered logic
            }

            return n;
        });

        this.saveNotifications(notifications);
    }

    static clearAll() {
        //not sure if this is needed atm 
    }
}

export interface PageNotificationsConfig {
    targetId?: string;
    targetType?: string;
    fromTimestamp?: number;
    toTimestamp?: number;
}

export function pageNotificationsProvider(store: Store<AppStateKeyed>, config?: PageNotificationsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "notifications",
        routes: ["notifications"],
        pageCreator: () => {
            return new PageNotifications(store);
        }
    };
}

/*
- Interface to add UI-specific state management
- Used to prevent (?) duplicate loading requests
- Track asset associations/modifying asset links
- Current PoC doesn't include modifying notifications
*/
// interface AlarmModel extends SentAlarm {
//     loaded?: boolean;
//     loading?: boolean;
//     alarmAssetLinks?: AlarmAssetLink[];
//     previousAssetLinks?: AlarmAssetLink[];
// }

@customElement("page-notifications")
export class PageNotifications extends Page<AppStateKeyed> {
    static get styles() {
        return [
            css`
                :host {
                    flex: 1;
                    width: 100%;
                }

                #wrapper {
                    height: 100%;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    overflow: auto;
                }

                #title {
                    padding: 0 20px;
                    font-size: 18px;
                    font-weight: bold;
                    margin: 10px 0;
                    display: flex;
                    align-items: center;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    justify-content: space-between;
                }

                #title-alarm {
                    display: flex;
                    align-items: center;
                    justify-content: flex-start;
                    width: calc(100% - 52px);
                    max-width: 1000px;
                    margin: 20px auto;
                    font-size: 18px;
                    font-weight: bold;
                }

                .breadcrumb-container {
                    width: calc(100% - 52px);
                    max-width: 1000px;
                    margin: 10px 20px 0 34px;
                    display: flex;
                    align-items: center;
                }

                .breadcrumb-clickable {
                    cursor: pointer;
                    color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
                }

                .breadcrumb-arrow {
                    margin: 0 5px -3px 5px;
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
                }

                .panel {
                    width: calc(100% - 100px);
                    max-width: 1000px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: auto;
                    padding: 12px 24px 24px;
                }

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

                .alarm-input {
                    margin-bottom: 12px;
                }

                #status-select, #severity-select {
                    width: 180px;
                }

                #controls {
                    flex: 0;
                    display: flex;
                    flex-direction: row;
                    justify-content: space-between;
                    align-items: center;
                }

                .controls-left {
                    display: flex;
                    align-items: center;
                    width: 100%;
                }

                .controls-left > * {
                    padding-right: 20px;
                }

                h5 {
                    margin-top: 12px;
                    margin-bottom: 6px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                }

                #table-container {
                    margin-left: 20px;
                    margin-right: 20px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    flex-wrap: wrap;
                    margin: auto;
                    flex: 1 1 0;
                    gap: 16px;
                    width: 100%;
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                }

                #details-panel {
                    min-width: 66%;
                    height: max-content;
                }

                #prop-panel {
                    min-width: 32%;
                    height: fit-content;
                }

                .hidden {
                    display: none;
                    margin: 0 !important;
                }

                #title.hidden {
                    display: none;
                    margin: 0 !important;
                }
                
                // additional table styling

                .table {
                width: calc(100% - 40px);
                margin: 20px;
                overflow-x: auto;
            }

            table {
                width: 100%;
                border-collapse: collapse;
                background-color: white;
                border-radius: 4px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.12);
            }

            th {
                background-color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                color: white;
                text-align: left;
                padding: 12px 16px;
                font-weight: 500;
            }

            td {
                padding: 12px 16px;
                border-top: 1px solid #e0e0e0;
            }

            tbody tr:hover {
                background-color: rgba(0,0,0,0.04);
            }
            
            .dialog-content {
                display: flex;
                flex-direction: column;
                gap: 16px;
                padding: 0 24px;
            }

            .dialog-content or-mwc-input {
                margin-bottom: 16px;
            }
                
            `];
    }

    @property()
    public realm?: string;

    @state()
    protected _data?: SentNotification[];
    @state()
    protected _isLocal: boolean = true; 

    protected _loading: boolean = false;

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    // dont remove this
    get name(): string {
        return "notification.notification_plural"
    }

    public stateChanged(state: AppStateKeyed): void {
        if (state.app.page == "notifications") {
            if (this.realm === undefined || this.realm == state.app.realm) {
                this.reset();
            } else {
                this.realm = state.app.realm;
                this.requestUpdate('realm');
            }
        }
    }

    protected reset(): void {
        this._data = undefined;
        this._loadLocal();
        // TODO: add proper refresh logic

        // if (this._refresh) {
        //     window.clearTimeout(this._refresh);
        // }

        this.requestUpdate();
    }

    public shouldUpdate(changedProperties: PropertyValues): boolean {
        // if changed props has realm this._loadData()
        return super.shouldUpdate(changedProperties);
    }

    public connectedCallback(): void {
        super.connectedCallback();
        this.realm = this.getState().app.realm;
        this._loadLocal();
    }

    //TODO add pagination
    //OG loadData method
    private async _loadData() {
        if (this._loading) {
            return;
        }

        // if (this._loading || (!manager.hasRole("read:admin") && !manager.hasRole("write:admin"))) {
        //     return;
        // }

        this._loading = true;

        try {

            
            const response = await manager.rest.api.NotificationResource.getNotifications({
                realmId: manager.displayRealm
            });
            console.log("in _loadData method");
            console.log("Get notifications response:", response);
            console.log("Response status:", response.status);
            console.log("Response data:", response.data);

            if (response.status === 200) {
                this._data = response.data;

                // Only show push notifications
                this._data = this._data.filter((notification) => 
                    notification.message.type === "push"
                );
                console.log("Filtered notifications:", this._data);
            }
        } catch (error) {
            console.error("Failed to load notifications:", error);
            if (isAxiosError(error)) {
                console.error("Error details:", error.response?.data)
            }
            showSnackbar(undefined, i18next.t("notification.loadFailed"), i18next.t("dismiss"));
        } finally {
            this._loading = false;
        }
    }

    private async _loadLocal() {
        if (this._loading) {
            return; 
        }

        this._loading = true;

        try {
            this._data = LocalNotificationManager.getNotifications();

            // could add filtering for push notifications here

            console.log("Loaded local notifications:", this._data);
        } catch (error) {
            console.error("Failed to load notifications:", error);
            showSnackbar(undefined, i18next.t("Loading for notifications failed"), i18next.t("Dismiss"));
        } finally {
            this._loading = false; 
        }
    }
    
    private async _sendTestNotification() {
        const message: PushNotificationMessage = {
            type: "push",
            title: "Test Notification",
            body: "This is a test notification", 
            priority: PushNotificationMessageMessagePriority.HIGH,
            // additional fields to check if any of these below are causing an issue
            targetType: PushNotificationMessageTargetType.TOPIC,
            data: {}, 
            buttons: []
        }

        const notification: Notification = {
            name: "Test notification", 
            message: message, 
            targets: [{
                type: NotificationTargetType.USER, 
                id: manager.username,
                data: {}
            }],
            repeatFrequency: RepeatFrequency.ONCE
        }

        try {
            console.log("Sending notification:", JSON.stringify(notification, null, 2));
            const response = await manager.rest.api.NotificationResource.sendNotification(notification);
            console.log("Notification response:", response);
            showSnackbar(undefined, i18next.t("Notification successfully created!"));
        } catch (error) {
            console.error("Failed to send notification", error);
            if (isAxiosError(error)) {
                console.error("Request payload:", error.config?.data);
                console.error("Response status:", error.response?.status);
                console.error("Response data:", error.response?.data)
            }
            showSnackbar(undefined, i18next.t("Notification creation failed."))
        }
    }

    private async _sendTestEmailNotification() {
        const message: EmailNotificationMessage = {
            type: "email",
            subject: "Test Notification",
            text: "This is a test notification", 
            html: "<p>This is a test notification</p>",
            to: [{
                name: "Test User",
                address: manager.username
            }]
        }

        const notification: Notification = {
            name: "Test notification", 
            message: message, 
            targets: [{
                type: NotificationTargetType.USER, 
                id: manager.username,
                data: {}
            }],
            repeatFrequency: RepeatFrequency.ONCE
        }

        try {
            const headers = {
                "org.openremote.model.notification.Notification.SOURCE": NotificationSource.CLIENT,
                "Accept": "application/json",
                "Content-Type": "application/json"
            }

            const requestParams = {
                headers: headers,
                debug: true, // Add debug flag
                realm: manager.displayRealm // Explicitly include realm
            };

            console.log("Current user:", manager.username);
            console.log("Current realm:", manager.displayRealm);
            console.log("Headers being sent:", headers);
            console.log("Full notification payload:", JSON.stringify(notification, null, 2));
            console.log("Request params:", requestParams);

            const response = await manager.rest.api.NotificationResource.sendNotification(notification);
            console.log("Notification response:", response);
            showSnackbar(undefined, i18next.t("Notification successfully created!"));
        } catch (error) {
            console.error("Failed to send notification", error);
            if (isAxiosError(error)) {
                console.error("Request payload:", error.config?.data);
                console.error("Response status:", error.response?.status);
                console.error("Response data:", error.response?.data)
            }
            showSnackbar(undefined, i18next.t("Notification creation failed."))
        }
    }

    protected _getDialogHTML() {
        return html`
        <div class="dialog-content">
            <or-mwc-input 
                label="${i18next.t("Name")}"
                type="${InputType.TEXT}"
                style="width: 100%;"
                required
                id="notificationName">
            </or-mwc-input>
            
            <or-mwc-input 
                label="${i18next.t("Title")}"
                type="${InputType.TEXT}"
                style="width: 100%;"
                required
                id="notificationTitle">
            </or-mwc-input>
            
            <or-mwc-input 
                label="${i18next.t("Body")}"
                type="${InputType.TEXTAREA}"
                rows="4"
                style="width: 100%;"
                required
                id="notificationBody">
            </or-mwc-input>

            <or-mwc-input 
                label="${i18next.t("Priority")}"
                type="${InputType.SELECT}"
                .options="${["NORMAL", "HIGH"]}"
                required
                style="width: 100%;"
                id="notificationPriority">
            </or-mwc-input>

            <or-mwc-input 
                label="${i18next.t("Target type")}"
                type="${InputType.SELECT}"
                .options="${["DEVICE", "TOPIC", "CONDITION"]}"
                required
                style="width: 100%;"
                id="targetType">
            </or-mwc-input>

            <or-mwc-input 
                label="${i18next.t("Target")}"
                type="${InputType.TEXT}"
                style="width: 100%;"
                required
                id="target">
            </or-mwc-input>
        </div>
    `
    }

    protected async _handleCreateNotification(dialog: OrMwcDialog) {
        const nameInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#notificationName");
        const titleInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#notificationTitle");
        const bodyInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#notificationBody");
        const priorityInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#notificationPriority");
        const targetTypeInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#targetType");
        const targetInput = dialog.shadowRoot?.querySelector<OrMwcInput>("#target");

        // sanity check
        if (!nameInput?.value || !titleInput?.value || !bodyInput?.value) {
            showSnackbar(undefined, i18next.t("Please fill in all required fields"));
            return;
        }

        const message: PushNotificationMessage = {
            type: "push",
            title: titleInput.value,
            body: bodyInput.value,
            priority: priorityInput.value as PushNotificationMessageMessagePriority,
            targetType: targetTypeInput.value as PushNotificationMessageTargetType,
            target: targetInput.value,
            data: {},
            buttons: []
        }

        const notification: Notification = {
            name: nameInput.value,
            message: message,
            targets: [{
                type: NotificationTargetType.USER,
                id: manager.username,
                data: {}
            }],
            repeatFrequency: RepeatFrequency.ONCE
        };

        try {
            const SentNotification = LocalNotificationManager.addNotification(notification);
            showSnackbar(undefined, i18next.t("Notification successfully created!"));
            dialog.close();
            await this._loadLocal();
        } catch (error) {
            console.error("Failed to create notification:", error);
            showSnackbar(undefined, i18next.t("Notification creation failed."));
        }
    }

    protected async _showCreateDialog() {
        const dialog = showDialog(
            new OrMwcDialog()
            .setHeading(i18next.t("Create notification"))
            .setContent(this._getDialogHTML())
                .setActions([
                    {
                        actionName: "cancel",
                        content: i18next.t("cancel"),
                        action: () => dialog.close()
                    },
                    {
                        actionName: "create",
                        content: i18next.t("create"),
                        action: async () => this._handleCreateNotification(dialog)
                    }
                ])
        )
    }

    protected render() {
        if (!manager.authenticated) {
            return html`<or-translate value="notAuthenticated"/>`;
        }

        const writeNotifications = manager.hasRole("write:admin")

        return html`
            <div id="wrapper">
                <div id="title">
                    <div style="display: flex; align-items: center;">
                        <or-icon icon="message-outline" style="padding: 0 10px 0 4px;"></or-icon>
                        <span><or-translate value="${i18next.t("notifications")}"/></span>
                    </div>
                    
                    ${writeNotifications ? html`
                        <or-mwc-input 
                            type="${InputType.BUTTON}"
                            icon="plus"
                            label="${i18next.t("add")}"
                            @or-mwc-input-changed="${() => this._showCreateDialog()}"
                        ></or-mwc-input>
                    ` : ``}
                </div>

                <div class="table">
                    <table>
                        <thead>
                            <tr>
                                <th><or-translate value="${i18next.t("Name")}"/></th>
                                <th><or-translate value="${i18next.t("Title")}"/></th>
                                <th><or-translate value="${i18next.t("Priority")}"/></th>
                                <th><or-translate value="${i18next.t("Sent on")}"/></th>
                                <th><or-translate value="${i18next.t("Status")}"/></th>
                            </tr>
                        </thead>
                        <tbody>
                            ${this._data ? this._data.map(notification => {
                                const pushMessage = notification.message as PushNotificationMessage;
                                return html`
                                    <tr>
                                        <td>${notification.name}</td>
                                        <td>${pushMessage.title}</td>
                                        <td>${pushMessage.priority || 'NORMAL'}</td>
                                        <td>${new Date(notification.sentOn).toLocaleString()}</td>
                                        <td>${notification.deliveredOn ? "Delivered" : "Sent"}</td>
                                    </tr>
                                `;
                            }) : html`
                                <tr>
                                    <td colspan="5" style="text-align: center;">
                                        <or-translate value="loading"/>
                                    </td>
                                </tr>
                            `}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

}
