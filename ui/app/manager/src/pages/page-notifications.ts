import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {
    Notification,
    SentNotification,
    PushNotificationMessage,
    Asset,
    User,
    PushNotificationMessageMessagePriority,
    PushNotificationMessageTargetType,
    NotificationTargetType,
    RepeatFrequency
} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor4} from "@openremote/core";
import i18next from "i18next";
import {when} from "lit/directives/when.js";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {GenericAxiosResponse, isAxiosError, AxiosError} from "@openremote/rest";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import { NotificationForm, NotificationFormData } from "../components/notifications/notification-form";
import "../components/notifications/notification-form";
import { OrNotificationsTable } from "../components/notifications/or-notifications-table";
import "../components/notifications/or-notifications-table";
import {
    OrMwcTable,
    OrMwcTableRowClickEvent,
    OrMwcTableRowSelectEvent
} from "@openremote/or-mwc-components/or-mwc-table";
// import { Input } from "@openremote/or-rules/src/flow-viewer/services/input";

export class NotificationService {
    
    async getNotifications(realm: string): Promise<SentNotification[]> {
        try {
            console.log("Fetching notifications for realm:", realm);

            // call the notifications endpoint with minimal filtering
            const response = await manager.rest.api.NotificationResource.getAllNotifications({});
            if (!response.data) {
                console.warn("No data in response:", response);
                return [];
            }

            console.log("Notifications response:", {
                total: response.data.length,
                data: response.data.slice(0, 3), // log first few for sanity check
                status: response.status
            });

            return response.data;
        } catch (err: unknown) {
            const error = err as AxiosError; //typecast
            console.error('Failed to fetch notifications:', error);
            if (isAxiosError(error)) {
                console.error('API Error details:', {
                    status: error.response?.status,
                    statusText: error.response?.statusText,
                    data: error.response?.data
                });
            }
            throw error;
        }
    }



    // async getNotifications(realm: string): Promise<SentNotification[]> {
    //     try {
            
    //         // get todays start (midnight) and end (23:59)
    //         const today = new Date();
    //         const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
    //         const endOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23,59,59,599).getTime();

    //         console.log("Making API request for realm:", realm);
    //         const response = await manager.rest.api.NotificationResource.getNotifications({
    //             realmId: realm,
    //             from: startOfDay,
    //             to: endOfDay
    //         });

    //         // Log the raw response
    //         console.log("Raw API response:", response);
    //         console.log("Response data length:", response.data?.length || 0);


    //         if (response.status !== 200) {
    //             throw new Error("Failed to load notifications");
    //         }

    //         console.log("All notifications:", response.data);
    //         const filtered = response.data.filter(notification => notification.message.type === "push");
    //         console.log("Filtered notifications:", filtered);
    //         return filtered; 
    //     } catch (error) {
    //         console.error('Failed to load notifications: ', error);
    //         if (isAxiosError(error)) {
    //             console.error('Error details:', error.response?.data);
    //         }
    //         throw error;
    //     }
    // }

