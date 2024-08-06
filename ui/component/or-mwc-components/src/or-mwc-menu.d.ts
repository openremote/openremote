import { LitElement, PropertyValues, TemplateResult } from "lit";
import { MDCMenu } from "@material/menu";
import { ListItem } from "./or-mwc-list";
export declare class OrMwcMenuChangedEvent extends CustomEvent<any[] | any> {
    static readonly NAME = "or-mwc-menu-changed";
    constructor(values: any[] | any);
}
export declare class OrMwcMenuClosedEvent extends CustomEvent<void> {
    static readonly NAME = "or-mwc-menu-closed";
    constructor();
}
declare global {
    export interface HTMLElementEventMap {
        [OrMwcMenuChangedEvent.NAME]: OrMwcMenuChangedEvent;
        [OrMwcMenuClosedEvent.NAME]: OrMwcMenuClosedEvent;
    }
}
export declare function getContentWithMenuTemplate(content: TemplateResult, menuItems: (ListItem | ListItem[] | null)[], selectedValues: string[] | string | undefined, valueChangedCallback: (values: string[] | string) => void, closedCallback?: () => void, multiSelect?: boolean, translateValues?: boolean, midHeight?: boolean, fullWidth?: boolean): TemplateResult;
export declare class OrMwcMenu extends LitElement {
    static get styles(): import("lit").CSSResult[];
    menuItems?: (ListItem | ListItem[] | null)[];
    values?: any[] | any;
    multiSelect?: boolean;
    visible?: boolean;
    translateValues?: boolean;
    midHeight?: boolean;
    fullWidth?: boolean;
    protected _wrapperElem: HTMLElement;
    protected _mdcElem: HTMLElement;
    protected _mdcComponent?: MDCMenu;
    open(): void;
    disconnectedCallback(): void;
    protected render(): TemplateResult<1>;
    protected getItemsTemplate(items: (ListItem | ListItem[] | null)[], translate?: boolean): TemplateResult;
    protected firstUpdated(_changedProperties: PropertyValues): void;
    protected updated(_changedProperties: PropertyValues): void;
    protected _onMenuClosed(): void;
    private _itemClicked;
}
