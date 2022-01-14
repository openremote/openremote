
export function getMapRoute(assetId?: string) {
    let route = "map";
    if (assetId) {
        route += "/" + assetId;
    }

    return route;
}

export function getAssetsRoute(editMode?: boolean, assetId?: string) {
    let route = "assets/" + (editMode ? "true" : "false");
    if (assetId) {
        route += "/" + assetId;
    }

    return route;
}
