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
import { css, html, TemplateResult, PropertyValues } from "lit";
import { customElement, state } from "lit/decorators.js";
import { Page, PageProvider, router } from "@openremote/or-app";
import { AppStateKeyed } from "@openremote/or-app";
import { createSelector, Store } from "@reduxjs/toolkit";
import { manager } from "@openremote/core";
import "@openremote/or-services";
import { Microservice, MicroserviceEvent, MicroserviceEventCause, MicroserviceStatus } from "@openremote/model";
import { getServicesRoute } from "../routes";

import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";

/**
 * Consolidate services by serviceId, preferring AVAILABLE over UNAVAILABLE
 * @param services - Array of services to consolidate
 * @returns Consolidated array with unique serviceIds
 */
function consolidateServices(services: Microservice[]): Microservice[] {
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

export function pageServicesProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "services",
        routes: ["services", "services/:serviceName"],
        pageCreator: () => {
            const page = new PageServices(store);
            return page;
        },
    };
}

const serviceStyles = css`
    :host {
        overflow: hidden;
    }

    #services {
        z-index: 0;
        background: transparent;
    }
`;

@customElement("page-services")
export class PageServices extends Page<AppStateKeyed> {
    static get styles() {
        return [serviceStyles];
    }

    get name(): string {
        return "services.title";
    }

    @state()
    protected serviceId: string | null = null;

    @state()
    protected realmName: string = "";

    @state()
    protected services: Microservice[] = [];

    @state()
    protected selectedService: Microservice | null = null;

    @state()
    protected _loading = false;

    protected _eventSubscriptionId?: string;

    constructor(store: Store<AppStateKeyed>) {
        super(store);
        this.realmName = manager.displayRealm;
    }

    connectedCallback(): void {
        super.connectedCallback();
        this._subscribeEvents();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._unsubscribeEvents();
    }

    protected async _subscribeEvents() {
        if (manager.events) {
            this._eventSubscriptionId = await manager.events.subscribe<MicroserviceEvent>(
                {
                    eventType: "microservice",
                },
                (ev) => this._onEvent(ev)
            );
        }
    }

    protected _unsubscribeEvents() {
        if (this._eventSubscriptionId) {
            manager.events?.unsubscribe(this._eventSubscriptionId);
        }
    }

    protected _onEvent(event: MicroserviceEvent) {
        if (!event.microservice || !event.cause) {
            return;
        }

        // Super users receive all events, so we need to filter them out if they are not for the display realm or global
        if (manager.isSuperUser && event.microservice.realm !== this.realmName && !event.microservice.isGlobal) {
            return;
        }

        // Handle the event based on the cause
        switch (event.cause) {
            case MicroserviceEventCause.REGISTER: {
                this._onServiceRegistered(event);
                break;
            }
            case MicroserviceEventCause.UPDATE: {
                this._onServiceUpdated(event);
                break;
            }
            case MicroserviceEventCause.DEREGISTER: {
                this._onServiceDeregistered(event);
                break;
            }
            default:
                break;
        }

        this.services = consolidateServices(this.services);
    }

    protected _onServiceRegistered(event: MicroserviceEvent): void {
        this.services.push(event.microservice);

        // If the pushed service is the same as the selected service.serviceId and the current selected service is unvailable, refresh the iframe
        if (
            this._eventIsForSelectedServiceId(event) &&
            this.selectedService?.status === MicroserviceStatus.UNAVAILABLE
        ) {
            this.selectedService = event.microservice;
            this._refreshIframe();
        }
    }

    protected _onServiceUpdated(event: MicroserviceEvent): void {
        const existingServiceInstance = this.services.find(
            (service) =>
                service.serviceId === event.microservice?.serviceId &&
                service.instanceId === event.microservice?.instanceId
        );

        // Overwrite the existing service with the updated service object
        if (existingServiceInstance) {
            Object.assign(existingServiceInstance, event.microservice);
        }

        // If the selected service instance is the one that was updated, and the status has changed, refresh the iframe
        if (
            this._eventIsForSelectedServiceInstance(event) &&
            this.selectedService?.status !== event.microservice?.status
        ) {
            this.selectedService = event.microservice;
            this._refreshIframe();
            return;
        }

        // If the selected serviceId is the same as the event serviceId and the current selected status is unavailable, refresh the iframe
        if (
            this._eventIsForSelectedServiceId(event) &&
            this.selectedService?.status === MicroserviceStatus.UNAVAILABLE
        ) {
            this.selectedService = event.microservice;
            this._refreshIframe();
        }
    }

