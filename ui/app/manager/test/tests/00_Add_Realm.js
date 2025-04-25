import { expect } from "@playwright/test";
import { test } from "../fixtures/test";

test.beforeEach(async ({ openRealm, login }) => {
  await openRealm("master");
  login("admin");
});

test("Add Realm", async ({ page, addRealm }) => {
  // When Login to OpenRemote "master" realm as "admin"
  // When Navigate to "Realms" page
  // Then Add a new Realm
  await addRealm("smartcity", true);
  // When Select smartcity realm
  await page.click("#realm-picker");
  await page.click('li[role="menuitem"]:has-text("smartcity")');
  // Then We see the smartcity realm
  expect(page.locator('div[id="realm-picker"]')).toContainText("smartcity");
});
