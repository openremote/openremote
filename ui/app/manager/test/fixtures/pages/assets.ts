import { BasePage, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import { Asset } from "@openremote/model";

export class AssetsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToTab("Assets");
  }

  async addAsset(type: string, name: string) {
    // start adding assets
    await this.page.click(".mdi-plus");
    await this.page.click(`li[data-value="${type}"]`);
    await this.page.fill('#name-input input[type="text"]', name);
    // create
    await this.shared.interceptResponse<Asset>("**/asset", (asset) => {
      if (asset) this.manager.assets.push(asset);
    });
    await this.page.click("#add-btn");
  }

  /**
   * Unselect the asset
   */
  async unselect() {
    const isCloseVisible = await this.page.isVisible(".mdi-close >> nth=0");

    // unselect the asset
    if (isCloseVisible) {
      //await page.page?.locator('.mdi-close').first().click()
      await this.page.click(".mdi-close >> nth=0");
    }
  }

  /**
   * Update asset in the general panel
   * @param attr attribute's name
   * @param type attribute's input type
   * @param value input value
   */
  async updateAssets(attr: string, type: string, value: string) {
    await this.page.fill(`#field-${attr} input[type="${type}"]`, value);
    await this.page.click(`#field-${attr} #send-btn span`);
  }

  /**
   * Update the data in the modify mode
   * @param attr attribute's name
   * @param type attribute's input type
   * @param value input value
   */
  async updateInModify(attr: string, type: string, value: string) {
    await this.page.fill(`text=${attr} ${type} >> input[type="number"]`, value);
  }

  /**
   * Update location so we can see in the map
   * @param location_x horizental coordinator (start from left edge)
   * @param location_y vertail coordinator (start from top edge)
   */
  async updateLocation(x: number, y: number) {
    await this.page.click("text=location GEO JSON point >> button span");
    await this.page.mouse.click(x, y, { delay: 1000 });
    await this.page.click('button:has-text("OK")');
  }

  /**
   * Delete an asset by its name
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
