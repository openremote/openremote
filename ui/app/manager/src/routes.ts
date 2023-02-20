
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

export function getUsersRoute(userId?: string) {
    let route = "users";
    if(userId) {
        route += "/" + userId;
    }
    return route;
}
export function getNewUserRoute(serviceAccount?: boolean) {
    let route = "users";
    if(serviceAccount != undefined) {
        let type = (serviceAccount ? 'serviceuser' : 'regular')
        route += "/new/" + type;
    }
    return route
}

export function getAlarmsRoute(alarmId?: string) {
    let route = "alarms";
    if(alarmId) {
        route += "/" + alarmId;
    }
    return route;
}
