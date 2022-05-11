const { join } = require('path');
const { Given, When, Then } = require("@cucumber/cucumber");

/**
 * Setup
 */

Given('Setup for {string}', { timeout: 30000 }, async function (section) {
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
        case "fundamental":
            await this.fundamentalSetup("smartcity")
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
When('Clean up {string}', { timeout: 10000 }, async function (section) {
    switch (section) {
        case "fundamental":
            await this.fundamentalClean()
            break;
        case "basic":
            await this.basicClean()
            break;
        case "convention":
            await this.conventionClean()
            break;
        case "thorough":
            await this.thoroughClean()
            break;
        default:
            break;
    }
})





/**
 * General steps 
 */

Given('Login OpenRemote as {string}', { timeout: 10000 }, async function (user) {
    await this.navigate(user)
    await this.login(user)
})

When('Navigate to {string} page', async function (name) {
    await this.navigateTo(name)
})

/**
 * Tab menu
 */
Then('Nevigate to map page', async function () {
    await this.click('#dessktop-left a:nth-child(1)')
});

Then('Nevigate to asset page', async function () {
    await this.click('#desktop-left a:nth-child(2)')
});


Then('Nevigate to rule page', async function () {
    await this.click('#desktop-left a:nth-child(3)')
});

Then('Nevigate to insight page', async function () {
    await this.click('#desktop-left a:nth-child(4)')
});


/**
 * Setting menu
 */
Then('Nevigate to role page', async function () {
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