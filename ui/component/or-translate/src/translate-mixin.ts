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
import {PropertyValues} from "lit";
import { InitOptions, i18n } from "i18next";

declare type Constructor<T> = new (...args: any[]) => T;

interface CustomElement {
    connectedCallback?(): void;
    disconnectedCallback?(): void;
    readonly isConnected: boolean;
}

// TODO: Can't currently export declaration files with explicit LitElement type (see https://github.com/Microsoft/TypeScript/issues/17293)
export const translate = (i18next: i18n) => <T extends Constructor<CustomElement>>(base: T) =>
        class extends base {

            _i18nextJustInitialized = false;
            _language: string = i18next.language;

            connectedCallback() {
                if (!i18next.language) {
                    i18next.on("initialized", this.initCallback);
                }

                i18next.on("languageChanged", this.langChangedCallback);

                if (super.connectedCallback) {
                    super.connectedCallback();
                }
            }

            disconnectedCallback() {
                i18next.off("initialized", this.initCallback);
                i18next.off("languageChanged", this.langChangedCallback);

                if (super.disconnectedCallback) {
                    super.disconnectedCallback();
                }
            }

            shouldUpdate(changedProps: PropertyValues) {
                if (this._i18nextJustInitialized) {
                    this._i18nextJustInitialized = false;
                    return true;
                }

                // @ts-ignore
                return super.shouldUpdate && super.shouldUpdate(changedProps);
            }

            public initCallback = (options: InitOptions) => {
                this._i18nextJustInitialized = true;
                // @ts-ignore
                if (this.requestUpdate) {
                    // @ts-ignore
                    this.requestUpdate();
                }
            };

            public langChangedCallback = () => {
                // @ts-ignore
                if (this.requestUpdate) {
                    this._language = i18next.language;
                    // @ts-ignore
                    this.requestUpdate("_language");
                }
            }
        };
