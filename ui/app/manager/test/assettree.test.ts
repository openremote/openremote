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
import {adminStatePath, Manager, test, userStatePath} from "./fixtures/manager.js";
import {batteryAsset, buildingAsset, electricityAsset, parentAssets} from "./fixtures/data/assettree.js";
import {Asset} from "@openremote/model";
import type {OrAssetTree} from "@openremote/or-asset-tree";

test.use({ storageState: userStatePath });

function createBatteryAssets(amount: number, realm = "smartcity"): Asset[] {
    return Array.from({ length: amount }, (_, i) => ({
        ...batteryAsset,
        name: `Battery ${i + 1}`,
        realm: realm
    }));
}

function createElectricityAssets(amount: number, realm = "smartcity"): Asset[] {
    return Array.from({ length: amount }, (_, i) => ({
        ...electricityAsset,
        name: `Electricity meter ${i + 1}`,
        realm: realm
    }));
}

function createBuildingAssets(amount: number, realm = "smartcity"): Asset[] {
    return Array.from({ length: amount }, (_, i) => ({
        ...buildingAsset,
        name: `Building ${i + 1}`,
        realm: realm
    }));
}

function createComplexTree(): Asset[] {
    const [cityAsset1, cityAsset2] = parentAssets;
    const buildingAssets = [cityAsset1, cityAsset2].flatMap(city =>
        createBuildingAssets(3).map(building => ({
            ...building, parentId: city.id
        }))
    );
    const batteryAssets = buildingAssets.flatMap(building =>
        createBatteryAssets(5).map(battery => ({
            ...battery, parentId: building.id
        }))
    );
    return [cityAsset1, cityAsset2, ...buildingAssets, ...batteryAssets];

}

// Utility function to create parent assets, and apply assets as children
async function applyParentAssets(parentAssets: Asset[], manager: Manager) {
    for (const p of parentAssets) {
        await manager.createAsset(p);
    }
    const cityIds = manager.assets.filter(a => a.type === "CityAsset").sort((a, b) => a.name!.localeCompare(b.name!)).map(a => a.id);
    const childAssets = manager.assets.filter(a => a.type !== "CityAsset");
    childAssets.forEach(((ca, i) => ca.parentId = cityIds[Math.floor(i / (childAssets.length / cityIds.length))]));
    for (const a of childAssets) {
        await manager.updateAsset(a);
    }
}

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to OpenRemote "smartcity" realm as admin
 * @and Navigating to the "Assets" tab
 * @and Tries to navigate through the assets in the system using the asset tree
 * @then The asset list should be complete and should not display any artifacts or visual errors.
 */
test(`Check if assets are visible in the tree`, async ({ assetTree, manager, assetsPage, page }) => {

    // Make sure the count endpoint during page load returns a correct value
    await page.route("**/asset/count", async (route, request) => {
        await route.continue();
        const response = await request.response();
        expect(response?.status()).toBe(200);
        expect(await response?.json()).toBe(9);
    });

    const batteryAssets = createBatteryAssets(2);
    const electricityAssets = createElectricityAssets(2)
    await manager.setup("smartcity", { assets: [...batteryAssets, ...electricityAssets] });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();

    // Check if City Asset 1, City Asset 2 and Consoles group asset are listed.
    await expect(assetTree.getAssetNodes()).toHaveCount(3);
    await expect(assetTree.getAssetNodes().nth(0)).toContainText(parentAssets[0].name!);
    await expect(assetTree.getAssetNodes().nth(1)).toContainText(parentAssets[1].name!);
    await expect(assetTree.getAssetNodes().nth(2)).toContainText('Consoles');
    await expect(assetTree.getAssetNodes().locator('[data-expandable]')).toHaveCount(3);

    // Check if expandable of City Asset 1 is correct.
    const cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2);
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveText(batteryAssets.map(a => a.name!));

    // Check if expandable of City Asset 2 is correct.
    const cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 2 + 2);
    await expect(assetTree.getChildNodes(cityAsset2)).toHaveText(electricityAssets.map(a => a.name!));

    // Check if expandable of Consoles group asset is correct.
    const consoleAsset = assetTree.getAssetNodes().filter({ hasText: 'Consoles' });
    await consoleAsset.locator('[data-expandable]').click();
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
test(`Search for and select the battery asset`, async ({ page, manager, assetTree, assetViewer, assetsPage }) => {
    await manager.setup("smartcity", { assets: [batteryAsset, electricityAsset] });
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await assetTree.getFilterInput().fill(batteryAsset.name);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await page.click(`text=${batteryAsset.name}`);
    await expect(assetViewer.getHeaderLocator(batteryAsset.name)).toBeVisible();
});

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab
 * @and Searching for the asset by ID
 * @and Selecting the asset from the list
 * @then The asset detail page is displayed
 */
