const { Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**
 * delete realm
 */

Then('Delete realm', async function () {
  await this.switchToRealmByRealmPicker("master")
  await this.deleteRealm()
})

Then('We should not see the Realm picker', async function () {
  await this.goToPage("admin")

   // must wait for the realm picker to be rendered
  await this.wait(500)
  const isVisible = await this.page?.locator('#realm-picker').isVisible()
  await expect(isVisible).toBeFalsy()
  await console.log("finished")
})

/**
 * delete role
 */
Then('Delete role', async function () {

  // reproduce the preparation steps to start from the beginnning
  await this.navigateToTab("Map")
  await this.navigateTo("Roles")

  // delete roles
  await this.click('text=Custom')

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
  await this.wait(200)
})

Then('We should not see the Custom role', async function () {
  const { page } = this
  const count = await page.locator('text=Custom').count()
  await expect(count).toEqual(0)
})


/**
 * delete user
 * only admin user has the rights to delete user
 */
Then('Delete user', async function () {

  await this.click('td:has-text("smartcity")')
  await this.click('button:has-text("delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')

  await this.wait(300)
})

Then('We should see an empty use page', async function () {
  const { page } = this
  const count = await page.locator('td:has-text("smartcity")').count()
  await expect(count).toEqual(0)
})

/**
 * delete assets
 */
Then('Delete assets', async function () {
  await this.deleteAssets()

  // must wait to confirm that assets have been deleted
  await this.wait(300)
})

Then('We should see an empty asset column', async function () {
  const { page } = this

  const count_console = await page.locator('text=Console').count()
  const count_solar = await page.locator('text=Solar').count()
  const count_battery = await page.locator('text=Battery').count()
  await expect(count_console).toEqual(1)
  await expect(count_solar).toEqual(0)
  await expect(count_battery).toEqual(0)
})


