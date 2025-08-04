/* eslint-disable import/no-duplicates */
import { css, html, TemplateResult, PropertyValues } from "lit";
import { customElement, state } from "lit/decorators.js";
import { Page, PageProvider, router } from "@openremote/or-app";
import { AppStateKeyed } from "@openremote/or-app";
import { createSelector, Store } from "@reduxjs/toolkit";
import { manager } from "@openremote/core";
import "@openremote/or-services";
import { Microservice } from "@openremote/model";
import { getServicesRoute } from "../routes";

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
    return "services";
  }

  @state()
  private serviceId: string | null = null;

  @state()
  protected realmName: string = "";

  constructor(store: Store<AppStateKeyed>) {
    super(store);
    this.realmName = manager.displayRealm;
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
    const service = e.detail.service as Microservice;

    if (service && service.serviceId !== this.serviceId) {
      this.serviceId = service.serviceId;
      this._updateRoute();
    }
  }

  protected updated(changedProperties: PropertyValues) {
    super.updated(changedProperties);

    if (changedProperties.has("serviceId")) {
      if (this.serviceId) {
        this._updateServiceSelection();
      }
    }
  }

  protected _updateRoute() {
    router.navigate(getServicesRoute(this.serviceId));
  }

  protected _updateServiceSelection() {
    const servicesComponent = this.shadowRoot?.querySelector("or-services") as any;
    if (servicesComponent && servicesComponent.setServiceName) {
      servicesComponent.setServiceName(this.serviceId);
    }
  }

  protected render(): TemplateResult {
    return html`
      <div style="width: 100%;">
        <or-services
          id="services"
          .realmName="${this.realmName}"
          @service-selected="${this._onServiceSelected}"
        ></or-services>
      </div>
    `;
  }
}
