const { When, Then } = require("@cucumber/cucumber");

When('Create a new when-then rule', async function () {
    await this.click('.mdi-plus')
    await this.click(`text=When-Then`)
})

Then('Name new rule {string}', async function (name) {
    await this.fill('text=Rule name', name)
})

Then('Create When condition on {string} of {string} of {string}', async function (attribute, asset, attribute_type) {

    // create a new condtion
    await this.click('or-rule-when #component span')
    await this.click(`or-rule-when >> text=${attribute_type}`)

    // select asset 
    await this.click('text=Asset Any of this type')  // It's the default text when nothing selected
    await this.click(`#idSelect >> text=${asset}`)
    
    // select attritube
    await this.click('div[role="button"]:has-text("Attribute")')
    await this.click(`li[role="option"]:has-text("${attribute}")`)

    //select condition
    await this.click('div[role="button"]:has-text("Operator")')
    await this.click('text=Less than or equal to')


})

Then('Create Then action on {string} of {string}', async function (attribute, asset) {

    // create a new action
    await this.click('or-rule-then-otherwise div or-panel .add-button-wrapper span span .plus-button #component or-icon .mdi-plus')
    await this.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${attribute_type}")`)

    // select asset
    await this.click('text=Asset Matched')
    await this.click(`li[role="option"]:has-text("${asset}")`)
})