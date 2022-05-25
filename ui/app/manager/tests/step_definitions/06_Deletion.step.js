const { Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");

/**
 * delete realm
 */

Then('Delete realm', async function () {
  await this.switchToRealmBySelector("master")
  await this.deleteRealm()
})

Then('We should not see the Realm selector', async function () {
  const { page } = this
  await page.reload()
  expect(await this.page?.locator('#realm-picker').isVisible()).toBeFalsy()
})

/**
 * delete role
 */
Then('Delete role', async function () {

  // delete roles
  await this.click('td:has-text("Custom") >> nth=0')

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
})

Then('We should not see the Custom role', async function () {
  const { page } = this
  await expect.null(page.waitForSelector('td:has-text("Custom") >> nth=0'))
})


/**
 * delete user
 * only admin user has the rights to delete user
 */
Then('Delete user', async function () {

  await this.click('td:has-text("smartcity")')
  await this.click('button:has-text("delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
})

Then('We should see an empty use page',async function(){
  const { page } = this
  await expect.null(page.waitForSelector('td:has-text("smartcity")'))
})

/**
 * delete assets
 */
Then('Delete assets', async function () {
  await this.deleteAssets()
})

Then('We should see an empty asset column',async function(){
  const { page } = this
  await expect.null(page.waitForSelector('text=Solar'))
  await expect.null(page.waitForSelector('text=Battery'))
})


