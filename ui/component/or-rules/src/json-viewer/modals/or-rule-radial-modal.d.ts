import { LitElement } from "lit";
import { AssetDescriptor, AttributePredicate, AssetQuery, RadialGeofencePredicate } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-map";
declare const OrRuleRadialModal_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleRadialModal extends OrRuleRadialModal_base {
    assetDescriptor?: AssetDescriptor;
    attributePredicate?: AttributePredicate;
    query?: AssetQuery;
    constructor();
    initRadialMap(): void;
    protected getAttributeName(attributePredicate: AttributePredicate): string | undefined;
    protected setValuePredicateProperty(propertyName: string, value: any): void;
    renderDialogHTML(value: RadialGeofencePredicate): void;
    protected render(): import("lit-html").TemplateResult<1>;
}
export {};
