const { setWorldConstructor, BeforeAll, AfterAll, After, Before } = require("@cucumber/cucumber");
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
        if (fs.existsSync('storageState.json')) {
            fs.unlinkSync('storageState.json')
        }
    }

    async fill(locate, value) {
        await this.page?.locator(locate).fill(value)
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

setWorldConstructor(CustomWorld);
