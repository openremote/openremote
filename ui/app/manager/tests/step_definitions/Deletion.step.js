const { When, Then } = require("@cucumber/cucumber");


Then('delete asset named {string}', async function (name) {
    const {page} = this

    await page.locator(`div:has-text("${name}")`).nth(2).click();

    await page.locator('.mdi-delete').click();

    await Promise.all([
        page.locator('button:has-text("Delete")').click()
      ]);
})