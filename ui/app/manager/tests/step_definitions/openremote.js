const { Given, When, Then } = require("@cucumber/cucumber");
const expect = require("playwright-expect")



/**       navigation   */
Given('Go to the OpenRemote demo website', async function () {
    await this.navigate()
});

/**       login        */
When('Login with username and password', async function () {
    await this.login()
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


/**       check if map and header exsit    */
Then('We see the map', { timeout: 6000 * 2 }, async function () {
    const { page } = this;
    const header = await page?.waitForSelector('body >> or-app >> or-header >> div[id="header"]')
    const map = await page?.waitForSelector('or-app >> main >> page-map >> or-map[id="map"]')
    expect(header).not.toBeNull()
    console.log('header exists');
    expect(map).not.toBeNull()
    console.log('map exists')
})







