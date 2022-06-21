const { setWorldConstructor, World, BeforeAll, AfterAll, After, Status, setDefaultTimeout } = require("@cucumber/cucumber");
const { ChromiumBroswer: ChromiumBrowser } = require("playwright");
const playwright = require('playwright');
const fs = require('fs');
const { expect } = require("@playwright/test");
const { join } = require('path');
const { Before } = require("@cucumber/cucumber");
const exp = require("constants");
require('dotenv').config();

/**
 * command for running all the test
 *         yarn test
 * 
 * command for running certain test/tests with the "tag"(@OpenRemote) 
 *         yarn run testTagged "@tag" (yarn run testTagged "@OpenRemote")
 * for skiping certain tag can run 
 *         yarn run testTagged "@tag and not @skip"  (yarn run testTagged "@asset and not @separate")
 * 
 * command for more.....
 */

const global = {
    browser: ChromiumBrowser,
    name: 1,
    startTime: 0,
    stepTime: 0,
    getAppUrl: (realm) => {
        const managerUrl = process.env.managerUrl || "https://localhost/";
        //const managerUrl = process.env.managerUrl || "localhost:8080/";
        const appUrl = managerUrl + "manager/?realm=";
        return appUrl + realm;
    },
    passwords: {
        admin: "secret",
        smartcity: "smartcity"
    }
}

const assets = [
    {
        asset: "Electricity battery asset",
        name: "Battery",
        attr_1: "energyLevel",
        attr_2: "power",
        attr_3: "powerSetpoint",
        a1_type: "Positive number",
        a2_type: "Number",
        a3_type: "number",
        v1: "30",
        v2: "50",
        v3: "70",
        location_x: 705,
        location_y: 210,
        config_item_1: "Rule state",
        config_item_2: "Store data points",
        config_attr_1: "energyLevel",
        config_attr_2: "power"
    },
    {
        asset: "PV solar asset",
        name: "Solar Panel",
        attr_1: "panelPitch",
        attr_2: "power",
        attr_3: "powerForecast",
        a1_type: "Positive integer",
        a2_type: "Number",
        a3_type: "number",
        v1: "30",
        v2: "70",
        v3: "100",
        location_x: 600,
        location_y: 200,
        config_item_1: "Rule state",
        config_item_2: "Store data points",
        config_attr_1: "power",
        config_attr_2: "powerForecast"
    }
]

class CustomWorld extends World {

    /**
     *  CUSTOM METHODS
     */

    /**
     * Go to start page of the manager app for the specified realm
     * @param {String} realm Realm name
     */
    async goToRealmStartPage(realm) {
        const url = global.getAppUrl(realm);
        await this.page.goto(url);
    }

    /**
     * open browser and navigate to start page for specified realm
     * @param {String} realm: Realm type (admin or other)
     */
    async openApp(realm) {
        await this.goToRealmStartPage(realm);
    }

    /**
     * login as user
     * @param {String} user  Username (admin or other)
     */
    async login(user) {
        const isLogin = await this.isVisible('input[name="username"]') || false
        if (isLogin) {
            // if (!fs.existsSync('test/storageState.json')) {
            let password = global.passwords[user];
            await this.page?.fill('input[name="username"]', user);
            await this.page?.fill('input[name="password"]', password);
            await this.page?.keyboard.press('Enter');
            //await this.page?.context().storageState({ path: 'test/storageState.json' });
            await this.page.waitForNavigation(user == "admin" ? process.env.URL + "master" : process.env.URL + "smartcity")
            //}
        }
    }


    /**
     * Logout and delete login certification
     */
    async logout() {
        // if (fs.existsSync('test/storageState.json')) {
        //     fs.unlinkSync('test/storageState.json')
        // }
        const isMenuBtnVisible = await this.isVisible('#menu-btn-desktop')
        if (isMenuBtnVisible) {
            await this.click('#menu-btn-desktop');
            await this.click('text=Log out');
        }
    }

    /**
     * click on the button
     * @param {String} button selector for an element
     */
    async click(button) {
        await this.page?.locator(button).click()
    }

