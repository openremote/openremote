const { setWorldConstructor, BeforeAll, AfterAll, After, Before } = require("@cucumber/cucumber");
const { chromium, ChromiumBroswer } = require("playwright");
const playwright = require('playwright');

var global = {
    browser: ChromiumBroswer
}

class CustomWorld {
    async navigateTo(url) {
        const context = await global.browser.newContext();
        this.page = await context.newPage();
        await this.page.goto(url);
    }

    async login(){
        await this.page?.fill('#username', 'smartcity')
        await this.page?.fill('#password', 'smartcity')
        await this.page?.press('body', 'Enter');
    }

    async click(button) {
        await this.page?.locator(button).click()
    }
}

BeforeAll(async function () {
    global.browser = await playwright.chromium.launch({
        headless: false,
        slowMo:300
    });
})

After(async function(){
    await this.page.close()
})

AfterAll(async function () {
    await global.browser.close()
})


setWorldConstructor(CustomWorld);
