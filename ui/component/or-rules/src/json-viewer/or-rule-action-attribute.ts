import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {getAssetIdsFromQuery, getAssetTypeFromQuery, RulesConfig} from "../index";
import {
    Asset,
    AssetQueryOrderBy$Property,
    AssetTypeInfo,
    RuleActionUpdateAttribute,
    RuleActionWriteAttribute,
    WellknownMetaItems,
    WellknownValueTypes,
    AssetModelUtil,
    AssetQuery,
    AssetQueryMatch
} from "@openremote/model";
import manager, {Util} from "@openremote/core";
import "@openremote/or-attribute-input";
import {InputType, OrInputChangedEvent, OrInputSearchChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {translate} from "@openremote/or-translate";
import {OrAttributeInputChangedEvent} from "@openremote/or-attribute-input";
import { ifDefined } from "lit/directives/if-defined.js";
import { when } from "lit/directives/when.js";
import {debounce} from "lodash";

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

    .min-width {
        min-width: 200px;
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
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public assetProvider!: (type: string) => Promise<Asset[] | undefined>

    @property({type: Array, attribute: false})
    protected _assets?: Asset[];

    @state()
    protected _assetSearchValue: string = '';


    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("action")) {
            this._assets = undefined;
        }
        return super.shouldUpdate(_changedProperties);
    }

    public refresh() {
        // Clear assets
        this._assets = undefined;
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
        const assetDescriptor = this.assetInfos ? this.assetInfos.find((assetTypeInfo) => assetTypeInfo.assetDescriptor!.name === assetType) : undefined;

        if (!assetDescriptor) {
            return html``;
        }

        if (!this._assets) {
            this.loadAssets(assetType);
        }

        // TODO: Add multiselect support
        const ids = getAssetIdsFromQuery(query);
        const idValue = ids && ids.length > 0 ? ids[0] : "*";
        const idOptions: [string, string] [] = [
            ["*", i18next.t("matched")]
        ];

        return html`
            
            <!-- Show SELECT input with 'loading' until the assets are retrieved -->
            ${when((!this._assets), () => html`
                <or-mwc-input id="matchSelect" class="min-width" type="${InputType.SELECT}" .readonly="${true}" .label="${i18next.t('loading')}"></or-mwc-input>
            `, () => {
                
                // Set list of displayed assets
                if(this._assets!.length <= 100) {
                    idOptions.push(...this._assets!.map((asset) => [asset.id!, asset.name!] as [string, string]));
                } else if(this._assetSearchValue.length > 0) {
                    const filtered = this._assets!.filter((asset) => (asset.name?.toLowerCase().includes(this._assetSearchValue.toLowerCase()) || asset.id == idValue));
                    idOptions.push(...filtered.map((asset) => [asset.id!, asset.name!] as [string, string]))
                } else {
                    const asset = this._assets?.find((asset) => asset.id == idValue);
                    if(asset) { idOptions.push([asset.id!, asset.name!]); }
                }
                
                // Get selected asset and its descriptors
                const asset = idValue && idValue !== "*" ? this._assets!.find(a => a.id === idValue) : undefined;
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetType, this.action.attributeName, asset && asset.attributes && this.action.attributeName ? asset.attributes[this.action.attributeName] : undefined);

                // Only RW attributes can be used in actions
                let attributes: [string, string][] = [];
                if (asset && asset.attributes) {
                    attributes = Object.values(asset.attributes)
                            .map((attr) => {
                                const label = Util.getAttributeLabel(attr, descriptors[0], assetType, false);
                                return [attr.name!, label];
                            });
                } else if (assetDescriptor) {
                    const assetTypeInfo = AssetModelUtil.getAssetTypeInfo(assetDescriptor);

                    attributes =
                            !assetTypeInfo || !assetTypeInfo.attributeDescriptors
                                    ? []
                                    : assetTypeInfo.attributeDescriptors.map((ad) => {
                                        const label = Util.getAttributeLabel(ad, descriptors[0], assetType, false);
                                        return [ad.name!, label];
                                    });
                }

                attributes.sort(Util.sortByString((attr) => attr[1]));
                let attributeInput: TemplateResult | undefined;
                if (this.action.attributeName) {
                    const label = descriptors[1] && (descriptors[1].name === WellknownValueTypes.BOOLEAN) ? "" : i18next.t("value");
                    let inputType;
                    if(descriptors[0]?.format?.asSlider) inputType = InputType.NUMBER;
                    attributeInput = html`<or-attribute-input ?compact=${descriptors[1] && (descriptors[1].name === WellknownValueTypes.GEOJSONPOINT)} .inputType="${ifDefined(inputType)}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setActionAttributeValue(ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${label}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${this.action.value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
                }
                
                return html`
                    <or-mwc-input id="matchSelect" class="min-width" .label="${i18next.t("asset")}" .type="${InputType.SELECT}"
                                  .searchable="${true}" .options="${idOptions}" .value="${idValue}" .readonly="${this.readonly || false}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => { this._assetId = (e.detail.value); this._assetSearchValue = ''; this.refresh(); }}"
                                  @or-mwc-inputsearch-changed="${debounce((event: OrInputSearchChangedEvent) => { this._assetSearchValue = event.detail.value; }, 500)}"
                    ></or-mwc-input>
                    ${attributes.length > 0 ? html`
                        <or-mwc-input id="attributeSelect" class="min-width" .label="${i18next.t("attribute")}" .type="${InputType.SELECT}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAttributeName(e.detail.value)}" .readonly="${this.readonly || false}" ?searchable="${(attributes.length >= 25)}" .options="${attributes}" .value="${this.action.attributeName}"></or-mwc-input>
                        ${attributeInput}
                    ` : html`
                        <or-translate value="No attributes with write permission"></or-translate>
                    `}
                `;
            })}
        `;
    }

    protected set _assetId(assetId: string) {
        const assetType = this._getAssetType();


        if (assetId === "*") {
            this.action.target!.assets = undefined;
            this.action.target = {
                matchedAssets: {
                    types: [
                         assetType || ""
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
                        assetType || ""
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
        this.assetProvider(type).then(assets => {
            this._assets = assets;
        })
    }
}
