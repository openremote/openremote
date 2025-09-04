import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets } from "./fixtures/data/assets.js";

test.use({ storageState: userStatePath });

/**
 * @given Assets with location are set up in the "smartcity" realm
 * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
 * @and Navigating to the "map" tab
 * @and Checking that asset markers are displayed on the map
 * @and Clicking on a marker for the asset "Battery"
 * @and Navigating to the asset detail page from the map card
 * @then The asset detail page for "Battery" is visible
 *
 * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
 */
test("Verify that asset markers appear on the map and navigate correctly", async ({ page, manager, browserName }) => {
  test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

  await manager.setup("smartcity", { assets });
  await manager.goToRealmStartPage("smartcity");

  const asset = assets[0].name;

  await expect(page.locator(".marker-icon")).toHaveCount(2);

  await page.click(".marker-container");
  await expect(page.locator("#card-container", { hasText: asset })).toBeVisible();

  await page.getByRole("button", { name: "View" }).click();
  await expect(page.locator(`#asset-header`, { hasText: asset })).toBeVisible();
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
