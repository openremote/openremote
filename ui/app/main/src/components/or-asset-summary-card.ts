import {
    css,
    CSSResult,
    CSSResultArray,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import {Asset, AssetEvent, AssetEventCause, AttributeEvent, AttributeType, AttributeValueType, MetaItemType, AttributeDescriptor} from "@openremote/model";
import manager, {
    AssetModelUtil,
    DefaultColor1,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    DefaultHeaderHeight,
    subscribe,
    Util
} from "@openremote/core";
import "@openremote/or-icon";
import {router} from "../index";
import "./or-attribute-field/src";
import {orAttributeTemplateProvider} from "./or-attribute-field/src";
import { i18next } from "@openremote/or-translate";

orAttributeTemplateProvider.setTemplate((attribute) => {
    let template;
    switch (attribute.type) {

        case AttributeValueType.BOOLEAN.name:
            template = html`<or-translate value="${attribute.value}"></or-translate>`;
            break;
        case AttributeValueType.SWITCH_TOGGLE.name:
            template = html`<or-translate value="${attribute.value ? "On" : "Off"}"></or-translate>`;
            break;
        case AttributeValueType.NUMBER.name:
        case AttributeValueType.SOUND.name:
            template = attribute.value ? new Intl.NumberFormat().format(attribute.value) : "-";
            break;
        case AttributeValueType.STRING.name:
            template = html`<or-translate value="${attribute.value}"></or-translate>`;
            break;
        case AttributeValueType.OBJECT.name:
        case AttributeValueType.ARRAY.name:
            if(attribute.name === "status") {
                template = html`<or-translate value="${attribute.value.result}"></or-translate>`;
            }
            else if(attribute.value && attribute.value.content.time) {
                template = new Intl.DateTimeFormat('default', {
                    year: 'numeric',
                    month: 'numeric',
                    day: 'numeric',
                    hour: 'numeric',
                    minute: 'numeric'
                }).format(new Date(attribute.value.content.time));
            }
            break;
        default:
            template = attribute.value ? attribute.value : "-";
            break;
    }
    return template;
});

@customElement("or-asset-summary-card")
export class OrAssetSummaryCard extends subscribe(manager)(LitElement) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public asset?: Asset;

    @property({type: Boolean, attribute: true})
    public useAssetColor: boolean = true;

    static get styles(): CSSResultArray | CSSResult {
        return css`
            :host {
                --internal-or-asset-summary-card-header-color: var(--or-asset-summary-card-header-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
                --internal-or-asset-summary-card-header-text-color: var(--or-asset-summary-card-header-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
                --internal-or-asset-summary-card-header-height: var(--or-asset-summary-card-header-height, calc(${unsafeCSS(DefaultHeaderHeight)} - 10px));
                --internal-or-asset-summary-card-background-color: var(--or-asset-summary-card-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
                --internal-or-asset-summary-card-background-text-color: var(--or-asset-summary-card-background-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
                --internal-or-asset-summary-card-separator-color: var(--or-asset-summary-card-separator-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
                
                display: block;
            }
            
            #card-container {
                display: flex;
                flex-direction: column;
                height: 100%;
                background-color: var(--internal-or-asset-summary-card-background-color);
                -webkit-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                -moz-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);  
            }
            
            #header {
                height: var(--internal-or-asset-summary-card-header-height);
                background-color: var(--internal-or-asset-summary-card-header-color);
                line-height: var(--internal-or-asset-summary-card-header-height);
                border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};
                text-align: center;
                color: var(--internal-or-asset-summary-card-header-text-color);
                --or-icon-fill: var(--internal-or-asset-summary-card-header-text-color);
                --or-icon-width: 20px;
                --or-icon-height: 20px;
                z-index: 99999;
            }

            #header > or-icon {
                margin-right: 5px;
            }
            
            #title {
                font-weight: 500;
            }
            
            #attribute-list {
                flex: 1;                
                color: var(--internal-or-asset-summary-card-background-text-color);
                padding: 10px 20px;
                overflow: auto;
                font-size: 14px;
            }
            
            ul {
                list-style-type: none;
                margin: 0;
                padding: 0;
            }
            
            li {
                display: flex;
                line-height: 30px;
                height: 32px;
            }
            
            .attribute-name {
                flex: 1;            
            }
            
            .attribute-value {
                overflow: hidden;
                padding-left: 20px;
                text-align: right;
            }
            
            #footer {
                height: var(--internal-or-asset-summary-card-header-height);
                border-top: 1px solid var(--internal-or-asset-summary-card-separator-color);
                text-align: right;
            }
            
            #footer > span {
                line-height: var(--internal-or-asset-summary-card-header-height);
                font-weight: 500;
                font-size: 14px;
                margin-right: 20px;
                cursor: pointer;
            }

            @media only screen and (min-width: 415px){
                #card-container {
                    height: 400px; /* fallback for IE */
                    height: max-content;
                    max-height: calc(100vh - 150px);
                    min-height: 134px;
                }
            }
        `;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("assetId")) {
            this.title = undefined;
            this.assetIds = this.assetId && this.assetId.length > 0 ? [this.assetId] : undefined;

            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }

        return super.shouldUpdate(_changedProperties);
    }

    public onAttributeEvent(event: AttributeEvent) {

        if (this.asset) {
            this.asset = Util.updateAsset(this.asset, event);
            this.requestUpdate();
        }
    }

    public onAssetEvent(event: AssetEvent) {
        switch (event.cause) {
            case AssetEventCause.READ:
            case AssetEventCause.CREATE:
            case AssetEventCause.UPDATE:
                this.asset = event.asset;
                break;
            case AssetEventCause.DELETE:
                this.asset = null;
                break;
        }
    }

    protected render(): TemplateResult | undefined {

        if (!this.asset) {
            return html``;
        }

        const icon = this.getIcon();
        const color = this.getColor();
        const styleStr = color ? "--internal-or-asset-summary-card-header-color: #" + color + ";" : "";



        return html`
            <div id="card-container" style="${styleStr}">
                <div id="header">
                    ${icon ? html`<or-icon icon="${icon}"></or-icon>` : ``}
                    <span id="title">${this.asset.name}</span>
                </div>
                <div id="attribute-list">
                    <ul>
                        ${Util.getAssetAttributes(this.asset).filter((attr) => attr.name !== AttributeType.LOCATION.attributeName).map((attr) => {
                            let attributeDescriptor: AttributeDescriptor | undefined = AssetModelUtil.getAttributeDescriptorFromAsset(attr.name!);
                            let label = Util.getAttributeLabel(attr, attributeDescriptor);
                            const unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, attr, attributeDescriptor);
                            
                            if(unit) 
                                label = label + " ("+i18next.t(unit)+")";
                            return html`<li>${label}<span class="attribute-value"><or-attribute-field .attribute="${attr}"></or-attribute-field></span></li>`; 
                        })}
                    </ul>
                </div>
                <div id="footer">
                    <span @click="${() => router.navigate('assets/'+this.asset.id)}">Asset details</span>          
                </div>
            </div>        
        `;
    }

    protected getIcon(): string | undefined {
        if (this.asset) {
            const descriptor = AssetModelUtil.getAssetDescriptor(this.asset.type);
            return descriptor.icon;
        }
    }

    protected getColor(): string | undefined {
        if (this.asset) {
            const descriptor = AssetModelUtil.getAssetDescriptor(this.asset.type);
            return descriptor.color;
        }
    }
}
