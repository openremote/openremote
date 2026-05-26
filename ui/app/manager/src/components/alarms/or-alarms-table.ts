/*
 * Copyright 2024, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {css, html, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {OrMwcTable, TableColumn, TableConfig, TableContent, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {AlarmSeverity, AlarmStatus, Asset, SentAlarm} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {classMap} from "lit/directives/class-map.js";

/**
 * ## AlarmTableColumn
 * Custom model that inherits on {@link TableColumn}, and adds {@link widthWeight},
 * which impacts the column width; higher numbers are 'more important' and get wider, others get smaller.
 * */
export interface AlarmTableColumn extends TableColumn {
    widthWeight?: number;
}

const styling = css`
    .alarm-text {
        padding: 4px 6px;
        border-radius: 5px;
    }

    .alarm-status-text__closed {
        color: var(--or-alarm-status-color__closed, grey);
        background-color: var(--or-alarm-status-background__closed);
        border: var(--or-alarm-status-border__closed, 1px #8590A2 solid);
    }

    .alarm-status-text__resolved {
        color: var(--or-alarm-status-color__resolved, white);
        background-color: var(--or-alarm-status-background__resolved, #22A06B);
        border: var(--or-alarm-status-border__resolved);
    }

    .alarm-status-text__acknowledged {
        color: var(--or-alarm-status-color__acknowledged);
        background-color: var(--or-alarm-status-background__acknowledged);
        border: var(--or-alarm-status-border__acknowledged, 1px #8F7EE7 solid);
    }

    .alarm-status-text__in-progress {
        color: var(--or-alarm-status-color__in-progress, white);
        background-color: var(--or-alarm-status-background__in-progress, #388BFF);
        border: var(--or-alarm-status-border__in-progress);
    }

    .alarm-status-text__open {
        color: var(--or-alarm-status-color__open, white);
        background-color: var(--or-alarm-status-background__open, #8F7EE7);
        border: var(--or-alarm-status-border__open);
    }

    .alarm-severity-text__low {
        color: white;
        background-color: #388BFF;
    }

    .alarm-severity-text__medium {
        color: white;
        background-color: #ffab00;
    }

    .alarm-severity-text__high {
        color: white;
        background-color: #ff5630;
    }
`;

/**
 * ## or-alarms-table
 * <p>
 * It extends on {@link OrMwcTable}, and applies both additional styling and helper functions to it.
 * </p>
 * Usage;
 * <pre>
 *   <or-alarms-table .alarms="${alarms}"></or-alarms-table>
 * </pre>
 * */
@customElement("or-alarms-table")
export class OrAlarmsTable extends OrMwcTable {

    @property()
    public alarms: SentAlarm[] = [];

    @property({type: Boolean})
    public readonly = true;

    // Setup columns
    public columns: AlarmTableColumn[] = [
        {title: i18next.t("alarm.severity"), widthWeight: 3, isSortable: true},
        {title: i18next.t("alarm.title"), widthWeight: 25, isSortable: true},
        {title: i18next.t("alarm.status"), widthWeight: 3, isSortable: true},
        {title: i18next.t("alarm.linkedAssets"), widthWeight: 6, hideMobile: true},
        {title: i18next.t("alarm.assignee"), widthWeight: 4, isSortable: true},
        {title: i18next.t("alarm.lastModified"), widthWeight: 6, isSortable: true}
    ];

    // Config override
    protected config: TableConfig = {
        columnFilter: [],
        stickyFirstColumn: false,
        pagination: {
            enable: true
        },
        multiSelect: !this.readonly
    };

