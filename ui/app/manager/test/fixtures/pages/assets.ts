import { BasePage, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import { Asset } from "@openremote/model";

export class AssetsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToTab("Assets");
  }

  /**
   * Add asset of type and with name.
   *
   * Internally registers the asset for cleanup.
   *
   * @param type The asset type
   * @param name The name of the asset
   */
  async addAsset(type: string, name: string) {
    await this.page.click(".mdi-plus");
    await this.page.click(`li[data-value="${type}"]`);
    await this.page.fill('#name-input input[type="text"]', name);
    await this.shared.interceptResponse<Asset>("**/asset", (asset) => {
      if (asset) this.manager.assets.push(asset);
    });
    await this.page.click("#add-btn");
  }

  /**
   * Delete an asset by its name.
   * @param asset The asset name
   */
  async deleteSelectedAsset(asset: string) {
    const assetLocator = this.page.locator(`text=${asset}`);
    await expect(assetLocator).toHaveCount(1);
    await assetLocator.click();
    await this.page.click(".mdi-delete");
    await this.page.getByRole("button", { name: "Delete" }).click();
    await expect(assetLocator).toHaveCount(0);
  }
}
