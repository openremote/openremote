
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

export function getInsightsRoute(editMode?: boolean, dashboardId?: string) {
    let route = "insights/" + (editMode ? "true" : "false");
    if(dashboardId) {
        route += "/" + dashboardId;
    }
    return route;
}
