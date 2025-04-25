const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**
 * add new asset
 */
Then('Create a {string} with name of {string}', { timeout: 30000 }, async function (asset, name) {
    let startTime = new Date() / 1000
    // select conosle first to get into the modify mode
    await this.click(`#list-container >> text="Consoles"`)
    await this.switchMode("modify")
    await this.unselect()

    // start adding assets
    await this.click('.mdi-plus')
    await this.click(`text=${asset}`)
    await this.fill('#name-input input[type="text"]', name)
    await this.click('#add-btn')
    await this.wait(400)

    // check if at modify mode
    // if yes we should see the save button then save
    const isSaveBtnVisible = await this.isVisible('button:has-text("Save")')
    console.log("save btn is " + isSaveBtnVisible)
    if (isSaveBtnVisible) {
        console.log("ready to save")
        await this.click('button:has-text("Save")')
    }

    await this.unselect()
    this.logTime(startTime)
})

When('Go to asset {string} info page', { timeout: 30000 }, async function (name) {
    let startTime = new Date() / 1000
    await this.click(`#list-container >> text=${name}`)
    this.logTime(startTime)
})

Then('Go to modify mode', { timeout: 30000 }, async function () {
    let startTime = new Date() / 1000
    await this.wait(1000)
    await this.switchMode("modify")
    this.logTime(startTime)
})

Then('Give {string} to the {string} with type of {string}', async function (value, attribute, type) {
    let startTime = new Date() / 1000
    await this.fill(`text=${attribute} ${type} >> input[type="number"]`, value)
    this.logTime(startTime)
})

Then('Save', async function () {
    const { page } = this
    let startTime = new Date() / 1000
    await this.save()
    this.logTime(startTime)
})

Then('We see the asset with name of {string}', async function (name) {
    let startTime = new Date() / 1000
    await this.wait(500)
    const count = await this.count(`text=${name}`)

    // reason why it's 1 is because that this scnario runs in a outline
    // each time only one set of data will be used in one run of outlines
    // thus, only one asset will be added and removed in one run and next time will start with the empty envrioment
    await expect(count).toBe(1)
    this.logTime(startTime)
})

/**
 * select an asset
 */
When('Search for the {string}', async function (name) {
    let startTime = new Date() / 1000

    await this.fill('#filterInput input[type="text"]', name)
    this.logTime(startTime)
})

When('Select the {string}', async function (name) {
    let startTime = new Date() / 1000
    await this.click(`text=${name}`)
    this.logTime(startTime)
})

Then('We see the {string} page', async function (name) {
    let startTime = new Date() / 1000
    const { page } = this;
    await expect(await page.waitForSelector(`#asset-header >> text=${name}`)).not.toBeNull()
    this.logTime(startTime)
})

/**
 * update
 */
Then('Update {string} to the {string} with type of {string}', async function (value, attribute, type) {
    let startTime = new Date() / 1000
    await this.switchMode("view")
    await this.fill(`#field-${attribute} input[type="${type}"]`, value)
    await this.click(`#field-${attribute} #send-btn span`)
    this.logTime(startTime)
})

Then('Update location of {int} and {int}', { timeout: 30000 }, async function (location_x, location_y) {
    let startTime = new Date() / 1000
    const { page } = this;
    await this.switchMode("modify")
    await this.click('text=location GEO JSON point >> button span')
    await this.wait(2000)
    // location_x and location_y are given by the example data
    // currently it's not implmented as dragging the map and clicking on a random place (could be possible in the future)
    await page.mouse.click(location_x, location_y, { delay: 1000 })
    await this.click('button:has-text("OK")')
    this.logTime(startTime)
})

/**
 * read only
 */
Then('Uncheck on readonly of {string}', async function (attribute) {
    let startTime = new Date() / 1000
    await this.click(`td:has-text("${attribute}") >> nth=0`)

    // bad solution 
    // nth number is decided by the default state
    // if default stete changes, please change the nth number
    if (attribute == "energyLevel")
        await this.click('text=Read only >> nth=2')
    else
        await this.click('text=Read only >> nth=1')

    this.logTime(startTime)
})

Then('Check on readonly of {string}', async function (attribute) {
    let startTime = new Date() / 1000
    await this.click(`td:has-text("${attribute}")`)

    // bad solution
    // in this case, i assume that the config items are as the beginning state, namely default state
    // if the default state changes, the following nth-chlid should change as well
    if (attribute == "efficiencyExport")
        await this.click('.item-add or-mwc-input #component >> nth=0')
    else
        await this.click('tr:nth-child(14) td .meta-item-container div .item-add or-mwc-input #component')
    await this.click('li[role="checkbox"]:has-text("Read only")')
    await this.click('div[role="alertdialog"] button:has-text("Add")')
    this.logTime(startTime)
})

When('Go to panel page', async function () {
    let startTime = new Date() / 1000
    await this.click('button:has-text("View")')
    await this.wait(1500)
    this.logTime(startTime)
})

Then('We should see a button on the right of {string}', async function (attribute) {
    let startTime = new Date() / 1000
    const { page } = this;

    await expect(await page.waitForSelector(`#field-${attribute} button`)).not.toBeNull()
    this.logTime(startTime)
})

Then('No button on the right of {string}', async function (attribute) {
    let startTime = new Date() / 1000
    const count = await this.count(`#field-${attribute} button`)
    expect(count).toBe(0)
    this.logTime(startTime)
})

/**
 * set configure item
 */
Then('Select {string} and {string} on {string}', async function (item1, item2, attribute) {
    let startTime = new Date() / 1000
    await this.configItem(item1, item2, attribute)
    this.logTime(startTime)
})
