import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager";
import { preparedAssetsForRules } from "./fixtures/data/assets";
import { AttributeEvent } from "@openremote/model";

test.use({ storageState: userStatePath });

test.beforeEach(async ({ manager }) => {
    await manager.setup("smartcity", { assets: exportAssets });
});

test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});

// Reuse assets from rules test which have storeDataPoints: true
const exportAssets = preparedAssetsForRules;
const exportAttributeName = "energyLevel";

/**
 * Generate attribute events with timestamps for creating historical datapoints.
 * @param assetId The asset ID to create events for
 * @param attributeName The attribute name to create events for
 * @param count Number of datapoints to create
 * @param intervalMinutes Interval between datapoints in minutes
 */
function generateDatapointEvents(assetId: string, attributeName: string, count: number = 5, intervalMinutes: number = 5): AttributeEvent[] {
  const now = Date.now();
  const events: AttributeEvent[] = [];

  for (let i = 0; i < count; i++) {
    events.push({
      eventType: "attribute",
      ref: {
        id: assetId,
        name: attributeName
      },
      value: 20 + i * 2,
      timestamp: now - (count - 1 - i) * intervalMinutes * 60 * 1000
    });
  }

  return events;
}

/**
 * Add an attribute to the export table using the attribute picker dialog.
 * @param page The page to find "add asset attribute" on
 * @param assetName The name of the asset to select in the asset tree
 * @param attributeName The name of the attribute to select
 */
async function addAttributeViaPicker(page: import("@playwright/test").Page, assetName: string, attributeName: string) {
  await page.locator("a.button:has-text('Add asset attribute')").click();
  const dialog = page.locator("or-asset-attribute-picker");
  await dialog.locator(".mdc-dialog--open").waitFor();

  const assetTree = dialog.locator("or-asset-tree");
  await assetTree.locator("li.asset-list-element").first().waitFor();
  await assetTree.locator(`li.asset-list-element:has-text("${assetName}")`).click();

  const attributeList = dialog.locator("#attribute-selector");
  await attributeList.waitFor();
  const displayName = attributeName.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/^./, c => c.toUpperCase());
  const attributeCheckbox = attributeList.locator("li.mdc-list-item").filter({ hasText: displayName });
  await attributeCheckbox.click();

  await dialog.locator("#add-btn").click();
  await dialog.locator(".mdc-dialog--open").waitFor({ state: "detached" });
}

/**
 * @given An asset with datapoints exists
 * @and Logged into the "smartcity" realm
 * @and Navigated to the "Export" page
 * @when Selecting an attribute with datapoints
 * @and Clicking export
 * @then The API call to the file export download should return ok
 */
test("Export datapoints successfully triggers download", async ({ page, manager }) => {
  const asset = manager.assets[0];
  const events = generateDatapointEvents(asset.id!, exportAttributeName, 5);
  await manager.api.AssetResource.writeAttributeEvents(events);

  await manager.goToRealmStartPage("smartcity");
  await page.waitForLoadState("networkidle");
  await manager.navigateToMenuItem("export");
  await page.locator("text=Data export").waitFor({ state: "visible" });

  await addAttributeViaPicker(page, asset.name!, exportAttributeName);
  await page.locator("table tbody tr").first().waitFor({ state: "visible", timeout: 15000 });

  const oldestDatapoint = await page.locator("table tbody tr").first().locator("td").nth(2).textContent();
  const latestDatapoint = await page.locator("table tbody tr").first().locator("td").nth(3).textContent();
  const toDatetimeLocal = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toISOString().slice(0, 16);
  };
  await page.locator("or-mwc-input[label='Export from']").locator("input").fill(toDatetimeLocal(oldestDatapoint!));
  await page.locator("or-mwc-input[label='To']").locator("input").fill(toDatetimeLocal(latestDatapoint!));

  await page.locator("or-mwc-input[label='Export format']").click();
  await page.locator("li.mdc-list-item:has-text('CSV')").first().click();

  const exportResponsePromise = page.waitForResponse(
    response => response.url().includes("/asset/datapoint/export")
  );
  await page.getByRole("button", { name: "Export", exact: true }).click();

  const response = await exportResponsePromise;
  expect(response.status()).toBe(200);
  await expect(page.locator("or-mwc-snackbar .mdc-snackbar--open")).not.toBeAttached({ timeout: 2000 });
});
