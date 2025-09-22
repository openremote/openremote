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
import { css, html, unsafeCSS, TemplateResult, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { DefaultColor3, DefaultColor5, DefaultColor6 , Util } from "@openremote/core";
import { OrTreeMenu, TreeMenuSelection, OrTreeNode, TreeMenuSorting } from "@openremote/or-tree-menu";
import { ExternalService, ExternalServiceStatus } from "@openremote/model";
import {
    ServiceTreeNode,
    ExternalServiceStatusIcon,
    ExternalServiceStatusColor,
    OrServiceSelectedEvent,
} from "./types";


export enum ServiceTreeSorting {
    NAME = "name", STATUS = "status"
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



/**
 * @event {OrServiceSelectedEvent} or-service-selected - Triggers upon selecting a service, and dispatches the selected service.
 */
@customElement("or-service-tree")
export class OrServiceTree extends OrTreeMenu {
    static get styles(): any[] {
        return [...super.styles, treeStyles];
    }

    @property({ type: Array })
    public services?: ExternalService[];

    @property({ type: Object })
    public selectedService?: ExternalService;

    @property({ type: Boolean })
    public readonly = false;

    nodes: ServiceTreeNode[] = [];
    selection = TreeMenuSelection.SINGLE;
    menuTitle = "services";
    sortBy: any = ServiceTreeSorting.NAME;
    sortOptions: any[] = [ServiceTreeSorting.NAME, ServiceTreeSorting.STATUS];

    protected willUpdate(changedProps: PropertyValues): void {
        if (changedProps.has("services")) {
            if (this.services) {
                this.nodes = this._getServiceNodes(this.services);

                // Select any existing selected service
                if (this.selectedService) {
                    const nodeToSelect = this.nodes.find((node) => node.id === this.selectedService?.serviceId);
                    if (nodeToSelect) {
                        this._selectNode(nodeToSelect as unknown as OrTreeNode);
                    }
                }
            }
        }

        // Handle select change from parent
        if (changedProps.has("selectedService")) {
            if (this.selectedService) {
                const nodeToSelect = this.nodes.find((node) => node.id === this.selectedService?.serviceId);
                if (nodeToSelect) {
                    this._selectNode(nodeToSelect as unknown as OrTreeNode);
                }
            } else {
                this.deselectAllNodes();
            }
        }

        super.willUpdate(changedProps);
    }

    protected _dispatchSelectEvent(nodes?: ServiceTreeNode[]): boolean {
        // Only select if ONE is selected
        const newSelected = !nodes || nodes.length > 1 ? undefined : nodes[0];

        if (newSelected?.service) {
            // Dispatch a custom event that the page can listen to
            const event = new OrServiceSelectedEvent(newSelected.service);
            this.dispatchEvent(event);
        }

        return super._dispatchSelectEvent(nodes);
    }

    protected _getServiceNodes(services: ExternalService[]): ServiceTreeNode[] {
        return services.map((service) => ({
            id: service.serviceId || "",
            label: service.label || "",
            service,
            disabled: service.status === ExternalServiceStatus.UNAVAILABLE,
        }));
    }

    protected _getSingleNodeSlotTemplate(node: ServiceTreeNode): TemplateResult {
        const service = node.service!;
        const statusIcon =
            service.status && ExternalServiceStatusIcon[service.status]
                ? ExternalServiceStatusIcon[service.status]
                : ExternalServiceStatusIcon.UNAVAILABLE;
        const statusColor =
            service.status && ExternalServiceStatusColor[service.status]
                ? ExternalServiceStatusColor[service.status]
                : ExternalServiceStatusColor.UNAVAILABLE;

   

        return html`
            <or-icon class="service-icon" slot="prefix" icon="${node.service?.icon ?? "puzzle"}"></or-icon>
            <span>${node.label}</span>
            <or-icon slot="suffix" icon="${statusIcon}" class="${statusColor}"> </or-icon>
        `;
    }

    protected _getHeaderTemplate(): TemplateResult {
        return html`
            <div id="tree-header">
                <h3 id="tree-header-title">
                    <or-translate value="services.title"></or-translate>
                </h3>
                <div id="tree-header-actions">
                    ${this._getSortActionTemplate(this.sortBy, this.sortOptions)}
                </div>
            </div>
        `;
    }

    protected _getSortFunction(sortBy?: TreeMenuSorting): (a: ServiceTreeNode, b: ServiceTreeNode) => number {
        const sorting = sortBy as unknown as ServiceTreeSorting | undefined;
        switch (sorting) {
            case ServiceTreeSorting.STATUS: {
                return Util.sortByString(node => node.service?.status || "");
            }
            case ServiceTreeSorting.NAME:
            default: {
                return super._getSortFunction(sortBy);
            }
        }
    }
}
