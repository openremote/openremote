import type {Page} from "@openremote/test";

export class AssetTree {
    constructor(private readonly page: Page) {}

    /**
     * Returns a locator of the filter input
     */
    getFilterInput() {
        return this.page.locator(`or-asset-tree #filterInput input[type="text"]`);
    }

    /**
     * Fills the filter input with the supplied text
     * @param text - Text to filter nodes by
     */
    fillFilterInput(text: string) {
        return this.page.fill(`or-asset-tree #filterInput input[type="text"]`, text);
    }

    /**
     * Returns a locator of all nodes that contain assets
     */
    getAssetNodes() {
        return this.page.locator(`or-asset-tree ol li [node-asset-id]:not([node-asset-id=""])`);
    }

    /**
     * Returns a locator of all nodes that are selected
     */
    getSelectedNodes() {
        return this.page.locator(`or-asset-tree ol li[data-selected]`);
    }
}
