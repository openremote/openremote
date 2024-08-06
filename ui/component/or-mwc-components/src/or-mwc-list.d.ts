import { LitElement, PropertyValues, TemplateResult } from "lit";
import { MDCList, MDCListActionEvent } from "@material/list";
import "@openremote/or-translate";
export { MDCListActionEvent };
export interface ListItem {
    icon?: string;
    trailingIcon?: string;
    text?: string;
    translate?: boolean;
    secondaryText?: string;
    value: any;
    data?: any;
    styleMap?: {
        [style: string]: string;
    };
}
export declare class OrMwcListChangedEvent extends CustomEvent<ListItem[]> {
    static readonly NAME = "or-mwc-list-changed";
    constructor(items: ListItem[]);
}
declare global {
    export interface HTMLElementEventMap {
        [OrMwcListChangedEvent.NAME]: OrMwcListChangedEvent;
    }
}
export declare enum ListType {
    PLAIN = "PLAIN",
    SELECT = "SELECT",
    RADIO = "RADIO",
    MULTI_CHECKBOX = "MULTI_CHECKBOX",
    MULTI_TICK = "MULTI_TICK"
}
export type ListGroupItem = {
    heading: string;
    list: TemplateResult;
};
export declare function createListGroup(lists: ListGroupItem[]): TemplateResult<1>;
export declare function getListTemplate(type: ListType, content: TemplateResult, isTwoLine?: boolean, role?: string, actionHandler?: (ev: MDCListActionEvent) => void): TemplateResult;
export declare function getItemTemplate(item: ListItem | null, index: number, selectedValues: any[], type: ListType, translate?: boolean, itemClickCallback?: (e: MouseEvent, item: ListItem) => void): TemplateResult;
export declare class OrMwcList extends LitElement {
    static get styles(): import("lit").CSSResult[];
    listItems?: (ListItem | null)[];
    values?: string[] | string;
    type: ListType;
    protected _wrapperElem: HTMLElement;
    protected _mdcElem: HTMLElement;
    protected _mdcComponent?: MDCList;
    constructor();
    disconnectedCallback(): void;
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    protected render(): TemplateResult;
    protected firstUpdated(_changedProperties: PropertyValues): void;
    get selectedItems(): ListItem[];
    setSelectedItems(items: ListItem | ListItem[] | string | string[] | undefined): void;
    protected _onSelected(ev: MDCListActionEvent): void;
}
