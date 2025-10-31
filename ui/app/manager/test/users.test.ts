import { expect } from "@openremote/test";
import { adminStatePath, test as setup, userStatePath } from "./fixtures/manager";
import { users } from "./fixtures/data/users";
import permissions from "./fixtures/data/permissions";


test.use({ storageState: adminStatePath });

/**
 * @given Logged into the "smartcity" realm as "admin"
 * @and Navigated to the "Users" page
 * @when Creating multiple regular users with different tags
 * @and Searching by tag in the regular user table
 * @then Only the user with the matching tag should be visible
 */
test(`Create regular users with tags and search by tag`, async ({ page, manager, usersPage }) => {await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToMenuItem("Users");
  await usersPage.addUser("user1", "password123", "Company1");
  await usersPage.addUser("user2", "password123", "Company2");
  await expect(page.getByRole("cell", { name: "user1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "user2", exact: true })).toBeVisible();

  await usersPage.searchRegularUsers("Company1");
  await expect(page.getByRole("cell", { name: "user1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "user2", exact: true })).not.toBeVisible();

  await usersPage.searchRegularUsers("Company2");
  await expect(page.getByRole("cell", { name: "user1", exact: true })).not.toBeVisible();
  await expect(page.getByRole("cell", { name: "user2", exact: true })).toBeVisible();

  await usersPage.searchRegularUsers("");
  await expect(page.getByRole("cell", { name: "user1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "user2", exact: true })).toBeVisible();
});

/**
 * @given Logged into the "smartcity" realm as "admin"
 * @and Navigated to the "Users" page
 * @when Creating multiple service users with different tags
 * @and Searching by tag in the service user table
 * @then Only the service user with the matching tag should be visible
 */
test(`Create service users with tags and search by tag`, async ({ page, manager, usersPage }) => {
  await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToMenuItem("Users");
  await usersPage.addServiceUser("serviceuser1", "api");
  await usersPage.addServiceUser("serviceuser2", "integration");
  await expect(page.getByRole("cell", { name: "serviceuser1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "serviceuser2", exact: true })).toBeVisible();

  await usersPage.searchServiceUsers("api");
  await expect(page.getByRole("cell", { name: "serviceuser1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "serviceuser2", exact: true })).not.toBeVisible();

  await usersPage.searchServiceUsers("integration");
  await expect(page.getByRole("cell", { name: "serviceuser1", exact: true })).not.toBeVisible();
  await expect(page.getByRole("cell", { name: "serviceuser2", exact: true })).toBeVisible();

  await usersPage.searchServiceUsers("");
  await expect(page.getByRole("cell", { name: "serviceuser1", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "serviceuser2", exact: true })).toBeVisible();
});

/**
 * @given Logged into the "smartcity" realm as "admin"
 * @when Creating a gateway asset
 * @and Waiting for the clientId to be populated
 * @and Navigating to the gateway service user
 * @then All fields should be visible but read-only and no save button should be available
 */
test(`Verify gateway service user is read-only`, async ({ page, manager, usersPage, assetsPage }) => {
  await manager.goToRealmStartPage("master");
  await manager.switchToRealmByRealmPicker("smartcity");
  await manager.navigateToTab("Assets");
  await assetsPage.addAsset("GatewayAsset", "TestGateway");
  await page.waitForURL("**/assets/**");
  const clientIdInput = page.locator(`#field-clientId input`);
  await expect(clientIdInput).not.toHaveValue("", { timeout: 15000 });
  const clientId = await clientIdInput.inputValue();
  expect(clientId).toBeTruthy();
  expect(clientId).toContain("gateway-");
  await manager.navigateToMenuItem("Users");
  const gatewayUsername = `${clientId}`;
  await usersPage.searchServiceUsers(gatewayUsername);
  await expect(page.getByRole("cell", { name: gatewayUsername, exact: true })).toBeVisible();
  await page.getByRole("cell", { name: gatewayUsername, exact: true }).click();
  const usernameInput = page.locator('[id*="username"]');
  await expect(usernameInput).toHaveAttribute("readonly");
  const tagInput = page.locator('or-mwc-input#new-tag');
  await expect(tagInput).toHaveAttribute("readonly");
  const activeCheckbox = page.locator('or-mwc-input').filter({ hasText: 'Active' }).locator('input[type="checkbox"]');
  await expect(activeCheckbox).toBeDisabled();
  const realmRolesSelect = page.locator('or-mwc-input').filter({ hasText: 'Realm roles' });
  await expect(realmRolesSelect).toHaveAttribute("disabled");
  const managerRolesSelect = page.locator('or-mwc-input').filter({ hasText: 'Manager roles' });
  await expect(managerRolesSelect).toHaveAttribute("disabled");
  const saveButton = page.getByRole("button", { name: "Save" });
  await expect(saveButton).toBeDisabled();
});
