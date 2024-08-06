import { LitElement, PropertyValues, TemplateResult } from "lit";
import { Asset, AssetDescriptor } from "@openremote/model";
import { ListItem } from "@openremote/or-mwc-components/or-mwc-list";
export declare class AssetTypeSelectEvent extends CustomEvent<string> {
    static readonly NAME = "assettype-select";
    constructor(assetTypeName: string);
}
export declare class AssetIdsSelectEvent extends CustomEvent<string | string[]> {
    static readonly NAME = "assetids-select";
    constructor(assetIds: string | string[]);
}
export declare class AttributeNamesSelectEvent extends CustomEvent<string | string[]> {
    static readonly NAME = "attributenames-select";
    constructor(attributeNames: string | string[]);
}
export interface AssetTypesFilterConfig {
    assets?: {
        enabled?: boolean;
        multi?: boolean;
    };
    attributes?: {
        enabled?: boolean;
        multi?: boolean;
        valueTypes?: string[];
    };
}
export declare class AssettypesPanel extends LitElement {
    protected assetType?: string;
    protected config: AssetTypesFilterConfig;
    protected assetIds: undefined | string | string[];
    protected attributeNames: undefined | string | string[];
    protected _attributeSelectList: string[][];
    protected _loadedAssetTypes: AssetDescriptor[];
    static get styles(): import("lit").CSSResult[];
    protected willUpdate(changedProps: PropertyValues): void;
    protected render(): TemplateResult;
    protected getAssetTypeTemplate(): TemplateResult;
    protected getSelectHeader(): TemplateResult;
    protected getSelectedHeader(descriptor: AssetDescriptor): TemplateResult;
    protected mapDescriptors(descriptors: AssetDescriptor[], withNoneValue?: ListItem): ListItem[];
    protected getAttributesByType(type: string): any[][] | undefined;
    protected _openAssetSelector(assetType: string, assetIds?: string[], multi?: boolean): void;
    protected assetTreeDataProvider: () => Promise<Asset[]>;
}
