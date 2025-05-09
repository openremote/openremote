import { expect } from "@playwright/test";
import { test } from "./fixtures/test.js";
import assets from "./fixtures/data/assets.js";

test.beforeEach(async ({ setup, login, goToRealmStartPage }) => {
  await setup("smartcity", "lv2");
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await goToRealmStartPage("smartcity");
  await login("smartcity");
});

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
    test(`Add new asset: ${name}`, async ({ page, navigateToTab, switchMode, unselect, save }) => {
      // Then Navigate to "asset" tab
      await navigateToTab("asset");
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
      console.log("save btn is " + isSaveBtnVisible);
      if (isSaveBtnVisible) {
        console.log("ready to save");
        await page.click('button:has-text("Save")');
      }
      await unselect();
      // When Go to asset "<name>" info page
      await page.click(`#list-container >> text=${name}`);
      // Then Go to modify mode
      await page.waitForTimeout(1000);
      await switchMode("modify");
      // Then Give "<value_1>" to the "<attribute_1>" with type of "<A1_type>"
      await page.fill(`text=${attr_1} ${a1_type} >> input[type="number"]`, v1);
      // Then Give "<value_2>" to the "<attribute_2>" with type of "<A2_type>"
      await page.fill(`text=${attr_2} ${a2_type} >> input[type="number"]`, v2);
      // Then Save
      await save();
      // When Unselect
      // Then We see the asset with name of "<name>"
      await page.waitForTimeout(500);
      const count = await page.locator(`text=${name}`).count();
      // reason why it's 1 is because that this scnario runs in a outline
      // each time only one set of data will be used in one run of outlines
      // thus, only one asset will be added and removed in one run and next time will start with the empty envrioment
      await expect(count).toBe(1);
    });
    test(`Search and select asset: ${name}`, async ({ page, navigateToTab }) => {
      // Given Setup "lv3"
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      // Then Navigate to "asset" tab
      await navigateToTab("asset");
      // When Search for the "<name>"
      await page.fill('#filterInput input[type="text"]', name);
      // When Select the "<name>"
      await page.click(`text=${name}`);
      // Then We see the "<name>" page
      await expect(await page.waitForSelector(`#asset-header >> text=${name}`)).not.toBeNull();
    });
    test(`Update asset: ${name}`, async ({ page, switchMode, navigateToTab }) => {
      // Given Setup "lv3"
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      // Then Navigate to "asset" tab
      await navigateToTab("asset");
      // When Select the "<name>"
      await page.click(`text=${name}`);
      // Then Update "<value>" to the "<attribute>" with type of "<type>"
      const item = page.locator(`#field-${attr_3} input[type="${a3_type}"]`);
      if (await item.isEditable()) {
        await item.fill(v3);
        await page.click(`#field-${attr_3} #send-btn span`);
      }
      // When Go to modify mode
      await switchMode("modify");
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
    test.skip(`Set and cancel read-only for asset: ${name}`, async ({ page, navigateToTab, switchMode }) => {
      // Given Setup "lv3"
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      // Then Navigate to "asset" tab
      await navigateToTab("asset");
      // When Go to asset "<name>" info page
      await page.click(`text=${name}`);
      // Then Go to modify mode
      await switchMode("modify");
      // Then Uncheck on readonly of "<attribute_1>"
      await page.click(`td:has-text("${attr_1}") >> nth=0`);
      // bad solution
      // nth number is decided by the default state
      // if default stete changes, please change the nth number
      if (attr_1 == "energyLevel") await page.click("text=Read only >> nth=2");
      else await page.click("text=Read only >> nth=1");
      // Then Check on readonly of "<attribute_2>"
      await page.click(`td:has-text("${attr_2}")`);
      // bad solution
      // in this case, i assume that the config items are as the beginning state, namely default state
      // if the default state changes, the following nth-chlid should change as well
      if (attr_2 == "efficiencyExport") await page.click(".item-add or-mwc-input #component >> nth=0");
      else await page.click("tr:nth-child(14) td .meta-item-container div .item-add or-mwc-input #component");
      await page.click('li[role="checkbox"]:has-text("Read only")');
      await page.click('div[role="alertdialog"] button:has-text("Add")');
      // Then Save
      // When Go to panel page
      await page.click('button:has-text("View")');
      await page.waitForTimeout(1500);
      // Then We should see a button on the right of "<attribute_1>"
      await expect(await page.waitForSelector(`#field-${attr_1} button`)).not.toBeNull();
      // And No button on the right of "<attribute_2>"
      expect(page.locator(`#field-${attr_2} button`)).toHaveCount(0);
    });
    test(`Set assets' configuration item for Insights and Rules: ${name}`, async ({
      page,
      configItem,
      navigateToTab,
      switchMode,
    }) => {
      // Given Setup "lv3"
      // When Login to OpenRemote "smartcity" realm as "smartcity"
      // Then Navigate to "asset" tab
      await navigateToTab("asset");
      // When Go to asset "<name>" info page
      await page.click(`text=${name}`);
      // Then Go to modify mode
      await switchMode("modify");
      // Then Select "<item_1>" and "<item_2>" on "<attribute_1>"
      await configItem(config_item_1, config_item_2, attr_1);
      // Then Select "<item_1>" and "<item_2>" on "<attribute_2>"
      await configItem(config_item_1, config_item_2, attr_2);
      // Then Save
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
    });
  }
);
