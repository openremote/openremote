import path from "node:path";

import rest, { RestApi } from "@openremote/rest";
import { users, Usernames } from "./data/users";
import type { DefaultAssets } from "./data/assets";
const { admin, smartcity } = users;

import { UserModel } from "../../src/pages/page-users";
import { Asset, Role } from "@openremote/model";
import { test as base, expect, type Page, type ComponentTestFixtures, type Shared, withPage } from "@openremote/test";
import { AssetsPage, RealmsPage, RolesPage, RulesPage, UsersPage } from "./pages";
import { AssetViewer } from "../../../../component/or-asset-viewer/test/fixtures";
import { CollapsiblePanel } from "../../../../component/or-components/test/fixtures";
import { JsonForms } from "../../../../component/or-json-forms/test/fixtures";

export const adminStatePath = path.join(__dirname, "data/.auth/admin.json");
export const userStatePath = path.join(__dirname, "data/.auth/user.json");

export class Manager {
  private readonly clientId = "openremote";
  private readonly managerHost: String;
  readonly axios: RestApi["_axiosInstance"];

  public realm?: string;
  public user?: UserModel;
  public role?: Role;
  public assets: Asset[] = [];
  public rules: number[] = [];

  constructor(readonly page: Page, readonly baseURL: string) {
    this.managerHost = process.env.managerUrl || "http://localhost:8080";
    rest.initialise(`${this.managerHost}/api/master/`);
    this.axios = rest.axiosInstance;
  }

  async goToRealmStartPage(realm: string) {
    await this.page.goto(this.getAppUrl(realm));
  }

  /**
   * Navigate to a settings page inside the manager using the settings menu at the top right
   * @param setting Name of the setting menu item
   */
  async navigateToMenuItem(setting: string) {
    await this.page.click("button#menu-btn-desktop");
    const menu = this.page.locator("#menu > #list > li").filter({ hasText: setting });
    await menu.waitFor({ state: "visible" });
    await menu.click();
  }

  /**
   * Switch to a realm using the realm picker
   * @param name Name of the realm
   */
  async switchToRealmByRealmPicker(realm: string) {
    await this.page.click("#realm-picker");
    await this.page.locator("#desktop-right li", { hasText: realm }).click();
  }

  /**
   * Navigate to a certain tab page
   * @param tab Tab name
   */
  async navigateToTab(tab: string) {
    await this.page.click(`#desktop-left a:has-text("${tab}")`);
  }

