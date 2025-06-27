import { Page, Shared } from "@openremote/test";
import { Manager } from "../manager";

export class RulesPage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToTab("Rules");
  }
}
