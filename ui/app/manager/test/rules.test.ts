import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsForRules as assets } from "./fixtures/data/assets.js";

test.beforeEach(async ({ manager }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { assets });
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await manager.goToRealmStartPage("smartcity");
});

const energyRule = {
  name: "Energy rule",
  attribute_type: "Electricity battery asset",
  asset: "Battery",
  attribute_when: "Energy level",
  attribute_then: "Energy capacity",
  value: 50,
};

test.use({ storageState: userStatePath });

test("Create a When-Then rule", async ({ page, manager, rulesPage }) => {
  // Then Navigate to "Rule" tab
  await manager.navigateToTab("Rules");
  // When Create a new "When-Then" rule
  await page.click(".mdi-plus >> nth=0");
  await page.locator("li").filter({ hasText: "When-Then" }).click();
  // Then Name new rule "<name>"
  await page.fill("text=Rule name", energyRule.name);
  // Then Create When condition on "<attribute_when>" of "<asset>" of "<attribute_type>" with threshold "<value>"
  // create a new condtion
  await page.click("or-rule-when #component span");
  // await page.click(`or-rule-when >> text=${energyRule.attribute_type}`);
  await page.locator("or-rule-when li").filter({ hasText: energyRule.attribute_type }).click();

  // select asset
  await page.click("text=Asset Any of this type"); // It's the default text when nothing selected
  await page.click(`#idSelect >> text=${energyRule.asset}`);

  // select attritube
  await page.click('div[role="button"]:has-text("Attribute")');
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_when}")`);

  // select condition
  await page.click('div[role="button"]:has-text("Operator")');
  await page.click("text=Less than or equal to");

  // set value
  await page.fill('input[type="number"]', energyRule.value.toString());
  // Then Create Then action on "<attribute_then>" of "<asset>" of "<attribute_type>" with threshold "<value>"
  // create a new action
  await page.click('button:has-text("Add action")');
  await page.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${energyRule.attribute_type}")`);

  // select asset
  await page.click("text=Asset Matched");
  await page.click(`#matchSelect li[role="option"]:has-text("${energyRule.asset}")`);

  // select attribute
  await page.click(
    'text=Attribute Efficiency export Efficiency import Energy capacity Energy export tota >> div[role="button"]'
  );
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_then}")`);

  // set value. force: true is required for webkit as or-app somehow intercepts the pointer event
  await page.click('label:has-text("Value")', { force: true });
  await page.fill(
    'text=Always Always Once Once per hour Once per day Once per week Building asset City  >> input[type="number"]',
    energyRule.value.toString()
  );
  // Then Save rule
  await rulesPage.interceptResponse<number>("**/rules/realm", (rule) => {
    if (rule) manager.rules.push(rule);
  });
  await page.click('or-mwc-input:has-text("Save")');
  // Then We see the rule with name of "<name>"
  await expect(page.locator(`text=${energyRule.name}`)).toHaveCount(1);
});

test("Create a Flow rule", async ({ page, rulesPage, manager }) => {
  // Then Navigate to "Rule" tab
  await manager.navigateToTab("Rules");
  // When Create a new "Flow" rule
  await page.click(".mdi-plus >> nth=0");
  await page.locator("li", { hasText: "Flow" }).click();
  // Then Name new rule "Solar panel"
  await page.fill("text=Rule name", "Solar panel");
  // Then Drag in the elements
  // page.dragAndDrop(source, target[, options]) is an alternative
  // move all the elements
  await page.locator(".node-item.input-node", { hasText: "Attribute value" }).hover();
  await rulesPage.drag(450, 250);

  await page.hover("text=Number >> nth=0");
  await rulesPage.drag(450, 350);

  await page.hover("text=Number >> nth=0");
  await rulesPage.drag(450, 500);

  await page.hover("text=Number >> nth=0");
  await rulesPage.drag(450, 600);

  await page.hover("text=>");
  await rulesPage.drag(650, 300);

  await page.hover("text=Number switch");
  await rulesPage.drag(800, 425);

  await page.locator(".node-item.output-node", { hasText: "Attribute value" }).hover();
  await rulesPage.drag(1000, 425);
  // Then Set value
  // set read and write
  await page.click('button:has-text("Attribute") >> nth=0'); // read
  await page.click('div[role="alertdialog"] >> text=Solar Panel');
  await page.click('or-translate:has-text("Power") >> nth=0');
  await page.click('button:has-text("Add")');

  await page.click('button:has-text("Attribute") >> nth=1'); // write
  await page.click('div[role="alertdialog"] >> text=Solar Panel');
  await page.click('or-translate:has-text("Power forecast")');
  await page.click('button:has-text("Add")');

  await page.fill('[placeholder="value"] >> nth=0', "50");
  await page.fill('[placeholder="value"] >> nth=1', "60");
  await page.fill('[placeholder="value"] >> nth=2', "40");
  // Then Connect elements
  // connect elements
  await page.dragAndDrop(".socket >> nth=0", ".socket-side.inputs flow-node-socket .socket >> nth=0");
  await page.dragAndDrop(
    "flow-node:nth-child(2) .socket-side flow-node-socket .socket",
    "flow-node-socket:nth-child(2) .socket"
  );
  await page.dragAndDrop(
    "div:nth-child(3) flow-node-socket .socket",
    " flow-node:nth-child(6) .socket-side.inputs flow-node-socket .socket >> nth=0"
  );
  await page.dragAndDrop(
    "flow-node:nth-child(3) .socket-side flow-node-socket .socket",
    "flow-node:nth-child(6) .socket-side.inputs flow-node-socket:nth-child(2)"
  );
  await page.dragAndDrop(
    "flow-node:nth-child(4) .socket-side flow-node-socket .socket",
    "flow-node-socket:nth-child(3) .socket"
  );
  await page.dragAndDrop(
    "flow-node:nth-child(6) .socket-side.outputs flow-node-socket .socket",
    "flow-node:nth-child(7) .socket-side flow-node-socket .socket"
  );
  // Then Save rule
  await rulesPage.interceptResponse<number>("**/rules/realm", (rule) => {
    if (rule) manager.rules.push(rule);
  });
  await page.click('or-mwc-input:has-text("Save")');
  // Then We see the flow rule with name of "Solar panel"
  await expect(page.locator(`text="Solar panel"`)).toHaveCount(1);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
