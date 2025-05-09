import { test as base, expect } from "@playwright/test";
import { getAppUrl } from "../utils";
import assets from "./data/assets";
import passwords from "./data/passwords";

export const test = base.extend({
  async openRealm({ baseURL, page }, use) {
    // TODO: handle this per app ?
    await use((realm) => page.goto(getAppUrl(baseURL, realm)));
  },
  async goToRealmStartPage({ baseURL, page }, use) {
    await use(async (realm) => {
      const url = getAppUrl(baseURL, realm);
      await page.goto(url);
      await page.waitForTimeout(1500);
    });
  },
  async login({ page }, use) {
    await use(async (user) => {
      await page.waitForTimeout(500);
      // const isLogin = (await page.isVisible('input[id="username"]')) || false;
      const username = page.getByRole("textbox", { name: "Username or email" });
      const password = page.getByRole("textbox", { name: "Password" });
      await username.waitFor();
      if ((await username.isVisible()) && (await password.isVisible())) {
        await username.fill(user);
        await password.fill(passwords[user]);
        await page.keyboard.press("Enter");
        // console.log(`User: "${user}" logged in,   ` + timeCost(false) + "s");
      }
    });
  },
  async logout({ page }, use) {
    await use(async () => {
      const isPanelVisibile = await page.isVisible('button:has-text("Cancel")');
      if (isPanelVisibile) {
        await page.click('button:has-text("Cancel")');
      }
      const isMenuBtnVisible = await page.isVisible("#menu-btn-desktop");
      if (isMenuBtnVisible) {
        await page.click("#menu-btn-desktop");
        await page.locator("#menu > #list > li").filter({ hasText: "Log out" }).click();
      }
    });
  },
  /**
   * Repeatable actions
   */
  async navigateToMenuItem({ page }, use) {
    await use(async (setting) => {
      // setStepStartTime();
      await page.waitForTimeout(500);
      await page.click('button[id="menu-btn-desktop"]');
      await page.waitForTimeout(500);
      const menu = page.locator("#menu > #list > li").filter({ hasText: setting });
      await menu.waitFor();
      if (await menu.isVisible()) {
        await menu.click();
      } else {
        console.log("not rendered yet");
      }
      // console.log(`Navigated to "${setting}" meun item,   ` + timeCost(false) + "s");
    });
  },
  async navigateToTab({ page }, use) {
    await use(async (tab) => {
      await page.click(`#desktop-left a:has-text("${tab}")`);
      // await page.wait(1500);
    });
  },
  async addRealm({ page }, use) {
    await use(async (name, first = false) => {
      await page.waitForTimeout(500);
      const isVisible = await page.isVisible(`[aria-label="attribute list"] span:has-text("${name}")`);
      if (!isVisible) {
        await page.click("text=Add Realm");
        await page.locator("#realm-row-1 label").filter({ hasText: "Realm" }).fill(name);
        await page.locator("#realm-row-1 label").filter({ hasText: "Friendly name" }).fill(name);
        await page.click('button:has-text("create")');

        // await page.wait(first == true ? 15000 : 10000);
        // const count = await page.count(`[aria-label="attribute list"] span:has-text("${name}")`)
        // await expect(count).toEqual(1)
        // await console.log("Realm: " + `"${name}"` + " added,   " + timeCost(false) + "s");
      }
    });
  },
  async switchToRealmByRealmPicker({ page }, use) {
    await use(async (name) => {
      await page.waitForTimeout(500);
      await page.click("#realm-picker");
      await page.waitForTimeout(500);
      await page.click(`li[role="menuitem"]:has-text("${name}")`);
    });
  },
  async addUser({ page }, use) {
    await use(async (username, password) => {
      await page.locator('#content').filter({ hasText: 'Regular users' }).getByRole('button', { name: "Add User" }).click();
      await page.locator('label').filter({ hasText: 'Username' }).fill(username);
      await page.locator('label').filter({ hasText: /Password/ }).fill(password);
      await page.locator('label').filter({ hasText: 'Repeat password' }).fill(password);
      // select permissions
      await page.getByRole('button', { name: 'Realm roles' }).click();
      await page.click('div[role="button"]:has-text("Manager Roles")');
      await page.click('li[role="menuitem"]:has-text("Read")');
      await page.click('li[role="menuitem"]:has-text("Write")');
      await page.click('div[role="button"]:has-text("Manager Roles")');
      await page.waitForTimeout(500)
      // create user
      await page.click('button:has-text("create")')
      await page.waitForTimeout(500)
    });
  },
  async switchMode({ page }, use) {
    await use(async (targetMode) => {
      await page.waitForTimeout(400);
      const atModifyMode = await page.isVisible('button:has-text("View")');
      const atViewMode = await page.isVisible('button:has-text("Modify")');

      if (atModifyMode && targetMode == "view") {
        await page.click('button:has-text("View")');
        console.log(":::::: at view mode");
      }
      if (atViewMode && targetMode == "modify") {
        await page.click('button:has-text("Modify")');
        console.log(":::::: at modify mode");
      }
    });
  },
  async addAssets(
    { page, switchMode, unselect, updateLocation, setConfigItem, updateInModify, save, updateAssets },
    use
  ) {
    await use(async (update, configOrLoction) => {
      // const addAssetTime = new Date() / 1000;

      await page.waitForTimeout(500);

      // Goes to asset page
      await page.click("#desktop-left a:nth-child(2)");

      // select conosle first to enter into the modify mode
      await page.click(`#list-container >> text="Consoles"`);
      await switchMode("modify");
      await unselect();

      // create assets accroding to assets array
      for (let asset of assets) {
        // setStepStartTime();
        let isAssetVisible = await page.isVisible(`#list-container >> text=${asset.name}`);
        try {
          if (!isAssetVisible) {
            await page.click(".mdi-plus");
            await page.click(`text=${asset.asset}`);
            await page.fill('#name-input input[type="text"]', asset.name);
            await page.click("#add-btn");
            await page.waitForTimeout(500);
            // check if at modify mode
            // if yes we should see the save button then save
            const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
            console.log("save btn is " + isSaveBtnVisible);
            if (isSaveBtnVisible) {
              console.log("ready to save");
              await page.click('button:has-text("Save")');
            }
            console.log(":::::: emtpy asset has been added");
            await switchMode("modify");
            // await page.unselect()
            // await page.click(`#list-container >> text=${asset.name}`)
            if (update) {
              // switch to modify mode if at view mode

              // update in modify mode
              if (configOrLoction == "location") {
                await updateLocation(asset.location_x, asset.location_y);
                console.log(":::::: location updated");
              } else if (configOrLoction == "config") {
                await setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2);
                console.log(":::::: config items have been added");
              } else {
                await updateLocation(asset.location_x, asset.location_y);
                await setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2);
                console.log(":::::: both settings have been added");
              }

              await updateInModify(asset.attr_1, asset.a1_type, asset.v1);
              await updateInModify(asset.attr_2, asset.a2_type, asset.v2);

              await save();

              //switch to view mode
              await switchMode("view");
              // update value in view mode
              await updateAssets(asset.attr_3, asset.a3_type, asset.v3);
              await page.waitForTimeout(500);

              //switch to modify mode
              await switchMode("modify");
            }
            await unselect();
            // console.log(
            //   "Asset: " +
            //     `"${asset.name}"` +
            //     " with " +
            //     configOrLoction +
            //     " updated has been added,  " +
            //     timeCost(false) +
            //     "s"
            // );
          }
        } catch (error) {
          console.log("error" + error);
        }
      }
      // console.log("Adding assets takes " + (new Date() / 1000 - addAssetTime).toFixed(3) + "s");
    });
  },
  async unselect({ page }, use) {
    await use(async () => {
      await page.waitForTimeout(500);
      const isCloseVisible = await page.isVisible(".mdi-close >> nth=0");

      // leave modify mode
      // if (isViewVisible) {
      //     await page.click('button:has-text("View")')
      //     let btnDisgard = await page.isVisible('button:has-text("Disgard")')
      //     if (btnDisgard) {
      //         await page.click('button:has-text("Disgard")')
      //         console.log("didn't save successfully")
      //     }
      // }

      // unselect the asset
      if (isCloseVisible) {
        //await page.page?.locator('.mdi-close').first().click()
        await page.click(".mdi-close >> nth=0");
      }

      await page.waitForTimeout(500);
    });
  },
  async updateAssets({ page }, use) {
    await use(async (attr, type, value) => {
      await page.fill(`#field-${attr} input[type="${type}"]`, value);
      await page.click(`#field-${attr} #send-btn span`);
    });
  },
  async updateInModify({ page }, use) {
    await use(async (attr, type, value) => {
      await page.fill(`text=${attr} ${type} >> input[type="number"]`, value);
      console.log("::::::  " + attr + " has been updated");
    });
  },
  async updateLocation({ page }, use) {
    await use(async (location_x, location_y) => {
      await page.click("text=location GEO JSON point >> button span");
      await page.mouse.click(location_x, location_y, { delay: 1000 });
      await page.click('button:has-text("OK")');
    });
  },
  async configItem({ page }, use) {
    await use(async (item_1, item_2, attr) => {
      await page.waitForTimeout(500);
      await page.click(`td:has-text("${attr} ") >> nth=0`);
      await page.waitForTimeout(500);
      await page.click(".attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component");
      await page.click(`li[role="checkbox"]:has-text("${item_1}")`);
      await page.click(`li[role="checkbox"]:has-text("${item_2}")`);
      await page.click('div[role="alertdialog"] button:has-text("Add")');
      await page.waitForTimeout(500);

      // close attribute menu
      await page.click(`td:has-text("${attr}") >> nth=0`);
    });
  },
  async setConfigItem({ page, configItem }, use) {
    await use(async (item_1, item_2, attr_1, attr_2) => {
      await configItem(item_1, item_2, attr_1);
      await page.waitForTimeout(500);
      await configItem(item_1, item_2, attr_2);
      await page.waitForTimeout(500);
    });
  },
  async deleteRealm({ page, goToRealmStartPage }, use) {
    await use(async (realm) => {
      // setStepStartTime();
      await page.waitForTimeout(500);
      await page.getByRole('cell', { name: realm }).first().click();
      await page.click('button:has-text("Delete")');
      await page.waitForTimeout(500);
      await page.fill('div[role="alertdialog"] input[type="text"]', realm);
      await page.click('button:has-text("OK")');
      // wait for backend to response
      await page.waitForTimeout(5000);
      try {
        await expect(page.locator('[aria-label="attribute list"] span:has-text("smartcity")')).toHaveCount(0);

        await goToRealmStartPage("master");
        await page.waitForTimeout(500);
        await expect(page.locator("#desktop-right #realm-picker")).not.toBeVisible();
        // await console.log(`Realm: "${realm}" deleted,    ` + timeCost(false) + "s");
      } catch (e) {
        console.log(e);
      }
    });
  },
  async deleteSelectedAsset({ page, navigateToTab }, use) {
    await use(async (asset) => {
      // setStepStartTime();
      await navigateToTab("Assets");
      let assetSelected = await page.locator(`text=${asset}`).count();
      if (assetSelected > 0) {
        await page.click(`text=${asset}`);
        await page.click(".mdi-delete");
        await page.click('button:has-text("Delete")');
        await page.waitForTimeout(1500);
        let visibile = await page.locator(`text=${asset}`).count();
        await expect(visibile).toBeFalsy();
      } else {
        console.log(`Asset: "${asset}" does not exsit`);
      }
      // console.log(`Asset: "${asset}" has been deleted,    ` + timeCost(false) + "s");
    });
  },
  async save({ page }, use) {
    await use(async () => {
      console.log(":::::: in saving");
      await page.waitForTimeout(200);
      await page.click("#edit-container");
      await page.waitForTimeout(200); // wait for button to enabled
      const isSaveBtnVisible = await page.isVisible('button:has-text("Save")');
      if (isSaveBtnVisible) {
        await page.click('button:has-text("Save")');
      }
      await page.waitForTimeout(200);
      const isDisabled = await page.locator('button:has-text("Save")').isDisabled();
      //asset modify
      const ifModifyMode = await page.isVisible('button:has-text("OK")');
      if (ifModifyMode) {
        await page.click('button:has-text("OK")');
        console.log("panel closed");
      }
      if (!isDisabled) {
        await page.click('button:has-text("Save")');
        await page.waitForTimeout(200);
      }
      await expect(page.locator('button:has-text("Save")')).toBeDisabled();
    });
  },
  async setup(
    {
      page,
      logout,
      goToRealmStartPage,
      login,
      addAssets,
      navigateToMenuItem,
      addRealm,
      switchToRealmByRealmPicker,
      addUser,
    },
    use
  ) {
    await use(async (realm, level, configOrLocation = "no") => {
      // global.startTime = new Date() / 1000;

      if (level !== "lv0") {
        await goToRealmStartPage("master");
        await login("admin");

        await page.waitForTimeout(1500);
        const isPickerVisible = await page.isVisible("#realm-picker");
        // add realm
        if (!isPickerVisible) {
          await navigateToMenuItem("Realms");
          await addRealm(realm);
        }
        await switchToRealmByRealmPicker(realm);

        // add user
        if (level >= "lv2") {
          await navigateToMenuItem("Users");
          await addUser("smartcity", passwords.smartcity);
          // add assets
          if (level >= "lv3") {
            await logout();
            await goToRealmStartPage(realm);
            await login("smartcity");
            await addAssets(level == "lv4", configOrLocation);
          }
        }
        await logout();
        // console.log(level + " setup takes " + timeCost(true) + "s");
      }
    });
  },
  async cleanUp({ page, goToRealmStartPage, login, switchToRealmByRealmPicker, navigateToMenuItem, deleteRealm }, use) {
    await use(async () => {
      // const cleanTime = new Date() / 1000;

      // ensure login as admin into master
      await page.waitForTimeout(500);
      await goToRealmStartPage("master");
      await login("admin");
      // must wait for the realm picker to be rendered
      await page.waitForTimeout(1500);
      const isPickerVisible = await page.isVisible("#realm-picker");
      if (isPickerVisible) {
        // switch to master realm to ensure being able to delete custom realm
        await switchToRealmByRealmPicker("master");
        // delete realms
        // should delete everything and set the envrioment to beginning
        await navigateToMenuItem("Realms");
        await deleteRealm("smartcity");
      }
      // console.log("Clean up takes " + (new Date() / 1000 - cleanTime).toFixed(3) + "s");
    });
  },
  async drag({ page }, use) {
    await use(async (position_x, position_y) => {
      await page.mouse.down()
      await page.mouse.move(position_x, position_y)
      await page.mouse.up()
    })
  }
});
