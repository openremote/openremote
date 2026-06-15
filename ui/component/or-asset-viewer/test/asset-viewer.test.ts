import { ct } from "./fixtures";
import { expect } from "@openremote/test";
import { OrAssetViewer } from "@openremote/or-asset-viewer";
import { validAsset, invalidAsset, configuredAsset, partiallyConfiguredAsset } from "./fixtures/data/asset";

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
    await shared.registerAssets([validAsset, invalidAsset, configuredAsset, partiallyConfiguredAsset]);
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

ct("Should export selected generic asset attribute configuration paths", async ({ page, mount }) => {
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
                    model: {
                        type: "text",
                        meta: {
                            agentLink: {
                                type: "ModbusAgentLink",
                            },
                        },
                    },
                    notes: {
                        type: "text",
                        meta: {
                            agentLink: {
                                type: "ModbusAgentLink",
                            },
                        },
                    },
                },
                genericParameters: {
                    agentLinkId: {
                        type: "text",
                        paths: [
                            "attributes.model.meta.agentLink.id",
                            "attributes.notes.meta.agentLink.id",
                        ],
                    },
                },
            }),
        });
    });

    await component.locator("#export-attribute-config-btn").click();

    const dialog = page.locator("or-mwc-dialog");
    await expect(dialog).toContainText("Generic parameters");
    await expect(dialog.locator("[data-generic-parameter-path='meta.agentLink.id']")).toBeVisible();
    await expect(dialog.locator("[data-generic-parameter-path='meta.agentLink.unitId']")).toBeVisible();
    await dialog.locator("[data-generic-parameter-path='meta.agentLink.id']").click();

    const downloadPromise = page.waitForEvent("download");
    await dialog.locator("[data-mdc-dialog-action='export']").click();
    await downloadPromise;

    expect(requestBody).toEqual({
        attributeNames: ["model", "notes"],
        genericParameterPaths: ["meta.agentLink.id"],
    });
});

const partiallyConfiguredId = partiallyConfiguredAsset.id;
ct("Should export generic paths shared by only some selected attributes", async ({ page, mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: partiallyConfiguredId, editMode: true },
    });

    let requestBody: unknown;
    await page.route("**/api/master/asset/partiallyConfiguredAsset/attribute-config/export", async (route) => {
        requestBody = route.request().postDataJSON();
        await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
                version: 1,
                assetType: "ThingAsset",
                attributes: {
                    model: {
                        type: "text",
                        meta: {
                            label: "Model",
                            agentLink: {
                                type: "ModbusAgentLink",
                            },
                        },
                    },
                    notes: {
                        type: "text",
                        meta: {
                            readOnly: true,
                            agentLink: {
                                type: "ModbusAgentLink",
                            },
                        },
                    },
                    serialNumber: {
                        type: "text",
                        meta: {
                            label: "Serial number",
                        },
                    },
                },
                genericParameters: {
                    agentLinkId: {
                        type: "text",
                        paths: [
                            "attributes.model.meta.agentLink.id",
                            "attributes.notes.meta.agentLink.id",
                        ],
                    },
                },
            }),
        });
    });

    await component.locator("#export-attribute-config-btn").click();

    const dialog = page.locator("or-mwc-dialog");
    await expect(dialog).toContainText("Generic parameters");
    await expect(dialog.locator("[data-generic-parameter-path='meta.agentLink.id']")).toBeVisible();
    await dialog.locator("[data-generic-parameter-path='meta.agentLink.id']").click();

    const downloadPromise = page.waitForEvent("download");
    await dialog.locator("[data-mdc-dialog-action='export']").click();
    await downloadPromise;

    expect(requestBody).toEqual({
        attributeNames: ["model", "notes", "serialNumber"],
        genericParameterPaths: ["meta.agentLink.id"],
    });
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
    await expect(dialog).toContainText("overwrite existing metadata");

    expect(requestBody.configuration).toEqual(configuration);
    expect(requestBody.targetAsset.id).toBe("configuredAsset");
});

