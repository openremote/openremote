import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {AssetDescriptor, AssetModelUtil, AssetTypeInfo} from "@openremote/model";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {i18next} from "@openremote/or-translate";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {Util} from "@openremote/core";

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

export class AttributeNameSelectEvent extends CustomEvent<string> {

    public static readonly NAME = "attributename-select";

    constructor(attributeName: string) {
        super(AttributeNameSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attributeName
        });
    }
}

export class ShowLabelsToggleEvent extends CustomEvent<boolean> {

    public static readonly NAME = "showlabels-toggle";

    constructor(state: boolean) {
        super(ShowLabelsToggleEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: state
        });
    }
}

export class ShowUnitsToggleEvent extends CustomEvent<boolean> {

    public static readonly NAME = "showunits-toggle";

    constructor(state: boolean) {
        super(ShowUnitsToggleEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: state
        });
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

    @property()
    protected assetType?: string;

    @property()
    protected attributeName?: string;

    @property()
    protected attributeList: string[][] = [];

    @property()
    protected showLabels!: boolean;

    @property()
    protected showUnits!: boolean;

    @property()
    protected valueTypes: string[] = [];

    @state()
    protected _loadedAssetTypes: AssetDescriptor[] = AssetModelUtil.getAssetDescriptors().filter((t) => t.descriptorType === "asset");
;

    static get styles() {
        return [styling];
    }

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if (changedProps.has("assetType") && this.assetType) {
            this.attributeList = this.getAttributesByType(this.assetType)!;
            this.dispatchEvent(new AssetTypeSelectEvent(this.assetType));
        }
        if (changedProps.has("attributeName") && this.attributeName) {
            this.dispatchEvent(new AttributeNameSelectEvent(this.attributeName));
        }
        if (changedProps.has("showLabels")) {
            this.dispatchEvent(new ShowLabelsToggleEvent(this.showLabels));
        }
        if (changedProps.has("showUnits")) {
            this.dispatchEvent(new ShowUnitsToggleEvent(this.showUnits));
        }
    }

    protected render(): TemplateResult {
        const options: [string, string][] = this.attributeList.map(al => [al[0], al[1]]);
        const searchProvider: (search?: string) => Promise<[any, string][]> = async (search) => {
            return search ? options.filter(o => o[1].toLowerCase().includes(search.toLowerCase())) : options;
        };
        return html`
            <div style="display: flex; flex-direction: column; gap: 8px;">
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
                <div>
                    <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t("filter.attributeLabel")}" .disabled="${!this.assetType}" style="width: 100%;"
                                  .options="${options}" .searchProvider="${searchProvider}" .value="${this.attributeName}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                      this.attributeName = ev.detail.value;
                                  }}"
                    ></or-mwc-input>
                </div>
                <div>
                    <div class="switchMwcInputContainer">
                        <span>${i18next.t("dashboard.showLabels")}</span>
                        <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                      .value="${this.showLabels}" .disabled="${!this.assetType}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          this.showLabels = ev.detail.value;
                                      }}"
                        ></or-mwc-input>
                    </div>
                    <div class="switchMwcInputContainer">
                        <span>${i18next.t("dashboard.showUnits")}</span>
                        <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                      .value="${this.showUnits}" .disabled="${!this.showLabels || !this.assetType}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          this.showUnits = ev.detail.value;
                                      }}"
                        ></or-mwc-input>
                    </div>

                </div>
            </div>
        `;
    }

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
                return typeInfo.attributeDescriptors
                    .filter((ad) => this.valueTypes.indexOf(ad.type!) > -1)
                    .map((ad) => {
                        const label = Util.getAttributeLabel(ad, undefined, type, false);
                        return [ad.name!, label];
                    })
                    .sort(Util.sortByString((attr) => attr[1]));
            }
        }
    }

}
