import { BasePage, Locator, Page, Shared, expect } from "@openremote/test";
import { Manager } from "../manager";
import permissions from "../data/permissions";
import { UserModel } from "../../../src/pages/page-users";

export class UsersPage implements BasePage {
  constructor(private readonly page: Page, private readonly shared: Shared, private readonly manager: Manager) {}

  async goto() {
    this.manager.navigateToMenuItem("Users");
  }

  async gotoUserCreation(realm: string, type: "serviceuser" | "regular") {
    await this.manager.goToRealmStartPage(realm);
    await this.manager.navigateToMenuItem("Users");
    if (type === "regular") {
      await this.page.getByText("Add User").click();
    } else {
      await this.page.getByText("Add Service user").click();
    }
  }

  /**
   * Get permission locator by name, optionally scoped to a container.
   * @param permission The permission name
   * @param scope Optional locator to scope the search within
   */
  getPermission(permission: string, scope: Page | Locator = this.page): Locator {
    return scope.getByRole("checkbox", { name: permission });
  }

  /**
   * Toggle roles when configuring a user, optionally scoped to a container.
   * @param scopeOrFirstRole A Locator to scope within, or the first role name (page-wide)
   * @param roles Additional role names to toggle
   */
  async toggleUserRoles(scopeOrFirstRole: Page | Locator | string, ...roles: string[]): Promise<void> {
    const scope = typeof scopeOrFirstRole !== "string" ? scopeOrFirstRole : this.page;
    const allRoles = typeof scopeOrFirstRole === "string" ? [scopeOrFirstRole, ...roles] : roles;
    const roleInput = scope.locator("or-mwc-input").filter({ hasText: "Manager roles" });
    const roleSelector = roleInput.getByRole("button", { name: "Manager roles" });
    await roleSelector.click();
    for (const role of allRoles) {
      await roleInput.locator("li").filter({ hasText: role }).click();
    }
    await roleSelector.click();
  }

  /**
   * Assert selected permissions, optionally scoped to a container.
   * @param scopeOrFirstPerm A Locator to scope within, or the first permission name (page-wide)
   * @param perms Additional permission names expected to be checked
   */
  async toHavePermissions(scopeOrFirstPerm?: Page | Locator | string, ...perms: string[]): Promise<void> {
    const scope = (scopeOrFirstPerm && typeof scopeOrFirstPerm !== "string") ? scopeOrFirstPerm : this.page;
    const allPerms = typeof scopeOrFirstPerm === "string" ? [scopeOrFirstPerm, ...perms] : perms;
    for (const p of allPerms) {
      await expect(this.getPermission(p, scope)).toBeChecked();
      await expect(this.getPermission(p, scope)).toBeDisabled();
    }
    for (const p of permissions.filter((p) => !allPerms.includes(p))) {
      await expect(this.getPermission(p, scope)).not.toBeChecked();
      await expect(this.getPermission(p, scope)).not.toBeDisabled();
    }
  }

  /**
   * Create a user with read and write access for the current realm.
   *
   * @param username The users' username
   * @param password The users' password
   * @param tag Optional tag for the user
   */
  async addUser(username: string, password: string, tag?: string) {
    await this.page.getByText("Add User").click();
    const lastRow = this.page.locator("#table-users tbody tr").last();
    await lastRow.getByRole("textbox", { name: "Username" }).fill(username);
    await lastRow.getByRole("textbox", { name: /Password/ }).fill(password);
    await lastRow.getByRole("textbox", { name: "Repeat password" }).fill(password);
    if (tag) {
      await lastRow.getByRole("textbox", { name: "Tag" }).fill(tag);
    }
    await this.toggleUserRoles(lastRow, "Read", "Write");
    await this.toHavePermissions(lastRow, ...permissions);
    await this.shared.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: "Create" }).click();
    await expect(this.page.locator("#table-users tr.attribute-meta-row.expanded")).toHaveCount(0);
  }

  /**
   * Create a service user for the current realm.
   *
   * @param username The service user's username
   * @param tag Optional tag for the service user
   */
  async addServiceUser(username: string, tag?: string): Promise<void> {
    await this.page.getByText("Add Service user").click();
    const lastRow = this.page.locator("#table-service-users tbody tr").last();
    await lastRow.getByRole("textbox", { name: "Username" }).fill(username);
    if (tag) {
      await lastRow.getByRole("textbox", { name: "Tag" }).fill(tag);
    }
    await this.toggleUserRoles(lastRow, "Read", "Write");
    await this.toHavePermissions(lastRow, ...permissions);
    await this.shared.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: "Create" }).click();
    await expect(this.page.locator("#table-service-users tr.attribute-meta-row.expanded")).toHaveCount(0);
  }

  /**
   * Search for users in the regular user table.
   * @param searchTerm The term to search for
   */
  async searchRegularUsers(searchTerm: string) {
    const panel = this.page.locator(".panel").filter({ hasText: "Regular users" }).first();
    const input = panel.locator('or-mwc-input[placeholder="Search"] input');
    await input.fill(searchTerm);
  }

  /**
   * Search for users in the service user table.
   * @param searchTerm The term to search for
   */
  async searchServiceUsers(searchTerm: string) {
    const panel = this.page.locator(".panel").filter({ hasText: "Service users" }).first();
    const input = panel.locator('or-mwc-input[placeholder="Search"] input');
    await input.fill(searchTerm);
  }
}
