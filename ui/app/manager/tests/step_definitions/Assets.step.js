const { Given, When, Then } = require("@cucumber/cucumber");


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

Then("Give value to the {string} of {string}", async function (attribute, value) {
    const { page } = this;
    await page.locator()
})