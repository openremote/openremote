/*
 * Copyright 2022, OpenRemote Inc.
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
import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";
import { FileInfo } from "@openremote/model";
import "./or-loading-indicator";

@customElement("or-file-uploader")
export class OrFileUploader extends LitElement {

    // Contains the content that will be shown towards the user.
    @property({ attribute: false })
    public readonly src: string = "";

    @property()
    public readonly title: string = "";

    @property({ attribute: false })
    public readonly accept: string = "image/png,image/jpeg,image/vnd.microsoft.icon,image/svg+xml";

    private loading = false;
    private files: FileInfo[] = []

    get Files(): FileInfo[] {
        return this.files
    }
    static get styles() {
        return css`
            #imageContainer {
                position: relative;
                max-width: 150px;
                background-color: whitesmoke;
                border: 1px solid var(--or-app-color5, #CCC);;
                cursor: pointer;
                border-radius: 4px;
                padding: 4px;
            }

            #imageContainer img {
                max-width: 100%;
                max-height: 100%;
                border-radius: 2px;
                margin: unset;
            }

            input {
                display: none;
            }

            .placeholder-container {
                text-align: center;
                width: 100%;
            }

            .placeholder-container or-icon {
                font-size: 24px;
                margin: 16px 0;
                max-height: 24px;
                width: 100%;
                text-align: center;
            }

            #imageContainer:hover .pencil-container{
                display: flex;
            }

            #imageContainer .pencil-container {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                right: 0;
                background: rgba(0,0,0,.4);
                color: var(--or-app-color4);
                display: none;
                justify-content: center;
                align-items: center;
            }

            .container {
                position: relative;
            }
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        const fileInput = this.shadowRoot?.getElementById('fileInput')
        const imageContainer = this.shadowRoot?.getElementById('imageContainer')
        fileInput?.addEventListener("input", (e) => {
            this._onChange(e);
        });
        imageContainer?.addEventListener("click", (e) => {
            fileInput?.click();
        });
    }

    async _onChange(e: any) {
        this.files = e?.target?.files;
        this.loading = true;
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent("change", {
            detail: { value: this.files },
        }));
        this.loading = false;
        this.requestUpdate();
    }

    render() {
        return html`
            <div class="container">
                ${this.loading ? html`
                    <or-loading-indicator .overlay="${true}"></or-loading-indicator>` : ""}
                <div class="title">${this.title}</div>
                <div id="imageContainer">

                    ${this.src ? html`
                          <img src="${this.src}" alt="OR-File-Uploader">
                          <div class="pencil-container">
                              <or-icon icon="pencil-circle"></or-icon>
                          </div>
                      ` : html`
                          <div class="placeholder-container">
                              <or-icon icon="upload"></or-icon>
                              ${i18next.t('uploadFile')}
                          </div>
                      `
                    }
                </div>
                <input type="file" id="fileInput" accept="${this.accept}">
            </div>
        `;
    }
}
