import { expect } from "@openremote/test";
import { adminStatePath, test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets, assignLocation, commonAttrs, getAssetTypeColour, getAssetTypes, randomAsset, rgbToHex } from "./fixtures/data/assets.js";
import { markers } from "./fixtures/data/manager.js";
import type { OrMap } from "@openremote/or-map/src/index.js";
import type { OrClusterMarker } from "@openremote/or-map/lib/markers/or-cluster-marker.js";
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
   * @given Assets with location are set up in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
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
   * @given Assets with location are set up in the "smartcity" realm
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
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

    await expect(page.locator(".or-map-marker")).toHaveCount(3);

    for (const [i, asset] of assets.entries()) {
      await page.locator(".marker-container").nth(i).click();
      await expect(page.locator("#card-container", { hasText: asset.name })).toBeVisible();
      await expect(page.locator('#attribute-list', { hasText: asset.attributes!.direction.value })).toBeVisible();
    }
  });
})

test.describe("Marker config", () => {
  const getRGBColor = (el: Element): string[] => window.getComputedStyle(el).color.match(/\d+/g)!;

  /**
   * @given Asset with location is set up in the "smartcity" realm
   * @and And a marker config is configured
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then 1 asset marker is displayed on the map
   * @and It is colored black
   * @when An update is sent to toggle the attribute value
   * @then The marker changes to yellow
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should toggle asset marker colour based on attribute value", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await page.addInitScript(() => {
      const OriginalWS = WebSocket;
      window.WebSocket = function (url: string | URL, protocols?: string | string[]) {
        return (window as any).ws = new OriginalWS(url, protocols);
      } as any
    });

    const assets = [assignLocation({
      name: "Light",
      type: "ThingAsset",
      realm: "smartcity",
      attributes: { ...commonAttrs, onOff: { name: "onOff", type: "boolean" } }
    }, { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 })]
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
   * @and And a marker config is configured
   * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
   * @and Navigating to the "map" page
   * @then 1 asset marker is displayed on the map
   * @and It is colored green
   * @when An update is sent with a value of 30
   * @then The marker changes to orange
   * @when An update is sent with a value of 40
   * @then The marker changes to red
   *
   * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
   */
  test("should display marker with label and change marker colour based on attribute range", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    await page.addInitScript(() => {
      const OriginalWS = WebSocket;
      window.WebSocket = function (url: string | URL, protocols?: string | string[]) {
        return (window as any).ws = new OriginalWS(url, protocols);
      } as any
    });

    const assets = [assignLocation({
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
    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
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

    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
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

  test.describe(() => {
    test.use({ storageState: adminStatePath });

    /**
     * @given Assets with location are set up in the "smartcity" realm
     * @and clustering is enabled
     * @when Logging in to OpenRemote "smartcity" realm as "smartcity"
     * @and Navigating to the "map" page
     * @then Shows markers
     * @when Switching to the "master" realm
     * @then Shows no markers
     *
     * @skip This test is skipped on Firefox because headless mode does not support WebGL required by maplibre
     */
    test("should update markers when switching realm", async ({ page, manager, browserName }) => {
      test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");
  
      const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
      const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
        return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
      });
      await manager.setup("smartcity", { assets });
      await manager.configureAppConfig({ pages: { map: { legend: { show: true }, clustering: { cluster: true } } } })

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

    const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
    const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
      return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
    });

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
      const checkbox = option.getByRole("checkbox");
      await expect(checkbox).toBeChecked()
      await expect(page.locator(".or-map-marker")).toHaveCount(10 - count);

      for (const asset of assets) { if (asset.type == (await option.getAttribute("data-asset-type"))) count++ }
      option.getByRole("checkbox").uncheck();
      await expect(page.locator(".or-map-marker")).toHaveCount(10 - count);
    }
  });

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
  test("should not not be shown with 1 asset type", async ({ page, manager, browserName }) => {
    test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

    const assets = [assignLocation({
      name: "Thing",
      type: "ThingAsset",
      realm: "smartcity",
      attributes: { ...commonAttrs }
    }, { west: 4.4857, south: 51.9162, east: 4.4865, north: 51.9167 })]

    await manager.setup("smartcity", { assets });
    await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
    await manager.goToRealmStartPage("smartcity");

    await expect(page.locator("or-map")).toBeVisible();
    await expect(page.locator("or-map-legend")).not.toBeVisible();
  });

  test.describe(() => {
    test.use({ storageState: adminStatePath });

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
    test("should reset when switching realm", async ({ page, manager, browserName }) => {
        test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");

        const assetInfos = (await manager.api.AssetModelResource.getAssetInfos()).data;
        const assets: Asset[] = Array.from({ length: 10 }).map((_, i) => {
          return { ...assignLocation(randomAsset(assetInfos)), name: String(i), realm: "smartcity" }
        });

        await manager.setup("smartcity", { assets });
        await manager.configureAppConfig({ pages: { map: { clustering: { cluster: false } } } });
        await manager.goToRealmStartPage("master");
        await manager.switchToRealmByRealmPicker("smartcity");

        await expect(page.locator("or-map")).toBeVisible();
        await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
        await expect(page.locator(".or-map-marker")).toHaveCount(10);

        await page.locator('or-map-legend [icon="menu"]').click();
        await expect(page.locator("or-map-legend #legend-content")).toBeVisible();

        const assetTypes = getAssetTypes(assets);

        const options = await page.locator("or-map-legend #legend-content").getByRole("listitem").all();
        for (const [i, option] of options.entries()) {
          await expect(option).toHaveAttribute("data-asset-type", assetTypes[i]);
        }

        for (const option of options) {
          const checkbox = option.getByRole("checkbox");
          await expect(checkbox).toBeChecked()
          option.getByRole("checkbox").uncheck();
        }
        await expect(page.locator(".or-map-marker")).toHaveCount(0);

        await manager.switchToRealmByRealmPicker("master");
        await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
        await expect(page.locator(".or-map-marker")).toHaveCount(0);
        await expect(page.locator('or-map-legend')).not.toBeVisible();

        await manager.switchToRealmByRealmPicker("smartcity");
        await page.locator("or-map").evaluate((map: OrMap) => map.flyTo(undefined, 10));
        await expect(page.locator(".or-map-marker")).toHaveCount(10);
        await page.locator('or-map-legend [icon="menu"]').click();
        for (const option of options) {
          const checkbox = option.getByRole("checkbox");
          await expect(checkbox).toBeChecked()
        }
    });
  });
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
