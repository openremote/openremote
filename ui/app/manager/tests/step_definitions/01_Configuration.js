const { Given, When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");
require('dotenv').config();

/**
 * add new user
 */
Then('Switch to {string} realm', async function (realm) {
    await this.switchToRealmBySelector(realm)
})

Then("Add a new user", async function () {

    const { page } = this

    // type in username
    await page.locator('.mdi-plus').first().click();
    await page.locator('input[type="text"]').first().fill('smartcity');

    // type in password
    await this.fill('#password-user0 input[type="password"]', 'smartcity')
    await this.fill('#repeatPassword-user0 input[type="password"]', 'smartcity')

    // select permissions
    await this.click('div[role="button"]:has-text("Roles")');
    await this.click('li[role="menuitem"]:has-text("Read")');
    await this.click('li[role="menuitem"]:has-text("Write")');
    await this.click('div[role="button"]:has-text("Roles")')
    await this.wait(400)

    //create
    await this.click('button:has-text("create")')
})

/**
 * add role
 */

Then('Create a new role', async function () {
    const { page } = this;

    await this.click('text=Add Role')

    // get total number of current roles
    var rows = await page.$$('.mdc-data-table__row')
    const count = await rows.length

    await page.locator(`#attribute-meta-row-${count - 1} input[type="text"]`).first().fill('Custom');
    await page.locator(`#attribute-meta-row-${count - 1} input[type="text"]`).nth(1).fill('read:asset, write:asset');
    await page.locator(`#attribute-meta-row-${count - 1} td .meta-item-container div:nth-child(2) div or-mwc-input:nth-child(3) #field #component #elem`).first().check();
    await page.locator(`#attribute-meta-row-${count - 1} td .meta-item-container div:nth-child(2) div:nth-child(2) or-mwc-input:nth-child(3) #field #component #elem`).check();
    await this.click('button:has-text("create")')
})

/**
 * apply new role
 */
Then('Select the new role and unselect others', async function () {
    const { page } = this;

    await this.click('td:has-text("smartcity")')
    await this.click('div[role="button"]:has-text("Roles")');
    await this.click('li[role="menuitem"]:has-text("Read")');
    await this.click('li[role="menuitem"]:has-text("Write")');
    await this.click('li[role="menuitem"]:has-text("Custom")')
    await page.keyboard.press("Enter")
})

Then('We should see assets permission are selected', async function () {

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
})

Then('Switch back to origin', async function () {

    const { page } = this;
    await this.click('text=Roles Custom')
    await this.click('li[role="menuitem"]:has-text("Read")');
    await this.click('li[role="menuitem"]:has-text("Write")');
    await this.click('li[role="menuitem"]:has-text("Custom")')
    await page.keyboard.press("Enter")
})

/**
 * switch user
 */
When('Logout', async function () {
    await this.logout();
})


// TODO: add steps to check if there is any element (like asset, realm, user) having the same name.
// If yes, then skip. Low priority