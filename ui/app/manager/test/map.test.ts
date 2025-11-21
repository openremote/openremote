import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets, assignLocation, randomAsset } from "./fixtures/data/assets.js";
import { OrMap } from "@openremote/or-map/src/index.js";
import { Asset, AssetTypeInfo } from "@openremote/model";

test.use({ storageState: userStatePath });

test.describe("Map markers", () => {
  /**
   * @given Assets with location are set up in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then 2 asset markers are displayed on the map
   * @when Clicking on the asset "Battery" marker
   * @then The asset card is displayed
   * @when Clicking "View"
   * @then Navigates to the asset detail page from the asset card
   * @and Shows the asset name "Battery" on the page header
   * @and Displays a default marker on the map preview
   * @when Clicking "Modify"
   * @and the location attribute input icon
   * @Then Displays a default marker on the map preview
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should show asset markers and navigate", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");

    const asset = assets[0].name;
  
    await expect(page.locator(".marker-icon")).toHaveCount(2);

    await page.click(".marker-container");
    await expect(page.locator("#card-container", { hasText: asset })).toBeVisible();
  
    await page.getByRole("button", { name: "View" }).click();
    await expect(page.locator(`#asset-header`, { hasText: asset })).toBeVisible();
    await expect(page.locator("or-map").locator(".marker-container")).toBeVisible();

    await page.getByRole("button", { name: "Modify" }).click();
    await page.locator("or-mwc-input[icon=crosshairs-gps]").click();
    await expect(page.locator("or-map").locator(".marker-container")).toBeVisible();
  });
})

test.describe("Marker clustering", () => {
  test.use({ storageState: userStatePath });

  /**
   * @given Assets with location are set up in the "smartcity" realm
   * @and clustering is configured
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Shows cluster marker
   * @when Zooming in
   * @then Shows cluster markers resize based on cluster size
   * @and Shows normal markers
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should display clustered markers", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assetInfos = (await manager.axios.request<AssetTypeInfo[]>({ url: "/model/assetInfos" })).data
    const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
      return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
    });
    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: true } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();

    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    const clusters = await page.locator("or-map").evaluate(
      (map: OrMap) => map.getMapLibre()
        ?.querySourceFeatures('mapPoints')
        .filter((feature) => feature.properties.cluster)
        .map(({ geometry }) => (geometry as any).coordinates)
    );

    expect(clusters).not.toBeUndefined();
    console.log(clusters)

    const clusterMarker = page.locator("or-cluster-marker");
    await expect(clusterMarker.locator("text")).toContainText("2");

    const colors = await clusterMarker.locator("path[fill]").evaluateAll(el => el.map(e => e.getAttribute("fill")!))
    expect(colors).toStrictEqual(["#"]); // default app color

    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo([4.482259693115793, 51.91756799273], 17));
    await expect(clusterMarker).not.toBeVisible();
    await expect(page.locator(".marker-icon")).toHaveCount(2);
  });

  /**
   * @given Assets with location are set up in the "smartcity" realm
   * @and clustering is disabled
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Shows asset markers
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should not display clustered markers", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: false } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator(".marker-icon")).toHaveCount(2);
  });

  /**
   * @todo Implement feature
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test.fixme("should allow clicking cluster markers", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: true } } } })
    await manager.goToRealmStartPage("smartcity");
  });
});

test.describe("Asset type legend", () => {
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
  test.fixme("Map legend", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: false } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map-legend")).toBeVisible();
  });
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
