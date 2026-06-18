import {expect} from "@openremote/test";
import {test, userStatePath} from "./fixtures/manager.js";
import {preparedAssetsForRules as assets} from "./fixtures/data/assets.js";
import {energyRule} from "./fixtures/data/rules.js";
import {Asset, RealmRuleset, RulesetLang} from "@openremote/model";

test.use({storageState: userStatePath});

function createRealmRule(rule: Partial<RealmRuleset> & Pick<RealmRuleset, "id" | "name" | "lang">): RealmRuleset {
    return {
        type: "realm",
        id: rule.id,
        version: rule.version ?? 0,
        realm: rule.realm ?? "smartcity",
        enabled: rule.enabled ?? true,
        name: rule.name,
        lang: rule.lang,
        rules: rule.rules ?? "SomeRulesCode",
        meta: rule.meta,
        status: rule.status,
        error: rule.error,
        accessPublicRead: rule.accessPublicRead,
    };
}

async function mockRealmRulesApi(page: any, initialRules: RealmRuleset[]) {
    let rules = [...initialRules];
    let deleteRequests = 0;
    let postRequests = 0;
    let putRequests = 0;

    await page.route("**/api/**/rules/realm**", async (route: any) => {
        const request = route.request();
        const method = request.method();
        const url = new URL(request.url());

        if (method === "GET") {
            if (/\/rules\/realm\/\d+$/.test(url.pathname)) {
                const id = Number(url.pathname.split("/").pop());
                const rule = rules.find((r) => r.id === id);
                await route.fulfill(rule ? { status: 200, json: rule } : { status: 404, json: { message: "Not found" } });
                return;
            }

            await route.fulfill({ status: 200, json: rules });
            return;
        }

        if (method === "DELETE") {
            deleteRequests += 1;
            const id = Number(url.pathname.split("/").pop());
            rules = rules.filter((rule) => rule.id !== id);
            await route.fulfill({ status: 204, body: "" });
            return;
        }

        if (method === "POST") {
            postRequests += 1;
            await route.fulfill({ status: 500, json: { message: "Unexpected create request" } });
            return;
        }

        if (method === "PUT") {
            putRequests += 1;
            await route.fulfill({ status: 500, json: { message: "Unexpected update request" } });
            return;
        }

        await route.fallback();
    });

    return {
        get deleteRequests() {
            return deleteRequests;
        },
        get postRequests() {
            return postRequests;
        },
        get putRequests() {
            return putRequests;
        },
        get rules() {
            return rules;
        }
    };
}

/**
 * Simple function that generates assets based on {@link assets}
 * @param multiplier - Amount of assets to generate per asset type
 */
function generateALotOfAssets(multiplier = 5): Asset[] {
    return Array.from({length: multiplier}, (_, i) =>
        assets.map((a: Asset) => ({...a, name: `${a.name} ${i}`}))
    ).flat();
}

/**
 * @when Creating a When-Then rule
 * @and Naming the rule
 * @and Configuring a When condition on the asset
 * @and Configuring a Then action on the same asset
 * @and Saving the rule
 * @then The When-Then rule should appear in the rule list
 */
test("Create a When-Then rule for an asset with a trigger and action", async ({page, manager, shared}) => {
    await manager.setup("smartcity", {assets});
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");
    await page.click(".mdi-plus >> nth=0");
    await page.locator("li").filter({hasText: "When-Then"}).click();
    await page.getByRole("textbox", { name: "Rule name" }).fill(energyRule.name);

    // When clause
    const when = page.locator("or-rule-when");
    await when.getByRole("button", { name: "Add condition", exact: true }).click();
    await when.locator("li[role='menuitem']").filter({hasText: energyRule.asset_type}).click();
    await when.getByRole("combobox", { name: "Asset", exact: true }).click();
    await when.getByRole("option", { name: energyRule.asset, exact: true }).click();
    await when.getByRole("combobox", { name: "Attribute", exact: true }).click();
    await when.getByRole("option", { name: energyRule.attribute_when, exact: true }).click();
    await when.getByRole("combobox", { name: "Operator", exact: true }).click();
    await when.getByRole("option", { name: "Less than or equal to", exact: true }).click();
    await when.getByRole("spinbutton", {name: "Energy level"}).fill(energyRule.value.toString());

    // Then clause
    const then = page.locator("or-rule-then-otherwise")
    await then.getByRole("button", {name: "Add action", exact: true}).click();
    await then.locator("li[role='menuitem']").filter({hasText: energyRule.asset_type}).click();
    await then.getByRole("combobox", { name: "Asset", exact: true }).click();
    await then.getByRole("option", { name: energyRule.asset, exact: true }).click();
    await then.getByRole("combobox", {name: "Attribute", exact: true}).click();
    await then.getByRole("option", {name: energyRule.attribute_then, exact: true}).click();
    await then.getByRole("spinbutton", {name: "Value"}).fill(energyRule.value.toString());

    await shared.interceptResponse<number>("**/rules/realm", (rule) => {
        if (rule) manager.rules.push(rule);
    });

    await page.getByRole("button", {name: "Save"}).click();
    await expect(page.locator(`text=${energyRule.name}`)).toHaveCount(1);
});

