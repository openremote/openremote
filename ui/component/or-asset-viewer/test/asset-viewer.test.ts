import { ct } from "./fixtures";
import { expect } from "@openremote/test";
import { OrAssetViewer } from "@openremote/or-asset-viewer";
import { validAsset, invalidAsset } from "./fixtures/data/asset";

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
    await shared.registerAssets([validAsset, invalidAsset]);
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
    await expect(component).toContainText("Asset is not valid");
});

ct("Should not show asset invalid error after switching assets", async ({ mount, assetViewer }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: invalidId, editMode: true },
    });
    await assetViewer.getAttributeValueLocator("invalid").fill("0.1");
    await assetViewer.getAttributeValueLocator("invalid").press("Enter");
    await expect(component).toContainText("Asset is not valid");

    await component.update({ props: { assetId: validId, editMode: true } });
    await expect(component).not.toContainText("Asset is not valid");
});
