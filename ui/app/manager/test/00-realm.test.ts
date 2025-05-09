import { expect } from "@playwright/test";
import { test } from "./fixtures/test.js";

test.beforeEach(async ({ goToRealmStartPage, login }) => {
  await goToRealmStartPage("master");
  // When Login to OpenRemote "master" realm as "admin"
  await login("admin");
});

test("Add new Realm", async ({ page, addRealm, navigateToMenuItem }) => {
  // When Navigate to "Realms" page
  await page.waitForTimeout(200);
  await navigateToMenuItem("Realms");
  await page.waitForTimeout(200);
  // Then Add a new Realm
  await addRealm("smartcity", true);
  // When Select smartcity realm
  await page.click("#realm-picker");
  await page.click('li[role="menuitem"]:has-text("smartcity")');
  // Then We see the smartcity realm
  await expect(page.locator("#desktop-right #realm-picker")).toContainText("smartcity");
});
