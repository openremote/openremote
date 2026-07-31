/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { expect } from "@openremote/test";
import { adminStatePath, test } from "./fixtures/manager.js";
import { custom } from "./fixtures/data/roles.js";
import type { Role } from "@openremote/model";
import permissions from "./fixtures/data/permissions.js";

test.use({ storageState: adminStatePath });

/**
 * @given The realm "smartcity" is set up
 * @when Logging into OpenRemote "master" realm as "admin"
 * @and Switching to the "smartcity" realm
 * @and Navigating to the "Roles" page
 * @and Creating a new role named "Custom" with specific permissions
 * @and Navigating to the "Users" page and selecting a user
 * @and Assigning only the "Custom" role to the user and verifying permissions
 * @and Switching back to original permissions and verifying all permissions are selected
 * @then The new role is created and assigned correctly with expected permissions
 */
test("Create a new role, assign it to a user, and verify permissions", async ({ page, manager, shared, usersPage }) => {
  await manager.setup("smartcity");
  await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToMenuItem("Roles");
  await page.getByText("Add Role").click();

  const lastRow = page.locator("#table-roles tbody tr").last();
  await lastRow.getByRole("textbox", { name: "Role" }).fill("Custom");
  await lastRow.getByRole("textbox", { name: "Description" }).fill("read:asset, write:asset");
  await lastRow.getByRole("checkbox", { name: "assets: Read asset data" }).click();
  await lastRow.getByRole("checkbox", { name: "assets: Write asset data" }).click();

  await shared.interceptRequest<Role[]>("**/user/master/roles", (roles) => {
    const role = roles?.find(({ name }) => name === "Custom");
    if (role) manager.role = role;
  });
  await page.getByRole("button", { name: "create" }).click();
  await expect(page.getByText("Custom").first()).toBeVisible();

  await manager.navigateToMenuItem("Users");
  await page.getByRole("cell", { name: "smartcity" }).click();

  await usersPage.toggleUserRoles("Read", "Write", "Custom");
  await usersPage.toHavePermissions("read:assets", "write:assets");

  await usersPage.toggleUserRoles("Read", "Write", "Custom");
  await usersPage.toHavePermissions(...permissions);
});

/**
 * @given The realm "smartcity" with a role named "Custom" is set up
 * @when Logging into OpenRemote "master" realm as "admin"
 * @and Switching to the "smartcity" realm
 * @and Navigating to the "Roles" page
 * @and Deleting the "Custom" role
 * @then The "Custom" role should no longer be visible in the roles list
 */
test("Delete an existing role and verify it no longer appears", async ({ page, manager }) => {
  await manager.setup("smartcity", { role: custom });
  await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToMenuItem("Roles");

  const row = page.locator("#table-roles tbody tr", { hasText: "Custom" });
  await row.click();
  await row.locator("+ tr").getByRole("button", { name: "Delete" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Delete" }).click();
  await expect(page.locator("text=Custom")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
