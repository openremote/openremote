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
import { css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { manager } from "@openremote/core";
import { ExternalService } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { OrIframe } from "@openremote/or-components/or-iframe";
import "@openremote/or-icon";
import "./or-service-tree";
import { getServiceUrlPath } from "./utils";
import { OrServiceSelectedEvent } from "./types";

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
        transition: opacity 0.3s ease-in-out;
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
        transition: opacity 0.3s ease-in-out;
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

@customElement("or-services")
export class OrServices extends LitElement {
    static get styles() {
        return [serviceStyles];
    }

    @property({ type: String })
    public realmName: string = manager.displayRealm;

    @property({ type: Array })
    public services: ExternalService[] = [];

    @property({ type: Object })
    public selectedService: ExternalService | null = null;

    @property({ type: Boolean })
    public loading = false;

    @state()
    protected showServiceTree = true;

    connectedCallback(): void {
        super.connectedCallback();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
    }

    // Method to refresh iframe - called by parent component
    public refreshIframe(): void {
        const iframe = this.shadowRoot?.querySelector("#service-iframe") as OrIframe;
        if (iframe && typeof iframe.reload === "function") {
            console.log("Reloading iframe");
            iframe.reload();
        } else {
            console.warn("Unable to reload iframe: iframe not found or reload method not available");
        }
    }

    protected _selectService(service: ExternalService): void {
        this.selectedService = service;

        // Dispatch event for parent components to handle navigation
        const event = new OrServiceSelectedEvent(service);
        this.dispatchEvent(event);

        // On mobile, hide the tree when a service is selected
        if (window.matchMedia("(max-width: 600px)").matches) {
            this.showServiceTree = false;
        }
    }

    protected _toggleServiceTree(): void {
        this.showServiceTree = !this.showServiceTree;
    }

    protected _getSidebarClass(): string {
        return this.selectedService ? "hideMobile" : "";
    }

    protected _onServiceSelected(e: OrServiceSelectedEvent): void {
        const service = e.detail;
        if (service) {
            this._selectService(service);
        }
    }

    protected _getServiceUrlPath(service: ExternalService): string {
        const isSuperUser = manager.isSuperUser();
        return getServiceUrlPath(service, this.realmName, isSuperUser);
    }

    protected _getServiceContentTemplate(): TemplateResult {
        if (this.loading) {
            return html``; // ignore
        }

        // If no services available, show a single consistent message
        if (this.services.length === 0) {
            return html`<div class="msg">
                <or-translate value="services.noServices"></or-translate>
            </div>`;
        }

        // If services exist but none selected, show selection prompt
        if (!this.selectedService) {
            return html`<div class="msg">
                <or-translate value="services.noServiceSelected"></or-translate>
            </div>`;
        }

        // Service selected - render iframe
        return html`<or-iframe id="service-iframe" .src="${this._getServiceUrlPath(this.selectedService)}">
            <span slot="onerror">${i18next.t("services.iframeLoadError")}</span>
        </or-iframe>`;
    }

    protected render(): TemplateResult {


        return html`
            <div class="wrapper">
                ${this.showServiceTree
                    ? html`
                          <div class="sidebar ${this._getSidebarClass()}">
                              <or-service-tree
                                  .services="${this.services}"
                                  .selectedService="${this.selectedService}"
                                  @or-service-selected="${this._onServiceSelected}"
                              ></or-service-tree>
                          </div>
                      `
                    : undefined}
                <div
                    class="${this.selectedService == null ? "hideMobile" : undefined}"
                    style="flex: 1; display: flex; flex-direction: column;"
                >
                    ${this.selectedService
                        ? html`
                              <div id="fullscreen-header">
                                  <div id="fullscreen-header-wrapper">
                                      <div id="fullscreen-header-title">
                                          <or-icon
                                              class="showMobile"
                                              icon="chevron-left"
                                              @click="${() => {
                                                  this.selectedService = null;
                                                  this.showServiceTree = true;
                                              }}"
                                          ></or-icon>
                                          <or-icon
                                              class="hideMobile"
                                              icon="${this.selectedService.icon ?? "puzzle"}"
                                              title="${i18next.t("services.services")}"
                                          ></or-icon>
                                          <span>${this.selectedService.label || this.selectedService.serviceId}</span>
                                      </div>
                                  </div>
                              </div>
                          `
                        : undefined}
                    <div style="flex: 1;">${this._getServiceContentTemplate()}</div>
                </div>
            </div>
        `;
    }
}
