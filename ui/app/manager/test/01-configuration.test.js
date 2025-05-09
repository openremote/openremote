const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**
 * add new user
 */
Then('Switch to {string} realm', async function (realm) {
    let startTime = new Date() / 1000
    await this.switchToRealmByRealmPicker(realm)
    this.logTime(startTime)
})

Then("Add a new user", async function () {
    let startTime = new Date() / 1000
    // type in name
    await this.click('.mdi-plus >> nth=0')
    await this.fill('input[type="text"] >> nth=0', "smartcity")
    // type in password
    await this.fill('#password-user0 input[type="password"]', "smartcity")
    await this.fill('#repeatPassword-user0 input[type="password"]', "smartcity")
    // select permissions
    await this.click('div[role="button"]:has-text("Realm Roles")')
    await this.click('li[role="menuitem"]:has-text("Default-roles-smartcity")')
    await this.click('div[role="button"]:has-text("Manager Roles")');
    await this.click('li[role="menuitem"]:has-text("Read")');
    await this.click('li[role="menuitem"]:has-text("Write")');
    await this.wait(500)
    await this.click('div[role="button"]:has-text("Manager Roles")');
    // create user
    await this.click('button:has-text("create")')
    await this.wait(500)
    this.logTime(startTime)
})

Then('We see a new user', async function () {
    let startTime = new Date() / 1000
    await this.wait(500)
    const count = await this.count('td:has-text("smartcity")')
    await expect(count).toBe(1)
    this.logTime(startTime)
})

/**
 * add role
 */

Then('Create a new role', async function () {
    const { page } = this;
    let startTime = new Date() / 1000
    await this.click('text=Add Role')

    // get total number of current roles
    let rows = await page.$$('.mdc-data-table__row')
    const count = await rows.length

    await this.fill(`#attribute-meta-row-${count - 1} input[type="text"] >> nth=0`, 'Custom')
    await this.fill(`#attribute-meta-row-${count - 1} input[type="text"] >> nth=1`, 'read:asset, write:asset')
    await this.check(`#attribute-meta-row-${count - 1} td .meta-item-container div:nth-child(2) div or-mwc-input:nth-child(3) #field #component #elem >> nth=0`)
    await this.check(`#attribute-meta-row-${count - 1} td .meta-item-container div:nth-child(2) div:nth-child(2) or-mwc-input:nth-child(3) #field #component #elem`)

    await this.click('button:has-text("create")')
    await this.wait(1500)
    this.logTime(startTime)
})


Then('We see a new role', async function () {
    let startTime = new Date() / 1000
    await this.wait(500)
    const count = await this.count('text=Custom')

    await expect(count).toBe(1)
    this.logTime(startTime)
})

/**
 * apply new role
 */
Then('Select the new role and unselect others', async function () {
    let startTime = new Date() / 1000
    await this.click('td:has-text("smartcity")')
    await this.click('div[role="button"]:has-text("Manager Roles")');
    await this.click('li[role="menuitem"]:has-text("Read")')
    await this.click('li[role="menuitem"]:has-text("Write")')
    await this.click('li[role="menuitem"]:has-text("Custom")')
    await this.wait(500)
    await this.press("Enter")
    this.logTime(startTime)
})

Then('We see that assets permission are selected', async function () {
    let startTime = new Date() / 1000

    //we expect to see two checkbox selected and disabled

    const { page } = this;
    var checkboxes = await page.$$('.mdc-checkbox__native-control')

    // third one is read asset 
    const readAsset_checked = await checkboxes[2].isChecked()
    const readAsset_disabled = await checkboxes[2].isDisabled()
    await expect(readAsset_checked).toBeTruthy()
    await expect(readAsset_disabled).toBeTruthy()

    // ninth one is write asset
    const writeAsset_checked = await checkboxes[8].isChecked()
    const writeAsset_disabled = await checkboxes[8].isDisabled()
    await expect(writeAsset_checked).toBeTruthy()
    await expect(writeAsset_disabled).toBeTruthy()
    this.logTime(startTime)
})

Then('Switch back to origin', async function () {
    let startTime = new Date() / 1000
    await this.click('text=Manager Roles Custom')
    await this.click('li[role="menuitem"]:has-text("Read")')
    await this.click('li[role="menuitem"]:has-text("Write")')
    await this.click('li[role="menuitem"]:has-text("Custom")')
    await this.wait(200)
    await this.press("Enter")
    this.logTime(startTime)
})

/**
 * switch user
 */
When('Logout', async function () {
    let startTime = new Date() / 1000
    await this.logout()
    this.logTime(startTime)
})

