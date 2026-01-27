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
import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {Page, PageProvider,AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";

export interface PageLogsConfig {
    viewer?: ViewerConfig
}

export function pageLogsProvider(store: Store<AppStateKeyed>, config?: PageLogsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "logs",
        routes: [
            "logs"
        ],
        pageCreator: () => {
            const page = new PageLogs(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-logs")
export class PageLogs extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;            
            }
            
            or-log-viewer {
                width: 100%;
            }
        `;
    }

    @property()
    public config?: PageLogsConfig;

    get name(): string {
        return "logs";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public stateChanged(state: AppStateKeyed) {
    }

    protected render() {
        return html`
            <or-log-viewer .config="${this.config?.viewer}"></or-log-viewer>
        `;
    }
}
