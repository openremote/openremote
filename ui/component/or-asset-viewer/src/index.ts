// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

import {html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-icon";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-attribute-input";
import "@openremote/or-attribute-history";
import "@openremote/or-chart";
import "@openremote/or-mwc-components/or-mwc-table";
import "@openremote/or-components/or-panel";
import "@openremote/or-mwc-components/or-mwc-dialog";
import {DialogAction, OrMwcDialog, showDialog, showOkCancelDialog, showOkDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import {translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import manager, {subscribe, Util, DefaultColor5} from "@openremote/core";
import {OrMwcTable, OrMwcTableRowClickEvent} from "@openremote/or-mwc-components/or-mwc-table";
import {OrChartConfig} from "@openremote/or-chart";
import {HistoryConfig, OrAttributeHistory} from "@openremote/or-attribute-history";
import {
    AgentDescriptor,
    Asset,
    AssetEvent,
    AssetModelUtil,
    Attribute,
    AttributeEvent,
    ClientRole,
    FileInfo,
    SharedEvent,
    UserAssetLink,
    WellknownAssets,
    WellknownAttributes,
    WellknownMetaItems,
} from "@openremote/model";
import {panelStyles, style} from "./style";
import i18next, {InitOptions, TOptions} from "i18next";
import {styleMap} from "lit/directives/style-map.js";
import {classMap} from "lit/directives/class-map.js";
import {GenericAxiosResponse} from "axios";
import "./or-edit-asset-panel";
import {OrEditAssetModifiedEvent, OrEditAssetPanel, ValidatorResult} from "./or-edit-asset-panel";
import "@openremote/or-mwc-components/or-mwc-snackbar";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import { progressCircular } from "@openremote/or-mwc-components/style";
import { OrAssetTree } from "@openremote/or-asset-tree";

export interface PanelConfig {
    type: "info" | "setup" | "history" | "group" | "survey" | "survey-results" | "linkedUsers";
    title?: string;
    hide?: boolean;
    column?: number;
    hideOnMobile?: boolean;
    panelStyles?: { [style: string]: string };
}

export interface InfoPanelItemConfig {
    label?: string;
    hideOnMobile?: boolean;
    readonly?: boolean;
    disabled?: boolean;
    disableButton?: boolean;
    disableHelperText?: boolean;
    inputTypeOverride?: InputType;
    fullWidth?: boolean;
    priority?: number;
    styles?: { [style: string]: string };
}

export interface InfoPanelConfig extends PanelConfig {
    type: "info",
    attributes: {
        include?: string[];
        exclude?: string[];
        itemConfig?: {
            [name: string]: InfoPanelItemConfig;
        };
    },
    properties: {
        include?: string[];
        exclude?: string[];
        itemConfig?: {
            [name: string]: InfoPanelItemConfig;
        };
    }
}

export interface SetupPanelConfig extends PanelConfig {
    type: "setup"
}

export interface HistoryPanelConfig extends PanelConfig {
    type: "history",
    include?: string[];
    exclude?: string[];
}

export interface GroupPanelConfig extends PanelConfig {
    type: "group",
    childAssetTypes?: {
        [assetType: string]: {
            availableAttributes?: string[];
            selectedAttributes?: string[];
        }
    };
}

export interface AssetViewerConfig {
    panels?: PanelConfigUnion[];
    viewerStyles?: { [style: string]: string };
    propertyViewProvider?: PropertyViewProvider;
    attributeViewProvider?: AttributeViewProvider;
    panelViewProvider?: PanelViewProvider;
    historyConfig?: HistoryConfig;
    chartConfig?: OrChartConfig;
}

export type PanelConfigUnion = InfoPanelConfig | SetupPanelConfig | GroupPanelConfig | HistoryPanelConfig | PanelConfig;

export interface ViewerConfig {
    default?: AssetViewerConfig;
    assetTypes?: { [assetType: string]: AssetViewerConfig };
    historyConfig?: HistoryConfig;
}

interface UserAssetLinkInfo {
    userId: string;
    usernameAndId: string;
    roles: string[];
    restrictedUser: boolean;
}

interface AssetInfo {
    asset: Asset;
    userAssetLinks?: UserAssetLinkInfo[];
    childAssets?: Asset[];
    viewerConfig: AssetViewerConfig;
    modified: boolean;
    attributeTemplateMap: {[attrName: string]: TemplateResult};
}

export type PanelViewProvider = (asset: Asset, attributes: { [index: string]: Attribute<any> }, panelName: string, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfigUnion) => TemplateResult | undefined;
export type PropertyViewProvider = (asset: Asset, property: string, value: any, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfigUnion) => TemplateResult | undefined;
export type AttributeViewProvider = (asset: Asset, attribute: Attribute<any>, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfigUnion) => TemplateResult | undefined;

export function getIncludedProperties(config?: InfoPanelConfig): string[] {
    const includedProperties = config && config.properties && config.properties.include ? config.properties.include : DEFAULT_ASSET_PROPERTIES;
    const excludedProperties =  config && config.properties && config.properties.exclude ? config.properties.exclude : [];

    return includedProperties.filter((prop) => !excludedProperties || excludedProperties.indexOf(prop) < 0);
}

export function getIncludedAttributes(attributes: { [index: string]: Attribute<any> }, config?: InfoPanelConfig): Attribute<any>[] {
    const includedAttributes = config && config.attributes && config.attributes.include ? config.attributes.include : undefined;
    const excludedAttributes = config && config.attributes && config.attributes.exclude ? config.attributes.exclude : undefined;
    if (includedAttributes || excludedAttributes) {
        return Object.values(attributes).filter((attr) =>
            (!includedAttributes || includedAttributes.some((inc) => Util.stringMatch(inc, attr.name!)))
            && (!excludedAttributes || !excludedAttributes.some((exc) => Util.stringMatch(exc, attr.name!))));
    }
    return Object.values(attributes);
}

export class OrAssetViewerComputeGridEvent extends CustomEvent<void> {

    public static readonly NAME = "or-asset-viewer-compute-grid-event";

    constructor() {
        super(OrAssetViewerComputeGridEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export type SaveResult = {
    success: boolean,
    assetId: string,
    isNew?: boolean,
    isCopy?: boolean
};

export class OrAssetViewerRequestSaveEvent extends CustomEvent<Util.RequestEventDetail<Asset>> {

    public static readonly NAME = "or-asset-viewer-request-save";

    constructor(asset: Asset) {
        super(OrAssetViewerRequestSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: asset
            }
        });
    }
}

export class OrAssetViewerSaveEvent extends CustomEvent<SaveResult> {

    public static readonly NAME = "or-asset-viewer-save";

    constructor(saveResult: SaveResult) {
        super(OrAssetViewerSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: saveResult
        });
    }
}

export class OrAssetViewerChangeParentEvent extends CustomEvent<{ parentId: string | undefined, assetsIds: string[] }> {

    public static readonly NAME = "or-asset-viewer-change-parent";

    constructor(parent: string | undefined, assetsIds: string[]) {
        super(OrAssetViewerChangeParentEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                parentId: parent,
                assetsIds: assetsIds
            }
        });
    }
}

export class OrAssetViewerRequestEditToggleEvent extends CustomEvent<Util.RequestEventDetail<boolean>> {

    public static readonly NAME = "or-asset-viewer-request-edit-toggle";

    constructor(edit: boolean) {
        super(OrAssetViewerRequestEditToggleEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: edit
            }
        });
    }
}

