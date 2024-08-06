import { LitElement, PropertyValues } from "lit";
import { RuleActionNotification, AssetQuery } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
declare const OrRuleNotificationModal_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleNotificationModal extends OrRuleNotificationModal_base {
    action: RuleActionNotification;
    title: string;
    query?: AssetQuery;
    constructor();
    initDialog(): void;
    renderDialogHTML(action: RuleActionNotification): void;
    firstUpdated(changedProperties: PropertyValues): void;
    checkForm(): void;
    protected render(): import("lit-html").TemplateResult<1>;
}
export {};
