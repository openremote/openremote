import { expect } from "@playwright/test";
import { test as setup } from "./fixtures/manager";
import { users } from "./fixtures/data/users";
import { custom } from "./fixtures/data/roles";
import permissions from "./fixtures/data/permissions";

setup("Login as admin", async ({ page, manager, context }) => {
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  await page.waitForURL("**/manager/**");
  await page.waitForTimeout(2000);
  await context.storageState({ path: "test/fixtures/data/admin.json" });
});

setup.describe(async () => {
  setup.use({ storageState: "test/fixtures/data/admin.json" });
  setup("Add realm", async ({ page, manager, realmsPage }) => {
    // Given Login to OpenRemote "master" realm as "admin"
    await manager.goToRealmStartPage("master");
    // When Navigate to "Realms" page
    await realmsPage.goto();
    // Then Add a new Realm
    await realmsPage.addRealm("smartcity");
    // When Select smartcity realm
    await realmsPage.selectRealm("smartcity");
    // Then We see the smartcity realm
    await expect(page.locator("#desktop-right #realm-picker")).toContainText("smartcity");
  });

  setup("Add user", async ({ page, manager, usersPage }) => {
    // Given the Realm "smartcity" is setup
    await manager.setup("smartcity");
    // When Login to OpenRemote "master" realm as "admin"
    await manager.goToRealmStartPage("master");
    // And switch to the smartcity realm
    await manager.switchToRealmByRealmPicker("smartcity");
    // And Navigate to "Users" page
    await manager.navigateToMenuItem("Users");
    // When Add a new user
    await usersPage.addUser(users.smartcity.username, users.smartcity.password);
    // Then We see a new user
    await expect(page.getByRole("cell", { name: "smartcity" })).toHaveCount(1);
    // When Navigate to user
    await page.getByRole("cell", { name: "smartcity" }).click();
    // Then We see that all permissions are selected
    await usersPage.toHavePermissions(...permissions);
  });
});

setup("Login as user", async ({ page, manager, context }) => {
  await manager.goToRealmStartPage("smartcity");
  await manager.login("smartcity");
  await page.waitForURL("**/manager/**");
  await context.storageState({ path: "test/fixtures/data/user.json" });
});