export class OrAssetViewerEditToggleEvent extends CustomEvent<boolean> {

    public static readonly NAME = "or-asset-viewer-edit-toggle";

    constructor(edit: boolean) {
        super(OrAssetViewerEditToggleEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: edit
        });
    }
}

export class OrAssetViewerLoadUserEvent extends CustomEvent<string> {

    public static readonly NAME = "or-asset-viewer-load-user-event";

    constructor(userId: string) {
        super(OrAssetViewerLoadUserEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: userId
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAssetViewerComputeGridEvent.NAME]: OrAssetViewerComputeGridEvent;
        [OrAssetViewerRequestSaveEvent.NAME]: OrAssetViewerRequestSaveEvent;
        [OrAssetViewerSaveEvent.NAME]: OrAssetViewerSaveEvent;
        [OrAssetViewerRequestEditToggleEvent.NAME]: OrAssetViewerRequestEditToggleEvent;
        [OrAssetViewerEditToggleEvent.NAME]: OrAssetViewerEditToggleEvent;
        [OrAssetViewerChangeParentEvent.NAME]: OrAssetViewerChangeParentEvent;
        [OrAssetViewerLoadUserEvent.NAME]: OrAssetViewerLoadUserEvent;
    }
}

export function getPanel(id: string, panelConfig: PanelConfig, content: TemplateResult | undefined) {

    if (!content) {
        return;
    }

    return html`
        <div class=${classMap({panel: true, mobileHidden: panelConfig.hideOnMobile === true})} style="${panelConfig && panelConfig.panelStyles ? styleMap(panelConfig.panelStyles) : ""}" id="${id}-panel">
            <div class="panel-content-wrapper">
                <div class="panel-title">
                    <or-translate value="${panelConfig.title || panelConfig.type}"></or-translate>
                </div>
                <div class="panel-content">
                    ${content}
                </div>
            </div>
        </div>
    `;
}

