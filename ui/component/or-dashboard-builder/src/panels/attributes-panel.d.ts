import { CSSResult, LitElement, PropertyValues, TemplateResult } from "lit";
import { Asset, Attribute, AttributeRef } from "@openremote/model";
import "@openremote/or-translate";
export interface AttributeAction {
    icon: string;
    tooltip: string;
    disabled: boolean;
}
export declare class AttributeActionEvent extends CustomEvent<{
    asset: Asset;
    attributeRef: AttributeRef;
    action: AttributeAction;
}> {
    static readonly NAME = "attribute-action";
    constructor(asset: Asset, attributeRef: AttributeRef, action: AttributeAction);
}
export declare class AttributesSelectEvent extends CustomEvent<{
    assets: Asset[];
    attributeRefs: AttributeRef[];
}> {
    static readonly NAME = "attribute-select";
    constructor(assets: Asset[], attributeRefs: AttributeRef[]);
}
export declare class AttributesPanel extends LitElement {
    protected attributeRefs: AttributeRef[];
    protected multi: boolean;
    protected onlyDataAttrs: boolean;
    protected attributeFilter?: (attribute: Attribute<any>) => boolean;
    protected attributeLabelCallback?: (asset: Asset, attribute: Attribute<any>, attributeLabel: string) => TemplateResult;
    protected attributeActionCallback?: (attribute: AttributeRef) => AttributeAction[];
    protected loadedAssets: Asset[];
    static get styles(): CSSResult[];
    protected willUpdate(changedProps: PropertyValues): void;
    protected getLoadedAsset(attrRef: AttributeRef): Asset | undefined;
    protected removeWidgetAttribute(attributeRef: AttributeRef): void;
    protected loadAssets(): Promise<Asset[]>;
    fetchAssets(attributeRefs?: AttributeRef[]): Promise<Asset[]>;
    protected onAttributeActionClick(asset: Asset, attributeRef: AttributeRef, action: AttributeAction): void;
    protected openAttributeSelector(attributeRefs: AttributeRef[], multi: boolean, onlyDataAttrs?: boolean, attributeFilter?: (attribute: Attribute<any>) => boolean): void;
    protected render(): TemplateResult;
}