  /**
   * Login as user, waits for username and password fields to be visible.
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
   * Logout from the manager.
   *
   * After logout waits until redirection finished.
   */
  async logout() {
    if (await this.page.isVisible("#menu-btn-desktop")) {
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
   * Get the roles of the current realm
   *
   * Expects a realm to be configured
   * @param config The axios request config
   */
  async getClientRoles(config) {
    try {
      const response = await rest.api.UserResource.getClientRoles(this.realm!, this.clientId, config);
      expect(response.status).toBe(200);
      return response.data;
    } catch (e) {
      console.error("Failed to get roles", e.response.status);
    }
  }

  /**
   * Create an new role
   *
   * The `compositeRoleIds` are mapped by name to the corresponding role id.
   *
   * Expects a realm to be configured
   * @param newRole The role to create
   * @param roles The current stored roles to add the new role to
   * @param config The axios request config
   */
  async createRole(newRole: Role, roles: Role[], config) {
    if (newRole.compositeRoleIds) {
      newRole.compositeRoleIds = newRole.compositeRoleIds
        .map((name) => roles.find((r) => r.name === name)?.id)
        .filter(Boolean) as string[];
    }
    roles.push(newRole);
    try {
      const response = await rest.api.UserResource.updateRoles(this.realm!, roles, config);
      expect(response.status).toBe(204);
      this.role = newRole;
    } catch (e) {
      console.error("Failed to create role", e.response.status);
    }
  }

  /**
   * Create an new user
   *
   * Expects a realm to be configured
   * @param user The user to create
   * @param config The axios request config
   */
  async createUser(user: UserModel, config) {
    try {
      const response = await rest.api.UserResource.create(this.realm!, user, config);
      expect(response.status).toBe(200);
      this.user = response.data;
    } catch (e) {
      console.error("Failed to create user", e.response.status);
    }
  }

  /**
   * Add a role to the current user
   *
   * Expects a realm and user to be configured
   * @param roles A list of roles to add to the user
   * @param config The axios request config
   */
  async addUserRoles(roles: string[], config) {
    try {
      const response = await rest.api.UserResource.updateUserClientRoles(
        this.realm!,
        this.user!.id!,
        this.clientId,
        roles,
        config
      );
      expect(response.status).toBe(204);
    } catch (e) {
      console.error("Failed to update users' roles", e.response.status);
    }
  }

  /**
   * Reset the user password with the smartcity users' password
   *
   * Expects a realm and user to be configured
   * @param config The axios request config
   */
  async resetUserPassword(config) {
    try {
      const response = await rest.api.UserResource.resetPassword(
        this.realm!,
        this.user!.id!,
        { value: smartcity.password },
        config
      );
      expect(response.status).toBe(204);
    } catch (e) {
      console.error("Failed to reset user password", e.response.status);
    }
  }

  /**
   * Create an asset
   * @param asset The asset to create
   * @param config The axios request config
   */
  async createAsset(asset: Asset, config) {
    await rest.api.AssetResource.create(asset, config)
      .then((response) => {
        expect(response.status).toBe(200);
        this.assets.push(response.data);
      })
      .catch((e) => {
        expect(e.response.status, { message: "Failed to create asset" }).toBe(409);
      });
  }

  /**
   * Setup the testing environment by giving the realm name and additional parameters
   * @param realm Realm to create
   * @param [options] - Optional parameters for setup
   * @param [options.user] - The user to create within the realm
   * @param [options.role] - The role to assign or create
   * @param [options.assets] - Assets to create for the realm
   */
  async setup(
    realm: string,
    { user, role, assets }: { user?: UserModel; role?: Role; assets?: Asset[] | DefaultAssets } = {}
  ) {
    const access_token = await this.getAccessToken("master", admin.username, admin.password);
    const config = { headers: { Authorization: `Bearer ${access_token}` } };

    this.realm = realm;

    // Provision role
    if (role) {
      const roles = await this.getClientRoles(config);
      if (roles) {
        await this.createRole(role, roles, config);
      }
    }

    // Provision user
    if (user) {
      await this.createUser(user, config);
      await this.addUserRoles(user.roles!, config);
      await this.resetUserPassword(config);
    }

    // Provision assets
    if (assets) {
      this.assets = [];
      for (const asset of assets) {
        await this.createAsset(asset, config);
      }
    }
  }

  /**
   * Deletes rules in the active realm
   * @param config The axios request config
   */
  async deleteRealmRulesets(config) {
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

  /**
   * Deletes assets
   * @param config The axios request config
   */
  async deleteAssets(config) {
    const assetIds = this.assets.map(({ id }) => id!);
    try {
      const response = await rest.api.AssetResource.delete({ assetId: assetIds }, config);
      expect(response.status).toBe(204);
      this.assets = [];
    } catch (e) {
      console.warn("Could not delete asset(s): ", assetIds);
    }
  }

  /**
   * Delete role
   *
   * Expects a realm to be configured
   * @param roles The stored roles
   * @param config The axios request config
   */
  async deleteRole(roles: Role[], config) {
    roles = roles.filter((r) => r.id !== this.role!.id);
    try {
      const response = await rest.api.UserResource.updateRoles(this.realm!, roles, config);
      expect(response.status).toBe(204);
      delete this.role;
    } catch (e) {
      console.warn("Could not update roles: ", this.role);
    }
  }

  /**
   *  Clean up the environment
   */
  async cleanUp() {
    const access_token = await this.getAccessToken("master", "admin", users.admin.password!);
    const config = { headers: { Authorization: `Bearer ${access_token}` } };

    if (this.rules.length > 0) {
      await this.deleteRealmRulesets(config);
    }

    if (this.assets.length > 0) {
      await this.deleteAssets(config);
    }

    if (this.role && this.realm) {
      const roles = await this.getClientRoles(config);
      if (roles) {
        await this.deleteRole(roles, config);
      }
    }
  }

  protected getAppUrl(realm: string) {
    return `${new URL(this.baseURL).origin}/manager/?realm=${realm}`;
  }
}

function withManager<R>(managerPage: Function): TestFixture<R, { page: Page; shared: Shared; manager: Manager }> {
  return async ({ page: basePage, shared, manager }, use) => {
    expect(manager).toBeInstanceOf(Manager);
    await use(new (managerPage.bind(null, basePage, shared, manager))());
  };
}

interface PageFixtures {
  assetsPage: AssetsPage;
  realmsPage: RealmsPage;
  rolesPage: RolesPage;
  rulesPage: RulesPage;
  usersPage: UsersPage;
}

interface ComponentFixtures extends ComponentTestFixtures {
  assetViewer: AssetViewer;
  collapsiblePanel: CollapsiblePanel;
  jsonForms: JsonForms;
}

interface Fixtures extends PageFixtures, ComponentFixtures {
  manager: Manager;
}

export const test = base.extend<Fixtures>({
  manager: async ({ page, baseURL }, use) => await use(new Manager(page, baseURL!)),
  // Pages
  assetsPage: withManager(AssetsPage),
  realmsPage: withManager(RealmsPage),
  rolesPage: withManager(RolesPage),
  rulesPage: withManager(RulesPage),
  usersPage: withManager(UsersPage),
  // Components
  assetViewer: withPage(AssetViewer),
  collapsiblePanel: withPage(CollapsiblePanel),
  jsonForms: withPage(JsonForms),
});
