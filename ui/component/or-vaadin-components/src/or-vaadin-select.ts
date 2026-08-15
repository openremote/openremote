/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { Select } from "@vaadin/select";
import { customElement } from "lit/decorators.js";
import type { OrVaadinComponent } from "./util";
import type { LitElement } from "lit";

export { SelectItem } from "@vaadin/select";
export { selectRenderer } from "@vaadin/select/lit.js";

/**
 * @slot label - The label element
 */
@customElement("or-vaadin-select")
export class OrVaadinSelect extends (Select as new () => Select & LitElement) implements OrVaadinComponent {}
