import { ct } from "./fixtures";
import { expect } from "@openremote/test";
import { OrAssetViewer } from "@openremote/or-asset-viewer";
import { validAsset, invalidAsset, configuredAsset } from "./fixtures/data/asset";

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
    await shared.registerAssets([validAsset, invalidAsset, configuredAsset]);
});

// Due to how the component tests resolve imports, imported data with an object reference gets
// confused for a component that is meant to be registered in the playwright component test app.
// Which causes the data to be transformed to an intermediate object referencing the data.
//
// So we can use a "cloned" variable outside the test (but in the same test file) to avoid this.
const validId = validAsset.id;
ct("Should not show asset invalid error", async ({ mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: validId, editMode: true },
    });
    await expect(component).not.toContainText("Asset is not valid");
});

// Due to how the component tests resolve imports, imported data with an object reference gets
// confused for a component that is meant to be registered in the playwright component test app.
// Which causes the data to be transformed to an intermediate object referencing the data.
//
// So we can use a "cloned" variable outside the test (but in the same test file) to avoid this.
const invalidId = invalidAsset.id;
ct("Should show asset invalid error", async ({ mount, assetViewer }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: invalidId, editMode: true },
    });
    await assetViewer.getAttributeValueLocator("invalid").fill("0.1");
    await assetViewer.getAttributeValueLocator("invalid").press("Enter");
    await expect(assetViewer.getAttributeValueLocator("invalid")).toHaveAttribute("invalid")
});

ct("Should not show asset invalid error after switching assets", async ({ mount, assetViewer }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: invalidId, editMode: true },
    });
    await assetViewer.getAttributeValueLocator("invalid").fill("0.1");
    await assetViewer.getAttributeValueLocator("invalid").press("Enter");
    await expect(assetViewer.getAttributeValueLocator("invalid")).toHaveAttribute("invalid")

    await component.update({ props: { assetId: validId, editMode: true } });
    await expect(component).not.toContainText("Asset is not valid");
});

ct("Should show asset configuration import and export actions between save and view in modify mode", async ({ mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: validId, editMode: true },
    });

    await expect(component.locator("#export-attribute-config-btn")).toBeVisible();
    await expect(component.locator("#import-attribute-config-btn")).toBeVisible();

    const actionOrder = await component.locator("#right-wrapper").evaluate((wrapper) =>
        Array.from(wrapper.querySelectorAll("or-vaadin-button")).map((button) => button.id)
    );
    expect(actionOrder).toEqual([
        "save-btn",
        "export-attribute-config-btn",
        "import-attribute-config-btn",
        "edit-btn",
    ]);
});

ct("Should disable asset configuration export when there are unsaved changes", async ({ mount, assetViewer }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: validId, editMode: true },
    });

    const exportButton = component.locator("#export-attribute-config-btn");
    await expect(exportButton).not.toBeDisabled();

    await assetViewer.getAttributeValueLocator("notes").fill("changed notes");
    await assetViewer.getAttributeValueLocator("notes").press("Enter");

    await expect(component.locator("#save-btn")).not.toBeDisabled();
    await expect(exportButton).toBeDisabled();
    await expect(component.locator("#import-attribute-config-btn")).not.toBeDisabled();
});

const configuredId = configuredAsset.id;
ct("Should export selected asset attribute configuration", async ({ page, mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: configuredId, editMode: true },
    });

    let requestBody: unknown;
    await page.route("**/api/master/asset/configuredAsset/attribute-config/export", async (route) => {
        requestBody = route.request().postDataJSON();
        await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
                version: 1,
                assetType: "ThingAsset",
                attributes: {
                    notes: {
                        type: "text",
                        meta: { readOnly: true },
                    },
                },
            }),
        });
    });

    await component.locator("#export-attribute-config-btn").click();

    const dialog = page.locator("or-mwc-dialog");
    await expect(dialog).toContainText("Model");
    await expect(dialog).toContainText("Notes");
    await expect(dialog).toContainText("Read Only");
    await expect(dialog).not.toContainText("Location");
    await dialog.locator("or-vaadin-checkbox[label='Model (text)']").click();

    const downloadPromise = page.waitForEvent("download");
    await dialog.locator("[data-mdc-dialog-action='export']").click();
    const download = await downloadPromise;

    expect(requestBody).toEqual({ attributeNames: ["notes"] });
    expect(download.suggestedFilename()).toBe("Configured Thing-attribute-config.json");
});

ct("Should preview imported asset attribute configuration", async ({ page, mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: configuredId, editMode: true },
    });

    const configuration = {
        version: 1,
        assetType: "OtherAsset",
        attributes: {
            notes: {
                type: "text",
                meta: { readOnly: false },
            },
            missing: {
                type: "number",
                meta: { readOnly: true },
            },
            model: {
                type: "number",
                meta: { label: "Model" },
            },
        },
    };

    let requestBody: any;
    await page.route("**/api/master/asset/configuredAsset/attribute-config/import/preview", async (route) => {
        requestBody = route.request().postDataJSON();
        await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
                assetTypeMismatch: {
                    expected: "ThingAsset",
                    actual: "OtherAsset",
                },
                importableAttributes: [
                    { name: "notes", type: "text" },
                ],
                missingAttributes: [
                    { name: "missing", type: "number" },
                ],
                typeMismatches: [
                    { name: "model", importedType: "number", targetType: "text" },
                ],
                patchedAttributes: {
                    notes: {
                        name: "notes",
                        type: "text",
                        meta: { readOnly: false },
                    },
                },
            }),
        });
    });

    await component.locator("#import-attribute-config-btn").click();

    const dialog = page.locator("or-mwc-dialog");
    await dialog.locator("input[type='file']").setInputFiles({
        name: "attribute-config.json",
        mimeType: "application/json",
        buffer: Buffer.from(JSON.stringify(configuration)),
    });

    await expect(dialog).toContainText("attribute-config.json");
    await expect(dialog).toContainText("ThingAsset");
    await expect(dialog).toContainText("OtherAsset");
    await expect(dialog).toContainText("notes (text)");
    await expect(dialog).toContainText("missing (number)");
    await expect(dialog).toContainText("model (number -> text)");

    expect(requestBody.configuration).toEqual(configuration);
    expect(requestBody.targetAsset.id).toBe("configuredAsset");
});
