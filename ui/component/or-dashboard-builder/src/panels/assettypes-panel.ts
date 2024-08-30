import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {Asset, AssetDescriptor, AssetModelUtil, AssetQuery, AssetTypeInfo} from "@openremote/model";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {i18next} from "@openremote/or-translate";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import manager, {Util} from "@openremote/core";
import {when} from "lit/directives/when.js";
import {createRef, Ref, ref} from 'lit/directives/ref.js';
import {AssetTreeConfig, OrAssetTree} from "@openremote/or-asset-tree";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

export class AssetTypeSelectEvent extends CustomEvent<string> {

    public static readonly NAME = "assettype-select";

    constructor(assetTypeName: string) {
        super(AssetTypeSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetTypeName
        });
    }
}

export class AssetIdsSelectEvent extends CustomEvent<string | string[]> {

    public static readonly NAME = "assetids-select"

    constructor(assetIds: string | string[]) {
        super(AssetIdsSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetIds
        });
    }
}

export class AttributeNamesSelectEvent extends CustomEvent<string | string[]> {

    public static readonly NAME = "attributenames-select";

    constructor(attributeNames: string | string[]) {
        super(AttributeNamesSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attributeNames
        });
    }
}

export interface AssetTypesFilterConfig {
    assets?: {
        enabled?: boolean,
        multi?: boolean
    },
    attributes?: {
        enabled?: boolean,
        multi?: boolean,
        valueTypes?: string[]
    }
}

const styling = css`
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`

@customElement("assettypes-panel")
export class AssettypesPanel extends LitElement {

    @property() // selected asset type
    protected assetType?: string;

    @property()
    protected config: AssetTypesFilterConfig = {
        attributes: {
            enabled: true
        }
    }

    @property() // IDs of assets; either undefined, a single entry, or multi select
    protected assetIds: undefined | string | string[];

    @property() // names of selected attributes; either undefined, a single entry, or multi select
    protected attributeNames: undefined | string | string[];

    /* ----------- */

    @state()
    protected _attributeSelectList: string[][] = [];

    @state()
    protected _loadedAssetTypes: AssetDescriptor[] = AssetModelUtil.getAssetDescriptors().filter((t) => t.descriptorType === "asset");


