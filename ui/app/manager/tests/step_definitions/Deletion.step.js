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
  
  // not correct
  await this.click('#mdc-data-table-icon-2 span')
  await this.click('#attribute-meta-row-2 button:has-text("Delete")')
  await this.click('div[role="alertdialog"] button:has-text("Delete")')
})