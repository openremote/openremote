/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
export function getMapRoute(assetId?: string) {
    let route = "map";
    if (assetId) {
        route += "/" + assetId;
    }

    return route;
}

export function getAssetsRoute(editMode?: boolean, assetIds?: string) {
    let route = "assets/" + (editMode ? "true" : "false");
    if (assetIds) {
        route += "/" + assetIds;
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
        const type = (serviceAccount ? 'serviceuser' : 'regular')
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
export function getServicesRoute(serviceId?: string) {
    let route = "services";
    if(serviceId) {
        route += "/" + serviceId;
    }
    return route;
}