function getPanelContent(id: string, assetInfo: AssetInfo, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfigUnion): TemplateResult | undefined {

    const asset = assetInfo.asset;

    // See if config has a custom way for rendering this panel
    if (viewerConfig.panelViewProvider) {
        const template = viewerConfig.panelViewProvider(asset, asset.attributes!, id, hostElement, viewerConfig, panelConfig);
        if (template) {
            return template;
        }
    }

    if (!panelConfig) return undefined;

    if (panelConfig.type === "info") {

        // This type of panel shows attributes and/or properties of the asset
        const infoConfig = panelConfig as InfoPanelConfig;
        const includedProperties = getIncludedProperties(infoConfig);
        const includedAttributes = getIncludedAttributes(asset.attributes!, infoConfig);

        if (includedProperties.length === 0 && includedAttributes.length === 0) {
            return undefined;
        }

        const items: {item: string | Attribute<any>, itemConfig: InfoPanelItemConfig}[] = [];

        includedProperties.forEach((prop) => {
            const itemConfig = infoConfig.properties && infoConfig.properties.itemConfig ? infoConfig.properties.itemConfig[prop] : {};
            if (itemConfig.label === undefined) {
                itemConfig.label = i18next.t(prop);
            }
            itemConfig.priority = itemConfig.priority || 0;
            items.push({
                item: prop,
                itemConfig: itemConfig
            });
        });

        includedAttributes.forEach((attribute) => {
            const itemConfig = infoConfig.attributes && infoConfig.attributes.itemConfig && infoConfig.attributes.itemConfig[attribute.name!] ? infoConfig.attributes.itemConfig[attribute.name!] : {};
            if (itemConfig.label === undefined) {
                // Get label here so we can sort the attributes
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type!, attribute.name, attribute);
                itemConfig.label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, true);
            }
            itemConfig.priority = itemConfig.priority || 0;
            items.push({
                item: attribute,
                itemConfig: itemConfig
            });
        });

        const labelSort = Util.sortByString((item: {item: string | Attribute<any>, itemConfig: InfoPanelItemConfig}) => item.itemConfig.label!.toUpperCase());

        items.sort((a, b) => {
            const priorityA = a.itemConfig.priority!;
            const priorityB = b.itemConfig.priority!;

            if (priorityA < priorityB) {
                return 1;
            }

            if (priorityA > priorityB) {
                return -1;
            }

            return labelSort(a,b);
        });

        return html`
            ${items.map((item) => {
                if (typeof item.item === "string") {
                    // This is a property                    
                    return getField(item.item, item.itemConfig, getPropertyTemplate(asset, item.item, hostElement, viewerConfig, panelConfig, item.itemConfig));
                } else {
                    // This is an attribute look for a cached template
                    if (assetInfo.attributeTemplateMap[item.item.name!]) {
                        return getField(item.item.name!, item.itemConfig, assetInfo.attributeTemplateMap[item.item.name!]);
                    }
                    
                    const template = getAttributeTemplate(asset, item.item, hostElement, viewerConfig, panelConfig, item.itemConfig);
                    assetInfo.attributeTemplateMap[item.item.name!] = template;
                    
                    return getField(item.item.name!, item.itemConfig, template);
                }
        })}`;
    }

    if (panelConfig.type === "setup") {

        const descriptor = AssetModelUtil.getAssetDescriptor(asset.type) as AgentDescriptor;

        if (!descriptor || !asset.id || descriptor.descriptorType !== "agent" || (!descriptor.assetDiscovery && !descriptor.assetImport)) {
            return;
        }

        const updateFileName = () => {
            const fileInputElem = hostElement.shadowRoot!.getElementById('fileupload-elem') as HTMLInputElement;
            const fileNameElem = hostElement.shadowRoot!.getElementById('filename-elem') as HTMLInputElement;
            const fileUploadBtn: OrMwcInput = hostElement.shadowRoot!.getElementById("fileupload-btn") as OrMwcInput;
            const str = fileInputElem.value;

            if (!str) {
                return;
            }

            fileUploadBtn.disabled = false;

            let i;
            if (str.lastIndexOf('\\')) {
                i = str.lastIndexOf('\\') + 1;
            } else if (str.lastIndexOf('/')) {
                i = str.lastIndexOf('/') + 1;
            }
            fileNameElem.value = str.slice(i, str.length);
        }

        const discoverAssets = () => {
            const discoverBtn: OrMwcInput = hostElement.shadowRoot!.getElementById("discover-btn") as OrMwcInput,
                cancelBtn: OrMwcInput = hostElement.shadowRoot!.getElementById("cancel-discover-btn") as OrMwcInput;

            if (!discoverBtn || !cancelBtn) {
                return false;
            }

            cancelBtn.hidden = false;
            discoverBtn.disabled = true;
            discoverBtn.label = i18next.t("discovering") + '...';

            manager.rest.api.AgentResource.doProtocolAssetDiscovery(asset.id!)
                .then(response => {
                    if (response.status !== 200) {
                        showSnackbar(undefined, "somethingWentWrong", "dismiss");
                    } else {
                        showSnackbar(undefined, "Import successful! Added "+response.data.length+" assets!", "dismiss");
                        console.info(response.data, response) //todo: do something with this response
                    }
                })
                .catch((err) => {
                    showSnackbar(undefined, "somethingWentWrong", "dismiss");
                    console.error(err);
                })
                .finally(() => {
                    cancelBtn.hidden = true;
                    discoverBtn.disabled = false;
                    discoverBtn.label = i18next.t("discoverAssets");
                });
        }

        const cancelDiscovery = () => {
            const discoverBtn: OrMwcInput = hostElement.shadowRoot!.getElementById("discover-btn") as OrMwcInput,
                cancelBtn: OrMwcInput = hostElement.shadowRoot!.getElementById("cancel-discover-btn") as OrMwcInput;

            discoverBtn.disabled = false;
            discoverBtn.label = i18next.t("discoverAssets");
            cancelBtn.hidden = true;

            // TODO: cancel the request to the manager
        }

        const doImport = () => {
            const fileNameElem = hostElement.shadowRoot!.getElementById('filename-elem') as HTMLInputElement;
            const fileUploadBtn = hostElement.shadowRoot!.getElementById("fileupload-btn") as OrMwcInput;
            const progressElement = hostElement.shadowRoot!.getElementById("progress-circular") as HTMLProgressElement;

            if (!fileUploadBtn || !progressElement) {
                return false;
            }

            fileUploadBtn.disabled = true;
            fileUploadBtn.classList.add("hidden");
            progressElement.classList.remove("hidden");

            const fileInputElem = hostElement.shadowRoot!.getElementById('fileupload-elem') as HTMLInputElement;
            if (fileInputElem) {
                const reader = new FileReader();
                if (fileInputElem.files && fileInputElem.files.length) {
                    reader.readAsDataURL(fileInputElem.files[0]); //convert to base64
                }

                reader.onload = () => {
                    if (!reader.result) {
                        showSnackbar(undefined, "somethingWentWrong", "dismiss");
                        console.error(reader);
                    } else {
                        let encoded = reader.result.toString().replace(/^data:(.*,)?/, '');
                        if ((encoded.length % 4) > 0) {
                            encoded += '='.repeat(4 - (encoded.length % 4));
                        }
                        const fileInfo = {
                            name: 'filename',
                            contents: encoded,
                            binary: true
                        } as FileInfo

                        manager.rest.api.AgentResource.doProtocolAssetImport(asset.id!, fileInfo, undefined, {timeout: 30000})
                            .then(response => {
                                if (response.status !== 200) {
                                    showSnackbar(undefined, "somethingWentWrong", "dismiss");
                                } else {
                                    showSnackbar(undefined, "Import successful! Added "+response.data.length+" assets!", "dismiss");
                                    console.info(response.data, response)
                                }
                            })
                            .catch((err) => {
                                showSnackbar(undefined, "somethingWentWrong", "dismiss");
                                console.error(err);
                            })
                            .finally(() => {
                                fileNameElem.value = "";
                                fileUploadBtn.disabled = true;
                                fileUploadBtn.classList.remove("hidden");
                                progressElement.classList.add("hidden");
                            });

                    }
                }
            }
        }

        let content: TemplateResult = html``;

        if (descriptor.assetImport) {
            content = html`
                <div id="fileupload">
                    <or-mwc-input style="flex: 0 1 auto;" outlined label="selectFile" .type="${InputType.BUTTON}" @or-mwc-input-changed="${() => hostElement.shadowRoot!.getElementById('fileupload-elem')!.click()}">
                        <input id="fileupload-elem" name="configfile" type="file" accept=".*" @change="${() => updateFileName()}"/>
                    </or-mwc-input>
                    <or-mwc-input style="flex: 1 1 auto; margin: 0 4px 0 10px;" id="filename-elem" .label="${i18next.t("file")}" .type="${InputType.TEXT}" disabled></or-mwc-input>
                    <or-mwc-input style="flex: 0 1 auto;" id="fileupload-btn" icon="upload" .type="${InputType.BUTTON}" @or-mwc-input-changed="${() => doImport()}" disabled></or-mwc-input>
                    <progress id="progress-circular" class="hidden pure-material-progress-circular"></progress>
                </div>
            `;
        }
        else if (descriptor.assetDiscovery) {
            content = html`
                <or-mwc-input outlined id="discover-btn" .type="${InputType.BUTTON}" label="discoverAssets" @or-mwc-input-changed="${() => discoverAssets()}"></or-mwc-input>
                <or-mwc-input id="cancel-discover-btn" .type="${InputType.BUTTON}" label="cancel" @or-mwc-input-changed="${() => cancelDiscovery()}" hidden style="margin-left:20px"></or-mwc-input>
            `;
        } else {
            showSnackbar(undefined, "agent type doesn't support a known protocol to add assets", "dismiss");
        }

        return html`
            <style>
                [hidden] {
                    display: none;
                }
            </style>
            ${content}
        `;

    }

    // Special handling for history panel which shows an attribute selector and a graph/data table of historical values
    if (panelConfig.type === "history") {

        const historyConfig = panelConfig as HistoryPanelConfig;
        const includedAttributes = historyConfig.include ? historyConfig.include : undefined;
        const excludedAttributes = historyConfig.exclude ? historyConfig.exclude : [];
        const historyAttrs = Object.values(assetInfo?.asset?.attributes!).filter((attr) =>
            (!includedAttributes || includedAttributes.indexOf(attr.name!) >= 0)
            && (!excludedAttributes || excludedAttributes.indexOf(attr.name!) < 0)
            && (attr.meta && (attr.meta.hasOwnProperty(WellknownMetaItems.STOREDATAPOINTS) ? attr.meta[WellknownMetaItems.STOREDATAPOINTS] : attr.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK))));

        if (historyAttrs.length === 0) {
            return undefined;
        }

        let selectedAttribute: Attribute<any> | undefined;

        const attributeChanged = (attributeName: string) => {
            if (hostElement.shadowRoot) {
                const attributeHistory = hostElement.shadowRoot.getElementById("attribute-history") as OrAttributeHistory;
                if (attributeName && attributeHistory) {
                    let attribute = asset.attributes && asset.attributes![attributeName];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute!.name, attribute);
                    const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, true);
                    attributeHistory.attribute = attribute;
                    selectedAttribute = attribute!;
                }
            }
        };

        const options = historyAttrs.map((attr) => {
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
            const label = Util.getAttributeLabel(attr, descriptors[0], asset.type, true);
            return [attr.name, label];
            }).sort(Util.sortByString((item) => item[1] === undefined ? item[0]! : item[1]));

        let attrTemplate = html`
                <div id="attribute-picker">
                    <or-mwc-input .checkAssetWrite="${false}" .label="${i18next.t("attribute")}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => attributeChanged(evt.detail.value)}" .type="${InputType.SELECT}" .options="${options}"></or-mwc-input>
                </div>`;

        return html`
            <style>
               #attribute-picker {
                   flex: 0;
                   margin: 0 0 10px 0;
                   position: unset;
               }
               
               #attribute-picker > or-mwc-input {
                   width: 250px;
               }
                
                or-attribute-history {
                    width: 100%;
                    --or-attribute-history-controls-margin: 0 0 10px -5px;
                    --or-attribute-history-controls-justify-content: flex-start;
                }

               @media screen and (min-width: 1900px) {
                   #attribute-picker {
                       position: absolute;
                   }

                   or-attribute-history {
                       --or-attribute-history-controls-margin: 0 0 10px 0;
                       --or-attribute-history-controls-justify-content: flex-end;
                       min-height: 70px;
                   }
               }
            </style>
            ${attrTemplate}
            <or-attribute-history id="attribute-history" .config="${viewerConfig.historyConfig}" .assetType="${asset.type}" .assetId="${asset.id}"></or-attribute-history>
        `;
    }

    if (panelConfig.type === "group") {

        if (!asset.id || asset.type !== "GroupAsset") {
            return;
        }

        const childAssets = assetInfo.childAssets;

        if (!childAssets || childAssets.length === 0) {
            return;
        }

        // Get child asset type attribute value
        const childAssetTypeAttribute = asset.attributes && asset.attributes[WellknownAttributes.CHILDASSETTYPE];
        const groupConfig = panelConfig as GroupPanelConfig;

        if (!childAssetTypeAttribute || typeof childAssetTypeAttribute.value !== "string") {
            return;
        }

        const childAssetType = childAssetTypeAttribute.value as string;

        // Determine available and selected attributes for the child asset type
        let availableAttributes: string[] = [];
        let selectedAttributes: string[] = [];


        if (groupConfig.childAssetTypes && groupConfig.childAssetTypes[childAssetType]) {
            availableAttributes = groupConfig.childAssetTypes[childAssetType].availableAttributes ? groupConfig.childAssetTypes[childAssetType].availableAttributes! : [];
            selectedAttributes = groupConfig.childAssetTypes[childAssetType].selectedAttributes ? groupConfig.childAssetTypes[childAssetType].selectedAttributes! : [];
        }

        const updateSelectedAttributes = (newSelection: string[]) => {
            selectedAttributes.length = 0;
            selectedAttributes.push(...newSelection);
            const attributeTable: OrMwcTable = hostElement.shadowRoot!.getElementById(id + "-attribute-table") as OrMwcTable;
            const headersAndRows = getHeadersAndRows();
            attributeTable!.columns = headersAndRows[0];
            attributeTable!.rows = headersAndRows[1];
            config.views[asset.id!] = [...selectedAttributes];
            manager.console.storeData("OrAssetConfig", config);
        };

        let config: {views: {[assetId: string]: string[]}};

        manager.console.retrieveData("OrAssetConfig")
            .then(conf => {

                config = (conf as any);

                if (!config) {
                    config = {
                        views: {}
                    };
                }
                if (!config.views) {
                    config.views = {};
                }

                const view = config.views[asset.id!];

                if (view) {
                    updateSelectedAttributes([...view]);
                }
            });

        // Get available and selected attributes from asset descriptor if not defined in config
        if (availableAttributes.length === 0) {
            const descriptor = AssetModelUtil.getAssetTypeInfo(childAssetType);
            if (descriptor && descriptor.attributeDescriptors) {
                availableAttributes = descriptor.attributeDescriptors.map((desc) => desc.name!);
            }
        }
        if ((!selectedAttributes || selectedAttributes.length === 0) && availableAttributes) {
            selectedAttributes = [...new Set(childAssets.map(childAsset => Object.keys(childAsset.attributes!)).flat())];
        }

        const getHeadersAndRows: () => [string[], string[][]] = () => {

            const attrNames = [...selectedAttributes].sort();

            const headers = attrNames.map((attrName) => {
                const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, childAssetType);
                return Util.getAttributeLabel(undefined, attributeDescriptor, asset.type, false);
            });

            const rows = childAssets.map((childAsset) => {
                // todo: it's only processing including selected headers here...
                // move this to the columnFilter option of the table
                const arr = attrNames.map((attributeName) => {
                    return childAsset.attributes![attributeName] ? childAsset.attributes![attributeName].value! as string : "";
                });
                arr.unshift(childAsset.name!);
                return arr;
            });

            headers.unshift(i18next.t("groupAssetName"));

            return [headers, rows];
        }

        const attributePickerModalOpen = () => {

            const newlySelectedAttributes = [...selectedAttributes];

            showOkCancelDialog(
                i18next.t("addRemoveAttributes"),
                html`
                    <div style="display: grid">
                        ${availableAttributes.sort().map((attribute) =>
                            html`<div style="grid-column: 1 / -1;">
                                    <or-mwc-input .type="${InputType.CHECKBOX}" .label="${i18next.t(Util.camelCaseToSentenceCase(attribute))}" .value="${!!selectedAttributes.find((selected) => selected === attribute)}"
                                        @or-mwc-input-changed="${(evt: OrInputChangedEvent) => evt.detail.value ? newlySelectedAttributes.push(attribute) : newlySelectedAttributes.splice(newlySelectedAttributes.findIndex((s) => s === attribute), 1)}"></or-mwc-input>
                                </div>`)}
                    </div>
                `
            ).then((ok) => {
                if (ok) {
                    updateSelectedAttributes(newlySelectedAttributes);
                }
            });
        };

        const headersAndRows = getHeadersAndRows();

        // Define the DOM content for this panel
        return html`
                <style>
                    .asset-group-add-remove-button {
                        position: absolute;
                        --or-mwc-input-color: currentColor;
                        top: calc(var(--internal-or-asset-viewer-panel-padding) - 15px);
                        right: calc(var(--internal-or-asset-viewer-panel-padding) - 15px);
                    }
                    .asset-group-add-remove-button.active {
                        cursor: pointer;
                        opacity: 1;
                    }
                </style>
                <or-mwc-input .type="${InputType.BUTTON}" class="asset-group-add-remove-button" icon="pencil" @click="${() => attributePickerModalOpen()}"></or-mwc-input>
                <or-mwc-table .columns="${headersAndRows[0]}" .rows="${headersAndRows[1]}" .id="${id}-attribute-table" .config="${{stickyFirstColumn: true}}"></or-mwc-table>
            `;
    }

    if (panelConfig.type === "linkedUsers") {

        const hasReadAdminRole = manager.hasRole(ClientRole.READ_ADMIN);
        const assetLinkInfos = assetInfo.userAssetLinks;

        if (!hasReadAdminRole) {
            return;
        }

        if (!assetLinkInfos || assetLinkInfos.length === 0) {
            return;
        }

        const cols = [i18next.t("username"), i18next.t("roles"), i18next.t("restrictedUser")];
        const rows = assetLinkInfos.sort(Util.sortByString(u => u.usernameAndId)).map(assetLinkInfo => {
            return [
                assetLinkInfo.usernameAndId,
                assetLinkInfo.roles.join(", "),
                assetLinkInfo.restrictedUser ? i18next.t("yes") : i18next.t("no")
            ];
        });
        return html`<or-mwc-table .rows="${rows}" .config="${{stickyFirstColumn:false}}" .columns="${cols}"
                                  @or-mwc-table-row-click="${(ev: OrMwcTableRowClickEvent) => { 
                                      hostElement.dispatchEvent(new OrAssetViewerLoadUserEvent(assetLinkInfos[ev.detail.index].userId));
                                  }}">
                    </or-mwc-table>`;
    }
}

