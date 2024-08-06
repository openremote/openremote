import { LitElement, PropertyValues, TemplateResult } from "lit";
import { MDCDialog } from "@material/dialog";
import "@openremote/or-translate";
import "./or-mwc-input";
export interface DialogActionBase {
    actionName: string;
    action?: (dialog: OrMwcDialog) => void;
}
export interface DialogAction extends DialogActionBase {
    default?: boolean;
    content: TemplateResult | string;
    disabled?: boolean;
}
export declare class OrMwcDialogOpenedEvent extends CustomEvent<void> {
    static readonly NAME = "or-mwc-dialog-opened";
    constructor();
}
export declare class OrMwcDialogClosedEvent extends CustomEvent<string | undefined> {
    static readonly NAME = "or-mwc-dialog-closed";
    constructor(action?: string);
}
declare global {
    export interface HTMLElementEventMap {
        [OrMwcDialogOpenedEvent.NAME]: OrMwcDialogOpenedEvent;
        [OrMwcDialogClosedEvent.NAME]: OrMwcDialogClosedEvent;
    }
}
export declare function showErrorDialog(errorMessage: string, hostElement?: HTMLElement): Promise<boolean>;
export declare function showOkCancelDialog(title: string, content: string | TemplateResult, okText?: string, hostElement?: HTMLElement): Promise<boolean>;
export declare function showOkDialog(title: string, content: string | TemplateResult, okText?: string, hostElement?: HTMLElement): Promise<boolean>;
export declare function showDialog<T extends OrMwcDialog>(dialog: T, hostElement?: HTMLElement): T;
export declare class OrMwcDialog extends LitElement {
    /**
     * Can be set by apps to control where in the DOM dialogs are added
     */
    static DialogHostElement: HTMLElement;
    static get styles(): import("lit").CSSResult[];
    heading?: string | TemplateResult;
    content?: TemplateResult | (() => TemplateResult);
    actions?: DialogAction[];
    dismissAction: DialogActionBase | null | undefined;
    avatar?: boolean;
    styles?: TemplateResult | string;
    protected _open: boolean;
    protected _mdcElem: HTMLElement;
    protected _mdcComponent?: MDCDialog;
    get isOpen(): boolean;
    setOpen(isOpen: boolean): OrMwcDialog;
    setHeading(heading: TemplateResult | string | undefined): OrMwcDialog;
    setContent(content: TemplateResult | (() => TemplateResult) | undefined): OrMwcDialog;
    setActions(actions: DialogAction[] | undefined): OrMwcDialog;
    setDismissAction(action: DialogActionBase | null | undefined): OrMwcDialog;
    setStyles(styles: string | TemplateResult | undefined): OrMwcDialog;
    setAvatar(avatar: boolean | undefined): OrMwcDialog;
    open(): void;
    close(action?: string): void;
    disconnectedCallback(): void;
    protected render(): TemplateResult<1>;
    protected updated(_changedProperties: PropertyValues): void;
    protected _onDialogOpened(): void;
    protected _onDialogClosed(action?: string): void;
}
