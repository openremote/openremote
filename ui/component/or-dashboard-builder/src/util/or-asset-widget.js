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
import { OrWidget } from "./or-widget";
import { state } from "lit/decorators.js";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { WidgetSettings } from "./widget-settings";
/*
* OrAssetWidget class
*
* It is an extension on base class OrWidget,
* where some asset-specific methods such as fetching, are already defined for ease of use.
* For example it is used in the chart-, gauge- and kpi widget
* */
export class OrAssetWidget extends OrWidget {
    constructor() {
        super(...arguments);
        this.loadedAssets = [];
        this.assetAttributes = [];
    }
    static get styles() {
        return [...super.styles];
    }
    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    fetchAssets(attributeRefs = []) {
        return __awaiter(this, void 0, void 0, function* () {
            return fetchAssetsByAttributeRef(attributeRefs);
        });
    }
    queryAssets(assetQuery) {
        return __awaiter(this, void 0, void 0, function* () {
            return fetchAssets(assetQuery);
        });
    }
    isAssetLoaded(assetId) {
        return isAssetIdLoaded(this.loadedAssets, assetId);
    }
    isAttributeRefLoaded(attributeRef) {
        return isAssetIdLoaded(this.loadedAssets, attributeRef.id);
    }
}
__decorate([
    state() // cached assets
], OrAssetWidget.prototype, "loadedAssets", void 0);
__decorate([
    state() // cached attribute list; [asset index of the loadedAssets array, attribute]
], OrAssetWidget.prototype, "assetAttributes", void 0);
/*
* AssetWidgetSettings class
*
* It is an extension on base class WidgetSettings
* where some asset-specific methods such as fetching, are already defined for ease of use.
* For example it is used in the chart-, gauge- and kpi widget settings
* */
export class AssetWidgetSettings extends WidgetSettings {
    constructor() {
        super(...arguments);
        this.loadedAssets = [];
    }
    fetchAssets(attributeRefs = []) {
        return __awaiter(this, void 0, void 0, function* () {
            return fetchAssetsByAttributeRef(attributeRefs);
        });
    }
    queryAssets(assetQuery) {
        return __awaiter(this, void 0, void 0, function* () {
            return fetchAssets(assetQuery);
        });
    }
    isAssetLoaded(assetId) {
        return isAssetIdLoaded(this.loadedAssets, assetId);
    }
    isAttributeRefLoaded(attributeRef) {
        return isAssetIdLoaded(this.loadedAssets, attributeRef.id);
    }
}
__decorate([
    state() // cached assets
], AssetWidgetSettings.prototype, "loadedAssets", void 0);
/* ---------------------------------------------------------- */
// GENERIC FUNCTIONS
// Simple async function for fetching assets by attributeRefs
function fetchAssetsByAttributeRef(attributeRefs = []) {
    return __awaiter(this, void 0, void 0, function* () {
        const assetIds = attributeRefs.map(ar => ar.id);
        const attributeNames = attributeRefs.map(ar => ar.name);
        return fetchAssets({
            ids: attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.map((x) => x.id),
            select: {
                attributes: attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.map((x) => x.name)
            }
        });
    });
}
function fetchAssets(assetQuery) {
    return __awaiter(this, void 0, void 0, function* () {
        let assets = [];
        assetQuery.realm = { name: manager.displayRealm };
        yield manager.rest.api.AssetResource.queryAssets(assetQuery).then(response => {
            assets = response.data;
        }).catch((reason) => {
            console.error(reason);
            showSnackbar(undefined, "errorOccurred");
        });
        return assets;
    });
}
function isAssetIdLoaded(loadedAssets, assetId) {
    return (loadedAssets === null || loadedAssets === void 0 ? void 0 : loadedAssets.find(asset => asset.id === assetId)) !== undefined;
}
//# sourceMappingURL=or-asset-widget.js.map