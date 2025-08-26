import { BasePage, Locator, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import permissions from "../data/permissions";
import { UserModel } from "../../../src/pages/page-users";

export class UsersPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Users");
  }

  /**
   * Get permission locator by name.
   * @param permission The permission name
   */
  getPermission(permission: string): Locator {
    return this.page.getByRole("checkbox", { name: permission });
  }

  /**
   * Toggle roles when configuring a user.
   * @param roles The roles to toggle
   */
  async toggleUserRoles(...roles: string[]) {
    const roleSelector = this.page.getByRole("button", { name: "Manager roles" });
    const itemSelector = this.page.locator("li");
    await roleSelector.click({ delay: 500 });
    for (const role of roles) {
      await itemSelector.filter({ hasText: role }).click();
    }
    await roleSelector.click();
  }

  /**
   * Assert selected permissions.
   * @param perms The permissions expected to be checked
   */
  async toHavePermissions(...perms: string[]) {
    for (const permisison of perms) {
      await expect(this.getPermission(permisison)).toBeChecked();
      await expect(this.getPermission(permisison)).toBeDisabled();
    }
    for (const permisison of permissions.filter((p) => !perms.includes(p))) {
      await expect(this.getPermission(permisison)).not.toBeChecked();
      await expect(this.getPermission(permisison)).not.toBeDisabled();
    }
  }

  /**
   * Create a user with read and write access for the current realm.
   *
   * Internally checks whether the permissions are correctly set and
   * registers the user for cleanup.
   *
   * @param username The users' username
   * @param password The users' password
   */
  async addUser(username: string, password: string) {
    await this.page
      .locator("#content")
      .filter({ hasText: "Regular users" })
      .getByRole("button", { name: "Add User" })
      .click();
    await this.page.locator("label").filter({ hasText: "Username" }).fill(username);
    await this.page
      .locator("label")
      .filter({ hasText: /Password/ })
      .fill(password);
    await this.page.locator("label").filter({ hasText: "Repeat password" }).fill(password);
    await this.toggleUserRoles("Read", "Write");
    await this.toHavePermissions(...permissions);
    await this.shared.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: "Create" }).click();
  }
}
