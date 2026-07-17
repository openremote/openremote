/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {OrMwcTable, TableColumn, TableConfig, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {DefaultColor4} from "@openremote/core";
import { SentNotification, SentNotificationSortField, PushNotificationMessage, EmailNotificationMessage, NotificationTargetType, NotificationSource } from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {classMap} from "lit/directives/class-map.js";
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

export class OrNotificationsSortChangedEvent extends CustomEvent<{sort: SentNotificationSortField, descending: boolean}> {
    static readonly NAME = "or-notifications-sort-changed";

    constructor(sort: SentNotificationSortField, descending: boolean) {
        super(OrNotificationsSortChangedEvent.NAME, {
            detail: {sort, descending},
            bubbles: true,
            composed: true
        });
    }
}

// Maps a sortable column index to the server-side sort field. Columns absent here (body, target) aren't sortable.
const SORT_FIELD_BY_COLUMN: Record<number, SentNotificationSortField> = {
    0: SentNotificationSortField.TITLE,
    2: SentNotificationSortField.STATUS,
    3: SentNotificationSortField.SOURCE,
    5: SentNotificationSortField.SENT_ON,
    6: SentNotificationSortField.DELIVERED_ON,
};

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
    
                .status-sent {
                    color: var(--or-notification-status-sent-color, #8F7EE7);
                    background: var(--or-notification-status-sent-bg, rgba(143, 126, 231, 0.1));
                }

                .status-error {
                    color: var(--or-notification-status-error-color, rgb(231, 126, 126));
                    background: var(--or-notification-status-error-bg, rgba(231, 126, 126, 0.1));
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

    // Map keys whose lookup failed; their cached fallback entries are retried on the next resolve
    protected failedTargetKeys: Set<string> = new Set();

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
            // Emails may carry a plain-text body instead of html (EmailNotificationHandler supports both); fall back to it
            const body = isEmail
                ? ((message as EmailNotificationMessage).html ?? (message as EmailNotificationMessage).text)
                : (message as PushNotificationMessage)?.body;
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
        const isError = !!notification.error;
        const isDelivered = !isError && !!notification.deliveredOn;

        const classes = {
            "notification-status": true,
            "status-error": isError,
            "status-delivered": isDelivered,
            // Delivery can only be confirmed by consoles acknowledging a push
            "status-sent": !isError && !isDelivered
        };

        return html`
            <span class="${classMap(classes)}" title="${notification.error || ""}">
                ${i18next.t(isError ? "error" : isDelivered ? "delivered" : "sent")}
            </span>
        `;
    }

    protected getDateContent(date?: number): TemplateResult {
        return html`${date ? new Date(date).toLocaleString() : '-'}`;
    }

    // Sorting is done server-side (across all pages) rather than on the current page's rows: emit the chosen
    // column/direction so the page can re-query. Rows are already returned in sorted order, so we don't sort locally.
    async onColumnSort(_ev: MouseEvent, index: number, sortDirection: "ASC" | "DESC") {
        const sort = SORT_FIELD_BY_COLUMN[index];
        if (!sort) return; // body/target columns aren't sortable

        const descending = sortDirection === "ASC"; // toggle relative to the current direction
        this.sortIndex = index;
        this.sortDirection = descending ? "DESC" : "ASC";
        this.dispatchEvent(new OrNotificationsSortChangedEvent(sort, descending));
    }

    protected async resolveAllTargetDetails(notifications: SentNotification[]) {
        // Drop entries cached by failed lookups so they get requested again; entries for deleted
        // targets stay cached since they can never resolve
        this.failedTargetKeys.forEach(key => this.targetDetailsMap.delete(key));
        this.failedTargetKeys.clear();

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

        // Each kind handles its own failure so a failing asset request cannot discard user results (and vice versa)
        await Promise.all([
            this.resolveTargetDetails(assetIds, NotificationTargetType.ASSET, "asset",
                this.notificationService.hasAssetReadPermissions(),
                ids => this.notificationService.getAssetsDetails(ids),
                asset => ({ id: asset.id!, name: asset.name || asset.id!, link: `#/${getAssetsRoute(false, asset.id!)}` })),
            this.resolveTargetDetails(userIds, NotificationTargetType.USER, "user",
                this.notificationService.hasUserReadPermissions(),
                ids => this.notificationService.getUsersDetails(ids),
                user => ({ id: user.id!, name: user.username, link: `#/${getUsersRoute(user.id!)}` })),
        ]);
        this.requestUpdate();
    }

    protected async resolveTargetDetails<T>(
        ids: Set<string>,
        targetType: NotificationTargetType,
        type: string,
        canRead: boolean,
        fetchDetails: (ids: string[]) => Promise<T[]>,
        toEntry: (item: T) => { id: string, name: string, link: string }
    ) {
        if (ids.size === 0) return;

        // Hide targets the caller has no permission to resolve
        if (!canRead) {
            ids.forEach(id => this.targetDetailsMap.set(this.getTargetMapKey(id, targetType), { name: "-", type, link: "" }));
            return;
        }

        try {
            (await fetchDetails(Array.from(ids))).forEach(item => {
                const { id, name, link } = toEntry(item);
                this.targetDetailsMap.set(this.getTargetMapKey(id, targetType), { name, type, link });
            });
        } catch (err) {
            console.error(`Failed to resolve ${type} target details`, err);
            ids.forEach(id => this.failedTargetKeys.add(this.getTargetMapKey(id, targetType)));
        }

        // Ids that resolved to nothing (deleted target or failed lookup) fall back to the raw id
        // without a link, so they don't stay in a permanent loading state
        ids.forEach(id => {
            const key = this.getTargetMapKey(id, targetType);
            if (!this.targetDetailsMap.has(key)) {
                this.targetDetailsMap.set(key, { name: id, type, link: "" });
            }
        });
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
        // Reuse the base template but drive pagination server-side: emit a page-changed event instead of
        // mutating the local paginationIndex. `max` comes from the overridden getRowCount() (the total count).
        return this.renderPaginationControls(this.currentPage, (page) =>
            this.dispatchEvent(new OrNotificationsPageChangedEvent(page, this.paginationSize)));
    }

    protected updated(changedProperties: Map<string, any>) {
        super.updated(changedProperties);
        if (changedProperties.has('paginationSize') && this.totalCount) {
            this.dispatchEvent(new OrNotificationsPageChangedEvent(0, this.paginationSize));
        }
    }
}
