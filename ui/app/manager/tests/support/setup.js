const { setWorldConstructor, BeforeAll, AfterAll, After, Before, setDefaultTimeout } = require("@cucumber/cucumber");
const { ChromiumBroswer } = require("playwright");
const playwright = require('playwright');
const fs = require('fs');
require('dotenv').config();

/**
 * command for running all the test
 *         yarn test
 * 
 * command for running certain test/tests with the "tag"(@OpenRemote) 
 *         yarn run tags "tag" (yarn run tags "@OpenRemote")
 * 
 * command for viewing the reports
 *         yarn run report
 * 
 * command for more.....
 */

var global = {
    browser: ChromiumBroswer,
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
        config_item_2: "Store data points"
    },
    {
        asset: "PV solar asset",
        name: "Solar",
        attr_1: "panelPitch",
        attr_2: "power",
        attr_3: "powerForecast",
        a1_type: "Positive integer",
        a2_type: "Number",
        a3_type: "number",
        v1: "30",
        v2: "70",
        v3: "100",
        location_x: 540,
        location_y: 110,
        config_item_1: "Rule state",
        config_item_2: "Store data points"
    }
]

const rules = [{ name: "Energy" }, { name: "Solar" }]


const DEFAULT_TIMEOUT = 10000;

class CustomWorld {

    /**
     *  CUSTOM METHODS
     */

    /**
     * 
     * @param { } realm String : Realm type (admin or other)
     */

    async navigate(realm) {
        var context
        var URL = realm == "admin" ? process.env.LOCAL_URL : process.env.SMARTCITY_URL
        if (fs.existsSync('storageState.json')) {
            context = await global.browser.newContext({
                storageState: 'storageState.json',
            });
        }
        else {
            context = await global.browser.newContext();

        }
        this.page = await context.newPage();
        await this.page.goto(URL);
    }

    /**
     * 
     * @param { } user String : User type (admin or other)
     */

    async login(user) {
        if (!fs.existsSync('storageState.json')) {
            if (user == "admin") {
                await this.page?.fill('input[name="username"]', process.env.USER_LOCAL_ID)
                await this.page?.fill('input[name="password"]', process.env.LOCAL_PASSWORD)
            }
            else {
                await this.page?.fill('input[name="username"]', process.env.SMARTCITY)
                await this.page?.fill('input[name="password"]', process.env.SMARTCITY)
            }
            await this.page?.keyboard.press('Enter');
            await this.page?.context().storageState({ path: 'storageState.json' });
        }
    }

    /**
     * 
     * @param {location selector as STRING} button 
     */

    async click(button) {
        await this.page?.locator(button).click()
    }

    /**
     * 
     * @param { } key String: location of selector
     */
    async press(key) {
        await this.page?.press(key)
    }

    /**
     * Logout and delete login certification
     */
    async logout() {
        await this.click('#menu-btn-desktop');
        await this.click('text=Log out');
        if (fs.existsSync('storageState.json')) {
            fs.unlinkSync('storageState.json')
        }
    }

    /**
     * 
     * @param {} locate String : location of selector 
     * @param { } value String : value
     */
    async fill(locate, value) {
        await this.page?.locator(locate).fill(value)
    }

    /**
     *  Repeatable actions
     */

    /**
     * navigation
     */
    async navigateTo(setting) {
        await this.click('button[id="menu-btn-desktop"]');
        await this.click(`text=${setting}`);
    }

    /**
     *  create Realm
     */
    async addRealm(name) {

        // add realm
        await this.click('text=Add Realm');
        await this.fill('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]', name)
        await this.page?.locator('input[type="text"]').nth(3).fill(name);
        await Promise.all([
            this.page?.waitForNavigation(`${process.env.LOCAL_URL}/manager/#/realms`),
            this.click('button:has-text("create")')
        ]);
    }

    async switichToRealm(name) {
        await this.click('#realm-picker');
        await this.click(`li[role="menuitem"]:has-text("${name}")`);
    }

