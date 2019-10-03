import {customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import "@openremote/or-select";
import "@openremote/or-icon";
import "@openremote/or-input";
import "@openremote/or-attribute-input";
import "@openremote/or-attribute-history";
import {getAttributeLabel} from "@openremote/or-attribute-input";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate/dist/translate-mixin";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";
import {
    Asset,
    AssetAttribute,
    AssetEvent,
    Attribute,
    AttributeEvent,
    AttributeType,
    MetaItemType,
    AttributeValueType
} from "@openremote/model";
import {style} from "./style";
import "@openremote/or-panel";
import openremote from "@openremote/core";
import rest from "@openremote/rest";
import {subscribe} from "@openremote/core/dist/asset-mixin";
import {getAssetAttribute, getAssetAttributes, getFirstMetaItem} from "@openremote/core/dist/util";
import i18next from "i18next";
import {styleMap} from "lit-html/directives/style-map";
import "@openremote/or-map";
import {Type as MapType} from "@openremote/or-map";
import {getLngLat} from "@openremote/or-map/dist/util";
import {OrAttributeHistory, HistoryConfig} from "@openremote/or-attribute-history";

export type PanelType = "property" | "location" | "attribute" | "history";

export interface PanelConfig {
    type?: PanelType;
    scrollable?: boolean;
    hide?: boolean;
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: { [style: string]: string };
    fieldStyles?: { [field: string]: { [style: string]: string } };
}

export interface AssetViewerConfig {
    panels: {[name: string] : PanelConfig};
    viewerStyles?: { [style: string]: string };
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

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(openremote)(translate(i18next)(LitElement)) {

    public static DEFAULT_MAP_TYPE = MapType.VECTOR;
    public static DEFAULT_PANEL_TYPE: PanelType = "attribute";

    public static DEFAULT_CONFIG: AssetViewerConfig = {
        viewerStyles: {
            gridTemplateColumns: "repeat(12, 1fr)",
            gridTemplateRows: "auto minmax(0, 1fr) minmax(0, 50%)"
        },
        panels: {
            "info": {
                type: "property",
                panelStyles: {
                    gridColumnStart: "1",
                    gridColumnEnd: "7",
                    gridRowStart: "1",
                    gridRowEnd: "2",
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
                    gridRowStart: "1",
                    gridRowEnd: "3",
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
                    gridRowStart: "2",
                    gridRowEnd: "4"
                }
            },
            "history": {
                type: "history",
                panelStyles: {
                    gridColumnStart: "7",
                    gridColumnEnd: "13",
                    gridRowStart: "3",
                    gridRowEnd: "4",
                },
                scrollable: false,
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

    @query("#property-path")
    protected _pathFieldInput!: OrInput;

    @property()
    protected _loading: boolean = false;

    @query("#history-attribute-picker")
    protected _historyAttributePicker?: OrInput;
    @query("#attribute-history")
    protected _attributeHistory?: OrAttributeHistory;

    protected render() {

        if (this._loading) {
            return html`
                <div id="loading"><or-translate value="loading"></or-translate></div>
            `;
        }

        if (!this.asset && !this.assetId) {
            return html`
                <div id="noneselected"><or-translate value="noAssetSelected"></or-translate></div>
            `;
        }

        if (!this.asset) {
            return html`
                <div><or-translate value="notFound"></or-translate></div>
            `;
        }

        const viewerConfig = this._getPanelConfig(this.asset!);
        const attributes = getAssetAttributes(this.asset);

        return html`
            <div id="container" style="${viewerConfig.viewerStyles ? styleMap(viewerConfig.viewerStyles) : ""}">
            ${html`${Object.entries(viewerConfig.panels).map(([name, panelConfig]) => {
                const panelTemplate = this._getPanel(name, panelConfig, this._getPanelContent(attributes, name, viewerConfig, panelConfig));
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

    protected _getInfoProperties(config?: PanelConfig): string[] {
        let properties = config && config.include ? config.include : OrAssetViewer.DEFAULT_INFO_PROPERTIES;

        if (config && config.exclude) {
            properties = properties.filter((p) => !config.exclude!.find((excluded) => excluded === p))
        }

        return properties;
    }

    protected _getPanel(name: string, config: PanelConfig | undefined, content: TemplateResult | undefined) {
        if (!content) {
            return;
        }

        return html`
            <div class="panel" id="${name}-panel" style="${config && config.panelStyles ? styleMap(config.panelStyles) : ""}">
                ${content}
            </div>
        `;
    }

    protected _getPanelContent(attributes: AssetAttribute[], panelName: string, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig): TemplateResult | undefined {
        if (panelConfig.hide || attributes.length === 0) {
            return;
        }

        if (this.config && this.config.panelViewProvider) {
            const template = this.config.panelViewProvider(attributes, panelName, viewerConfig, panelConfig);
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

        let innerContent: TemplateResult | undefined;

        if (panelConfig && panelConfig.type === "property") {
            // Special handling for info panel which only shows properties
            let properties = this._getInfoProperties(panelConfig);

            if (properties.length === 0) {
                return;
            }

            innerContent = html`
                ${properties.map((prop) => {
                let style = styles ? styles[prop!] : undefined;
                return prop === "attributes" ? `` : this._getField(prop, true, style, this._getPropertyTemplate(prop, (this.asset! as {[index: string]:any})[prop], viewerConfig, panelConfig));
            })}
            `;
        } else if (panelConfig && panelConfig.type === "history") {
            // Special handling for history panel which shows an attribute selector and a graph/data table of historical values
            const historyAttrs = attrs.filter((attr) => getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!));
            if (historyAttrs.length > 0) {
                innerContent = html`
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
                            <or-input id="history-attribute-picker" @or-input-changed="${(evt: OrInputChangedEvent) => this._historyAttributeChanged(evt.detail.value)}" .type="${InputType.SELECT}" .options="${historyAttrs.map((attr) => [attr.name, getAttributeLabel(attr)])}"></or-input>
                        </div>        
                        <or-attribute-history id="attribute-history" .config="${this.config ? this.config.historyConfig : undefined}" .assetType="${this.asset!.type}"></or-attribute-history>
                    </div>
                `;
            }
        } else if (panelConfig && panelConfig.type === "location") {
            const attribute = attrs.find((attr) => attr.name === AttributeType.LOCATION.attributeName);
            if (attribute) {
                // Special handling for location panel which shows an attribute selector and a map showing the location of the attribute
                const mapType = this.config && this.config.mapType ? this.config.mapType : OrAssetViewer.DEFAULT_MAP_TYPE;
                const lngLat = getLngLat(attribute);
                const center = lngLat ? lngLat.toArray() : undefined;
                const showOnMapMeta = getFirstMetaItem(attribute, MetaItemType.SHOW_ON_DASHBOARD.urn!);

                return html`
                    <style>
                        #location-container {
                            height: 100%;
                            width: 100%;
                            display: flex;
                            flex-direction: column;
                        }
                        #location-container > or-map {
                            flex: 1;
                            border: #dbdbdb 1px solid;
                        }
                        #location-map-input {
                            flex: 0 0 auto;
                            padding: 20px 0 0 0;
                        }
                    </style>
                    <div id="location-container">
                        <or-map id="location-map" class="or-map" .center="${center}" type="${mapType}">
                             <or-map-marker-asset active .asset="${this.asset}"></or-map-marker-asset>
                        </or-map>
                        ${attribute.name === AttributeType.LOCATION.attributeName ? html`
                            <or-input id="location-map-input" type="${InputType.SWITCH}" readonly dense .value="${showOnMapMeta ? showOnMapMeta.value : undefined}" label="${i18next.t("showOnMap")}"></or-input>
                        ` : ``}                    
                    </div>
                `;
            }
        } else {
            innerContent = html`
                ${attrs.sort((attr1, attr2) => attr1.name! < attr2.name! ? -1 : attr1.name! > attr2.name! ? 1 : 0).map((attr) => {
                    let style = styles ? styles[attr.name!] : undefined;
                    return this._getField(attr.name!, false, style, this._getAttributeTemplate(attr, viewerConfig, panelConfig));
                })}
            `;
        }

        if (!innerContent) {
            return;
        }

        return html`
            <div class="panel-title">
                <or-translate value="${name}"></or-translate>
            </div>
            ${!panelConfig || panelConfig.scrollable === undefined || panelConfig.scrollable ? html`
                <or-panel class="panel-content-wrapper">
                    <div class="panel-content">
                        ${innerContent}
                    </div>
                </or-panel>
            `: html`
                <div class="panel-content-wrapper">
                    <div class="panel-content">
                        ${innerContent}
                    </div>
                </div>
            `}
        `;
    }

    protected _historyAttributeChanged(attributeName: string | undefined) {
        if (this._historyAttributePicker && this._attributeHistory) {

            let attribute: AssetAttribute | undefined;

            if (attributeName) {
                attribute = getAssetAttribute(this.asset!, attributeName);
            }

            this._attributeHistory.attribute = attribute;
        }
    }

    protected _getPropertyTemplate(property: string, value: any, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) {
        let type = InputType.TEXT;
        let minLength: number | undefined;
        let maxLength: number | undefined;

        if (this.config && this.config.propertyViewProvider) {
            const result = this.config.propertyViewProvider(property, value, viewerConfig, panelConfig);
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
                this._getAssetNames(value as string[]).then(
                    (names) => {
                        if (this._pathFieldInput) {
                            this._pathFieldInput.value = names.reverse().join(" > ");
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

    protected _getAttributeTemplate(attribute: AssetAttribute, viewerConfig: AssetViewerConfig, panelConfig: PanelConfig) {
        if (this.config && this.config.attributeViewProvider) {
            const result = this.config.attributeViewProvider(attribute, viewerConfig, panelConfig);
            if (result) {
                return result;
            }
        }

        // Default panel type
        const panelType = panelConfig ? panelConfig.type : OrAssetViewer.DEFAULT_PANEL_TYPE;

        return html`
            <or-attribute-input dense .assetType="${this.asset!.type}" .attribute="${attribute}" .label="${i18next.t(attribute.name!)}"></or-attribute-input>
        `;
    }

    protected _getField(name: string, isProperty: boolean, styles: { [style: string]: string } | undefined, content: TemplateResult | undefined) {
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
    public onAttributeEvent(event: AttributeEvent) {
        const attrName = event.attributeState!.attributeRef!.attributeName!;

        if (this.asset && this.asset.attributes && this.asset.attributes.hasOwnProperty(attrName)) {
            if (event.attributeState!.deleted) {
                delete this.asset.attributes[attrName];
            } else {
                const attr = this.asset.attributes[attrName]! as Attribute;
                attr.valueTimestamp = event.timestamp;
                attr.value = event.attributeState!.value;
                this.asset.attributes[attrName] = {...attr};
            }
            this.asset = {...this.asset}
        }
    }

    public onAssetEvent(event: AssetEvent) {
        this.asset = event.asset;
        this._loading = false;
    }

    protected _getPanelConfig(asset: Asset): AssetViewerConfig {
        let config = {...OrAssetViewer.DEFAULT_CONFIG};
        if (this.config) {
            if (!config.viewerStyles) {
                config.viewerStyles = {};
            }
            if (!config.panels) {
                config.panels = {};
            }
            if (this.config.assetTypes && this.config.assetTypes.hasOwnProperty(asset.type!)) {
                const assetConfig = this.config.assetTypes[asset.type!];
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
            }
        }

        return config;
    }

    protected async _getAssetNames(ids: string[]): Promise<string[]> {
        const response = await rest.api.AssetResource.queryAssets({
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