    protected _onServiceDeregistered(event: MicroserviceEvent): void {
        // Filter out the service instance that was deregistered
        this.services = this.services.filter(
            (service) =>
                service.serviceId !== event.microservice?.serviceId &&
                service.instanceId !== event.microservice?.instanceId
        );

        // If the selected service instance is the one that was deregistered, set the selected service to null
        if (this._eventIsForSelectedServiceInstance(event)) {
            this.selectedService = null;
        }
    }

    protected _eventIsForSelectedServiceInstance(event: MicroserviceEvent): boolean {
        return (
            this.selectedService?.serviceId === event.microservice?.serviceId &&
            this.selectedService?.instanceId === event.microservice?.instanceId
        );
    }

    protected _eventIsForSelectedServiceId(event: MicroserviceEvent): boolean {
        return this.selectedService?.serviceId === event.microservice?.serviceId;
    }

    protected async _loadData(silent = false): Promise<void> {
        if (!silent) {
            this._loading = true;
        }

        try {
            const realmServices = await manager.rest.api.MicroserviceResource.getServices({ realm: this.realmName });
            const globalServices = await manager.rest.api.MicroserviceResource.getGlobalServices();

            if (realmServices.status === 200 && globalServices.status === 200) {
                this.services = consolidateServices([...realmServices.data, ...globalServices.data]);
                this.requestUpdate();

                // Re-select service if necessary after loading services
                if (this.serviceId && !this.selectedService) {
                    const service = this.services.find((service) => service.serviceId === this.serviceId);
                    if (service) {
                        this.selectedService = service;
                    }
                }
            }
        } catch (error) {
            console.error("Failed to load services:", error);
            showSnackbar(undefined, i18next.t("services.servicesLoadError"));
        } finally {
            this._loading = false;
        }
    }

    // Reload the current service iframe
    protected _refreshIframe(): void {
        const servicesComponent = this.shadowRoot?.querySelector("or-services") as any;
        if (servicesComponent && typeof servicesComponent.refreshIframe === "function") {
            servicesComponent.refreshIframe();
        } else {
            console.warn("Unable to reload iframe: services component not found or refreshIframe method not available");
        }
    }

    public stateChanged(state: AppStateKeyed): void {
        this.getRealmState(state);

        // Update serviceId from URL params - this will trigger updated() which handles the selection
        const newServiceId = state.app.params?.serviceName;
        if (newServiceId !== this.serviceId) {
            this.serviceId = newServiceId;
        }
    }

    protected realmSelector = (state: AppStateKeyed) => state.app.realm || manager.config.realm;

    protected getRealmState = createSelector([this.realmSelector], (realm: string) => {
        this.realmName = realm;
    });

    protected _onServiceSelected(e: CustomEvent): void {
        const service = e.detail as Microservice;

        if (service && service.serviceId !== this.serviceId) {
            this.serviceId = service.serviceId;
            this.selectedService = service;
            this._updateRoute();
        }
    }

    protected willUpdate(changedProperties: PropertyValues) {
        super.willUpdate(changedProperties);

        if (changedProperties.has("serviceId")) {
            if (this.serviceId) {
                this._updateServiceSelection();
            }
        }

        if (changedProperties.has("realmName")) {
            this._loadData();
        }
    }

    protected _updateRoute() {
        router.navigate(getServicesRoute(this.serviceId));
    }

    protected _updateServiceSelection() {
        if (this.serviceId) {
            const service = this.services.find((service) => service.serviceId === this.serviceId);
            if (service) {
                this.selectedService = service;
            }
        } else {
            this.selectedService = null;
        }
    }

    protected render(): TemplateResult {
        return html`
            <div style="width: 100%;">
                <or-services
                    id="services"
                    .realmName="${this.realmName}"
                    .services="${this.services}"
                    .selectedService="${this.selectedService}"
                    .loading="${this._loading}"
                    @or-service-selected="${this._onServiceSelected}"
                ></or-services>
            </div>
        `;
    }
}
