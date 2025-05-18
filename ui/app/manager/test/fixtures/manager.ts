import { test as base, type Page, type Locator, expect, type TestFixture } from "@playwright/test";
import rest, { RestApi } from "@openremote/rest";
import { users, Usernames } from "./data/users";
import type { DefaultAssets } from "./data/assets";
const { admin, smartcity } = users;

import { UserModel } from "../../src/pages/page-users";
import { Asset, Role, Ruleset } from "@openremote/model";
import { BasePage } from "@openremote/test";
import permissions from "./data/permissions";

class Manager {
  private readonly clientId = "openremote";
  private readonly managerHost: String;
  readonly axios: RestApi["_axiosInstance"];

  public realm?: string;
  public user?: UserModel;
  public role?: Role;
  public assets: Asset[] = [];
  public rules: number[] = [];

  constructor(readonly page: Page, readonly baseURL: string) {
    // TODO: parameterize
    this.managerHost = "http://localhost:8080";
    rest.initialise(`${this.managerHost}/api/master/`);
    this.axios = rest.axiosInstance;
  }

  async goToRealmStartPage(realm: string) {
    await this.page.goto(this.getAppUrl(realm));
  }

  /**
   * Navigate to a setting page inside the manager
   * for the setting list menu at the top right
   * @param setting Name of the setting menu item
   */
  async navigateToMenuItem(setting: string) {
    await this.page.click('button[id="menu-btn-desktop"]');
    const menu = this.page.locator("#menu > #list > li").filter({ hasText: setting });
    await menu.waitFor({ state: "visible" });
    await menu.click();
  }

  /**
   * Switch to a realm in the manager's realm picker
   * @param name Name of custom realm
   */
  async switchToRealmByRealmPicker(name: string) {
    await this.page.click("#realm-picker");
    await this.page.click(`li[role="menuitem"]:has-text("${name}")`);
  }

  /**
   * Navigate to a certain tab page
   * @param tab Tab name
   */
  async navigateToTab(tab: string) {
    await this.page.click(`#desktop-left a:has-text("${tab}")`);
  }

  /**
   * Login as user
   * @param user Username (admin or other)
   */
  async login(user: Usernames) {
    const username = this.page.getByRole("textbox", { name: "Username or email" });
    const password = this.page.getByRole("textbox", { name: "Password" });
    await username.waitFor();
    if ((await username.isVisible()) && (await password.isVisible())) {
      await username.fill(user);
      await password.fill(users[user].password);
      await this.page.keyboard.press("Enter");
    }
  }

  /**
   * Logout and delete login
   */
  async logout() {
    const isPanelVisibile = await this.page.isVisible('button:has-text("Cancel")');
    if (isPanelVisibile) {
      await this.page.click('button:has-text("Cancel")');
    }
    const isMenuBtnVisible = await this.page.isVisible("#menu-btn-desktop");
    if (isMenuBtnVisible) {
      await this.page.click("#menu-btn-desktop");
      await this.page.locator("#menu > #list > li").filter({ hasText: "Log out" }).click();
    }
    // Wait for navigation to login page to prevent simultaneous navigation
    await this.page.waitForURL("**/auth/realms/**");
  }

  async getAccessToken(realm: string, username: Usernames, password: string) {
    const data = new URLSearchParams();
    data.append("client_id", this.clientId);
    data.append("username", username);
    data.append("password", password);
    data.append("grant_type", "password");
    const { access_token } = (
      await this.axios.post(`${this.managerHost}/auth/realms/${realm}/protocol/openid-connect/token`, data, {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      })
    ).data;
    return access_token;
  }

