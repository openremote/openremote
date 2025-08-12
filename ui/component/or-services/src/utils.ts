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
import { Microservice, MicroserviceStatus } from "@openremote/model";

/**
 * Consolidate services by serviceId, preferring AVAILABLE over UNAVAILABLE.
 *
 * This is used to ensure that only one service is displayed for a given serviceId.
 *
 * @param services - Array of services to consolidate
 * @returns Consolidated array with unique serviceIds
 */
export function consolidateServices(services: Microservice[]): Microservice[] {
    return Object.values(
        services.reduce((acc, service) => {
            const serviceId = service.serviceId || "";
            const existing = acc[serviceId];
            if (
                !existing ||
                (service.status === MicroserviceStatus.AVAILABLE && existing.status !== MicroserviceStatus.AVAILABLE)
            ) {
                acc[serviceId] = service;
            }
            return acc;
        }, {} as Record<string, Microservice>)
    );
}

/**
 * Get the iframe src path for a given service
 * @param service - The service to get the iframe src path for
 * @param realmName - The realm name
 * @param isSuperUser - Whether the user is a super user
 * @returns The iframe src path
 */
export function getServiceUrlPath(service: Microservice, realmName: string, isSuperUser: boolean): string {
    // Replace {realm} param if provided, uses query param if not super user
    const homepageUrl = service.homepageUrl || "";
    return homepageUrl.replace("{realm}", isSuperUser ? realmName : `?realm=${realmName}`);
}
