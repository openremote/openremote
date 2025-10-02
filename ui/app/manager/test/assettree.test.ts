/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { expect } from "@openremote/test";
import {Manager, test, userStatePath} from "./fixtures/manager.js";
import {batteryAsset, electricityAsset, parentAssets} from "./fixtures/data/assettree.js";
import {Asset} from "@openremote/model";

test.use({ storageState: userStatePath });

// After each test, clean up all data
test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});

// Utility function to create parent assets, and apply assets as children
async function applyParentAssets(parentAssets: Asset[], manager: Manager) {
    await Promise.all(parentAssets.map(a => manager.createAsset(a)));
    const cityIds = manager.assets.filter(a => a.type === "CityAsset").sort((a, b) => a.name!.localeCompare(b.name!)).map(a => a.id);
    const childAssets = manager.assets.filter(a => a.type !== "CityAsset");
    childAssets.forEach(ca => ca.parentId = cityIds[0]);
    await Promise.all(childAssets.map(ca => manager.updateAsset(ca)));
}

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to OpenRemote "smartcity" realm as admin
 * @and Navigating to the "Assets" tab
 * @and Tries to navigate through the assets in the system using the asset tree
 * @then The asset list should be complete and should not display any artifacts or visual errors.
 */
test(`Check if assets are visible in the tree`, async ({ assetTree, manager, assetsPage }) => {
    await manager.setup("smartcity", { assets: [batteryAsset, electricityAsset] });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();

    // Check if City Asset 1, City Asset 2 and Consoles group asset are listed.
    await expect(assetTree.getAssetNodes()).toHaveCount(3);
    await expect(assetTree.getAssetNodes().nth(0)).toContainText(parentAssets[0].name);
    await expect(assetTree.getAssetNodes().nth(1)).toContainText(parentAssets[1].name);
    await expect(assetTree.getAssetNodes().nth(2)).toContainText('Consoles');
    await expect(assetTree.getAssetNodes().locator('.expander')).toHaveCount(3);

    // Check if expandable of City Asset 1 is correct.
    const cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('.expander').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2);
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveText([batteryAsset.name, electricityAsset.name]);

    // Check if expandable of City Asset 2 is correct.
    const cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('.expander').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2 + 0);

    // Check if expandable of Consoles group asset is correct.
    const consoleAsset = assetTree.getAssetNodes().filter({ hasText: 'Consoles' });
    await consoleAsset.locator('.expander').click();
    await expect.poll(async () => await assetTree.getAssetNodes().count(), {
        message: "Waiting for the Console assets to appear..."
    }).toBeGreaterThanOrEqual(3 + 2 + 0 + 1); // (there is at least 1 console, but could be more with a larger test suite)
});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab
 * @and Searching for the asset by name
 * @and Selecting the asset from the list
 * @then The asset detail page is displayed
 */
test(`Search for and select the battery asset`, async ({ page, manager, assetTree, assetsPage }) => {
    await manager.setup("smartcity", { assets: [batteryAsset, electricityAsset] });
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await assetTree.fillFilterInput(batteryAsset.name);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await page.click(`text=${batteryAsset.name}`);
    await expect(page.locator(`#asset-header`, { hasText: batteryAsset.name })).toBeVisible();
});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab
 * @and Searching for the asset by ID
 * @and Selecting the asset from the list
 * @then The asset detail page is displayed
 */
test(`Search by Asset ID and select the battery asset`, async ({ page, manager, assetTree, assetsPage }) => {
    const assets = [batteryAsset, electricityAsset];
    await manager.setup("smartcity", { assets: [batteryAsset, electricityAsset] });
    const id = manager.assets.find(asset => asset.name === batteryAsset.name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await assetTree.fillFilterInput(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(assets.length);
    await page.click(`text=${batteryAsset.name}`);
    await expect(page.locator(`#asset-header`, { hasText: batteryAsset.name })).toBeVisible();
})

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "/assets/false/{id}" page directly
 * @then The filter should contain the ID as input
 * @and The asset list should contain the asset with the given ID
 * @and The asset detail page is displayed
 */
test(`Open browser tab directly to the battery asset`, async ({ page, manager, assetsPage, assetTree }) => {
    const assets = [batteryAsset, electricityAsset];
    await manager.setup("smartcity", { assets: [batteryAsset, electricityAsset] });
    const id = manager.assets.find(asset => asset.name === batteryAsset.name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.gotoAssetId("smartcity", id!);
    await expect(assetTree.getFilterInput()).toHaveValue(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(assets.length);
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(page.locator(`#asset-header`, { hasText: batteryAsset.name })).toBeVisible();
})

// TODO: Add test for the "Load more" button, by modifying the LIMIT variable in or-asset-tree
