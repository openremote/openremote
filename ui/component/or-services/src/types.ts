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
import { TreeNode } from "@openremote/or-tree-menu";
import { ExternalService } from "@openremote/model";

export enum ExternalServiceStatusIcon {
    AVAILABLE = "play",
    UNAVAILABLE = "alert-octagon",
}

export enum ExternalServiceStatusColor {
    AVAILABLE = "iconfill-gray",
    UNAVAILABLE = "iconfill-red",
}

export interface ServiceTreeNode extends TreeNode {
    service?: ExternalService;
}

/**
 * Model for the SERVICE-SELECTED event that {@link OrServiceTree} can dispatch.
 * Once a service is selected, the selected service will be shared with the consumer elements.
 */
export class OrServiceSelectedEvent extends CustomEvent<ExternalService> {
    public static readonly NAME = "or-service-selected";

    constructor(service: ExternalService) {
        super(OrServiceSelectedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: service,
        });
    }
}
