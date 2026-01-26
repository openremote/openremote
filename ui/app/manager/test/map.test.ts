import { expect } from "@openremote/test";
import { adminStatePath, test, userStatePath } from "./fixtures/manager.js";
import {
    preparedAssetsWithLocation as assets,
    assignRandomLocationInArea,
    commonAttrs,
    getAssetTypeColour,
    getAssetTypes,
    getAssetAt,
    rgbToHex,
    getAssetsForAllTypes,
    getRGBColor,
} from "./fixtures/data/assets.js";
import { markers } from "./fixtures/data/manager.js";
import type { OrMap, OrClusterMarker } from "@openremote/or-map";
import { type Asset, WellknownMetaItems } from "@openremote/model";

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
  test("should show asset and navigate", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
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

  /**
   * @given 1 asset is setup with "show on dashboard" set to true in the "smartcity" realm
   * @and 1 asset is setup with "show on dashboard" set to false
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Only shows 1 marker
   * @when Clicking the marker
   * @then Shows the asset name "ShownOnMap" on the card title
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test('should not show when "show on dashboard" is disabled', async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assets = [
      ["ShownOnMap", { [WellknownMetaItems.SHOWONDASHBOARD]: true }, 4.482259693115793, 51.91756799273],
      ["NotShownOnMap", { [WellknownMetaItems.SHOWONDASHBOARD]: false }, 4.4845127486877345, 51.917435642781214]
    ].map(([name, meta, ...coordinates]) => Object.assign({ name, type: "ThingAsset", realm: "smartcity" }, {
      attributes: {
        ...commonAttrs,
        location: { meta, value: { type: "Point", coordinates } }
      }
    })) as Asset[];
    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator(".or-map-marker")).toHaveCount(1);

    await page.click(".marker-container");
    await expect(page.locator("#card-container", { hasText: assets[0].name })).toBeVisible();
  });

  /**
   * @given Assets with location and different directions are set up in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Shows 3 asset markers
   * @when Clicking each individually
   * @then Shows their name and direction value
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should show marker direction", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assets = [
      ["Thing1", 30, 4.482259693115793, 51.91756799273],
      ["Thing2", 60, 4.4845127486877345, 51.917435642781214],
      ["Thing3", 180, 4.486293735477517, 51.91605818019178]
    ].map(([name, direction, ...coordinates]) => Object.assign({ name, type: "ThingAsset", realm: "smartcity" }, {
      attributes: {
        ...commonAttrs,
        location: { value: { type: "Point", coordinates } },
        direction: { type: "direction", value: direction }
      }
    })) as Asset[];
    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator(".or-map-marker")).toHaveCount(assets.length);

    // const getDirection = (el: Element) => window.getComputedStyle(el).transform;

    for (const [i, asset] of assets.entries()) {
      await page.locator(".marker-container").nth(i).click();
      await expect(page.locator("#card-container", { hasText: asset.name })).toBeVisible();
      await expect(page.locator('#attribute-list', { hasText: asset.attributes!.direction.value })).toBeVisible();
    }
  });
})

test.describe("Marker config", () => {

  /**
   * @given Asset with location is set up in the "smartcity" realm
   * @and a marker config is configured
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then 1 asset marker is displayed on the map
   * @and It is colored black
   * @when the marker is clicked
   * @then shows the correct marker name
   * @and The attribute value is shows "-"
   * @when An update is sent to toggle the attribute value
   * @then The marker changes to yellow
   * @and The attribute value is shows "true"
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should toggle asset marker colour based on attribute value", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await page.addInitScript(manager.hijackWebSocket());

    const assets = [assignRandomLocationInArea({
      name: "Light",
      type: "ThingAsset",
      realm: "smartcity",
      attributes: { ...commonAttrs, onOff: { name: "onOff", type: "boolean" } }
    }, { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 })];
    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false }, markers: { ThingAsset: markers[0] } } } });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator(".or-map-marker")).toHaveCount(1);
    await page.click(".marker-container");
    await expect(page.locator("#card-container", { hasText: assets[0].name })).toBeVisible();
    await expect(page.locator('#attribute-list', { hasText: "-" })).toBeVisible();

    const off = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(off)).toBe(markers[0].colours.false);

    await manager.sendWebSocketEvent({ eventType: "attribute", ref: { id: manager.assets[0].id, name: "onOff" }, value: true });
    await expect(page.locator('#attribute-list', { hasText: "true" })).toBeVisible();

    const on = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(on)).toBe(markers[0].colours.true);
  });

  /**
   * @given Asset with location is set up in the "smartcity" realm
   * @and a marker config is configured
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then 1 asset marker is displayed on the map
   * @when the marker is clicked
   * @then shows the correct marker name
   * @and it has the default marker color
   * @and The attribute value is shows "-"
   * @when An update is sent with a value of 0
   * @then It is colored green
   * @and The attribute value is shows "0"
   * @when An update is sent with a value of 30
   * @then The marker changes to orange
   * @and The attribute value is shows "30"
   * @when An update is sent with a value of 40
   * @then The marker changes to red
   * @and The attribute value is shows "40"
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should display marker with label and change marker colour based on attribute range", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await page.addInitScript(manager.hijackWebSocket());

    const assets = [assignRandomLocationInArea({
      name: "Thermometer",
      type: "ThingAsset",
      realm: "smartcity",
      attributes: { ...commonAttrs, temperature: { name: "temperature", type: "number" } }
    }, { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 })]
    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { markers: { ThingAsset: markers[1] } } } });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator(".or-map-marker")).toHaveCount(1);
    await page.click(".marker-container");
    await expect(page.locator("#card-container", { hasText: assets[0].name })).toBeVisible();
    await expect(page.locator('#attribute-list', { hasText: "-" })).toBeVisible();
    await expect(page.locator(".marker-container .label", { hasText: "-" })).toBeVisible();

    const _default = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(_default)).toBe("4c4c4c");

    await manager.sendWebSocketEvent({ eventType: "attribute", ref: { id: manager.assets[0].id, name: "temperature" }, value: 0 });
    await expect(page.locator('#attribute-list', { hasText: "0" })).toBeVisible();
    await expect(page.locator(".marker-container .label", { hasText: "0" })).toBeVisible();

    const green = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(green)).toBe(markers[1].colours.ranges[0].colour);

    await manager.sendWebSocketEvent({ eventType: "attribute", ref: { id: manager.assets[0].id, name: "temperature" }, value: 30 });
    await expect(page.locator('#attribute-list', { hasText: "30" })).toBeVisible();
    await expect(page.locator(".marker-container .label", { hasText: "30" })).toBeVisible();

    const orange = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(orange)).toBe(markers[1].colours.ranges[1].colour);

    await manager.sendWebSocketEvent({ eventType: "attribute", ref: { id: manager.assets[0].id, name: "temperature" }, value: 40 });
    await expect(page.locator('#attribute-list', { hasText: "40" })).toBeVisible();
    await expect(page.locator(".marker-container .label", { hasText: "40" })).toBeVisible();

    const red = await page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor);
    expect(rgbToHex(red)).toBe(markers[1].colours.ranges[2].colour);
  });
})

test.describe("Marker clustering", () => {
  /**
   * @given Assets with location are set up in the "smartcity" realm
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
    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
    const assets = getAssetsForAllTypes(assetInfos, { bbox });
    const outlierbbox = { west: 4.483812, south: 51.916359, east: 4.484017, north: 51.916495 };
    assets.push({ ...assignRandomLocationInArea(getAssetAt(assetInfos), outlierbbox), name: "outlier", realm: "smartcity" });

    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    const clusterMarker = page.locator("or-cluster-marker");
    const coords = await clusterMarker.evaluate((marker: OrClusterMarker) => [marker.lng,marker.lat]);
    await expect(clusterMarker.locator("text")).toContainText(`${assets.length}`);
    const assetTypes = getAssetTypes(assets);
    expect(
      await clusterMarker.locator("path[fill]").evaluateAll(el => el.map(e => e.getAttribute("fill")!))
    ).toStrictEqual(assetTypes.map((t) => "#" + getAssetTypeColour(t, assetInfos)));

    // await page.locator("or-map").evaluate((map: OrMap) => map.flyTo([4.482259693115793, 51.91756799273], 17));
    await page.locator("or-map").evaluate((map: OrMap, [lng,lat]) => map.flyTo([lng!,lat!], 16.1), coords);

    await expect(clusterMarker.locator("text")).toContainText(`${assets.length}`);
    await expect(page.locator('.or-map-marker')).toHaveCount(1);

    await clusterMarker.click();
    await expect(clusterMarker).not.toBeVisible();
    await expect(page.locator('.or-map-marker')).toHaveCount(assets.length - 1);
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

    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
    const assets = getAssetsForAllTypes(assetInfos);

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } })
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));

    await expect(page.locator("or-cluster-marker")).not.toBeVisible();
    await expect(page.locator(".or-map-marker")).toHaveCount(assets.length);
  });

  test.describe(() => {
    test.use({ storageState: adminStatePath });

    /**
     * @given Assets with location are set up in the "smartcity" realm
     * @when Logging in to OpenRemote "master" realm as "admin"
     * @and Navigating to the "map" page
     * @then Shows no markers
     * @when Switching to the "smartcity" realm
     * @then Shows cluster markers
     * @when Switching back to the "master" realm
     * @then Shows no markers
     *
     * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
     */
    test("should update markers when switching realm", async ({ page, manager, browserName }) => {
      test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");
  
      const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
      const assets = getAssetsForAllTypes(assetInfos);
      await manager.setup("smartcity", { assets });

      await manager.goToRealmStartPage("master");
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator("or-cluster-marker")).not.toBeVisible();
      await expect(page.locator(".or-map-marker")).not.toBeVisible();

      await manager.switchToRealmByRealmPicker("smartcity");
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator(".or-map-marker").or(page.locator("or-cluster-marker")).first()).toBeVisible();

      await manager.switchToRealmByRealmPicker("master");
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator("or-cluster-marker")).not.toBeVisible();
      await expect(page.locator(".or-map-marker")).not.toBeVisible();
    });
  })
});