    /**
     * press the key (keyboard)
     * @param {String} key location of selector
     */
    async press(key) {
        await this.page?.keyboard.press(key)
    }

    /**
     * Check  
     * @param {String} checkbox location of checkbox
     */
    async check(checkbox) {
        await this.page?.check(checkbox)
    }
    /**
     * uncheck
     * @param {String} checkbox 
     */
    async uncheck(checkbox) {
        await this.page?.uncheck(checkbox)
    }

    /**
     * fill into a input box
     * @param {String} locate location of selector 
     * @param {String} value value
     */
    async fill(locate, value) {
        await this.page?.locator(locate).fill(value)
    }

    /**
     * hover on an element
     * @param {String} locate location of selector
     */
    async hover(locate) {
        await this.page?.locator(locate).hover()
    }

    /**
     * drag to position_x and position_y
     * @param {Int} position_x coordinator of screen pixel
     * @param {Int} position_y coordinator of screen pixel
     */
    async drag(position_x, position_y) {
        await this.page.mouse.down()
        await this.page.mouse.move(position_x, position_y)
        await this.page.mouse.up()
    }

    /**
     * start dragging from origin and end at destination
     * @param {String} origin start point (a selector)
     * @param {String} destination end point (a selector)
     */
    async dragAndDrop(origin, destination) {
        await this.page.dragAndDrop(origin, destination)
    }

    /**
     * wait for millisecond
     * @param {Int} millisecond 
     */
    // async wait(millisecond) {
    //     return new Promise(function (resolve) {
    //         setTimeout(resolve, millisecond)
    //     });
    // }
    async wait(millisecond) {
        await this.page.waitForTimeout(millisecond);
    }

    /**
     * @param {String} element check how many elements are there in the page
     * @returns {Int} number of elements
     */
    async count(element) {
        return await this.page.locator(element).count()
    }

    /**
     * 
     * @param {String} element check if the element is visibile in the page
     * Note that this element should be the only one in the page
     * @returns {Boolean} if the element if visible 
     */
    async isVisible(element) {
        return await this.page.locator(element).isVisible()
    }

    /**
     *  Repeatable actions
     */

    /**
     * navigate to a setting page inside the manager 
     * for the setting list menu at the top right
     * @param {Sting} setting name of the setting menu item
     */
    async navigateToMenuItem(setting) {
        await this.wait(500)
        await this.click('button[id="menu-btn-desktop"]');
        await this.wait(500)
        const isItemVisible = await this.isVisible(`text=${setting}`)
        if (isItemVisible) {
            await this.click(`text=${setting}`);
        }
        else {
            console.log("not rendered yet")
        }

    }

    /**
     * navigate to a certain tab page
     * @param {String} tab tab name
     */
    async navigateToTab(tab) {
        await this.click(`#desktop-left a:has-text("${tab}")`)
    }

    /**
     *  create Realm with name
     * @param {String} name realm name
     */
    async addRealm(name) {

        global.stepTime = new Date() / 1000

        await this.wait(300)
        const isVisible = await this.isVisible(`[aria-label="attribute list"] span:has-text("${name}")`)
        if (!isVisible) {
            await this.click('text=Add Realm');
            await this.fill('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]', name)

            await this.page?.locator('input[type="text"]').nth(3).fill(name);
            await Promise.all([
                this.click('button:has-text("create")'),
                //this.page.waitForNavigation(global.getAppUrl().substring(0, 23) + '#/realms', { waitUntil: 'load', timeout: 50000 }),
                this.page.waitForNavigation(global.getAppUrl().substring(0, 26) + '#/realms', { waitUntil: 'load', timeout: 0 })
            ]);
            await this.wait(3000)
            await console.log("Realm: " + name + " added,   " + timeCost(false) + "s")

        }
        const count = await this.count(`[aria-label="attribute list"] span:has-text("${name}")`)
        await expect(count).toEqual(1)
    }

    /**
     * switch to a realm in the manager's realm picker
     * @param {String} name name of custom realm
     */
    async switchToRealmByRealmPicker(name) {
        await this.click('#realm-picker');
        await this.wait(300)
        await this.click(`li[role="menuitem"]:has-text("${name}")`)
    }

