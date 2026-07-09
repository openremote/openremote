/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import { OrMapBaseControl } from "./base";
import "@openremote/or-icon";

export class OrLogoControl extends OrMapBaseControl {
    onAdd(): HTMLElement {
        const a = document.createElement("a");
        a.href = "https://openremote.io/";
        a.target = "_blank";
        a.rel = "noopener noreferrer";
        a.style.cssText = "margin-left:10px;height:24px;pointer-events:auto;";
        const icon = document.createElement("or-icon");
        icon.setAttribute("icon", "or:logo-grayscale");
        a.appendChild(icon);
        return (this._container = a);
    }
}
