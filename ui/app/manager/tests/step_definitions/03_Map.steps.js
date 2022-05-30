const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

When('Check {string} on map', async function (asset) {
    const { page } = this;

    await this.wait(400)
    await this.click('div:nth-child(3) .marker-container div or-icon svg path')

    await this.wait(400)
    const cardPanel = await page.locator(`text=${asset}`).isVisible()
    await expect(cardPanel).toBeTruthy()
})

Then('Click and nevigate', async function () {

    await this.click('button:has-text("View")')
})

Then('We are at {string} page', async function (asset) {
    const { page } = this
    const assetPage = await page.locator(`#asset-header >> text=${asset}`).isVisible()
    expect(assetPage).toBeTruthy()
})