import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";

/**
 * A simple loading wrapper around some other content that will hide the content whilst loading property is true
 */
@customElement("or-loading-wrapper")
export class OrLoadingWrapper extends LitElement {

    @property({type: Number})
    public loadingHeight?: number;

    @property({type: Boolean})
    public loadDom: boolean = true;

    @property({type: Boolean})
    public fadeContent: boolean = false;

    @property({type: Boolean})
    public loading: boolean = false;

    @property({type: Object, attribute: false})
    public content?: TemplateResult;

    // language=CSS
    public static get styles() {
        return css`
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
        return html`
            <div id="wrapper">
                ${this.loading ? html`<div id="loader">LOADING</div>` : ``}
                ${this.loadDom || !this.loading? html`
                    <div id="content-wrapper" class="${this.loading ? this.fadeContent ? "faded" : "hidden" : ""}">
                        <slot></slot>
                        ${this.content || ``}
                    </div>` : ``}
            </div>
        `;
    }
}
