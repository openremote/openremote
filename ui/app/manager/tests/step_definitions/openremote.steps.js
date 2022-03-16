const { Given, When, Then } = require("@cucumber/cucumber");
const { OpenRemote_DEMO_URL } = require("../support/config");
const expect = require("playwright-expect")

/**
 * command for running all the test： npm test
 * command for running certain test/tests with the "tag"(@OpenRemote) npm run tags "tag" (npm run tags "@OpenRemote")
 * command for more.....
 */

// this should be the pre-condition of every case
Given('Go to the OpenRemote demo website', async function () {
    await this.openUrl(OpenRemote_DEMO_URL)
});

// type in the username and password with both being "smartcity"
When('Type in username and password', async function () {
    const { page } = this;
    await page?.fill('#username', 'smartcity')
    await page?.fill('#password', 'smartcity')
})

// before a certain scenario
// Before({ tags: '@LoginMobile', timeout: 5000 * 2 }, async function () {
//     console.log("this is before login")
//     global.mobileBrowser = await webkit.launch({
//         // Not headless so we can watch test runs
//         headless: false,
//         // Slow so we can see things happening
//         slowMo: 200,
//     })
//     const iphone = devices["iPhone 12 Pro"]
//     const iphoneContext = await global.mobileBrowser.newContext({
//         ...iphone,
//         viewport: iphone.viewport,
//         userAgent: iphone.userAgent,
//         colorScheme: 'dark'
//     });

//     this.page = await iphoneContext.newPage()
// })

// After({ tags: '@LoginMobile' }, async function () {
//     await global.mobileBrowser.close()
// })

// click on login button and wait 5 seconds for map to load
When('Login', async function () {
    const { page } = this;
    /**
     * Click on the login button 
     * await page?.click('button[class="btn waves-effect waves-light"]')
     * await setTimeout(5000)
     */

    // press enter
    await page?.press('body', 'Enter');

    // save authentication
    await this.context.storageState({ path: 'state.json' });
})

// check if map and header exsit
Then('We see the map', { timeout: 6000 * 2 }, async function () {
    const { page } = this;
    const header = await page?.waitForSelector('body >> or-app >> or-header >> div[id="header"]')
    const map = await page?.waitForSelector('or-app >> main >> page-map >> or-map[id="map"]')
    expect(header).not.toBeNull()
    console.log('header exists');
    expect(map).not.toBeNull()
    console.log('map exists')
})







