const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**   Map attribute  **/
Given('Login OpenRemote demo website', { timeout: 10000 }, async function () {
    await this.navigate()
})


// not a good way to select a certain marker on the map
// now it's using nth decorater to select item
// skip if there is no such marker 
When('Click on the Parking Erasmusbrug', { timeout: 10000 }, async function () {
    await this.click('div:nth-child(30) .marker-container div or-icon svg path')
})

// skip if the last step does not finish
Then('We see a map panel with Parking Erasmusbrug', async function () {
    const { page } = this;
    const card = await page.waitForSelector('or-map-asset-card')
    const cardId = await card.getAttribute('assetid')
    await expect(cardId).toEqual('2tLAEBGjmRCu1KrdJn9T2Z')
})

When('Click on View button', async function () {
    await this.click('button:has-text("View")')
})

Then('We see the Parking Erasmusbrug page and History graph', async function () {
    const { page } = this;
    await expect(page.waitForSelector('div[id="history-panel"]')).not.toBeNull()
})

Then('Asset option is selected', async function () {
    const { page } = this;
    const element = await page.waitForSelector('#desktop-left a:nth-child(2)')
    const text = await element.getAttribute('selected')
    await expect(text).not.toBeNull()
})

/**   Click on Assets  **/

When('Navigate to asset page', async function () {
    await this.click('#desktop-left a:nth-child(2)')
})

Then('We see the collapsed asset tree with nothing selected', async function () {
    const { page } = this;
    let options = await page.$$('#tree #list-container ol[id="list"] >> li');
    let options_2 = []

    // not a smart way
    for (let i of options) {
        let text = await i.getAttribute('data-expanded')
        if (text != null) {
            options_2.push(i)
        }
    }
    await expect(options_2.length).toEqual(0)
})

Then('We see text on the main panel', { timeout: 6000 }, async function () {
    const { page } = this;
    const value = await (await page.waitForSelector('.msg >> or-translate')).getAttribute('value')

    await expect(value).toEqual('noAssetSelected')
})

When('Check markers on map',async function(){
    const { page } =this;
    await page.locator('or-icon svg path').first().click();
    let cardPanel = await page.waitForSelector('or-map-asset-card')
    await expect(cardPanel).not.toBeNull()
    
    await page.locator('div:nth-child(3) .marker-container div or-icon svg path').click()
    cardPanel = await page.waitForSelector('or-map-asset-card')
    await expect(cardPanel).not.toBeNull()
    
    //await page.locator('div:nth-child(3) .marker-container div or-icon svg path').click();
    // const mapBattery = await page.waitForSelector('or-map-asset-card')
    // await expect(mapBattery).not.toBeNull()
    // await page.locator('div:nth-child(3) .marker-container div or-icon svg path').click();
    // const mapSolar = await page.waitForSelector('or-map-asset-card')
    // await expect(mapSolar).not.toBeNull()
})

