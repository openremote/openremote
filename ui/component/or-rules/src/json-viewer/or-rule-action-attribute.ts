import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {getAssetIdsFromQuery, getAssetTypeFromQuery, RulesConfig} from "../index";
import {
    Asset,
    AssetTypeInfo,
    RuleActionUpdateAttribute,
    RuleActionWriteAttribute,
    WellknownValueTypes,
    AssetModelUtil, AssetQuery
} from "@openremote/model";
import {Util} from "@openremote/core";
import "@openremote/or-attribute-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next, translate} from "@openremote/or-translate"
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {OrAttributeInputChangedEvent} from "@openremote/or-attribute-input";
import {ifDefined} from "lit/directives/if-defined.js";
import {when} from "lit/directives/when.js";

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
    public assetProvider!: (type: string, query?: AssetQuery) => Promise<Asset[] | undefined>

    @state()
    protected _cache?: {query: AssetQuery, assets: Asset[]};

    @state()
    protected _selected?: Asset;

    protected _loading = false;

    public shouldUpdate(changedProps: PropertyValues): boolean {
        if (changedProps.has("action")) {
            this._cache = undefined;
        }
        return super.shouldUpdate(changedProps);
    }

    public refresh() {
        // Clear assets
        this._cache = undefined;
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

        if (!this._cache && !this._loading) {
            this.loadAssets(assetType);
        }

        // TODO: Add multiselect support
        const ids = getAssetIdsFromQuery(query);
        const idValue = ids && ids.length > 0 ? ids[0] : "*";
        const idOptions: Map<string, string> = new Map([
            ["*", i18next.t("matched")]
        ]);

        // Set list of displayed assets, and filtering assets out if needed.
        // If <= 25 assets: display everything
        // If between 25 and 100 assets: display everything with search functionality
        // If >= 100 assets: only display if in line with search input
        const assets: Asset[] = this._cache ? this._cache.assets : [];
        const searchable = assets.length > 25;
        if(searchable && this._selected) {
            idOptions.set(this._selected.id!, this._selected.name!);
        }

        let searchProvider: (search?: string) => Promise<[any, string][]>;

        return html`
            
            <!-- Show SELECT input with 'loading' until the assets are retrieved -->
            ${when((!this._cache || this._loading), () => html`
                <or-mwc-input id="matchSelect" class="min-width" type="${InputType.SELECT}" .readonly="${true}" .label="${i18next.t('loading')}"></or-mwc-input>
            `, () => {
                if (!searchable) {
                    assets.forEach(a => idOptions.set(a.id!, a.name!));
                } else {
                    searchProvider = async (search?: string) => {
                        await this.loadAssets(assetType, search, idValue); // Wait for asset retrieval based on search
                        if (search) {
                            return assets.filter(a => a.name?.toLowerCase().includes(search.toLowerCase())).map(a => [a.id!, a.name!] as [string, string]);
                        } else if (assets.length <= 100) {
                            assets.forEach(a => idOptions.set(a.id!, a.name!));
                            return [...idOptions];
                        } else {
                            const selected = assets.find(a => a.id === idValue);
                            if (selected && !Array.from(idOptions.keys()).includes(selected.id!)) {
                                idOptions.set(selected.id!, selected.name!); // add selected asset if there is one.
                            }
                            return [...idOptions];
                        }
                    };
                }
                
                // Get selected asset and its descriptors
                const asset = idValue && idValue !== "*" ? this._cache!.assets.find(a => a.id === idValue) : undefined;
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
                                  .options="${[...idOptions]}" .searchProvider="${searchProvider}" .value="${idValue}" .readonly="${this.readonly || false}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => { this._assetId = (e.detail.value); }}"
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

    protected set _assetId(assetId: string | undefined) {
        const assetType = this._getAssetType();

        if (!assetId || assetId === "*") {
            this.action.target!.assets = undefined;
            this.action.target = {
                matchedAssets: {
                    types: [
                         assetType || ""
                    ]
                }
            };
        } else {
            this._selected = this._cache?.assets?.find(a => a.id === assetId);
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
            };
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

    /**
     * Fetches assets using the {@link assetProvider} from the parent component.
     * This is often linked to the OpenRemote HTTP API to request assets from using an {@link AssetQuery} object.
     * @param type - The asset type name to filter by
     * @param search - The asset name to filter by (acts as a search)
     * @param idValue - Selected asset ID to query along
     * @protected
     */
    protected async loadAssets(type: string, search?: string, idValue?: string): Promise<Asset[] | undefined> {
        let promises: Promise<Asset[] | undefined>[] = [];

        const query: AssetQuery = { limit: 100 };
        if(search) {
            query.names ??= [];
            query.names.push({ predicateType: "string", value: search });
        }
        // If the cache contains assets from the same query, don't send HTTP request again
        const isQueryCached = this._cache?.query && Util.objectsEqual(this._cache.query, query, true);
        if(!this._loading && !isQueryCached) {
            this._loading = true;

            // Use assetProvider from the parent component to retrieve assets using HTTP
            promises.push(this.assetProvider(type, {...query}));

            // When idValue is present, it should also be fetched alongside the other assets
            if(idValue && idValue !== "*") {
                promises.push(this.assetProvider(type, { ids: [idValue] }));
            }

            // Start retrieving assets through the assetProvider
            const responses = await Promise.all(promises);
            this._loading = false;

            // Only update the state when we retrieve new assets
            const assets = responses.filter(value => !!value).flat();
            const cachedIds = this._cache?.assets.map(asset => asset.id) ?? [];
            this._cache = {
                query: query,
                assets: [...(this._cache?.assets ?? []), ...(assets?.filter(a => !cachedIds.includes(a.id)) ?? [])]
            };
            return assets;
        }
        return this._cache?.assets;
    }
}
