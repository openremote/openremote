/* eslint-disable import/no-duplicates */
import { css, html, unsafeCSS, TemplateResult, PropertyValues, CSSResult } from "lit";
import { customElement, state, property } from "lit/decorators.js";
import "@openremote/or-log-viewer";
import { Page, PageProvider, router } from "@openremote/or-app";
import { AppStateKeyed } from "@openremote/or-app";
import { createSelector, Store } from "@reduxjs/toolkit";
import { DefaultColor3, DefaultColor4, DefaultColor5, DefaultColor6, manager } from "@openremote/core";
import { style as OrAssetTreeStyle } from "@openremote/or-asset-tree";
import "@openremote/or-components/or-iframe";
import "@openremote/or-icon";
import { OrTreeMenu, TreeNode, TreeMenuSelection } from "@openremote/or-tree-menu";

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

// TODO: Needs to come from the manager backend model generator
export enum ServiceStatus {
  AVAILABLE = "available",
  UNAVAILABLE = "unavailable",
  UNHEALTHY = "unhealthy",
}

export enum ServiceStatusIcon {
  available = "play",
  unavailable = "alert-octagon",
  unhealthy = "minus-circle",
}

export enum ServiceStatusColor {
  available = "iconfill-gray",
  unavailable = "iconfill-red",
  unhealthy = "iconfill-red",
}

export interface ExternalService {
  label: string;
  name: string;
  iframe_url: string;
  multiTenancy: boolean;
  status: ServiceStatus;
}

export interface ServiceTreeNode extends TreeNode {
  service?: ExternalService;
}

const treeStyles = css`
  .iconfill-gray {
    --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
  }

  .iconfill-red {
    --or-icon-fill: var(--or-app-color6, ${unsafeCSS(DefaultColor6)});
  }
`;

@customElement("or-service-tree")
class OrServiceTree extends OrTreeMenu {
  static get styles(): any[] {
    return [...super.styles, treeStyles];
  }

  @property({type: Array})
  public services?: ExternalService[];

  @property({type: Boolean})
  public readonly = false;

  nodes: ServiceTreeNode[] = [];
  selection = TreeMenuSelection.SINGLE;
  menuTitle = "services";

  protected willUpdate(changedProps: PropertyValues) {
    if (changedProps.has("services")) {
      if (this.services) {
        this.nodes = this._getServiceNodes(this.services);
      }
    }
    return super.willUpdate(changedProps);
  }

  protected _dispatchSelectEvent(nodes?: ServiceTreeNode[]): boolean {
    // Only select if ONE is selected
    const newSelected = (!nodes || nodes.length > 1) ? undefined : nodes[0];
    
    if (newSelected?.service) {
      // Dispatch a custom event that the page can listen to
      const event = new CustomEvent('service-selected', {
        detail: {
          service: newSelected.service
        }
      });
      this.dispatchEvent(event);
    }

    return super._dispatchSelectEvent(nodes);
  }

  protected _getServiceNodes(services: ExternalService[]): ServiceTreeNode[] {
    return services.map(service => ({
      id: service.name,
      label: service.label,
      service: service,
      disabled: service.status === ServiceStatus.UNAVAILABLE
    }));
  }

  protected _getSingleNodeSlotTemplate(node: ServiceTreeNode): TemplateResult {
    const service = node.service!;
    return html`
      <or-icon slot="prefix" icon="puzzle"></or-icon>
      <span>${node.label}</span>
      <or-icon slot="suffix" 
               icon="${ServiceStatusIcon[service.status]}" 
               class="${ServiceStatusColor[service.status]}">
      </or-icon>
    `;
  }

  protected _getHeaderTemplate(): TemplateResult {
    return html`
      <div id="tree-header">
        <h3 id="tree-header-title">
          <or-translate value="services.title"></or-translate>
        </h3>
      </div>
    `;
  }
}

const serviceStyles = css`
  :host {
    flex: 1;
    width: 100%;
  }

  .wrapper {
    display: flex;
    flex-direction: row;
    height: 100%;
  }

  .sidebar {
    background: #fff;
    width: 300px;
    min-width: 300px;
    box-shadow: rgba(0, 0, 0, 0.21) 0px 1px 3px 0px;
    z-index: 1;
    display: flex;
    flex-direction: column;
  }

  .msg {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    width: 100%;
  }
`;

@customElement("page-services")
export class PageServices extends Page<AppStateKeyed> {
  static get styles() {
    return [serviceStyles, OrAssetTreeStyle];
  }

  get name(): string {
    return "services";
  }

  @state()
  private externalServices: ExternalService[] = [];

  @state()
  private selectedService: ExternalService | null = null;

  @state()
  private serviceName: string | null = null;

  @state()
  protected realmName: string;

  connectedCallback() {
    super.connectedCallback();

    // TODO: Get services from backend via registry mechanism
    const testService = {
      label: "ML Forecast Service",
      name: "ml-forecast",
      iframe_url: "http://localhost:8001/services/ml-forecast/ui",
      multiTenancy: true,
      status: ServiceStatus.AVAILABLE,
    };

    const testService2 = {
      label: "Home Assistant",
      name: "home-assistant",
      iframe_url: "http://192.168.0.106:8123/lovelace/default_view",
      multiTenancy: true,
      status: ServiceStatus.UNAVAILABLE,
    };

    this.externalServices = [testService, testService2];
  }

  constructor(store: Store<AppStateKeyed>) {
    super(store);
    this.realmName = manager.displayRealm;
  }

  public stateChanged(state: AppStateKeyed) {
    this.getRealmState(state);

    // If a service name is provided, try and find the service and select it
    this.serviceName = state.app.params?.serviceName;
    if (this.serviceName) {
      const service = this.externalServices.find((service) => service.name === this.serviceName);
      if (service) {
        this.selectService(service);
      }
    } else {
      // If no service name is provided, clear the selected service
      this.selectedService = null;
    }
  }

  protected selectService(service: ExternalService) {
    this.selectedService = service;
    router.navigate(`/services/${service.name}`);
  }

  protected realmSelector = (state: AppStateKeyed) => state.app.realm || manager.config.realm;

  protected getRealmState = createSelector([this.realmSelector], async (realm) => {
    this.realmName = realm;
  });

  protected _onServiceSelected(e: CustomEvent) {
    const service = e.detail.service as ExternalService;
    if (service) {
      this.selectService(service);
    }
  }

  /**
   * Get the iframe path for a given service
   * @param service - The service to get the iframe path for
   * @returns The iframe path
   */
  protected getIframePath(service: ExternalService) {
    const isSuperUser = manager.isSuperUser();

    // If the service is not multi-tenancy, we can just use the iframe_url
    if (!service.multiTenancy) {
      return service.iframe_url;
    }

    // If the user is super user, we can just use the iframe_url with the realm name
    if (isSuperUser) {
      return `${service.iframe_url}/${this.realmName}`;
    }

    // Otherwise we need to add the realm name to the iframe_url as a query param
    return `${service.iframe_url}?realm=${this.realmName}`;
  }

  protected render() {
    const noSelection = !this.selectedService;

    return html`
      <div class="wrapper">
        <div class="sidebar">
          <or-service-tree
            .services="${this.externalServices}"
            @service-selected="${this._onServiceSelected}"
          ></or-service-tree>
        </div>
        ${noSelection
          ? html`<div class="msg"><or-translate value="services.noServiceSelected"></or-translate></div>`
          : html`<or-iframe .src="${this.getIframePath(this.selectedService)}"></or-iframe>`}
      </div>
    `;
  }
}
