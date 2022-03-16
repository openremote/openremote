const { Given, When, Then, AfterAll } = require("@cucumber/cucumber");
const { OpenRemote_DEMO_URL } = require("../support/config");
const config = require("../support/config")

Given("Bob opens Manabie website", { timeout: 60 * 1000 }, async function () {
    await this.openUrl(OpenRemote_DEMO_URL);
});