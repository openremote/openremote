import { Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class RolesPage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Roles");
  }
}
