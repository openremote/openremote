const { Given, When, Then } = require("@cucumber/cucumber");



// add new user
When("Navigate to user page", async function () {
    const { page } = this;
    await page.locator('button[id="menu-btn-desktop"]').click()
    await page.locator('li[role="menuitem"]:has-text("Users")').click();
})

Then("Add a new user", { timeout: 20000 }, async function () {

    const { page } = this;

    // type in username
    await page.locator('.mdi-plus').first().click();
    await page.locator('input[type="text"]').first().click();
    await page.locator('input[type="text"]').first().fill('smartcity');

    // type in password
    for (i = 0; i < 4; i++) {
        await page.keyboard.press("Tab")
    }
    await page.keyboard.type("smartcity")
    await page.keyboard.press("Tab")
    await page.keyboard.type("smartcity")
    // await page.locator('#password-user2 input[type="password"]').fill('smartcity');
    // await page.locator('#repeatPassword-user2 input[type="password"]').fill('smartcity');

    // select permissions
    await page.locator('div[role="button"]:has-text("Roles")').click();
    await page.locator('li[role="menuitem"]:has-text("Read")').click();
    await page.locator('.mdi-checkbox-blank-outline').click();

    //create
    await page.locator('button:has-text("create")').click();
})

// add new asset
Given('Nevigate to asset page', async function () {
    await this.click('#desktop-left a:nth-child(2)')
});

Then("Create a {string} with name of {string}", async function (asset, name) {
    const { page } = this;

    await page.locator('.mdi-plus').click();
    await page.locator(`text=${asset}`).click();
    await page.locator('#name-input input[type="text"]').fill(name);
    await page.locator('#add-btn').click()
})

When("Goes to asset {string} info page", async function (name) {
    const { page } = this;
    await page.locator(`#list-container div:has-text(${name})`).nth(1).click();
})

Then("Give value to the {sting} of {string}", async function (attribute, value) {
    const { page } = this;
    await page.locator()
})