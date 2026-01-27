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
     * Drag an drop a widget on to the insights page grid
     * @param type The widget type to drag and drop
     * @param location The location on the grid where to drop the widget
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
        await this.page.dragAndDrop(`#sidebar .grid-stack-item :has-text("${type}")`, ".maingrid", {
            sourcePosition: { x: 10, y: 10 }, // Depending on where you grab the widget the drop
            targetPosition: { x, y },
            steps: 50,
        });
        await expect(this.getWidgets()).toHaveCount(count + 1);
    }

    /**
     * Resize a widget to the specified grid cell coordinates
     * @param widget The locator of the widget
     * @param from The grid cell coordinates of the widget
     * @param to The grid cell coordinates to resize the widget to
     */
    async resizeWidgetAtTo(widget: Locator, [gridX, gridY] = [0, 0], [cellsWidth, cellsHeight] = [8, 8]) {
        const [cellWidth, cellHeight] = await this.getGridCellDimensions();
        const [width, height] = [cellsWidth * cellWidth, cellsHeight * cellHeight];

        await this.page.dragAndDrop(`.ui-resizable-handle.ui-resizable-se`, ".maingrid", {
            // We apply -cellWidth/Height / 2 as the handle causes the source position to be offset
            targetPosition: {
                x: gridX * cellWidth + width - cellWidth / 2,
                y: gridY * cellHeight + height - cellHeight / 2,
            },
            steps: 10,
        });

        const bbox = await widget.boundingBox();
        // The dashboard dimensions should be equal to the specified cells,
        // or between the specified cells and the specified cells - 1 cell,
        // due to margins.
        expect(bbox?.width).toBeGreaterThan(width - cellWidth);
        expect(bbox?.width).toBeLessThanOrEqual(width);
        expect(bbox?.height).toBeGreaterThan(cellHeight - cellHeight);
        expect(bbox?.height).toBeLessThanOrEqual(height);
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