    static get styles() {
        return [styling];
    }

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if (changedProps.has("assetType") && this.assetType) {
            this._attributeSelectList = this.getAttributesByType(this.assetType)!;
            this.dispatchEvent(new AssetTypeSelectEvent(this.assetType));
        }
        if (changedProps.has("assetIds") && this.assetIds) {
            this.dispatchEvent(new AssetIdsSelectEvent(this.assetIds));
        }
        if (changedProps.has("attributeNames") && this.attributeNames) {
            this.dispatchEvent(new AttributeNamesSelectEvent(this.attributeNames));
        }
    }

    protected render(): TemplateResult {
        return html`
            <div style="display: flex; flex-direction: column; gap: 8px;">

                <!-- Select asset type -->
                <div>
                    ${this._loadedAssetTypes.length > 0 ? getContentWithMenuTemplate(
                            this.getAssetTypeTemplate(),
                            this.mapDescriptors(this._loadedAssetTypes, {
                                text: i18next.t("filter.assetTypeMenuNone"),
                                value: "",
                                icon: "selection-ellipse"
                            }) as ListItem[],
                            undefined,
                            (v: string[] | string) => {
                                this.assetType = v as string;
                            },
                            undefined,
                            false,
                            true,
                            true,
                            true) : html``
                    }
                </div>

                <!-- Select one or more assets -->
                ${when(this.config.assets?.enabled, () => {
                    const assetIds = (typeof this.assetIds === 'string') ? [this.assetIds] : this.assetIds;
                    return html`
                        <div>
                            <or-mwc-input .type="${InputType.BUTTON}" .label="${(this.assetIds?.length || 0) + ' ' + i18next.t('assets')}" .disabled="${!this.assetType}" fullWidth outlined comfortable style="width: 100%;"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._openAssetSelector(this.assetType!, assetIds, this.config.assets?.multi)}"
                            ></or-mwc-input>
                        </div>
                    `;
                })}

                <!-- Select one or more attributes -->
                ${when(this.config.attributes?.enabled, () => {
                    const options: [string, string][] = this._attributeSelectList.map(al => [al[0], al[1]]);
                    const searchProvider: (search?: string) => Promise<[any, string][]> = async (search) => {
                        return search ? options.filter(o => o[1].toLowerCase().includes(search.toLowerCase())) : options;
                    };
                    return html`
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t("filter.attributeLabel")}" .disabled="${!this.assetType}" style="width: 100%;"
                                          .options="${options}" .searchProvider="${searchProvider}" .multiple="${this.config.attributes?.multi}" .value="${this.attributeNames as string}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                              this.attributeNames = ev.detail.value;
                                          }}"
                            ></or-mwc-input>
                        </div>
                    `

                })}
            </div>
        `;
    }


    /* ----------- */

    protected getAssetTypeTemplate(): TemplateResult {
        if (this.assetType) {
            const descriptor: AssetDescriptor | undefined = this._loadedAssetTypes.find((at: AssetDescriptor) => at.name === this.assetType);
            if (descriptor) {
                return this.getSelectedHeader(descriptor);
            } else {
                return this.getSelectHeader();
            }
        } else {
            return this.getSelectHeader();
        }
    }

    protected getSelectHeader(): TemplateResult {
        return html`
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" readonly .label="${i18next.t("filter.assetTypeLabel")}"
                          iconTrailing="menu-down" iconColor="rgba(0, 0, 0, 0.87)" icon="selection-ellipse"
                          value="${i18next.t("filter.assetTypeNone")}">
            </or-mwc-input>
        `;
    }

    protected getSelectedHeader(descriptor: AssetDescriptor): TemplateResult {
        return html`
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" readonly .label="${i18next.t("filter.assetTypeLabel")}"
                          .iconColor="${descriptor.colour}" iconTrailing="menu-down" icon="${descriptor.icon}"
                          value="${Util.getAssetTypeLabel(descriptor)}">
            </or-mwc-input>
        `;
    }

    protected mapDescriptors(descriptors: AssetDescriptor[], withNoneValue?: ListItem): ListItem[] {
        const items: ListItem[] = descriptors.map((descriptor) => {
            return {
                styleMap: {
                    "--or-icon-fill": descriptor.colour ? "#" + descriptor.colour : "unset"
                },
                icon: descriptor.icon,
                text: Util.getAssetTypeLabel(descriptor),
                value: descriptor.name!,
                data: descriptor
            }
        }).sort(Util.sortByString((listItem) => listItem.text));

        if (withNoneValue) {
            items.splice(0, 0, withNoneValue);
        }
        return items;
    }

    protected getAttributesByType(type: string) {
        const descriptor: AssetDescriptor = (AssetModelUtil.getAssetDescriptor(type) as AssetDescriptor);
        if (descriptor) {
            const typeInfo: AssetTypeInfo = (AssetModelUtil.getAssetTypeInfo(descriptor) as AssetTypeInfo);
            if (typeInfo?.attributeDescriptors) {
                const valueTypes = this.config.attributes?.valueTypes;
                const filtered = valueTypes ? typeInfo.attributeDescriptors.filter(ad => valueTypes.indexOf(ad.type!) > -1) : typeInfo.attributeDescriptors;
                return filtered
                    .map((ad) => {
                        const label = Util.getAttributeLabel(ad, undefined, type, false);
                        return [ad.name!, label];
                    })
                    .sort(Util.sortByString((attr) => attr[1]));
            }
        }
    }

    protected _openAssetSelector(assetType: string, assetIds?: string[], multi = false) {
        const assetTreeRef: Ref<OrAssetTree> = createRef();
        const config = {
            select: {
                types: [assetType],
                multiSelect: multi
            }
        } as AssetTreeConfig
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("linkedAssets"))
            .setContent(html`
                <div style="width: 400px;">
                    <or-asset-tree ${ref(assetTreeRef)} .dataProvider="${this.assetTreeDataProvider}" expandAllNodes
                                   id="chart-asset-tree" readonly .config="${config}" .selectedIds="${assetIds}"
                                   .showSortBtn="${false}" .showFilter="${false}" .checkboxes="${multi}"
                    ></or-asset-tree>
                </div>
            `)
            .setActions([
                {
                    default: true,
                    actionName: "cancel",
                    content: "cancel",
                },
                {
                    actionName: "ok",
                    content: "ok",
                    action: () => {
                        const tree = assetTreeRef.value;
                        if(tree?.selectedIds) {
                            if(multi) {
                                this.assetIds = tree.selectedIds;
                            } else {
                                this.assetIds = tree.selectedIds[0];
                            }
                        }
                    }
                }
            ])
            .setDismissAction({
                actionName: "cancel",
            }));
    }

    protected assetTreeDataProvider = async (): Promise<Asset[]> => {
        const assetQuery: AssetQuery = {
            realm: {
                name: manager.displayRealm
            },
            select: { // Just need the basic asset info
                attributes: []
            }
        };
        // At first, just fetch all accessible assets without attribute info...
        const assets = (await manager.rest.api.AssetResource.queryAssets(assetQuery)).data;

        // After fetching, narrow down the list to assets with the same assetType.
        // Since it is a tree, we also include the parents of those assets, based on the 'asset.path' variable.
        const pathsOfAssetType = assets.filter(a => a.type === this.assetType).map(a => a.path!);
        const filteredAssetIds = [...new Set([].concat(...pathsOfAssetType as any[]))] as string[];
        return assets.filter(a => filteredAssetIds.includes(a.id!));
    }

}
