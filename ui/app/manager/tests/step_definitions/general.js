const { join } = require('path');
const { Given, When, Then } = require("@cucumber/cucumber");

/**
 * Setup
 */

Given('Setup for {string}', { timeout: 50000 }, async function (section) {
    /**
     *  most of cases need the setup of: realm, user and assets
     *  for rules, "rule state" as the the configure item should selected
     */
    switch (section) {

        // contains realm
        case "fundamental":
            await this.fundamentalSetup()
            break;

        // contains realm and user
        case "basic":
            await this.basicSetup()
            break;

        // contians realm, user and empty assets
        case "convention":
            await this.conventionSetup()
            break;

        // contains realm, user and assets with data
        case "thorough":
            await this.thoroughSetup()
            break;
        default:
            break;

    }

})

/**
 *  Clean up
 */
When('Clean up {string}', async function (section) {
    switch (section) {
        case "general":
            break;
        default:
            break;
    }
})





/**
 * General steps 
 */

Given('Login OpenRemote as {string}', { timeout: 10000 }, async function (user) {
    await this.navigate(user, user)
})

/**
 * Tab menu
 */
Given('Nevigate to map page', async function () {
    await this.click('#desktop-left a:nth-child(1)')
});

Given('Nevigate to asset page', async function () {
    await this.click('#desktop-left a:nth-child(2)')
});


Given('Nevigate to rule page', async function () {
    await this.click('#desktop-left a:nth-child(3)')
});

Given('Nevigate to insight page', async function () {
    await this.click('#desktop-left a:nth-child(4)')
});


/**
 * Setting menu
 */
Given('Nevigate to role page', async function () {
    await this.click('#menu-btn-desktop');
    await this.click('text=Roles');
})




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