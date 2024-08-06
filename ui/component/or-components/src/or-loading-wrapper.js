var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
/**
 * A simple loading wrapper around some other content that will hide the content whilst loading property is true
 */
let OrLoadingWrapper = class OrLoadingWrapper extends LitElement {
    constructor() {
        super(...arguments);
        this.loadDom = true;
        this.fadeContent = false;
        this.loading = false;
    }
    // language=CSS
    static get styles() {
        return css `
            :host {
                display: block;
            }
            
            .hidden {
                display: none;
            }
            
            .faded {
                opacity: 0.5;
            }
            
            #wrapper {
                position: relative;
            }
            
            #loader {
                position: absolute;
                width: 100%;
                height: 100%;
            }
        `;
    }
    render() {
        return html `
            <div id="wrapper">
                ${this.loading ? html `<div id="loader">LOADING</div>` : ``}
                ${this.loadDom || !this.loading ? html `
                    <div id="content-wrapper" class="${this.loading ? this.fadeContent ? "faded" : "hidden" : ""}">
                        <slot></slot>
                        ${this.content || ``}
                    </div>` : ``}
            </div>
        `;
    }
};
__decorate([
    property({ type: Number })
], OrLoadingWrapper.prototype, "loadingHeight", void 0);
__decorate([
    property({ type: Boolean })
], OrLoadingWrapper.prototype, "loadDom", void 0);
__decorate([
    property({ type: Boolean })
], OrLoadingWrapper.prototype, "fadeContent", void 0);
__decorate([
    property({ type: Boolean })
], OrLoadingWrapper.prototype, "loading", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrLoadingWrapper.prototype, "content", void 0);
OrLoadingWrapper = __decorate([
    customElement("or-loading-wrapper")
], OrLoadingWrapper);
export { OrLoadingWrapper };
//# sourceMappingURL=or-loading-wrapper.js.map