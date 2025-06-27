import path from "node:path";

import rest, { RestApi } from "@openremote/rest";
import { users, Usernames } from "./data/users";
import type { DefaultAssets } from "./data/assets";
const { admin, smartcity } = users;

import { UserModel } from "../../src/pages/page-users";
import { Asset, Role } from "@openremote/model";
import {
  test as base,
  expect,
  type Page,
  type Locator,
  type TestFixture,
  type ComponentFixtures,
  type Shared,
} from "@openremote/test";
import { AssetsPage, RealmsPage, RolesPage, RulesPage, UsersPage } from "./pages";

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
    return `${new URL(this.baseURL).origin}/manager/?realm=${realm}`;
  }
}

function withManager<R>(managerPage: Function): TestFixture<R, { page: Page; shared: Shared; manager: Manager }> {
  return async ({ page: basePage, shared, manager }, use) => {
    expect(manager).toBeInstanceOf(Manager);
    await use(new (managerPage.bind(null, basePage, shared, manager))());
  };
}

interface Fixtures extends ComponentFixtures {
  manager: Manager;
  assetsPage: AssetsPage;
  realmsPage: RealmsPage;
  rolesPage: RolesPage;
  rulesPage: RulesPage;
  usersPage: UsersPage;
}

export const test = base.extend<Fixtures>({
  manager: async ({ page, baseURL }, use) => await use(new Manager(page, baseURL!)),
  assetsPage: withManager(AssetsPage),
  realmsPage: withManager(RealmsPage),
  rolesPage: withManager(RolesPage),
  rulesPage: withManager(RulesPage),
  usersPage: withManager(UsersPage),
});
