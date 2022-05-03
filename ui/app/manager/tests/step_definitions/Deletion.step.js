const { When, Then } = require("@cucumber/cucumber");

/**
 * delete assets
 */
Then('delete asset named {string}', async function (name) {
  const { page } = this

  await page.locator(`div:has-text("${name}")`).nth(2).click();

  await page.locator('.mdi-delete').click();

  await Promise.all([
    page.locator('button:has-text("Delete")').click()
  ]);
})

/**
 * delete role
 */
Then('Delete role', async function () {
  const { page } = this
  await page.locator('td:has-text("asset")').first().click()

  // click on the button that in the first row
  // not a smart way 
  // if it's not the first row then will have error
  await this.click('#attribute-meta-row-0 button:has-text("Delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
})


/**
 * delete user
 * only admin user has the rights to delete user
 */
Then('Delete user', async function () {
  const { page } = this;

  await this.click('td:has-text("smartcity")')
  await this.click('button:has-text("delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
})


/**
 * delete configure item
 */
Then('Delete {string} on {string}', async function (item, attribute) {
  const { page } = this;

  await this.click(`td:has-text("${attribute}")`)
  await page.locator('#input').first().hover()
  await page.locator('.mdi-close-circle').first().click();
})