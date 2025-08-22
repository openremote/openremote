import type { Locator, Page } from "@openremote/test";
import type { WellknownMetaItems } from "@openremote/model";
import * as Util from "@openremote/core/lib/util";

export class AssetViewer {
  constructor(private readonly page: Page) {}

  /**
   * Switch between modify mode and view mode.
   * @param targetMode The mode to switch to
   */
  async switchMode(targetMode: "modify" | "view") {
    const selector = this.page.getByRole("button", { name: targetMode });
    await selector.waitFor({ state: "visible" });
    await selector.click();
  }

  /**
   * Get a locator to an attribute.
   * @param attribute The attribute name
   */
  getAttributeLocator(attribute: string): Locator {
    return this.page.getByRole("row", { name: new RegExp(`\\b${attribute}\\b`, "i") });
  }

  /**
   * Set a value for a given attribute.
   * @param attribute The attribute name
   * @param value The value to set
   */
  async setAttributeValue(attribute: string, value: string) {
    await this.getAttributeLocator(attribute).locator("input").fill(value);
  }

  /**
   * Add configuration items to an attribute by its name.
   * @param attribute The attribute's name
   * @param items The configuration items to add
   */
  async addConfigurationItems(attribute: string, ...items: `${WellknownMetaItems}`[]) {
    const locator = this.getAttributeLocator(attribute);
    // TODO: handle expanded and non expand configuration items programmatically
    // if (await this.page.getByRole("button", { name: "Expand all" }).isVisible()) {
    //   await locator.getByRole("cell", { name: new RegExp(`\\b${attribute}\\b`, "i") }).click();
    // }
    await locator.locator("+ tr").getByRole("button", { name: "Add configuration items" }).click();
    for (const item of items) {
      await this.page
        .locator("li")
        .filter({ hasText: Util.camelCaseToSentenceCase(item) })
        .check();
    }
    await this.page.getByRole("button", { name: "Add", exact: true }).click();
    // // close attribute menu
    // await this.page.click(`td:has-text("${attr}") >> nth=0`);
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
        this.getAttributeLocator(attribute).locator("+ tr", { hasText: new RegExp(`\\b${item}\\b`, "i") })
      : this.getAttributeLocator(attribute).locator("+ tr");
  }
}
