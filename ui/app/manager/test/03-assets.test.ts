import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import assets, { assetPatches } from "./fixtures/data/assets.js";

test.use({ storageState: "test/fixtures/data/user.json" });

assets.forEach(({ type, name, attributes }) => {
  const { attribute1, attribute2, attribute3, value1, value2, value3, x, y } =
    assetPatches[name as keyof typeof assetPatches];

  test(`Add new asset: ${name}`, async ({ page, manager, assetsPage }) => {
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await assetsPage.goto();
    // Then Create a "<asset>" with name of "<name>"
    await assetsPage.addAsset(type!, name!);
    // When Go to asset "<name>" info page
    await page.click(`#list-container >> text=${name}`);
    // Then Go to modify mode
    await assetsPage.switchMode("modify");
    // When set "<value_1>" to the "<attribute_1>"
    await assetsPage.setAttributeValue(attribute1, value1);
    // And set "<value_2>" to the "<attribute_2>"
    await assetsPage.setAttributeValue(attribute2, value2);
    // And save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
    // Then We see the asset with name of "<name>"
    await expect(page.locator(`text=${name}`)).toHaveCount(1);
  });
  test(`Search and select asset: ${name}`, async ({ page, manager }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
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
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Select the "<name>"
    await page.click(`text=${name}`);
    // Then Update "<value>" to the "<attribute>" with type of "<type>"
    const type = attributes[attribute3 as keyof typeof attributes].type; // TODO: check if this can be refactored
    const item = page.locator(`#field-${attribute3} input[type="${type}"]`);
    if (await item.isEditable()) {
      await item.fill(value3);
      await page.click(`#field-${attribute3} #send-btn span`);
    }
    // When Go to modify mode
    await assetsPage.switchMode("modify");
    // Then Update location of <location_x> and <location_y>
    await page.click("text=location GEO JSON point >> button span");
    await page.mouse.click(x, y, { delay: 1000 });
    await page.click('button:has-text("OK")');
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });
  test(`Set and cancel read-only for asset: ${name}`, async ({ page, manager, assetsPage }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Go to asset "<name>" info page
    await page.click(`text=${name}`);
    // Then Go to modify mode
    await assetsPage.switchMode("modify");
    await page.getByRole("button", { name: "Expand all" }).click();
    // Then Uncheck on readonly of "<attribute_1>"
    await assetsPage.getAttributeLocator(attribute1).click();
    await assetsPage.getConfigurationItemLocator(attribute1, "Read only").click();
    // Then Check on readonly of "<attribute_2>"
    await assetsPage.getAttributeLocator(attribute2).click();
    await assetsPage.getConfigurationItemLocator(attribute2, "Read only").click();
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
    // When Go to panel page
    await page.getByRole("button", { name: "View" }).click();
    // Then We should see a button on the right of "<attribute_1>"
    await expect(page.getByRole("button", { name: "Modify" })).toBeVisible();
    // And No button on the right of "<attribute_2>"
    expect(page.locator(`#field-${attribute2} button`)).toHaveCount(0);
  });
  test(`Set assets' configuration item for Insights and Rules: ${name}`, async ({ page, manager, assetsPage }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Go to asset "<name>" info page
    await page.click(`text=${name}`);
    // Then Go to modify mode
    await assetsPage.switchMode("modify");
    const config_item_1 = "Rule state";
    const config_item_2 = "Store data points";
    // Then Select "<item_1>" and "<item_2>" on "<attribute_1>"
    await assetsPage.configItem(config_item_1, config_item_2, attribute1);
    // Then Select "<item_1>" and "<item_2>" on "<attribute_2>"
    await assetsPage.configItem(config_item_1, config_item_2, attribute2);
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });
});

test("Delete assets", async ({ page, manager, assetsPage }) => {
  // Given assets are setup
  await manager.setup("smartcity", { assets });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  // When Delete assets
  await assetsPage.deleteSelectedAsset("Battery");
  await assetsPage.deleteSelectedAsset("Solar Panel");
  // Then We should see an empty asset column
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
