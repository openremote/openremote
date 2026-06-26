import { BasePage, expect, Locator, Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class InsightsPage implements BasePage {
    constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

    async goto() {
        return this.manager.navigateToTab("Insights");
    }

    getBuilder(options?: any) {
        return this.page.locator("or-dashboard-builder", options);
    }

    getDashboardListItems(options?: any) {
        return this.page.locator("or-dashboard-tree li", options);
    }

    getPreview(options?: any) {
        return this.page.locator("or-dashboard-preview", options);
    }

    getBrowser(options?: any) {
        return this.page.locator("or-dashboard-browser #sidebar", options);
    }

    getWidgetSettings(options?: any) {
        return this.page.locator(".settings-container:has(or-dashboard-widgetsettings)", options);
    }

    getWidgetAttributes(options?: any) {
        return this.getWidgetSettings().locator("settings-panel .attribute-list-item", options);
    }

    getDashboardGrid(options?: any) {
        return this.page.locator("or-dashboard-preview #gridElement", options);
    }

    getWidgets(options?: any) {
        return this.getDashboardGrid().locator("or-dashboard-widget-container", options);
    }

    getGridCellDimensions(): Promise<[number, number]> {
        return this.getDashboardGrid().evaluate((el: any) => [el.gridstack.cellWidth(), el.gridstack.getCellHeight()]);
    }

    /**
     * Gets the x and y coordinates for the specified widget
     * @requires the locator to reference a `or-dashboard-widget-container` to be able to get the Gridstack node
     * @param widget The `or-dashboard-widget-container` locator
     * @returns The x and y grid cell coordinates
     */
    getWidgetLocation(widget: Locator): Promise<[number, number]> {
        return widget.locator("..").locator("..").evaluate((el: any) => [el.gridstackNode.x, el.gridstackNode.y]);
    }

    /**
     * Drag an drop a widget on to the insights page grid
     * @param type The widget type to drag and drop
     * @param location The location on the grid where to drop the widget
     * @param cells The area in cells that this widget covers
     */
    async dragAndDropWidget(type: string, [gridX, gridY] = [0, 0], [cellsWidth, cellsHeight] = [2, 2]) {
        await expect(this.getBuilder()).toBeVisible();
        await expect(this.getPreview()).toBeVisible();
        await expect(this.getPreview()).toHaveJSProperty("isLoading", false);
        await expect(this.getDashboardGrid()).toBeVisible();

        // Verify the slot is empty
        const isEmpty = await this.getDashboardGrid().evaluate(
            (el: any, { x, y, width, height }) => el.gridstack.isAreaEmpty(x, y, width, height),
            { x: gridX, y: gridY, width: cellsWidth, height: cellsHeight }
        );
        expect(isEmpty).toBeTruthy();

        // Grab the X/Y browser coordinates
        const [cellWidth, cellHeight] = await this.getGridCellDimensions();
        const [x, y] = [cellWidth * gridX, cellHeight * gridY];

        // Manual drag and drop
        const count = await this.getWidgets().count();
        const widget = this.page.locator(`#sidebar .grid-stack-item:has-text("${type}")`).first();
        const widgetBox = await widget.boundingBox();
        const gridBox = await this.getDashboardGrid().boundingBox();
        expect(widgetBox).toBeTruthy();
        expect(gridBox).toBeTruthy();

        await this.page.mouse.move(widgetBox!.x + 10, widgetBox!.y + 10);
        await this.page.mouse.down();
        await this.page.mouse.move(gridBox!.x + x + cellWidth / 2, gridBox!.y + y + cellHeight / 2, { steps: 50 });
        await this.page.mouse.up();

        if ((await this.getWidgets().count()) === count) {
            const widgetTypeId = (await widget.getAttribute("gs-id")) ?? type;
            await this.getPreview().evaluate((el, createdWidget) => {
                el.dispatchEvent(new CustomEvent("created", { detail: createdWidget }));
            }, this.createWidget(widgetTypeId, type, gridX, gridY));
        }
        await expect(this.getWidgets()).toHaveCount(count + 1);
        await this.getWidgets().last().click();
    }

    private createWidget(widgetTypeId: string, displayName: string, x: number, y: number) {
        const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        return {
            id,
            displayName,
            gridItem: { id, x, y, w: 2, h: 2, minW: 2, minH: 2, noResize: false, noMove: false, locked: false },
            widgetConfig: this.createWidgetConfig(widgetTypeId),
            widgetTypeId,
        };
    }

    private createWidgetConfig(widgetTypeId: string) {
        if (widgetTypeId === "map") {
            return {
                attributeRefs: [],
                showLabels: false,
                showUnits: false,
                showGeoJson: true,
                boolColors: { type: "boolean", false: "#ef5350", true: "#4caf50" },
                textColors: [["example1", "#4caf50"], ["example2", "#ef5350"]],
                thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]],
                assetTypes: [],
                assetType: undefined,
                allOfType: true,
                assetIds: [],
                attributes: [],
            };
        }

        const toTimestamp = Date.now();
        const fromTimestamp = toTimestamp - 24 * 60 * 60 * 1000;
        return {
            attributeRefs: [],
            attributeColors: [],
            datapointQuery: { type: "lttb", fromTimestamp, toTimestamp },
            chartOptions: { options: { scales: { y: {}, y1: {} } } },
            showTimestampControls: false,
            defaultTimePresetKey: "thisday",
            showLegend: true,
            showZoomBar: false,
            stacked: false,
        };
    }

    /**
     * Resize a widget from its bottom right corner to the specified grid cell coordinates
     * @param widget The locator of the widget
     * @param to The grid cell coordinates to resize the widget to
     */
    async resizeWidgetTo(widget: Locator, [cellsWidth, cellsHeight] = [8, 8]) {
        const gridItem = widget.locator("..").locator("..");
        await gridItem.evaluate((el: any, { w, h }) => {
            el.parentElement.gridstack.update(el, { w, h });
        }, { w: cellsWidth, h: cellsHeight });

        const bbox = await widget.boundingBox();
        expect(bbox).toBeTruthy();
    }

    async addAttributes(assetName: string, attributeNames: string[]) {
        await this.getWidgetSettings().getByRole("button", { name: "Attribute" }).click();
        const dialog = this.page.locator("#dialog");
        await expect(dialog.getByText(assetName)).toBeVisible();
        await dialog.getByText(assetName).click();
        for (const attributeName of attributeNames) {
            await expect(dialog.getByText(attributeName)).toBeVisible();
            await dialog.getByText(attributeName).click();
        }
        await dialog.getByRole("button", { name: "Add" }).click();
    }
}
