import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property} from "lit/decorators.js";
import {OrMwcTable, TableColumn, TableConfig, TableContent, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {DefaultColor3} from "@openremote/core";
import {Notification, SentNotification, PushNotificationMessage} from "@openremote/model";
import i18next from "i18next";
import {classMap} from "lit/directives/class-map.js";

@customElement("or-notifications-table")
export class OrNotificationsTable extends OrMwcTable {
    static get styles() {
            return [
                ...super.styles,
                css`
                .notification-container {
                    padding: 20px;
                    overflow: auto;
                }
    
                table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 0;
                    background-color: white;
                    border-radius: 4px;
                    box-shadow: var(--or-table-shadow, 0 1px 3px rgba(0,0,0,0.12));
                }
    
                th {
                    position: sticky;
                    top: 0;
                    background-color: white;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    text-align: left;
                    padding: 12px 16px;
                    font-weight: 500;
                    border-bottom: 2px solid #e0e0e0;
                    white-space: nowrap;
                }
    
                td {
                    padding: 12px 16px;
                    border-bottom: 1px solid #e0e0e0;
                    vertical-align: middle;
                }
    
                tbody tr:hover {
                    background-color: var(--or-table-row-hover-color, rgba(0,0,0,0.04));
                }
    
                .notification-status {
                    padding: 4px 8px;
                    border-radius: 4px;
                    display: inline-flex;
                    align-items: center;
                    font-size: 0.875rem;
                    font-weight: 500;
                }
    
                .status-delivered {
                    color: var(--or-notification-status-delivered-color, #22A06B);
                    background: var(--or-notification-status-delivered-bg, rgba(34, 160, 107, 0.1));
                }
    
                .status-pending {
                    color: var(--or-notification-status-pending-color, #8F7EE7);
                    background: var(--or-notification-status-pending-bg, rgba(143, 126, 231, 0.1));
                }
            `
        ];
    } 

    @property({type: Array})
    public notifications?: SentNotification[] = [];

    @property({type: Boolean})
    public loading = false; 

    @property({type: Array})
    public selectedIndices: number[] = []

    @property({type: Boolean})
    public selectable: boolean = true;

    @property({type: Boolean})
    public readonly = true

    public columns: TableColumn[] = [
        {title: i18next.t("Title"), isSortable: true},
        {title: i18next.t("Content")},
        {title: i18next.t("Status"), isSortable: true},
        {title: i18next.t("Sent on"), isSortable: true},
        {title: i18next.t("Delivered on"), isSortable: true},
        {title: i18next.t("Target")}
    ];

    protected config: TableConfig = {
        columnFilter: [],
        stickyFirstColumn: false, 
        pagination: {
            enable: true
        },
        multiSelect: false
    }

    protected sortIndex = 4; // sort by sent date by default
    protected sortDirection: "ASC" | "DESC" = "DESC";

    protected willUpdate(changedProps: Map<string, any>): void {
        // update rows when notifications change
        if (changedProps.has("notifications")) {
            this.rows = this.getTableRows(this.notifications)
        }

        // handle readonly state changes
        if (changedProps.has("readonly")) {
            this.config.multiSelect = !this.readonly;
        }

        return super.willUpdate(changedProps);
    }

    protected getTableRows(notifications: SentNotification[]): TableRow[] {
        return notifications.map(notification => {
            const pushMessage = notification.message as PushNotificationMessage;
            return {
                content: [
                    pushMessage.title, 
                    pushMessage.body,
                    this.getStatusContent(notification),
                    this.getDateContent(notification.sentOn),
                    this.getDateContent(notification.deliveredOn),
                    pushMessage.target
                ],
                clickable: true,
                data: notification // store the notification data for click handling
            };
        });
    }


    protected getStatusContent(notification: SentNotification): TemplateResult {
        const isDelivered = !!notification.deliveredOn;

        console.log('Notification status:', {
            deliveredOn: notification.deliveredOn,
            isDelivered: isDelivered
        });

        const classes = {
            "notification-status": true, 
            "status-delivered": isDelivered,
            "status-pending": !isDelivered
        };


        const result = html`
            <span class="${classMap(classes)}">
                ${isDelivered ? i18next.t("delivered") : i18next.t("pending")}
            </span> 
        `;

        console.log('Template result:', result);
        return result;
    }

    protected getDateContent(date?: number): string {
        return date ? new Date(date).toLocaleString() : '-';
    }

    protected _onRowClick(index: number) {
        //dispatch event the implementation source will handle
        const notification = this.notifications[index];
        if (notification) {
            const event = new CustomEvent('or-mwc-table-row-click', {
                detail: {
                    index: index,
                    data: notification
                },
                bubbles: true,
                composed: true
            });
            this.dispatchEvent(event);
        }
    }
}