/**
 * @when Creating a When-Then rule
 * @and Naming the rule
 * @and Configuring a When condition by searching for a specific asset
 * @and Configuring a Then action by searching for a different asset
 * @and Saving the rule
 * @then The When-Then rule should appear in the rule list
 */
test("Create a When-Then rule by searching for an asset", async ({page, manager, shared}) => {
    const multiplier = 200; // Using a multiplier above 100, which is the default querying limit
    const aLotOfAssets = generateALotOfAssets(multiplier);
    await manager.setup("smartcity", {assets: aLotOfAssets});
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");

    // Make sure the correct amount assets are set up, and the variables for this rule are adjusted
    expect(manager.assets.length).toBe(multiplier * assets.length);
    const firstAssetName = [...manager.assets].find(a => a.name?.includes(energyRule.asset))?.name ?? "";
    const lastAssetName = [...manager.assets].reverse().find(a => a?.name?.includes(energyRule.asset))?.name ?? "";
    expect(firstAssetName).toContain(energyRule.asset);
    expect(lastAssetName).toContain(energyRule.asset);

    await page.click(".mdi-plus >> nth=0");
    await page.locator("li").filter({hasText: "When-Then"}).click();
    await page.getByRole("textbox", { name: "Rule name" }).fill(energyRule.name);

    // Select asset type of the When clause, search for the last asset in the list, and select the attribute
    const when = page.locator("or-rule-when")
    await when.getByRole("button", { name: "Add condition" }).click();
    await when.locator("li[role='menuitem']").filter({hasText: energyRule.asset_type}).click();
    await when.getByRole("combobox", {name: "Asset", exact: true}).fill(lastAssetName);
    await when.getByRole("option", { name: lastAssetName, exact: true }).click();
    await when.getByRole("combobox", {name: "Attribute", exact: true}).fill(energyRule.attribute_when);
    await when.getByRole("option", {name: energyRule.attribute_when, exact: true}).click();
    await when.getByRole("combobox", { name: "Operator", exact: true }).fill("Less than or");
    await when.getByRole("option", {name: "Less than or equal to", exact: true}).click();
    await when.getByRole("spinbutton", {name: "Energy level"}).fill(energyRule.value.toString());

    // Configure Then clause
    const then = page.locator("or-rule-then-otherwise");
    await then.getByRole("button", {name: "Add action"}).click();
    await then.locator("li[role='menuitem']").filter({hasText: energyRule.asset_type}).click();
    await then.getByRole("combobox", {name: "Asset", exact: true}).fill(firstAssetName);
    await then.getByRole("option", {name: firstAssetName, exact: true}).click();
    await then.getByRole("combobox", {name: "Attribute", exact: true}).fill(energyRule.attribute_then);
    await then.getByRole("option", {name: energyRule.attribute_then, exact: true}).click();
    await then.getByRole("spinbutton", {name: "Value"}).fill(energyRule.value.toString());

    await shared.interceptResponse<number>("**/rules/realm", (rule) => {
        if (rule) manager.rules.push(rule);
    });

    await page.getByRole("button", {name: "Save"}).click();
    await expect(page.locator(`text=${energyRule.name}`)).toHaveCount(1);
});

/**
 * @when Creating a Flow rule
 * @and Naming the rule
 * @and Dragging elements onto the canvas
 * @and Assigning attributes and values
 * @and Connecting elements together
 * @and Saving the rule
 * @then The Flow rule should appear in the rule list
 */
test("Create a Flow rule for an asset with logic connections", async ({page, shared, manager}) => {
    await manager.setup("smartcity", {assets});
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");
    await page.click(".mdi-plus >> nth=0");
    await page.locator("li", {hasText: "Flow"}).click();
    await page.getByRole("textbox", { name: "Rule name" }).fill("Solar panel");

    await page.locator(".node-item.input-node", {hasText: "Attribute value"}).hover();
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
    await page.locator(".node-item.output-node", {hasText: "Attribute value"}).hover();
    await shared.drag(1000, 425);

    await page.getByRole("button", {name: "Attribute"}).nth(0).click();
    await page.getByRole("alertdialog").getByText("Solar Panel").click();
    await page.getByRole("option", {name: "Power", exact: true}).click();
    await page.getByRole("button", {name: "Add"}).click();

    await page.getByRole("button", {name: "Attribute"}).nth(1).click();
    await page.getByRole("alertdialog").getByText("Solar Panel").click();
    await page.getByRole("option", {name: "Power forecast", exact: true}).click();
    await page.getByRole("button", {name: "Add"}).click();

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

    await page.getByRole("button", {name: "Save"}).click();
    await expect(page.locator("or-rule-tree").getByText("Solar panel")).toHaveCount(1);
});

