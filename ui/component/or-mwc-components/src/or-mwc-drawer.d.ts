import { LitElement, TemplateResult } from "lit";
import { MDCDrawer } from "@material/drawer";
export declare class OrMwcDrawerChangedEvent extends CustomEvent<boolean> {
    static readonly NAME = "or-mwc-drawer-changed";
    constructor(value: boolean);
}
export declare class OrMwcDrawer extends LitElement {
    static get styles(): import("lit").CSSResult[];
    header: TemplateResult;
    dismissible: boolean;
    rightSided: boolean;
    transparent: boolean;
    open: boolean;
    appContent: HTMLElement;
    topBar: HTMLElement;
    protected drawerElement: HTMLElement;
    protected drawer?: MDCDrawer;
    toggle(): void;
    disconnectedCallback(): void;
    protected render(): TemplateResult<1>;
    protected updated(): void;
    protected dispatchChangedEvent(value: boolean): void;
    protected firstUpdated(): void;
}
