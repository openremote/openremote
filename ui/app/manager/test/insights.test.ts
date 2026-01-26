import { test, userStatePath } from "./fixtures/manager";
import { Dashboard } from "@openremote/model";
import {
    preparedAssetsForInsights as assets,
    assignLocation,
    commonAttrs,
    getRGBColor,
    rgbToHex,
} from "./fixtures/data/assets.js";
import { expect } from "@openremote/test";

test.use({ storageState: userStatePath });

test("Create a Line Chart widget", async ({ manager, shared, page, insightsPage }) => {
    await shared.interceptResponse<Dashboard>("**/dashboard", (dashboard) => {
        if (dashboard) manager.dashboards.push(dashboard.id!);
    });

    await manager.setup("smartcity", { assets });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Insights");

    // Drag and drop Line Chart widget
    await expect(insightsPage.getDashboardListItems()).toHaveCount(0);
    await page.click(".mdi-plus >> nth=0");
    await expect(insightsPage.getDashboardListItems()).toHaveCount(1);
    await expect(insightsPage.getWidgets()).toHaveCount(0);
    await insightsPage.dragAndDropWidget("Line Chart");
    await expect(insightsPage.getWidgets()).toHaveCount(1);

    // Make widget larger by resizing it using the handle
    const chartWidget = insightsPage.getWidgets({ hasText: "Line Chart" }).first();
    await expect(chartWidget).toBeVisible();
    await chartWidget.hover();
    const handle = page.locator(".ui-resizable-handle.ui-resizable-se").first();
    await expect(handle).toBeVisible();
    await handle.hover();

    const box = await chartWidget.boundingBox();
    const startX = box!.x + box!.width - 4;
    const startY = box!.y + box!.height - 4;
    await page.mouse.down();
    await page.mouse.move(startX + (box!.width * 2), startY + (box!.height * 2), { steps: 10 });
    await page.mouse.up();
    await page.waitForTimeout(100);
    await expect(chartWidget).not.toContainClass("ui-resizable-resizing");
    await expect.poll(async () => (await chartWidget.boundingBox())!.width).toBeGreaterThan(box!.width * 3);
    await expect.poll(async () => (await chartWidget.boundingBox())!.height).toBeGreaterThan(box!.height * 3);
    await expect(insightsPage.getWidgets()).toHaveCount(1);

    // Select widget, and add attribute of Asset #2 and Asset #1
    const attrName = "Energy level";
    await expect(chartWidget).toBeVisible();
    await chartWidget.click();
    await expect(insightsPage.getBrowser()).toBeHidden();
    await expect(insightsPage.getWidgetSettings({ hasText: "Line Chart" })).toBeVisible();
    await insightsPage.addAttributes(assets[1].name!, [attrName]);
    await expect(insightsPage.getWidgetAttributes().getByText(assets[1].name!).first()).toBeVisible();
    await expect(insightsPage.getWidgetAttributes().getByText(attrName)).toHaveCount(1);
    await expect(insightsPage.getWidgetAttributes()).toHaveCount(1);
    await insightsPage.addAttributes(assets[0].name!, ["Power", attrName]);
    await expect(insightsPage.getWidgetAttributes().getByText(assets[0].name!).first()).toBeVisible();
    await expect(insightsPage.getWidgetAttributes().getByText(attrName)).toHaveCount(2);
    await expect(insightsPage.getWidgetAttributes().getByText("Power")).toHaveCount(1);
    await expect(insightsPage.getWidgetAttributes()).toHaveCount(3);

    // Save the dashboard
    await shared.interceptResponse<Dashboard>("**/dashboard", async (_dashboard, _request, response) => {
        expect(response?.status()).toBe(200);
        expect(await response?.json()).toBeDefined();
    });
    await page.getByRole("button", { name: "Save" }).click();
    await expect(page.getByRole("button", { name: "Save" })).toBeDisabled();

    // Select the widget and do final checks
    await chartWidget.click();
    await expect(insightsPage.getWidgetAttributes()).toHaveCount(3);
    await expect(insightsPage.getWidgetAttributes().first()).toContainText(assets[1].name!);
    await expect(insightsPage.getWidgetAttributes().first()).toContainText(attrName);
    await expect(insightsPage.getWidgetAttributes().last()).toContainText(assets[0].name!);
    await expect(insightsPage.getWidgetAttributes().last()).toContainText("Power");
});

