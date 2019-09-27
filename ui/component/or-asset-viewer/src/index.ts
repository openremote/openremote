import {customElement, html, LitElement, property, PropertyValues, TemplateResult, query} from "lit-element";
import "@openremote/or-select";
import "@openremote/or-icon";
import "@openremote/or-input";
import "@openremote/or-attribute-input";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate/dist/translate-mixin";
import {InputType, OrInput} from "@openremote/or-input";
import {Asset, AttributeEvent, AssetEvent, Attribute, AssetAttribute, AttributeType, MetaItemType} from "@openremote/model";
import {style} from "./style";
import "./components/or-panel";
import openremote from "@openremote/core";
import rest from "@openremote/rest";
import {subscribe} from "@openremote/core/dist/asset-mixin";
import {getAssetAttributes, getFirstMetaItem} from "@openremote/core/dist/util";
import i18next from "i18next";
import {styleMap} from "lit-html/directives/style-map";
import "@openremote/or-map";
import {Type as MapType} from "@openremote/or-map";
import {getLngLat} from "@openremote/or-map/dist/util";

export interface PanelConfig {
    scrollable?: boolean;
    hide?: boolean;
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: { [style: string]: string };
    fieldStyles?: { [field: string]: { [style: string]: string } };
}

export interface ViewerConfig {
    default?: {[name: string]: PanelConfig};
    assetTypes?: { [assetType: string]: {[name: string]: PanelConfig} };
    propertyViewProvider?: (property: string, value: any) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute) => TemplateResult | undefined;
    assetNameProvider?: (id: string) => string;
    mapType?: MapType;
}

@customElement("or-asset-viewer")
export class OrAssetViewer extends subscribe(openremote)(translate(i18next)(LitElement)) {

    public static DEFAULT_CONFIG: {[name: string]: PanelConfig} = {
        "info": {
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
            scrollable: false,
            include: ["location"],
            fieldStyles: {
                location: {
                    height: "100%",
                    margin: "0"
                }
            }
        },
        "attributes": {

        },
        "history": {
            scrollable: false,
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

    protected render() {

        if (this._loading) {
            return html`
                <div>LOADING</div>
            `;
        }

        if (!this.asset && !this.assetId) {
            return html``;
        }

        if (!this.asset) {
            return html`
                <div><or-translate value="notFound"></or-translate></div>
            `;
        }

        const panelConfigs = this._getPanelConfig(this.asset!);
        const attributes = getAssetAttributes(this.asset);

        return html`
            <div id="container">
            ${html`${Object.entries(panelConfigs).map(([name, config]) => {
                const panelTemplate = this._getPanel(name, config, this._getPanelContent(name, attributes, config));
                return panelTemplate || ``;
            })}`
            }`;
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
            
                <div class="panel-title">
                    <or-translate value="${name}"></or-translate>
                </div>
                ${!config || config.scrollable === undefined || config.scrollable ? html`
                    <or-panel class="panel-content-wrapper">
                        <div class="panel-content">
                            ${content}
                        </div>
                    </or-panel>
                `: html`
                    <div class="panel-content">
                        ${content}
                    </div>                    
                `}
            </div>
        `;
    }

    protected _getPanelContent(panelName: string, attributes: AssetAttribute[], config: PanelConfig): TemplateResult | undefined {
        if (config.hide || attributes.length === 0) {
            return;
        }

        let styles = config ? config.fieldStyles : undefined;

        // Special handling for info panel which only shows properties
        if (panelName === "info") {
            let properties = this._getInfoProperties(config);

            if (properties.length === 0) {
                return;
            }

            return html`
                ${properties.map((prop) => {
                let style = styles ? styles[prop!] : undefined;
                return prop === "attributes" ? `` : this._getField(prop, true, style, this._getPropertyTemplate(prop, (this.asset! as {[index: string]:any})[prop]));
            })}
            `;
        }

        // Special handling for history panel
        if (panelName === "history") {
            return html``;
        }

        const includedAttributes = config && config.include ? config.include : undefined;
        const excludedAttributes = config && config.exclude ? config.exclude : [];
        const attrs = attributes.filter((attr) =>
            (!includedAttributes || includedAttributes.indexOf(attr.name!) >= 0)
            && (!excludedAttributes || excludedAttributes.indexOf(attr.name!) < 0));

        return html`
            ${attrs.sort((attr1, attr2) => attr1.name! < attr2.name! ? -1 : attr1.name! > attr2.name! ? 1 : 0).map((attr) => {
            let style = styles ? styles[attr.name!] : undefined;
            return this._getField(attr.name!, false, style, this._getAttributeTemplate(panelName, attr));
        })}
        `;
    }

    protected _getPropertyTemplate(property: string, value: any) {
        let type = InputType.TEXT;
        let minLength: number | undefined;
        let maxLength: number | undefined;

        if (this.config && this.config.propertyViewProvider) {
            const result = this.config.propertyViewProvider(property, value);
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

    protected _getAttributeTemplate(panel: string, attribute: AssetAttribute) {
        if (this.config && this.config.attributeViewProvider) {
            const result = this.config.attributeViewProvider(attribute);
            if (result) {
                return result;
            }
        }

        // Special control for main asset location attribute when shown in the location panel
        if (panel === "location" && attribute.name === AttributeType.LOCATION.attributeName) {

            const mapType = this.config && this.config.mapType ? this.config.mapType : MapType.VECTOR;
            const lngLat = getLngLat(attribute);
            const center = lngLat ? lngLat.toArray() : undefined;
            const showOnMapMeta = getFirstMetaItem(attribute, MetaItemType.SHOW_ON_DASHBOARD.urn!);

            return html`
                <style>
                    #location-map-container {
                        height: 100%;
                        width: 100%;
                        display: flex;
                        flex-direction: column;
                    }
                    #location-map-container > or-map {
                        flex: 1;
                    }
                    #location-map-input {
                        flex: 0 0 auto;
                        padding: 20px 0 0 0;
                    }
                </style>
                <div id="location-map-container">
                    <or-map id="location-map" class="or-map" .center="${center}" type="${mapType}">
                         <or-map-marker-asset active .asset="${this.asset}"></or-map-marker-asset>
                    </or-map>
                    <or-input id="location-map-input" type="${InputType.SWITCH}" readonly dense .value="${showOnMapMeta ? showOnMapMeta.value : undefined}" label="${i18next.t("showOnMap")}"></or-input>                    
                </div>
            `;
        }

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

    protected _getPanelConfig(asset: Asset) {
        let config = this.config && this.config.default ? Object.assign({...OrAssetViewer.DEFAULT_CONFIG}, this.config.default) : {...OrAssetViewer.DEFAULT_CONFIG};

        if (this.config) {
            if (this.config.assetTypes && this.config.assetTypes.hasOwnProperty(asset.type!)) {
                config =  Object.assign(config, this.config.assetTypes[asset.type!]);
            }
        }

        return config;
    }

    protected async _getAssetNames(ids: string[]): Promise<string[]> {
        if (this.config && this.config.assetNameProvider) {
            return ids.map((id) => this.config!.assetNameProvider!(id));
        }

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