test("Legacy JavaScript rules are visible but read-only in the rules UI", async ({page, manager}) => {
    const legacyJavascriptRule = createRealmRule({
        id: 71001,
        name: "Legacy JS rule",
        lang: RulesetLang.JAVASCRIPT,
        rules: "console.log('legacy');"
    });
    const api = await mockRealmRulesApi(page, [legacyJavascriptRule]);

    await manager.setup("smartcity");
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");

    await expect(page.locator("or-rule-tree").getByText(legacyJavascriptRule.name!)).toHaveCount(1);
    await page.locator("or-rule-tree").getByText(legacyJavascriptRule.name!).click();

    await expect(page.locator("or-rule-viewer")).toContainText("JavaScript rules are legacy and can only be viewed or deleted.");
    await expect(page.getByRole("button", {name: "Save"})).toBeDisabled();
    await expect(page.locator("or-rule-tree").locator("or-mwc-input[icon='content-copy']")).toHaveCount(0);

    await page.click(".mdi-plus >> nth=0");
    await expect(page.locator("li").filter({hasText: "JavaScript"})).toHaveCount(0);

    expect(api.postRequests).toBe(0);
    expect(api.putRequests).toBe(0);
});

test("Legacy JavaScript rules can still be deleted from the rules UI", async ({page, manager}) => {
    const legacyJavascriptRule = createRealmRule({
        id: 71002,
        name: "Legacy JS deletable rule",
        lang: RulesetLang.JAVASCRIPT,
        rules: "console.log('delete me');"
    });
    const api = await mockRealmRulesApi(page, [legacyJavascriptRule]);

    await manager.setup("smartcity");
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");

    await page.locator("or-rule-tree").getByText(legacyJavascriptRule.name!).click();
    await page.locator("or-rule-tree").getByRole("button").filter({ has: page.locator('*[icon="delete"]') }).click();
    await page.getByRole("button", {name: "Delete"}).click();

    await expect(page.locator("or-rule-tree").getByText(legacyJavascriptRule.name!)).toHaveCount(0);
    expect(api.deleteRequests).toBe(1);
});

test("Groups containing legacy JavaScript rules cannot be renamed", async ({page, manager}) => {
    const groupedLegacyJavascriptRule = createRealmRule({
        id: 71003,
        name: "Legacy JS grouped rule",
        lang: RulesetLang.JAVASCRIPT,
        meta: {groupId: "Legacy JS group"},
        rules: "console.log('grouped');"
    });
    const api = await mockRealmRulesApi(page, [groupedLegacyJavascriptRule]);

    await manager.setup("smartcity");
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");

    await expect(page.locator("or-rule-tree").getByText("Legacy JS group", { exact: true })).toHaveCount(1);
    await page.locator("or-rule-tree").getByText("Legacy JS group", { exact: true }).click();
    await page.getByRole("textbox", { name: "Group name"}).fill("Renamed JS group");
    await page.getByRole("button", {name: "Save"}).isDisabled();
    expect(api.putRequests).toBe(0);
});

test("Dragging a legacy JavaScript rule is blocked", async ({page, manager}) => {
    const legacyJavascriptRule = createRealmRule({
        id: 71004,
        name: "Legacy JS drag rule",
        lang: RulesetLang.JAVASCRIPT,
        rules: "console.log('drag');"
    });
    const api = await mockRealmRulesApi(page, [legacyJavascriptRule]);

    await manager.setup("smartcity");
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("Rules");

    await expect(page.locator("or-rule-tree").getByText(legacyJavascriptRule.name!)).toHaveCount(1);

    const dispatchSucceeded = await page.locator("or-rules").evaluate((element, ruleName) => {
        const tree = element.shadowRoot?.querySelector("or-rule-tree") as any;
        const rule = tree?.rules?.find((candidate: any) => candidate.name === ruleName);

        if (!rule) {
            throw new Error(`Could not find ruleset '${ruleName}' in tree state`);
        }

        return element.dispatchEvent(new CustomEvent("or-tree-drag", {
            bubbles: true,
            composed: true,
            cancelable: true,
            detail: {
                nodes: [{id: String(rule.id), label: rule.name, ruleset: rule}],
                groupNode: {id: "Target Group", label: "Target Group", children: []},
                newNodes: []
            }
        }));
    }, legacyJavascriptRule.name!);

    expect(dispatchSucceeded).toBe(false);
    await expect(page.locator("or-mwc-snackbar")).toContainText("JavaScript rules are legacy and cannot be updated.");
    expect(api.putRequests).toBe(0);
});

test.afterEach(async ({manager}) => {
    await manager.cleanUp();
});
