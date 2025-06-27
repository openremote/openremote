import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsForRules as assets } from "./fixtures/data/assets.js";
import { energyRule } from "./fixtures/data/rules.js";

/**
 * @given The realm "smartcity" with user "smartcity" and assets are set up
 * @and Logged into OpenRemote "smartcity" realm as "smartcity"
 * @and Navigated to the "Rules" tab
 * @and Click add new rule
 */
test.beforeEach(async ({ page, manager }) => {
  await manager.setup("smartcity", { assets });
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Rules");
  await page.click(".mdi-plus >> nth=0");
});

test.use({ storageState: userStatePath });

/**
 * @when Selecting When-Then rule
 * @and Naming the rule
 * @and Configuring a When condition on the asset
 * @and Configuring a Then action on the same asset
 * @and Saving the rule
 * @then The When-Then rule should appear in the rule list
 */
test("Create a When-Then rule for an asset with a trigger and action", async ({ page, manager, shared }) => {
  await page.locator("li").filter({ hasText: "When-Then" }).click();
  await page.fill("text=Rule name", energyRule.name);

  await page.click("or-rule-when #component span");
  await page.locator("or-rule-when li").filter({ hasText: energyRule.attribute_type }).click();
  await page.click("text=Asset Any of this type");
  await page.click(`#idSelect >> text=${energyRule.asset}`);
  await page.click('div[role="button"]:has-text("Attribute")');
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_when}")`);
  await page.click('div[role="button"]:has-text("Operator")');
  await page.click("text=Less than or equal to");
  await page.fill('input[type="number"]', energyRule.value.toString());

  await page.click('button:has-text("Add action")');
  await page.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${energyRule.attribute_type}")`);
  await page.click("text=Asset Matched");
  await page.click(`#matchSelect li[role="option"]:has-text("${energyRule.asset}")`);
  await page.click(
    'text=Attribute Efficiency export Efficiency import Energy capacity Energy export tota >> div[role="button"]'
  );
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_then}")`);
  await page.click('label:has-text("Value")', { force: true });
  await page.fill(
    'text=Always Always Once Once per hour Once per day Once per week Building asset City  >> input[type="number"]',
    energyRule.value.toString()
  );

  await shared.interceptResponse<number>("**/rules/realm", (rule) => {
    if (rule) manager.rules.push(rule);
  });

  await page.click('or-mwc-input:has-text("Save")');
  await expect(page.locator(`text=${energyRule.name}`)).toHaveCount(1);
});

/**
 * @when Selecting Flow rule
 * @and Naming the rule
 * @and Dragging elements onto the canvas
 * @and Assigning attributes and values
 * @and Connecting elements together
 * @and Saving the rule
 * @then The Flow rule should appear in the rule list
 */
test("Create a Flow rule for an asset with logic connections", async ({ page, shared, manager }) => {
  await page.locator("li", { hasText: "Flow" }).click();
  await page.fill("text=Rule name", "Solar panel");

  await page.locator(".node-item.input-node", { hasText: "Attribute value" }).hover();
  await shared.drag(450, 250);
  await page.hover("text=Number >> nth=0");
  await shared.drag(450, 350);
  await page.hover("text=Number >> nth=0");
  await shared.drag(450, 500);
  await page.hover("text=Number >> nth=0");
  await shared.drag(450, 600);
  await page.hover("text=>");
  await shared.drag(650, 300);
  await page.hover("text=Number switch");
  await shared.drag(800, 425);
  await page.locator(".node-item.output-node", { hasText: "Attribute value" }).hover();
  await shared.drag(1000, 425);

  await page.click('button:has-text("Attribute") >> nth=0');
  await page.click('div[role="alertdialog"] >> text=Solar Panel');
  await page.click('or-translate:has-text("Power") >> nth=0');
  await page.click('button:has-text("Add")');

  await page.click('button:has-text("Attribute") >> nth=1');
  await page.click('div[role="alertdialog"] >> text=Solar Panel');
  await page.click('or-translate:has-text("Power forecast")');
  await page.click('button:has-text("Add")');

  await page.fill('[placeholder="value"] >> nth=0', "50");
  await page.fill('[placeholder="value"] >> nth=1', "60");
  await page.fill('[placeholder="value"] >> nth=2', "40");

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

  await shared.interceptResponse<number>("**/rules/realm", (rule) => {
    if (rule) manager.rules.push(rule);
  });

  await page.click('or-mwc-input:has-text("Save")');
  await expect(page.locator(`text="Solar panel"`)).toHaveCount(1);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
