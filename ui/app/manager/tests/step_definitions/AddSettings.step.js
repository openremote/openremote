const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

Given('Login OpenRemote local website as admin', { timeout: 10000 }, async function () {
    await this.navigate("admin", "admin")
})

// add new realm
When('Navigate to Realm page', async function () {
    const { page } = this;

    await page.locator('button[id="menu-btn-desktop"]').click()
    await page.locator('text=Realms').click();
})

Then('Add a new Realm', async function () {
    const { page } = this;

    await page.locator('text=Add Realm').click();
    await page.locator('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]').fill('smartcity');
    await page.locator('input[type="text"]').nth(3).fill('smartcity');
    await Promise.all([
        page.waitForNavigation(`${process.env.LOCAL_URL}/manager/#/realms`),
        page.locator('button:has-text("create")').click()
    ]);
})



/**
 * switch realm 
*/
When('Select smartcity realm', async function () {
    const { page } = this;

    await page.locator('#realm-picker').click();
    await page.locator('li[role="menuitem"]:has-text("smartcity")').click();
})

Then('We see the smartcity realm', async function () {
    const { page } = this;

    // textcontent() will return muiltiple lines 
    const text = await page.locator('div[id="realm-picker"]').innerText()
    await expect(text).toBe("smartcity")
})


/**
 * add new user
 */
Given("Switch to smartcity realm", async function () {
    const { page } = this;

    await page.locator('#realm-picker').click();
    await page.locator('li[role="menuitem"]:has-text("smartcity")').click();
})


When("Navigate to user page", async function () {
    const { page } = this;

    await page.locator('#menu-btn-desktop').click();
    await page.locator('text=Users').click();
})

Then("Add a new user", { timeout: 10000 }, async function () {

    const { page } = this;

    // type in username
    await page.locator('.mdi-plus').first().click();
    // await page.locator('input[type="text"]').first().click();
    await page.locator('input[type="text"]').first().fill('smartcity');

    // type in password
    // for (i = 0; i < 4; i++) {
    //     await page.keyboard.press("Tab")
    // }
    // await page.keyboard.type("smartcity")
    // await page.keyboard.press("Tab")
    // await page.keyboard.type("smartcity")

    // type in password
    await page.locator('#password-user0 input[type="password"]').fill('smartcity');
    await page.locator('#repeatPassword-user0 input[type="password"]').fill('smartcity');

    // select permissions
    await page.locator('div[role="button"]:has-text("Roles")').click();
    await page.locator('li[role="menuitem"]:has-text("Read")').click();
    await page.locator('li[role="menuitem"]:has-text("Write")').click();
    await page.keyboard.press("Enter")

    //create
    await page.locator('button:has-text("create")').click();
})




