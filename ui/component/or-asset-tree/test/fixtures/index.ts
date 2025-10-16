import type {Page} from "@openremote/test";
import type {Locator} from "@playwright/test";

export class AssetTree {
    constructor(private readonly page: Page) {}

    /**
     * Returns a locator of the filter input
     */
    getFilterInput() {
        return this.page.locator(`or-asset-tree #filterInput input[type="text"]`);
    }

    /**
     * Returns a locator of all nodes that contain assets
     */
    getAssetNodes() {
        return this.page.locator(`or-asset-tree ol li:has(.node-container[node-asset-id]:not([node-asset-id=""]))`);
    }

    /**
     * Returns a locator of all child nodes of the supplied node
     * @param node - Locator pointing to the parent node
     */
    getChildNodes(node: Locator) {
        return node.locator(`ol li .node-container[node-asset-id]:not([node-asset-id=""])`);
    }

    /**
     * Returns a locator of all nodes that are selected
     */
    getSelectedNodes() {
        return this.page.locator(`or-asset-tree ol li[data-selected]`);
    }
}
