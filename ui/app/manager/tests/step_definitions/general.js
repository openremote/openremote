const { Then } = require('@cucumber/cucumber');
const { join } = require('path');

/**
 * General steps 
 */





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