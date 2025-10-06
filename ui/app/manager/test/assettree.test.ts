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
import type {OrAssetTree} from "@openremote/or-asset-tree";

test.use({ storageState: userStatePath });

// After each test, clean up all data
test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});

function createBatteryAssets(amount: number) {
    return Array.from({ length: amount }, (_, i) => ({
        ...batteryAsset,
        name: `Battery ${i + 1}`
    }));
}

function createElectricityAssets(amount: number) {
    return Array.from({ length: amount }, (_, i) => ({
        ...electricityAsset,
        name: `Electricity meter ${i + 1}`
    }));
}

// Utility function to create parent assets, and apply assets as children
async function applyParentAssets(parentAssets: Asset[], manager: Manager) {
    await Promise.all(parentAssets.map(a => manager.createAsset(a)));
    const cityIds = manager.assets.filter(a => a.type === "CityAsset").sort((a, b) => a.name!.localeCompare(b.name!)).map(a => a.id);
    const childAssets = manager.assets.filter(a => a.type !== "CityAsset");
    childAssets.forEach(((ca, i) => ca.parentId = cityIds[Math.floor(i / (childAssets.length / cityIds.length))]));
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
    await manager.setup("smartcity", { assets: [...createBatteryAssets(2), ...createElectricityAssets(2)] });
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
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2 + 2);

    // Check if expandable of Consoles group asset is correct.
    const consoleAsset = assetTree.getAssetNodes().filter({ hasText: 'Consoles' });
    await consoleAsset.locator('.expander').click();
    await expect.poll(async () => await assetTree.getAssetNodes().count(), {
        message: "Waiting for the Console assets to appear..."
    }).toBeGreaterThanOrEqual(3 + 2 + 2 + 1); // (there is at least 1 console, but could be more with a larger test suite)
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
    await manager.setup("smartcity", { assets: assets });
    const id = manager.assets.find(asset => asset.name === batteryAsset.name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await assetTree.fillFilterInput(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
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
    await manager.setup("smartcity", { assets: assets });
    const id = manager.assets.find(asset => asset.name === batteryAsset.name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.gotoAssetId("smartcity", id!);
    await expect(assetTree.getFilterInput()).toHaveValue(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(page.locator(`#asset-header`, { hasText: batteryAsset.name })).toBeVisible();
})

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab, while the QUERY_LIMIT is set to 2
 * @then the other assets should not be visible
 * @and a "Load More" button has to be displayed
 * @when the "Load More" button is clicked
 * @then the other assets should be visible
 */
test(`Load more buttons are shown when there are a lot of assets`, async ({ page, manager, assetsPage, assetTree }) => {
    const assets = [batteryAsset, batteryAsset, electricityAsset, electricityAsset];
    await manager.setup("smartcity", { assets: assets });
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await expect(assetTree.getAssetNodes()).toHaveCount(5);
    await page.locator('or-asset-tree').evaluate(tree => {(tree as OrAssetTree).setAttribute('queryLimit', '2')});
    await expect(assetTree.getAssetNodes()).toHaveCount(2);
    await page.locator('.loadmore-element or-mwc-input').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(4);
    await page.locator('.loadmore-element or-mwc-input').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(5);
})

/**
 * @given 20 assets are loaded in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Selecting an asset from the list
 * @and Attempting to delete the asset
 * @then The asset should be deleted
 * @and The asset list should be updated, but kept in the same state without visual artifacts
 * @and The asset detail page should be closed
 */
test(`Deleting an asset properly keeps the tree and viewer in tact`, async ({ page, manager, assetsPage, assetTree }) => {
    const batteryAssets = createBatteryAssets(10);
    const electricityAssets = createElectricityAssets(10);
    const assets = [...batteryAssets, ...electricityAssets];
    await manager.setup("smartcity", { assets: assets });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await expect(assetTree.getAssetNodes()).toHaveCount(3); // 2 parent assets + 1 console group

    // Expand both City Asset 1 and City Asset 2
    const cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('.expander').click();
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveCount(assets.length / 2);
    const cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('.expander').click();
    await expect(assetTree.getChildNodes(cityAsset2)).toHaveCount(assets.length / 2);
    await expect(assetTree.getAssetNodes()).toHaveCount(1 + 10 + 1 + 10 + 1);

    // Delete battery asset 5 of the first city, and check if state is the same
    await assetsPage.deleteSelectedAsset(batteryAssets[4].name);
    await expect(assetTree.getSelectedNodes()).toHaveCount(0);
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveCount(assets.length / 2 - 1);
    await expect(assetTree.getChildNodes(cityAsset2)).toHaveCount(assets.length / 2);
    await expect(assetTree.getAssetNodes()).toHaveCount(1 + 9 + 1 + 10 + 1);
    await expect(page.locator('or-asset-tree li.asset-list-element .expander[data-expandable]')).toHaveCount(3);
    await expect(page.locator('or-asset-tree li.asset-list-element[data-expanded] .expander[data-expandable]')).toHaveCount(2);
    await expect(page.locator('or-asset-viewer or-translate')).toHaveAttribute('value', 'noAssetSelected');
})

/**
 * @given 20 assets are loaded in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Selecting an asset from the list
 */
/*test(`Searching for an asset and removing it keeps the tree and viewer in tact`, async ({page, manager, assetsPage, assetTree}) => {
    const batteryAssets = createBatteryAssets(10);
    const electricityAssets = createElectricityAssets(10);
    const assets = [...batteryAssets, ...electricityAssets];
    await manager.setup("smartcity", { assets: assets });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await expect(assetTree.getAssetNodes()).toHaveCount(3); // 2 parent assets + 1 console group
})*/
