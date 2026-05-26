/*
 * Copyright 2026, OpenRemote Inc.
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
import { LitElement, html } from "lit";
import {customElement, property} from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { EditorWorkspace } from "./editor-workspace";
import { project } from "./flow-editor";

@customElement("connection-container")
export class ConnectionContainer extends LitElement {
    @property({ attribute: false }) private workspace!: EditorWorkspace;

    constructor() {
        super();
        project.addListener("connectioncreated", () => {
            this.requestUpdate();
        });
        project.addListener("connectionremoved", () => {
            this.requestUpdate();
        });
        project.addListener("cleared", () => {
            this.requestUpdate();
        });
    }

    protected render() {
        return html`${repeat(project.connections, (c) => c.from! + c.to!, (c) => html`<connection-line .workspace="${this.workspace}" .connection="${c}"></connection-line>`)}`;
    }
}