export function getAttributeTemplate(asset: Asset, attribute: Attribute<any>, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig, itemConfig: InfoPanelItemConfig): TemplateResult {
    if (viewerConfig.attributeViewProvider) {
        const result = viewerConfig.attributeViewProvider(asset, attribute, hostElement, viewerConfig, panelConfig);
        if (result) {
            return result;
        }
    }

    let attrLabel: string | undefined;
    let attrDisabled: boolean | undefined;
    let attrReadonly: boolean | undefined;
    let attrDisableButton: boolean | undefined;
    let attrInputType: InputType | undefined;
    let attrDisableHelper: boolean | undefined;

    if (itemConfig) {
        attrLabel = itemConfig.label;
        attrDisabled = itemConfig.disabled;
        attrReadonly = itemConfig.readonly;
        attrDisableButton = itemConfig.disableButton;
        attrDisableHelper = itemConfig.disableHelperText;
        attrInputType = itemConfig.inputTypeOverride;
    }

    return html`
        <or-attribute-input class="force-btn-padding" disablesubscribe .assetType="${asset!.type}" .attribute="${attribute}" .assetId="${asset.id!}" .disabled="${attrDisabled}" .label="${attrLabel}" .readonly="${attrReadonly}" resizeVertical .disableButton="${attrDisableButton}" .inputType="${attrInputType}" .hasHelperText="${!attrDisableHelper}" .fullWidth="${attribute.name === 'location' ? true : false}"></or-attribute-input>
    `;
}