ct("Should request generic parameter values before previewing imported asset attribute configuration", async ({ page, mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: configuredId, editMode: true },
    });

    const configuration = {
        version: 1,
        assetType: "ThingAsset",
        attributes: {
            notes: {
                type: "text",
                meta: {
                    agentLink: {
                        type: "ModbusAgentLink",
                    },
                },
            },
        },
        genericParameters: {
            agentLinkId: {
                type: "text",
                paths: ["attributes.notes.meta.agentLink.id"],
            },
            agentLinkUnitId: {
                type: "number",
                paths: ["attributes.notes.meta.agentLink.unitId"],
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
                importableAttributes: [
                    { name: "notes", type: "text" },
                ],
                patchedAttributes: {
                    notes: {
                        name: "notes",
                        type: "text",
                        meta: {
                            agentLink: {
                                type: "ModbusAgentLink",
                                id: "agent-1",
                                unitId: 3,
                            },
                        },
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

    await expect(dialog.locator("[data-mdc-dialog-action='import']")).toBeDisabled();
    await expect(dialog.locator("#asset-attribute-config-preview-btn")).toBeDisabled();

    await dialog.locator("#asset-attribute-config-generic-agentLinkId input").fill("agent-1");
    await dialog.locator("#asset-attribute-config-generic-agentLinkUnitId input").fill("3");
    await dialog.locator("#asset-attribute-config-preview-btn").click();

    await expect(dialog).toContainText("notes (text)");
    expect(requestBody.configuration).toEqual(configuration);
    expect(requestBody.genericParameterValues).toEqual({
        agentLinkId: "agent-1",
        agentLinkUnitId: 3,
    });
});

ct("Should apply previewed asset attribute configuration to the draft", async ({ page, mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: configuredId, editMode: true },
    });

    await page.route("**/api/master/asset/configuredAsset/attribute-config/import/preview", async (route) => {
        await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
                importableAttributes: [
                    { name: "notes", type: "text" },
                ],
                missingAttributes: [],
                typeMismatches: [],
                patchedAttributes: {
                    notes: {
                        name: "notes",
                        type: "text",
                        meta: { readOnly: false },
                    },
                    model: {
                        name: "model",
                        type: "text",
                        meta: { label: "Model" },
                    },
                    location: {
                        name: "location",
                        type: "GEO_JSONPoint",
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
        buffer: Buffer.from(JSON.stringify({
            version: 1,
            assetType: "ThingAsset",
            attributes: {
                notes: {
                    type: "text",
                    meta: { readOnly: false },
                },
            },
        })),
    });

    await expect(dialog).toContainText("notes (text)");
    await dialog.locator("[data-mdc-dialog-action='import']").click();

    const resultDialog = page.locator("or-mwc-dialog");
    await expect(resultDialog).toContainText("Attribute configuration import result");
    await expect(resultDialog).toContainText("notes (text)");
    await resultDialog.locator("[data-mdc-dialog-action='ok']").click();
    await expect(page.locator("or-mwc-dialog")).toHaveCount(0);
    await expect(component.locator("#save-btn")).not.toBeDisabled();

    const draft = await component.evaluate((element: any) => ({
        modified: element._assetInfo.modified,
        notesReadOnly: element._assetInfo.asset.attributes.notes.meta.readOnly,
        notesHasLabel: Object.prototype.hasOwnProperty.call(element._assetInfo.asset.attributes.notes.meta, "label"),
        modelLabel: element._assetInfo.asset.attributes.model.meta.label,
        locationType: element._assetInfo.asset.attributes.location.type,
    }));

    expect(draft).toEqual({
        modified: true,
        notesReadOnly: false,
        notesHasLabel: false,
        modelLabel: "Model",
        locationType: "GEO_JSONPoint",
    });
});
