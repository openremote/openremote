/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import "./or-loading-indicator";
import "@openremote/or-icon";

export enum OrIFrameEventType {
  LOADED = 'or-iframe-loaded',
  ERROR = 'or-iframe-error',
  TIMEOUT = 'or-iframe-timeout'
}

export interface OrIFrameEventDetail {
  type: OrIFrameEventType;
  src?: string;
  error?: string;
}

// language=CSS
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
  private loading = false;

  @state()
  private loadingError = false;

  @query('#or-iframe')
  private iframe!: HTMLIFrameElement;

  private timeoutId?: number;


  private handleLoadEvent = (event: Event): void => {
    this.handleIframeEvent(OrIFrameEventType.LOADED, event);
  };

  private handleErrorEvent = (event: Event): void => {
    this.handleIframeEvent(OrIFrameEventType.ERROR, event);
  };

  private handleIframeEvent = (type: OrIFrameEventType, event: Event): void => {
    this.clearTimeout();

    // Dispatch event to parent
    const detail: OrIFrameEventDetail = {
      type,
      src: this.src,
      error: type === OrIFrameEventType.ERROR || type === OrIFrameEventType.TIMEOUT ? 
        (type === OrIFrameEventType.TIMEOUT ? 'Timeout loading iframe' : 'Error loading iframe') : undefined
    };

    this.dispatchEvent(new CustomEvent('or-iframe-event', { detail }));

    switch (type) {
      case OrIFrameEventType.LOADED:
        this.loadingError = false;
        this.loading = false;
        this.requestUpdate();
        break;
      case OrIFrameEventType.ERROR:
      case OrIFrameEventType.TIMEOUT:
        this.loadingError = true;
        this.loading = false;
        this.requestUpdate();
        break;
    }
  };

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
      this.handleIframeEvent(OrIFrameEventType.TIMEOUT, new Event('timeout'));
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
    // if src is changed, start loading again
    if (changedProperties.has('src')) {
      this.resetState();
    }
  }

  firstUpdated(): void {
    // Don't add listeners here since iframe might not exist yet
  }

  updated(changedProperties: PropertyValues): void {
    super.updated(changedProperties);
    
    // Add event listeners when iframe is rendered (when loading becomes false and error is false)
    if (changedProperties.has('loading') || changedProperties.has('loadingError')) {
      if (!this.loading && !this.loadingError && this.iframe) {
        // Remove any existing listeners first
        this.iframe.removeEventListener('load', this.handleLoadEvent);
        this.iframe.removeEventListener('error', this.handleErrorEvent);
        
        // Add new listeners
        this.iframe.addEventListener('load', this.handleLoadEvent);
        this.iframe.addEventListener('error', this.handleErrorEvent);
      }
    }
  }

  disconnectedCallback(): void {
    this.clearTimeout();
    if (this.iframe) {
      this.iframe.removeEventListener('load', this.handleLoadEvent);
      this.iframe.removeEventListener('error', this.handleErrorEvent);
    }
  }

  render() {
    return html`
      <div class="wrapper">
        ${this.loading ? html`<or-loading-indicator></or-loading-indicator>` : html``}
        ${this.loadingError ? html`<div class="error"><or-icon icon="alert-octagon"></or-icon> ${this.loadErrorMessage}</div>` : html``}
        ${!this.loading && !this.loadingError ? html`
          <iframe
            id="or-iframe"
            src="${this.src}"
          ></iframe>
        ` : html``}
      </div>
    `;
  }
}
