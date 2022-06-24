const { Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**
 * delete realm
 */

Then('Delete realm', { timeout: 60000 }, async function () {
  let startTime = new Date() / 1000
  await this.switchToRealmByRealmPicker("master")
  await this.deleteRealm("smartcity")
  this.logTime(startTime)
})

Then('We should not see the Realm picker', async function () {
  let startTime = new Date() / 1000

  await this.goToRealmStartPage("master")

  // must wait for the realm picker to be rendered
  await this.wait(500)
  const isVisible = await this.isVisible('#realm-picker')
  await expect(isVisible).toBeFalsy()
  this.logTime(startTime)
})

/**
 * delete role
 */
Then('Delete role', { timeout: 60000 }, async function () {
  let startTime = new Date() / 1000

  // reproduce the preparation steps to start from the beginning
  await this.navigateToTab("Map")
  await this.navigateToMenuItem("Roles")

  // delete roles
  await this.click('text=Custom')

  // bad solution costing a lot of time
  // can't find a way to locate the delete button 
  // since the sorting of the role is random everytime 
  // the html tag is in form of "#attribute-meta-row-2" in which number inside is decided by order
  // if the order is random then then number of html may change every time
  // then the delete button is not being able to been determined

  // instead i will use tab key to move to the delete button
  // it's not a decent solution but that's the only way i can come up with
  for (let i = 0; i < 15; i++) {
    await this.press('Tab')
  }
  await this.press('Enter')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
  await this.wait(100)
  this.logTime(startTime)
})

Then('We should not see the Custom role', async function () {
  let startTime = new Date() / 1000
  const count = await this.count('text=Custom')
  await expect(count).toEqual(0)
  this.logTime(startTime)
})


/**
 * delete user
 * only admin user has the rights to delete user
 */
Then('Delete user', async function () {
  let startTime = new Date() / 1000

  await this.click('td:has-text("smartcity")')
  await this.click('button:has-text("delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')

  await this.wait(500)
  this.logTime(startTime)
})

Then('We should not see the {string} user', async function (user) {
  let startTime = new Date() / 1000
  const count = await this.count(`td:has-text("${user}")`)
  await expect(count).toEqual(0)
  this.logTime(startTime)
})

/**
 * delete assets
 */
Then('Delete assets', { timeout: 40000 }, async function () {
  let startTime = new Date() / 1000
  await this.deleteSelectedAsset("Battery")
  await this.wait(500)
  await this.deleteSelectedAsset("Solar Panel")

  // must wait to confirm that assets have been deleted
  await this.wait(500)
  this.logTime(startTime)
})

Then('We should see an empty asset column', async function () {

  let startTime = new Date() / 1000

  const count_console = await this.count('text=Console')
  const count_solar = await this.count('text=Solar Panel')
  const count_battery = await this.count('text=Battery')

  await expect(count_console).toEqual(1)
  await expect(count_solar).toEqual(0)
  await expect(count_battery).toEqual(0)

  this.logTime(startTime)
})


