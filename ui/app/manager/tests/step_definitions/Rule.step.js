const { When, Then } = require("@cucumber/cucumber");
const { test, expect } = require('@playwright/test');

/**
 * General
 */
When('Create a new {string} rule', async function (type) {
    await this.click('.mdi-plus')
    await this.click(`text=${type}`)
})


/**
 *  When-Then rules
 */
Then('Name new rule {string}', async function (name) {
    await this.fill('text=Rule name', name)
})

Then('Create When condition on {string} of {string} of {string} with threshold {string}', async function (attribute_when, asset, attribute_type, value) {

    // create a new condtion
    await this.click('or-rule-when #component span')
    await this.click(`or-rule-when >> text=${attribute_type}`)

    // select asset 
    await this.click('text=Asset Any of this type')  // It's the default text when nothing selected
    await this.click(`#idSelect >> text=${asset}`)

    // select attritube
    await this.click('div[role="button"]:has-text("Attribute")')
    await this.click(`li[role="option"]:has-text("${attribute_when}")`)

    // select condition
    await this.click('div[role="button"]:has-text("Operator")')
    await this.click('text=Less than or equal to')

    // set value
    await this.fill('input[type="number"]', value)
})

Then('Create Then action on {string} of {string} of {string} with threshold {string}', async function (attribute_then, asset, attribute_type, value) {

    // create a new action
    await this.click('or-rule-then-otherwise div or-panel .add-button-wrapper span span .plus-button #component or-icon .mdi-plus')
    await this.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${attribute_type}")`)

    // select asset
    await this.click('text=Asset Matched')
    await this.click(`#matchSelect li[role="option"]:has-text("${asset}")`)

    // select attribute
    await this.click('text=Attribute Efficiency import Energy capacity Energy level Energy level percentage >> div[role="button"]')
    await this.click(`li[role="option"]:has-text("${attribute_then}")`)

    // set value
    await this.click('label:has-text("Value")')
    await this.fill('text=Always Always Once Once per hour Once per day Once per week Building asset City  >> input[type="number"]', value)

})

Then('Save rule', async function () {
    await this.click('or-mwc-input:has-text("Save")')
})

/**
 *  Flow editor
 */
Then('Drag in the elements', async function () {
    const { page } = this

    // page.dragAndDrop(source, target[, options]) is an alternative 
    await page.locator('text=Read attribute').hover()
    await page.mouse.down()
    await page.waitForTimeout(2000)
})

