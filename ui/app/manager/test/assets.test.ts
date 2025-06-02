import { expect, camelCaseToSentenceCase } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import assets, { agent, assetPatches, thing } from "./fixtures/data/assets.js";
import { WellknownMetaItems } from "@openremote/model";

test.use({ storageState: userStatePath });

assets.forEach(({ type, name, attributes }) => {
  const { attribute1, attribute2, attribute3, value1, value2, value3, x, y } =
    assetPatches[name as keyof typeof assetPatches];

  test(`Add new asset: ${name}`, async ({ page, manager, assetsPage, components }) => {
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await assetsPage.goto();
    // Then Create a "<asset>" with name of "<name>"
    await assetsPage.addAsset(type!, name!);
    // When Go to asset "<name>" info page
    await page.click(`#list-container >> text=${name}`);
    // Then Go to modify mode
    await components.assetViewer.switchMode("modify");
    // When set "<value_1>" to the "<attribute_1>"
    await components.assetViewer.setAttributeValue(attribute1, value1);
    // And set "<value_2>" to the "<attribute_2>"
    await components.assetViewer.setAttributeValue(attribute2, value2);
    // And save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
    // Then We see the asset with name of "<name>"
    await expect(page.locator(`text=${name}`)).toHaveCount(1);
  });
  test(`Search and select asset: ${name}`, async ({ page, manager }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Search for the "<name>"
    await page.fill('#filterInput input[type="text"]', name);
    // When Select the "<name>"
    await page.click(`text=${name}`);
    // Then We see the "<name>" page
    await expect(await page.waitForSelector(`#asset-header >> text=${name}`)).not.toBeNull();
  });
  test(`Update asset: ${name}`, async ({ page, manager, components }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Select the "<name>"
    await page.click(`text=${name}`);
    // Then Update "<value>" to the "<attribute>" with type of "<type>"
    const type = attributes[attribute3 as keyof typeof attributes].type; // TODO: check if this can be refactored
    const item = page.locator(`#field-${attribute3} input[type="${type}"]`);
    if (await item.isEditable()) {
      await item.fill(value3);
      await page.click(`#field-${attribute3} #send-btn span`);
    }
    // When Go to modify mode
    await components.assetViewer.switchMode("modify");
    // Then Update location of <location_x> and <location_y>
    await page.click("text=location GEO JSON point >> button span");
    await page.mouse.click(x, y, { delay: 1000 });
    await page.click('button:has-text("OK")');
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });
  test(`Set and cancel read-only for asset: ${name}`, async ({ page, manager, components }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Go to asset "<name>" info page
    await page.click(`text=${name}`);
    // Then Go to modify mode
    await components.assetViewer.switchMode("modify");
    await page.getByRole("button", { name: "Expand all" }).click();
    // Then Uncheck on readonly of "<attribute_1>"
    await components.assetViewer.getAttributeLocator(attribute1).click();
    await components.assetViewer.getConfigurationItemLocator(attribute1, "Read only").locator("label").click();
    // Then Check on readonly of "<attribute_2>"
    await components.assetViewer.getAttributeLocator(attribute2).click();
    await components.assetViewer.getConfigurationItemLocator(attribute2, "Read only").locator("label").click();
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
    // When Go to panel page
    await page.getByRole("button", { name: "View" }).click();
    await expect(page.getByRole("button", { name: "Modify" })).toBeVisible();
    // Then We should see a button on the right of "<attribute_1>"
    await expect(page.locator(`#field-${attribute1} #send-btn`)).toBeVisible();
    // And No button on the right of "<attribute_2>"
    await expect(page.locator(`#field-${attribute2} #send-btn`)).not.toBeVisible();
  });
  test(`Set assets' configuration item for Insights and Rules: ${name}`, async ({ page, manager, components }) => {
    // Given assets are setup
    await manager.setup("smartcity", { assets });
    // When Login to OpenRemote "smartcity" realm as "smartcity"
    await manager.goToRealmStartPage("smartcity");
    // Then Navigate to "asset" tab
    await manager.navigateToTab("asset");
    // When Go to asset "<name>" info page
    await page.click(`text=${name}`);
    // Then Go to modify mode
    await components.assetViewer.switchMode("modify");
    await page.getByRole("button", { name: "Expand all" }).click();
    // Then Select "<item_1>" and "<item_2>" on "<attribute_1>"
    await components.assetViewer.addConfigurationItems(attribute1, "ruleState", "storeDataPoints");
    // Then Select "<item_1>" and "<item_2>" on "<attribute_2>"
    await components.assetViewer.addConfigurationItems(attribute2, "ruleState", "storeDataPoints");
    // Then Save
    const saveBtn = page.getByRole("button", { name: "Save" });
    await saveBtn.click();
    await expect(saveBtn).toBeDisabled();
  });
});

