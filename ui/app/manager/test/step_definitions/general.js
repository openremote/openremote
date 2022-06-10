const { join } = require('path');
const { Given, When, Then } = require("@cucumber/cucumber");

/**
 * Setup
 */

Given('Setup {string}', { timeout: 50000 }, async function (section) {

    await this.setup("smartcity", section)
})


/**
 * General steps 
 */

/**
 * Login to a realm as expected user
 */
When('Login to OpenRemote {string} realm as {string}', { timeout: 10000 }, async function (realm, user) {
    await this.openApp(realm)
    await this.login(user)
})

/**
 * Setting menu
 */
When('Navigate to {string} page', async function (name) {
    await this.navigateToMenuItem(name)
    await this.wait(200)
})

/**
 * Tab menu
 */
When('Navigate to {string} tab', async function (tab) {
    await this.navigateToTab(tab)
    await this.wait(200)
});


/**
 * snapshots
 */
Then('Snapshot {string}', async function (string) {
    const { page } = this;
    await page?.screenshot({ path: join('screenshots', `${string}.png`) });
});
