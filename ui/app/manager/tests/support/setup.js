const { setWorldConstructor, BeforeAll, AfterAll, After, Before } = require("@cucumber/cucumber");
const { ChromiumBroswer } = require("playwright");
const playwright = require('playwright');
const fs = require('fs');
require('dotenv').config();

/**
 * command for running all the test
 *         npm test
 * 
 * command for running certain test/tests with the "tag"(@OpenRemote) 
 *         npm run tags "tag" (npm run tags "@OpenRemote")
 * 
 * command for viewing the reports
 *         npm run report
 * 
 * command for more.....
 */

var global = {
    browser: ChromiumBroswer
}

class CustomWorld {
    async navigateTo(url) {
        var context
        if (fs.existsSync('storageState.json')) {
            context = await global.browser.newContext({
                storageState: 'storageState.json',
            });
            this.page = await context.newPage();
        }
        else {
            context = await global.browser.newContext();
            this.page = await context.newPage();
            this.login()
        }
        await this.page.goto(url);
    }

    async login() {
        await this.page?.fill('#username', process.env.USERID)
        await this.page?.fill('#password', process.env.PASSWORD)
        await this.page?.press('body', 'Enter');
        await this.page?.context().storageState({ path: 'storageState.json' });
    }

    async click(button) {
        await this.page?.locator(button).click()
    }

}

// launch broswer
BeforeAll(async function () {
    global.browser = await playwright.chromium.launch({
        headless: false,
        slowMo: 300
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
