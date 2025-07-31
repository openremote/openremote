import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import "./or-loading-indicator";
import "@openremote/or-icon";

export enum OrIFrameEventType {
  LOADED = "or-iframe-loaded",
  ERROR = "or-iframe-error",
  TIMEOUT = "or-iframe-timeout",
}

// event detail for or-iframe-event
export interface OrIFrameEventDetail {
  type: OrIFrameEventType;
  src?: string;
  error?: string;
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

@customElement("or-iframe")
export class OrIframe extends LitElement {
  static get styles() {
    return [style];
  }

  @property({ type: String })
  public src?: string;

  @property({ type: Number })
  public timeout = 10000; // 10 seconds default timeout

  @property({ type: String })
  public loadErrorMessage?: string;

  @state()
  private loading = true;

  @state()
  private loadingError = false;

  private timeoutId?: number;

  private clearTimeout(): void {
    if (this.timeoutId) {
      window.clearTimeout(this.timeoutId);
      this.timeoutId = undefined;
    }
  }

  private startTimeout(): void {
    this.clearTimeout();

    this.timeoutId = window.setTimeout(() => {
      console.warn(`Iframe load timeout after ${this.timeout}ms for src: ${this.src}`);
      this.handleIframeEvent(OrIFrameEventType.TIMEOUT, new Event("timeout"));
    }, this.timeout);
  }

  private resetState(): void {
    this.loading = true;
    this.loadingError = false;
    this.startTimeout();
  }

  connectedCallback(): void {
    super.connectedCallback();
    this.resetState();
  }


  willUpdate(changedProperties: PropertyValues): void {
    if (changedProperties.has("src")) {
      this.resetState();
    }
  }

  private handleLoadEvent = (event: Event): void => {
    this.handleIframeEvent(OrIFrameEventType.LOADED, event);
  };

  private handleErrorEvent = (event: Event): void => {
    this.handleIframeEvent(OrIFrameEventType.ERROR, event);
  };

  private handleIframeEvent = (type: OrIFrameEventType, event: Event): void => {
    this.clearTimeout();

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

    this.dispatchEvent(new CustomEvent("or-iframe-event", { detail }));

    switch (type) {
      case OrIFrameEventType.LOADED:
        console.log("or-iframe content loaded", event);
        this.loadingError = false;
        this.loading = false;
        this.requestUpdate();
        break;
      case OrIFrameEventType.ERROR:
        console.log("or-iframe content error", event);
      case OrIFrameEventType.TIMEOUT:
        console.log("or-iframe content timeout", event);
        this.loadingError = true;
        this.loading = false;
        this.requestUpdate();
        break;
    }
  };

  disconnectedCallback(): void {
    this.clearTimeout();
  }

  /**
   * Reload the iframe content by clearing and resetting the src
   */
  public reload(): void {
    if (!this.src) {
      console.warn('Cannot reload iframe: no src specified');
      return;
    }
    this.resetState();
    
    // Force iframe reload by temporarily clearing and resetting src
    const iframe = this.shadowRoot?.querySelector('iframe');
    if (iframe) {
      const currentSrc = this.src;
      iframe.src = '';
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
        ${this.loadingError
          ? html`<div class="error">
              <or-icon icon="alert-octagon"></or-icon> ${this.loadErrorMessage || "Failed to load iframe"}
            </div>`
          : html``}
        <iframe
          @load=${this.handleLoadEvent}
          @error=${this.handleErrorEvent}
          id="or-iframe"
          src="${this.src}"
          class="${!this.loading ? "loaded" : ""}"
        ></iframe>
      </div>
    `;
  }
}
