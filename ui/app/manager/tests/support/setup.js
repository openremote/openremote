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
    },
    {
        asset: "PV solar asset",
        name: "Solar",
    }
]

const DEFAULT_TIMEOUT = 10000;

class CustomWorld {

    /**
     *  CUSTOM METHODS
     */

    /**
     * 
     * @param { } realm String : Realm type (admin or other)
     * @param { } user String : User type (admin or other)
     */

    async navigate(realm, user) {
        var context
        var URL = realm == "admin" ? process.env.LOCAL_URL : process.env.SMARTCITY_URL
        if (fs.existsSync('storageState.json')) {
            context = await global.browser.newContext({
                storageState: 'storageState.json',
            });
            this.page = await context.newPage();
            await this.page.goto(URL);
        }
        else {
            context = await global.browser.newContext();
            this.page = await context.newPage();
            await this.page.goto(URL);
            this.login(user)
        }
    }

    /**
     * 
     * @param { } user String : User type (admin or other)
     */

    async login(user) {
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
     *          *******         ********      ***********        *           *           * * * *
     *         *                *                  *             *           *           *      *
     *        *                 *                  *             *           *           *       *
     *         *                *                  *             *           *           *       *
     *          *******         ********           *             *           *           * * * *
     *                 *        *                  *             *           *           * 
     *                  *       *                  *              *         *            *
     *                 *        *                  *               *       *             *
     *          *******         ********           *                 * * *               *
     * 
     *               
     */               

    /**
     *  create Realm
     */
    async addRealm() {
        // go to realm page
        await this.click('button[id="menu-btn-desktop"]');
        await this.click('text=Realms');
        // add realm
        await this.click('text=Add Realm');
        await this.fill('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]', 'smartcity')
        await this.page?.locator('input[type="text"]').nth(3).fill('smartcity');
        await Promise.all([
            this.page?.waitForNavigation(`${process.env.LOCAL_URL}/manager/#/realms`),
            this.click('button:has-text("create")')
        ]);
    }

    /**
     *  Create User
     * @param { } isRealmAdded Boolen
     */
    async addUser(isRealmAdded) {
        if (!isRealmAdded)
            await this.addRealm()
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
            }
            catch (error) {
                console.log('error' + error);
            }
        }
    }

    /**
     *  basic setup: only contains realm and user
     */
    async basicSetup() {

        let isRealmAdded = false

        await this.navigate("admin", "admin")
        // wait for the button to be visible
        await this.page.waitForTimeout(1000)
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
