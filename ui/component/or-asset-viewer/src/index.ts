import {customElement, html, LitElement, property, PropertyValues, TemplateResult} from "lit-element";
import "@openremote/or-icon";
import "@openremote/or-input";
import "@openremote/or-attribute-input";
import "@openremote/or-attribute-history";
import "@openremote/or-chart";
import "@openremote/or-table";
import "@openremote/or-panel";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {OrTranslate, translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import manager, {AssetModelUtil, subscribe, Util} from "@openremote/core";
import {OrTable} from "@openremote/or-table";
import {OrChartConfig, OrChartEvent} from "@openremote/or-chart";
import {HistoryConfig, OrAttributeHistory, OrAttributeHistoryEvent} from "@openremote/or-attribute-history";
import {
    Asset,
    AssetAttribute,
    AssetEvent,
    AssetType,
    Attribute,
    AttributeEvent,
    AttributeType,
    MetaItemType,
    SharedEvent
} from "@openremote/model";
import {style} from "./style";
import i18next from "i18next";
import {styleMap} from "lit-html/directives/style-map";
import {classMap} from "lit-html/directives/class-map";
import {DialogAction, OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import { GenericAxiosResponse } from "axios";
import { OrIcon } from "@openremote/or-icon";

export type PanelType = "property" | "location" | "attribute" | "history" | "chart" | "group";

export interface PanelConfig {
    type?: PanelType;
    title?: string;
    hide?: boolean;
    hideOnMobile?: boolean;
    defaults?: string[];
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: { [style: string]: string };
    fieldStyles?: { [field: string]: { [style: string]: string } };
}

export interface GroupPanelConfig extends PanelConfig {
    childAssetTypes?: { [assetType: string]: {
        availableAttributes?: string[];
        selectedAttributes?: string[];
    }};
}

export interface AssetViewerConfig {
    panels?: {[name: string]: PanelConfig};
    viewerStyles?: { [style: string]: string };
    propertyViewProvider?: (asset: Asset, property: string, value: any, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (asset: Asset, attribute: Attribute, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (asset: Asset, attributes: Attribute[], panelName: string, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    historyConfig?: HistoryConfig;
    chartConfig?: OrChartConfig;
}

export interface ViewerConfig {
    default?: AssetViewerConfig;
    assetTypes?: { [assetType: string]: AssetViewerConfig };
    historyConfig?: HistoryConfig;
}

class EventHandler {
    _callbacks: Function[];

    constructor() {
        this._callbacks = [];
    }

    startCallbacks() {
        return new Promise((resolve, reject) => {
            if (this._callbacks && this._callbacks.length > 0) {
                this._callbacks.forEach(cb => cb());
            }
            resolve();
        })

    }

    addCallback(callback: Function) {
        this._callbacks.push(callback);
    }
}
const onRenderComplete = new EventHandler();

function getPanel(name: string, panelConfig: PanelConfig, content: TemplateResult | undefined) {

    if (!content) {
        return;
    }

    return html`
            <div class=${classMap({panel: true, mobileHidden: panelConfig.hideOnMobile === true})} id="${name}-panel" style="${panelConfig && panelConfig.panelStyles ? styleMap(panelConfig.panelStyles) : ""}">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <or-translate value="${panelConfig.title ? panelConfig.title : name}"></or-translate>
                    </div>
                    <div class="panel-content">
                        ${content}
                    </div>
                </div>
            </div>
        `;
}

export function getPanelContent(panelName: string, asset: Asset, attributes: AssetAttribute[], hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig): TemplateResult | undefined {
    if (panelConfig.hide || attributes.length === 0) {
        return;
    }

    if (viewerConfig.panelViewProvider) {
        const template = viewerConfig.panelViewProvider(asset, attributes, panelName, hostElement, viewerConfig, panelConfig);
        if (template) {
            return template;
        }
    }

    const styles = panelConfig ? panelConfig.fieldStyles : undefined;
    const defaultAttributes = panelConfig && panelConfig.defaults ? panelConfig.defaults : undefined;
    const includedAttributes = panelConfig && panelConfig.include ? panelConfig.include : undefined;
    const excludedAttributes = panelConfig && panelConfig.exclude ? panelConfig.exclude : [];
    const attrs = attributes.filter((attr) =>
        (!includedAttributes || includedAttributes.indexOf(attr.name!) >= 0)
        && (!excludedAttributes || excludedAttributes.indexOf(attr.name!) < 0));

    let content: TemplateResult | undefined;

    // if (panelConfig && panelConfig.type === "property") {
    //     // Special handling for info panel which only shows properties
    //     let properties = OrAssetViewer.getInfoProperties(panelConfig);

    //     if (properties.length === 0) {
    //         return;
    //     }

    //     content = html`
    //         ${properties.map((prop) => {
    //         let style = styles ? styles[prop!] : undefined;
    //         return prop === "attributes" ? `` : OrAssetViewer.getField(prop, true, style, OrAssetViewer.getPropertyTemplate(prop, (asset as { [index: string]: any })[prop], viewerConfig, panelConfig, shadowRoot));
    //     })}
    //     `;
    // } else
    if (panelConfig && panelConfig.type === "history") {
        // Special handling for history panel which shows an attribute selector and a graph/data table of historical values
        const historyAttrs = attrs.filter((attr) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!));
        if (historyAttrs.length > 0) {

            const attributeChanged = (attributeName: string) => {
                if (hostElement.shadowRoot) {
                    const attributeHistory = hostElement.shadowRoot.getElementById("attribute-history") as OrAttributeHistory;

                    if (attributeHistory) {

                        let attribute: AssetAttribute | undefined;

                        if (attributeName) {
                            attribute = Util.getAssetAttribute(asset, attributeName);
                        }

                        attributeHistory.attribute = attribute;
                    }
                }
            };

            const options = historyAttrs.map((attr) => {
                const attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(attr.name!);
                let label = Util.getAttributeLabel(attr, attributeDescriptor);
                const unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, attr, attributeDescriptor);
                if(unit) {
                    label = label + " ("+i18next.t(unit)+")";
                }
                return [attr.name, label]
            });
            const attrName: string = historyAttrs[0].name!;
            onRenderComplete.addCallback(() => attributeChanged(attrName));
            content = html`
                    <style>
                       or-attribute-history{
                            min-height: 70px;
                            width: 100%;
                       }
                        #history-controls {
                            flex: 0;
                            margin-bottom: 10px;
                            position: absolute;
                        }
                        
                        #history-attribute-picker {
                            flex: 0;
                            width: 200px;
                        }
                        
                        or-attribute-history {
                            --or-attribute-history-controls-margin: 0 0 20px 204px;  
                        }
                        
                        @media screen and (max-width: 2028px) {
                          #history-controls {
                                position: unset;
                                margin: 0 0 10px 0;
                          }
                          
                          or-attribute-history {
                                --or-attribute-history-controls-margin: 10px 0 0 0;  
                                --or-attribute-history-controls-margin-children: 0 20px 20px 0;
                          }
                        }
                    </style>
                    <div id="history-controls">
                        <or-input id="history-attribute-picker" .checkAssetWrite="${false}" value="${historyAttrs[0].name}" .label="${i18next.t("attribute")}" @or-input-changed="${(evt: OrInputChangedEvent) => attributeChanged(evt.detail.value)}" .type="${InputType.SELECT}" .options="${options}"></or-input>
                    </div>        
                    <or-attribute-history id="attribute-history" .config="${viewerConfig.historyConfig}" .assetType="${asset.type}"></or-attribute-history>

                `;
        }

    }
        // DEPRECATED
        // else if (panelConfig && panelConfig.type === "chart") {

        //     let storeDataPointAttrs = attrs.filter((attr) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!))

        //     let assetAttributes;
        //     // let defaultAttrs = storeDataPointAttrs.filter((attr) => (defaultAttributes && defaultAttributes.indexOf(attr.name!) >= 0));
        //     // if(defaultAttrs.length > 0){
        //     //     assetAttributes = defaultAttrs;
        //     // } else
        //     if(storeDataPointAttrs.length > 0) {
        //         assetAttributes = storeDataPointAttrs;
        //         assetAttributes.length = 1;
        //     }
        //     const assetList:Asset[] = [];
        //     if(assetAttributes) {
        //         assetAttributes.forEach(attr => assetList.push(asset));
        //     }
        //     content = html`
        //         <or-chart id="chart" panelName="${panelName}" .config="${viewerConfig.chartConfig}" .activeAsset="${asset}"  activeAssetId="${asset.id}" .assets="${assetList ? assetList : [asset]}" .assetAttributes="${assetAttributes}"></or-chart>
        //     `;

    // }
    else if (panelConfig && panelConfig.type === "location") {

        const attribute = attrs.find((attr) => attr.name === AttributeType.LOCATION.attributeName);
        if (attribute) {
            content = html`
                <div class="field">
                    <or-attribute-input .assetType="${asset.type}" .attribute="${attribute}"></or-attribute-input>
                </div>
            `;
        }
    } else if (panelConfig && panelConfig.type === "group") {

        if (asset.type !== "urn:openremote:asset:group") {
            return;
        }

        // Get child asset type attribute value
        const childAssetTypeAttribute = Util.getAssetAttribute(asset, "childAssetType");
        const groupConfig = panelConfig as GroupPanelConfig;

        if (!childAssetTypeAttribute || typeof childAssetTypeAttribute.value !== "string") {
            return;
        }
        let childAssetType = childAssetTypeAttribute.value as string;
        let childAssets: Asset[] = [];

        // Determine available and selected attributes for the child asset type
        let availableAttributes: string[] = [];
        let selectedAttributes: string[] = [];
        let newlySelectedAttributes: string[] = []; // Updated when the dialog is open

        if (groupConfig.childAssetTypes && groupConfig.childAssetTypes[childAssetType]) {
            availableAttributes = groupConfig.childAssetTypes[childAssetType].availableAttributes ? groupConfig.childAssetTypes[childAssetType].availableAttributes! : [];
            selectedAttributes = groupConfig.childAssetTypes[childAssetType].selectedAttributes ? groupConfig.childAssetTypes[childAssetType].selectedAttributes! : [];
        }

        // Get available and selected attributes from asset descriptor if not defined in config
        if (availableAttributes.length === 0) {
            const descriptor = AssetModelUtil.getAssetDescriptor(childAssetType);
            if (descriptor && descriptor.attributeDescriptors) {
                availableAttributes = descriptor.attributeDescriptors.map((descriptor) => descriptor.attributeName!);
            }
        }
        if ((!selectedAttributes || selectedAttributes.length === 0) && availableAttributes) {
            selectedAttributes = [...availableAttributes];
        }

        const attributePickerModalActions: DialogAction[] = [
            {
                actionName: "ok",
                default: true,
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("ok")}"></or-input>`,
                action: () => {
                    selectedAttributes.length = 0;
                    selectedAttributes.push(...newlySelectedAttributes);
                    updateTable();
                }
            },
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
        ];

        const attributePickerModalOpen = () => {
            const dialog: OrMwcDialog = hostElement.shadowRoot!.getElementById(panelName + "-attribute-modal") as OrMwcDialog;

            if (dialog) {
                newlySelectedAttributes.length = 0;
                newlySelectedAttributes.push(...selectedAttributes);
                // Update content which will cause a re-render
                dialog.dialogContent = html`
                        <div style="display:grid">
                            ${availableAttributes.sort().map((attribute) =>
                    html`<div style="grid-column: 1 / -1;">
                                        <or-input .type="${InputType.CHECKBOX}" .label="${i18next.t(attribute)}" .value="${!!newlySelectedAttributes.find((selected) => selected === attribute)}"
                                            @or-input-changed="${(evt: OrInputChangedEvent) => evt.detail.value ? newlySelectedAttributes.push(attribute) : newlySelectedAttributes.splice(newlySelectedAttributes.findIndex((s) => s == attribute), 1)}"></or-input>
                                    </div>`)}
                        </div>
                    `;
                dialog.open();
            }
        };

        // Function to update the table and message when assets or config changes
        let updateTable = () => {

            const loadingMsg: OrTranslate = hostElement.shadowRoot!.getElementById(panelName + "-attribute-table-msg") as OrTranslate;
            const attributeTable: OrTable = hostElement.shadowRoot!.getElementById(panelName + "-attribute-table") as OrTable;
            const addRemoveButton: OrIcon = hostElement.shadowRoot!.getElementById(panelName + "-add-remove-columns") as OrIcon;

            if (!loadingMsg || !attributeTable || !addRemoveButton) {
                return;
            }

            if (selectedAttributes.length === 0 || !childAssets || childAssets.length === 0) {
                loadingMsg.value = "noChildAssets";
                loadingMsg.hidden = false;
                attributeTable.hidden = true;
                addRemoveButton.classList.remove("active");
                return;
            }

            // Update table properties which will cause a re-render
            addRemoveButton.classList.add("active");
            loadingMsg.hidden = true;
            attributeTable.hidden = false;
            const headers = [...selectedAttributes].sort();
            attributeTable.headers = headers.map((header) => i18next.t(header));
            attributeTable.headers.unshift(i18next.t("groupAssetName"));
            attributeTable.rows = childAssets.map((asset) => {
                // todo: it's only processing including selected headers here...
                // move this to the columnFilter option of the table
                const arr = headers.map((attributeName) => {
                    return asset.attributes![attributeName] ? asset.attributes![attributeName].value! as string : "";
                });
                arr.unshift(asset.name!);
                return arr;
            });
            window.setTimeout(() => OrAssetViewer.generateGrid(hostElement.shadowRoot), 0);
        };

        // Load child assets async then update the table
        getAssetChildren(asset.id!, asset.attributes!.childAssetType.value).then((assetChildren) => {
            childAssets = assetChildren;
            updateTable();
        });

        // Define the DOM content for this panel
        content = html`
                <style>
                    .asset-group-add-remove-button {
                        position: absolute;
                        top: 20px;
                        right: var(--internal-or-asset-viewer-panel-padding);
                        opacity: 0.5;
                    }
                    .asset-group-add-remove-button.active {
                        cursor: pointer;
                        opacity: 1;
                    }
                </style>
                <or-icon class="asset-group-add-remove-button" .id="${panelName}-add-remove-columns" icon="pencil" @click="${() => attributePickerModalOpen()}"></or-icon>
                <or-table hidden .id="${panelName}-attribute-table" .options="{stickyFirstColumn:true}"></or-table>
                <span><or-translate id="${panelName}-attribute-table-msg" value="loading"></or-translate></span>
                <or-mwc-dialog id="${panelName}-attribute-modal" dialogTitle="addRemoveAttributes" .dialogActions="${attributePickerModalActions}"></or-mwc-dialog>
            `;
    } else {
        if (attrs.length === 0) {
            return undefined;
        }

        content = html`
                ${attrs.sort((attr1, attr2) => attr1.name! < attr2.name! ? -1 : attr1.name! > attr2.name! ? 1 : 0).map((attr) => {
            let style = styles ? styles[attr.name!] : undefined;
            return getField(attr.name!, false, style, getAttributeTemplate(asset, attr, hostElement, viewerConfig, panelConfig));
        })}
            `;
    }

    return content;
}

export function getAttributeTemplate(asset: Asset, attribute: AssetAttribute, hostElement: LitElement, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) {
    if (viewerConfig.attributeViewProvider) {
        const result = viewerConfig.attributeViewProvider(asset, attribute, hostElement, viewerConfig, panelConfig);
        if (result) {
            return result;
        }
    }
    return html`
        <or-attribute-input dense .assetType="${asset!.type}" .attribute="${attribute}"}"></or-attribute-input>
    `;
}

export function getField(name: string, isProperty: boolean, styles: { [style: string]: string } | undefined, content: TemplateResult | undefined): TemplateResult {
    if (!content) {
        return html``;
    }
    return html`
            <div id="field-${name}" style="${styles ? styleMap(styles) : ""}" class="field ${isProperty ? "field-property" : "field-attribute"}">
                ${content}
            </div>
        `;
}

async function getAssetChildren(id: string, childAssetType: string): Promise<Asset[]> {
    let response: GenericAxiosResponse<Asset[]>;

    try {
        response = await manager.rest.api.AssetResource.queryAssets({
            select: {
                excludePath: true,
                excludeParentInfo: true
            },
            parents: [
                {
                    id: id
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

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(manager)(translate(i18next)(LitElement)) {

    public static DEFAULT_PANEL_TYPE: PanelType = "attribute";

    public static DEFAULT_VIEWER_CONFIG: AssetViewerConfig = {
        viewerStyles: {

        },
        panels: {
            group: {
                type: "group",
                // childAssetTypes: {
                //     "urn:openremote:asset:environment": {
                //         availableAttributes: ["nO2"],
                //         selectedAttributes: ["nO2"]
                //     }
                // },
                panelStyles: {}
            } as GroupPanelConfig,
            info: {
                type: "attribute",
                hideOnMobile: true,
                include: ["userNotes", "manufacturer", "model"],
                panelStyles: {
                },
                fieldStyles: {
                    name: {
                        width: "60%"
                    },
                    createdOn: {
                        width: "40%",
                        paddingLeft: "20px",
                        boxSizing: "border-box"
                    }
                }
            },
            location: {
                type: "location",
                include: ["location"],
                panelStyles: {
                },
                fieldStyles: {
                    location: {
                    }
                }
            },
            attributes: {
                type: "attribute",
                panelStyles: {
                }
            },
            history: {
                type: "history",
                panelStyles: {
                }
            }
        }
    };

    public static DEFAULT_INFO_PROPERTIES = [
        "name",
        "createdOn",
        "type",
        "path",
        "accessPublicRead"
    ];

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Object, reflect: false})
    public asset?: Asset;

    @property({type: String})
    public assetId?: string;

    @property({type: Object})
    public config?: ViewerConfig;

    @property()
    protected _loading: boolean = false;

    protected _viewerConfig?: AssetViewerConfig;
    protected _attributes?: AssetAttribute[];

    constructor() {
        super();
        window.addEventListener("resize", () => OrAssetViewer.generateGrid(this.shadowRoot));
        
        this.addEventListener(OrChartEvent.NAME, () => OrAssetViewer.generateGrid(this.shadowRoot));
        this.addEventListener(OrAttributeHistoryEvent.NAME, () => OrAssetViewer.generateGrid(this.shadowRoot));
    }

    shouldUpdate(changedProperties: PropertyValues): boolean {

        if (changedProperties.has("asset")) {
            this._viewerConfig = undefined;
            this._attributes = undefined;

            if (this.asset) {
                this._viewerConfig = this._getPanelConfig(this.asset);
                this._attributes = Util.getAssetAttributes(this.asset);
            }
        }

        return super.shouldUpdate(changedProperties);
    }

    protected render() {

        if (this._loading) {
            return html`
                <div class="msg"><or-translate value="loading"></or-translate></div>
            `;
        }

        if (!this.asset && !this.assetId) {
            return html`
                <div class="msg"><or-translate value="noAssetSelected"></or-translate></div>
            `;
        }

        if (!this.asset) {
            return html`
                <div><or-translate value="notFound"></or-translate></div>
            `;
        }

        if (!this._attributes || !this._viewerConfig) {
            return html``;
        }

        const descriptor = AssetModelUtil.getAssetDescriptor(this.asset!.type!);

        return html`
            <div id="wrapper">
                <div id="asset-header">
                    <a class="back-navigation" @click="${() => window.history.back()}">
                        <or-icon icon="chevron-left"></or-icon>
                    </a>
                    <div id="title">
                        <or-icon title="${descriptor && descriptor.type ? descriptor.type : "unset"}" style="--or-icon-fill: ${descriptor && descriptor.color ? "#" + descriptor.color : "unset"}" icon="${descriptor && descriptor.icon ? descriptor.icon : AssetType.THING.icon}"></or-icon>${this.asset.name}
                    </div>
                    <div id="created" class="mobileHidden"><or-translate value="createdOnWithDate" .options="${{ date: new Date(this.asset!.createdOn!) } as i18next.TOptions<i18next.InitOptions>}"></or-translate></div>
                </div>
                <div id="container" style="${this._viewerConfig.viewerStyles ? styleMap(this._viewerConfig.viewerStyles) : ""}">
                    ${this._viewerConfig.panels ? html`${Object.entries(this._viewerConfig.panels).map(([name, panelConfig]) => {
                        const panelTemplate = getPanel(name, panelConfig, getPanelContent(name, this.asset!, this._attributes!, this, this._viewerConfig!, panelConfig));
                        return panelTemplate || ``;
                    })}` : ``}
                </div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);

        if (_changedProperties.has("assetId")) {
            this.asset = undefined;
            if (this.assetId) {
                this._loading = true;
                super.assetIds = [this.assetId];
            } else {
                super.assetIds = undefined;
            }
        }

        this.onCompleted().then(() => {
            onRenderComplete.startCallbacks().then(() => {
                OrAssetViewer.generateGrid(this.shadowRoot);
            });
        });

    }

    async onCompleted() {
        await this.updateComplete;
    }

    public static generateGrid(shadowRoot: ShadowRoot | null) {
        if (shadowRoot) {
            const grid = shadowRoot.querySelector('#container');
            if (grid) {
                const rowHeight = parseInt(window.getComputedStyle(grid).getPropertyValue('grid-auto-rows'));
                const rowGap = parseInt(window.getComputedStyle(grid).getPropertyValue('grid-row-gap'));
                const items = shadowRoot.querySelectorAll('.panel');
                if (items) {
                    items.forEach((item) => {
                        const content = item.querySelector('.panel-content-wrapper');
                        if (content) {
                            const rowSpan = Math.ceil((content.getBoundingClientRect().height + rowGap) / (rowHeight + rowGap));
                            (item as HTMLElement).style.gridRowEnd = "span " + rowSpan;
                        }
                    });
                }
            }
        }
    }

    public static getInfoProperties(config?: PanelConfig): string[] {
        let properties = config && config.include ? config.include : OrAssetViewer.DEFAULT_INFO_PROPERTIES;

        if (config && config.exclude) {
            properties = properties.filter((p) => !config.exclude!.find((excluded) => excluded === p))
        }

        return properties;
    }

    // public static getPropertyTemplate(property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig, shadowRoot: ShadowRoot | null) {
    //     let type = InputType.TEXT;
    //     let minLength: number | undefined;
    //     let maxLength: number | undefined;

    //     if (viewerConfig.propertyViewProvider) {
    //         const result = viewerConfig.propertyViewProvider(property, value, viewerConfig, panelConfig);
    //         if (result) {
    //             return result;
    //         }
    //     }

    //     switch (property) {
    //         case "path":
    //             if (!value || !(Array.isArray(value))) {
    //                 return;
    //             }

    //             // Populate value when we get the response
    //             OrAssetViewer.getAssetNames(value as string[]).then(
    //                 (names) => {
    //                     if (shadowRoot) {
    //                         const pathField = shadowRoot.getElementById("property-path") as OrInput;
    //                         if (pathField) {
    //                             pathField.value = names.reverse().join(" > ");
    //                         }
    //                     }
    //                 }
    //             );
    //             value = i18next.t("loading");
    //             break;
    //         case "createdOn":
    //             type = InputType.DATETIME;
    //             break;
    //         case "accessPublicRead":
    //             type = InputType.CHECKBOX;
    //             break;
    //         case "name":
    //             minLength = 1;
    //             maxLength = 1023;
    //             break;
    //     }

    //     return html`<or-input id="property-${property}" type="${type}" .minLength="${minLength}" .maxLength="${maxLength}" dense .value="${value}" readonly label="${i18next.t(property)}"></or-input>`;
    // }

    // TODO: Add debounce in here to minimise render calls
    _onEvent(event: SharedEvent) {
        if (event.eventType === "asset") {
            this.asset = (event as AssetEvent).asset;
            this._loading = false;
            return;
        }

        if (event.eventType === "attribute") {
            const attributeEvent = event as AttributeEvent;
            const attrName = attributeEvent.attributeState!.attributeRef!.attributeName!;

            if (this.asset && this.asset.attributes && this.asset.attributes.hasOwnProperty(attrName)) {
                if (attributeEvent.attributeState!.deleted) {
                    delete this.asset.attributes[attrName];
                    this.asset = {...this.asset};
                }
            }
        }
    }

    protected _getPanelConfig(asset: Asset): AssetViewerConfig {
        const config = {...OrAssetViewer.DEFAULT_VIEWER_CONFIG};

        if (this.config) {

            config.viewerStyles = {...config.viewerStyles};
            config.panels = {...config.panels};
            const assetConfig = this.config.assetTypes && this.config.assetTypes.hasOwnProperty(asset.type!) ? this.config.assetTypes[asset.type!] : this.config.default;

            if (assetConfig) {

                if (assetConfig.viewerStyles) {
                    Object.assign(config.viewerStyles, assetConfig.viewerStyles);
                }

                if (assetConfig.panels) {
                    Object.entries(assetConfig.panels).forEach(([name, assetPanelConfig]) => {
                        if (config.panels!.hasOwnProperty(name)) {
                            const panelStyles = {...config.panels![name].panelStyles};
                            const fieldStyles = {...config.panels![name].fieldStyles};
                            config.panels![name] = Object.assign(config.panels![name], {...assetPanelConfig});
                            config.panels![name].panelStyles = Object.assign(panelStyles, assetPanelConfig.panelStyles);
                            config.panels![name].fieldStyles = Object.assign(fieldStyles, assetPanelConfig.fieldStyles);
                        } else {
                            config.panels![name] = {...assetPanelConfig};
                        }
                    });
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
