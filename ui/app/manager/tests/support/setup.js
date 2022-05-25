const { setWorldConstructor, BeforeAll, AfterAll, After } = require("@cucumber/cucumber");
const { ChromiumBroswer } = require("playwright");
const playwright = require('playwright');
const fs = require('fs');
const { env } = require("process");
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

const global = {
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
        config_item_2: "Store data points",
        config_attr_1: "energyLevel",
        config_attr_2: "power"
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
        location_x: 600,
        location_y: 200,
        config_item_1: "Rule state",
        config_item_2: "Store data points",
        config_attr_1: "power",
        config_attr_2: "powerForecast"
    }
]

const rules = [{ name: "Energy" }, { name: "Solar" }]


class CustomWorld {

    /**
     *  CUSTOM METHODS
     */

    /**
     * navigate to a page where if for the desired realm
     * @param { } realm String : Realm type (admin or other)
     */

    async navigate(realm) {
        let context
        let URL = realm == "admin" ? process.env.LOCAL_URL : process.env.SMARTCITY_URL
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
     * @param { } button STRING, selector for an element
     */
    async click(button) {
        await this.page?.locator(button).click()
    }

    /**
     * 
     * @param { } key String: location of selector
     */
    async press(key) {
        await this.page?.keyboard.press(key)
    }

    /**
     * Check and uncheck
     * @param {*} checkbox String: location of checkbox
     */
    async check(checkbox) {
        await this.page?.check(checkbox)
    }
    async uncheck(checkbox) {
        await this.page?.uncheck(checkbox)
    }

    /**
     * Logout and delete login certification
     */
    async logout() {
        if (fs.existsSync('storageState.json')) {
            fs.unlinkSync('storageState.json')
        }
        if (await this.page?.locator('#menu-btn-desktop').isVisible()) {
            await this.click('#menu-btn-desktop');
            await this.click('text=Log out');
        }
    }

    /**
     * 
     * @param { } locate String : location of selector 
     * @param { } value String : value
     */
    async fill(locate, value) {
        await this.page?.locator(locate).fill(value)
    }

    /**
     * drag to position_x and position_y
     * @param {*} position_x coordinator of screen pixel
     * @param {*} position_y coordinator of screen pixel
     */
    async drag(position_x, position_y) {
        await this.page.mouse.down()
        await this.page.mouse.move(position_x, position_y)
        await this.page.mouse.up()
    }

    async dragAndDrop(origin, destination) {
        await this.page.dragAndDrop(origin, destination)
    }

    /**
     * wait for millisecond
     * @param {*} millisecond number
     */
    async wait(millisecond) {
        await this.page?.waitForTimeout(millisecond)
    }

    /**
     *  Repeatable actions
     */

    /**
     * navigate to a setting page inside the manager 
     * for the setting list menu at the top right
     * @param {*} setting STRING, name of the setting menu item
     */
    async navigateTo(setting) {
        await this.click('button[id="menu-btn-desktop"]');
        await this.click(`text=${setting}`);
    }

    /**
     * navigate to a ceratin tab page
     * @param {*} tab STRING, tab name
     */
    async navigateToTab(tab) {
        await this.click(`#desktop-left a:has-text("${tab}")`)
    }

    /**
     *  create Realm
     */
    async addRealm(name) {
        if (!await this.page?.locator('[aria-label="attribute list"] span:has-text("smartcity")').isVisible()) {
            await this.click('text=Add Realm');
            await this.fill('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]', name)
            await this.page?.locator('input[type="text"]').nth(3).fill(name);
            await Promise.all([
                this.page?.waitForNavigation(`${process.env.LOCAL_URL}/manager/#/realms`),
                this.click('button:has-text("create")')
            ]);
        }
    }

    /**
     * switch to a new manager realms by giving URL
     * @param {*} name String: name of custom realm
     */
    async switchRealmByURL(name) {
        await this.logout()
        let URL = name == "admin" ? process.env.LOCAL_URL : process.env.SMARTCITY_URL
        await this.page?.goto(URL)
        await this.login(name)
    }

    /**
     * switch to a realm in the manager's realm picker
     * @param {*} name String: name of custom realm
     */
    async switchToRealmBySelector(name) {
        await this.click('#realm-picker');
        await this.click(`li[role="menuitem"]:has-text("${name}")`);
    }

    /**
     *  Create User
     * @param { } isRealmAdded Boolen
     */
    async addUser() {
        /**
         * add user
         */
        await this.wait(100)
        // go to user page
        await this.click('#menu-btn-desktop');
        await this.click('text=Users');
        await this.wait(400)
        // add user if not exsit
        if (!await this.page?.locator('main[role="main"] >> text=smartcity').isVisible()) {

            await this.click('.mdi-plus >> nth=0')
            await this.fill('input[type="text"] >> nth=0', 'smartcity')
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
        else {
            console.log("user exsits")
        }
    }

    /**
     * create new empty assets
     * @param {*} update Boolean: for checking if updating values is needed
     */
    async addAssets(update) {

        await this.wait(1000)

        // Goes to asset page
        await this.click('#desktop-left a:nth-child(2)')

        // create assets accroding to assets array
        for (let asset of assets) {
            try {
                if (!await this.page?.locator(`text=${asset.name}`).isVisible()) {
                    await this.click('.mdi-plus')
                    await this.click(`text=${asset.asset}`)
                    await this.fill('#name-input input[type="text"]', asset.name)
                    await this.click('#add-btn')
                    if (update) {
                        // update value in general panel
                        await this.updateAssets(asset.attr_3, asset.a3_type, asset.v3)

                        // update in modify mode
                        await this.click('button:has-text("Modify")')
                        await this.updateInModify(asset.attr_1, asset.a1_type, asset.v1)
                        await this.updateInModify(asset.attr_2, asset.a2_type, asset.v2)
                        await this.updateLocation(asset.location_x, asset.location_y)
                        await this.setConfigItem(asset.config_item_1, asset.config_item_2, asset.config_attr_1, asset.config_attr_2)
                        await this.save()

                        await this.wait(400)
                    }
                    await this.unSelectAll()
                }
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
        await this.wait(200)
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
    }

    /**
     * update the data in the modify mode
     * @param {*} attr STRING
     * @param {*} type STRING
     * @param {*} value STRING
     */
    async updateInModify(attr, type, value) {
        await this.fill(`text=${attr} ${type} >> input[type="number"]`, value)
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
    }

    /**
     * select two config items for an attribute
     * @param {*} item_1 STRING
     * @param {*} item_2 STRING
     * @param {*} attr STRING
     */
    async configItem(item_1, item_2, attr) {

        await this.click(`td:has-text("${attr} ") >> nth=0`)
        await this.wait(400)
        await this.click('.attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component')
        await this.click(`li[role="checkbox"]:has-text("${item_1}")`)
        await this.click(`li[role="checkbox"]:has-text("${item_2}")`)
        await this.click('div[role="alertdialog"] button:has-text("Add")')
        await this.click(`td:has-text("${attr}") >> nth=0`)
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

    async deleteRealm() {
        await this.click('[aria-label="attribute list"] span:has-text("smartcity")')
        await this.click('button:has-text("Delete")')
        await this.fill('div[role="alertdialog"] input[type="text"]', 'smartcity')
        await Promise.all([
            await this.click('button:has-text("OK")')
        ])
        await this.wait(100)
    }

    /**
     * delete all the assets
     * right now it's deleting all the assets in the array
     */
    async deleteAssets() {
        await this.click('#desktop-left a:nth-child(2)')

        for (let asset of assets) {
            if (await this.page?.locator(`text=${asset.name}`).count() > 0) {
                await this.click(`text=${asset.name}`)
                await this.click('.mdi-delete')
                await Promise.all([
                    this.click('button:has-text("Delete")')
                ]);
            }
        }
    }


    /**
     * delete all the rule
     * right now it's deleting all the rules in the array
     * i expect it to be possible to delete accroding to the UI 
     * (like detect all the rules in the rule tree and then delete)
     */
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
    async setup(realm, level) {

        // clean storage
        if (fs.existsSync('storageState.json')) {
            fs.unlinkSync('storageState.json')
        }

        // lv0 is no setup at all
        // lv1 is to create a realm
        // lv2 is to create a user
        // lv3 is to create empty assets
        // lv4 is to set the values for assets
        if (!(level === "lv0")) {
            await this.navigate("admin")
            await this.login("admin")
            // add realm
            await this.wait(400)
            if (!await this.page?.locator('#realm-picker').isVisible()) {
                await this.navigateTo("Realms")
                await this.addRealm(realm)
            }
            await this.switchToRealmBySelector(realm)

            const update = level == "lv4" ? true : false
            // add user
            if (level >= 'lv2') {
                await this.addUser()
                // add assets
                if (level >= 'lv3') {
                    await this.switchRealmByURL(realm)
                    await this.addAssets(update)
                }
            }
            await this.logout()
            await this.page.close()
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
     *  Clean up the enviroment
     *  Called in After() 
     */
    async cleanUp() {

        // ensure login as admin into master
        await this.logout()
        await this.page.goto(process.env.LOCAL_URL);
        await this.login("admin")
        await this.wait(400)
        // switch to master realm to ensure being able to delete custom realm
        await this.switchToRealmBySelector("master")

        // delete realms
        // should delete everything and set the envrioment to beginning
        await this.navigateTo("Realms")
        await this.deleteRealm()
    }

}


/**
 * broswer setting
 */

// launch broswer
BeforeAll(async function () {
    global.browser = await playwright.chromium.launch({
        headless: false,
        slowMo: 50
    });
})

// delete realm when a senario ends
// delete a realm should be able to delete everything inside
// close page
After({ timeout: 10000 }, async function () {
    await this.cleanUp()
    await this.page?.close()
})

// close browser and delete authentication file
AfterAll(async function () {
    await global.browser.close()
    if (fs.existsSync('storageState.json')) {
        fs.unlinkSync('storageState.json')
    }
})

setWorldConstructor(CustomWorld);
