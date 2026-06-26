import {
    ct as base,
    expect,
    withPage,
    type SharedComponentTestFixtures,
    type Locator,
    type Page,
} from "@openremote/test";
import type { WellknownMetaItems } from "@openremote/model";
import * as Util from "@openremote/core/lib/util";

export class AssetViewer {
    constructor(private readonly page: Page) {}

    /**
     * Switch between modify mode and view mode.
     * @param targetMode The mode to switch to
     */
    async switchMode(targetMode: "modify" | "view") {
        const targetEditMode = targetMode === "modify";
        const viewer = this.page.locator("or-asset-viewer#viewer");
        const isEditMode = await viewer.evaluate((element) => !!(element as HTMLElement & { editMode?: boolean }).editMode);
        if (isEditMode === targetEditMode) {
            return;
        }

        const selector = this.page.locator("or-asset-viewer #edit-btn").getByRole("button");
        await selector.waitFor({ state: "visible" });
        await selector.click();
        await expect(viewer).toHaveJSProperty("editMode", targetEditMode);
    }

    async expandAll() {
        await this.page.getByRole("button", { name: /Expand all|expandAll/i }).click();
    }

    getSaveButton() {
        return this.page.locator("or-asset-viewer #save-btn").getByRole("button");
    }

    async save() {
        const saveBtn = this.getSaveButton();
        const viewer = this.page.locator("or-asset-viewer#viewer");
        await Promise.all([
            viewer.evaluate((viewer) => new Promise<void>((resolve, reject) => {
                viewer.addEventListener("or-asset-viewer-save", (event) => {
                    const saveResult = (event as CustomEvent<{ success?: boolean }>).detail;
                    if (saveResult.success) {
                        resolve();
                    } else {
                        reject(new Error("Asset save failed"));
                    }
                }, { once: true });
            })),
            saveBtn.click()
        ]);
        await viewer.evaluate((element) => {
            const viewer = element as HTMLElement & { _assetInfo?: { modified: boolean }; requestUpdate?: () => void };
            if (viewer._assetInfo) {
                viewer._assetInfo.modified = false;
            }
            viewer.requestUpdate?.();
        });
    }

    /**
     * Get a locator to the asset header.
     * @param assetName The asset name
     */
    getHeaderLocator(assetName: string) {
        return this.page.locator(`#asset-header`, { hasText: assetName });
    }

    /**
     * Get a locator to an attribute.
     * @param attribute The attribute name
     */
    getAttributeLocator(attribute: string): Locator {
        return this.page.getByRole("row", { name: new RegExp(`\\b${attribute}\\b`, "i") });
    }

    /**
     * Get a locator to the specified attribute value input.
     * @param attribute The attribute name
     */
    getAttributeValueLocator(attribute: string | Locator) {
        return (typeof attribute === "string" ? this.getAttributeLocator(attribute) : attribute).locator("input");
    }

    /**
     * Add configuration items to an attribute by its name.
     * @param attribute The attribute's name
     * @param items The configuration items to add
     */
    async addConfigurationItems(attribute: string, ...items: `${WellknownMetaItems}`[]) {
        const configItemLocator = await this.expandAttribute(attribute);
        await configItemLocator.getByRole("button", { name: /Add configuration items|addMetaItems/i }).click();
        for (const item of items) {
            await this.page.locator(`li[data-value="${item}"]`).check();
        }
        await this.page.getByRole("button", { name: /^Add$|^add$/i }).click();
    }

    /**
     * Remove configuration items to an attribute by its name.
     * @param attribute The attribute's name
     * @param items The configuration items to remove
     */
    async removeConfigurationItems(attribute: string, ...items: `${WellknownMetaItems}`[]) {
        const configItemLocator = await this.expandAttribute(attribute);
        for (const item of items) {
            const byText = AssetViewer.textMatcher(Util.camelCaseToSentenceCase(item), item);
            await expect(configItemLocator.filter({ hasText: byText })).toBeVisible();
            const removeButton = configItemLocator
                .locator("[class=meta-item-wrapper]", { hasText: byText })
                .locator("button:last-child");
            await removeButton.hover({ force: true });
            await removeButton.click({ force: true });
            await expect(configItemLocator.filter({ hasText: byText })).not.toBeVisible();
        }
    }

    /**
     * Gets the locator of the attribute configuration item row.
     *
     * If an item is supplied the locator will be narrowed down to the item.
     *
     * @param attribute The attribute name
     * @param item The configuration item to narrow down to
     */
    getConfigurationItemLocator(attribute: string, item?: string): Locator {
        return item
            ? // match the sibling row i.e. the configuration items row of the attribute
              this.getAttributeLocator(attribute).locator("+ tr", { hasText: AssetViewer.textMatcher(item, AssetViewer.translationKey(item)) })
            : this.getAttributeLocator(attribute).locator("+ tr");
    }

    /**
     * Ensure that the attribute is expanded
     * @param attribute The attribute name to ensure to expand.
     * @returns The config item locator that belongs to the attribute.
     */
    async expandAttribute(attribute: string): Promise<Locator> {
        const attributeLocator = this.getAttributeLocator(attribute);
        const configItemLocator = attributeLocator.locator("+ tr");
        if (!(await configItemLocator.getAttribute("class"))?.includes("expanded")) {
            await attributeLocator.getByRole("cell", { name: new RegExp(`\\b${attribute}\\b`, "i") }).click();
            await expect(configItemLocator).toHaveClass(/expanded/);
        }
        return configItemLocator;
    }

    private static textMatcher(...values: (string | undefined)[]) {
        return new RegExp(values.filter(Boolean).map(AssetViewer.escapeRegExp).join("|"), "i");
    }

    private static translationKey(value: string) {
        return value.replace(/\s+([a-z])/g, (_, letter) => letter.toUpperCase()).replace(/\s/g, "");
    }

    private static escapeRegExp(value: string) {
        return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    }
}

interface ComponentFixtures extends SharedComponentTestFixtures {
    assetViewer: AssetViewer;
}

export const ct = base.extend<ComponentFixtures>({
    assetViewer: withPage(AssetViewer),
});
