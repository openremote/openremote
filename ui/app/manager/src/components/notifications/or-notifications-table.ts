import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {OrMwcTable, OrMwcTableRowClickDetail, OrMwcTableRowClickEvent, TableColumn, TableConfig, TableContent, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import manager, {DefaultColor3} from "@openremote/core";
import {Notification, SentNotification, PushNotificationMessage, NotificationTargetType} from "@openremote/model";
import i18next from "i18next";
import {classMap} from "lit/directives/class-map.js";
import { NotificationService } from "../../pages/page-notifications";

export interface NotificationTableRow extends TableRow {
    data: {
        notification: SentNotification
    }
}

export class NotificationTableClickEvent extends CustomEvent<{notificationId: number}> {
    static readonly NAME="or-notification-selected";

    constructor(notificationId: number) {
            super(NotificationTableClickEvent.NAME, {
                detail: {notificationId},
                bubbles: true, 
                composed: true
            })
        }
    }

@customElement("or-notifications-table")
export class OrNotificationsTable extends OrMwcTable {
    static get styles() {
            return [
                ...super.styles,
                css`
                .notification-container {
                    padding: 20px;
                    margin: 0 20px;
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
                    width: auto;
                }
    
                td {
                    padding: 4px 6px;
                    border-bottom: 1px solid #e0e0e0;
                    vertical-align: middle;
                    width: auto;
                }
    
                tbody tr:hover {
                    background-color: var(--or-table-row-hover-color, rgba(0,0,0,0.04));
                }

                td:nth-child(1), th:nth-child(1) { width: 15%; } /* Title column */
                td:nth-child(2), th:nth-child(2) { width: 25%; } /* Content column */
                td:nth-child(3), th:nth-child(3) { width: 6%; } /* Status column */
                td:nth-child(4), th:nth-child(4) { width: 15%; } /* Target column */
                td:nth-child(5), th:nth-child(5) { width: 8%; } /* SentOn column */
                td:nth-child(6), th:nth-child(6) { width: 8%; } /* DeliveredOn column */
    
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

                .status-error {
                    color: var(--or-notification-status-pending-color,rgb(231, 126, 126));
                    background: var(--or-notification-status-pending-bg, rgba(143, 126, 231, 0.1));
                }

                .target-link {
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    text-decoration: none;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .target-link:hover {
                    text-decoration: none;
                }

                .target-icon {
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
                }

                .target-loading {
                    color: var(--or-app-color3);
                    font-style: italic;
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

    @property({type: Object})
    public notificationService!: NotificationService;

    @state()
    protected targetDetailsMap: Map<string, {name: string, type: string, link: string}> = new Map();


    public columns: TableColumn[] = [
        {title: i18next.t("Title"), isSortable: true},
        {title: i18next.t("Content")},
        {title: i18next.t("Status"), isSortable: true},
        {title: i18next.t("Target")},
        {title: i18next.t("Sent on"), isSortable: true},
        {title: i18next.t("Delivered on"), isSortable: true}
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

    protected async willUpdate(changedProps: Map<string, any>) {
        // update rows when notifications change
        if (changedProps.has("notifications")) {
            if (this.notifications?.length) {
                await Promise.all(
                    this.notifications.map(notification => 
                        this.resolveTargetDetails(notification)
                    )
                );
            }
            this.rows = this.getTableRows(this.notifications || []);
        }

        // handle readonly state changes
        if (changedProps.has("readonly")) {
            this.config.multiSelect = !this.readonly;
        }

        return super.willUpdate(changedProps);
    }

    protected getTableRows(notifications: SentNotification[]): TableRow[] | undefined {
        notifications.forEach(notification => {
            this.resolveTargetDetails(notification);
        })

        return notifications.map((notification, index) => {
            const pushMessage = notification.message as PushNotificationMessage;
            return {
                content: [
                    pushMessage.title, 
                    pushMessage.body,
                    this.getStatusContent(notification),
                    this.getTargetContent(notification),
                    this.getDateContent(notification.sentOn),
                    this.getDateContent(notification.deliveredOn)
                ],
                clickable: true,
                // store the full notification object in the row data
                data: { notification },
            } as NotificationTableRow;
        });
    }

    protected getTargetContent(notification: SentNotification): TemplateResult {
        const details = this.targetDetailsMap.get(notification.targetId);

        if (!details) {
            // show ID while loading or if we couldn't load details
            return html`
                <span class="target-loading">
                    ${notification.targetId ? 
                        html`<or-translate value="loading"/>` : 
                        '-'
                    }
                </span>
            `;
        }

        const iconMap = {
            'asset': 'cube-outline',
            'user': 'account',
            'realm': 'domain'
        };

        return html`
            <a href="${details.link}" class="target-link ${details.type}-link">
                <or-icon 
                    class="target-icon"
                    icon="${iconMap[details.type] || 'help-circle-outline'}">
                </or-icon>
                ${details.name}
            </a>
        `;
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

    protected getDateContent(date?: number): TemplateResult {
        return html`${date ? new Date(date).toLocaleString() : '-'}`;
    }

    protected sortTemplateRows(cellA: any, cellB: any, cIndex: number, sortDirection: "ASC" | "DESC"): number {
        console.log("called sortTemplateRows from notifications table");
        // first extract the primitive values from the cell content
        const valueA: string | undefined = (cellA.values as any[])
        .filter(v => typeof v === 'string')
        .map(v=> v.toString())?.[0];

        const valueB: string | undefined = (cellB.values as any[])
        .filter(v=> typeof v === 'string')
        .map(v=> v.toString())?.[0];
        // second only proceed if we have valid values to compare

        if (valueA !== undefined && valueB !== undefined) {
            if (cIndex == 2) {
                const statusA = valueA.includes('status-delivered') ? 1 : 0;
                const statusB = valueB.includes('status-delivered') ? 1 : 0;

                return sortDirection === 'DESC' ?
                statusB - statusA : // for desc if B > A, result is positive (B comes first)
                statusA - statusB; // for asc if A > B, result is positive (A comes first)
            }
            if (cIndex === 3 ) {
                const dateA = new Date(valueA).getTime();
                const dateB = new Date(valueB).getTime();

                return sortDirection === 'DESC' ?
                dateB - dateA : 
                dateA - dateB;
            }

            if (cIndex === 4 ) {
                const dateA = new Date(valueA).getTime();
                const dateB = new Date(valueB).getTime();

                return sortDirection === 'DESC' ?
                dateB - dateA : 
                dateA - dateB;
            }

            // for other columns
            // fallback
            return this.sortPrimitiveRows([valueA], [valueB], 0, sortDirection)
        } else {
            return 1; // if either value is undefined move it to the end
        }
    }

    protected async resolveTargetDetails(notification: SentNotification) {
        if (!notification.target || !notification.targetId) return; 

        if (this.targetDetailsMap.has(notification.targetId)) {
            return;
        }

        try {
            let details;
            switch(notification.target) {
                case NotificationTargetType.ASSET:
                    const asset = await this.notificationService.getAssetDetails(notification.targetId);
                    if (asset) {
                        details = {
                            name: asset.name || asset.id,
                            type: 'asset',
                            link: `#/assets/false/${notification.targetId}`
                        };
                    }
                    break;

                case NotificationTargetType.USER:
                    const user = await this.notificationService.getUserDetails(notification.targetId, manager.displayRealm);
                    if (user) {
                        details = {
                            name: user.username,
                            type: 'user',
                            link: `#/users/${notification.targetId}`
                        };
                    }
                    break;
            }

            if (details) {
                this.targetDetailsMap.set(notification.targetId, details);
                this.requestUpdate();
            }
        } catch (err) {
            console.error(`Failed to resolve target details for ${notification.target}`);
        }
    }

    protected onRowClick(ev: MouseEvent, item: NotificationTableRow) {
        if (item?.data?.notification) {
            const event = new NotificationTableClickEvent(item.data.notification.id)
            this.dispatchEvent(event);

            console.log('DISPATCHING NOTIFICATION CLICK:', {
                id: item.data.notification.id,
            })
        }
    }
}