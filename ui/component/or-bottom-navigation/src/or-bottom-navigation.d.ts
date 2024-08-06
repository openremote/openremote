import { LitElement } from "lit";
import "./or-navigation-item";
import { FlattenedNodesObserver } from "@polymer/polymer/lib/utils/flattened-nodes-observer";
import { OrNavigationItem } from "./or-navigation-item";
export declare class OrBottomNavigation extends LitElement {
    static _iconsCss: HTMLLinkElement | null;
    static iconsUrl: string;
    protected _observer?: FlattenedNodesObserver;
    protected _temp: number[];
    protected _virtualItems: OrNavigationItem[];
    private _itemsSlot;
    constructor();
    static styles: import("lit").CSSResult;
    connectedCallback(): void;
    firstUpdated(): void;
    disconnectedCallback(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private _onAdd;
    private _onRemove;
    protected static _virtualTemplate(navItem: OrNavigationItem): import("lit-html").TemplateResult<1>;
}
