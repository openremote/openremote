import {customElement, html, LitElement, property, PropertyValues, TemplateResult} from "lit-element";
import "@openremote/or-icon";
import "@openremote/or-input";
import "@openremote/or-attribute-input";
import "@openremote/or-attribute-history";
import "@openremote/or-chart";
import "@openremote/or-table";
import "@openremote/or-map";
import "@openremote/or-panel";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {OrTranslate, translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import manager, {AssetModelUtil, subscribe, Util} from "@openremote/core";
import {OrTable} from "@openremote/or-table";
import {OrChartConfig, OrChartEvent} from "@openremote/or-chart";
import {HistoryConfig, OrAttributeHistory, OrAttributeHistoryEvent} from "@openremote/or-attribute-history";
import {Type as MapType, Util as MapUtil} from "@openremote/or-map";
import {
    Asset,
    AssetAttribute,
    AssetEvent,
    AssetType,
    Attribute,
    AttributeEvent,
    AttributeType,
    MetaItem,
    MetaItemType
} from "@openremote/model";
import {style} from "./style";
import i18next from "i18next";
import {styleMap} from "lit-html/directives/style-map";
import {classMap} from "lit-html/directives/class-map";
import {DialogAction, OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

export type PanelType = "property" | "location" | "attribute" | "history" | "chart" | "group";

export interface PanelConfig {
    type?: PanelType;
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
    }}
}

