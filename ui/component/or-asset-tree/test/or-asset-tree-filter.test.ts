import {ct, expect} from "@openremote/test";
import {OrAssetTree} from "@openremote/or-asset-tree";
import {Asset, Attribute} from "@openremote/model";
import "@openremote/or-asset-tree";

ct.beforeEach(async ({ shared }) => {
    await shared.fonts();
    await shared.locales();
});

ct("Asset tree filter by Asset ID", async ({mount}) => {
    const component = await mount(OrAssetTree, {
        props: {
            disableSubscribe: true,
            dataProvider: async (): Promise<Asset[]> => [{
                id: "customlight1",
                version: 1,
                createdOn: Date.now(),
                name: "Custom Light 1",
                realm: "master",
                type: "LightAsset"
            }],
        },
        // slots: {},
        // on: {},
    });
    expect(component).toBeAttached();
    expect(component).toBeVisible();
})
