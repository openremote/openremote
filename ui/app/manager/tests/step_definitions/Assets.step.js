const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");
const { expectExtension } = require("../support/expect_extension")

/**
 * add new asset
 */
Then('Create a {string} with name of {string}', async function (asset, name) {
    const { page } = this;

    await this.click('.mdi-plus')
    await this.click(`text=${asset}`)
    await page.locator('#name-input input[type="text"]').fill(name);
    await this.click('#add-btn')
})

When('Go to asset {string} info page', async function (name) {
    const { page } = this;

    await page.locator(`#list-container div:has-text("${name}")`).nth(1).click()
})

Then('Go to modify mode', async function () {
    await this.click('button:has-text("Modify")')
})

Then('Give {string} to the {string} with type of {string}', async function (value, attribute, type) {

    await this.fill(`text=${attribute} ${type} >> input[type="number"]`, value)
    //await page.locator(`text=${attribute} ${type} >> input[type="number"]`).fill(value);
})

Then('Save', async function () {

    // press enter to enable the save button (could be any other action) (or this will be fixed then no pre-action needed)
    // await this.press('Enter')
    await this.click('#edit-container')

    await this.click('button:has-text("Save")')
})

/**
 * select an asset
 */
When('Search for the {string}', async function (name) {

    await this.fill('input[type="text"]', name)
    //await page.locator('input[type="text"]').fill(name);
})

When('Select the {string}', async function (name) {
    const { page } = this;
    await page.locator(`text=${name}`).click()
})

Then('We see the {string} page', async function (name) {
    const { page } = this;
    await expect(page.waitForSelector('div[id="view-container"]')).not.toBeNull()
})

/**
 * update
 */
Then('Update {string} to the {string} with type of {string}', async function (value, attribute, type) {

    await this.fill(`#field-${attribute} input[type="${type}"]`, value)
    await this.click(`#field-${attribute} #send-btn span`)
})

Then('Update {int} and {int}', async function (location_x, location_y) {
    const { page } = this;

    await this.click('text=location GEO JSON point >> button span')
    //await page.locator('[aria-label="Map"]').click(1230,500,{delay:300,})

    // location_x and location_y are given by the example data
    // currently it's not implmented as dragging the map and clicking on a random place (could be possible in the future)
    await page.mouse.click(location_x, location_y, { delay: 1000 })
    await this.click('button:has-text("OK")')
})

/**
 * read only
 */
Then('Uncheck on readonly of {string}', async function (attribute) {
    const { page } = this;

    await page.locator(`td:has-text("${attribute}")`).first().click()
    // very bad solution
    if (attribute == "energyLevel")
        await page.locator('text=Read only').nth(2).click()
    else
        await page.locator('text=Read only').nth(1).click()
})

Then('Check on readonly of {string}', async function (attribute) {
    const { page } = this;

    await this.click(`td:has-text("${attribute}")`)

    // bad solution
    if (attribute == "efficiencyExport")
        await page.locator('.item-add or-mwc-input #component').first().click()
    else
        await this.click('tr:nth-child(14) td .meta-item-container div .item-add or-mwc-input #component')
    await this.click('li[role="checkbox"]:has-text("Read only")')
    await this.click('div[role="alertdialog"] button:has-text("Add")')

})

When('Go to panel page', async function () {
    await this.click('button:has-text("View")')
})

Then('We should see a button on the right of {string}', async function (attribute) {
    const { page } = this;

    await expect(page.waitForSelector(`#field-${attribute} button`)).not.toBeNull()
})

Then('No button on the right of {string}', async function (attribute) {
    const { page } = this;

    await expect.null(page.waitForSelector(`#field-${attribute} button`))
    //await expect(page.waitForSelector(`#field-${attribute} button`)).toBeNull()
})

/**
 * set configure item
 */
Then('Select {string} and {string} on {string}', async function (item1, item2, attribute) {
    const { page } = this;

    await page.locator(`td:has-text("${attribute}")`).first().click()
    await this.click('.attribute-meta-row.expanded td .meta-item-container div .item-add or-mwc-input #component')
    await page.locator('li:nth-child(17) .mdc-checkbox .mdc-checkbox__native-control').check() // No.17 is the rule state
    await page.locator('li:nth-child(20) .mdc-checkbox .mdc-checkbox__native-control').check() // No.20 is the store data points
    await this.click('div[role="alertdialog"] button:has-text("Add")')
})


// TODO: improve the code on detecting element