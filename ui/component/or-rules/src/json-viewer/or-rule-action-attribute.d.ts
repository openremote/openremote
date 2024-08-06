import { LitElement, PropertyValues, TemplateResult } from "lit";
import { RulesConfig } from "../index";
import { Asset, AssetTypeInfo, RuleActionUpdateAttribute, RuleActionWriteAttribute } from "@openremote/model";
import "@openremote/or-attribute-input";
declare const OrRuleActionAttribute_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleActionAttribute extends OrRuleActionAttribute_base {
    static get styles(): import("lit").CSSResult;
    action: RuleActionWriteAttribute | RuleActionUpdateAttribute;
    targetTypeMap?: [string, string?][];
    readonly?: boolean;
    config?: RulesConfig;
    assetInfos?: AssetTypeInfo[];
    assetProvider: (type: string) => Promise<Asset[] | undefined>;
    protected _assets?: Asset[];
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    refresh(): void;
    protected _getAssetType(): string | undefined;
    protected render(): TemplateResult<1>;
    protected set _assetId(assetId: string);
    protected setActionAttributeName(name: string | undefined): void;
    protected setActionAttributeValue(value: any): void;
    protected loadAssets(type: string): void;
}
export {};