export function getPropertyTemplate(asset: Asset, property: string, hostElement: LitElement, viewerConfig: AssetViewerConfig | undefined, panelConfig: PanelConfig | undefined, itemConfig: InfoPanelItemConfig) {
    let value = (asset as { [index: string]: any })[property];

    if (viewerConfig && viewerConfig.propertyViewProvider && panelConfig) {
        const result = viewerConfig.propertyViewProvider(asset, property, value, hostElement, viewerConfig, panelConfig);
        if (result) {
            return result;
        }
    }

    let type = InputType.TEXT;

    switch (property) {
        case "parentId":
            // Display the path instead
            value = asset.path || ["", asset.parentId];

            // Populate value when we get the response
            const ancestors = [...value];
            // Remove this asset from the path
            ancestors.pop();
            value = "";
            if (ancestors.length > 0) {
                getAssetNames(ancestors).then(
                    (names) => {
                        if (hostElement && hostElement.shadowRoot) {
                            const pathField = hostElement.shadowRoot.getElementById("property-parentId") as OrMwcInput;
                            if (pathField) {
                                pathField.value = names.join(" > ");
                            }
                        }
                    }
                );
                value = i18next.t("loading");
            }
            break;
        case "createdOn":
            type = InputType.DATETIME;
            break;
        case "accessPublicRead":
            type = InputType.CHECKBOX;
            break;
    }

    return html`<or-mwc-input id="property-${property}" .type="${type}" dense .value="${value}" .readonly="${itemConfig.readonly !== undefined ? itemConfig.readonly : true}" .label="${itemConfig.label}"></or-mwc-input>`;
}

export function getField(name: string, itemConfig?: InfoPanelItemConfig, content?: TemplateResult): TemplateResult {
    if (!content) {
        return html``;
    }

    return html`
            <div id="field-${name}" style="${itemConfig && itemConfig.styles ? styleMap(itemConfig.styles) : ""}" class=${classMap({field: true, mobileHidden: !!itemConfig && !!itemConfig.hideOnMobile})}>
                ${content}
            </div>
        `;
}

async function getAssetNames(ids: string[]): Promise<string[]> {
    const response = await manager.rest.api.AssetResource.queryAssets({
        select: {
            attributes: []
        },
        ids: ids
    });

    if (response.status !== 200 || !response.data || response.data.length !== ids.length) {
        return ids;
    }

    return ids.map((id) => response.data.find((asset) => asset.id === id)!.name!);
}

async function getAssetChildren(parentId: string, childAssetType: string): Promise<Asset[]> {
    let response: GenericAxiosResponse<Asset[]>;

    try {
        response = await manager.rest.api.AssetResource.queryAssets({
            parents: [
                {
                    id: parentId
                }
            ]
        });
    } catch (e) {
        console.log("Failed to get child assets: " + e);
        return [];
    }

    if (response.status !== 200 || !response.data) {
        return [];
    }

    return response.data.filter((asset) => asset.type === childAssetType);
}

async function getLinkedUserInfo(userAssetLink: UserAssetLink): Promise<UserAssetLinkInfo> {
    const userId = userAssetLink.id!.userId!;
    const username = userAssetLink.userFullName!;

    const roleNames = await manager.rest.api.UserResource.getUserRoles(manager.displayRealm, userId)
        .then((response) => {
            return response.data.filter(role => role.composite).map(r => r.name!);
        })
        .catch((err) => {
            console.info('User not allowed to get roles', err);
            return [];
        });

    const isRestrictedUser = await manager.rest.api.UserResource.getUserRealmRoles(manager.displayRealm, userId)
        .then((rolesRes) => {
            return rolesRes.data ? !!rolesRes.data.find(r => r.assigned && r.name === "restricted_user") : false;
        });

    return {
        userId: userId,
        usernameAndId: username,
        roles: roleNames,
        restrictedUser: isRestrictedUser
    };
}

