import { Microservice, MicroserviceStatus } from "@openremote/model";

/**
 * Consolidate services by serviceId, preferring AVAILABLE over UNAVAILABLE
 * @param services - Array of services to consolidate
 * @returns Consolidated array with unique serviceIds
 */
export function consolidateServices(services: Microservice[]): Microservice[] {
  return Object.values(
    services.reduce((acc, service) => {
      const serviceId = service.serviceId || "";
      const existing = acc[serviceId];
      if (!existing || (service.status === MicroserviceStatus.AVAILABLE && existing.status !== MicroserviceStatus.AVAILABLE)) {
        acc[serviceId] = service;
      }
      return acc;
    }, {} as Record<string, Microservice>)
  );
}

/**
 * Get the iframe path for a given service
 * @param service - The service to get the iframe path for
 * @param realmName - The realm name
 * @param isSuperUser - Whether the user is a super user
 * @returns The iframe path
 */
export function getServiceUrlPath(service: Microservice, realmName: string, isSuperUser: boolean): string {
  // Replace {realm} param if provided, uses query param if not super user
  const homepageUrl = service.homepageUrl || "";
  return homepageUrl.replace("{realm}", isSuperUser ? realmName : `?realm=${realmName}`);
} 