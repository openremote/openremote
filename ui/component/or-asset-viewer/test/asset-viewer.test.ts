import { ct } from "./fixtures";
import { expect } from "@openremote/test";
import { OrAssetViewer } from "@openremote/or-asset-viewer";
import { validAsset, invalidAsset } from "./fixtures/data/asset";

ct.beforeEach(async ({ shared }) => {
    await shared.locales();
    await shared.fonts();
    await shared.registerAssets([validAsset, invalidAsset]);
});

// Must destructure "validAsset" to avoid this being registered under
// the playwright test app as import.
const validId = validAsset.id;
ct("Should not show asset invalid error", async ({ mount }) => {
    const component = await mount(OrAssetViewer, {
        props: { assetId: validId, editMode: true },
    });
    await expect(component).not.toContainText("Invalid asset");
});

// Must destructure "validAsset" to avoid this being registered under
// the playwright test app as import.
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
