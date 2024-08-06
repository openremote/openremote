import { LitElement, TemplateResult } from "lit";
import { MDCDataTable } from "@material/data-table";
export interface TableConfig {
    columnFilter?: string[];
    stickyFirstColumn?: boolean;
    fullHeight?: boolean;
    pagination?: {
        enable?: boolean;
        options?: number[];
    };
    multiSelect?: boolean;
}
export interface TableColumn {
    title?: string;
    isNumeric?: boolean;
    hideMobile?: boolean;
    isSortable?: boolean;
}
export interface TableRow {
    content?: (string | number | TemplateResult)[];
    clickable?: boolean;
}
export interface OrMwcTableRowClickDetail {
    index: number;
}
export declare class OrMwcTableRowClickEvent extends CustomEvent<OrMwcTableRowClickDetail> {
    static readonly NAME = "or-mwc-table-row-click";
    constructor(index: number);
}
export declare class OrMwcTable extends LitElement {
    static get styles(): import("lit").CSSResult[];
    columns?: TableColumn[] | string[];
    protected columnsTemplate?: TemplateResult;
    rows?: TableRow[] | string[][];
    protected rowsTemplate?: TemplateResult;
    protected config: TableConfig;
    protected paginationIndex: number;
    protected paginationSize: number;
    protected _dataTable?: MDCDataTable;
    protected sortDirection?: 'ASC' | 'DESC';
    protected sortIndex?: number;
    selectedRows?: TableRow[] | string[][] | any[];
    protected indeterminate?: boolean;
    protected allSelected?: boolean;
    protected firstUpdated(changedProperties: Map<string, any>): void;
    protected updated(changedProperties: Map<string, any>): void;
    protected render(): TemplateResult<1>;
    protected onRowClick(ev: MouseEvent, item: TableRow | string[]): void;
    getSortHeader(index: number, title: string, sortDirection: 'ASC' | 'DESC', arrowOnLeft?: boolean): Promise<TemplateResult>;
    sortRows(ev: MouseEvent, index: number, sortDirection: 'ASC' | 'DESC'): Promise<void>;
    protected customSort(a: any, b: any, index: number, sortDirection: 'ASC' | 'DESC'): number;
    protected onCheckChanged(checked: boolean, type: "all" | "single", item?: any): void;
    getPaginationControls(): Promise<TemplateResult>;
    getRowCount(wait?: boolean, tableElem?: HTMLElement): Promise<number>;
    getTableElem(wait?: boolean): Promise<HTMLElement | undefined>;
}
