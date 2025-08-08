import { expect } from "@openremote/test";
import { adminStatePath, test as setup, userStatePath } from "./fixtures/manager";
import { users } from "./fixtures/data/users";
import permissions from "./fixtures/data/permissions";

setup(`Login as "admin" user`, async ({ page, manager, context }) => {
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  await page.waitForURL("**/manager/**");
  await page.waitForTimeout(2000);
  await context.storageState({ path: adminStatePath });
});

setup.describe(async () => {
  setup.use({ storageState: adminStatePath });

  /**
   * @given Logged into the "master" realm as "admin"
   * @when Navigating to the "Realms" page
   * @and Adding a new realm "smartcity"
   * @and Selecting the "smartcity" realm
   * @then The realm picker should show "smartcity"
   */
  setup(`Add realm called "smartcity"`, async ({ page, manager, realmsPage }) => {
    await manager.goToRealmStartPage("master");
    await realmsPage.goto();
    await realmsPage.addRealm("smartcity");
    await manager.switchToRealmByRealmPicker("smartcity");
    await expect(page.locator("#desktop-right #realm-picker")).toContainText("smartcity");
  });

  /**
   * @given The "smartcity" realm is set up
   * @when Logged into the "master" realm as "admin"
   * @and Switched to the "smartcity" realm
   * @and Navigated to the "Users" page
   * @and Added a new user "smartcity"
   * @then The user "smartcity" should be visible
   * @and All default permissions should be selected
   */
  setup(`Add user called "smartcity"`, async ({ page, manager, usersPage }) => {
    await manager.setup("smartcity");
    await manager.goToRealmStartPage("master");
    await manager.switchToRealmByRealmPicker("smartcity");
    await manager.navigateToMenuItem("Users");
    await usersPage.addUser(users.smartcity.username, users.smartcity.password);
    await expect(page.getByRole("cell", { name: "smartcity" })).toHaveCount(1);
    await page.getByRole("cell", { name: "smartcity" }).click();
    await usersPage.toHavePermissions(...permissions);
  });
});

setup(`Login as "smartcity" user`, async ({ page, manager, context }) => {
  await manager.goToRealmStartPage("smartcity");
  await manager.login("smartcity");
  await page.waitForURL("**/manager/**");
  await context.storageState({ path: userStatePath });
});
