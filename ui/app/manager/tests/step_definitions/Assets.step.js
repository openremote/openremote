const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");



// add new asset
Then('Create a {string} with name of {string}', async function (asset, name) {
    const { page } = this;

    await this.click('.mdi-plus');
    await this.click(`text=${asset}`);
    await page.locator('#name-input input[type="text"]').fill(name);
    await this.click('#add-btn')
})

When('Go to asset {string} info page', async function (name) {
    const { page } = this;

    await page.locator(`#list-container div:has-text("${name}")`).nth(1).click();
})

Then('Go to modify mode', async function () {
    await this.click('button:has-text("Modify")')
})

Then('Give {string} to the {string} with type of {string}', async function (value, attribute, type) {
    const { page } = this;

    await page.locator(`text=${attribute} ${type} >> input[type="number"]`).fill(value);
})

Then('Save', async function () {

    // press enter to enable the save button (could be any other action) (or this will be fixed then no pre-action needed)
    // await this.press('Enter')
    await this.click('#edit-container')

    await this.click('button:has-text("Save")')
})


// select an asset
When('Search for the {string}', async function (name) {
    const { page } = this;
    await page.locator('input[type="text"]').fill(name);
})

When('Select the {string}', async function (name) {
    const { page } = this;
    await page.locator(`text=${name}`).click();
})

Then('We see the {string} page', async function (name) {
    const { page } = this;
    await expect(page.waitForSelector('div[id="view-container"]')).not.toBeNull()
})


// update
Then('Update {string} to the {string} with type of {string}', async function (value, attribute, type) {
    const { page } = this;
    await page.locator(`#field-${attribute} input[type="${type}"]`).fill(value)
    await this.click(`#field-${attribute} #send-btn span`)
})

Then('Update {int} and {int}', async function (location_x, location_y) {
    const { page } = this;

    await this.click('text=location GEO JSON point >> button span')
    //await page.locator('[aria-label="Map"]').click(1230,500,{delay:300,})

    // location_x and location_y are given by the example data
    // currently there is no way to drag the map and click on a random place (could be possible in the future)
    await page.mouse.click(location_x, location_y, { delay: 1000 })
    await this.click('button:has-text("OK")')
})