test.describe("Asset type legend", () => {
  /**
   * @given Assets with location are set up in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Show the asset type legend
   * @when clicking the open menu button
   * @then the asset type legend opens and shows the asset types
   * @and the asset types checkboxes are checked
   * @when Unchecking the asset type check boxes 1 by 1
   * @then Hides the markers
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should toggle asset types", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
    const assets = getAssetsForAllTypes(assetInfos, { limit: 3 });

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
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
      await expect(option.getByRole("checkbox")).toBeChecked()
      await expect(page.locator(".or-map-marker")).toHaveCount(assets.length - count);

      for (const asset of assets) { if (asset.type == (await option.getAttribute("data-asset-type"))) count++ }
      await option.getByRole("checkbox").uncheck();
      await expect(page.locator(".or-map-marker")).toHaveCount(assets.length - count);
    }
  });

  /**
   * @given Only 1 asset type is setup with a location in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then Shows the marker
   * @and the legend is not visible
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should not not be shown with 1 asset type", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assets = [assignRandomLocationInArea({
      name: "Thing",
      type: "ThingAsset",
      realm: "smartcity",
      attributes: { ...commonAttrs }
    }, { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 })]

    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await expect(page.locator(".or-map-marker")).toBeVisible();
    await expect(page.locator("or-map-legend")).not.toBeVisible();
  });

  test.describe(() => {
    test.use({ storageState: adminStatePath });

    /**
     * @given Assets with location are set up in the "smartcity" realm
     * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
     * @and Navigating to the "map" page
     * @then Show the asset type legend
     * @when clicking the open menu button
     * @then the asset type legend opens and shows the asset types
     * @and the asset types checkboxes are checked
     * @when Unchecking the asset type check boxes 1 by 1
     * @then Hides the markers
     * @when Switching to the "master" realm
     * @then Hides the legend
     * @and No markers are shown
     * @when Switching back to the "smartcity" realm
     * @then Shows the legend again
     * @and All asset types are checked
     * @and All markers are shown
     *
     * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
     */
    test("should reset when switching realm", async ({ page, manager, browserName }) => {
      test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

      const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
      const assets = getAssetsForAllTypes(assetInfos, { limit: 3 });

      await manager.setup("smartcity", { assets });
      await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
      await manager.goToRealmStartPage("master");
      await manager.switchToRealmByRealmPicker("smartcity");

      await expect(page.locator("or-map")).toBeVisible();
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator(".or-map-marker")).toHaveCount(assets.length);

      await page.locator('or-map-legend [icon="menu"]').click();
      await expect(page.locator("or-map-legend #legend-content")).toBeVisible();

      const assetTypes = getAssetTypes(assets);

      const options = await page.locator("or-map-legend #legend-content").getByRole("listitem").all();
      for (const [i, option] of options.entries()) {
        await expect(option).toHaveAttribute("data-asset-type", assetTypes[i]);
      }

      for (const option of options) {
        await expect(option.getByRole("checkbox")).toBeChecked()
        await option.getByRole("checkbox").uncheck();
      }
      await expect(page.locator(".or-map-marker")).toHaveCount(0);

      await manager.switchToRealmByRealmPicker("master");
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator(".or-map-marker")).toHaveCount(0);
      await expect(page.locator('or-map-legend')).not.toBeVisible();

      await manager.switchToRealmByRealmPicker("smartcity");
      await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
      await expect(page.locator(".or-map-marker")).toHaveCount(assets.length);
      await page.locator('or-map-legend [icon="menu"]').click();
      for (const option of options) {
        await expect(option.getByRole("checkbox")).toBeChecked()
      }
    });
  });
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
