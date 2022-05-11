const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");



Then('Add a new Realm', async function () {

    await this.addRealm()
})

/**
 * switch realm 
*/
When('Select smartcity realm', async function () {
    await this.click('#realm-picker');
    await this.click('li[role="menuitem"]:has-text("smartcity")');
})


Then('We see the smartcity realm', async function () {
    const { page } = this;

    // textcontent() will return muiltiple lines 
    const text = await page.locator('div[id="realm-picker"]').innerText()
    await expect(text).toBe("smartcity")
})