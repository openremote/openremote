import {OrWidget} from "./or-widget";
import {Asset, AssetQuery, Attribute, AttributeRef} from "@openremote/model";
import { state } from "lit/decorators.js";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import {isAxiosError} from "@openremote/rest";
import {WidgetSettings} from "./widget-settings";
import { CSSResult } from "lit";

/*
* OrAssetWidget class
*
* It is an extension on base class OrWidget,
* where some asset-specific methods such as fetching, are already defined for ease of use.
* For example it is used in the chart-, gauge- and kpi widget
* */
export abstract class OrAssetWidget extends OrWidget {

    @state() // cached assets
    protected loadedAssets: Asset[] = [];

    @state() // cached attribute list; [asset index of the loadedAssets array, attribute]
    protected assetAttributes: [number, Attribute<any>][] = [];

    @state()
    protected _error?: string;

    static get styles(): CSSResult[] {
        return [...super.styles];
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    protected async fetchAssets(attributeRefs: AttributeRef[] = []) {
        return fetchAssetsByAttributeRef(attributeRefs);
    }

    protected async queryAssets(assetQuery: AssetQuery) {
        return fetchAssets(assetQuery);
    }

    protected isAssetLoaded(assetId: string) {
        return isAssetIdLoaded(this.loadedAssets, assetId);
    }

    protected isAttributeRefLoaded(attributeRef: AttributeRef) {
        return isAssetIdLoaded(this.loadedAssets, attributeRef.id!);
    }

}

/*
* AssetWidgetSettings class
*
* It is an extension on base class WidgetSettings
* where some asset-specific methods such as fetching, are already defined for ease of use.
* For example it is used in the chart-, gauge- and kpi widget settings
* */
export abstract class AssetWidgetSettings extends WidgetSettings {

    @state() // cached assets
    protected loadedAssets: Asset[] = [];

    protected async fetchAssets(attributeRefs: AttributeRef[] = []) {
        return fetchAssetsByAttributeRef(attributeRefs);
    }

    protected async queryAssets(assetQuery: AssetQuery) {
        return fetchAssets(assetQuery);
    }

    protected isAssetLoaded(assetId: string) {
        return isAssetIdLoaded(this.loadedAssets, assetId)
    }

    protected isAttributeRefLoaded(attributeRef: AttributeRef) {
        return isAssetIdLoaded(this.loadedAssets, attributeRef.id!);
    }

}



/* ---------------------------------------------------------- */

// GENERIC FUNCTIONS

// Simple async function for fetching assets by attributeRefs
async function fetchAssetsByAttributeRef(attributeRefs: AttributeRef[] = []) {
    return fetchAssets({
        ids: attributeRefs?.map((x: AttributeRef) => x.id) as string[],
        select: {
            attributes: attributeRefs?.map((x: AttributeRef) => x.name) as string[]
        }
    });
}

/**
 * Function that will fetch an array of assets based on {@link assetQuery}.
 * On failure, by default, it will only log to the web browser console.
 * In some cases it will throw a custom error, that holds a custom message.
 */
async function fetchAssets(assetQuery: AssetQuery) {
    let assets: Asset[] = [];
    assetQuery.realm = { name: manager.displayRealm };
    await manager.rest.api.AssetResource.queryAssets(assetQuery).then(response => {
        assets = response.data;
    }).catch((e) => {
        console.error(e);
        showSnackbar(undefined, "errorOccurred");
        if(isAxiosError(e)) {
            if(e.message === "Network Error") {
                throw new Error("youAreOffline")
            } else if(e.code === "ECONNABORTED") {
                throw new Error("noAttributeDataTimeout")
            }
        }
    });
    return assets;
}

function isAssetIdLoaded(loadedAssets: Asset[] | undefined, assetId: string) {
    return loadedAssets?.find(asset => asset.id === assetId) !== undefined;
}
