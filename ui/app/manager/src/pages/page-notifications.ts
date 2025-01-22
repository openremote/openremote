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
import manager, {DefaultColor3} from "@openremote/core";
import i18next from "i18next";
import {when} from "lit/directives/when.js";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {isAxiosError, AxiosError} from "@openremote/rest";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import { NotificationForm, NotificationFormData } from "../components/notifications/notification-form";
import "../components/notifications/notification-form";
import { OrNotificationsTable, NotificationTableClickEvent } from "../components/notifications/or-notifications-table";
import "../components/notifications/or-notifications-table";

export class NotificationService {
    
    async getNotifications(realm: string, fromDate?: number, toDate?: number): Promise<SentNotification[]> {
        try {
            const timeRange = fromDate && toDate ? 
            {fromDate, toDate} :
            this.getDefaultTimeRange();

            // const response = await manager.rest.api.NotificationResource.getNotifications({
            //     from: timeRange.fromDate,
            //     to: timeRange.toDate
            // });
            const response = await manager.rest.api.NotificationResource.getAllNotifications({
                from: timeRange.fromDate,
                to: timeRange.toDate
            });
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
            const error = err as AxiosError;
            console.error('Failed to fetch notifications:', error);
            throw error;
        }
    }

    async sendNotification(notification: Notification): Promise<boolean> {
        try {    
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

    public getLastDayTimeRange(): {fromDate: number, toDate: number} {
        const now = new Date();
        const yesterday = new Date(now.getDate() - 1);

        return {
            fromDate: yesterday.getTime(),
            toDate: now.getTime()
        }
    }

    public getDefaultTimeRange(): {fromDate: number, toDate: number} {
        const now = new Date();
        const toDate = new Date(now);
        toDate.setHours(23, 59, 59, 999);

        // start date is set to 30 days ago, beginning of the day
        const fromDate = new Date(now);
        fromDate.setDate(fromDate.getDate() - 30);
        fromDate.setHours(0, 0, 0, 0);

        const fromTimestamp = fromDate.getTime();
        const toTimestamp = toDate.getTime();

        if (!Number.isInteger(fromTimestamp) || !Number.isInteger(toTimestamp)) {
            console.error("Invalid timestamp generation:", {fromTimestamp, toTimestamp});
            throw new Error("Invalid timestamp generation")
        }

        return {
            fromDate: fromTimestamp,
            toDate: toTimestamp
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
        name: "Notifications",
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

            or-icon {
                vertical-align: middle;
                --or-icon-width: 20px;
                --or-icon-height: 20px;
            }
            
            // Dialog and form specific styles
            .dialog-content {
                display: flex;
                flex-direction: column;
                gap: 16px;
                padding: 0 24px;
            }

            .dialog-content or-mwc-input {
                margin-bottom: 16px;
            }

            .section {
                border-radius: 4px;
                padding: 20px;
                margin-bottom: 24px;
            }

            .section:last-child {
                margin-bottom: 0;
            }

            .section-title {
                color: var(--or-app-color3);
                font-size: 14px;
                font-weight: 500;
                margin-bottom: 16px;
            }

            .form-preview {
                display: grid;
                grid-template-columns: 2fr 1fr;
                gap: 24px;
                padding: 20px;
                width: 100%;
            }

            .right-column {
                display: flex;
                flex-direction: column;
                gap: 24px;
            }

            .section {
                background: white;
                border-radius: 4px;
                padding: 20px;
            }

            .field-group {
                display: flex;
                flex-direction: column;
                gap: 16px;
                width: 100%;  // Add this to ensure full width
            }

            or-mwc-input[readonly] {
                --mdc-text-field-fill-color: #f5f5f5;
                --mdc-text-field-disabled-line-color: #e0e0e0;
            }

            .filter-section {
                gap: 16px;
                padding: 16px;
                background: white;
                border-radius: 4px;
                margin: 0 20px 20px 20px;
                margin-left: auto;
                order: 2;
            }
            
            .create-btn {
                order: 3;
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
    protected notification?: SentNotification;

    @state()
    protected _fromDate?: number;

    @state()
    protected _toDate?: number;

    @state()
    protected _selectedSource?: string;

    @state()
    protected creationState?: {
        notificationModel: Notification;
    }

    @state()
    protected _isFiltered: boolean = false;

    protected _loading: boolean = false;
    protected notificationService: NotificationService;
    protected _sourceOptions = [
        { value: " ", text: i18next.t("All sources") },
        { value: "CLIENT", text: i18next.t("CLIENT") },
        { value: "INTERNAL", text: i18next.t("INTERNAL") },
        { value: "GLOBAL_RULESET", text: i18next.t("GLOBAL_RULESET") },
        { value: "REALM_RULESET", text: i18next.t("REALM_RULESET") },
        { value: "ASSET_RULESET", text: i18next.t("ASSET_RULESET") },
    ]

    constructor(store: Store<AppStateKeyed>) {
        super(store);
        this.notificationService = new NotificationService();

        // set initial date range
        const {fromDate, toDate} = this.notificationService.getLastDayTimeRange();
        this._fromDate = fromDate;
        this._toDate = toDate;
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

        try {
            // this is where that undefined comes from jfc 
            const fromDate = this._isFiltered ? this._fromDate : undefined;
            const toDate = this._isFiltered ? this._toDate : undefined;

            //if filtering is enabled, use selected dates
            // otherwise default to range (last 30 days)

            const timeRange = this._isFiltered && this._fromDate && this._toDate ?
            {
                fromDate: this._fromDate,
                toDate: this._toDate
            } :
            this.notificationService.getDefaultTimeRange();

            if (!this._isFiltered) {
                this._fromDate = timeRange.fromDate;
                this._toDate = timeRange.toDate;
            }

            console.log("getNotifications called for time range:", this._fromDate, this._toDate)

            this._data = await this.notificationService.getNotifications(
                this.realm, 
                fromDate, 
                toDate
            );

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

        try {
            const buttons = [];
            if (formData.openButtonText) {
                buttons.push({
                    title: formData.openButtonText,
                    action: {
                        url: formData.actionUrl,
                        openInBrowser: true
                    }
                });
            }
            if (formData.closeButtonText) {
                buttons.push({title: formData.closeButtonText});
            }

            const message: PushNotificationMessage = {
                type: "push" as const,
                title: formData.title,
                body: formData.body,
                data: {},
                action: formData.actionUrl ? {
                    url: formData.actionUrl,
                    openInBrowser: true
                } : undefined,
                buttons: buttons.length > 0 ? buttons : undefined
            };

            const notification: Notification = {
                name: formData.title,
                message: message,
                targets: [{
                    type: formData.targetType as NotificationTargetType,
                    id: formData.target
                }]
            };

            const response = await this.notificationService.sendNotification(notification);
            console.log("Server response:", response);

            showSnackbar(undefined, i18next.t("Creating notification success."));
            dialog.close();
        } catch (error) {
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

    protected _getFilteredNotifications(): SentNotification[] {
        if (!this._data) return [];
        if (this._selectedSource == " ") {
            return this._data;
        }

        return this._data.filter(notification => {
            if (this._selectedSource && notification.source !== this._selectedSource) {
                return false;
            }
            return true;
        });
    }

    protected isValidTargetType(type: string): boolean {
        return type === NotificationTargetType.REALM ||
            type === NotificationTargetType.USER ||
            type === NotificationTargetType.ASSET ||
            type === NotificationTargetType.CUSTOM;
    }

    protected reset(): void {
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

        // load data if not already there
        if (!this._data) {
            this._loadData();
        }
    }

    get name(): string {
        return "notification.notification_plural"
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
                        <span><or-translate value="${i18next.t("Notifications")}"/></span>
                    </div>

                     <div class="filter-section">
                        <or-mwc-input
                            type="${InputType.SELECT}"
                            label="${i18next.t('Source')}"
                            .options="${this._sourceOptions.map(o => [o.value, o.text])}"
                            .value="${this._selectedSource}"
                            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                this._selectedSource = e.detail.value;
                                this.requestUpdate();
                            }}"
                        ></or-mwc-input>

                        <or-mwc-input
                        type="${InputType.CHECKBOX}"
                        label="${i18next.t('Enable time filtering')}"
                        .checked="${this._isFiltered}"
                        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            this._isFiltered = e.detail.value;
                            if (!this._isFiltered) {
                                // Clear date filters when disabling
                                this._fromDate = undefined;
                                this._toDate = undefined;
                            }
                            this._loadData();
                        }}"
                        ></or-mwc-input>


                        ${when(this._isFiltered, () => html`
                            <or-mwc-input
                                type="${InputType.DATETIME}"
                                label="${i18next.t('From')}"
                                .value="${this._fromDate}"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    this._fromDate = e.detail.value;
                                    this._loadData();
                                }}"
                            ></or-mwc-input>
    
                            <or-mwc-input
                                type="${InputType.DATETIME}"
                                label="${i18next.t('To')}"
                                .value="${this._toDate}"
                                @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    this._toDate = e.detail.value;
                                    this._loadData();
                                }}"
                            ></or-mwc-input>
    
                            <or-mwc-input 
                                type="${InputType.BUTTON}"
                                label="${i18next.t('Last 24 hours')}"
                                @or-mwc-input-changed="${() => {
                                    const {fromDate, toDate} = this.notificationService.getLastDayTimeRange();
                                    this._fromDate = fromDate;
                                    this._toDate = toDate;
                                    this._loadData();
                                }}"
                            ></or-mwc-input>
                        `)}
                    </div>
                    
                    <div class="create-btn">
                    ${writeNotifications ? html`
                        <or-mwc-input 
                            type="${InputType.BUTTON}"
                            icon="plus"
                            label="${i18next.t("send new")}"
                            @or-mwc-input-changed="${() => this._showCreateDialog()}"
                        ></or-mwc-input>
                    ` : ``}
                    </div>
                </div>

                ${this.getNotificationsTable()}
            </div>
        `;
    }

    protected getNotificationsTable() {
        return html`
            <or-notifications-table 
                .notifications=${this._getFilteredNotifications() || []}
                @or-notification-selected="${(e: NotificationTableClickEvent) => this._onRowClick(e)}"
            ></or-notifications-table>
        `;
    }

    protected _getCreateDialogHTML() {
        return html`
            <div class="dialog-content">
                <notification-form
                    id="notificationForm"
                    ?disabled="${false}">
                </notification-form>
            </div>
        `
    }

    protected _getNotificationDetailsContent(notification: SentNotification) {
        return html`
        <div class="form-preview">
            <notification-form
                    .notification=${notification}
                    ?readonly=${true}
                ></notification-form>
        </div>
        `;
    }

    protected async _showCreateDialog() {
        await customElements.whenDefined('notification-form');

        const dialog = showDialog(
            new OrMwcDialog()
            .setHeading(i18next.t("Create notification"))
            .setContent(this._getCreateDialogHTML())
                .setActions([
                    {
                        actionName: "cancel",
                        content: i18next.t("cancel"),
                        action: () => {
                            dialog.close();
                            this._loadData(); // reload after cancel
                        }
                    },
                    {
                        actionName: "create",
                        content: i18next.t("create"),
                        action: async () => this._handleCreateNotification(dialog)
                        // note: _loadData is already called in _handleCreateNotification
                    }
                ])
        )
    }

    private _onRowClick(e: NotificationTableClickEvent) {
        if (!this._data || !e.detail) {
            console.warn("No data available.");
            return;
        }
        const notificationId = e.detail.notificationId;
        console.log("EVENT RECEIVED BY NOTIFICATIONS:", e);
        console.log("Clicked notification from notifications page with index:", notificationId);

        const notification = this._data.find(n=> n.id === notificationId);
        // if no notif. return
        if (!notification) {
            console.warn(`No notification found with id ${notificationId}`);
            return;
        }
        console.log("Clicked notification from notifications page:", notification);
        
        const dialog = showDialog(
            new OrMwcDialog()
            .setHeading(i18next.t("Notification details"))
            .setContent(this._getNotificationDetailsContent(notification))   
            .setActions([
            {
                actionName: "close",
                content: i18next.t("close"),
                action: () => dialog.close()
            }
        ])
        );
    }


}

