import { BasePage, Locator, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import permissions from "../data/permissions";
import { UserModel } from "../../../src/pages/page-users";

export class UsersPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    await this.manager.navigateToMenuItem("Users");
  }

  async gotoUserCreation(realm: string, type: "serviceuser" | "regular") {
      return this.page.goto(this.manager.getAppUrl(realm) + `#/users/new/${type}`);
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
    const roleSelector = this.page.locator("or-vaadin-multi-select-combo-box", { hasText: /Manager roles|manager_role_plural/i });
    await roleSelector.click();
    for (const role of roles) {
      await this.page.getByRole("option", { name: new RegExp(`^\\s*${escapeRegExp(role)}\\s*$`, "i") }).click();
    }
    await roleSelector.locator("#toggleButton").click();
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
   * @param tag Optional tag for the user
   */
  async addUser(username: string, password: string, tag?: string) {
    await this.page
      .locator("#content")
      .filter({ hasText: /Regular users|regularUser_plural/i })
      .getByRole("button", { name: /Add User|add user/i })
      .click();
    await this.page.getByLabel(/Username|username/i, { exact: true }).fill(username);
    await this.page.getByLabel(/^Password$|^password$/i, { exact: true }).fill(password);
    await this.page.getByLabel(/Repeat password|repeatPassword/i, { exact: true }).fill(password);
    if (tag) {
      await this.page.getByLabel(/Tag|tag/i, { exact: true }).fill(tag);
    }
    await this.toggleUserRoles("Read", "Write");
    await this.toHavePermissions(...permissions);
    await this.shared.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: /Create|create/i }).click();
  }

  /**
   * Create a service user for the current realm.
   *
   * @param username The service user's username
   * @param tag Optional tag for the service user
   */
  async addServiceUser(username: string, tag?: string): Promise<void> {
    await this.page
      .locator("#content")
      .filter({ hasText: /Service users|serviceUser_plural/i })
      .getByRole("button", { name: /Add User|add user/i })
      .click();
    await this.page.getByLabel(/Username|username/i).fill(username);
    if (tag) {
      await this.page.getByLabel(/Tag|tag/i).fill(tag);
    }
    await this.toggleUserRoles("Read", "Write");
    await this.toHavePermissions(...permissions);
    await this.shared.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: /Create|create/i }).click();
  }

  /**
   * Search for users in the regular user table.
   * @param searchTerm The term to search for
   */
  async searchRegularUsers(searchTerm: string) {
    const panel = this.page.locator("#content").filter({ hasText: /Regular users|regularUser_plural/i }).first();
    const input = panel.locator('*[placeholder="Search"] input');
    await input.fill(searchTerm);
  }

  /**
   * Search for users in the service user table.
   * @param searchTerm The term to search for
   */
  async searchServiceUsers(searchTerm: string) {
    const panel = this.page.locator("#content").filter({ hasText: /Service users|serviceUser_plural/i }).first();
    const input = panel.locator('*[placeholder="Search"] input');
    await input.fill(searchTerm);
  }
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