export interface AssetViewerConfig {
    panels: {[name: string]: PanelConfig};
    viewerStyles?: { [style: string]: string };
    propertyViewProvider?: (property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (attributes: AssetAttribute[], panelName: string, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    mapType?: MapType;
    historyConfig?: HistoryConfig;
    chartConfig?: OrChartConfig;
}

export interface ViewerConfig {
    default?: AssetViewerConfig;
    assetTypes?: { [assetType: string]: AssetViewerConfig };
    propertyViewProvider?: (property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (attributes: AssetAttribute[], panelName: string, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    mapType?: MapType;
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

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(manager)(translate(i18next)(LitElement)) {

    public static DEFAULT_MAP_TYPE = MapType.VECTOR;
    public static DEFAULT_PANEL_TYPE: PanelType = "attribute";

    public static DEFAULT_CONFIG: AssetViewerConfig = {
        viewerStyles: {

        },
        panels: {
            group: {
                type: "group",
                // childAssetTypes: {
                //     "urn:openremote:asset:enviroment": {
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
            },
            chart: {
                type: "chart",
                hideOnMobile: true,
                panelStyles: {
                    gridColumn: "1 / -1",
                    gridRowStart: "1"
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
                    ${html`${Object.entries(this._viewerConfig.panels).map(([name, panelConfig]) => {
                        const panelTemplate = OrAssetViewer.getPanel(name, this.asset!, this._attributes!, this._viewerConfig!, panelConfig, this.shadowRoot);
                        return panelTemplate || ``;
                    })}`}
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

    public static getPanel(name: string, asset: Asset, attributes: AssetAttribute[], viewerConfig: AssetViewerConfig, panelConfig: PanelConfig, shadowRoot: ShadowRoot | null) {

        const content = OrAssetViewer.getPanelContent(name, asset, attributes, viewerConfig, panelConfig, shadowRoot);
        if (!content) {
            return;
        }

        return html`           
            <div class=${classMap({"panel": true, mobileHidden: panelConfig.hideOnMobile === true})} id="${name}-panel" style="${panelConfig && panelConfig.panelStyles ? styleMap(panelConfig.panelStyles) : ""}">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <or-translate value="${name}"></or-translate>
                    </div>
                    <div class="panel-content">
                        ${content}
                    </div>
                </div>
            </div>
        `;
    }

    public static getPanelContent(panelName: string, asset: Asset, attributes: AssetAttribute[], viewerConfig: AssetViewerConfig, panelConfig: PanelConfig, shadowRoot: ShadowRoot | null): TemplateResult | undefined {
        if (panelConfig.hide || attributes.length === 0) {
            return;
        }

        if (viewerConfig.panelViewProvider) {
            const template = viewerConfig.panelViewProvider(attributes, panelName, viewerConfig, panelConfig);
            if (template) {
                return template;
            }
        }

        let styles = panelConfig ? panelConfig.fieldStyles : undefined;
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
                    if (shadowRoot) {
                        const attributeHistory = shadowRoot.getElementById("attribute-history") as OrAttributeHistory;

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
                    let unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, attr, attributeDescriptor);
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
                        <or-input id="history-attribute-picker" value="${historyAttrs[0].name}" .label="${i18next.t("attribute")}" @or-input-changed="${(evt: OrInputChangedEvent) => attributeChanged(evt.detail.value)}" .type="${InputType.SELECT}" .options="${options}"></or-input>
                    </div>        
                    <or-attribute-history id="attribute-history" .config="${viewerConfig.historyConfig}" .assetType="${asset.type}"></or-attribute-history>

                `;
            }

        } else if (panelConfig && panelConfig.type === "chart") {

            let storeDataPointAttrs = attrs.filter((attr) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!))
          
            let assetAttributes;
            // let defaultAttrs = storeDataPointAttrs.filter((attr) => (defaultAttributes && defaultAttributes.indexOf(attr.name!) >= 0));
            // if(defaultAttrs.length > 0){
            //     assetAttributes = defaultAttrs;
            // } else 
            if(storeDataPointAttrs.length > 0) {
                assetAttributes = storeDataPointAttrs;
                assetAttributes.length = 1;
            }
            const assetList:Asset[] = [];
            if(assetAttributes) {
                assetAttributes.forEach(attr => assetList.push(asset));
            }
            content = html`
                <or-chart id="chart" .config="${viewerConfig.chartConfig}" .activeAsset="${asset}"  activeAssetId="${asset.id}" .assets="${assetList ? assetList : [asset]}" .assetAttributes="${assetAttributes}"></or-chart>
            `;

        } else if (panelConfig && panelConfig.type === "location") {

            const attribute = attrs.find((attr) => attr.name === AttributeType.LOCATION.attributeName);
            if (attribute) {
                // Special handling for location panel which shows an attribute selector and a map showing the location of the attribute
                const mapType = viewerConfig.mapType || OrAssetViewer.DEFAULT_MAP_TYPE;
                const lngLat = MapUtil.getLngLat(attribute);
                const center = lngLat ? lngLat.toArray() : undefined;
                const showOnMapMeta = Util.getFirstMetaItem(attribute, MetaItemType.SHOW_ON_DASHBOARD.urn!);
                const attributeMetaChanged = async (value: string) => {
                    if (shadowRoot) {

                        if (attribute) {

                            if(asset.id && asset.attributes && asset.attributes.location){

                                const showOnMapMeta = Util.getFirstMetaItem(attribute, MetaItemType.SHOW_ON_DASHBOARD.urn!);
                                if(showOnMapMeta) {
                                    showOnMapMeta.value = value;
                                } else {
                                    const meta:MetaItem = {
                                        name: MetaItemType.SHOW_ON_DASHBOARD.urn,
                                        value: value
                                    }

                                    if(attribute.meta){
                                        attribute.meta.push(meta);
                                    }
                                }
                                asset.attributes.location = {...attribute};
                                const response = await manager.rest.api.AssetResource.update(asset.id, asset);

                                if (response.status !== 200) {
                                }
                            }


                        }
                    }
                };

                content = html`
                    <style>
                        or-map {
                            border: #e5e5e5 1px solid;
                        }
                        
                        #location-map-input {
                            padding: 20px 0 0 0;
                        }
                    </style>
                    <or-map id="location-map" class="or-map" .center="${center}" type="${mapType}">
                         <or-map-marker-asset active .asset="${asset}"></or-map-marker-asset>
                    </or-map>
                    ${attribute.name === AttributeType.LOCATION.attributeName ? html`
                        <or-input id="location-map-input" type="${InputType.SWITCH}" @or-input-changed="${(evt: OrInputChangedEvent) => attributeMetaChanged(evt.detail.value)}" dense .value="${showOnMapMeta ? showOnMapMeta.value : undefined}" label="${i18next.t("showOnMap")}"></or-input>
                    ` : ``}                    
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
                const dialog: OrMwcDialog = shadowRoot!.getElementById(panelName + "-attribute-modal") as OrMwcDialog;

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

            // let headers = ["a", "b", "c"];
            // let rows = [["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"],
            //     ["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"], ["0", "1", "2"]];
            // let columnFilter = viewerConfig.groupConfig!.columnFilters || ["a"];

            // Function to update the table and message when assets or config changes
            let updateTable = () => {

                const loadingMsg: OrTranslate = shadowRoot!.getElementById(panelName + "-attribute-table-msg") as OrTranslate;
                const attributeTable: OrTable = shadowRoot!.getElementById(panelName + "-attribute-table") as OrTable;

                if (!loadingMsg || !attributeTable) {
                    return;
                }

                if (selectedAttributes.length === 0 || !childAssets || childAssets.length === 0) {
                    loadingMsg.value = "noData";
                    loadingMsg.hidden = false;
                    attributeTable.hidden = true;
                    return;
                }

                // Update table properties which will cause a re-render
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
            };

            // Load child assets async then update the table
            this.getAssetChildren(asset.id!, asset.attributes!.childAssetType.value).then((assetChildren) => {
                childAssets = assetChildren;
                updateTable();
            });

            // Define the DOM content for this panel
            content = html`
                <style>
                    #asset-group-add-remove-columns {
                        position: absolute;
                        top: 20px;
                        right: var(--internal-or-asset-viewer-panel-padding);
                    }
                </style>
                <or-icon id="asset-group-add-remove-columns" icon="plus-minus" @click="${() => attributePickerModalOpen()}"></or-icon>
                <or-table hidden .id="${panelName}-attribute-table" .options="{stickyFirstColumn:true}"></or-table>
                <span><or-translate id="${panelName}-attribute-table-msg" value="loading"></or-translate></span>
                <or-mwc-dialog id="${panelName}-attribute-modal" dialogTitle="addRemoveAttributes" .dialogActions="${attributePickerModalActions}"></or-mwc-dialog>
            `;
        } else {
            if(attrs.length === 0) {
                return undefined;
            }

            content = html`
                ${attrs.sort((attr1, attr2) => attr1.name! < attr2.name! ? -1 : attr1.name! > attr2.name! ? 1 : 0).map((attr) => {
                    let style = styles ? styles[attr.name!] : undefined;
                    return this.getField(attr.name!, false, style, OrAssetViewer.getAttributeTemplate(asset, attr, viewerConfig, panelConfig));
                })}
            `;
        }

        return content;
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

    public static getAttributeTemplate(asset: Asset, attribute: AssetAttribute, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) {
        if (viewerConfig.attributeViewProvider) {
            const result = viewerConfig.attributeViewProvider(attribute, viewerConfig, panelConfig);
            if (result) {
                return result;
            }
        }
        return html`
            <or-attribute-input dense .assetType="${asset!.type}" .attribute="${attribute}" .label="${i18next.t(attribute.name!)}"></or-attribute-input>
        `;
    }

    public static getField(name: string, isProperty: boolean, styles: { [style: string]: string } | undefined, content: TemplateResult | undefined) {
        if (!content) {
            return ``;
        }
        return html`
            <div id="field-${name}" style="${styles ? styleMap(styles) : ""}" class="field ${isProperty ? "field-property" : "field-attribute"}">
                ${content}
            </div>
        `;
    }

    // TODO: Add debounce in here to minimise render calls
    onAttributeEvent(event: AttributeEvent) {
        const attrName = event.attributeState!.attributeRef!.attributeName!;

        if (this.asset && this.asset.attributes && this.asset.attributes.hasOwnProperty(attrName)) {
            if (event.attributeState!.deleted) {
                delete this.asset.attributes[attrName];
                this.asset = {...this.asset}
            }
        }
    }

    onAssetEvent(event: AssetEvent) {
        this.asset = event.asset;
        this._loading = false;
    }

    protected _getPanelConfig(asset: Asset): AssetViewerConfig {
        let config = {...OrAssetViewer.DEFAULT_CONFIG};

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
                        if (config.panels.hasOwnProperty(name)) {
                            const panelStyles = {...config.panels[name].panelStyles};
                            const fieldStyles = {...config.panels[name].fieldStyles};
                            config.panels[name] = Object.assign(config.panels[name], {...assetPanelConfig});
                            config.panels[name].panelStyles = Object.assign(panelStyles, assetPanelConfig.panelStyles);
                            config.panels[name].fieldStyles = Object.assign(fieldStyles, assetPanelConfig.fieldStyles);
                        } else {
                            config.panels[name] = {...assetPanelConfig};
                        }
                    });
                }

                config.attributeViewProvider = assetConfig.attributeViewProvider || this.config.attributeViewProvider;
                config.panelViewProvider = assetConfig.panelViewProvider || this.config.panelViewProvider;
                config.propertyViewProvider = assetConfig.propertyViewProvider || this.config.propertyViewProvider;
                config.mapType = assetConfig.mapType || this.config.mapType;
                config.historyConfig = assetConfig.historyConfig || this.config.historyConfig;
            }
        }
        return config;
    }

    public static async getAssetChildren(id: string, childAssetType: string): Promise<Asset[]> {
        const response = await manager.rest.api.AssetResource.queryAssets({
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

        if (response.status !== 200 || !response.data) {
            return [];
        }

        return response.data.filter((asset) => asset.type === childAssetType);
    }
}
