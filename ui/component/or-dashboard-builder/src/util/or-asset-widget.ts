import {OrWidget} from "./or-widget";
import {Asset, Attribute, AttributeRef} from "@openremote/model";
import { state } from "lit/decorators.js";
import manager from "@openremote/core";
import {i18next} from "@openremote/or-translate";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import {WidgetConfig} from "./widget-config";
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

    static get styles(): CSSResult[] {
        return [...super.styles];
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    protected async fetchAssets(attributeRefs: AttributeRef[] = []) {
        return fetchAssets(attributeRefs);
    }

    protected isAttributeRefLoaded(attributeRef: AttributeRef) {
        return isAttributeRefLoaded(this.loadedAssets, attributeRef);
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
        return fetchAssets(attributeRefs);
    }

    protected isAttributeRefLoaded(attributeRef: AttributeRef) {
        return isAttributeRefLoaded(this.loadedAssets, attributeRef);
    }

}



/* ---------------------------------------------------------- */

// GENERIC FUNCTIONS

// Simple async function for fetching assets by attributeRefs
async function fetchAssets(attributeRefs: AttributeRef[] = []) {
    let assets: Asset[] = [];
    await manager.rest.api.AssetResource.queryAssets({
        ids: attributeRefs?.map((x: AttributeRef) => x.id) as string[],
        realm: { name: manager.displayRealm },
        select: {
            attributes: attributeRefs?.map((x: AttributeRef) => x.name) as string[]
        }
    }).then(response => {
        assets = response.data;
    }).catch((reason) => {
        console.error(reason);
        showSnackbar(undefined, i18next.t('errorOccurred'));
    });
    return assets;
}

function isAttributeRefLoaded(loadedAssets: Asset[] | undefined, attrRef: AttributeRef) {
    return loadedAssets?.find((asset) => asset.id === attrRef.id) !== undefined;
}
