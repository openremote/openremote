import {Asset, AssetQuery, AssetQueryMatch} from "@openremote/model";
import manager from "@openremote/core";

export async function getApartment1Asset(): Promise<Asset | undefined> {
    const query: AssetQuery = {
        names: [{
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "Apartment 1"
        }],
        types: [{
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "urn:openremote:asset:residence"
        }],
        select: {
            excludeRealm: true,
            excludePath: true,
            excludeAttributes: true,
            excludeParentInfo: true
        }
    };

    const response = await manager.rest.api.AssetResource.queryAssets(query);
    const assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the 'Apartment 1' asset");
        return;
    }
    return assets[0];
}