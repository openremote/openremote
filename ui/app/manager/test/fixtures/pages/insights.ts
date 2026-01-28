import {BasePage, expect, Page, Shared} from "@openremote/test";
import {Manager} from "../manager";

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

    async dragAndDropWidget(type: string, location: [number, number] = [0,0], size: [number, number] = [2, 2]) {
        await expect(this.getBuilder()).toBeVisible();
        await expect(this.getPreview()).toBeVisible();
        await expect(this.getPreview()).toHaveJSProperty("isLoading", false);
        await expect(this.getDashboardGrid()).toBeVisible();

        // Verify the slot is empty
        const isEmpty = await this.getDashboardGrid().evaluate((el: any, args) =>
            el.gridstack.isAreaEmpty(args.loc[0], args.loc[1], args.size[0], args.size[1]),
            { loc: location, size: size }
        );
        expect(isEmpty).toBeTruthy();

        // Grab the X/Y browser coordinates
        const coordinates = await this.getDashboardGrid().evaluate((el: any, args) =>
            ([args.loc[0] * args.size[0] * el.gridstack.cellWidth(), args.loc[1] * args.size[1] * el.gridstack.getCellHeight()]),
            { loc: location, size: size }
        );
        expect(coordinates).toBeDefined();

        // Manual drag and drop
        const count = await this.getWidgets().count();
        await this.page.locator("#sidebar .grid-stack-item", {hasText: type}).first().hover();
        await this.page.mouse.down();
        await this.page.mouse.move(-200, 0, { steps: 5 }); // Move tiny bit left, so the hover event is triggered
        await this.page.locator(".maingrid").hover({ position: { x: coordinates[0], y: coordinates[1] }});
        await this.page.mouse.up();
        await expect(this.getWidgets()).toHaveCount(count + 1);
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
