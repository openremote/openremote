import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import assets, { assetMap, parentAssets, assetPatches, thing } from "./fixtures/data/assets.js";
import {WellknownMetaItems} from "@openremote/model";
import * as Util from "@openremote/core/lib/util";

test.use({ storageState: userStatePath });

assets.forEach(({ type, name, attributes }) => {
  const { attribute1, attribute2, attribute3, value1, value2, value3, x, y } =
    assetPatches[name as keyof typeof assetPatches];

  /**
   * @given Logged in to OpenRemote "smartcity" realm as "smartcity"
   * @when Navigating to the "asset" tab
   * @and Creating an asset of a specific type with the given name
   * @and Opening the asset's detail page
   * @and Switching to modify mode
   * @and Setting values for specified attributes
   * @and Saving the changes
   * @then The asset with the given name should be visible in the UI
   */
  test(`Add new ${name} asset and configure its attributes`, async ({ page, manager, assetsPage, assetViewer }) => {
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await assetsPage.addAsset(assetMap[name!], name!);
    await page.click(`#list-container >> text=${name}`);
    await assetViewer.switchMode("modify");
    await assetViewer.setAttributeValue(attribute1, value1);
    await assetViewer.setAttributeValue(attribute2, value2);
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator(`text=${name}`)).toHaveCount(1);
  });

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "asset" tab
   * @and Searching for the asset by name
   * @and Selecting the asset from the list
   * @then The asset detail page is displayed
   */
  test(`Search for and select a ${name} asset`, async ({ page, manager, assetTree }) => {
    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await assetTree.fillFilterInput(name);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await page.click(`text=${name}`);
    await expect(page.locator(`#asset-header`, { hasText: name })).toBeVisible();
  });

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "asset" tab
   * @and Searching for the asset by ID
   * @and Selecting the asset from the list
   * @then The asset detail page is displayed
   */
  test(`Search by Asset ID and select the ${name} asset`, async ({ page, manager, assetTree }) => {
    await manager.setup("smartcity", { assets });
    const id = manager.assets.find(asset => asset.name === name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await assetTree.fillFilterInput(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await page.click(`text=${name}`);
    await expect(page.locator(`#asset-header`, { hasText: name })).toBeVisible();
  })

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "/assets/false/{id}" page directly
   * @then The filter should contain the ID as input
   * @and The asset list should contain the asset with the given ID
   * @and The asset detail page is displayed
   */
  test(`Open browser tab directly to the ${name} asset`, async ({ page, manager, assetsPage, assetTree }) => {
    await manager.setup("smartcity", { assets });
    const id = manager.assets.find(asset => asset.name === name)?.id;
    expect(id).toBeDefined();
    await assetsPage.gotoAssetId("smartcity", id!);
    await expect(assetTree.getFilterInput()).toHaveValue(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(page.locator(`#asset-header`, { hasText: name })).toBeVisible();
  })

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "asset" tab
   * @and Selecting an asset by name
   * @and Updating a specific attribute with a new value and type
   * @and Switching to modify mode
   * @and Updating the asset's location via map click
   * @and Saving the changes
   * @then The updated asset is saved and changes are persisted
   */
  test(`Update a ${name} asset's attributes and location`, async ({ page, manager, assetViewer }) => {
    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await page.click(`text=${name}`);

    const type = attributes[attribute3 as keyof typeof attributes].type;
    const item = page.locator(`#field-${attribute3} input[type="${type}"]`);
    if (await item.isEditable()) {
      await item.fill(value3);
      await page.click(`#field-${attribute3} #send-btn span`);
    }

    await assetViewer.switchMode("modify");
    await assetViewer.getAttributeLocator("location").getByRole("button").click();
    await page.mouse.click(x, y, { delay: 1000 });
    await page.getByRole("button", { name: "OK" }).click();

    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "asset" tab
   * @and Selecting the asset by name
   * @and Switching to modify mode
   * @and Toggling read-only status for two attributes
   * @and Saving the changes
   * @and Navigating to the asset's view panel
   * @then The correct read-only indicators should be present for the attributes
   */
  test(`Toggle read-only for two attributes on a ${name} asset`, async ({ page, manager, assetViewer }) => {
    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await page.click(`text=${name}`);

    await assetViewer.switchMode("modify");
    await page.getByRole("button", { name: "Expand all" }).click();

    await assetViewer.getAttributeLocator(attribute1).click();
    await assetViewer.getConfigurationItemLocator(attribute1, "Read only").locator("label").click();

    await assetViewer.getAttributeLocator(attribute2).click();
    await assetViewer.getConfigurationItemLocator(attribute2, "Read only").locator("label").click();

    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();

    await page.getByRole("button", { name: "View" }).click();
    await expect(page.getByRole("button", { name: "Modify" })).toBeVisible();

    await expect(page.locator(`#field-${attribute1} #send-btn`)).toBeVisible();
    await expect(page.locator(`#field-${attribute2} #send-btn`)).not.toBeVisible();
  });

  /**
   * @given Assets are set up in the "smartcity" realm
   * @when Logging in to the OpenRemote "smartcity" realm
   * @and Navigating to the "asset" tab
   * @and Selecting the asset by name
   * @and Switching to modify mode
   * @and Selecting configuration items like "ruleState" and "storeDataPoints" for two attributes
   * @and Saving the changes
   * @then The configuration items are persisted correctly
   */
  test(`Set "ruleState" and "storeDataPoints" for ${name} asset attributes`, async ({ page, manager, assetViewer }) => {
    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await page.click(`text=${name}`);

    await assetViewer.switchMode("modify");
    await page.getByRole("button", { name: "Expand all" }).click();

    await assetViewer.addConfigurationItems(attribute1, "ruleState", "storeDataPoints");
    await assetViewer.addConfigurationItems(attribute2, "ruleState", "storeDataPoints");

    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });
});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Login to OpenRemote "smartcity" realm as "smartcity"
 * @and Modifying thing asset
 * @and Adding primitive configuration items
 * @then The configuration items to be visible
 */
test("Add all primitive configuration items", async ({ page, manager, assetViewer }) => {
  await manager.setup("smartcity", { assets: [thing] });
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  await page.getByText("Thing").click();
  await assetViewer.switchMode("modify");
  await page.getByRole("button", { name: "Expand all" }).click();
  const items: [`${WellknownMetaItems}`, any][] = [
    ["ruleState", true],
    ["hasPredictedDataPoints", true],
    ["storeDataPoints", true],
    ["label", "test"],
    ["showOnDashboard", true],
    ["readOnly", true],
    ["multiline", true],
    ["accessPublicWrite", true],
    ["momentary", true],
    ["accessRestrictedRead", true],
    ["dataPointsMaxAgeDays", 7],
    ["accessRestrictedWrite", true],
    ["secret", true],
    ["ruleResetImmediate", true],
    ["userConnected", "test"],
    ["accessPublicRead", true],
  ];
  await assetViewer.addConfigurationItems("notes", ...items.map(([item]) => item));
  for (const [item, value] of items) {
    const itemLocator = assetViewer.getConfigurationItemLocator("notes");
    const options = { name: Util.camelCaseToSentenceCase(item) };
    if (typeof value === "string") {
      const input = itemLocator.getByRole("textbox", options);
      await input.fill(value);
      await expect(input).toHaveValue(value);
    } else if (typeof value === "number") {
      const input = itemLocator.getByRole("spinbutton", options);
      await input.fill(`${value}`);
      await expect(input).toHaveValue(`${value}`);
    } else if (value) {
      await expect(itemLocator.getByRole("checkbox", options)).toBeChecked();
    } else {
      await expect(itemLocator.getByRole("checkbox", options)).not.toBeChecked();
    }
  }
});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to OpenRemote "smartcity" realm as admin
 * @and Navigating to the "Assets" tab
 * @and Tries to navigate through the assets in the system using the asset tree
 * @then The asset list should be complete and should not display any artifacts or visual errors.
 */
test(`Check if assets are visible in the tree`, async ({ page, manager, assetsPage, assetTree }) => {
    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();

    // Create additional assets that form a tree ('parent-child' structure)
    await Promise.all(parentAssets.map(a => manager.createAsset(a)));
    const cityIds = manager.assets.filter(a => a.type === "CityAsset").sort((a, b) => a.name!.localeCompare(b.name!)).map(a => a.id);
    const childAssets = manager.assets.filter(a => a.type !== "CityAsset");
    childAssets.forEach(ca => ca.parentId = cityIds[0]);
    await Promise.all(childAssets.map(ca => manager.updateAsset(ca)));

    // Check if City Asset 1, City Asset 2 and Consoles group asset are listed.
    await expect(assetTree.getAssetNodes()).toHaveCount(3);
    await expect(assetTree.getAssetNodes().nth(0)).toContainText(parentAssets[0].name);
    await expect(assetTree.getAssetNodes().nth(1)).toContainText(parentAssets[1].name);
    await expect(assetTree.getAssetNodes().nth(2)).toContainText('Consoles');
    await expect(assetTree.getAssetNodes().locator('.expander')).toHaveCount(3);

    // Check if expandable of City Asset 1 is correct.
    const cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('.expander').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2);
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveText([assets[0].name, assets[1].name]);

    // Check if expandable of City Asset 2 is correct.
    const cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('.expander').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2 + 0);

    // Check if expandable of Consoles group asset is correct.
    const consoleAsset = assetTree.getAssetNodes().filter({ hasText: 'Consoles' });
    await consoleAsset.locator('.expander').click();
    expect(assetTree.getAssetNodes().count).toBeGreaterThanOrEqual(3 + 2 + 0 + 1); // (there is at least 1 console, but could be more with a larger test suite)
});

// TODO: Add test for the "Load more" button, by modifying the LIMIT variable in or-asset-tree

test.fixme("Add all complex configuration items", async ({ page, manager, shared }) => {});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to OpenRemote "smartcity" realm as admin
 * @and Navigating to the "Assets" tab
 * @and Deleting the assets "Battery" and "Solar Panel"
 * @then The asset list should no longer show the deleted assets
 * @and The asset column should appear empty (showing "Console")
 */
test("Delete specified assets and verify they are removed", async ({ page, manager, assetsPage }) => {
  await manager.setup("smartcity", { assets });
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  await assetsPage.deleteSelectedAsset("Battery");
  await assetsPage.deleteSelectedAsset("Solar Panel");
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
