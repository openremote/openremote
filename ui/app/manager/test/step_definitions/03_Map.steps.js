const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

When('Check {string} on map', async function (asset) {
    let startTime = new Date() / 1000
    await this.wait(1000)
    const iconCount = await this.count('.marker-icon')
    await expect(iconCount).toEqual(2)
    await this.click('div:nth-child(3) .marker-container div or-icon svg path')
    await this.wait(400)

    const cardPanel = await this.isVisible(`text=${asset}`)
    await expect(cardPanel).toBeTruthy()
    
    this.logTime(startTime)
})

Then('Click and nevigate', async function () {
    let startTime = new Date() / 1000
    await this.wait(500)
    await this.click('button:has-text("View")')
    this.logTime(startTime)
})

Then('We are at {string} page', async function (asset) {
    let startTime = new Date() / 1000
    await this.wait(1500)
    const assetPage = await this.isVisible(`#asset-header >> text=${asset}`)
    await expect(assetPage).toBeTruthy()
    this.logTime(startTime)
})