test(`Search by Asset ID and select the battery asset`, async ({ page, manager, assetTree, assetViewer, assetsPage }) => {
    const assets = [batteryAsset, electricityAsset];
    await manager.setup("smartcity", { assets: assets });
    const id = manager.assets.find(asset => asset.name === batteryAsset.name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await manager.navigateToTab("asset");
    await assetTree.getFilterInput().fill(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(1);
    await page.click(`text=${batteryAsset.name}`);
    await expect(assetViewer.getHeaderLocator(batteryAsset.name)).toBeVisible();
})

/**
 * @given Assets are set up in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "/assets/false/{id}" page directly
 * @then The filter should contain the ID as input
 * @and The asset list should contain the asset with the given ID
 * @and The asset detail page is displayed
 */
test(`Open browser tab directly to the battery asset`, async ({ page, manager, assetsPage, assetTree, assetViewer }) => {
    const batteryAssets = createBatteryAssets(10);
    const electricityAssets = createElectricityAssets(10);
    const assets = [...batteryAssets, ...electricityAssets];
    await manager.setup("smartcity", { assets: assets });
    await applyParentAssets(parentAssets, manager);
    const id = manager.assets.find(asset => asset.name === batteryAssets[0].name)?.id;
    expect(id).toBeDefined();
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.gotoAssetId("smartcity", id!);
    await expect(assetTree.getFilterInput()).toHaveValue(id!);
    await expect(assetTree.getAssetNodes()).toHaveCount(2); // The parent city asset + selected battery asset
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(assetViewer.getHeaderLocator(batteryAsset.name)).toBeVisible();
    expect(page.url()).toContain(id!);
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
    await expect(assetTree.getAssetNodes()).toHaveCount(5); // 2 battery assets + 2 electricity assets + 1 console group
    await page.locator('or-asset-tree').evaluate(tree => {
        (tree as OrAssetTree).setAttribute('queryLimit', '2');
        (tree as OrAssetTree).setAttribute('paginationThreshold', '1');
    });
    await expect(assetTree.getAssetNodes()).toHaveCount(2);
    await page.getByRole('button', { name: "Load More" }).click();
    await expect(assetTree.getAssetNodes()).toHaveCount(4);
    await page.getByRole('button', { name: "Load More" }).click();
    await expect(assetTree.getAssetNodes()).toHaveCount(5);
})

/**
 * @given 50 assets are loaded in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab
 * @and Pressing "load more" buttons within each parent
 * @then The correct amount of children should be displayed
 * @and The scroll position should be kept consistent, without jumping back and forth.
 */
test(`Load more buttons are shown properly without any unexpected scroll behavior`, async ({page, manager, assetsPage, assetTree}) => {
    const batteryAssets = createBatteryAssets(50);
    await manager.setup("smartcity", { assets: batteryAssets });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();

    // Test if expanding the node works correctly
    let cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 25); // City 1, City 2, Consoles asset and 25 battery assets

    // Limit the number of expanding nodes to 20, and check if the tree is still in tact
    await page.locator('or-asset-tree').evaluate(tree => {
        (tree as OrAssetTree).setAttribute('queryLimit', '20');
        (tree as OrAssetTree).setAttribute('paginationThreshold', '1');
    });
    await expect(assetTree.getAssetNodes()).toHaveCount(3); // City 1, City 2 and Consoles asset
    cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 20); // City 1, City 2, Consoles asset and 20 out of 25 battery assets

    // Scroll down to the "load more" botton, and check if the scroll position is kept after pressing it.
    const listContainer = page.locator('or-asset-tree #list-container');
    await cityAsset1.getByRole('button', { name: "Load More" }).click();
    const scrollPos1 = await listContainer.evaluate(el => el.scrollTop);
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 25); // Now it's 25 out of 25 assets again
    expect(await listContainer.evaluate(el => el.scrollTop)).toStrictEqual(scrollPos1);

    // Now click the 2nd city, scroll down to their "load more" button, and do the same.
    let cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 25 + 20); // City 2 now has 20 out of 25 assets
    await cityAsset2.getByRole('button', { name: "Load More" }).click();
    const scrollPos2 = await listContainer.evaluate(el => el.scrollTop);
    await expect(assetTree.getAssetNodes()).toHaveCount(3 + 25 + 25); // Now it's 25 out of 25 assets again
    expect(await listContainer.evaluate(el => el.scrollTop)).toStrictEqual(scrollPos2);
})

/**
 * @given 50 assets are loaded in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Navigating to the "asset" tab
 * @and Pressing "load more" buttons within each parent
 * @then The correct children are displayed, and the correct asset is selected
 */
