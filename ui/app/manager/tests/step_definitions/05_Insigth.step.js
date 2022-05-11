const { When, Then } = require("@cucumber/cucumber");

When('Select {string} from {string}', async function (attribute, asset) {
    const { page } = this

    await this.click('#chart-panel button:has-text("Select attributes")')
    await this.click(`text=${asset}`)

    await this.click(`li[role="checkbox"]:has-text("${attribute}")`)
    await this.click('button:has-text("Add")')

    await page.reload()

    await page.waitForTimeout(1000);
})

// Then('We should see the graph',async function(){
//     const {page} = this

//     var height = await page.waitForSelector('#chart-panel > .panel-content > or-chart[panelname="chart"] > canvas').getAttribute("height")
//     console.log(height)
// })