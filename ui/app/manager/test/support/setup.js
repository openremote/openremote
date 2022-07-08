const { setWorldConstructor, World, BeforeAll, AfterAll, After, Status, setDefaultTimeout } = require("@cucumber/cucumber");
const { ChromiumBroswer: ChromiumBrowser } = require("playwright");
const playwright = require('playwright');
const { expect } = require("@playwright/test");
const { join } = require('path');
const { Before } = require("@cucumber/cucumber");
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
        await this.wait(1500)
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

        setStepStartTime()
        await this.wait(500)
        const isLogin = await this.isVisible('input[name="username"]') || false
        if (isLogin) {
            let password = global.passwords[user];
            await this.page?.fill('input[name="username"]', user);
            await this.page?.fill('input[name="password"]', password);
            await this.page?.keyboard.press('Enter');
            console.log(`User: "${user}" logged in,   ` + timeCost(false) + "s")
        }
    }


    /**
     * Logout and delete login certification
     */
    async logout() {
        const isPanelVisibile = await this.isVisible('button:has-text("Cancel")')
        if (isPanelVisibile) {
            await this.click('button:has-text("Cancel")')
        }
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
    async wait(millisecond) {
        await this.page.waitForTimeout(millisecond);
    }

    /**
     * check how many elements are there in the page
     * @param {String} element css selector
     * @returns {Int} number of elements
     */
    async count(element) {
        return await this.page.locator(element).count()
    }

    /**
     * check if the element is visible in the page
     * Note that this element should be the only one in the page
     * @param {String} element css selector
     * @returns {Boolean} if the element if visible 
     */
    async isVisible(element) {
        if (element != null)
            return await this.page.locator(element).isVisible()
        else
            return false
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
        setStepStartTime()
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
        console.log(`Navigated to "${setting}" meun item,   ` + timeCost(false) + 's')
    }

    /**
     * navigate to a certain tab page
     * @param {String} tab tab name
     */
    async navigateToTab(tab) {
        await this.click(`#desktop-left a:has-text("${tab}")`)
        await this.wait(1500)
    }

    /**
     *  create Realm with name
     * @param {String} name realm name
     */
    async addRealm(name, first = false) {

        setStepStartTime()

        await this.wait(500)
        const isVisible = await this.isVisible(`[aria-label="attribute list"] span:has-text("${name}")`)
        if (!isVisible) {
            await this.click('text=Add Realm');
            await this.fill('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]', name)

            await this.page?.locator('input[type="text"]').nth(3).fill(name);
            await this.click('button:has-text("create")')

            await this.wait(first == true ? 15000 : 10000)
            // const count = await this.count(`[aria-label="attribute list"] span:has-text("${name}")`)
            // await expect(count).toEqual(1)
            await console.log("Realm: " + `"${name}"` + " added,   " + timeCost(false) + "s")
        }
    }

    /**
     * switch to a realm in the manager's realm picker
     * @param {String} name name of custom realm
     */
    async switchToRealmByRealmPicker(name) {
        await this.wait(500)
        await this.click('#realm-picker');
        await this.wait(500)
        await this.click(`li[role="menuitem"]:has-text("${name}")`)
    }

    /**
     * Create user
     * @param {String} username 
     * @param {String} password 
     */
    async addUser(username, password) {

        setStepStartTime()
        /**
         * add user
         */
        await this.wait(100)
        // go to user page
        await this.click('#menu-btn-desktop');
        await this.click('text=Users');
        await this.wait(500)
        const isVisible = await this.isVisible('main[role="main"] >> text=' + username)
        // add user if not exist
        if (!isVisible) {
            // type in name
            await this.click('.mdi-plus >> nth=0')
            await this.fill('input[type="text"] >> nth=0', username)
            // type in password
            await this.fill('#password-user0 input[type="password"]', password)
            await this.fill('#repeatPassword-user0 input[type="password"]', password)
            // select permissions
            await this.click('div[role="button"]:has-text("Realm Roles")')
            await this.click('li[role="menuitem"]:has-text("Default-roles-smartcity")')
            await this.click('div[role="button"]:has-text("Manager Roles")');
            await this.click('li[role="menuitem"]:has-text("Read")');
            await this.click('li[role="menuitem"]:has-text("Write")');
            await this.wait(1500)
            await this.click('div[role="button"]:has-text("Manager Roles")');
            // create user
            await this.click('button:has-text("create")')
            await this.wait(1500)
            console.log(`User: "${username}" added,    ` + timeCost(false) + "s")
        }
        else {
        }
    }
    /**
    * Switch between modify mode and view mode
    * @param {String} targetMode view or modify
    */
    async switchMode(targetMode) {
        await this.wait(400)
        const atModifyMode = await this.isVisible('button:has-text("View")')
        const atViewMode = await this.isVisible('button:has-text("Modify")')

        if (atModifyMode && (targetMode == "view")) {
            await this.click('button:has-text("View")')
            console.log(":::::: at view mode")
        }
        if (atViewMode && (targetMode == "modify")) {
            await this.click('button:has-text("Modify")')
            console.log(":::::: at modify mode")
        }
    }

    /**
     * create new empty assets
     * @param {Boolean} update for checking if updating values is needed
     */
    async addAssets(update, configOrLoction) {

        const addAssetTime = new Date() / 1000

        await this.wait(500)

        // Goes to asset page
        await this.click('#desktop-left a:nth-child(2)')

        // select conosle first to enter into the modify mode
        await this.click(`#list-container >> text="Consoles"`)
        await this.switchMode("modify")
        await this.unselect()

        // create assets accroding to assets array
        for (let asset of assets) {
            setStepStartTime()
            let isAssetVisible = await this.isVisible(`#list-container >> text=${asset.name}`)
            try {
                if (!isAssetVisible) {
                    await this.click('.mdi-plus')
                    await this.click(`text=${asset.asset}`)
                    await this.fill('#name-input input[type="text"]', asset.name)
                    await this.click('#add-btn')
                    await this.wait(500)
                    // check if at modify mode
                    // if yes we should see the save button then save
                    const isSaveBtnVisible = await this.isVisible('button:has-text("Save")')
                    console.log("save btn is " + isSaveBtnVisible)
                    if (isSaveBtnVisible) {
                        console.log("ready to save")
                        await this.click('button:has-text("Save")')
                    }
                    console.log(":::::: emtpy asset has been added")
                    await this.switchMode("modify")
                    // await this.unselect()
                    // await this.click(`#list-container >> text=${asset.name}`)
                    if (update) {
                        // switch to modify mode if at view mode


                        // update in modify mode
                        if (configOrLoction == "location") {
                            await this.updateLocation(asset.location_x, asset.location_y)
                            console.log(":::::: location updated")
                        }
                        else if (configOrLoction == "config") {
                            await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2)
                            console.log(":::::: config items have been added")
                        }
                        else {
                            await this.updateLocation(asset.location_x, asset.location_y)
                            await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2)
                            console.log(":::::: both settings have been added")
                        }

                        await this.updateInModify(asset.attr_1, asset.a1_type, asset.v1)
                        await this.updateInModify(asset.attr_2, asset.a2_type, asset.v2)

                        await this.save()

                        //switch to view mode
                        await this.switchMode("view")
                        // update value in view mode
                        await this.updateAssets(asset.attr_3, asset.a3_type, asset.v3)
                        await this.wait(500)

                        //switch to modify mode
                        await this.switchMode("modify")
                    }
                    await this.unselect()
                    console.log("Asset: " + `"${asset.name}"` + " with " + configOrLoction + " updated has been added,  " + timeCost(false) + "s")
                }
            }
            catch (error) {
                console.log('error' + error);
            }
        }
        console.log("Adding assets takes " + (new Date() / 1000 - addAssetTime).toFixed(3) + "s")
    }

    /**
     * unselect the asset
     */
    async unselect() {
        await this.wait(500)
        const isCloseVisible = await this.isVisible('.mdi-close >> nth=0')

        // leave modify mode
        // if (isViewVisible) {
        //     await this.click('button:has-text("View")')
        //     let btnDisgard = await this.isVisible('button:has-text("Disgard")')
        //     if (btnDisgard) {
        //         await this.click('button:has-text("Disgard")')
        //         console.log("didn't save successfully")
        //     }
        // }

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
        console.log("::::::  " + attr + " has been updated")
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
        await this.wait(500)
        await this.click(`td:has-text("${attr} ") >> nth=0`)
        await this.wait(500)
        await this.click('.attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component')
        await this.click(`li[role="checkbox"]:has-text("${item_1}")`)
        await this.click(`li[role="checkbox"]:has-text("${item_2}")`)
        await this.click('div[role="alertdialog"] button:has-text("Add")')
        await this.wait(500)

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
        await this.wait(500)
        await this.configItem(item_1, item_2, attr_2)
        await this.wait(500)
    }

    /**
     * Delete a certain realm by its name
     * @param {String} name Realm's name
     */
    async deleteRealm(realm) {

        setStepStartTime()
        await this.wait(500)
        await this.click(`[aria-label="attribute list"] span:has-text("${realm}")`)
        await this.click('button:has-text("Delete")')
        await this.wait(500)
        await this.fill('div[role="alertdialog"] input[type="text"]', realm)
        await this.click('button:has-text("OK")')
        // wait for backend to response
        await this.wait(5000)
        try {
            const count = await this.count('[aria-label="attribute list"] span:has-text("smartcity")')
            await expect(count).toBe(0)

            await this.goToRealmStartPage("master")
            await this.wait(500)
            const isVisible = await this.isVisible('#realm-picker')
            await expect(isVisible).toBeFalsy()
            await console.log(`Realm: "${realm}" deleted,    ` + timeCost(false) + "s")
        } catch (e) {
            console.log(e)
        }
    }

    /**
     * Delete a certain asset by its name
     * @param {String} asset asset's name 
     */
    async deleteSelectedAsset(asset) {
        setStepStartTime()
        await this.navigateToTab("Assets")
        let assetSelected = await this.count(`text=${asset}`)
        if (assetSelected > 0) {
            await this.click(`text=${asset}`)
            await this.click('.mdi-delete')
            await this.click('button:has-text("Delete")')
            await this.wait(1500)
            let visibile = await this.count(`text=${asset}`)
            await expect(visibile).toBeFalsy()
        }
        else {
            console.log(`Asset: "${asset}" does not exsit`)
        }
        console.log(`Asset: "${asset}" has been deleted,    ` + timeCost(false) + "s")
    }



    /**
     * Save
     */
    async save() {
        console.log(":::::: in saving")
        await this.wait(200)
        await this.click('#edit-container')
        await this.wait(200)  // wait for button to enabled 
        const isSaveBtnVisible = await this.isVisible('button:has-text("Save")')
        if (isSaveBtnVisible) {
            await this.click('button:has-text("Save")')
        }
        await this.wait(200)
        const isDisabled = await this.page.locator('button:has-text("Save")').isDisabled()
        //asset modify
        const ifModifyMode = await this.isVisible('button:has-text("OK")')
        if (ifModifyMode) {
            await this.click('button:has-text("OK")')
            console.log("panel closed")
        }
        if (!isDisabled) {
            await this.click('button:has-text("Save")')
            await this.wait(200)
        }
        await expect(await this.page.locator('button:has-text("Save")')).toBeDisabled()
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

        if (level !== "lv0") {
            await this.openApp("master")
            await this.login("admin")

            await this.wait(1500)
            const isPickerVisible = await this.isVisible('#realm-picker')
            // add realm
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

        const cleanTime = new Date() / 1000

        // ensure login as admin into master
        await this.wait(500)
        await this.goToRealmStartPage("master")
        await this.login("admin")
        // must wait for the realm picker to be rendered
        await this.wait(1500)
        const isPickerVisible = await this.isVisible('#realm-picker')
        if (isPickerVisible) {
            // switch to master realm to ensure being able to delete custom realm
            await this.switchToRealmByRealmPicker("master")
            // delete realms
            // should delete everything and set the envrioment to beginning
            await this.navigateToMenuItem("Realms")
            await this.deleteRealm("smartcity")
        }
        console.log("Clean up takes " + (new Date() / 1000 - cleanTime).toFixed(3) + "s")
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
            args: ["--use-gl=desktop"],
        }
    });
    context = await global.browser.newContext({ ignoreHTTPSErrors: true });

    let page = await context.newPage()
})

// open pages
Before(async function () {
    this.page = await context.newPage();
})

// delete realm when a scenario ends
// delete a realm should be able to delete everything inside
// close page
After({ timeout: 100000 }, async function (testCase) {
    console.log("After start")

    // if test fails then take a screenshot
    setStepStartTime()
    if (testCase.result.status === Status.FAILED) {
        await this.page?.screenshot({ path: join("test", 'screenshots', `${global.name}.png`) });
        await global.name++
    }

    await this.cleanUp()
    await this.page.close()
    console.log("This test takes " + timeCost(true) + "s")

})

// close browser and delete authentication file
AfterAll(async function () {
    await global.browser.close()
})

/**
 * set the start time of a single test
 */
function setStepStartTime() {
    global.stepTime = new Date() / 1000
}

/**
 * calculate the the time cost from either the start of all tests or a single
 * @param {Boolean} startAtBeginning 
 * @returns 
 */
function timeCost(startAtBeginning) {
    let endTime = new Date() / 1000
    let startTime = startAtBeginning == true ? global.startTime : global.stepTime
    let timeDiff = (endTime - startTime).toFixed(3)
    return timeDiff
}


setDefaultTimeout(1000 * 22);
setWorldConstructor(CustomWorld);


