const { Given, When, Then } = require("@cucumber/cucumber");


// add new asset
Given('Nevigate to asset page', async function () {
    await this.click('#desktop-left a:nth-child(2)')
});

Then("Create a {string} with name of {string}", async function (asset, name) {
    const { page } = this;

    await this.click('.mdi-plus');
    await this.click(`text=${asset}`);
    await page.locator('#name-input input[type="text"]').fill(name);
    await this.click('#add-btn')
})

When("Go to asset {string} info page", async function (name) {
    const { page } = this;
    await page.locator(`#list-container div:has-text(${name})`).nth(1).click();
})

Then("Give value to the {string} of {string}", async function (attribute, value) {
    const { page } = this;
    await page.locator()
})