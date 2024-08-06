var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { customElement, property } from "lit/decorators.js";
import { OrMapMarker } from "./or-map-marker";
import { AssetModelUtil } from "@openremote/model";
import manager, { subscribe, Util } from "@openremote/core";
import { getMarkerIconAndColorFromAssetType } from "../util";
export function getMarkerConfigForAssetType(config, assetType) {
    if (!config || !assetType || !config[assetType]) {
        return;
    }
    return config[assetType];
}
export function getMarkerConfigAttributeName(config, assetType) {
    const assetTypeConfig = getMarkerConfigForAssetType(config, assetType);
    if (!assetTypeConfig) {
        return;
    }
    return assetTypeConfig.attributeName;
}
let OrMapMarkerAsset = class OrMapMarkerAsset extends subscribe(manager)(OrMapMarker) {
    constructor() {
        super();
        this.assetTypeAsIcon = true;
        this.visible = false;
    }
    set type(type) {
        let overrideOpts;
        const assetTypeConfig = getMarkerConfigForAssetType(this.config, type);
        if (assetTypeConfig && assetTypeConfig.attributeName && this.asset && this.asset.attributes && this.asset.attributes[assetTypeConfig.attributeName] && assetTypeConfig.colours) {
            const currentValue = this.asset.attributes[assetTypeConfig.attributeName].value;
            overrideOpts = {
                markerConfig: assetTypeConfig.colours,
                currentValue: currentValue
            };
        }
        const iconAndColour = getMarkerIconAndColorFromAssetType(type, overrideOpts);
        if (!iconAndColour) {
            this.visible = false;
            return;
        }
        if (this.assetTypeAsIcon) {
            this.icon = iconAndColour.icon;
        }
        this.markerColor = (Array.isArray(iconAndColour.color)) ? iconAndColour.color[0].colour : iconAndColour.color || undefined;
        this.updateColor(this.markerContainer);
        this.visible = true;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("assetId")) {
            this.lat = undefined;
            this.lng = undefined;
            this.type = undefined;
            this.direction = undefined;
            this.displayValue = undefined;
            this.assetIds = this.assetId && this.assetId.length > 0 ? [this.assetId] : undefined;
            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }
        if (_changedProperties.has("asset")) {
            try {
                this.onAssetChanged(this.asset);
            }
            catch (e) {
                console.error(e);
            }
        }
        return super.shouldUpdate(_changedProperties);
    }
    /**
     * This will only get called when assetId is set; if asset is set then it is expected that attribute changes are
     * handled outside this component and the asset should be replaced when attributes change that require the marker
     * to re-render
     */
    _onEvent(event) {
        if (event.eventType === "attribute") {
            const attributeEvent = event;
            if (attributeEvent.ref.name === "location" /* WellknownAttributes.LOCATION */) {
                this._updateLocation(attributeEvent.value);
                return;
            }
            if (this.asset) {
                this.asset = Util.updateAsset(this.asset, event);
                this.requestUpdate();
            }
            return;
        }
        if (event.eventType === "asset") {
            const assetEvent = event;
            switch (assetEvent.cause) {
                case "READ" /* AssetEventCause.READ */:
                case "CREATE" /* AssetEventCause.CREATE */:
                case "UPDATE" /* AssetEventCause.UPDATE */:
                    this.onAssetChanged(assetEvent.asset);
                    break;
                case "DELETE" /* AssetEventCause.DELETE */:
                    this.onAssetChanged(undefined);
                    break;
            }
        }
    }
    onAssetChanged(asset) {
        return __awaiter(this, void 0, void 0, function* () {
            if (asset) {
                this.direction = undefined;
                this.displayValue = undefined;
                const locAttr = asset.attributes ? asset.attributes["location" /* WellknownAttributes.LOCATION */] : undefined;
                this._updateLocation(locAttr ? locAttr.value : null);
                const assetTypeConfig = getMarkerConfigForAssetType(this.config, asset.type);
                const showDirection = !assetTypeConfig || !assetTypeConfig.hideDirection;
                const showLabel = assetTypeConfig && assetTypeConfig.showLabel === true && !!assetTypeConfig.attributeName;
                const showUnits = !!(assetTypeConfig && assetTypeConfig.showUnits !== false);
                if (showLabel && asset.attributes && asset.attributes[assetTypeConfig === null || assetTypeConfig === void 0 ? void 0 : assetTypeConfig.attributeName]) {
                    const attr = asset.attributes[assetTypeConfig.attributeName];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
                    this.displayValue = Util.getAttributeValueAsString(attr, descriptors[0], asset.type, showUnits, "-");
                }
                if (showDirection) {
                    if (asset.attributes && asset.attributes["direction" /* WellknownAttributes.DIRECTION */]) {
                        const directionVal = asset.attributes["direction" /* WellknownAttributes.DIRECTION */].value;
                        if (directionVal !== undefined && directionVal !== null) {
                            this.direction = directionVal.toString();
                        }
                    }
                }
                this.type = asset.type;
            }
            else {
                this.lat = undefined;
                this.lng = undefined;
            }
        });
    }
    _updateLocation(location) {
        this.lat = location && location.coordinates ? location.coordinates[1] : undefined;
        this.lng = location && location.coordinates ? location.coordinates[0] : undefined;
    }
    getColor() {
        if (this.markerColor && !this.color) {
            return "#" + this.markerColor;
        }
        return super.getColor();
    }
    getActiveColor() {
        if (this.markerColor && !this.activeColor) {
            return "#" + this.markerColor;
        }
        return super.getActiveColor();
    }
};
__decorate([
    property({ type: String, reflect: true, attribute: true })
], OrMapMarkerAsset.prototype, "assetId", void 0);
__decorate([
    property({ type: Object, attribute: true })
], OrMapMarkerAsset.prototype, "asset", void 0);
__decorate([
    property()
], OrMapMarkerAsset.prototype, "config", void 0);
OrMapMarkerAsset = __decorate([
    customElement("or-map-marker-asset")
], OrMapMarkerAsset);
export { OrMapMarkerAsset };
//# sourceMappingURL=or-map-marker-asset.js.map