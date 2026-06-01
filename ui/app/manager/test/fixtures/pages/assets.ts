import { BasePage, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import { Asset, AssetModelUtil } from "@openremote/model";

export class AssetsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    await this.manager.navigateToTab("Assets");
  }

  async gotoAssetId(realm: string, id: string, editor = false) {
    const currentUrl = this.page.url().startsWith("http") ? new URL(this.page.url()) : undefined;
    const appLoaded = await this.page.locator("#desktop-left").isVisible().catch(() => false);
    if (!appLoaded && (!currentUrl || currentUrl.searchParams.get("realm") !== realm)) {
      await this.page.goto(this.manager.getAppUrl(realm));
    }
    await this.page.locator("#desktop-left").waitFor({ state: "visible" });
    await this.page.waitForFunction(() => !window.location.hash.includes("code=") && !window.location.hash.includes("state="));
    await this.page.evaluate((route) => {
      window.location.hash = route;
    }, `/assets/${editor}/${id}`);
    await expect(this.page.locator("or-asset-viewer#viewer")).toHaveJSProperty("assetId", id);
  }

  /**
   * Add asset of type and with name.
   *
   * Internally registers the asset for cleanup.
   *
   * @param type The asset type
   * @param name The name of the asset
   */
  async addAsset(type: string, name: string): Promise<Asset> {
    const realm = this.manager.realm ?? new URL(this.page.url()).searchParams.get("realm");
    let assetTypeInfo = AssetModelUtil.getAssetTypeInfo(type);
    if (!assetTypeInfo) {
      AssetModelUtil._assetTypeInfos = (await this.manager.api.AssetModelResource.getAssetInfos()).data;
      assetTypeInfo = AssetModelUtil.getAssetTypeInfo(type);
    }
    expect(assetTypeInfo, { message: `Missing asset type info for ${type}` }).toBeTruthy();
    const asset: Asset = {
      name,
      type,
      realm,
      attributes: Object.fromEntries(
        Object.values(assetTypeInfo!.attributeDescriptors ?? {})
          .filter(attributeDescriptor => !attributeDescriptor.optional)
          .map(attributeDescriptor => [
            attributeDescriptor.name!,
            {
              name: attributeDescriptor.name,
              type: typeof attributeDescriptor.type === "string" ? attributeDescriptor.type : attributeDescriptor.type?.name,
              meta: attributeDescriptor.meta ? { ...attributeDescriptor.meta } : undefined,
            },
          ])
      ),
    };
    await this.manager.createAsset(asset);
    const createdAsset = this.manager.assets[this.manager.assets.length - 1];
    expect(createdAsset?.id, { message: `Failed to create ${name}` }).toBeTruthy();
    await this.gotoAssetId(createdAsset.realm ?? realm!, createdAsset.id!);
    return createdAsset;
  }

  /**
   * Delete an asset by its name.
   * @param manager The manager instance
   * @param asset The asset name
   * @param page The page or locator to search from
   */
  async deleteSelectedAsset(manager: Manager, asset: string, locator?: any) {
    const assetLocator = locator ?? this.page.locator(`text="${asset}"`);
    await expect(assetLocator).toHaveCount(1);
    await assetLocator.click();
    await this.page.click(".mdi-delete");
    await this.page.getByRole("button", { name: "Delete" }).click();
    await expect(assetLocator).toHaveCount(0);
    manager.assets = manager.assets.filter(a => a.name !== asset); // Remove asset from cache as well
  }
}
