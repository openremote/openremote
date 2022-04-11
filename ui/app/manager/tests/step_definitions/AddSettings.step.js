const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

Given('Login OpenRemote local website as admin', { timeout: 10000 }, async function () {
    await this.navigate("admin", "admin")
})

// add new realm
When('Navigate to Realm page', async function () {

    await this.click('button[id="menu-btn-desktop"]');
    await this.click('text=Realms');
})

Then('Add a new Realm', async function () {
    const { page } = this;

    await this.click('text=Add Realm');
    await page.locator('#attribute-meta-row-1 >> text=Realm Enabled >> input[type="text"]').fill('smartcity');
    await page.locator('input[type="text"]').nth(3).fill('smartcity');
    await Promise.all([
        page.waitForNavigation(`${process.env.LOCAL_URL}/manager/#/realms`),
        this.click('button:has-text("create")')
    ]);
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


/**
 * add new user
 */
Given("Switch to smartcity realm", async function () {

    await this.click('#realm-picker');
    await this.click('li[role="menuitem"]:has-text("smartcity")');
})


When("Navigate to user page", async function () {
    await this.click('#menu-btn-desktop');
    await this.click('text=Users');
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
    await this.click('div[role="button"]:has-text("Roles")');
    await this.click('li[role="menuitem"]:has-text("Read")');
    await this.click('li[role="menuitem"]:has-text("Write")');
    await page.locator('div[role="button"]:has-text("Roles")').click({timeout: 1000});

    //create
    await page.locator('button:has-text("create")').click();
})

/**
 * switch user
 */
When('Logout',async function(){


    await this.click('#menu-btn-desktop');
    await this.click('text=Log out');
    await this.logout();
})

Then('Go to new Realm and login',async function(){
    const { page } = this;

    await page.goto(process.env.SMARTCITY_URL)
    await this.login("smartcity")
})