    /**
     *  Create User
     */
    async addUser(username, password) {

        global.stepTime = new Date() / 1000
        /**
         * add user
         */
        await this.wait(100)
        // go to user page
        await this.click('#menu-btn-desktop');
        await this.click('text=Users');
        await this.wait(300)
        const isVisible = await this.isVisible('main[role="main"] >> text=' + username)
        // add user if not exist
        if (!isVisible) {

            await this.click('.mdi-plus >> nth=0')
            await this.fill('input[type="text"] >> nth=0', username)
            // type in password
            await this.fill('#password-user0 input[type="password"]', password)
            await this.fill('#repeatPassword-user0 input[type="password"]', password)
            // select permissions
            await this.click('div[role="button"]:has-text("Roles")');
            await this.click('li[role="menuitem"]:has-text("Read")');
            await this.click('li[role="menuitem"]:has-text("Write")');
            await this.wait(2000)
            await this.click('div[role="button"]:has-text("Roles")')
            // create user
            await this.click('button:has-text("create")')
            console.log("User added,    " + timeCost(false) + "s")
        }
        else {
        }
    }

    /**
     * create new empty assets
     * @param {Boolean} update for checking if updating values is needed
     */
    async addAssets(update, configOrLoction) {

        global.stepTime = new Date() / 1000

        await this.wait(600)

        // Goes to asset page
        await this.click('#desktop-left a:nth-child(2)')

        // create assets accroding to assets array
        for (let asset of assets) {
            let isAssetVisible = await this.isVisible(`#list-container >> text=${asset.name}`)
            try {
                if (!isAssetVisible) {
                    await this.click('.mdi-plus')
                    await this.click(`text=${asset.asset}`)
                    await this.fill('#name-input input[type="text"]', asset.name)
                    await this.click('#add-btn')
                    await this.unselectAll()
                    await this.click(`#list-container >> text=${asset.name}`)
                    if (update) {
                        // update value in general panel
                        await this.updateAssets(asset.attr_3, asset.a3_type, asset.v3)

                        // update in modify mode
                        await this.click('button:has-text("Modify")')
                        if (configOrLoction == "location") {
                            await this.updateLocation(asset.location_x, asset.location_y)
                        }
                        else if (configOrLoction == "config") {
                            await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2)
                        }
                        else {
                            await this.updateLocation(asset.location_x, asset.location_y)
                            await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2)
                        }

                        await this.updateInModify(asset.attr_1, asset.a1_type, asset.v1)
                        await this.updateInModify(asset.attr_2, asset.a2_type, asset.v2)


                        await this.save()

                        await this.wait(300)
                    }
                    await this.unselectAll()
                    console.log("Asset: " + asset.name + " with " + configOrLoction + " updated has been added")
                }
            }
            catch (error) {
                console.log('error' + error);
            }
        }
        console.log("Adding assets takes " + timeCost(false) + "s")
    }

    /**
     * unselect the asset
     */
    async unselectAll() {

        const isViewVisible = await this.isVisible('button:has-text("View")')
        const isCloseVisible = await this.isVisible('.mdi-close >> nth=0')

        // leave modify mode
        if (isViewVisible) {
            await this.click('button:has-text("View")')
        }

        // unselect the asset
        if (isCloseVisible) {
            //await this.page?.locator('.mdi-close').first().click()
            await this.click('.mdi-close >> nth=0')
        }
        await this.wait(500)
    }

    /**
     * update asset in the general panel
     * @param {String} attr attribute's name
     * @param {String} type attribute's input type
     * @param {String} value input value
     */
    async updateAssets(attr, type, value) {
        await this.fill(`#field-${attr} input[type="${type}"]`, value)
        await this.click(`#field-${attr} #send-btn span`)
    }

    /**
     * update the data in the modify mode
     * @param {String} attr attribute's name
     * @param {String} type attribute's input type
     * @param {String} value input value
     */
    async updateInModify(attr, type, value) {
        await this.fill(`text=${attr} ${type} >> input[type="number"]`, value)
    }

    /**
     * update location so we can see in the map
     * @param {Int} location_x horizental coordinator (start from left edge)
     * @param {Int} location_y vertail coordinator (start from top edge)
     */
    async updateLocation(location_x, location_y) {
        await this.click('text=location GEO JSON point >> button span')
        await this.page?.mouse.click(location_x, location_y, { delay: 1000 })
        await this.click('button:has-text("OK")')
    }

    /**
     * select two config items for an attribute
     * @param {String} item_1 the first config item
     * @param {String} item_2 the second config item
     * @param {String} attr attribute's name
     */
    async configItem(item_1, item_2, attr) {
        await this.wait(300)
        await this.click(`td:has-text("${attr} ") >> nth=0`)
        await this.wait(300)
        await this.click('.attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component')
        await this.click(`li[role="checkbox"]:has-text("${item_1}")`)
        await this.click(`li[role="checkbox"]:has-text("${item_2}")`)
        await this.click('div[role="alertdialog"] button:has-text("Add")')
        await this.wait(300)

        // close attribute menu
        await this.click(`td:has-text("${attr}") >> nth=0`)
    }

    /**
     * set config item for rule and insight to use
     * @param {String} item1 the first config item
     * @param {String} item2 the second config item
     * @param {String} attr attribute's name
     */
    async setConfigItem(item_1, item_2, attr_1, attr_2) {
        await this.configItem(item_1, item_2, attr_1)
        await this.wait(300)
        await this.configItem(item_1, item_2, attr_2)
        await this.wait(300)
    }

    /**
     * Delete a certain realm by its name
     * @param {String} name Realm's name
     */
    async deleteRealm(realm) {

        global.stepTime = new Date() / 1000
        await this.wait(300)
        await this.click(`[aria-label="attribute list"] span:has-text("${realm}")`)
        await this.click('button:has-text("Delete")')
        await this.wait(300)
        await this.fill('div[role="alertdialog"] input[type="text"]', realm)
        await this.click('button:has-text("OK")')
        // wait for backend to response
        await this.wait(3000)
        try {
            const count = await this.count('[aria-label="attribute list"] span:has-text("smartcity")')
            //const count = await this.page.locator('[aria-label="attribute list"] span:has-text("smartcity")').count()
            await expect(count).toBe(0)

            await this.goToRealmStartPage("master")
            await this.wait(500)
            const isVisible = await this.isVisible('#realm-picker')
            await expect(isVisible).toBeFalsy()
            await console.log("Realm: smartcity deleted,    " + timeCost(false) + "s")
        } catch (e) {
            console.log(e)
        }
    }

    /**
     * Delete a certain asset by its name
     * @param {String} asset asset's name 
     */
    async deleteSelectedAsset(asset) {
        await this.navigateToTab("Assets")
        let assetSelected = await this.count(`text=${asset}`)
        if (assetSelected > 0) {
            await this.click(`text=${asset}`)
            await this.click('.mdi-delete')
            await Promise.all([
                this.click('button:has-text("Delete")')
            ]);
        }
    }



    /**
     * Save
     */
    async save() {
        await this.click('#edit-container')
        await this.click('button:has-text("Save")')
    }

    /**
        *          *******          ********       ***********        *           *           * * * *
        *         *                 *                   *             *           *           *      *
        *        *                  *                   *             *           *           *       *
        *         *                 *                   *             *           *           *       *
        *          *******          ********            *             *           *           * * * *
        *                 *         *                   *             *           *           * 
        *                  *        *                   *              *         *            *
        *                 *         *                   *               *       *             *
        *          *******          ********            *                 * * *               *
        *              
        */


    /**
    *  setup the testing environment by giving the realm name and setup level
    *  // lv0 is no setup at all
    *  // lv1 is to create a realm
    *  // lv2 is to create a user
    *  // lv3 is to create empty assets
    *  // lv4 is to set the values for assets
    * @param {String} realm realm name
    * @param {String} level level (lv0, lv1, etc.)
    * @param {String} configOrLoction update on config or location, default as no
    */
    async setup(realm, level, configOrLocation = "no") {

        global.startTime = new Date() / 1000

        // clean storage
        // if (fs.existsSync('test/storageState.json')) {
        //     fs.unlinkSync('test/storageState.json')
        // }

        if (level !== "lv0") {
            await this.openApp("master")
            await this.wait(300)
            await this.login("admin")

            // add realm
            await this.wait(300)
            const isPickerVisible = await this.isVisible('#realm-picker')
            if (!isPickerVisible) {
                await this.navigateToMenuItem("Realms")
                await this.addRealm(realm)
            }
            await this.switchToRealmByRealmPicker(realm)

            const update = level == "lv4" ? true : false
            // add user
            if (level >= 'lv2') {
                await this.addUser("smartcity", global.passwords["smartcity"])
                // add assets
                if (level >= 'lv3') {
                    await this.logout();
                    await this.goToRealmStartPage(realm);
                    await this.login("smartcity");
                    await this.addAssets(update, configOrLocation);
                }
            }
            await this.logout()
            // await this.page.close()
            console.log(level + " setup takes " + timeCost(true) + "s")
        }
    }


    /**
     *        *****       *           ******            *             *       *
     *       *            *           *                * *            * *     *
     *      *             *           *               *   *           *  *    *
     *      *             *           ******         *  *  *          *   *   *
     *      *             *           *             *       *         *    *  *
     *       *            *           *            *         *        *     * *  
     *        *****       ******      ******      *           *       *       *
     *   
     */

    /**
     *  Clean up the environment
     *  Called in After() 
     */
    async cleanUp() {

        global.stepTime = new Date() / 1000

        // ensure login as admin into master
        await this.logout()
        await this.wait(700)
        await this.goToRealmStartPage("master")
        await this.login("admin")
        // must wait for the realm picker to be rendered
        await this.wait(600)
        const isPickerVisible = await this.isVisible('#realm-picker')
        if (isPickerVisible) {
            // switch to master realm to ensure being able to delete custom realm
            await this.switchToRealmByRealmPicker("master")
            // delete realms
            // should delete everything and set the envrioment to beginning
            await this.navigateToMenuItem("Realms")
            await this.deleteRealm("smartcity")
        }
        console.log("Clean up takes " + timeCost(false) + "s")
    }

    async logTime(startTime) {
        let endTime = new Date() / 1000
        console.log((endTime - startTime).toFixed(3) + "s")
    }
}


