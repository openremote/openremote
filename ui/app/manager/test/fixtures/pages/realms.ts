import { BasePage, Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class RealmsPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Realms");
  }

  /**
   * Create Realm with name
   * @param name realm name
   */
  async addRealm(name: string) {
    const locator = this.page.getByRole("cell", { name, exact: true });
    await this.page.getByRole("cell", { name: "Master", exact: true }).waitFor();
    if (await locator.isVisible()) {
      console.warn(`Realm "${name}" already present`);
    } else {
      await this.page.click("text=Add Realm");
      await this.page.locator("#realm-row-1 label").filter({ hasText: "Realm" }).fill(name);
      await this.page.locator("#realm-row-1 label").filter({ hasText: "Friendly name" }).fill(name);
      await this.page.click('button:has-text("create")');
    }
  }

  /**
   * Select realm by its name
   * @param name Realm's name
   */
  async selectRealm(realm: string) {
    await this.page.click("#realm-picker");
    await this.page.locator("#desktop-right li").filter({ hasText: realm }).click();
  }

  /**
   * Delete a certain realm by its name
   * @param name Realm's name
   */
  async deleteRealm(realm: string) {
    await this.page.getByRole("cell", { name: realm }).first().click();
    await this.page.click('button:has-text("Delete")');
    await this.page.fill('div[role="alertdialog"] input[type="text"]', realm);
    await this.page.click('button:has-text("OK")');
  }
}