    /**
     *  Create User
     * @param { } isRealmAdded Boolen
     */
    async addUser(isRealmAdded) {
        if (!isRealmAdded)
            await this.addRealm("smartcity")
        /**
         * add user
         */
        // swithch to smartcity realm
        await this.click('#realm-picker');
        await this.click('li[role="menuitem"]:has-text("smartcity")');
        // go to user page
        await this.click('#menu-btn-desktop');
        await this.click('text=Users');
        // add user
        await this.page?.locator('.mdi-plus').first().click();
        await this.page?.locator('input[type="text"]').first().fill('smartcity');
        // type in password
        await this.fill('#password-user0 input[type="password"]', 'smartcity')
        await this.fill('#repeatPassword-user0 input[type="password"]', 'smartcity')
        // select permissions
        await this.click('div[role="button"]:has-text("Roles")');
        await this.click('li[role="menuitem"]:has-text("Read")');
        await this.click('li[role="menuitem"]:has-text("Write")');
        await this.page?.locator('div[role="button"]:has-text("Roles")').click({ timeout: 1000 });
        // create user
        await this.click('button:has-text("create")')
    }

    /**
     * Create empty asset
     * TODO: set optional parameter
     */
    async addAssets() {

        await this.page.waitForTimeout(1000)

        // Goes to asset page
        await this.click('#desktop-left a:nth-child(2)')

        // create assets accroding to assets array
        for (let asset of assets) {
            try {
                await this.click('.mdi-plus')
                await this.click(`text=${asset.asset}`)
                await this.fill('#name-input input[type="text"]', asset.name)
                await this.click('#add-btn')
                await this.unSelectAll()
            }
            catch (error) {
                console.log('error' + error);
            }
        }
    }

    /**
     * unselect the asset
     */
    async unSelectAll() {

        // leave modify mode
        if (await this.page?.locator('button:has-text("View")').isVisible()) {
            await this.click('button:has-text("View")')
            console.log("view clicked")
        }

        // unselect the asset
        if (await this.page?.locator('.mdi-close').first().isVisible()) {
            await this.page?.locator('.mdi-close').first().click()
            console.log("unselected")
        }

    }

    /**
     *  Select assets
     */
    async selectAssets(name) {
        await this.click(`text=${name}`)

        console.log(name + " select succeed")
    }


    /**
     * update asset in the general panel
     * @param {*} attr STRING
     * @param {*} type STRING
     * @param {*} value STRING
     */
    async updateAssets(attr, type, value) {
        await this.fill(`#field-${attr} input[type="${type}"]`, value)
        await this.click(`#field-${attr} #send-btn span`)

        console.log(attr + " update succeed")
    }

    /**
     * update the data in the modify mode
     * @param {*} attr STRING
     * @param {*} type STRING
     * @param {*} value STRING
     */
    async updateInModify(attr, type, value) {

        await this.fill(`text=${attr} ${type} >> input[type="number"]`, value)
        console.log(attr + " modify succeed")
    }

    /**
     * update location so we can see in the map
     * @param {*} location_x int
     * @param {*} location_y int
     */
    async updateLocation(location_x, location_y) {
        await this.click('text=location GEO JSON point >> button span')
        await this.page?.mouse.click(location_x, location_y, { delay: 1000 })
        await this.click('button:has-text("OK")')
        console.log("location succeed")
    }

    /**
     * select two config items for an attribute
     * @param {*} item_1 STRING
     * @param {*} item_2 STRING
     * @param {*} attr STRING
     */
    async configItem(item_1, item_2, attr) {
        await this.page.locator(`td:has-text("${attr} ")`).first().click()
        await this.page.waitForTimeout(500)
        console.log(attr + " opened")
        await this.click('.attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component')
        await this.click(`li[role="checkbox"]:has-text("${item_1}")`)
        await this.click(`li[role="checkbox"]:has-text("${item_2}")`)
        await this.click('div[role="alertdialog"] button:has-text("Add")')
        await this.page.locator(`td:has-text("${attr}")`).first().click()
    }

    /**
     * set config item for rule and insight to use
     * @param {*} item1 STRING
     * @param {*} item2 STRING
     * @param {*} attr STRING
     */
    async setConfigItem(item_1, item_2, attr_1, attr_2) {
        await this.configItem(item_1, item_2, attr_1)
        await this.configItem(item_1, item_2, attr_2)
    }