  /**
   * setup the testing environment by giving the realm name and additional parameters
   * @param realm Realm to create
   * @param user Realm user to create
   * @param user Role to create
   * @param user Assets to create
   */
  async setup(
    realm: string,
    { user, role, assets }: { user?: UserModel; role?: Role; assets?: Asset[] | DefaultAssets } = {}
  ) {
    const access_token = await this.getAccessToken("master", admin.username, admin.password);
    const config = { headers: { Authorization: `Bearer ${access_token}` } };

    this.realm = realm;

    // Add role
    if (role) {
      let roles: Role[] = [];
      try {
        const response = await rest.api.UserResource.getClientRoles(realm, this.clientId, config);
        expect(response.status).toBe(200);
        roles = response.data;
        if (role.compositeRoleIds) {
          role.compositeRoleIds = role.compositeRoleIds
            .map((name) => roles.find((r) => r.name === name)?.id)
            .filter(Boolean) as string[];
        }
        roles.push(role);
        try {
          const response = await rest.api.UserResource.updateRoles(realm, roles, config);
          expect(response.status).toBe(204);
          this.role = role;
        } catch (e) {
          console.error("Failed to create role", e.response.status);
        }
      } catch (e) {
        console.error("Failed to get roles", e.response.status);
      }
    }

    // Add user
    if (user) {
      try {
        const response = await rest.api.UserResource.create(realm, user, config);
        expect(response.status).toBe(200);
        this.user = response.data;
        // Add users' roles
        try {
          const response = await rest.api.UserResource.updateUserClientRoles(
            realm,
            this.user!.id!,
            this.clientId,
            user.roles!,
            config
          );
          expect(response.status).toBe(204);
          // Reset users' password
          try {
            const response = await rest.api.UserResource.resetPassword(
              realm,
              this.user!.id!,
              { value: smartcity.password },
              config
            );
            expect(response.status).toBe(204);
          } catch (e) {
            console.error("Failed to reset user password", e.response.status);
          }
        } catch (e) {
          console.error("Failed to update users' roles", e.response.status);
        }
      } catch (e) {
        console.error("Failed to create user", e.response.status);
      }
    }

    if (assets) {
      // Add assets
      this.assets = [];
      for (const asset of assets) {
        await rest.api.AssetResource.create(asset, config)
          .then((response) => {
            expect(response.status).toBe(200);
            this.assets!.push(response.data);
          })
          .catch((e) => {
            expect(e.response.status, { message: "Failed to create asset" }).toBe(409);
          });
      }
    }
  }

  /**
   *  Clean up the environment
   */
  async cleanUp() {
    const access_token = await this.getAccessToken("master", "admin", users.admin.password!);
    const config = { headers: { Authorization: `Bearer ${access_token}` } };

    if (this.rules.length > 0) {
      for (const [i, id] of this.rules.entries()) {
        try {
          const response = await rest.api.RulesResource.deleteRealmRuleset(id!, config);
          expect(response.status).toBe(204);
          this.rules.splice(i);
        } catch (e) {
          console.warn("Could not delete realm rule: ", id);
        }
      }
    }

    if (this.assets.length > 0) {
      const assetIds = this.assets.map(({ id }) => id!);
      try {
        const response = await rest.api.AssetResource.delete({ assetId: assetIds }, config);
        expect(response.status).toBe(204);
        this.assets = [];
      } catch (e) {
        console.warn("Could not delete asset(s): ", assetIds);
      }
    }

    if (this.role && this.realm) {
      let roles;
      try {
        const response = await rest.api.UserResource.getClientRoles(this.realm, this.clientId, config);
        roles = response.data.filter((r) => r.id !== this.role!.id);
        try {
          const response = await rest.api.UserResource.updateRoles(this.realm, roles, config);
          expect(response.status).toBe(204);
          delete this.role;
        } catch (e) {
          console.warn("Could not update roles: ", this.role);
        }
      } catch (e) {
        console.warn("Could not get roles: ", this.user);
      }
    }
  }

  protected getAppUrl(realm: string) {
    const appUrl = this.baseURL + "manager/?realm=";
    return appUrl + realm;
  }
}

class AssetsPage extends BasePage {
  // private readonly inputBox: Locator;
  // private readonly todoItems: Locator;

  constructor(readonly page: Page, private readonly manager: Manager) {
    super(page);
  }

  async goto() {
    this.manager.navigateToTab("Assets");
  }

  // TODO: move to shared app & component fixture
  getAttributeLocator(attribute: string): Locator {
    return this.page.getByRole("row", { name: new RegExp(`\\b${attribute}\\b`) });
  }

  // TODO: move to shared app & component fixture
  getConfigurationItemLocator(attribute: string, item: string): Locator {
    // match the sibling row i.e. the configuration item row of the attribute
    return this.getAttributeLocator(attribute)
      .locator("+ tr", { hasText: new RegExp(`\\b${item}\\b`) })
      .locator("label");
  }

