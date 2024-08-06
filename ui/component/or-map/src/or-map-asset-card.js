var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { classMap } from 'lit-html/directives/class-map.js';
import { AssetModelUtil } from "@openremote/model";
import manager, { subscribe, Util } from "@openremote/core";
import "@openremote/or-icon";
import { mapAssetCardStyle } from "./style";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { getMarkerConfigAttributeName } from "./markers/or-map-marker-asset";
export class OrMapAssetCardLoadAssetEvent extends CustomEvent {
    constructor(assetId) {
        super(OrMapAssetCardLoadAssetEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetId
        });
    }
}
OrMapAssetCardLoadAssetEvent.NAME = "or-map-asset-card-load-asset";
export const DefaultConfig = {
    default: {
        exclude: ["notes"]
    },
    assetTypes: {}
};
let OrMapAssetCard = class OrMapAssetCard extends subscribe(manager)(LitElement) {
    constructor() {
        super(...arguments);
        this.useAssetColor = true;
    }
    static get styles() {
        return mapAssetCardStyle;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("assetId")) {
            this.title = "";
            this.assetIds = this.assetId && this.assetId.length > 0 ? [this.assetId] : undefined;
            if (_changedProperties.size === 1) {
                return false;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }
    _onEvent(event) {
        if (event.eventType === "asset") {
            const assetEvent = event;
            switch (assetEvent.cause) {
                case "READ" /* AssetEventCause.READ */:
                case "CREATE" /* AssetEventCause.CREATE */:
                case "UPDATE" /* AssetEventCause.UPDATE */:
                    this.asset = assetEvent.asset;
                    break;
                case "DELETE" /* AssetEventCause.DELETE */:
                    this.asset = undefined;
                    break;
            }
        }
        if (event.eventType === "attribute") {
            if (this.asset) {
                this.asset = Util.updateAsset(this.asset, event);
                this.requestUpdate();
            }
        }
    }
    getCardConfig() {
        let cardConfig = this.config || DefaultConfig;
        if (!this.asset) {
            return cardConfig.default;
        }
        return cardConfig.assetTypes && cardConfig.assetTypes.hasOwnProperty(this.asset.type) ? cardConfig.assetTypes[this.asset.type] : cardConfig.default;
    }
    render() {
        if (!this.asset) {
            return html ``;
        }
        const icon = this.getIcon();
        const color = this.getColor();
        const styleStr = color ? "--internal-or-map-asset-card-header-color: #" + color + ";" : "";
        const cardConfig = this.getCardConfig();
        const attributes = Object.values(this.asset.attributes).filter((attr) => attr.name !== "location" /* WellknownAttributes.LOCATION */);
        const includedAttributes = cardConfig && cardConfig.include ? cardConfig.include : undefined;
        const excludedAttributes = cardConfig && cardConfig.exclude ? cardConfig.exclude : [];
        const attrs = attributes.filter((attr) => (!includedAttributes || includedAttributes.indexOf(attr.name) >= 0)
            && (!excludedAttributes || excludedAttributes.indexOf(attr.name) < 0)
            && (!attr.meta || !attr.meta.hasOwnProperty("showOnDashboard" /* WellknownMetaItems.SHOWONDASHBOARD */) || !!Util.getMetaValue("showOnDashboard" /* WellknownMetaItems.SHOWONDASHBOARD */, attr)))
            .sort(Util.sortByString((listItem) => listItem.name));
        const highlightedAttr = getMarkerConfigAttributeName(this.markerconfig, this.asset.type);
        return html `
            <div id="card-container" style="${styleStr}">
                <div id="header">
                    ${icon ? html `<or-icon icon="${icon}"></or-icon>` : ``}
                    <span id="title">${this.asset.name}</span>
                </div>
                <div id="attribute-list">
                    <ul>
                        ${attrs.map((attr) => {
            if (!this.asset || !this.asset.type) {
                return;
            }
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.asset.type, attr.name, attr);
            if (descriptors && descriptors.length) {
                const label = Util.getAttributeLabel(attr, descriptors[0], this.asset.type, true);
                const value = Util.getAttributeValueAsString(attr, descriptors[0], this.asset.type, false, "-");
                const classes = { highlighted: highlightedAttr === attr.name };
                return html `<li class="${classMap(classes)}"><span class="attribute-name">${label}</span><span class="attribute-value">${value}</span></li>`;
            }
        })}
                    </ul>
                </div>
                ${cardConfig && cardConfig.hideViewAsset ? html `` : html `
                    <div id="footer">
                        <or-mwc-input .type="${InputType.BUTTON}" label="viewAsset" @or-mwc-input-changed="${(e) => { e.preventDefault(); this._loadAsset(this.asset.id); }}"></or-mwc-input>
                    </div>
                `}
            </div>
        `;
    }
    _loadAsset(assetId) {
        this.dispatchEvent(new OrMapAssetCardLoadAssetEvent(assetId));
    }
    getIcon() {
        var _a;
        if (this.asset) {
            const descriptor = AssetModelUtil.getAssetDescriptor(this.asset.type);
            const icon = (_a = getMarkerIconAndColorFromAssetType(descriptor)) === null || _a === void 0 ? void 0 : _a.icon;
            return icon ? icon : undefined;
        }
    }
    getColor() {
        var _a;
        if (this.asset) {
            const descriptor = AssetModelUtil.getAssetDescriptor(this.asset.type);
            const color = (_a = getMarkerIconAndColorFromAssetType(descriptor)) === null || _a === void 0 ? void 0 : _a.color;
            if (color) {
                // check if range
                return (typeof color === 'string') ? color : color[0].colour;
            }
        }
    }
};
__decorate([
    property({ type: String, reflect: true, attribute: true })
], OrMapAssetCard.prototype, "assetId", void 0);
__decorate([
    property({ type: Object, attribute: true })
], OrMapAssetCard.prototype, "asset", void 0);
__decorate([
    property({ type: Object })
], OrMapAssetCard.prototype, "config", void 0);
__decorate([
    property({ type: Object })
], OrMapAssetCard.prototype, "markerconfig", void 0);
__decorate([
    property({ type: Boolean, attribute: true })
], OrMapAssetCard.prototype, "useAssetColor", void 0);
OrMapAssetCard = __decorate([
    customElement("or-map-asset-card")
], OrMapAssetCard);
export { OrMapAssetCard };
//# sourceMappingURL=or-map-asset-card.js.map