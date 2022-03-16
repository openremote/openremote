const { Given, When, Then } = require("@cucumber/cucumber");
const { OpenRemote_DEMO_URL } = require("../support/config");
const fs = require("fs")
const { expect } = require("@playwright/test");

// Map attribute
Given('Login OpenRemote demo website', { timeout: 10000 }, async function () {
    await this.navigateTo(OpenRemote_DEMO_URL)
    await this.login()
})

When('Click on the Parking Erasmusbrug', { timeout: 10000 }, async function () {
    const { page } = this;
    await page.locator('div:nth-child(30) .marker-container div or-icon svg path').click();

})

Then('We see a map panel with Parking Erasmusbrug', async function () {
    const { page } = this;
    await expect(page).toHaveURL('https://demo.openremote.io/manager/?realm=smartcity#/map/2tLAEBGjmRCu1KrdJn9T2Z');
})

When('Click on View button', async function () {
    await this.click('button:has-text("View")')
})

Then('We see the Parking Erasmusbrug page and History graph', async function () {
    const { page } = this;
    await expect(page).toHaveURL('https://demo.openremote.io/manager/?realm=smartcity#/assets/false/2tLAEBGjmRCu1KrdJn9T2Z');
    await expect(page.waitForSelector('div[id="history-panel"]')).not.toBeNull()
})

Then('Asset option is selected', async function () {
    const { page } = this;
    const element = await page.waitForSelector('#desktop-left a:nth-child(2)')
    const text = await element.getAttribute('selected')
    await expect(text).not.toBeNull()
})

// Click on Assets
When('Click on the Asset option', async function () {
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
            // its not null!!
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