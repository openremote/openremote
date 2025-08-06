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
  await page.getByRole("button", { name: "Attribute", exact: true }).click();
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_when}")`);
  await page.getByRole("button", { name: "Operator", exact: true }).click();
  await page.click("text=Less than or equal to");
  await page.getByRole("spinbutton", { name: "Energy level" }).fill(energyRule.value.toString());

  await page.getByRole("button", { name: "Add action" }).click();
  await page.click(`or-rule-then-otherwise li[role="menuitem"]:has-text("${energyRule.attribute_type}")`);
  await page.click("text=Asset Matched");
  await page.click(`#matchSelect li[role="option"]:has-text("${energyRule.asset}")`);
  await page.getByRole("button", { name: "Attribute", exact: true }).click();
  await page.click(`li[role="option"]:has-text("${energyRule.attribute_then}")`);
  await page.getByRole("spinbutton", { name: "Value" }).fill(energyRule.value.toString());

  await shared.interceptResponse<number>("**/rules/realm", (rule) => {
    if (rule) manager.rules.push(rule);
  });

  await page.getByRole("button", { name: "Save" }).click();
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
  await page.hover("text=Number");
  await shared.drag(450, 350);
  await page.hover("text=Number");
  await shared.drag(450, 500);
  await page.hover("text=Number");
  await shared.drag(450, 600);
  await page.hover("text=>");
  await shared.drag(650, 300);
  await page.hover("text=Number switch");
  await shared.drag(800, 425);
  await page.locator(".node-item.output-node", { hasText: "Attribute value" }).hover();
  await shared.drag(1000, 425);

  await page.getByRole("button", { name: "Attribute" }).nth(0).click();
  await page.getByRole("alertdialog").getByText("Solar Panel").click();
  await page.getByRole("option", { name: "Power", exact: true }).click();
  await page.getByRole("button", { name: "Add" }).click();

  await page.getByRole("button", { name: "Attribute" }).nth(1).click();
  await page.getByRole("alertdialog").getByText("Solar Panel").click();
  await page.getByRole("option", { name: "Power forecast", exact: true }).click();
  await page.getByRole("button", { name: "Add" }).click();

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

  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.locator("or-rule-tree").getByText("Solar panel")).toHaveCount(1);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
