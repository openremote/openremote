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
import { customElement } from "lit/decorators.js";
import { RadioGroup } from "@vaadin/radio-group";
import { OrVaadinComponent } from "./util";
import { css, LitElement } from "lit";

type LitJoin<T> = T & typeof LitElement;

@customElement("or-vaadin-radio-group")
export class OrVaadinGroup extends RadioGroup implements OrVaadinComponent {
  static get styles() {
        return [
            // (RadioGroup as LitJoin<typeof RadioGroup>).styles,
            css`
              :root,
              :where(:root),
              :where(:host) {
                  --lumo-space-xs: 0 !important;
              }
            `,
        ];
    }
}
