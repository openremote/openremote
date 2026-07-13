import { BasePage, Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class RealmsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Realms");
  }

  /**
   * Create realm with name if not already present.
   * @param name The realm name
   */
  async addRealm(name: string) {
    const locator = this.page.getByRole("button", { name, exact: true });
    await this.page.getByRole("button", { name: "Master", exact: true }).waitFor();
    if (await locator.isVisible()) {
      console.warn(`Realm "${name}" already present`);
    } else {
      await this.page.click("text=Add Realm");
      const realmRow = this.page.locator("#realm-row-1");
      const realmNameInput = realmRow.getByLabel("Realm");
      const displayNameInput = realmRow.getByLabel("Friendly name");
      await realmNameInput.fill(name);
      await realmNameInput.dispatchEvent("change");
      await displayNameInput.fill(name);
      await displayNameInput.dispatchEvent("change");
      await this.page.getByRole("button", { name: "create" }).click();
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
