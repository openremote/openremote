import { BasePage, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";

export class RealmsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    await this.manager.navigateToMenuItem("Realms");
  }

  /**
   * Create realm with name if not already present.
   * @param name The realm name
   */
  async addRealm(name: string) {
    await this.page.getByRole("button", { name: "Master", exact: true }).waitFor();
    await this.page.getByRole("cell", { name: "master", exact: true }).first().waitFor();
    const existingRealmCell = this.page.getByRole("cell", { name, exact: true }).first();
    if (await existingRealmCell.isVisible()) {
      console.warn(`Realm "${name}" already present`);
    } else {
      await this.page.click("text=Add Realm");
      const realmRow = this.page.locator("tr.realm-row").filter({
        has: this.page.getByRole("button", { name: /Create|create/i })
      });
      const realmNameInput = realmRow.getByLabel("Realm");
      const displayNameInput = realmRow.getByLabel(/Friendly name|displayName/i);
      await expect(realmRow).toBeVisible();
      await expect(realmNameInput).toBeEditable();
      await realmNameInput.fill(name);
      await realmNameInput.dispatchEvent("change");
      await displayNameInput.fill(name);
      await displayNameInput.dispatchEvent("change");
      const response = this.page.waitForResponse((res) =>
        res.request().method() === "POST" && res.url().endsWith("/api/master/realm")
      );
      await realmRow.getByRole("button", { name: /Create|create/i }).click();
      expect((await response).status()).toBe(204);
      await expect(existingRealmCell).toBeVisible();
    }
  }

  /**
   * Delete a certain realm by its name.
   * @param name The realm's name
   */
  async deleteRealm(realm: string) {
    await this.page.getByRole("cell", { name: realm }).first().click();
    await this.page.getByRole("button", { name: "Delete" }).click();
    await this.page.getByRole("alertdialog").getByRole("textbox", { name: "Realm" }).fill(realm);
    await this.page.getByRole("button", { name: "OK" }).click();
  }
}
