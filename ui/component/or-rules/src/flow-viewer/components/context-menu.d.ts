import { LitElement } from "lit";
import { ContextMenuEntry } from "../models/context-menu-button";
declare const ContextMenu_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class ContextMenu extends ContextMenu_base {
    static get opened(): boolean;
    static get styles(): import("lit").CSSResult;
    static open(x: number, y: number, container: Element, buttons: (ContextMenuEntry)[]): void;
    static close(): void;
    private static main;
    private entries;
    private isOpen;
    private container;
    protected firstUpdated(): void;
    protected updated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private closeCallback;
    private buttonTemplate;
    private separatorTemplate;
}
export {};
