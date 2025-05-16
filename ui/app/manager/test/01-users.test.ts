import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import { users } from "./fixtures/data/users.js";
import { custom } from "./fixtures/data/roles.js";

test("Add new user", async ({ page, manager, usersPage }) => {
  // Given the Realm "smartcity" is setup
  await manager.setup("smartcity");
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // When Navigate to "Users" page
  await manager.navigateToMenuItem("Users");
  // Then Add a new user
  await usersPage.addUser(users.smartcity.username, users.smartcity.password);
  // Then We see a new user
  await expect(page.locator('td:has-text("smartcity")')).toHaveCount(1);
  // When Navigate to "Roles" page
  await manager.navigateToMenuItem("Roles");
  // Then Create a new role
  await page.getByText("Add Role").click();

  // get total number of current roles
  let rows = await page.$$(".mdc-data-table__row");
  const count = await rows.length;

  await page.fill(`#attribute-meta-row-${count - 1} input[type="text"] >> nth=0`, "Custom");
  await page.fill(`#attribute-meta-row-${count - 1} input[type="text"] >> nth=1`, "read:asset, write:asset");
  await page
    .locator(`#attribute-meta-row-${count - 1}`)
    .getByText("assets: Read asset data")
    .click();
  await page
    .locator(`#attribute-meta-row-${count - 1}`)
    .getByText("assets: Write asset data")
    .click();

  await page.click('button:has-text("create")');
  // Then We see a new role
  await expect(page.locator("text=Custom")).toHaveCount(1);
  // When Navigate to "asset" tab
  await manager.navigateToTab("asset");
  // When Navigate to "Users" page
  await manager.navigateToMenuItem("Users");
  // Then Select the new role and unselect others
  await page.click('td:has-text("smartcity")');
  await usersPage.toggleUserRoles("Read", "Write", "Custom");
  await page.keyboard.press("Enter");
  // Then We see that assets permission are selected
  // TODO: fix these assertions
  await expect(page.locator('or-mwc-input[title="Read asset data"] input[type="checkbox"]')).toBeChecked();
  await expect(page.locator('or-mwc-input[title="Read asset data"] input[type="checkbox"]')).toBeDisabled();
  await expect(page.locator('or-mwc-input[title="Write asset data"] input[type="checkbox"]')).toBeChecked();
  await expect(page.locator('or-mwc-input[title="Write asset data"] input[type="checkbox"]')).toBeDisabled();
  // Then Switch back to origin
  await usersPage.toggleUserRoles("Read", "Write", "Custom");
  await page.keyboard.press("Enter");
});

test("Delete user", async ({ page, manager }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { user: users.smartcity, role: custom });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // When Navigate to "Users" page
  await manager.navigateToMenuItem("Users");
  // Then Delete user
  await page.click('td:has-text("smartcity")');
  await page.click('button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');

  // Then We should not see the "smartcity" user
  await expect(page.locator(`td:has-text("smartcity")`)).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
