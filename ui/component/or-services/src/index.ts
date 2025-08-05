import { css, html, TemplateResult, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { manager } from "@openremote/core";
import { Microservice, MicroserviceStatus } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-components/or-iframe";
import { OrIFrameEventType, OrIFrameEventDetail, OrIframe } from "@openremote/or-components/or-iframe";
import "@openremote/or-icon";
import "./or-service-tree";
import { consolidateServices, getServiceUrlPath } from "./utils";

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

  or-iframe {
    margin: 0 10px;
    width: calc(100% - 20px);
  }

  .sidebar {
    background: #fff;
    width: 300px;
    min-width: 300px;
    box-shadow: rgba(0, 0, 0, 0.21) 0px 1px 3px 0px;
    z-index: 2;
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


  
  #fullscreen-header-wrapper {
    min-height: 36px;
    padding: 20px 30px 15px;
    display: flex;
    flex-direction: row;
    align-items: center;
  }
  
  #fullscreen-header-title {
    font-size: 18px;
    font-weight: bold;
    color: var(--or-app-color3, #424242);
    display: flex;
    align-items: center;
  }
  
  #fullscreen-header-title > or-icon {
    margin-right: 10px;
    cursor: pointer;
  }
  
  #fullscreen-header-actions {
    flex: 1 1 auto;
    text-align: right;
  }
  
  #fullscreen-header-actions-content {
    display: flex;
    flex-direction: row;
    align-items: center;
    float: right;
  }
  
  .small-btn {
    margin-left: 8px;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: background-color 0.2s;
  }
  
  .small-btn:hover {
    background-color: rgba(0, 0, 0, 0.1);
  }

  /* Mobile responsive styles */
  @media only screen and (max-width: 640px) {
    .hideMobile {
      display: none !important;
    }
    
    .sidebar {
      width: 100%;
      min-width: 100%;
      flex: 1;
    }
    
    .sidebar.hidden {
      display: none !important;
    }
    
    .wrapper {
      flex-direction: column;
    }
    
    or-iframe {
      flex: 1;
      min-height: 400px;
    }
    

    #fullscreen-header-wrapper {
      padding: 11px !important;

    }
  }

  @media only screen and (min-width: 641px) {
    .showMobile {
      display: none !important;
    }
    
    .sidebar {
      width: 300px;
      min-width: 300px;
    }
    
    .wrapper {
      flex-direction: row;
    }
  }

  /* Desktop layout */
  @media only screen and (min-width: 768px) {
    .sidebar {
      width: 300px;
      min-width: 300px;
      box-shadow: rgba(0, 0, 0, 0.21) 0px 1px 3px 0px;
    }
    
    or-iframe {
      flex: 1;
      max-width: calc(100vw - 300px);
    }
  }
