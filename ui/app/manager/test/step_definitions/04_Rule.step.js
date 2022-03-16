const { When, Then } = require("@cucumber/cucumber");
const { expect } = require('@playwright/test');

/**
 * General
 */
When('Create a new {string} rule', async function (type) {
    await this.click('.mdi-plus >> nth=0')
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
    await this.click('button:has-text("Add action")')
    await this.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${attribute_type}")`)

    // select asset
    await this.click('text=Asset Matched')
    await this.click(`#matchSelect li[role="option"]:has-text("${asset}")`)

    // select attribute
    await this.click('text=Attribute Efficiency export Efficiency import Energy capacity Energy export tota >> div[role="button"]')
    await this.click(`li[role="option"]:has-text("${attribute_then}")`)

    // set value
    await this.click('label:has-text("Value")')
    await this.fill('text=Always Always Once Once per hour Once per day Once per week Building asset City  >> input[type="number"]', value)

})

Then('Save rule', async function () {
    await this.click('or-mwc-input:has-text("Save")')
    await this.wait(1000)
})


Then('We see the rule with name of {string}', async function (name) {
    const count = await this.count(`text=${name}`)
    await expect(count).toBe(1)
})
/**
 *  Flow editors
 */
Then('Drag in the elements', async function () {

    // page.dragAndDrop(source, target[, options]) is an alternative 
    // move all the elements
    await this.hover('text=Read attribute')
    await this.drag(450, 250)

    await this.hover('text=Number >> nth=0')
    await this.drag(450, 350)

    await this.hover('text=Number >> nth=0')
    await this.drag(450, 500)

    await this.hover('text=Number >> nth=0')
    await this.drag(450, 600)

    await this.hover('text=>')
    await this.drag(650, 300)

    await this.hover('text=Number switch')
    await this.drag(800, 425)

    await this.hover('text=Write attribute')
    await this.drag(1000, 425)
})

Then('Set value', async function () {
    // set read and write
    await this.click('button:has-text("Attribute") >> nth=0')   // read 
    await this.click('div[role="alertdialog"] >> text=Solar Panel')
    await this.click('or-translate:has-text("Power") >> nth=0')
    await this.click('button:has-text("Add")')

    await this.click('button:has-text("Attribute") >> nth=1')   // write
    await this.click('div[role="alertdialog"] >> text=Solar Panel')
    await this.click('or-translate:has-text("Power forecast")')
    await this.click('button:has-text("Add")')

    await this.fill('[placeholder="value"] >> nth=0', '50')
    await this.fill('[placeholder="value"] >> nth=1', '60')
    await this.fill('[placeholder="value"] >> nth=2', '40')
})

Then('Connect elements', async function () {
    // connect elements
    await this.dragAndDrop('.socket >> nth=0', '.socket-side.inputs flow-node-socket .socket >> nth=0')
    await this.dragAndDrop('flow-node:nth-child(2) .socket-side flow-node-socket .socket', 'flow-node-socket:nth-child(2) .socket')
    await this.dragAndDrop('div:nth-child(3) flow-node-socket .socket', ' flow-node:nth-child(6) .socket-side.inputs flow-node-socket .socket >> nth=0')
    await this.dragAndDrop('flow-node:nth-child(3) .socket-side flow-node-socket .socket', 'flow-node:nth-child(6) .socket-side.inputs flow-node-socket:nth-child(2)')
    await this.dragAndDrop('flow-node:nth-child(4) .socket-side flow-node-socket .socket', 'flow-node-socket:nth-child(3) .socket')
    await this.dragAndDrop('flow-node:nth-child(6) .socket-side.outputs flow-node-socket .socket', 'flow-node:nth-child(7) .socket-side flow-node-socket .socket')
    await this.wait(500)
})

Then('We see the flow rule with name of {string}', async function (name) {
    // name with FLOW can ensuring that it's a flow rule
    const count = await this.count(`text=${name}FLOW`)
    await expect(count).toBe(1)
})

Then('We should see {int} rules in total', async function (int) {
    const count = await this.count('.node-container')
    await expect(count).toBe(int)
})


