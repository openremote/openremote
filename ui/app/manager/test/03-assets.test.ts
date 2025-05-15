import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import assets, { preparedAssets, preparedAssetsWithReadonly } from "./fixtures/data/assets.js";
import { users } from "./fixtures/data/users.js";

assets.forEach(
  ({
    name,
    asset,
    attr_1,
    attr_2,
    attr_3,
    a1_type,
    a2_type,
    a3_type,
    v1,
    v2,
    v3,
    config_item_1,
    config_item_2,
    location_x,
    location_y,
  }) => {
    test(`Add new asset: ${name}`, async ({ page, manager, assetsPage }) => {
      // Given the Realm "smartcity" with the user "smartcity" is setup
      await manager.setup("smartcity", { user: users.smartcity });
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      await manager.goToRealmStartPage("smartcity");
      await manager.login("smartcity");
      // Then Navigate to "asset" tab
      await manager.navigateToTab("asset");
      // Then Create a "<asset>" with name of "<name>"
      // select conosle first to get into the modify mode
      // await page.click(`#list-container >> text="Consoles"`);
      // await switchMode("modify");
      // await unselect();
      // start adding assets
      await page.click(".mdi-plus");
      await page.click(`text=${asset}`);
      await page.fill('#name-input input[type="text"]', name);
      await page.click("#add-btn");
      await page.waitForTimeout(400);
      // check if at modify mode
      // if yes we should see the save button then save
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
      await assetsPage.unselect();
      // When Go to asset "<name>" info page
      await page.click(`#list-container >> text=${name}`);
      // Then Go to modify mode
      await page.waitForTimeout(1000);
      await assetsPage.switchMode("modify");
      // Then Give "<value_1>" to the "<attribute_1>" with type of "<A1_type>"
      await page.fill(`text=${attr_1} ${a1_type} >> input[type="number"]`, v1);
      // Then Give "<value_2>" to the "<attribute_2>" with type of "<A2_type>"
      await page.fill(`text=${attr_2} ${a2_type} >> input[type="number"]`, v2);
      // Then Save
      await manager.save();
      // When Unselect
      // Then We see the asset with name of "<name>"
      await page.waitForTimeout(500);
      // reason why it's 1 is because that this scnario runs in a outline
      // each time only one set of data will be used in one run of outlines
      // thus, only one asset will be added and removed in one run and next time will start with the empty envrioment
      await expect(page.locator(`text=${name}`)).toHaveCount(1);
    });
    test(`Search and select asset: ${name}`, async ({ page, manager }) => {
      // Given the Realm "smartcity" with the user "smartcity" and assets is setup
      await manager.setup("smartcity", { user: users.smartcity, assets: preparedAssets });
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      await manager.goToRealmStartPage("smartcity");
      await manager.login("smartcity");
      // Then Navigate to "asset" tab
      await manager.navigateToTab("asset");
      // When Search for the "<name>"
      await page.fill('#filterInput input[type="text"]', name);
      // When Select the "<name>"
      await page.click(`text=${name}`);
      // Then We see the "<name>" page
      await expect(await page.waitForSelector(`#asset-header >> text=${name}`)).not.toBeNull();
    });
    test(`Update asset: ${name}`, async ({ page, manager, assetsPage }) => {
      // Given the Realm "smartcity" with the user "smartcity" and assets is setup
      await manager.setup("smartcity", { user: users.smartcity, assets: preparedAssets });
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      await manager.goToRealmStartPage("smartcity");
      await manager.login("smartcity");
      // Then Navigate to "asset" tab
      await manager.navigateToTab("asset");
      // When Select the "<name>"
      await page.click(`text=${name}`);
      // Then Update "<value>" to the "<attribute>" with type of "<type>"
      const item = page.locator(`#field-${attr_3} input[type="${a3_type}"]`);
      if (await item.isEditable()) {
        await item.fill(v3);
        await page.click(`#field-${attr_3} #send-btn span`);
      }
      // When Go to modify mode
      await assetsPage.switchMode("modify");
      // Then Update location of <location_x> and <location_y>
      await page.click("text=location GEO JSON point >> button span");
      await page.waitForTimeout(2000);
      await page.mouse.click(location_x, location_y, { delay: 1000 });
      await page.click('button:has-text("OK")');
      // Then Save
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
    });
    test(`Set and cancel read-only for asset: ${name}`, async ({ page, manager, assetsPage }) => {
      // Given the Realm "smartcity" with the user "smartcity" and assets is setup
      await manager.setup("smartcity", { user: users.smartcity, assets: preparedAssetsWithReadonly });
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      await manager.goToRealmStartPage("smartcity");
      await manager.login("smartcity");
      // Then Navigate to "asset" tab
      await manager.navigateToTab("asset");
      // When Go to asset "<name>" info page
      await page.click(`text=${name}`);
      // Then Go to modify mode
      await assetsPage.switchMode("modify");
      await page.getByRole("button", { name: "Expand all" }).click();
      // Then Uncheck on readonly of "<attribute_1>"
      await assetsPage.getAttributeLocator(attr_1).click();
      await assetsPage.getConfigurationItemLocator(attr_1, "Read only").click();
      // Then Check on readonly of "<attribute_2>"
      await assetsPage.getAttributeLocator(attr_2).click();
      await assetsPage.getConfigurationItemLocator(attr_2, "Read only").click();
      // Then Save
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
      // When Go to panel page
      await page.click('button:has-text("View")');
      await page.waitForTimeout(1500);
      // Then We should see a button on the right of "<attribute_1>"
      await expect(await page.waitForSelector(`#field-${attr_1} button`)).not.toBeNull();
      // And No button on the right of "<attribute_2>"
      expect(page.locator(`#field-${attr_2} button`)).toHaveCount(0);
    });
    test(`Set assets' configuration item for Insights and Rules: ${name}`, async ({ page, manager, assetsPage }) => {
      // Given the Realm "smartcity" with the user "smartcity" and assets is setup
      await manager.setup("smartcity", { user: users.smartcity, assets: preparedAssets });
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      await manager.goToRealmStartPage("smartcity");
      await manager.login("smartcity");
      // Then Navigate to "asset" tab
      await manager.navigateToTab("asset");
      // When Go to asset "<name>" info page
      await page.click(`text=${name}`);
      // Then Go to modify mode
      await assetsPage.switchMode("modify");
      // Then Select "<item_1>" and "<item_2>" on "<attribute_1>"
      await assetsPage.configItem(config_item_1, config_item_2, attr_1);
      // Then Select "<item_1>" and "<item_2>" on "<attribute_2>"
      await assetsPage.configItem(config_item_1, config_item_2, attr_2);
      // Then Save
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
    });
  }
);

test("Delete assets", async ({ page, manager, assetsPage }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { assets: preparedAssets });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  // When Delete assets
  await assetsPage.deleteSelectedAsset("Battery");
  await page.waitForTimeout(500);
  await assetsPage.deleteSelectedAsset("Solar Panel");

  // must wait to confirm that assets have been deleted
  await page.waitForTimeout(500);
  // Then We should see an empty asset column
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