    protected sortIndex = 5;
    protected sortDirection: "ASC" | "DESC" = "DESC";

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: Map<string, any>): any {

        // If alarms changes, update content of the table accordingly
        if (changedProps.has("alarms") && this.alarms) {
            this.selectedRows = [];
            this.rows = this.getTableRows(this.alarms);
        }

        // If readonly state changes, update multi select
        if (changedProps.has("readonly")) {
            this.config.multiSelect = !this.readonly;
        }

        return super.willUpdate(changedProps);
    }


    protected getTableRows(alarms: SentAlarm[]): TableRow[] | undefined {
        return alarms?.map(a => {
            return {
                content: [
                    this.getSeverityContent(a.severity),
                    this.getDisplayNameContent(a.title),
                    this.getAlarmStatusContent(a.status),
                    this.getLinkedAssetsContent(a.asset),
                    this.getAssigneeContent(a.assigneeUsername),
                    this.getLastModifiedContent(a.lastModified)
                ],
                clickable: true
            } as TableRow;
        });
    }

    /* --------------------------------------- */

    /* TEMPLATING FUNCTIONS */

    protected getSeverityContent(severity?: AlarmSeverity): TemplateResult | string | number {
        const classes = {
            "alarm-text": true,
            "alarm-severity-text__low": severity === AlarmSeverity.LOW,
            "alarm-severity-text__medium": severity === AlarmSeverity.MEDIUM,
            "alarm-severity-text__high": severity === AlarmSeverity.HIGH
        };
        return html`
            <or-translate class=${classMap(classes)}
                          value="${severity ? `alarm.severity_${severity}` : "error"}"></or-translate>
        `;
    }

    protected getDisplayNameContent(title?: string): TableContent {
        return title;
    }

    protected getAlarmStatusContent(status?: AlarmStatus): TableContent {
        const classes = {
            "alarm-text": true,
            "alarm-status-text__closed": status === AlarmStatus.CLOSED,
            "alarm-status-text__resolved": status === AlarmStatus.RESOLVED,
            "alarm-status-text__acknowledged": status === AlarmStatus.ACKNOWLEDGED,
            "alarm-status-text__in-progress": status === AlarmStatus.IN_PROGRESS,
            "alarm-status-text__open": status === AlarmStatus.OPEN
        };
        return html`
            <or-translate class=${classMap(classes)}
                          value="${status ? `alarm.status_${status}` : "error"}"></or-translate>
        `;
    }

    protected getLinkedAssetsContent(assets?: Asset[]): TableContent {
        return html`${assets?.map(a => a.name).join(', ')}`;
    }

    protected getAssigneeContent(assigneeName?: string): TableContent {
        return assigneeName;
    }

    protected getLastModifiedContent(lastModified?: number): TableContent {
        return new Date(lastModified);
    }

    protected sortTemplateRows(cellA: any, cellB: any, cIndex: number, sortDirection: 'ASC' | 'DESC'): number {
        const valueA: string | undefined = (cellA.values as any[]).filter(v => typeof v === 'string' || typeof v === 'number').map(v => v.toString())?.[0];
        const valueB: string | undefined = (cellB.values as any[]).filter(v => typeof v === 'string' || typeof v === 'number').map(v => v.toString())?.[0];
        if (valueA !== undefined && valueB !== undefined) {
            if (valueA.includes("alarm.status") && valueB.includes("alarm.status")) {
                const sortingArr = ["alarm.status_OPEN", "alarm.status_ACKNOWLEDGED", "alarm.status_IN_PROGRESS", "alarm.status_RESOLVED", "alarm.status_CLOSED"];
                if (sortDirection === 'DESC') {
                    return sortingArr.indexOf(valueB) - sortingArr.indexOf(valueA);
                } else {
                    return sortingArr.indexOf(valueA) - sortingArr.indexOf(valueB);
                }
            }
            if (valueA.includes("alarm.severity") && valueB.includes("alarm.severity")) {
                const sortingArr = ["alarm.severity_LOW", "alarm.severity_MEDIUM", "alarm.severity_HIGH"];
                if (sortDirection === 'DESC') {
                    return sortingArr.indexOf(valueB) - sortingArr.indexOf(valueA);
                } else {
                    return sortingArr.indexOf(valueA) - sortingArr.indexOf(valueB);
                }
            }

            return this.sortPrimitiveRows([valueA], [valueB], 0, sortDirection);
        } else {
            return 1;
        }
    }


    /* --------------------------------------- */

    /* STYLING MODIFICATIONS */

    /** Applies the custom 'weighted width' field of AlarmTableColumn, to make columns wider/smaller relative to others. */
    protected getColumnWidth(index: number, columns?: string[] | TableColumn[], tableWidthPx?: number): string | undefined {
        const column = columns[index];
        const totalWeight = this.getTotalColumnWidthWeight(columns);
        if (typeof column !== "string" && totalWeight && (column as AlarmTableColumn).widthWeight) {
            return `${(column as AlarmTableColumn).widthWeight / totalWeight * 100}%`;
        }
        return super.getColumnWidth(index, columns, tableWidthPx);
    }

    /** In AlarmTableColumn, columns can have a 'weighted width' which makes them wider/smaller relative to others.
     * This function calculates the total of these values. */
    protected getTotalColumnWidthWeight(columns?: string[] | TableColumn[]): number | undefined {
        let totalWeight = 0;
        columns.forEach(c => {
            if (typeof c !== "string") {
                totalWeight = totalWeight + (c as AlarmTableColumn).widthWeight;
            }
        });
        return totalWeight;
    }
}
