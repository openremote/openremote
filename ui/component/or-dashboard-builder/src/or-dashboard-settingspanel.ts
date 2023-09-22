import manager, {DefaultColor5, Util} from "@openremote/core";
import {
    Asset, Attribute, AssetModelUtil, AttributeRef, AssetDescriptor, AssetTypeInfo,
} from "@openremote/model";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "@openremote/or-mwc-components/or-mwc-menu";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {i18next} from "@openremote/or-translate";
import {css, html, LitElement, unsafeCSS, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {until} from "lit/directives/until.js";
import {OrWidgetConfig} from "./widgets/or-base-widget";
import {style} from "./style";
import {ifDefined} from 'lit/directives/if-defined.js';

//language=css
export const widgetSettingsStyling = css`

    /* ------------------------------- */
    .switchMwcInputContainer {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    /* ---------------------------- */
    #attribute-list {
        overflow: auto;
        flex: 1 1 0;
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }

    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        line-height: 16px;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    .attribute-list-item .button.delete {
        display: none;
    }

    .attribute-list-item:hover .button.delete {
        display: block;
    }

    /* ---------------------------- */
    .button-clear {
        background: none;
        visibility: hidden;
        color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .attribute-list-item:hover .button-clear {
        visibility: visible;
    }

    .button-clear:hover {
        --or-icon-fill: var(--or-app-color4);
    }
    
    .expanded-panel {
        padding: 6px 16px 24px;
        display: flex;
        flex-direction: column;
        gap: 16px;
    }

    /* ---------------------------- */
    
    .threshold-list-item {
        display: flex; 
        flex-direction: row; 
        align-items: center;
    }
    
    .threshold-list-item-colour {
        height: 100%; 
        padding: 0 4px 0 0;
    }

    .threshold-list-item:hover .button-clear {
        visibility: visible;
    }
`

export enum SettingsPanelType {
    SINGLE_ATTRIBUTE, MULTI_ATTRIBUTE, THRESHOLDS, ASSETTYPES,
}

@customElement('or-dashboard-settingspanel')
export class OrDashboardSettingsPanel extends LitElement {

    @property()
    private readonly type?: SettingsPanelType;

    @property()
    private readonly widgetConfig: OrWidgetConfig | any;

    private _config?: OrWidgetConfig | any; // local duplicate that does not update parent. Sent back in dispatchEvent() only.

    @property({type: Boolean}) // used for SINGLE_ATTRIBUTE and MULTI_ATTRIBUTE types.
    private readonly onlyDataAttrs: boolean = true;

    @property() // used to specify the attribute valuetype that can be used in the widget.
    public attributeFilter?: ((attribute: Attribute<any>) => boolean) | undefined;

    @state()
    private loadedAssets?: Asset[];

    static get styles() {
        return [style, widgetSettingsStyling];
    }

    @state()
    protected _loadedAssetTypes: AssetDescriptor[] = [];

    protected _allowedValueTypes: string[] = ["boolean", "number", "positiveInteger", "positiveNumber",
        "negativeInteger", "negativeNumber", "text"];


    /* ---------------------------- */

    // Setting local duplicate config before triggering update,
    // and fetching assets if any of the AttributeRefs are not pulled yet.
    willUpdate(changedProperties: Map<string, any>) {
        if (changedProperties.has("widgetConfig")) {
            if (this.widgetConfig.attributeRefs != undefined && (!this._config || (changedProperties.get("widgetConfig") != this._config))) {
                const loadedRefs: AttributeRef[] = this.widgetConfig.attributeRefs.filter((attrRef: AttributeRef) => this.isAttributeRefLoaded(attrRef));
                if (loadedRefs.length != this.widgetConfig.attributeRefs.length) {
                    this.fetchAssets(this.widgetConfig).then(assets => {
                        this.loadedAssets = assets;
                    });
                }
            }
            this._config = (this.widgetConfig ? JSON.parse(JSON.stringify(this.widgetConfig)) : undefined);
        }
    }


    /* ---------------------------------- */

    render() {
        switch (this.type) {

            case SettingsPanelType.SINGLE_ATTRIBUTE:
            case SettingsPanelType.MULTI_ATTRIBUTE: {
                if (!this._config.attributeRefs) {
                    return html`<span>${i18next.t('errorOccurred')}</span>`;
                } else {
                    return html`
                        ${until(this.getAttributeHTML(this._config, (this.type == SettingsPanelType.MULTI_ATTRIBUTE), this.onlyDataAttrs, this.attributeFilter), html`${i18next.t('loading')}`)}
                    `;
                }
            }
            case SettingsPanelType.ASSETTYPES: {
                if (!this._config.assetTypes) {
                    return html`<span>${i18next.t('errorOccurred')}</span>`;
                } else {
                    return html`
                        ${until(this.getAssettypesHTML(this._config), html`${i18next.t('loading')}`)}
                    `;
                }
            }

            case SettingsPanelType.THRESHOLDS: {
                if (!this._config.thresholds) {
                    return html`<span>${i18next.t('errorOccurred')}</span>`;
                } else {
                    return html`
                        ${until(this.getThresholdsHTML(this._config), html`${i18next.t('loading')}`)}
                    `;
                }
            }
        }
    }


    /* --------------------------------- */

    // UTILITY FUNCTIONS


    // Method to update the Grid. For example after changing a setting.
    updateParent(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    async getAttributeHTML(config: any, multi: boolean, onlyDataAttrs: boolean = true, attributeFilter?: (attribute: Attribute<any>) => boolean) {
        return html`
            <div style="padding: 0 14px 12px 14px;">
                ${(config.attributeRefs == null || config.attributeRefs.length == 0) ? html`
                    <span style="padding: 14px 0; display: block;">${i18next.t('noAttributesConnected')}</span>
                ` : undefined}
                <div id="attribute-list">
                    ${(config.attributeRefs != null && this.loadedAssets != null) ? config.attributeRefs.map((attributeRef: AttributeRef) => {
                        const asset = this.loadedAssets?.find((x: Asset) => {
                            return x.id == attributeRef.id;
                        }) as Asset;
                        if (asset) {
                            const attribute = asset.attributes![attributeRef.name!];
                            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                            const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, true);
                            return html`
                                <div class="attribute-list-item">
                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type))}</span>
                                    <div class="attribute-list-item-label">
                                        <span>${asset.name}</span>
                                        <span style="font-size:14px; color:grey;">${label}</span>
                                    </div>
                                    <button class="button-clear"
                                            @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                        <or-icon icon="close-circle"></or-icon>
                                    </button>
                                </div>
                            `;
                        } else {
                            return undefined;
                        }
                    }) : undefined}
                </div>
                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('attribute')}" icon="${(multi || config.attributeRefs.length == 0) ? "plus" : "swap-horizontal"}"
                              style="margin-top: 8px;"
                              @or-mwc-input-changed="${() => this.openDialog(config.attributeRefs, multi, onlyDataAttrs, attributeFilter)}">
                </or-mwc-input>
            </div>
        `
    }

    isAttributeRefLoaded(attrRef: AttributeRef) {
        return this.loadedAssets?.find((asset) => asset.id == attrRef.id)
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if (config.attributeRefs) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[],
                select: {
                    attributes: config.attributeRefs?.map((x: AttributeRef) => x.name) as string[]
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }

    setWidgetAttributes(selectedAttrs?: AttributeRef[]) {
        if (this._config != null) {
            this._config.attributeRefs = selectedAttrs;
            this.fetchAssets(this._config).then((assets) => {
                this.loadedAssets = assets;
                this.updateComplete.then(() => {
                    this.updateParent(new Map<string, any>([["config", this._config], ["loadedAssets", assets]]));
                });
            });
        }
    }

    removeWidgetAttribute(attributeRef: AttributeRef) {
        if (this._config.attributeRefs != null) {
            this._config.attributeRefs.splice(this._config.attributeRefs.indexOf(attributeRef), 1);
            this.updateParent(new Map<string, any>([["config", this._config]]));
        }
    }

    // Opening the attribute picker dialog, and listening to its result. (UI related)
    openDialog(attributeRefs: AttributeRef[], multi: boolean, onlyDataAttrs: boolean = true, attributeFilter?: (attribute: Attribute<any>) => boolean) {
        let dialog: OrAttributePicker;
        if (attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setSelectedAttributes(attributeRefs).setShowOnlyDatapointAttrs(onlyDataAttrs).setAttributeFilter(attributeFilter));
        } else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setShowOnlyDatapointAttrs(onlyDataAttrs))
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.setWidgetAttributes(event.detail);
        })
    }


    /* ---------------------------------------------------- */

    async getThresholdsHTML(config: OrWidgetConfig | any) {
        if (config.thresholds && config.valueType) {
            return html`
                <div id="thresholds-list" class="expanded-panel">
                    ${(config.valueType === 'number' || config.valueType === 'positiveInteger'
                            || config.valueType === 'positiveNumber' || config.valueType === 'negativeInteger'
                            || config.valueType === 'negativeNumber') ? html`
                        ${(config.thresholds as [number, string][]).sort((x, y) => (x[0] < y[0]) ? -1 : 1).map((threshold, index) => {
                            return html`
                                <div class="threshold-list-item">
                                    <div class="threshold-list-item-colour">
                                        <or-mwc-input type="${InputType.COLOUR}" value="${threshold[1]}"
                                                      @or-mwc-input-changed="${(event: CustomEvent) => {
                                                          this._config.thresholds[index][1] = event.detail.value;
                                                          this.updateParent(new Map<string, any>([["config", this._config]]));
                                                      }}"
                                        ></or-mwc-input>
                                    </div>
                                    <or-mwc-input type="${InputType.NUMBER}" comfortable .value="${threshold[0]}"
                                                  ?disabled="${index === 0 && config.max}"
                                                  .min="${ifDefined(config.min)}" .max="${ifDefined(config.max)}"
                                                  @or-mwc-input-changed="${(event: CustomEvent) => {
                                                      if ((!config.min || event.detail.value >= config.min) && (!config.max || event.detail.value <= config.max)) {
                                                          this._config.thresholds[index][0] = event.detail.value;
                                                          this.updateParent(new Map<string, any>([["config", this._config]]));
                                                      }
                                                  }}"
                                    ></or-mwc-input>
                                    ${index == 0 ? html`
                                        <button class="button-clear"
                                                style="margin-left: 8px;">
                                            <or-icon icon="lock" style="--or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});"></or-icon>
                                        </button>
                                    ` : html`
                                        <button class="button-clear"
                                                style="margin-left: 8px;"
                                                @click="${() => {
                                                    this.removeThreshold(this._config, threshold);
                                                }}">
                                            <or-icon icon="close-circle"></or-icon>
                                        </button>
                                    `}
                                </div>
                            `
                        })}
                        <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('threshold')}" icon="plus"
                                      @or-mwc-input-changed="${() => this.addNewThreshold(this._config)}">
                        </or-mwc-input>
                    ` : null}
                    ${(config.valueType === 'boolean') ? html`
                        <div class="threshold-list-item">
                            <div class="threshold-list-item-colour">
                                <or-mwc-input type="${InputType.COLOUR}" value="${config.boolColors.true}"
                                              @or-mwc-input-changed="${(event: CustomEvent) => {
                                                  this._config.boolColors.true = event.detail.value;
                                                  this.updateParent(new Map<string, any>([["config", this._config]]));
                                              }}"
                                ></or-mwc-input>
                            </div>
                            <or-mwc-input type="${InputType.TEXT}" comfortable .value="${'True'}" .readonly="${true}"
                            ></or-mwc-input>
                        </div>
                        <div class="threshold-list-item">
                            <div class="threshold-list-item-colour">
                                <or-mwc-input type="${InputType.COLOUR}" value="${config.boolColors.false}"
                                              @or-mwc-input-changed="${(event: CustomEvent) => {
                                                  this._config.boolColors.false = event.detail.value;
                                                  this.updateParent(new Map<string, any>([["config", this._config]]));
                                              }}"
                                ></or-mwc-input>
                            </div>
                            <or-mwc-input type="${InputType.TEXT}" comfortable .value="${'False'}" .readonly="${true}"
                            ></or-mwc-input>
                        </div>
                    ` : null}
                    ${(config.valueType === 'text' && config.textColors) ? html`
                        ${(config.textColors as [string, string][]).map((threshold, index) => {
                            return html`
                                <div class="threshold-list-item">
                                    <div class="threshold-list-item-colour">
                                        <or-mwc-input type="${InputType.COLOUR}" value="${threshold[1]}"
                                                      @or-mwc-input-changed="${(event: CustomEvent) => {
                                                          this._config.textColors[index][1] = event.detail.value;
                                                          this.updateParent(new Map<string, any>([["config", this._config]]));
                                                      }}"
                                        ></or-mwc-input>
                                    </div>
                                    <or-mwc-input type="${InputType.TEXT}" comfortable .value="${threshold[0]}"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                      this._config.textColors[index][0] = event.detail.value;
                                                      this.updateParent(new Map<string, any>([["config", this._config]]));
                                                  }}"

                                    ></or-mwc-input>
                                    ${index == 0 ? html`
                                        <button class="button-clear"
                                                style="margin-left: 8px;">
                                            <or-icon icon="lock" style="--or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});"></or-icon>
                                        </button>
                                    ` : html`
                                        <button class="button-clear"
                                                style="margin-left: 8px;"
                                                @click="${() => {
                                                    this.removeThreshold(this._config, threshold);
                                                }}">
                                            <or-icon icon="close-circle"></or-icon>
                                        </button>
                                    `}
                                </div>
                            `
                        })}
                        <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('threshold')}" icon="plus"
                                      @or-mwc-input-changed="${() => this.addNewThreshold(this._config)}">
                        </or-mwc-input>                                                                                                                        </div>
                    ` : null}
                </div> `

        }
    }

    removeThreshold(config: any, threshold: [any, string]) {
        switch (typeof threshold[0]) {
            case "number":
                config.thresholds = (config.thresholds as [number, string][]).filter((x) => x !== threshold);
                break;
            case "string":
                config.textColors = (config.textColors as [string, string][]).filter((x) => x !== threshold);
                break;
        }
        this.updateParent(new Map<string, any>([["config", this._config]]));
    }

    addThreshold(config: any, threshold: [any, string]) {
        switch (typeof threshold[0]) {
            case "number":
                (config.thresholds as [number, string][]).push(threshold as [number, string]);
                break;
            case "string":
                (config.textColors as [string, string][]).push(threshold as [string, string]);
                break;
        }
        this.updateParent(new Map<string, any>([["config", this._config]]));
    }

    addNewThreshold(config: any) {
        if (config.valueType === 'text') {
            this.addThreshold(config, ["new", "#000000"]);
        } else {
            const suggestedValue = (config.thresholds[config.thresholds.length - 1][0] + 10);
            this.addThreshold(config, [(!config.max || suggestedValue <= config.max ? suggestedValue : config.max), "#000000"]);
        }
        this.updateComplete.then(() => {
            const elem = this.shadowRoot?.getElementById('thresholds-list') as HTMLElement;
            const inputField = Array.from(elem.children)[elem.children.length - 2] as HTMLElement;
            (inputField.children[1] as HTMLElement).setAttribute('focused', 'true');
        })
    }

    /* ---------------------------------------------------- */

    async getAssetTypes() {
        return AssetModelUtil.getAssetDescriptors().filter((t) => t.descriptorType == "asset");
    }

    getAttributesByType(type: string) {
        const descriptor: AssetDescriptor = (AssetModelUtil.getAssetDescriptor(type) as AssetDescriptor);
        if(descriptor) {
            const typeInfo: AssetTypeInfo = (AssetModelUtil.getAssetTypeInfo(descriptor) as AssetTypeInfo);
            if (typeInfo?.attributeDescriptors) {
                return typeInfo.attributeDescriptors.filter((ad) => {
                    return this._allowedValueTypes.indexOf(ad.type!) > -1;
                }).map((ad) => {
                    const label = Util.getAttributeLabel(ad, undefined, type, false);
                    return [ad.name!, label];
                }).sort(Util.sortByString((attr) => attr[1]));
            }
        }
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

    protected getSelectHeader(): TemplateResult {
        return html`
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" .label="${i18next.t("filter.assetTypeLabel")}"
                          iconTrailing="menu-down" iconColor="rgba(0, 0, 0, 0.87)" icon="selection-ellipse"
                          value="${i18next.t("filter.assetTypeNone")}"></or-mwc-input>`;
    }

    protected getSelectedHeader(descriptor: AssetDescriptor): TemplateResult {
        return html`
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" .label="${i18next.t("filter.assetTypeLabel")}"
                          .iconColor="${descriptor.colour}" iconTrailing="menu-down" icon="${descriptor.icon}"
                          value="${Util.getAssetTypeLabel(descriptor)}"></or-mwc-input>`;
    }

    protected assetTypeSelect(): TemplateResult {
        if (this._config.assetType) {
            const descriptor: AssetDescriptor | undefined = this._loadedAssetTypes.find((at: AssetDescriptor) => {
                return at.name === this._config.assetType
            });
            if (descriptor) {
                return this.getSelectedHeader(descriptor);
            } else {
                return this.getSelectHeader();
            }
        } else {
            return this.getSelectHeader();
        }
    }

    protected handleTypeSelect(value: string) {
        if (this._config.assetType !== value) {
            this._config.attributeName = undefined;
            this._config.assetIds = [];
            this._config.showLabels = false;
            this._config.showUnits = false;
            this._config.boolColors = {type: 'boolean', 'false': '#ef5350', 'true': '#4caf50'};
            this._config.textColors = [['example', "#4caf50"], ['example2', "#ff9800"]];
            this._config.thresholds = [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]];
            this._config.assetType = value;
            this._config.attributes = this.getAttributesByType(value);
            this.updateParent(new Map<string, any>([["config", this._config]]));
        }
    }

    protected async handleAttributeSelect(value: string) {
        this._config.attributeName = value;
        await manager.rest.api.AssetResource.queryAssets({
            realm: {
                name: manager.displayRealm
            },
            select: {
                attributes: [value, 'location']
            },
            types: [this._config.assetType],
        }).then(response => {
            this._config.assetIds = response.data.map((a) => a.id);
            this._config.valueType = (response.data.length > 0) ? response.data[0].attributes![value].type : "text"; // sometimes no asset exists of that assetType, so using 'text' as fallback.
        }).catch((reason) => {
            console.error(reason);
            showSnackbar(undefined, i18next.t('errorOccurred'));
        });

        this.updateParent(new Map<string, any>([["config", this._config]]));
    }

    async getAssettypesHTML(config: OrWidgetConfig | any) {
        if (this._loadedAssetTypes.length === 0) {
            this._loadedAssetTypes = await this.getAssetTypes() as AssetDescriptor[]
        }
        return html`
            <div class="expanded-panel">
                ${this._loadedAssetTypes.length > 0 ? getContentWithMenuTemplate(
                        this.assetTypeSelect(),
                        this.mapDescriptors(this._loadedAssetTypes, {
                            text: i18next.t("filter.assetTypeMenuNone"),
                            value: "",
                            icon: "selection-ellipse"
                        }) as ListItem[],
                        undefined,
                        (v: string[] | string) => {
                            this.handleTypeSelect(v as string);
                        },
                        undefined,
                        false,
                        true,
                        true,
                        true) : html``
                }
                <div>
                    <or-mwc-input .type="${InputType.SELECT}" .disabled="${config.assetType == undefined || config.assetType == ''}" style="width: 100%;"
                                  .options="${this._config.attributes}"
                                  .value="${config.attributeName}" label="${i18next.t('filter.attributeLabel')}"
                                  @or-mwc-input-changed="${(event: CustomEvent) => {
                                      this.handleAttributeSelect(event.detail.value);
                                  }}"
                    ></or-mwc-input>
                </div>
                <div>
                    <div class="switchMwcInputContainer">
                        <span>${i18next.t('dashboard.showLabels')}</span>
                        <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                      .value="${config.showLabels}" .disabled="${config.assetType == undefined || config.assetType == ''}"
                                      @or-mwc-input-changed="${(event: CustomEvent) => {
                                          this._config.showLabels = event.detail.value;
                                          this.updateParent(new Map<string, any>([["config", this._config]]));
                                      }}"
                        ></or-mwc-input>
                    </div>
                    <div class="switchMwcInputContainer">
                        <span>${i18next.t('dashboard.showUnits')}</span>
                        <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                      .value="${config.showUnits}" .disabled="${!config.showLabels || config.assetType == undefined || config.assetType == ''}"
                                      @or-mwc-input-changed="${(event: CustomEvent) => {
                                          this._config.showUnits = event.detail.value;
                                          this.updateParent(new Map<string, any>([["config", this._config]]));
                                      }}"
                        ></or-mwc-input>
                    </div>

                </div>
            </div>
        `
    }
}