/**
 * browser setting
 */

// launch broswer
BeforeAll(async function () {
    global.browser = await playwright.chromium.launch({
        headless: true,
        slowMo: 50,
        launchOptions: {
            // force GPU hardware acceleration (even in headless mode)
            // without hardware acceleration, tests will be much slower
            args: ["--use-gl=desktop"]
        }
    });

    // if (fs.existsSync('test/storageState.json')) {
    //     context = await global.browser.newContext({
    //         storageState: 'test/storageState.json',
    //     });
    // }
    // else {
    context = await global.browser.newContext({ ignoreHTTPSErrors: true });
    //}
    let page = await context.newPage()
})

// open pages
Before(async function () {
    //global.startTime = new Date() / 1000
    this.page = await context.newPage();
})

// delete realm when a scenario ends
// delete a realm should be able to delete everything inside
// close page
After({ timeout: 100000 }, async function (testCase) {
    console.log("After start")

    // if test fails then take a screenshot
    global.stepTime = new Date() / 1000
    if (testCase.result.status === Status.FAILED) {
        await this.page?.screenshot({ path: join("test", 'screenshots', `${global.name}.png`) });
        await global.name++
    }

    await this.cleanUp()
    await this.page.close()
    console.log("After takes " + timeCost(false) + "s")
    console.log("This test takes " + timeCost(true) + "s")

})

// close browser and delete authentication file
AfterAll(async function () {
    await global.browser.close()
})

function timeCost(startAtBeginning) {
    let endTime = new Date() / 1000
    let startTime = startAtBeginning == true ? global.startTime : global.stepTime
    let timeDiff = (endTime - startTime).toFixed(3)
    return timeDiff
}

setDefaultTimeout(1000 * 15);
setWorldConstructor(CustomWorld);