    async sendNotification(notification: Notification): Promise<boolean> {
        try {
            console.log("About to send notification:", notification);
    
            // const response = await manager.rest.api.NotificationResource.sendNotification(
            //     notification
            // );

            const response = await manager.rest.api.NotificationResource.createNotificationInDB(
                notification
            );
            
            console.log("Response received:", response);
            return response.status === 200;
        } catch (err: unknown) {
            const error = err as AxiosError;
            console.error('Failed to send notification: ', error);
            if (isAxiosError(error)) {
                console.error('Full error object:', {
                    config: error.config,
                    response: error.response
                });
            }
            throw error;
        }
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
    protected _users?: User[];

    @state()
    protected _assets?: Asset[];

    @state()
    protected _realms?: string[];

    @state()
    protected _selectedTargetType?: string;

    @state()
    protected _targetOptions: {text: string, value: string}[] = [];

    protected _targetTypeInput?: OrMwcInput;

    @state()
    protected _targetInput?: OrMwcInput;

    @state()
    protected notification?: Notification;

    @state()
    protected creationState?: {
        notificationModel: Notification;
    }

    protected _loading: boolean = false;
    protected notificationService: NotificationService; 

    constructor(store: Store<AppStateKeyed>) {
        super(store);
        this.notificationService = new NotificationService();
    }

    public stateChanged(state: AppStateKeyed): void {
        if (state.app.page == "notifications") {
            if (this.realm === undefined || this.realm !== state.app.realm) {
                this.realm = state.app.realm;
                this._loadData();
            }
        }
    }

    protected async _loadData() {
        console.log("_loadData called with realm:", this.realm);
        if (this._loading || !this.realm) {
            return;
        }

        this._loading = true;
        console.log("Starting notification load with:", {
            realm: this.realm,
            authenticated: manager.authenticated,
            hasReadAdminRole: manager.hasRole("read:admin"),
            baseUrl: manager.rest.api.NotificationResource.getNotifications()
        });

        try {
            this._data = await this.notificationService.getNotifications(this.realm);
            this.requestUpdate();
        } catch (err: unknown) {
            const error = err as AxiosError
            console.error("Notification load failed with error:", error);
        if (error.response) {
            console.error("Error response:", {
                status: error.response.status,
                statusText: error.response.statusText,
                data: error.response.data,
                headers: error.response.headers
            });
            }
            showSnackbar(undefined, i18next.t("Notification load failed."));
        } finally {
            this._loading = false;
        }
    }

    protected _getFormData(dialog: OrMwcDialog): NotificationFormData | null {
        const form = dialog.shadowRoot?.querySelector<NotificationForm>("notification-form");
        if (!form) {
            console.error("Form not found in dialog");
            return null;
        }
    
        const formData = form.getFormData();
        
        // validate required fields per schema
        if (!formData?.name || !formData?.targetType || !formData?.target) {
            console.error("Missing required fields:", {
                hasName: !!formData?.name,
                hasTargetType: !!formData?.targetType,
                hasTarget: !!formData?.target
            });
            return null;
        }
    
        console.log("Form data validation passed:", formData);
        return formData;
    }

    protected async _handleCreateNotification(dialog: OrMwcDialog) {
        const formData = this._getFormData(dialog);
        if (!formData) {
            return;
        }

        console.log("Raw form data:", formData);

        try {
            // create a basic notification first
            const message: PushNotificationMessage = {
                type: "push" as const,
                title: formData.title,
                body: formData.body,
                data: {}  // keep it simple
            };

            const notification: Notification = {
                name: formData.name,
                message: message,
                targets: [{
                    type: formData.targetType as NotificationTargetType,
                    id: formData.target
                }]
            };

            console.log("Sending simplified notification:", notification);

            const response = await this.notificationService.sendNotification(notification);
            console.log("Server response:", response);

            showSnackbar(undefined, i18next.t("Creating notification success."));
            dialog.close();
        } catch (err: unknown) {
            const error = err as AxiosError;
            console.error("Error creating notification:", error);
            if (error.response) {
                console.error("Error response:", {
                    status: error.response.status,
                    data: error.response.data,
                    headers: error.response.headers
                });
            }
            showSnackbar(undefined, i18next.t("Creating notification failure."));
        } finally {
            await this._loadData();
        }
    }

    protected isValidTargetType(type: string): boolean {
        return type === NotificationTargetType.REALM ||
            type === NotificationTargetType.USER ||
            type === NotificationTargetType.ASSET ||
            type === NotificationTargetType.CUSTOM;
    }

    protected _createNotificationFromFormData(formData: any): Notification {
        // validate if target type is one of the allowed values 
        if (!this.isValidTargetType(formData.targetType)) {
            throw new Error(`Invalid target type: ${formData.targetType}`)
        }

        // extend it with PushNotification specific fields
        const pushMessage: PushNotificationMessage = {
            type: "push",
            title: formData.title,
            body: formData.body, 
            priority: PushNotificationMessageMessagePriority.NORMAL,
            targetType: PushNotificationMessageTargetType.DEVICE,
            target: formData.target,
            data: {}
        };

        // create the target that matches the API schema
        const target = {
            type: formData.targetType as NotificationTargetType,
            id: formData.target,
            data: {} 
        };
        

        // create notification matching the schema
        const notification: Notification = {
            name: formData.name,
            message: pushMessage,
            targets: [target],
            repeatFrequency: RepeatFrequency.ONCE
            // repeatInterval is optional according to schema
        };
    
        // add validation logging
        console.log("Sending notification with types:", {
            messageTargetType: pushMessage.targetType,
            notificationTargetType: target.type,
            fullPayload: notification
        });
    
        return notification;
    }

    get name(): string {
        return "notification.notification_plural"
    }

    protected reset(): void {
        this.notification = undefined;
        this.creationState = undefined;
        this._data = undefined;
        this.requestUpdate();
    }

    public shouldUpdate(changedProperties: PropertyValues): boolean {
        console.log(changedProperties);

        // if changed props has realm this._loadData()
        return super.shouldUpdate(changedProperties);
    }

    public connectedCallback(): void {
        super.connectedCallback();
        this.realm = this.getState().app.realm;
    }

    protected _getDialogHTML() {
        return html`
        <div class="dialog-content">
            <notification-form
                id="notificationForm"
                ?disabled="${false}">
            </notification-form>
        </div>
        `;
    }

    protected async _showCreateDialog() {
        await customElements.whenDefined('notification-form');

        // declare type first
        let dialogInstance: OrMwcDialog;

        dialogInstance = showDialog(
            new OrMwcDialog()
            .setHeading(i18next.t("Create notification"))
            .setContent(this._getDialogHTML())
                .setActions([
                    {
                        actionName: "cancel",
                        content: i18next.t("cancel"),
                        action: () => {
                            dialogInstance.close();
                            this._loadData(); // reload after cancel
                        }
                    },
                    {
                        actionName: "create",
                        content: i18next.t("create"),
                        action: async () => this._handleCreateNotification(dialogInstance)
                        // note! _loadData is already called in _handleCreateNotification
                    }
                ])
        )
    }

    protected getNotificationsTable() {
        return html`
            <or-notifications-table .notifications=${this._data || []}
                @or-mwc-table-row-click="${(e: OrMwcTableRowClickEvent) => this._onRowClick(e)}"
            ></or-notifications-table>
        `;
    }

    private _onRowClick(e: OrMwcTableRowClickEvent) {
        // prevent handling if no notificiation data
        if (!this._data || !e.detail) {
            console.warn("No data available.");
            return;
        }

        // get notification by index
        const index = e.detail.index;
        const notification = this._data[index];

        // if no notif. return
        if (!notification) {
            console.warn(`No notification found at index ${index}`);
            return;
        }

        // cast the message to PushNotificationMessage (to access the content)
        const pushMessage = notification.message as PushNotificationMessage;

        // log selection
        console.log("Clicked notification:", notification);
        
        // show notification details in dialog
        const dialog = showDialog(
            new OrMwcDialog()
            .setHeading(i18next.t("Notification details"))
            .setContent(html`
                    <div style="padding: 20px;">
                        <h3>${notification.name || ''}</h3>
                        <p><strong>${i18next.t("Id")}:</strong> ${notification.id}</p>
                        <p><strong>${i18next.t("Title")}:</strong> ${pushMessage.title}</p>
                        <p><strong>${i18next.t("Message")}:</strong> ${pushMessage.body}</p>
                        <p><strong>${i18next.t("Source")}:</strong> ${notification.source + ", " + notification.sourceId}</p>
                        <p><strong>${i18next.t("Target")}:</strong> ${notification.target + ", " + notification.targetId}</p>
                        <p><strong>${i18next.t("Sent")}:</strong> ${notification.sentOn ? new Date(notification.sentOn).toLocaleString() : '-'}</p>
                        <p><strong>${i18next.t("Delivered")}:</strong> ${notification.deliveredOn ? new Date(notification.deliveredOn).toLocaleString() : '-'}</p>
                        <p><strong>${i18next.t("Status")}:</strong> ${notification.deliveredOn ? i18next.t("delivered") : i18next.t("pending")}</p>
                    </div>
                `)
        ).setActions([
            {
                actionName: "close",
                content: i18next.t("close"),
                action: () => dialog.close()
            }
        ])
    }

    protected getNewNotificationModel(): Notification {
        return {
            name: "",
            message: {
                type: "push",
                title: "",
                body: "",
                data: {}
            } as PushNotificationMessage,
            targets: [],
        };
    }

    protected render() {
        if (!manager.authenticated) {
            return html`<or-translate value="notAuthenticated"/>`;
        }

        const writeNotifications = manager.hasRole("write:admin")

        return html`
        <div id="wrapper">
            <div id="title" class="${this.creationState || this.notification ? "hidden" : ""}">
                <div style="display: flex; align-items: center;">
                    <or-icon icon="message-outline" style="padding: 0 10px 0 4px;"></or-icon>
                    <span><or-translate value="${i18next.t("notifications")}"/></span>
                </div>
                
                ${writeNotifications ? html`
                    <or-mwc-input 
                        type="${InputType.BUTTON}"
                        icon="plus"
                        label="${i18next.t("add")}"
                        @or-mwc-input-changed="${() => this.creationState = {notificationModel: this.getNewNotificationModel()}}"
                    ></or-mwc-input>
                ` : ``}
            </div>

            ${when(this.notification || this.creationState,
                () => this._showCreateDialog(),
                () => html`
                    ${this.getNotificationsTable()}
                `
            )}
        </div>
    `;

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

                ${this.getNotificationsTable()}
            </div>
        `;
    }

}

