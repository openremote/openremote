import {customElement, html, LitElement, property, PropertyValues, TemplateResult} from "lit-element";
import "@openremote/or-icon";
import "@openremote/or-input";
import "@openremote/or-attribute-input";
import "@openremote/or-attribute-history";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";
import "@openremote/or-map";
import manager, {subscribe, Util, AssetModelUtil} from "@openremote/core";
import "@openremote/or-panel";
import {HistoryConfig, OrAttributeHistory} from "@openremote/or-attribute-history";
import {Type as MapType, Util as MapUtil} from "@openremote/or-map";
import {
    Asset,
    AssetAttribute,
    AssetEvent,
    AssetType,
    Attribute,
    AttributeEvent,
    AttributeType,
    MetaItemType,
    AttributeDescriptor
} from "@openremote/model";
import {style} from "./style";
import i18next from "i18next";
import {styleMap} from "lit-html/directives/style-map";
import {classMap} from "lit-html/directives/class-map";

export type PanelType = "property" | "location" | "attribute" | "history";

export interface PanelConfig {
    type?: PanelType;
    scrollable?: boolean;
    hide?: boolean;
    hideOnMobile?: boolean;
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: { [style: string]: string };
    fieldStyles?: { [field: string]: { [style: string]: string } };
}

export interface AssetViewerConfig {
    panels: {[name: string] : PanelConfig};
    viewerStyles?: { [style: string]: string };
    propertyViewProvider?: (property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (attributes: AssetAttribute[], panelName: string, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    mapType?: MapType;
    historyConfig?: HistoryConfig;
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

function getAttributeLabel(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, fallback?: string): string {
    if (!attribute && !descriptor) {
        return fallback || "";
    }

    const labelMetaValue = AssetModelUtil.getMetaValue(MetaItemType.LABEL, attribute, descriptor);
    const name = attribute ? attribute.name : descriptor!.attributeName;
    return i18next.t([name, fallback || labelMetaValue || name]);
}

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(manager)(translate(i18next)(LitElement)) {

    public static DEFAULT_MAP_TYPE = MapType.VECTOR;
    public static DEFAULT_PANEL_TYPE: PanelType = "attribute";

    public static DEFAULT_CONFIG: AssetViewerConfig = {
        viewerStyles: {
            gridTemplateColumns: "repeat(12, 1fr)",
            gridTemplateRows: "30px max-content"
        },
        panels: {
            "info": {
                type: "property",
                panelStyles: {
                    gridColumnStart: "1",
                    gridColumnEnd: "7",
                    gridRowStart: "2",
                    gridRowEnd: "3",
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
            "location": {
                type: "location",
                scrollable: false,
                include: ["location"],
                panelStyles: {
                    gridColumnStart: "7",
                    gridColumnEnd: "13",
                    gridRowStart: "2",
                    gridRowEnd: "4",
                    minHeight: "400px"
                },
                fieldStyles: {
                    location: {
                        height: "100%",
                        margin: "0"
                    }
                }
            },
            "attributes": {
                type: "attribute",
                panelStyles: {
                    gridColumnStart: "1",
                    gridColumnEnd: "7",
                    gridRowStart: "3",
                    gridRowEnd: "5"
                }
            },
            "history": {
                type: "history",
                panelStyles: {
                    gridColumnStart: "7",
                    gridColumnEnd: "13",
                    gridRowStart: "4",
                    gridRowEnd: "5",
                },
                scrollable: false
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
        var assetDate = new Date(this.asset!.createdOn!);
        var formattedDate = assetDate.toLocaleString('nl-NL');

        return html`
            <div id="container" style="${this._viewerConfig.viewerStyles ? styleMap(this._viewerConfig.viewerStyles) : ""}">
                <div id="asset-header">
                    <div class="title">
                        <or-icon title="${descriptor && descriptor.type ? descriptor.type : "unset"}" style="--or-icon-fill: ${descriptor && descriptor.color ? "#" + descriptor.color : "unset"}" icon="${descriptor && descriptor.icon ? descriptor.icon : AssetType.THING.icon}"></or-icon>${this.asset.name}
                    </div>
                    <div class="created mobileHidden">Aangemaakt op: ${formattedDate}</div>
                </div>
            ${html`${Object.entries(this._viewerConfig.panels).map(([name, panelConfig]) => {
                const panelTemplate = OrAssetViewer.getPanel(name, this.asset!, this._attributes!, this._viewerConfig!, panelConfig, this.shadowRoot);
                return panelTemplate || ``;
            })}`
            }`;
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
                <div class="panel-title">
                    <or-translate value="${name}"></or-translate>
                </div>
                ${!panelConfig || panelConfig.scrollable === undefined || panelConfig.scrollable ? html`
                    <or-panel class="panel-content-wrapper">
                        <div class="panel-content">
                            ${content}
                        </div>
                    </or-panel>
                `: html`
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            ${content}
                        </div>
                    </div>
                `}
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

        const includedAttributes = panelConfig && panelConfig.include ? panelConfig.include : undefined;
        const excludedAttributes = panelConfig && panelConfig.exclude ? panelConfig.exclude : [];
        const attrs = attributes.filter((attr) =>
            (!includedAttributes || includedAttributes.indexOf(attr.name!) >= 0)
            && (!excludedAttributes || excludedAttributes.indexOf(attr.name!) < 0));

        let content: TemplateResult | undefined;

        if (panelConfig && panelConfig.type === "property") {
            // Special handling for info panel which only shows properties
            let properties = OrAssetViewer.getInfoProperties(panelConfig);

            if (properties.length === 0) {
                return;
            }

            content = html`
                ${properties.map((prop) => {
                let style = styles ? styles[prop!] : undefined;
                return prop === "attributes" ? `` : OrAssetViewer.getField(prop, true, style, OrAssetViewer.getPropertyTemplate(prop, (asset as {[index: string]:any})[prop], viewerConfig, panelConfig, shadowRoot));
            })}
            `;
        } else if (panelConfig && panelConfig.type === "history") {
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

                content = html`
                    <style>
                        #history-container {
                            height: 100%;
                            width: 100%;
                            display: flex;
                            flex-direction: column;
                        }
                        
                        #history-controls {
                            margin-bottom: 10px;
                        }
                        
                        #history-attribute-picker {
                            flex: 0;
                        }
                        
                        or-attribute-history {
                            height: 100%;
                        }
                    </style>
                    <div id="history-container">
                        <div id="history-controls">
                            <or-input id="history-attribute-picker" @or-input-changed="${(evt: OrInputChangedEvent) => attributeChanged(evt.detail.value)}" .type="${InputType.SELECT}" .options="${historyAttrs.map((attr) => [attr.name, getAttributeLabel(attr, undefined)])}"></or-input>
                        </div>        
                        <or-attribute-history id="attribute-history" .config="${viewerConfig.historyConfig}" .assetType="${asset.type}"></or-attribute-history>
                    </div>
                `;
            }
        } else if (panelConfig && panelConfig.type === "location") {
            const attribute = attrs.find((attr) => attr.name === AttributeType.LOCATION.attributeName);
            if (attribute) {
                // Special handling for location panel which shows an attribute selector and a map showing the location of the attribute
                const mapType = viewerConfig.mapType || OrAssetViewer.DEFAULT_MAP_TYPE;
                const lngLat = MapUtil.getLngLat(attribute);
                const center = lngLat ? lngLat.toArray() : undefined;
                const showOnMapMeta = Util.getFirstMetaItem(attribute, MetaItemType.SHOW_ON_DASHBOARD.urn!);

                content = html`
                    <style>
                        #location-container {
                            height: 100%;
                            width: 100%;
                            display: flex;
                            flex-direction: column;
                            padding-top: 10px;
                        }
                        #location-container > or-map {
                            flex: 1;
                            border: #e5e5e5 1px solid;
                        }
                        #location-map-input {
                            flex: 0 0 auto;
                            padding: 20px 0 0 0;
                        }
                    </style>
                    <div id="location-container">
                        <or-map id="location-map" class="or-map" .center="${center}" type="${mapType}">
                             <or-map-marker-asset active .asset="${asset}"></or-map-marker-asset>
                        </or-map>
                        ${attribute.name === AttributeType.LOCATION.attributeName ? html`
                            <or-input id="location-map-input" type="${InputType.SWITCH}" readonly dense .value="${showOnMapMeta ? showOnMapMeta.value : undefined}" label="${i18next.t("showOnMap")}"></or-input>
                        ` : ``}                    
                    </div>
                `;
            }
        } else {
            content = html`
                ${attrs.sort((attr1, attr2) => attr1.name! < attr2.name! ? -1 : attr1.name! > attr2.name! ? 1 : 0).map((attr) => {
                    let style = styles ? styles[attr.name!] : undefined;
                    return this.getField(attr.name!, false, style, OrAssetViewer.getAttributeTemplate(asset, attr, viewerConfig, panelConfig));
                })}
            `;
        }

        return content;
    }

    public static getPropertyTemplate(property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig, shadowRoot: ShadowRoot | null) {
        let type = InputType.TEXT;
        let minLength: number | undefined;
        let maxLength: number | undefined;

        if (viewerConfig.propertyViewProvider) {
            const result = viewerConfig.propertyViewProvider(property, value, viewerConfig, panelConfig);
            if (result) {
                return result;
            }
        }

        switch(property) {
            case "path":
                if (!value || !(Array.isArray(value))) {
                    return;
                }

                // Populate value when we get the response
                OrAssetViewer.getAssetNames(value as string[]).then(
                    (names) => {
                        if (shadowRoot) {
                            const pathField = shadowRoot.getElementById("property-path") as OrInput;
                            if (pathField) {
                                pathField.value = names.reverse().join(" > ");
                            }
                        }
                    }
                );
                value = i18next.t("loading");
                break;
            case "createdOn":
                type = InputType.DATETIME;
                break;
            case "accessPublicRead":
                type = InputType.CHECKBOX;
                break;
            case "name":
                minLength = 1;
                maxLength = 1023;
                break;
        }

        return html`<or-input id="property-${property}" type="${type}" .minLength="${minLength}" .maxLength="${maxLength}" dense .value="${value}" readonly label="${i18next.t(property)}"></or-input>`;
    }

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
                            Object.assign(config.panels[name], {...assetPanelConfig});
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

    public static async getAssetNames(ids: string[]): Promise<string[]> {
        const response = await manager.rest.api.AssetResource.queryAssets({
            select: {
                excludePath: true,
                excludeParentInfo: true,
                excludeRealm: true
            },
            ids: ids
        });

        if (response.status !== 200 || !response.data || response.data.length !== ids.length) {
            return ids;
        }

        return ids.map((id) => response.data.find((asset) => asset.id === id)!.name!);
    }
}
