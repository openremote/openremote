const { When, Then } = require("@cucumber/cucumber");

When('Create a new when-then rule', async function () {
    await this.click('.mdi-plus')
    await this.click(`text=When-Then`)
})

Then('Name new rule {string}', async function (name) {
    await this.fill('text=Rule name',name)
})

