import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets, assignLocation, BBox, getAssetTypeColour, getAssetTypes, randomAsset } from "./fixtures/data/assets.js";
import { OrMap } from "@openremote/or-map/src/index.js";
import { Asset, AssetTypeInfo } from "@openremote/model";
import { OrClusterMarker } from "@openremote/or-map/lib/markers/or-cluster-marker.js";

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
  
    await expect(page.locator(".or-map-marker")).toHaveCount(2);

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
   * @then Shows cluster marker with the expected asset type colours
   * @when Zooming in
   * @then Shows cluster marker reduce to 10
   * @and Shows a normal marker
   * @when When clicking the cluster marker
   * @then Cluster marker disappears
   * @and All 10 asset markers part of the cluster are shown
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should display clustered markers", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const bbox = { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 };
    const assetInfos = (await manager.axios.request<AssetTypeInfo[]>({ url: "/model/assetInfos" })).data;
    const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
      return { ...assignLocation(randomAsset(assetInfos), bbox), name: String(i), realm: "smartcity" }
    });
    const outlierbbox = { west: 4.483812, south: 51.916359, east: 4.484017, north: 51.916495 };
    assets.push({ ...assignLocation(randomAsset(assetInfos), outlierbbox), name: "outlier", realm: "smartcity" })

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: true } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    const clusterMarker = page.locator("or-cluster-marker");
    const coords = await clusterMarker.evaluate((marker: OrClusterMarker) => [marker.lng,marker.lat]);
    await expect(clusterMarker.locator("text")).toContainText("11");
    const assetTypes = getAssetTypes(assets);
    expect(
      await clusterMarker.locator("path[fill]").evaluateAll(el => el.map(e => e.getAttribute("fill")!))
    ).toStrictEqual(assetTypes.map((t) => "#" + getAssetTypeColour(t, assetInfos)));

    // await page.locator("or-map").evaluate((map: OrMap) => map.flyTo([4.482259693115793, 51.91756799273], 17));
    await page.locator("or-map").evaluate((map: OrMap, [lng,lat]) => map.flyTo([lng!,lat!], 16.1), coords);

    await expect(clusterMarker.locator("text")).toContainText("10");
    await expect(page.locator('.or-map-marker')).toHaveCount(1);

    await clusterMarker.click();
    await expect(clusterMarker).not.toBeVisible();
    await expect(page.locator('.or-map-marker')).toHaveCount(10);
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

    const assetInfos = (await manager.axios.request<AssetTypeInfo[]>({ url: "/model/assetInfos" })).data;
    const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
      return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
    });

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: false } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    await expect(page.locator("or-cluster-marker")).not.toBeVisible();
    await expect(page.locator(".or-map-marker")).toHaveCount(10);
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
  test("should toggle asset types", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assetInfos = (await manager.axios.request<AssetTypeInfo[]>({ url: "/model/assetInfos" })).data;
    const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
      return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
    });

    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    await page.locator('or-map-legend [icon="menu"]').click();
    await expect(page.locator("or-map-legend #legend-content")).toBeVisible();

    const assetTypes = getAssetTypes(assets);

    const options = await page.locator("or-map-legend #legend-content").getByRole("listitem").all();
    for (const [i, option] of options.entries()) {
      await expect(option).toHaveAttribute("data-asset-type", assetTypes[i]);
    }

    let count = 0;
    for (const option of options) {
      const checkbox = option.getByRole("checkbox");
      await expect(checkbox).toBeChecked()
      await expect(page.locator(".or-map-marker")).toHaveCount(10 - count);

      for (const asset of assets) { if (asset.type == (await option.getAttribute("data-asset-type"))) count++ }
      option.getByRole("checkbox").uncheck();
      await expect(page.locator(".or-map-marker")).toHaveCount(10 - count);
    }
  });
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