test("Create a Map widget", async ({ manager, shared, page, insightsPage, mwcMenu, mwcInput }) => {
    await shared.interceptResponse<Dashboard>("**/dashboard", (dashboard) => {
        if (dashboard) manager.dashboards.push(dashboard.id!);
    });

    await page.addInitScript(manager.hijackWebSocket());

    await manager.setup("smartcity", {
        assets: [
            assignLocation({
                name: "Thing",
                type: "ThingAsset",
                realm: "smartcity",
                attributes: { ...commonAttrs },
            }),
        ],
    });
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Insights");

    // Drag and drop Map widget
    await expect(insightsPage.getDashboardListItems()).toHaveCount(0);
    await page.click(".mdi-plus >> nth=0");
    await expect(insightsPage.getDashboardListItems()).toHaveCount(1);
    await expect(insightsPage.getWidgets()).toHaveCount(0);
    await insightsPage.dragAndDropWidget("Map");
    await expect(insightsPage.getWidgets()).toHaveCount(1);

    // Make widget larger by resizing it using the handle
    const mapWidget = insightsPage.getWidgets({ hasText: "Map" }).first();
    await expect(mapWidget).toBeVisible();
    mapWidget.hover();
    const handle = page.locator(".ui-resizable-handle.ui-resizable-se").first();
    await expect(handle).toBeVisible();
    await handle.hover();

    const box = await mapWidget.boundingBox();
    const startX = box!.x + box!.width - 4;
    const startY = box!.y + box!.height - 4;
    await page.mouse.down();
    await page.mouse.move(startX + box!.width * 2, startY + box!.height * 2, { steps: 10 });
    await page.mouse.up();
    // await expect.poll(async () => (await chartWidget.boundingBox())!.width).toBeGreaterThan(box!.width * 3.95);
    // await expect.poll(async () => (await chartWidget.boundingBox())!.height).toBeGreaterThan(box!.height * 3.95);
    await expect(insightsPage.getWidgets()).toHaveCount(1);
    await expect(page.locator(".or-map-marker")).not.toBeVisible();

    // Select widget, and add attribute
    await mapWidget.click();
    await expect(insightsPage.getBrowser()).toBeHidden();
    await expect(insightsPage.getWidgetSettings({ hasText: "Map" })).toBeVisible();
    await page.getByRole("textbox", { name: "Asset type" }).click();
    await mwcMenu.getMenuItem("Thing asset").click();
    await insightsPage.getWidgetSettings().getByRole("button", { name: "Attribute" }).click();
    await mwcInput.getSelectInputOption("Notes", insightsPage.getWidgetSettings()).click();
    await expect(page.locator(".or-map-marker")).toBeVisible();

    // Check default text configuration is correct
    const thresholdPanel = insightsPage.getWidgetSettings().locator("thresholds-panel");
    const textThresholds = mwcInput.getInputByType("text", thresholdPanel);
    const thresholdsColors = mwcInput.getInputByType("color", thresholdPanel);
    await expect(textThresholds).toHaveCount(2);
    await expect(textThresholds.first()).toHaveValue("example1");
    await expect(textThresholds.last()).toHaveValue("example2");
    await expect(thresholdsColors).toHaveCount(2);
    await expect(thresholdsColors.first()).toHaveValue("#4caf50");
    await expect(thresholdsColors.last()).toHaveValue("#ff9800");

    await expect
        .poll(() => page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor).then(rgbToHex))
        .toBe("4c4c4c");

    // Update attribute to match the first threshold
    await manager.sendWebSocketEvent({
        eventType: "attribute",
        ref: { id: manager.assets[0].id, name: "notes" },
        value: "example1",
    });
    await page.getByTitle("Refresh", { exact: true }).click();
    await expect(page.locator(".or-map-marker")).toBeVisible();

    await expect
        .poll(() => page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor).then(rgbToHex))
        .toBe("4caf50");

    await mapWidget.click();
    await thresholdsColors.last().fill("#000000");

    // Update attribute to match the second threshold
    await manager.sendWebSocketEvent({
        eventType: "attribute",
        ref: { id: manager.assets[0].id, name: "notes" },
        value: "example2",
    });
    await page.getByTitle("Refresh", { exact: true }).click();
    await expect(page.locator(".or-map-marker")).toBeVisible();
    await expect
        .poll(() => page.locator('or-icon[icon="or:marker"]').evaluate(getRGBColor).then(rgbToHex))
        .toBe("000000");
});

test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});
