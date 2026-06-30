import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {OrMwcTable, TableColumn, TableConfig, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {DefaultColor4} from "@openremote/core";
import { SentNotification, PushNotificationMessage, EmailNotificationMessage, NotificationTargetType, NotificationSource } from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {classMap} from "lit/directives/class-map.js";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import { NotificationService } from "../../pages/page-notifications";
import { getAssetsRoute, getUsersRoute } from "../../routes";

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

export class OrNotificationsPageChangedEvent extends CustomEvent<{page: number, size: number}> {
    static readonly NAME = "or-notifications-page-changed";

    constructor(page: number, size: number) {
        super(OrNotificationsPageChangedEvent.NAME, {
            detail: {page, size},
            bubbles: true,
            composed: true
        });
    }
}

@customElement("or-notifications-table")
export class OrNotificationsTable extends OrMwcTable {
    static get styles() {
            return [
                ...super.styles,
                css`
                :host {
                    display: flex;
                    flex-direction: column;
                    min-height: 0;
                }

                /* Size the table to its rows; the host height only acts as an upper bound */
                .mdc-data-table,
                .mdc-data-table__paginated {
                    max-height: none;
                    min-height: 0;
                }

                /* Scroll rows within the table so the pagination footer stays visible when space runs out */
                .mdc-data-table__table-container {
                    overflow-y: auto;
                }

                .notification-container {
                    padding: 20px;
                    margin: 0 20px;
                    overflow: auto;
                }
    
                tbody tr:hover {
                    background-color: var(--or-table-row-hover-color, rgba(0,0,0,0.04));
                }

                td:nth-child(1), th:nth-child(1) { width: 15%; } /* Title column */
                td:nth-child(2), th:nth-child(2) { width: 25%; } /* Content column */
                td:nth-child(3), th:nth-child(3) { width: 6%; } /* Status column */
                td:nth-child(4), th:nth-child(4) { width: 4%; } /* Source column */
                td:nth-child(5), th:nth-child(5) { width: 15%; } /* Target column */
                td:nth-child(6), th:nth-child(6) { width: 6%; } /* SentOn column */
                td:nth-child(7), th:nth-child(7) { width: 6%; } /* DeliveredOn column */
    
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

                .title-wrapper {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .type-icon {
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
                    color: var(--or-app-color3);
                    flex: 0 0 auto;
                }

                .target-wrapper {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .target-link {
                    color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
                    overflow: hidden;
                    text-overflow: ellipsis;
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

    @property({type: Number})
    public currentPage: number = 0;

    @property({type: Number})
    public totalCount: number = 0;

    @state()
    protected targetDetailsMap: Map<string, {name: string, type: string, link: string}> = new Map();

    protected getTargetMapKey(targetId: string, targetType: string): string {
        return `${targetType}:${targetId}`;
    }

    public columns: TableColumn[] = [
        {title: i18next.t("title"), isSortable: true},
        {title: i18next.t("body")},
        {title: i18next.t("status"), isSortable: true},
        {title: i18next.t("notifications.source"), isSortable: true},
        {title: i18next.t("notifications.target")},
        {title: i18next.t("sentOn"), isSortable: true},
        {title: i18next.t("deliveredOn"), isSortable: true}
    ];

    protected config: TableConfig = {
        columnFilter: [],
        stickyFirstColumn: false,
        pagination: {
            enable: true
        },
        multiSelect: false
    }

    protected sortIndex = 5; // sort by sent date by default (column 5 = sentOn)
    protected sortDirection: "ASC" | "DESC" = "DESC";

    protected async willUpdate(changedProps: PropertyValues) {
        // update rows when notifications change
        if (changedProps.has("notifications")) {
            if (this.notifications?.length) {
                await this.resolveAllTargetDetails(this.notifications);
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
        return notifications.map((notification) => {
            const message = notification.message as PushNotificationMessage | EmailNotificationMessage;
            const isEmail = message?.type === "email";
            const title = isEmail ? (message as EmailNotificationMessage).subject : (message as PushNotificationMessage)?.title;
            const body = isEmail ? (message as EmailNotificationMessage).html : (message as PushNotificationMessage)?.body;
            return {
                content: [
                    this.getTitleContent(notification, title),
                    body,
                    this.getStatusContent(notification),
                    this.getSourceContent(notification.source),
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

    protected getTitleContent(notification: SentNotification, title?: string): TemplateResult {
        const iconMap: {[type: string]: string} = {
            "push": "cellphone-message",
            "email": "email-outline"
        };

        // The icon is nested in its own template so the title remains the first string value used for sorting
        return html`
            <div class="title-wrapper">
                ${html`<or-icon class="type-icon" title="${notification.type || ""}" icon="${iconMap[notification.type] || "bell-outline"}"></or-icon>`}
                <span>${title || "-"}</span>
            </div>
        `;
    }

    protected getSourceContent(source: NotificationSource): TemplateResult {
        return html`<or-translate value="notifications.sources.${source}"></or-translate>`;
    }

    protected getTargetContent(notification: SentNotification): TemplateResult {
        const details = this.targetDetailsMap.get(this.getTargetMapKey(notification.targetId, notification.target));

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
            <div class="target-wrapper">
                <or-icon
                    class="target-icon"
                    icon="${iconMap[details.type] || 'help-circle-outline'}">
                </or-icon>
                ${details.link
                    ? html`<a href="${details.link}" class="target-link ${details.type}-link">${details.name}</a>`
                    : html`<span class="target-link">${details.name}</span>`
                }
            </div>
        `;
    }

    protected getStatusContent(notification: SentNotification): TemplateResult {
        const isDelivered = !!notification.deliveredOn;

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

        return result;
    }

    protected getDateContent(date?: number): TemplateResult {
        return html`${date ? new Date(date).toLocaleString() : '-'}`;
    }

    // The status/date columns render as colored spans / localized date strings, which the base table can't
    // sort meaningfully. Sort them from the underlying notification on the row instead, using the real column
    // index that sortObjectRows receives. Other columns fall back to the default sorting.
    protected sortObjectRows(rowA: TableRow, rowB: TableRow, cIndex: number, sortDirection: "ASC" | "DESC"): number {
        const notifA = (rowA as NotificationTableRow).data?.notification;
        const notifB = (rowB as NotificationTableRow).data?.notification;

        if (notifA && notifB) {
            const dir = sortDirection === "DESC" ? -1 : 1;
            switch (cIndex) {
                case 2: // Status: pending (no deliveredOn) before delivered
                    return dir * ((notifA.deliveredOn ? 1 : 0) - (notifB.deliveredOn ? 1 : 0));
                case 5: // Sent date
                    return dir * ((notifA.sentOn ?? 0) - (notifB.sentOn ?? 0));
                case 6: // Delivered date
                    return dir * ((notifA.deliveredOn ?? 0) - (notifB.deliveredOn ?? 0));
            }
        }

        return super.sortObjectRows(rowA, rowB, cIndex, sortDirection);
    }

    protected async resolveAllTargetDetails(notifications: SentNotification[]) {
        const assetIds = new Set<string>();
        const userIds = new Set<string>();

        notifications.forEach((n) => {
            if (!n.targetId || this.targetDetailsMap.has(this.getTargetMapKey(n.targetId, n.target))) return;
            if (n.target === NotificationTargetType.ASSET) assetIds.add(n.targetId);
            else if (n.target === NotificationTargetType.USER) userIds.add(n.targetId);
            else {
                // REALM or unknown — just display the ID directly, no API lookup needed
                this.targetDetailsMap.set(this.getTargetMapKey(n.targetId, n.target), { name: n.targetId, type: "realm", link: "" });
            }
        });

        if (assetIds.size === 0 && userIds.size === 0) return;

        const canReadAssets = this.notificationService.hasAssetReadPermissions();
        const canReadUsers = this.notificationService.hasUserReadPermissions();

        // Store raw IDs for targets we cannot resolve due to missing permissions
        if (!canReadAssets) {
            assetIds.forEach(id => {
                this.targetDetailsMap.set(this.getTargetMapKey(id, NotificationTargetType.ASSET), { name: "-", type: "asset", link: "" });
            });
        }
        if (!canReadUsers) {
            userIds.forEach(id => {
                this.targetDetailsMap.set(this.getTargetMapKey(id, NotificationTargetType.USER), { name: "-", type: "user", link: "" });
            });
        }

        try {
            const [assets, users] = await Promise.all([
                canReadAssets && assetIds.size > 0
                    ? this.notificationService.getAssetsDetails(Array.from(assetIds))
                    : Promise.resolve([]),
                canReadUsers && userIds.size > 0
                    ? this.notificationService.getUsersDetails(Array.from(userIds))
                    : Promise.resolve([]),
            ]);

            assets.forEach((asset) => {
                this.targetDetailsMap.set(this.getTargetMapKey(asset.id!, NotificationTargetType.ASSET), {
                    name: asset.name || asset.id!,
                    type: "asset",
                    link: `#/${getAssetsRoute(false, asset.id!)}`,
                });
            });

            users.forEach((user) => {
                this.targetDetailsMap.set(this.getTargetMapKey(user.id!, NotificationTargetType.USER), {
                    name: user.username,
                    type: "user",
                    link: `#/${getUsersRoute(user.id!)}`,
                });
            });

            this.requestUpdate();
        } catch (err) {
            console.error("Failed to resolve bulk target details", err);
        }
    }

    protected onRowClick(ev: MouseEvent, item: NotificationTableRow) {
        if (item?.data?.notification) {
            const event = new NotificationTableClickEvent(item.data.notification.id)
            this.dispatchEvent(event);
        }
    }

    async getRowCount(): Promise<number> {
        return this.totalCount ?? await super.getRowCount();
    }

    async getPaginationControls(): Promise<TemplateResult> {
        const max = this.totalCount ?? await super.getRowCount();
        const start = this.currentPage * this.paginationSize + 1;
        const end = Math.min((this.currentPage + 1) * this.paginationSize, max);
        const isFirst = this.currentPage === 0;
        const isLast = end >= max;
        const lastPage = Math.max(0, Math.ceil(max / this.paginationSize) - 1);
        return html`
            <div class="mdc-data-table__pagination-navigation">
                <div class="mdc-data-table__pagination-total">
                    <span>${start}-${end} of ${max}</span>
                </div>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                    data-first-page="true" icon="page-first" .disabled="${isFirst}"
                    @or-mwc-input-changed="${() => this.dispatchEvent(new OrNotificationsPageChangedEvent(0, this.paginationSize))}">
                </or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                    data-prev-page="true" icon="chevron-left" .disabled="${isFirst}"
                    @or-mwc-input-changed="${() => this.dispatchEvent(new OrNotificationsPageChangedEvent(this.currentPage - 1, this.paginationSize))}">
                </or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                    data-next-page="true" icon="chevron-right" .disabled="${isLast}"
                    @or-mwc-input-changed="${() => this.dispatchEvent(new OrNotificationsPageChangedEvent(this.currentPage + 1, this.paginationSize))}">
                </or-mwc-input>
                <or-mwc-input class="mdc-data-table__pagination-button" .type="${InputType.BUTTON}"
                    data-last-page="true" icon="page-last" .disabled="${isLast}"
                    @or-mwc-input-changed="${() => this.dispatchEvent(new OrNotificationsPageChangedEvent(lastPage, this.paginationSize))}">
                </or-mwc-input>
            </div>
        `;
    }

    protected updated(changedProperties: Map<string, any>) {
        super.updated(changedProperties);
        if (changedProperties.has('paginationSize') && this.totalCount) {
            this.dispatchEvent(new OrNotificationsPageChangedEvent(0, this.paginationSize));
        }
    }
}
