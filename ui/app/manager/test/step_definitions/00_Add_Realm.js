const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");



Then('Add a new Realm', { timeout: 100000 }, async function () {
    let startTime =new Date() / 1000
    await this.addRealm("smartcity")
    this.logTime(startTime)
})

/**
 * switch realm 
*/
When('Select smartcity realm', async function () {
    let startTime =new Date() / 1000
    await this.click('#realm-picker');
    await this.click('li[role="menuitem"]:has-text("smartcity")');
    this.logTime(startTime)
})


Then('We see the smartcity realm', async function () {
    const { page } = this;
    let startTime =new Date() / 1000
    // textcontent() will return muiltiple lines 
    const text = await page.locator('div[id="realm-picker"]').innerText()
    await expect(text).toBe("smartcity")
    this.logTime(startTime)
})
