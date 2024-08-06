import { LitElement, PropertyValues } from "lit";
import { MDCSnackbar } from "@material/snackbar";
export interface OrMwcSnackbarChangedEventDetail {
    opened: boolean;
    closeReason?: string;
}
export declare class OrMwcSnackbarChangedEvent extends CustomEvent<OrMwcSnackbarChangedEventDetail> {
    static readonly NAME = "or-mwc-snackbar-changed";
    constructor(value: OrMwcSnackbarChangedEventDetail);
}
declare global {
    export interface HTMLElementEventMap {
        [OrMwcSnackbarChangedEvent.NAME]: OrMwcSnackbarChangedEvent;
    }
}
export declare function showSnackbar(hostElement: HTMLElement | undefined, text: string, buttonText?: string, buttonAction?: () => void): OrMwcSnackbar;
export declare class OrMwcSnackbar extends LitElement {
    /**
     * Can be set by apps to control where in the DOM snackbars are added
     */
    static DialogHostElement: HTMLElement;
    static get styles(): import("lit").CSSResult[];
    text: string;
    buttonText?: string;
    buttonAction?: () => void;
    timeout?: number;
    _open: boolean;
    protected _mdcElem: HTMLElement;
    protected _mdcComponent?: MDCSnackbar;
    get isOpen(): boolean;
    set isOpen(isOpen: boolean);
    open(): void;
    close(action?: string): void;
    disconnectedCallback(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    protected updated(_changedProperties: PropertyValues): void;
    protected onClose(reason: string | undefined): void;
    protected onOpen(): void;
    protected dispatchChangedEvent(detail: OrMwcSnackbarChangedEventDetail): void;
}
