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
import { OrTreeMenu, TreeNode, TreeMenuSelection, OrTreeNode } from "@openremote/or-tree-menu";
import { Microservice, MicroserviceStatus } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

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

export enum MicroserviceStatusIcon {
  AVAILABLE = "play",
  UNAVAILABLE = "alert-octagon",
  UNHEALTHY = "minus-circle",
}

export enum MicroserviceStatusColor {
  AVAILABLE = "iconfill-gray",
  UNAVAILABLE = "iconfill-red",
  UNHEALTHY = "iconfill-red",
}

export interface ServiceTreeNode extends TreeNode {
  service?: Microservice;
}

const treeStyles = css`
  .iconfill-gray {
    --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
  }

  .iconfill-red {
    --or-icon-fill: var(--or-app-color6, ${unsafeCSS(DefaultColor6)});
  }

  .service-icon {
    --or-icon-fill: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
  }

  or-tree-node {
    padding-left: 10px;
  }
`;

@customElement("or-service-tree")
class OrServiceTree extends OrTreeMenu {
  static get styles(): any[] {
    return [...super.styles, treeStyles];
  }

  @property({ type: Array })
  public services?: Microservice[];

  @property({ type: String })
  public selectedService?: Microservice;

  @property({ type: Boolean })
  public readonly = false;

  nodes: ServiceTreeNode[] = [];
  selection = TreeMenuSelection.SINGLE;
  menuTitle = "services";

  protected willUpdate(changedProps: PropertyValues) {
    if (changedProps.has("services")) {
      if (this.services) {
        this.nodes = this._getServiceNodes(this.services);

        // Select any existing selected service
        if (this.selectedService) {
          const nodeToSelect = this.nodes.find((node) => node.id === this.selectedService.serviceId);
          if (nodeToSelect) {
            this._selectNode(nodeToSelect as unknown as OrTreeNode);
          }
        }
      }
    }

    // Handle select change from parent
    if (changedProps.has("selectedService")) {
      if (this.selectedService) {
        const nodeToSelect = this.nodes.find((node) => node.id === this.selectedService.serviceId);
        if (nodeToSelect) {
          this._selectNode(nodeToSelect as unknown as OrTreeNode);
        }
      } else {
        this.deselectAllNodes();
      }
    }

    return super.willUpdate(changedProps);
  }

  protected _dispatchSelectEvent(nodes?: ServiceTreeNode[]): boolean {
    // Only select if ONE is selected
    const newSelected = !nodes || nodes.length > 1 ? undefined : nodes[0];

    if (newSelected?.service) {
      // Dispatch a custom event that the page can listen to
      const event = new CustomEvent("service-selected", {
        detail: {
          service: newSelected.service,
        },
      });
      this.dispatchEvent(event);
    }

    return super._dispatchSelectEvent(nodes);
  }

  protected _getServiceNodes(services: Microservice[]): ServiceTreeNode[] {
    return services.map((service) => ({
      id: service.serviceId,
      label: service.label,
      service: service,
      disabled: service.status === MicroserviceStatus.UNAVAILABLE,
    }));
  }

  protected _getSingleNodeSlotTemplate(node: ServiceTreeNode): TemplateResult {
    const service = node.service!;
    return html`
      <or-icon class="service-icon" slot="prefix" icon="puzzle"></or-icon>
      <span>${node.label}</span>
      <or-icon slot="suffix" icon="${MicroserviceStatusIcon[service.status]}" class="${MicroserviceStatusColor[service.status]}">
      </or-icon>
    `;
  }

  protected _getHeaderTemplate(): TemplateResult {
    return html`
      <div id="tree-header">
        <h3 id="tree-header-title">
          <or-translate value="services.title"></or-translate>
        </h3>
        <or-mwc-input type=${InputType.BUTTON} icon="refresh" title="Refresh services" @or-mwc-input-changed=${this._onRefreshServices}></or-mwc-input>
      </div>
    `;
  }

  protected _onRefreshServices() {
    this.dispatchEvent(new CustomEvent("refresh-services", { detail: {} }));
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
    background-color: #f9f9f9;
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
  private services: Microservice[] = [];

  @state()
  private selectedService: Microservice | null = null;

  @state()
  private serviceName: string | null = null;

  @state()
  protected realmName: string;


  @state()
  protected _loading = false;

  connectedCallback() {
    super.connectedCallback();
    this._loadData();
  }

  private async _loadData() {
    this._loading = true;

    const response = await manager.rest.api.MicroserviceResource.getServices();
    if (response.status === 200) {
      this.services = response.data;
      this.requestUpdate();
    } // else error handling

    this._loading = false;
  }

  private async _onRefreshServices() {
    this._loadData();
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
      const service = this.services.find((service) => service.serviceId === this.serviceName);
      if (service) {
        this.selectService(service);
      }
    } else {
      // If no service name is provided, clear the selected service
      this.selectedService = undefined;
    }
  }

  protected selectService(service: Microservice) {
    this.selectedService = service;
    router.navigate(`/services/${service.serviceId}`);
  }

  protected realmSelector = (state: AppStateKeyed) => state.app.realm || manager.config.realm;

  protected getRealmState = createSelector([this.realmSelector], async (realm) => {
    this.realmName = realm;
  });

  protected _onServiceSelected(e: CustomEvent) {
    const service = e.detail.service as Microservice;
    if (service) {
      this.selectService(service);

    }
  }

  /**
   * Get the iframe path for a given service
   * @param service - The service to get the iframe path for
   * @returns The iframe path
   */
  protected getServiceUrlPath(service: Microservice) {
    const isSuperUser = manager.isSuperUser();

    // If the service is not multi-tenancy, we can just use the iframe_url
    if (!service.multiTenancy) {
      return service.url;
    }

    // If the user is super user, we can just use the iframe_url with the realm name
    if (isSuperUser) {
      return `${service.url}/${this.realmName}`;
    }

    // Otherwise we need to add the realm name to the iframe_url as a query param
    return `${service.url}?realm=${this.realmName}`;
  }

  protected render() {
    const noSelection = !this.selectedService;

    return html`
      <div class="wrapper">
        <div class="sidebar">
          <or-service-tree
            .services="${this.services}"
            .selectedService="${this.selectedService}"
            @service-selected="${this._onServiceSelected}"
            @refresh-services="${this._onRefreshServices}"
          ></or-service-tree>
        </div>
        ${noSelection
          ? html`<div class="msg"><or-translate value="services.noServiceSelected"></or-translate></div>`
          : html`<or-iframe .src="${this.getServiceUrlPath(this.selectedService)}"></or-iframe>`}
      </div>
    `;
  }
}
