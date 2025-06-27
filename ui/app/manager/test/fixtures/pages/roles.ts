import { BasePage, Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class RolesPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Roles");
  }
}
