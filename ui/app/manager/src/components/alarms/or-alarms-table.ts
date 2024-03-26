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
    padding: 4px;
    border-radius: 5px;
  }

  .alarm-status-text__closed {
    color: var(--or-alarm-status-color__closed, grey);
    background-color: var(--or-alarm-status-background__closed);
    border: var(--or-alarm-status-border__closed, 1px grey solid);
  }

  .alarm-status-text__resolved {
    color: var(--or-alarm-status-color__resolved);
    background-color: var(--or-alarm-status-background__resolved);
    border: var(--or-alarm-status-border__resolved);
  }

  .alarm-status-text__acknowledged {
    color: var(--or-alarm-status-color__acknowledged);
    background-color: var(--or-alarm-status-background__acknowledged);
    border: var(--or-alarm-status-border__acknowledged, 1px mediumblue solid);
  }

  .alarm-status-text__in-progress {
    color: var(--or-alarm-status-color__in-progress, white);
    background-color: var(--or-alarm-status-background__in-progress, green);
    border: var(--or-alarm-status-border__in-progress);
  }

  .alarm-status-text__open {
    color: var(--or-alarm-status-color__open, white);
    background-color: var(--or-alarm-status-background__open, mediumblue);
    border: var(--or-alarm-status-border__open);
  }

    .alarm-severity-text__low {
        color: white;
        background-color: mediumblue;
    }

    .alarm-severity-text__medium {
        color: white;
        background-color: darkorange;
    }

    .alarm-severity-text__high {
        color: white;
        background-color: red;
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

    static get styles() {
        return [...super.styles, styling];
    }

    protected willUpdate(changedProps: Map<string, any>): any {

        // If alarms changes, update content of the table accordingly
        if (changedProps.has("alarms") && this.alarms) {
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
            <or-translate class=${classMap(classes)} value="${severity ? `alarm.severity_${severity}` : "error"}"></or-translate>
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
            <or-translate class=${classMap(classes)} value="${status ? `alarm.status_${status}` : "error"}"></or-translate>
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
