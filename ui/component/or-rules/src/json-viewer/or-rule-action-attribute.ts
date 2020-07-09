import {customElement, html, LitElement, property, PropertyValues, css, TemplateResult} from "lit-element";
import {getAssetIdsFromQuery, getAssetTypeFromQuery, RulesConfig} from "../index";
import {
    Asset,
    AssetDescriptor,
    AssetQueryOrderBy$Property,
    Attribute,
    AttributeValueType,
    MetaItemType,
    RuleActionUpdateAttribute,
    RuleActionWriteAttribute,
    ValueType
} from "@openremote/model";
import manager, {AssetModelUtil, Util} from "@openremote/core";
import "@openremote/or-attribute-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import i18next from "i18next";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import { translate } from "@openremote/or-translate";
import { OrAttributeInputChangedEvent } from "@openremote/or-attribute-input";

// language=CSS
const style = css`
    :host {
        display: flex;
        align-items: center;

        flex-wrap: wrap;
    }

    :host > * {
        margin: 0 3px 6px;
    }
`;

@customElement("or-rule-action-attribute")
export class OrRuleActionAttribute extends translate(i18next)(LitElement) {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public action!: RuleActionWriteAttribute | RuleActionUpdateAttribute;

    @property({type: Object, attribute: false})
    public targetTypeMap?: [string, string?][];

    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Object})
    public assetDescriptors?: AssetDescriptor[];

    @property({type: Array, attribute: false})
    protected _assets?: Asset[];

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("action")) {
            this._assets = undefined;
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected _getAssetType() {
        if (!this.action.target) {
            return;
        }
        const query = this.action.target.assets ? this.action.target.assets : this.action.target.matchedAssets ? this.action.target.matchedAssets : undefined;
        return query ? getAssetTypeFromQuery(query) : undefined;
    }

    protected render() {

        if (!this.action.target) {
            return html``;
        }

        const assetType = this._getAssetType();

        if (!assetType) {
            return html``;
        }

        const query = this.action.target.assets ? this.action.target.assets : this.action.target.matchedAssets ? this.action.target.matchedAssets : undefined;
        const assetDescriptor = this.assetDescriptors ? this.assetDescriptors.find((ad) => ad.type === assetType) : undefined;

        if (!assetDescriptor) {
            return html``;
        }

        if (!this._assets) {
            this.loadAssets(assetType);
            return html``;
        }

        // TODO: Add multiselect support
        const ids = getAssetIdsFromQuery(query);
        const idValue = ids && ids.length > 0 ? ids[0] : "*";
        const idOptions: [string, string] [] = [
            ["*", i18next.t("matched")]
        ];

        idOptions.push(...this._assets!.map((asset) => [asset.id!, asset.name!] as [string, string]));

        const asset = idValue && idValue !== "*" ? this._assets.find(a => a.id === idValue) : undefined;
        const showUpdateOptions = !this.config || !this.config.controls || !this.config.controls.hideActionUpdateOptions;
        const attributeDescriptor = AssetModelUtil.getAssetAttributeDescriptor(assetDescriptor, this.action.attributeName);
        let attribute: Attribute | undefined = asset && this.action.attributeName ? Util.getAssetAttribute(asset, this.action.attributeName) : undefined;
        const attributeValueDescriptor = attributeDescriptor && attributeDescriptor.valueDescriptor ? typeof attributeDescriptor.valueDescriptor === "string" ? AssetModelUtil.getAttributeValueDescriptor(attributeDescriptor.valueDescriptor as string) : attributeDescriptor.valueDescriptor : attribute ? AssetModelUtil.getAttributeValueDescriptor(attribute.type as string) : undefined;

        // Only RW attributes can be used in actions
        let attributes: [string, string][];
        if (asset) {
            attributes = Util.getAssetAttributes(asset).filter((attr) => !Util.hasMetaItem(attr, MetaItemType.READ_ONLY.urn!))
                .map((attr) => {
                    const attrDescriptor = AssetModelUtil.getAssetAttributeDescriptor(assetDescriptor, attr.name);
                    return [attr.name!, Util.getAttributeLabel(attr, attrDescriptor)];
                });
        } else {
            attributes = !assetDescriptor || !assetDescriptor.attributeDescriptors ? []
                : assetDescriptor.attributeDescriptors.filter((ad) => !AssetModelUtil.hasMetaItem(ad, MetaItemType.READ_ONLY.urn!))
                    .map((ad) => [ad.attributeName!, Util.getAttributeLabel(undefined, ad)]);
        }

        let attributeInput: TemplateResult | undefined;

        if (this.action.attributeName) {
            const label = (attributeValueDescriptor && (attributeValueDescriptor.valueType === ValueType.BOOLEAN || attributeValueDescriptor.name === AttributeValueType.BOOLEAN.name || attributeValueDescriptor.name === AttributeValueType.SWITCH_TOGGLE.name)) ? "" : i18next.t("value");
            attributeInput = html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setActionAttributeValue(ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${label}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${this.action.value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
        }

        return html`
            <or-input id="matchSelect" .label="${i18next.t("asset")}" .type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this._assetId = (e.detail.value)}" .readonly="${this.readonly || false}" .options="${idOptions}" .value="${idValue}"></or-input>
            ${attributes.length > 0 ? html`
                <or-input id="attributeSelect" .label="${i18next.t("attribute")}" .type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionAttributeName(e.detail.value)}" .readonly="${this.readonly || false}" .options="${attributes}" .value="${this.action.attributeName}"></or-input>
                ${attributeInput}
            ` : html`
                <or-translate value="No attributes with write permission"></or-translate>
            `}
        `;
    }

    protected set _assetId(assetId: string) {
        const assetType = this._getAssetType();

        if (assetId === "*") {
            this.action.target!.assets = undefined;
            this.action.target = {
                matchedAssets: {
                    types: [
                        {
                            predicateType: "string",
                            value: assetType
                        }
                    ]
                }
            }
        } else {
            this.action.target!.matchedAssets = undefined;
            this.action.target = {
                assets: {
                    ids: [
                        assetId
                    ],
                    types: [
                        {
                            predicateType: "string",
                            value: assetType
                        }
                    ]
                }
            }
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeName(name: string | undefined) {
        this.action.attributeName = name;
        this.action.value = undefined;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeValue(value: any) {
        this.action.value = value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected loadAssets(type: string) {
        manager.rest.api.AssetResource.queryAssets({
            types: [
                {
                    predicateType: "string",
                    value: type
                }
            ],
            select: {
                excludeAttributeTimestamp: true,
                excludeAttributeValue: true,
                excludeParentInfo: true,
                excludePath: true
            },
            orderBy: {
                property: AssetQueryOrderBy$Property.NAME
            }
        }).then((response) => this._assets = response.data);
    }
}
