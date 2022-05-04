const { join } = require('path');
const { Given, When, Then } = require("@cucumber/cucumber");

/**
 * General steps 
 */
Given('Login to smartcity realm', async function () {
    await this.navigate('smart')
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
Given('Nevigate to role page',async function(){
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