test(`Load more buttons are shown properly when there is a complex tree`, async ({page, manager, assetsPage, assetTree, assetViewer}) => {
    await manager.setup("smartcity", { assets: parentAssets });
    const cityAssets = manager.assets.filter(a => a.type === "CityAsset");
    expect(cityAssets.length).toBe(2);
    expect(cityAssets[0].id).toBeDefined();
    expect(cityAssets[1].id).toBeDefined();

    // Beforehand, create buildings for each city
    let buildingAssets = cityAssets.flatMap(city =>
        createBuildingAssets(3).map(building => ({
            ...building, parentId: city.id
        }))
    ) as Asset[];
    for(const building of buildingAssets) {
        await manager.createAsset(building);
    }
    buildingAssets = manager.assets.filter(a => a.type === "BuildingAsset");
    expect(buildingAssets.length).toBe(2 * 3);

    // Beforehand, create batteries for each building
    let batteryAssets = buildingAssets.flatMap(building =>
        createBatteryAssets(5).map(battery => ({
            ...battery, parentId: building.id
        }))
    ) as Asset[];
    for(const battery of batteryAssets) {
        await manager.createAsset(battery);
    }
    batteryAssets = manager.assets.filter(a => a.name?.includes("Battery"));
    expect(batteryAssets.length).toBe(2 * 3 * 5);

    // Navigate to the asset tree
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();
    await page.locator('or-asset-tree').evaluate(tree => {
        (tree as OrAssetTree).setAttribute('queryLimit', '2');
        (tree as OrAssetTree).setAttribute('paginationThreshold', '1')
    });
    await expect(assetTree.getAssetNodes()).toHaveCount(2); // 2 parent assets (the other console group is hidden because of queryLimit=2)

    // Navigate to the 1st building within the first city
    let cityAsset1 = assetTree.getAssetNodes().filter({ hasText: parentAssets[0].name });
    await cityAsset1.locator('[data-expandable]').click();
    await expect(assetTree.getAssetNodes()).toHaveCount(2 + 2); // City 1, City 2, and 2 out of 3 building assets
    await page.click(`text=${buildingAssets[0].name}`); // Clicking "Building 1" within the 1st city
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(assetTree.getSelectedNodes()).toHaveText(buildingAssets[0].name!);
    await expect(assetViewer.getHeaderLocator(buildingAssets[0].name)).toBeVisible();

    // Select the 1st building of the city
    await expect(page.getByRole('button', { name: 'Load More' })).toHaveCount(2);
    await page.getByRole('button', { name: 'Load More' }).first().click();
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(assetTree.getSelectedNodes()).toHaveText(buildingAssets[0].name!);
    await expect(assetViewer.getHeaderLocator(buildingAssets[0].name)).toBeVisible();
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
    await cityAsset1.locator('[data-expandable]').click();
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveCount(assets.length / 2);
    const cityAsset2 = assetTree.getAssetNodes().filter({ hasText: parentAssets[1].name });
    await cityAsset2.locator('[data-expandable]').click();
    await expect(assetTree.getChildNodes(cityAsset2)).toHaveCount(assets.length / 2);
    await expect(assetTree.getAssetNodes()).toHaveCount(1 + 10 + 1 + 10 + 1);

    // Delete battery asset 5 of the first city, and check if state is the same
    await assetsPage.deleteSelectedAsset(manager, batteryAssets[4].name!);
    await expect(assetTree.getSelectedNodes()).toHaveCount(0);
    await expect(assetTree.getChildNodes(cityAsset1)).toHaveCount(assets.length / 2 - 1);
    await expect(assetTree.getChildNodes(cityAsset2)).toHaveCount(assets.length / 2);
    await expect(assetTree.getAssetNodes()).toHaveCount(1 + 9 + 1 + 10 + 1);
    await expect(page.locator('or-asset-tree [data-expandable]')).toHaveCount(3);
    await expect(page.locator('or-asset-tree [data-expanded] [data-expandable]')).toHaveCount(2);
    await expect(page.locator('or-asset-viewer', { hasText: "Please select an asset on the left"})).toBeVisible();
})

/**
 * @given 20 assets are loaded in the "smartcity" realm
 * @when Logging in to the OpenRemote "smartcity" realm
 * @and Searching for an asset using the filter
 * @and The asset is appearing in the list as expected
 * @and The asset is selected
 * @then Attempting to remove the asset from the list succeeds
 * @and the asset list should be updated, but kept in the same state without visual artifacts
 */