  /**
   * Switch between modify mode and view mode
   * @param targetMode view or modify
   */
  async switchMode(targetMode: string) {
    const selector = this.page.getByRole("button", { name: targetMode });
    await selector.waitFor({ state: "visible" });
    await selector.click();
  }

  async addAsset(type: string, name: string) {
    // start adding assets
    await this.page.click(".mdi-plus");
    await this.page.click(`li[data-value="${type}"]`);
    await this.page.fill('#name-input input[type="text"]', name);
    // create
    await this.interceptResponse<Asset>("**/asset", (asset) => {
      if (asset) this.manager.assets.push(asset);
    });
    await this.page.click("#add-btn");
  }

  async setAttributeValue(attribute: string, value: string) {
    await this.page
      .getByRole("row", { name: new RegExp(`\\b${attribute}\\b`) })
      .locator("input")
      .fill(value);
  }

  /**
   * unselect the asset
   */
  async unselect() {
    const isCloseVisible = await this.page.isVisible(".mdi-close >> nth=0");

    // unselect the asset
    if (isCloseVisible) {
      //await page.page?.locator('.mdi-close').first().click()
      await this.page.click(".mdi-close >> nth=0");
    }
  }

  /**
   * update asset in the general panel
   * @param attr attribute's name
   * @param type attribute's input type
   * @param value input value
   */
  async updateAssets(attr: string, type: string, value: string) {
    await this.page.fill(`#field-${attr} input[type="${type}"]`, value);
    await this.page.click(`#field-${attr} #send-btn span`);
  }

  /**
   * update the data in the modify mode
   * @param attr attribute's name
   * @param type attribute's input type
   * @param value input value
   */
  async updateInModify(attr: string, type: string, value: string) {
    await this.page.fill(`text=${attr} ${type} >> input[type="number"]`, value);
  }

  /**
   * update location so we can see in the map
   * @param location_x horizental coordinator (start from left edge)
   * @param location_y vertail coordinator (start from top edge)
   */
  async updateLocation(x: number, y: number) {
    await this.page.click("text=location GEO JSON point >> button span");
    await this.page.mouse.click(x, y, { delay: 1000 });
    await this.page.click('button:has-text("OK")');
  }

  /**
   * select two config items for an attribute
   * @param item_1 the first config item
   * @param item_2 the second config item
   * @param attr attribute's name
   */
  async configItem(item_1: string, item_2: string, attr: string) {
    await this.page.click(`td:has-text("${attr} ") >> nth=0`);
    await this.page.click(".attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component");
    await this.page.click(`li[role="checkbox"]:has-text("${item_1}")`);
    await this.page.click(`li[role="checkbox"]:has-text("${item_2}")`);
    await this.page.click('div[role="alertdialog"] button:has-text("Add")');

    // close attribute menu
    await this.page.click(`td:has-text("${attr}") >> nth=0`);
  }

  /**
   * set config item for rule and insight to use
   * @param item1 the first config item
   * @param item2 the second config item
   * @param attr1 attribute's name
   * @param attr2 attribute's name
   */
  async setConfigItem(item_1: string, item_2: string, attr_1: string, attr_2: string) {
    await this.configItem(item_1, item_2, attr_1);
    await this.configItem(item_1, item_2, attr_2);
  }

  /**
   * Delete an asset by its name
   * @param asset The asset name
   */
  async deleteSelectedAsset(asset: string) {
    const assetLocator = this.page.locator(`text=${asset}`);
    await expect(assetLocator).toHaveCount(1);
    await assetLocator.click();
    await this.page.click(".mdi-delete");
    await this.page.getByRole("button", { name: "Delete" }).click();
    await expect(assetLocator).toHaveCount(0);
  }
}

// class InsightsPage extends BasePage { }

// class MapPage extends BasePage { }

class RealmsPage extends BasePage {
  constructor(readonly page: Page, private readonly manager: Manager) {
    super(page);
  }

  async goto() {
    this.manager.navigateToMenuItem("Realms");
  }

  /**
   * Create Realm with name
   * @param name realm name
   */
  async addRealm(name: string) {
    const locator = this.page.getByRole("cell", { name, exact: true });
    await this.page.getByRole("cell", { name: "Master", exact: true }).waitFor();
    if (await locator.isVisible()) {
      console.warn(`Realm "${name}" already present`);
    } else {
      await this.page.click("text=Add Realm");
      await this.page.locator("#realm-row-1 label").filter({ hasText: "Realm" }).fill(name);
      await this.page.locator("#realm-row-1 label").filter({ hasText: "Friendly name" }).fill(name);
      await this.page.click('button:has-text("create")');
    }
  }

