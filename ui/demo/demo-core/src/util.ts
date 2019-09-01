import {Asset, AssetQuery, AssetQueryMatch, AssetQueryInclude} from "@openremote/model";
import rest from "@openremote/rest";

export async function getApartment1Asset(): Promise<Asset | undefined> {
    const query: AssetQuery = {
        name: {
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "Apartment 1"
        },
        type: {
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "urn:openremote:asset:residence"
        },
        select: {
            include: AssetQueryInclude.ONLY_ID_AND_NAME
        }
    };

    const response = await rest.api.AssetResource.queryAssets(query);
    const assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the 'Apartment 1' asset");
        return;
    }
    return assets[0];
}