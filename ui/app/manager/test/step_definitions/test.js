const { When, Then } = require("@cucumber/cucumber");
const { expect } = require("@playwright/test");


When("go to google", async function () {
    const { page } = this
    await page.goto("https://google.nl")
})

Then("go to youtube", async function () {
    const { page } = this
    await page.goto("https://youtube.com")
})