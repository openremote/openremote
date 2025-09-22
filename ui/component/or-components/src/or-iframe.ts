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
import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "./or-loading-indicator";
import "@openremote/or-icon";

export enum OrIFrameEventType {
    LOADED = "or-iframe-loaded",
    ERROR = "or-iframe-error",
    TIMEOUT = "or-iframe-timeout",
}

/**
 * Event detail for or-iframe-event
 */
export interface OrIFrameEventDetail {
    type: OrIFrameEventType;
    src?: string;
    error?: string; // Error message if the event type is ERROR or TIMEOUT
}

/**
 * Model for the IFRAME-EVENT that {@link OrIframe} can dispatch.
 * Fired when the iframe loading state changes.
 */
export class OrIFrameEvent extends CustomEvent<OrIFrameEventDetail> {
    public static readonly NAME = "or-iframe-event";

    constructor(detail: OrIFrameEventDetail) {
        super(OrIFrameEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail,
        });
    }
}

const style = css`
    :host {
        display: block;
        width: 100%;
        height: 100%;
    }

    .wrapper {
        position: relative;
        width: 100%;
        height: 100%;
    }

    iframe {
        width: 100%;
        height: 100%;
        border: none;
        display: block;
        opacity: 0;
        transition: opacity 0.3s ease;
        overflow-y: auto;
        overflow-x: hidden;
    }

    iframe.loaded {
        opacity: 1;
    }

    .error {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100%;
        font-weight: 500;
        gap: 0.5rem;
    }
`;

/**
 * @event {OrIFrameEvent} or-iframe-event - Fired when the iframe loading state changes, and dispatches the event detail containing type, src, and optional error.
 */
@customElement("or-iframe")
export class OrIframe extends LitElement {
    static get styles() {
        return [style];
    }

    @property({ type: String })
    public src?: string;

    @property({ type: Number })
    public timeout = 10000; // 10 seconds default timeout

    @state()
    protected loading = true;

    @state()
    protected error = false;

    @property({ type: Boolean })
    public preventCache = true;

    protected timeoutId?: number;

    protected _clearTimeout(): void {
        if (this.timeoutId) {
            window.clearTimeout(this.timeoutId);
            this.timeoutId = undefined;
        }
    }

    protected _startTimeout(): void {
        this._clearTimeout();

        this.timeoutId = window.setTimeout(() => {
            console.warn(`Iframe load timeout after ${this.timeout}ms for src: ${this.src}`);
            this._handleIframeEvent(OrIFrameEventType.TIMEOUT, new Event("timeout"));
        }, this.timeout);
    }

    protected _resetState(): void {
        this.loading = true;
        this.error = false;
        this._startTimeout();
    }

    connectedCallback(): void {
        super.connectedCallback();
        this._resetState();
    }

    willUpdate(changedProperties: PropertyValues): void {
        if (changedProperties.has("src")) {
            this._resetState();
        }
    }

    protected readonly _handleLoadEvent = (event: Event): void => {
        this._handleIframeEvent(OrIFrameEventType.LOADED, event);
    };

    protected readonly _handleErrorEvent = (event: Event): void => {
        this._handleIframeEvent(OrIFrameEventType.ERROR, event);
    };

    protected readonly _handleIframeEvent = (type: OrIFrameEventType, event: Event): void => {
        this._clearTimeout();

        const detail: OrIFrameEventDetail = {
            type,
            src: this.src,
            error:
                type === OrIFrameEventType.ERROR || type === OrIFrameEventType.TIMEOUT
                    ? type === OrIFrameEventType.TIMEOUT
                        ? "Timeout loading iframe"
                        : "Error loading iframe"
                    : undefined,
        };

        this.dispatchEvent(new OrIFrameEvent(detail));

        switch (type) {
            case OrIFrameEventType.LOADED:
                this.error = false;
                this.loading = false;
                break;
            case OrIFrameEventType.ERROR:
                this.error = true;
                this.loading = false;
                console.error(`Error event loading iframe for src: ${this.src}`, event);
                break;
            case OrIFrameEventType.TIMEOUT:
                this.error = true;
                this.loading = false;
                console.error(`Timeout event loading iframe for src: ${this.src}`, event);
                break;
        }
    };

    disconnectedCallback(): void {
        this._clearTimeout();
    }

    public getSrc(): string {
        if (this.preventCache && this.src) {
            const url = new URL(this.src);
            url.searchParams.set("t", Date.now().toString());
            return url.toString();
        }
        return this.src || "";
    }

    /**
     * Reload the iframe content by clearing and resetting the src
     */
    public reload(): void {
        if (!this.src) {
            console.warn("Cannot reload iframe: no src specified");
            return;
        }
        this._resetState();

        // Force iframe reload by temporarily clearing and resetting src
        const iframe = this.shadowRoot?.querySelector("iframe");
        if (iframe) {
            const currentSrc = this.getSrc();
            iframe.src = "";
            // We can't use iframe.contentWindow.location.reload() because it doesn't work in all cases due to browser security restrictions
            requestAnimationFrame(() => {
                iframe.src = currentSrc;
            });
        }
    }

    render() {
        return html`
            <div class="wrapper">
                ${this.loading ? html`<or-loading-indicator></or-loading-indicator>` : html``}
                ${this.error
                    ? html`<div class="error">
                          <or-icon icon="alert-octagon"></or-icon>
                          <slot name="onerror">Failed to load iframe</slot>
                      </div>`
                    : html``}
                <iframe
                    @load=${this._handleLoadEvent}
                    @error=${this._handleErrorEvent}
                    id="or-iframe"
                    src="${this.getSrc()}"
                    class="${!this.loading ? "loaded" : ""}"
                ></iframe>
            </div>
        `;
    }
}
