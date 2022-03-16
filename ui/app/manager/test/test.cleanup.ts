import { expect } from "@openremote/test";
import { adminStatePath, test as cleanup } from "./fixtures/manager";
import { custom } from "./fixtures/data/roles";

cleanup.use({ storageState: adminStatePath });

cleanup("Delete user", async ({ page, manager }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { role: custom });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
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

cleanup("Delete realm", async ({ page, manager, realmsPage }) => {
  // Given Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  // When Navigate to "Realms" page
  await manager.navigateToMenuItem("Realms");
  // Then Delete realm
  await realmsPage.deleteRealm("smartcity");
  // Then wait for the realm to be deleted
  await expect(page.getByRole("cell", { name: "smartcity", exact: true })).toHaveCount(0);
  // Then the realm picker should not be shown
  await manager.goToRealmStartPage("master");
  await page.locator("#desktop-right").waitFor();
  await expect(page.locator("#desktop-right #realm-picker")).not.toBeVisible();
});
