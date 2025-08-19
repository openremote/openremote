import { expect } from "@openremote/test";
import { adminStatePath, test as cleanup } from "./fixtures/manager";
import { custom } from "./fixtures/data/roles";

cleanup.use({ storageState: adminStatePath });

/**
 * @given The "smartcity" realm is set up with the "custom" role
 * @when Logging into the "master" realm as "admin"
 * @and Navigating to the "Users" page
 * @and Deleting the "smartcity" user
 * @then The user "smartcity" should no longer exist
 */
cleanup(`Delete the "smartcity" user`, async ({ page, manager }) => {
  await manager.setup("smartcity", { role: custom });
  await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToMenuItem("Users");
  await page.getByRole("cell", { name: "smartcity" }).click();
  await page.getByRole("button", { name: "delete" }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "Delete" }).click();
  await expect(page.locator("td", { hasText: "smartcity" })).toHaveCount(0);
});

/**
 * @given Logged in to the "master" realm as "admin"
 * @when Navigating to the "Realms" page
 * @and Deleting the "smartcity" realm
 * @then The "smartcity" realm should no longer appear
 * @and The realm picker should no longer be visible
 */
cleanup(`Delete the "smartcity" realm`, async ({ page, manager, realmsPage }) => {
  await manager.goToRealmStartPage("master");
  await manager.navigateToMenuItem("Realms");
  await realmsPage.deleteRealm("smartcity");
  await expect(page.getByRole("cell", { name: "smartcity", exact: true })).toHaveCount(0);
  await manager.goToRealmStartPage("master");
  await page.locator("#desktop-right").waitFor();
  await expect(page.locator("#desktop-right #realm-picker")).not.toBeVisible();
});
