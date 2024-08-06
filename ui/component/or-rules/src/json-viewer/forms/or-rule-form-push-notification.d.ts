import { LitElement } from "lit";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-mwc-components/or-mwc-input";
import { RuleActionNotification } from "@openremote/model";
declare const OrRuleFormPushNotification_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleFormPushNotification extends OrRuleFormPushNotification_base {
    action: RuleActionNotification;
    static get styles(): import("lit").CSSResult;
    protected updated(_changedProperties: Map<PropertyKey, unknown>): void;
    protected render(): import("lit-html").TemplateResult<1>;
    protected setActionNotificationName(value: string | undefined, key?: string): void;
}
export {};