    /**
     * delete all the assets
     */
    async deleteAssets() {
        await this.click('#desktop-left a:nth-child(2)')

        for (let asset of assets) {
            if (this.page?.locator(`text=${asset.name}`).count() > 0) {
                await this.click(`text=${asset.name}`)
                await this.click('.mdi-delete')
                await Promise.all([
                    this.click('button:has-text("Delete")')
                ]);
            }
        }
    }

    async deleteRules() {
        await this.click('#desktop-left a:nth-child(3)')

        for (let rule of rules) {
            await this.click(`text=${rule.name}`)
            await this.click('.mdi-delete')
            await this.click('button:has-text("Delete")')
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
     *  fundamental setup: only contains realm
     */
    async fundamentalSetup(name) {

        await this.navigate("admin")
        await this.login("admin")

        await this.page.waitForTimeout(1000)
        if (!await this.page?.locator('#realm-picker').isVisible()) {
            await console.log("cant be seen")
            await this.navigateTo("Realms")
            await this.addRealm(name)
        }
        await this.switichToRealm(name)
    }


    /**
     *  basic setup: only contains realm and user
     */
    async basicSetup() {

        let isRealmAdded = false

        await this.navigate("admin")
        await this.login("admin")
        // wait for the button to be visible
        await this.page.waitForTimeout(500)
        // set isRealmAdded to true only when smartcity realm is there
        if (await this.page?.locator('#realm-picker').isVisible()) {
            await this.click('#realm-picker');
            if (await this.page?.locator('li[role="menuitem"]:has-text("smartcity")').count() > 0) {
                await this.click('li[role="menuitem"]:has-text("smartcity")')
                isRealmAdded = true
            }
        }

        await this.addUser(isRealmAdded)
    }

    /**
     * convention setup: contains realm, user and emtpy assets
     */
    async conventionSetup() {

        // create realm and user
        await this.basicSetup();

        // logout
        await this.logout();

        // nevigate to smartcity
        await this.page?.goto(process.env.SMARTCITY_URL)
        await this.login("smartcity")

        // create assets
        await this.addAssets()
    }

    /**
     *  thorough setup: contains realm, user and assets with data and configuration items
     */
    async thoroughSetup() {
        await this.conventionSetup();

        for (let asset of assets) {

            await this.unSelectAll()
            // select assets
            await this.selectAssets(asset.name)

            // update value in general panel
            await this.updateAssets(asset.attr_3, asset.a3_type, asset.v3)

            // update in modify mode
            await this.click('button:has-text("Modify")')
            await this.updateInModify(asset.attr_1, asset.a1_type, asset.v1)
            await this.updateInModify(asset.attr_2, asset.a2_type, asset.v2)
            await this.updateLocation(asset.location_x, asset.location_y)
            await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.attr_1, asset.attr_2)
            await this.save()

            await this.page.waitForTimeout(500)
            console.log("item set")
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
     * Delete realm
     * not possible right now since there is no way to delete a realm in graphic UI
     */
    async fundamentalClean() {

    }

    /**
     * Delete user
     */
    async basicClean() {

        await this.navigate("admin")
        if (!fs.existsSync('storageState.json')) {
            await this.login("admin")
        }

        await this.click('#realm-picker');
        await this.click('li[role="menuitem"]:has-text("smartcity")')

        await this.click('#menu-btn-desktop');
        await this.click('text=Users');

        await this.click('td:has-text("smartcity")')
        await this.click('button:has-text("Delete")')
        await this.click('div[role="alertdialog"] button:has-text("Delete")')
    }

    /**
     *  Delete user and assets
     */
    async conventionClean() {
        await this.basicClean()
        await this.deleteAssets()
    }


    /**
     * Delete user, assets, rules and insights
     */
    async thoroughClean() {
        await this.conventionClean()
        await this.deleteRules()

        // insights are not there yet
    }
}

// launch broswer
BeforeAll(async function () {
    global.browser = await playwright.chromium.launch({
        headless: false,
        slowMo: 100
    });
})


// close page
After(async function () {
    await this.page.close()
})


// close browser and delete authentication file
AfterAll(async function () {
    await global.browser.close()
    if (fs.existsSync('storageState.json')) {
        fs.unlinkSync('storageState.json')
    }
})


//setDefaultTimeout(DEFAULT_TIMEOUT)
setWorldConstructor(CustomWorld);