`;

export interface ServicesConfig {
  // Configuration options for the services component
}

@customElement("or-services")
export class OrServices extends LitElement {
  static get styles() {
    return [serviceStyles];
  }

  @property({ type: Object })
  public config?: ServicesConfig;

  @property({ type: String })
  public realmName: string = manager.displayRealm;

  @property({ type: Boolean })
  public readonly: boolean = false;

  @state()
  private services: Microservice[] = [];

  @state()
  private selectedService: Microservice | null = null;

  @state()
  private serviceName: string | null = null;

  @state()
  protected _loading = false;

  @state()
  protected showServiceTree: boolean = true;

  connectedCallback(): void {
    super.connectedCallback();
    this._loadData();
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
  }

  private async _loadData(): Promise<void> {
    this._loading = true;

    try {
      const response = await manager.rest.api.MicroserviceResource.getServices();
      if (response.status === 200) {
        this.services = consolidateServices(response.data);
        this.requestUpdate();

        // Re-select service if necessary after loading services
        if (this.serviceName && !this.selectedService) {
          const service = this.services.find((service) => service.serviceId === this.serviceName);
          if (service) {
            this.selectService(service);
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

  // Refresh all services by reloading the data
  private async _onRefreshServices(): Promise<void> {
    await this._loadData();
  }

  // Reload the current service iframe
  private _onRefreshServiceIFrame(): void {
    const iframe = this.shadowRoot?.querySelector('#service-iframe') as OrIframe;
    if (iframe && typeof iframe.reload === 'function') {
      iframe.reload();
    } else {
      console.warn('Unable to reload iframe: iframe not found or reload method not available');
    }
  }

  public setServiceName(serviceName: string | null): void {
    this.serviceName = serviceName;
    if (serviceName) {
      const service = this.services.find((service) => service.serviceId === serviceName);
      if (service) {
        this.selectService(service);
      }
    } else {
      this.selectedService = null;
    }
  }

  protected selectService(service: Microservice): void {
    this.selectedService = service;
    
    // Dispatch event for parent components to handle navigation
    const event = new CustomEvent("service-selected", {
      detail: {
        service: service,
        serviceId: service.serviceId
      }
    });
    this.dispatchEvent(event);
    
    // On mobile, hide the tree when a service is selected
    if (window.matchMedia("(max-width: 600px)").matches) {
      this.showServiceTree = false;
    }
  }

  protected toggleServiceTree(): void {
    this.showServiceTree = !this.showServiceTree;
  }

  protected _onServiceSelected(e: CustomEvent): void {
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
  protected getServiceUrlPath(service: Microservice): string {
    const isSuperUser = manager.isSuperUser();
    return getServiceUrlPath(service, this.realmName, isSuperUser);
  }

  /**
   * Open the selected service in a new tab
   */
  protected openServiceInNewTab(): void {
    if (this.selectedService) {
      const url = this.getServiceUrlPath(this.selectedService);
      window.open(url, '_blank');
    }
  }

  protected render(): TemplateResult {
    const noSelection = !this.selectedService;


    return html`
      <div class="wrapper">
        ${this.showServiceTree ? html`
          <div class="sidebar ${this.selectedService ? 'hideMobile' : ''}">
            <or-service-tree
              .services="${this.services}"
              .selectedService="${this.selectedService}"
              @service-selected="${this._onServiceSelected}"
              @refresh-services="${this._onRefreshServices}"
            ></or-service-tree>
          </div>
        ` : undefined}
        <div class="${this.selectedService == null ? 'hideMobile' : undefined}" style="flex: 1; display: flex; flex-direction: column;">
          ${this.selectedService ? html`
            <div id="fullscreen-header">
              <div id="fullscreen-header-wrapper">
                <div id="fullscreen-header-title">
                  <or-icon class="showMobile" icon="chevron-left" @click="${() => { this.selectedService = null; this.showServiceTree = true; }}"></or-icon>
                  <or-icon class="hideMobile" icon="puzzle" title="${i18next.t("services.services")}"></or-icon>
                  <span>${this.selectedService.label || this.selectedService.serviceId}</span>
                </div>
                <div id="fullscreen-header-actions">
                  <div id="fullscreen-header-actions-content">
                    <or-icon class="small-btn" icon="refresh" title="${i18next.t("services.refresh")}" @click="${this._onRefreshServiceIFrame}"></or-icon>
                    <or-icon class="small-btn" icon="open-in-new" title="${i18next.t("services.openInNewTab")}" @click="${this.openServiceInNewTab}"></or-icon>
                  </div>
                </div>
              </div>
            </div>
          ` : undefined}
          <div style="flex: 1;">
            ${noSelection
              ? html`<div class="msg"><or-translate value="services.noServiceSelected"></or-translate></div>`
              : this.selectedService ? html`<or-iframe id="service-iframe"
                  .src="${this.getServiceUrlPath(this.selectedService)}"
                  .loadErrorMessage="${i18next.t("services.iframeLoadError")}"
                ></or-iframe>` : html``}
          </div>
        </div>
      </div>
    `;
  }

  protected updated(changedProperties: Map<string, any>) {
    if (changedProperties.has("realmName")) {
      this._loadData();
    }
  }
} 