const { join } = require('path');
const { Given, When, Then } = require("@cucumber/cucumber");

/**
 * Setup
 */

Given('Setup {string}', { timeout: 32000 }, async function (section) {
    /**
     *  most of cases need the setup of: realm, user and assets
     *  for rules, "rule state" as the the configure item should selected
     */
    switch (section) {

        case "none":
            await this.navigate("admin")
            await this.login("admin")
            break;

        // contains realm
        case "lv1":
            await this.lv1_Setup("smartcity")
            break;

        // contains realm and user
        case "lv2":
            await this.lv2_Setup()
            break;

        // contians realm, user and empty assets
        case "lv3":
            await this.lv3_Setup()
            break;

        // contains realm, user and assets with data
        case "lv4":
            await this.lv4_Setup()
            break;

        default:
            break;

    }

})

/**
 *  Clean up
 */
When('Clean up {string}', { timeout: 10000 }, async function (section) {
    switch (section) {
        case "lv1":
            await this.lv1_Cleanup()
            break;
        case "lv2":
            await this.lv2_Cleanup()
            break;
        case "lv3":
            await this.lv3_Cleanup()
            break;
        case "lv4":
            await this.lv4_Cleanup()
            break;
        default:
            break;
    }
})





/**
 * General steps 
 */

/**
 * Login to a realm as expected user
 */
Given('Login OpenRemote as {string}', { timeout: 10000 }, async function (user) {
    await this.navigate(user)
    await this.login(user)
})

/**
 * Setting menu
 */
When('Navigate to {string} page', async function (name) {
    await this.navigateTo(name)
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
Then('Snapshot', async function () {
    const { page } = this;
    const image = await page?.screenshot();
    image && (await this.attach(image, 'image/png'));
});


Then('Snapshot {string}', async function (string) {
    const { page } = this;
    await page?.screenshot({ path: join('screenshots', `${string}.png`) });
});