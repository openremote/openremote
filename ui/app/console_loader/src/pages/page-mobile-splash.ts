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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {Page, PageProvider, AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";

export interface SplashConfig {
    redirect: string;
    interval?: number;
    logoMobile?: HTMLTemplateElement | string;
}

export function pageMobileSplashProvider(store: Store<AppStateKeyed>, config?: SplashConfig): PageProvider<AppStateKeyed> {
    return {
        name: "splash",
        routes: [
            "splash"
        ],
        pageCreator: () => {
            const page = new PageMobileSplash(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-mobile-splash")
export class PageMobileSplash extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;     
                align-items: center;
                justify-content: center;       
            }
        `;
    }

    get name(): string {
        return "mobile-splash";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
        if (this.config) {
            setTimeout(() => {
                window.location.href = this.config!.redirect;
            }, this.config.interval ? this.config.interval : 3000);
        }
    }
    

    public stateChanged(state: AppStateKeyed) {
    }


    @property()
    public config?: SplashConfig;

    protected render() {
        return html`
            <div><img id="logo-mobile" src="${this.config?.logoMobile}" /></div>
        `;
    }
}