test("Add all primitive configuration items", async ({ page, manager, components }) => {
  // Given assets are setup
  await manager.setup("smartcity", { assets: [thing] });
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  // When modifying thing asset
  await page.getByText("Thing").click();
  await components.assetViewer.switchMode("modify");
  // And adding primitive configuration items
  await page.getByRole("button", { name: "Expand all" }).click();
  const items: [`${WellknownMetaItems}`, any][] = [
    ["ruleState", true],
    ["hasPredictedDataPoints", true],
    ["storeDataPoints", true],
    ["label", "test"],
    ["showOnDashboard", true],
    ["readOnly", true],
    ["multiline", true],
    ["accessPublicWrite", true],
    ["momentary", true],
    ["accessRestrictedRead", true],
    ["dataPointsMaxAgeDays", 7],
    ["accessRestrictedWrite", true],
    ["secret", true],
    ["ruleResetImmediate", true],
    ["userConnected", "test"],
    ["accessPublicRead", true],
  ];
  await components.assetViewer.addConfigurationItems("notes", ...items.map(([item]) => item));
  // Then match values
  for (const [item, value] of items) {
    const itemLocator = components.assetViewer.getConfigurationItemLocator("notes");
    const options = { name: camelCaseToSentenceCase(item) };
    if (typeof value === "string") {
      const input = itemLocator.getByRole("textbox", options);
      await input.fill(value);
      await expect(input).toHaveValue(value);
    } else if (typeof value === "number") {
      const input = itemLocator.getByRole("spinbutton", options);
      await input.fill(`${value}`);
      await expect(input).toHaveValue(`${value}`);
    } else if (value) {
      await expect(itemLocator.getByRole("checkbox", options)).toBeChecked();
    } else {
      await expect(itemLocator.getByRole("checkbox", options)).not.toBeChecked();
    }
  }
});

test("Add all complex configuration items", async ({ page, manager, components, shared }) => {
  test.setTimeout(60_000);
  // Given assets are setup
  await manager.setup("smartcity", { assets: [thing, agent] });
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  // When modifying thing
  await page.getByText("Thing").click();
  await components.assetViewer.switchMode("modify");
  // And adding an agent link
  await page.getByRole("button", { name: "Expand all" }).click();
  // let resolvedSchema;
  // await shared.interceptResponse<any>("**/model/getItemSchemas", (schema) => {
  //   resolvedSchema = shared.resolveSubSchemasRecursive(schema["definitions"]["SimulatorAgentLink"], schema);
  // });
  // await components.assetViewer.addConfigurationItems("notes", "agentLink");
  // const agentLink = components.assetViewer.getConfigurationItemLocator("notes", "Agent link");
  // await agentLink.locator("or-collapsible-panel").waitFor();
  // await agentLink.locator("or-collapsible-panel").click();
  // await agentLink.locator("or-collapsible-panel").getByRole("button", { name: "Agent ID*" }).click();
  // const id = manager.assets.find(({ name }) => name === agent.name)?.id;
  // // Then
  // await agentLink.locator("li", { hasText: id }).click();
  // await components.jsonForms.walkForm(agentLink, resolvedSchema, { selectAllProps: true });

  const items: `${WellknownMetaItems}`[] = ["format", "constraints", "attributeLinks", "forecast", "units"];
  for (const item of items) {
    let schema;
    await shared.interceptResponse<any>("**/model/getItemSchemas", (v) => {
      schema = shared.resolveSubSchemasRecursive(v, v);
      console.log(schema);
      // schema = v;
    });
    await components.assetViewer.addConfigurationItems("notes", item);
    const itemLocator = components.assetViewer.getConfigurationItemLocator("notes", camelCaseToSentenceCase(item));
    await page.waitForTimeout(3000);
    // Then
    console.log(schema);
    await components.jsonForms.walkForm(itemLocator, schema, { selectAllProps: true });
  }
});

test("Delete assets", async ({ page, manager, assetsPage }) => {
  // Given assets are setup
  await manager.setup("smartcity", { assets });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("smartcity");
  await manager.navigateToTab("Assets");
  // When Delete assets
  await assetsPage.deleteSelectedAsset("Battery");
  await assetsPage.deleteSelectedAsset("Solar Panel");
  // Then We should see an empty asset column
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
