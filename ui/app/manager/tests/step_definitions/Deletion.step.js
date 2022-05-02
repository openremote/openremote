const { When, Then } = require("@cucumber/cucumber");


Then('delete asset named {string}', async function (name) {
  const { page } = this

  await page.locator(`div:has-text("${name}")`).nth(2).click();

  await page.locator('.mdi-delete').click();

  await Promise.all([
    page.locator('button:has-text("Delete")').click()
  ]);
})

Then('Delete role', async function () {
  const { page } = this
  await page.locator('td:has-text("asset")').first().click()

  // click on the button that in the first row
  // not a smart way 
  // if it's not inside the first row then will have error
  await this.click('#attribute-meta-row-0 button:has-text("Delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
})