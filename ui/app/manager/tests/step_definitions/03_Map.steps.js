const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

When('Check {string} on map', async function (asset) {
    const { page } = this;

    await this.wait(400)
    await page.locator('or-icon svg path').first().click();

    const cardPanel = await page.waitForSelector('or-map-asset-card')
    await expect(cardPanel).not.toBeNull()

})

Then('Click and nevigate to {string} page', async function (asset) {

    await this.click('button:has-text("View")')
})

Then('We are at {string} page', async function (asset) {
    const { page } = this
    const assetPage = await page.waitForSelector(`#asset-header >> text=${asset}`)
    expect(assetPage).not.toBeNull()
})