async function getLinkedUsers(asset: Asset): Promise<UserAssetLinkInfo[]> {

    try {
        return await manager.rest.api.AssetResource.getUserAssetLinks(
            {realm: manager.displayRealm, assetId: asset.id}
        ).then((response) => {
            const userAssetLinks = response.data;
            const infoPromises = userAssetLinks.map(userAssetLink => {
                return getLinkedUserInfo(userAssetLink)
            });

            return Promise.all(infoPromises);
        });

    } catch (e) {
        console.log("Failed to get child assets: " + e);
        return [];
    }
}

export async function saveAsset(asset: Asset): Promise<SaveResult> {

    const isUpdate = !!asset.id && asset.version !== undefined;
    let success: boolean;
    let id: string = "";

    try {
        if (isUpdate) {
            if (!asset.id) {
                throw new Error("Request to update existing asset but asset ID is not set");
            }
            const response = await manager.rest.api.AssetResource.update(asset.id!, asset);
            success = response.status === 200;
            id = asset.id!;
        } else {
            const response = await manager.rest.api.AssetResource.create(asset);
            success = response.status === 200;
            if (success) {
                id = response.data.id!;
            }
        }
    } catch (e) {
        success = false;
        showSnackbar(undefined, (isUpdate ? "saveAssetFailed" : "createAssetFailed"), "dismiss");
        console.error("Failed to save asset", e);
    }

    return {
        assetId: id,
        success: success,
        isNew: !isUpdate
    };
}

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export const DEFAULT_ASSET_PROPERTIES = [
    "name",
    "createdOn",
    "type",
    "parentId",
    "accessPublicRead"
];

