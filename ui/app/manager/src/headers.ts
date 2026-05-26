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
import { manager } from "@openremote/core";
import { AppStateKeyed, HeaderItem, OrApp } from "@openremote/or-app";
import {AnyAction} from "@reduxjs/toolkit";
import { getMapRoute } from "./routes";

export function headerItemMap<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "map",
        href: getMapRoute(),
        text: "map"
    };
}

export function headerItemAssets<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "rhombus-split",
        href: "assets",
        text: "asset_plural",
    };
}

export function headerItemServices<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "puzzle",
        href: "services",
        text: "services",
        roles: ["read:services"]
    };
}

export function headerItemRules<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "state-machine",
        href: "rules",
        text: "rule_plural",
        hideMobile: true,
        roles: () => !manager.isRestrictedUser()
    };
}

export function headerItemInsights<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "chart-areaspline",
        href: "insights",
        text: "insights"
    };
}

export function headerItemGatewayConnection<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "cloud",
        value: "gateway",
        href: "gateway",
        text: "gatewayConnection",
        roles: ["write:admin", "read:admin"]
    };
}

export function headerItemGatewayTunnel<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "lan-connect",
        value: "gateway-tunnel",
        href: "gateway-tunnel",
        text: "gatewayTunnel",
        roles: ["write:admin", "read:admin"]
    }
}

export function headerItemLanguage<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "web",
        value: "language",
        text: "language",
        action: () => {
            orApp.showLanguageModal();
        }
    };
}

export function headerItemLogout<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "logout",
        value: "logout",
        text: "logout",
        action: () => {
            orApp.logout();
        }
    };
}

export function headerItemLogs<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "text-box-search-outline",
        value: "logs",
        href: "logs",
        text: "logs",
        hideMobile: true,
        roles: ["write:logs", "read:logs"]
    };
}
export function headerItemAccount<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "account",
        value: "account",
        href: "account",
        text: "account",
        roles: ["write:user"]
    };
}
export function headerItemUsers<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "account-group",
        value: "users",
        href: "users",
        text: "user_plural",
        roles: ["read:admin", "write:admin"]
    };
}
export function headerItemRoles<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "account-box-multiple",
        value: "roles",
        href: "roles",
        text: "role_plural",
        roles: ["read:admin", "write:admin"]
    };
}
export function headerItemRealms<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "domain",
        value: "realms",
        href: "realms",
        text: "realm_plural",
        roles: () => manager.isSuperUser()
    };
}

export function headerItemExport<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "database-export",
        value: "export",
        href: "data-export",
        text: "dataExport"
    };
}

export function headerItemProvisioning<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "cellphone-cog",
        value: "provisioning",
        href: "provisioning",
        text: "autoProvisioning",
        roles: () => manager.isSuperUser()
    };
}

export function headerItemConfiguration<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "palette-outline",
        value: "configuration",
        href: "configuration",
        text: "appearance",
        roles: () => manager.isSuperUser()
    };
}
