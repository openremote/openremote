import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import { custom } from "./fixtures/data/roles.js";
import { Role } from "@openremote/model";

test.use({ storageState: "test/fixtures/data/admin.json" });

test("Create role", async ({ page, manager, rolesPage }) => {
  // Given the Realm "smartcity" is setup
  await manager.setup("smartcity");
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
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

  await rolesPage.interceptRequest<Role[]>("**/user/master/roles", (roles) => {
    const role = roles?.find(({ name }) => name === "Custom");
    if (role) manager.role = role;
  });
  await page.click('button:has-text("create")');
  // Then We see a new role
  await expect(page.locator("text=Custom")).toHaveCount(1);
});

test("Delete role", async ({ page, manager }) => {
  // Given the Realm "smartcity" with a role is setup
  await manager.setup("smartcity", { role: custom });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // Then Navigate to "Roles" page
  await manager.navigateToMenuItem("Roles");
  // Then Delete role
  await page.click("text=Custom");
  await page.click('tr[class="attribute-meta-row expanded"] >> button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');
  // Then We should not see the Custom role
  await expect(page.locator("text=Custom")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
