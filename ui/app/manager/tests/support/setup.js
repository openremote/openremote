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

const DEFAULT_TIMEOUT = 10000;

class CustomWorld {

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


    async login(user) {
        if (user == "admin") {
            await this.page?.fill('#username', process.env.USER_LOCAL_ID)
            await this.page?.fill('#password', process.env.LOCAL_PASSWORD)
        }
        else {
            await this.page?.fill('#username', process.env.SMARTCITY)
            await this.page?.fill('#password', process.env.SMARTCITY)
        }

        await this.page?.keyboard.press('Enter');
        await this.page?.context().storageState({ path: 'storageState.json' });
    }


    async click(button) {
        await this.page?.locator(button).click()
    }

    async press(key) {
        await this.page?.press(key)
    }


    async logout() {
        await this.click('#menu-btn-desktop');
        await this.click('text=Log out');
        if (fs.existsSync('storageState.json')) {
            fs.unlinkSync('storageState.json')
        }
    }

    async fill(locate, value) {
        await this.page?.locator(locate).fill(value)
    }

    async addRealm() {
        /**
         * add realm
         */
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

    // basic only contains realms and users
    async basicSetup() {

        let isRealmAdded = false

        await this.navigate("admin", "admin")
        // wait for the button to be visible
        await this.page.waitForTimeout(1000)
        if (await this.page?.locator('#realm-picker').isVisible()) {
            await this.click('#realm-picker');
            if (await this.page?.locator('li[role="menuitem"]:has-text("smartcity")').count() > 0) {
                await this.click('li[role="menuitem"]:has-text("smartcity")')
                isRealmAdded = true
            }
        }
        await this.addUser(isRealmAdded)
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
