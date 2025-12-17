/*
 * Copyright 2025, OpenRemote Inc.
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
import { ExternalService } from "@openremote/model";



/**
 * Get the iframe src path for a given service
 * @param service - The service to get the iframe src path for
 * @param realmName - The realm name
 * @param isSuperUser - Whether the user is a super user
 * @returns The iframe src path
 */
export function getServiceUrlPath(service: ExternalService, realmName: string, isSuperUser: boolean): string {
    // Replace {realm} param if provided, uses query param if not super user
    const homepageUrl = service.homepageUrl || "";
    return homepageUrl.replace("{realm}", isSuperUser ? realmName : `?realm=${realmName}`);
}