export const DEFAULT_VIEWER_CONFIG: AssetViewerConfig = {
    viewerStyles: {},
    panels: [
        {
            type: "group",
            title: "underlyingAssets"
        },
        {
            type: "info",
            hideOnMobile: true,
            properties: {
                include:[]
            },
            attributes: {
                include: ["notes", "manufacturer", "model"]
            }
        },
        {
            title: "attributes",
            type: "info",
            properties: {
                include:[]
            },
            attributes: {
                exclude: ["location", "notes", "manufacturer", "model"]
            }
        },
        {
            type: "setup",
            hideOnMobile: false
        },
        {
            title: "location",
            type: "info",
            column: 1,
            properties: {
                include:[]
            },
            attributes: {
                include: ["location"],
                itemConfig: {
                    location: {
                        label: "",
                        readonly: true
                    }
                }
            }
        },
        {
            type: "history",
            column: 1
        },
        {
            type: "linkedUsers",
            column: 1
        }
    ]
};

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(manager)(translate(i18next)(LitElement)) {

    static get styles() {
        return [
            unsafeCSS(tableStyle),
            progressCircular,
            panelStyles,
            style
        ];
    }

    @property({type: Object, reflect: false})
    public asset?: Asset;

    @property({type: Array})
    public ids: string[] | undefined;

    @property({type: Object})
    public config?: ViewerConfig;

    @property({type: Boolean})
    public editMode?: boolean;

    @property({type: Boolean})
    public readonly?: boolean;

    @state()
    protected _assetInfo?: AssetInfo;

    @state()
    protected _validationResults: ValidatorResult[] = [];

    @query("#wrapper")
    protected wrapperElem!: HTMLDivElement;

    @query("#save-btn")
    protected saveBtnElem!: OrMwcInput;

    @query("#edit-btn")
    protected editBtnElem!: OrMwcInput;

    @query("#editor")
    protected editor!: OrEditAssetPanel;

    @query("#asset-header")
    protected headerElem!: HTMLDivElement;

    @query("#view-container")
    protected containerElem!: HTMLDivElement;

    protected _saveResult?: SaveResult;

    constructor() {
        super();
        this.addEventListener(OrEditAssetModifiedEvent.NAME, (ev: OrEditAssetModifiedEvent) => this._onAssetModified(ev.detail));
    }

    public isModified() {
        return !!this.editMode && this._assetInfo && this._assetInfo.modified;
    }

    shouldUpdate(changedProperties: PropertyValues): boolean {

        if (this._isReadonly()) {
            this.editMode = false;
        }

        if (changedProperties.has("ids")) {
            this._assetInfo = undefined;
            this.asset = undefined;

            // Set asset ID on mixin which will go and load the asset
            if (this.ids && this.ids.length === 1) {
                super.assetIds = [this.ids[0]];
            } else {
                super.assetIds = undefined;
            }
        }

        if (changedProperties.has("asset")) {
            this._assetInfo = undefined;
            this.ids = undefined;
            super.assetIds = undefined;

            if (this.asset) {
                this.loadAssetInfo(this.asset)
                    .then(assetInfo => this._assetInfo = assetInfo)
                    .catch(reason => {
                        // We can ignore this as it should indicate that the asset has changed
                    });
            }
        }

        return super.shouldUpdate(changedProperties);
    }

    updated(_changedProperties: PropertyValues) {
        if (_changedProperties.has("asset")) {
            this._doValidation();
        }
    }

    async loadAssetInfo(asset: Asset): Promise<AssetInfo> {

        if (!asset) {
            throw new Error("Asset has changed");
        }

        if (!asset.attributes) {
            asset.attributes = {};
        }

        const exists = !!asset.id;
        const modified = !exists;
        const viewerConfig = this._getPanelConfig(asset);

        if (!exists) {
            // Newly created asset not yet saved
            return {
                asset: asset,
                modified: modified,
                viewerConfig: viewerConfig,
                attributeTemplateMap: {}
            };
        }

        const links = await getLinkedUsers(asset);

        // Check this asset is still the correct one
        if (!this.ids || this.ids.length != 1 || this.ids[0] !== asset.id) {
            throw new Error("Asset has changed");
        }

        // Load child assets for group asset
        let childAssets: Asset[] | undefined = undefined;
        if (asset.type === WellknownAssets.GROUPASSET) {
            childAssets = await getAssetChildren(asset.id!, asset.attributes![WellknownAttributes.CHILDASSETTYPE].value);
        }

        // Check this asset is still the correct one
        if (!this.ids || this.ids.length != 1 || this.ids[0] !== asset.id) {
            throw new Error("Asset has changed");
        }

        return {
            asset: asset,
            modified: modified,
            viewerConfig: viewerConfig,
            childAssets: childAssets,
            userAssetLinks: links,
            attributeTemplateMap: {}
        };
    }

    protected _doValidation() {
        if (this.editMode && this.editor) {
            this._validationResults = this.editor.validate();
        }
    }

    protected _onParentChangeClick() {
        let dialog: OrMwcDialog;

        const blockEvent = (ev: Event) => {
            ev.stopPropagation();
        };

        const dialogContent = html`
            <or-asset-tree id="parent-asset-tree" disableSubscribe readonly .selectedIds="${[]}"
                           @or-asset-tree-request-select="${blockEvent}"
                           @or-asset-tree-selection-changed="${blockEvent}"></or-asset-tree>`;

        const setParent = () => {
            const assetTree = dialog.shadowRoot!.getElementById("parent-asset-tree") as OrAssetTree;
            let idd = assetTree.selectedIds!.length === 1 ? assetTree.selectedIds![0] : undefined;

            this.dispatchEvent(new OrAssetViewerChangeParentEvent(idd, this.ids || []));
        };

        const clearParent = () => {
            this.dispatchEvent(new OrAssetViewerChangeParentEvent(undefined, this.ids || []));
        };

        const dialogActions: DialogAction[] = [
            {
                actionName: "clear",
                content: "none",
                action: clearParent
            },
            {
                actionName: "ok",
                content: "ok",
                action: setParent
            },
            {
                default: true,
                actionName: "cancel",
                content: "cancel"
            }
        ];

        dialog = showDialog(new OrMwcDialog()
            .setContent(dialogContent)
            .setActions(dialogActions)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        width: 400px;
                        height: 800px;
                        display: flex;
                        overflow: visible;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }

                    #dialog-content {
                        flex: 1;
                        overflow: visible;
                        min-height: 0;
                        padding: 0;
                    }

                    footer.mdc-dialog__actions {
                        border-top: 1px solid ${unsafeCSS(DefaultColor5)};
                    }

                    or-asset-tree {
                        height: 100%;
                    }
                </style>
            `)
            .setHeading(i18next.t("setParent"))
            .setDismissAction(null));
    }

    protected render(): TemplateResult | void {

        const noSelection = !this.asset && (!this.ids || this.ids.length === 0);
        const multiSelection = !this.asset && (this.ids && this.ids.length > 1);

        if (multiSelection) {
            return html `
                <div class="msg">
                    <div class="multipleAssetsView">
                        <or-translate value="multiAssetSelected" .options="${ { assetNbr: this.ids!.length } }"></or-translate>
                        <or-mwc-input .type="${InputType.BUTTON}" label="changeParent" @click="${() => this._onParentChangeClick()}" outlined></or-mwc-input>
                    </div>
                </div>
            `;
        }

        if (noSelection) {
            return html`
                <div class="msg"><or-translate value="noAssetSelected"></or-translate></div>
            `;
        }

        if (!this._assetInfo) {
            return html`
                <div class="msg"><or-translate value="loading"></or-translate></div>
            `;
        }

        if (!this._assetInfo.asset) {
            return html`
                <div><or-translate value="notFound"></or-translate></div>
            `;
        }

        const asset = this._assetInfo.asset;
        const descriptor = AssetModelUtil.getAssetDescriptor(asset.type!);
        const editMode = !!this.editMode;
        let content: TemplateResult | string = ``;
        let validationErrors: string[] = [];

        if (editMode) {
            content = html`
                <div id="edit-container">
                    <or-edit-asset-panel id="editor" .asset="${asset}"></or-edit-asset-panel>
                </div>
            `;

            validationErrors = this._validationResults
                .filter((validationResult) => !validationResult.valid || validationResult.metaResults && validationResult.metaResults.some((r) => !r.valid))
                .flatMap((validationResult) => {
                    const errors: string[] = [];
                    if (!validationResult.valid) {
                        errors.push(i18next.t("validation.invalidAttributeValue", {attrName: validationResult.name}));
                    }
                    if (validationResult.metaResults) {
                        validationResult.metaResults.filter((result) => !result.valid).forEach((metaResult) => {
                            errors.push(i18next.t("validation.invalidMetaItemValue", {attrName: validationResult.name, metaName: metaResult.name}));
                        });
                    }
                    return errors;
                });
        } else {
            const viewerConfig = this._assetInfo.viewerConfig;

            if (viewerConfig.panels) {

                const leftColumn: (TemplateResult | string)[] = [];
                const rightColumn: (TemplateResult | string)[] = [];

                viewerConfig.panels.forEach((panelConfig, index) => {
                    if (!panelConfig.hide) {
                        const id = index+"";
                        const column = panelConfig.column || 0;
                        const template = getPanel(id, panelConfig, getPanelContent(id, this._assetInfo!, this, viewerConfig, panelConfig)) || ``;

                        if (template) {
                            if (column == 0) {
                                leftColumn.push(template);
                            } else {
                                rightColumn.push(template);
                            }
                        }
                    }});

                content = html`                
                    <div id="view-container" style="${viewerConfig.viewerStyles ? styleMap(viewerConfig.viewerStyles) : ""}" @scroll="${this._toggleHeaderShadow}">
                        <div id="left-column" class="panelContainer">
                            ${leftColumn}
                        </div>
                        <div id="right-column" class="panelContainer">
                            ${rightColumn}
                        </div>
                    </div>`;
            }
        }

        return html`
            <div id="wrapper">
                <div id="asset-header" class=${editMode ? "editmode" : ""}>
                    <a class="back-navigation" @click="${() => window.history.back()}">
                        <or-icon icon="chevron-left"></or-icon>
                    </a>
                    <div id="title">
                        <or-icon title="${descriptor && descriptor.name ? descriptor.name : "unset"}" style="--or-icon-fill: ${descriptor && descriptor.colour ? "#" + descriptor.colour : "unset"}" icon="${descriptor && descriptor.icon ? descriptor.icon : AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.THINGASSET)}"></or-icon>
                        ${editMode 
                                ? html`
                                    <or-mwc-input id="name-input" .type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined .label="${i18next.t("name")}" .value="${asset.name}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => {asset!.name = e.detail.value; this._assetInfo!.modified = true; this._doValidation();}}"></or-mwc-input>
                                `
                                : html`<span>${asset.name}</span>`}
                    </div>
                    <div id="right-wrapper" class="mobileHidden">
                        ${validationErrors.length === 0 ? (asset!.createdOn ? html`<or-translate id="created-time" class="tabletHidden" value="createdOnWithDate" .options="${{ date: new Date(asset!.createdOn!) } as TOptions<InitOptions>}"></or-translate>` : ``) : html`<span id="error-wrapper" .title="${validationErrors.join("\n")}"><or-icon icon="alert"></or-icon><or-translate class="tabletHidden" value="validation.invalidAsset"></or-translate></span>`}
                        ${editMode ? html`<or-mwc-input id="save-btn" .disabled="${!this.isModified()}" raised .type="${InputType.BUTTON}" label="save" @or-mwc-input-changed="${() => this._onSaveClicked()}"></or-mwc-input>` : ``}
                        ${!this._isReadonly() ? html`<or-mwc-input id="edit-btn" .disabled="${!this._assetInfo.asset.id}" outlined .type="${InputType.BUTTON}" .value="${this.editMode}" .label="${this.editMode ? i18next.t("viewAsset") : i18next.t("editAsset")}" icon="${this.editMode ? "eye" : "pencil"}" @or-mwc-input-changed="${() => this._onEditToggleClicked(!this.editMode!)}"></or-mwc-input>
                        `: ``}
                    </div>
                </div>
                ${content}
            </div>
        `;
    }

    protected _toggleHeaderShadow() {
        (this.containerElem.scrollTop > 0) ? this.headerElem.classList.add('scrolled') : this.headerElem.classList.remove('scrolled');
    }

    protected _isReadonly() {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_ASSETS);
    }

    protected _onEditToggleClicked(edit: boolean) {
        Util.dispatchCancellableEvent(
            this,
            new OrAssetViewerRequestEditToggleEvent(edit)).then(
                (detail) => {
                    if (detail.allow) {
                        this._doEditToggle(edit);
                    } else {
                        this.editBtnElem.value = !edit;
                    }
                });
    }

    protected _doEditToggle(edit: boolean) {
        this.editMode = edit;
        this.dispatchEvent(new OrAssetViewerEditToggleEvent(edit));
    }

    protected _onSaveClicked() {
        if (!this._assetInfo || !this._assetInfo.asset) {
            return;
        }

        Util.dispatchCancellableEvent(this, new OrAssetViewerRequestSaveEvent(this._assetInfo.asset))
            .then((detail) => {
                if (detail.allow) {
                    this._doSave();
                }
            });
    }

    protected async _doSave() {
        if (!this._assetInfo) {
            return;
        }

        const asset = this._assetInfo.asset;
        this.saveBtnElem.disabled = true;
        this.wrapperElem.classList.add("saving");

        this._saveResult = await saveAsset(asset);

        this.wrapperElem.classList.remove("saving");
        this.saveBtnElem.disabled = false;

        if (!this._assetInfo || this._assetInfo.asset !== asset) {
            // Asset has changed during save so ignore save result
            return;
        }

        if (this._saveResult.success) {
            this._assetInfo.modified = false;
            this.asset = undefined;
            this.ids = [this._saveResult.assetId];
        }

        this.dispatchEvent(new OrAssetViewerSaveEvent(this._saveResult));
    }

    protected _onAssetModified(validationResults: ValidatorResult[]) {
        if (this._assetInfo) {
            this._assetInfo.modified = true;
            this._validationResults = validationResults;
        }
    }

    _onEvent(event: SharedEvent) {
        const assetId = this.ids && this.ids.length > 0 ? this.ids[0] : undefined;
        const processEvent = (event.eventType === "asset" && (event as AssetEvent).asset!.id === assetId) || (event.eventType === "attribute" && (event as AttributeEvent).ref!.id == assetId);

        if (!processEvent) {
            return;
        }

        if (event.eventType === "asset") {

            const asset = (event as AssetEvent).asset!;

            if (!this._assetInfo) {
                this.loadAssetInfo(asset)
                    .then(assetInfo => this._assetInfo = assetInfo)
                    .catch(reason => {
                        // We can ignore this as it should indicate that the asset has changed
                    });
                return;
            }

            if (asset.id !== assetId) {
                return;
            }

            if (this.editMode) {
                // Asset hasn't been modified yet so just re-render with new version of asset
                if (!this._assetInfo.modified || (this._saveResult?.assetId === assetId)) {
                    this._assetInfo = undefined;
                    this._saveResult = undefined;
                    this.loadAssetInfo(asset)
                        .then(assetInfo => this._assetInfo = assetInfo)
                        .catch(reason => {
                            // We can ignore this as it should indicate that the asset has changed
                        });
                    return;
                }

                // Asset has changed whilst we're editing it so inform the user and reload
                showOkDialog("assetModified", i18next.t("assetModifiedMustRefresh")).then(() => {
                    this._assetInfo = undefined;
                    this.loadAssetInfo(asset)
                        .then(assetInfo => this._assetInfo = assetInfo)
                        .catch(reason => {
                            // We can ignore this as it should indicate that the asset has changed
                        });
                });
            } else {
                // Just reload the whole view
                this._assetInfo = undefined;
                this.loadAssetInfo(asset)
                    .then(assetInfo => this._assetInfo = assetInfo)
                    .catch(reason => {
                        // We can ignore this as it should indicate that the asset has changed
                    });
            }
        }

        if (event.eventType === "attribute") {

            if (!this._assetInfo) {
                return;
            }

            const asset = this._assetInfo.asset;

            // Inject the attribute as we don't subscribe to events from individual attribute inputs
            const attributeEvent = event as AttributeEvent;
            const attrName = attributeEvent.ref!.name!;

            if (asset && asset.attributes && asset.attributes[attrName]) {

                // Remove any cached template
                delete this._assetInfo.attributeTemplateMap[attrName];

                // Update attribute within the asset
                const attr = {...asset.attributes[attrName]};
                attr.value = attributeEvent.value;
                attr.timestamp = attributeEvent.timestamp;
                asset.attributes[attrName] = attr;

                if (this.editMode) {
                    // Notify editor that attribute has changed
                    const editor = this.shadowRoot!.getElementById("editor") as OrEditAssetPanel;

                    if (editor) {
                        editor.attributeUpdated(attrName);
                    }
                } else {
                    this.requestUpdate();
                }
            }
        }
    }

    protected _getPanelConfig(asset: Asset): AssetViewerConfig {
        const config = {...DEFAULT_VIEWER_CONFIG};

        if (this.config) {

            config.viewerStyles = {...config.viewerStyles};
            config.panels = config.panels ? [...config.panels] : [];
            const assetConfig = this.config.assetTypes && this.config.assetTypes.hasOwnProperty(asset.type!) ? this.config.assetTypes[asset.type!] : this.config.default;

            if (assetConfig) {

                if (assetConfig.viewerStyles) {
                    Object.assign(config.viewerStyles, assetConfig.viewerStyles);
                }

                if (assetConfig.panels) {
                    config.panels = assetConfig.panels;
                }

                config.attributeViewProvider = assetConfig.attributeViewProvider || (this.config.default ? this.config.default.attributeViewProvider : undefined);
                config.panelViewProvider = assetConfig.panelViewProvider || (this.config.default ? this.config.default.panelViewProvider : undefined);
                config.propertyViewProvider = assetConfig.propertyViewProvider || (this.config.default ? this.config.default.propertyViewProvider : undefined);
                config.historyConfig = assetConfig.historyConfig || this.config.historyConfig;
            }
        }
        return config;
    }
}
