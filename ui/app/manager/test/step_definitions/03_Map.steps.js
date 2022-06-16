const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

When('Check {string} on map', async function (asset) {
    let startTime =new Date() / 1000
    await this.wait(400)
    await this.click('div:nth-child(3) .marker-container div or-icon svg path')

    await this.wait(400)
    const cardPanel = await this.isVisible(`text=${asset}`)
    //const cardPanel = await page.locator(`text=${asset}`).isVisible()
    await expect(cardPanel).toBeTruthy()
    this.logTime(startTime)
})

Then('Click and nevigate', async function () {
    let startTime =new Date() / 1000

    await this.click('button:has-text("View")')
    this.logTime(startTime)
})

Then('We are at {string} page', { timeout: 10000 }, async function (asset) {
    let startTime =new Date() / 1000
    await this.wait(1000)
    const assetPage = await this.isVisible(`#asset-header >> text=${asset}`)
    //const assetPage = await page.locator(`#asset-header >> text=${asset}`).isVisible()
    expect(assetPage).toBeTruthy()
    this.logTime(startTime)
})