test(`Searching for an asset and removing it keeps the tree and viewer in tact`, async ({page, manager, assetsPage, assetTree, assetViewer}) => {
    const batteryAssets = createBatteryAssets(10);
    const electricityAssets = createElectricityAssets(10);
    await manager.setup("smartcity", { assets: [...batteryAssets, ...electricityAssets] });
    await applyParentAssets(parentAssets, manager);
    await manager.goToRealmStartPage("smartcity");
    await assetsPage.goto();

    // Fill in "Battery 10", and expect that single asset + parent to show up
    const battery10 = batteryAssets[batteryAssets.length - 1];
    await expect(assetTree.getAssetNodes()).toHaveCount(3); // 2 parent assets + 1 console group
    await assetTree.getFilterInput().fill(battery10.name!);
    await expect(assetTree.getAssetNodes()).toContainText([parentAssets[0].name!, battery10.name!]);
    await expect(assetTree.getAssetNodes()).toHaveCount(2); // Parent asset + child battery asset
    await page.click(`text=${battery10.name}`);
    await expect(assetTree.getSelectedNodes()).toHaveCount(1);
    await expect(assetViewer.getHeaderLocator(battery10.name!)).toBeVisible();

    // Fill in "Electricity meter 1", and expect that single asset + "Electricity meter 10", and their parent to show up"
    const meter1 = electricityAssets[0];
    const meter10 = electricityAssets[electricityAssets.length - 1];
    await assetTree.getFilterInput().fill(meter1.name!);
    await expect(assetTree.getAssetNodes()).toContainText([parentAssets[1].name!, meter1.name!]);
    await expect(assetTree.getAssetNodes()).toHaveCount(2 + 1); // Parent asset + the two electricity meter assets

    // Attempt to select and delete the "Electricity meter 1"
    await assetsPage.deleteSelectedAsset(manager, meter1.name!);
    await expect(assetTree.getSelectedNodes()).toHaveCount(1); // It automatically selects the other asset, "Electricity meter 10"
    await expect(assetTree.getAssetNodes()).toContainText([parentAssets[1].name!, meter10.name!]);
    await expect(assetTree.getAssetNodes()).toHaveCount(1 + 1); // Only parent + "Electricity meter 10" are visible now.

    // We now clear the filter, and try to delete "Battery 10" from earlier as well
    await assetTree.getFilterInput().fill(battery10.name!);
    await expect(assetTree.getAssetNodes()).toContainText([parentAssets[0].name!, battery10.name!]);
    await expect(assetTree.getAssetNodes()).toHaveCount(2); // Parent asset + child electricity asset
    await assetsPage.deleteSelectedAsset(manager, battery10.name!, assetTree.getSelectedNodes());
    await expect(assetTree.getSelectedNodes()).toHaveCount(0);
    await expect(assetTree.getAssetNodes()).toHaveCount(0); // Nothing is visible anymore, since there is nothing matching the "Battery 10" text filter.
})

/**
 * @given 4 assets are created in the "master" realm
 * @and 2 assets are created in the "smartcity" realm
 * @and the assets are visible in the tree (a total of 5)
 * @when the user switches to the "smartcity" realm using the realm picker
 * @then the asset tree should show assets from the "smartcity" realm instead, (a total of 3)
 * @and the asset viewer becomes empty, and doesn't show the old asset from the "master" realm anymore
 */
test.describe(() => {
    test.use({ storageState: adminStatePath });

    test(`Selecting an asset, clears the asset viewer when switching realms`, async ({page, manager, assetsPage, assetTree, assetViewer}) => {
        const batteryAssets = createBatteryAssets(2, "master");
        const electricityAssets = createElectricityAssets(2, "master");
        await manager.setup("master", { assets: [...batteryAssets, ...electricityAssets] });
        await manager.goToRealmStartPage("master");
        await assetsPage.goto();
        await expect(assetTree.getAssetNodes()).toHaveCount(5); // 2 battery assets + 2 electricity assets + 1 console group

        // Select asset
        await page.click(`text=${electricityAssets[0].name}`); // Clicking "Electricity asset 1"
        await expect(assetTree.getSelectedNodes()).toHaveCount(1);
        await expect(assetViewer.getHeaderLocator(electricityAssets[0].name!)).toBeVisible();

        // Create assets for another realm
        const smartCityAssets = createBatteryAssets(2);
        await manager.setup("smartcity", { assets: smartCityAssets });

        // Switch realms and expect assets to be visible
        await manager.switchToRealmByRealmPicker("smartcity");
        await expect(assetTree.getAssetNodes()).toHaveCount(3); // 2 battery assets + 1 console group
        await expect(assetViewer.getHeaderLocator(electricityAssets[0].name!)).not.toBeVisible();
    });
});

// After each test, clean up all data
test.afterEach(async ({ manager }) => {
    await manager.cleanUp();
});