  /**
   * Select realm by its name
   * @param name Realm's name
   */
  async selectRealm(realm: string) {
    await this.page.click("#realm-picker");
    await this.page.locator("#desktop-right li").filter({ hasText: realm }).click();
  }

  /**
   * Delete a certain realm by its name
   * @param name Realm's name
   */
  async deleteRealm(realm: string) {
    await this.page.getByRole("cell", { name: realm }).first().click();
    await this.page.click('button:has-text("Delete")');
    await this.page.fill('div[role="alertdialog"] input[type="text"]', realm);
    await this.page.click('button:has-text("OK")');
  }
}

class RolesPage extends BasePage {
  constructor(readonly page: Page, private readonly manager: Manager) {
    super(page);
  }

  async goto() {
    this.manager.navigateToMenuItem("Roles");
  }
}

class RulesPage extends BasePage {
  constructor(readonly page: Page, private readonly manager: Manager) {
    super(page);
  }

  async goto() {
    this.manager.navigateToTab("Rules");
  }
}

class UsersPage extends BasePage {
  constructor(readonly page: Page, private readonly manager: Manager) {
    super(page);
  }

  getPermission(permission: string): Locator {
    return this.page.getByRole("checkbox", { name: permission });
  }

  async toggleUserRoles(...roles: string[]) {
    const roleSelector = this.page.getByRole("button", { name: "Manager roles" });
    const itemSelector = this.page.locator("li");
    await roleSelector.click({ delay: 500 });
    for (const role of roles) {
      await itemSelector.filter({ hasText: role }).click();
    }
    await roleSelector.click();
  }

  async toHavePermissions(...roles: string[]) {
    for (const permisison of roles) {
      await expect(this.getPermission(permisison)).toBeChecked();
      await expect(this.getPermission(permisison)).toBeDisabled();
    }
    for (const permisison of permissions.filter((p) => !roles.includes(p))) {
      await expect(this.getPermission(permisison)).not.toBeChecked();
      await expect(this.getPermission(permisison)).not.toBeDisabled();
    }
  }

  /**
   * Create user
   * @param username
   * @param password
   */
  async addUser(username: string, password: string) {
    await this.page
      .locator("#content")
      .filter({ hasText: "Regular users" })
      .getByRole("button", { name: "Add User" })
      .click();
    await this.page.locator("label").filter({ hasText: "Username" }).fill(username);
    await this.page
      .locator("label")
      .filter({ hasText: /Password/ })
      .fill(password);
    await this.page.locator("label").filter({ hasText: "Repeat password" }).fill(password);
    // select permissions
    // await this.page.getByRole("button", { name: "Realm roles" }).click();
    await this.toggleUserRoles("Read", "Write");
    await this.toHavePermissions(...permissions);
    await this.interceptResponse<UserModel>(`user/${this.manager.realm}/users`, (user) => {
      if (user) this.manager.user = user;
    });
    await this.page.getByRole("button", { name: "Create" }).click();
  }
}

function withManager<R>(managerPage: Function): TestFixture<R, { page: Page; manager: Manager }> {
  return async ({ page: basePage, manager }, use) => {
    // TODO: TEST THIS
    // Check that the manager has been initialized
    expect(manager).toBeInstanceOf(Manager);
    await use(new (managerPage.bind(null, basePage, manager) as VoidFunction)());
  };
}

interface Fixtures {
  manager: Manager;
  assetsPage: AssetsPage;
  realmsPage: RealmsPage;
  rolesPage: RolesPage;
  rulesPage: RulesPage;
  usersPage: UsersPage;
}

export const test = base.extend<Fixtures>({
  // TODO: handle baseURL
  manager: async ({ page, baseURL }, use) => await use(new Manager(page, baseURL!)),
  assetsPage: withManager(AssetsPage),
  realmsPage: withManager(RealmsPage),
  rolesPage: withManager(RolesPage),
  rulesPage: withManager(RulesPage),
  usersPage: withManager(UsersPage),
});
