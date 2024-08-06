var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
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
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";
import "./or-loading-indicator";
let OrFileUploader = class OrFileUploader extends LitElement {
    constructor() {
        super(...arguments);
        // Contains the content that will be shown towards the user.
        this.src = "";
        this.title = "";
        this.accept = "image/png,image/jpeg,image/vnd.microsoft.icon,image/svg+xml";
        this.loading = false;
        this.files = [];
    }
    get Files() {
        return this.files;
    }
    static get styles() {
        return css `
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
    firstUpdated(_changedProperties) {
        var _a, _b;
        super.firstUpdated(_changedProperties);
        const fileInput = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.getElementById('fileInput');
        const imageContainer = (_b = this.shadowRoot) === null || _b === void 0 ? void 0 : _b.getElementById('imageContainer');
        fileInput === null || fileInput === void 0 ? void 0 : fileInput.addEventListener("input", (e) => {
            this._onChange(e);
        });
        imageContainer === null || imageContainer === void 0 ? void 0 : imageContainer.addEventListener("click", (e) => {
            fileInput === null || fileInput === void 0 ? void 0 : fileInput.click();
        });
    }
    _onChange(e) {
        var _a;
        return __awaiter(this, void 0, void 0, function* () {
            this.files = (_a = e === null || e === void 0 ? void 0 : e.target) === null || _a === void 0 ? void 0 : _a.files;
            this.loading = true;
            this.requestUpdate();
            this.dispatchEvent(new CustomEvent("change", {
                detail: { value: this.files },
            }));
            this.loading = false;
            this.requestUpdate();
        });
    }
    render() {
        return html `
            <div class="container">
                ${this.loading ? html `
                    <or-loading-indicator .overlay="${true}"></or-loading-indicator>` : ""}
                <div class="title">${this.title}</div>
                <div id="imageContainer">

                    ${this.src ? html `
                          <img src="${this.src}" alt="OR-File-Uploader">
                          <div class="pencil-container">
                              <or-icon icon="pencil-circle"></or-icon>
                          </div>
                      ` : html `
                          <div class="placeholder-container">
                              <or-icon icon="upload"></or-icon>
                              ${i18next.t('uploadFile')}
                          </div>
                      `}
                </div>
                <input type="file" id="fileInput" accept="${this.accept}">
            </div>
        `;
    }
};
__decorate([
    property({ attribute: false })
], OrFileUploader.prototype, "src", void 0);
__decorate([
    property()
], OrFileUploader.prototype, "title", void 0);
__decorate([
    property({ attribute: false })
], OrFileUploader.prototype, "accept", void 0);
OrFileUploader = __decorate([
    customElement("or-file-uploader")
], OrFileUploader);
export { OrFileUploader };
//# sourceMappingURL=or-file-uploader.js.map