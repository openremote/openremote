import {Asset, AssetQuery, AssetQueryMatch, WellknownAssets} from "@openremote/model";
import manager from "@openremote/core";

export async function getBuildingAsset(): Promise<Asset | undefined> {
    const query: AssetQuery = {
        names: [{
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "De Rotterdam"
        }],
        types: [WellknownAssets.BUILDINGASSET],
        select: {
            attributes: []
        }
    };

    const response = await manager.rest.api.AssetResource.queryAssets(query);
    const assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the asset");
        return;
    }
    return assets[0];
}

export async function getElectricityConsumerAsset(): Promise<Asset | undefined> {
    const query: AssetQuery = {
        names: [{
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "Consumption Erasmianum"
        }],
        types: [WellknownAssets.ELECTRICITYCONSUMERASSET]
    };

    const response = await manager.rest.api.AssetResource.queryAssets(query);
    const assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the asset");
        return;
    }
    return assets[0];
}

export async function getElectricityChargerAsset(): Promise<Asset | undefined> {
    const query: AssetQuery = {
        names: [{
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "Charger 1 Markthal"
        }],
        types: [WellknownAssets.ELECTRICITYCHARGERASSET]
    };

    const response = await manager.rest.api.AssetResource.queryAssets(query);
    const assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the asset");
        return;
    }
    return assets[0];
}
