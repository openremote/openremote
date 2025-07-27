/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import "./or-loading-indicator";

export enum OrIFrameEventType {
  LOADED = 'or-iframe-loaded',
  ERROR = 'or-iframe-error'
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
    color: var(--or-app-color6, #f44336);
    font-weight: 500;
  }
`;

@customElement("or-iframe")
export class OrIframe extends LitElement {
  static get styles() {
    return [style];
  }

  @property({ type: String })
  public src?: string;

  @state()
  private loading = false;

  @state()
  private error = false;

  @query('#or-iframe')
  private iframe!: HTMLIFrameElement;

  private handleIframeEvent = (type: OrIFrameEventType, event: Event): void => {
    console.info("Handling iframe event: ", type, event);

    switch (type) {
      case OrIFrameEventType.LOADED:
        this.error = false;
        this.loading = false;
        this.requestUpdate();
        break;
      case OrIFrameEventType.ERROR:
        this.error = true;
        this.loading = false;
        this.requestUpdate();
        break;
    }
  };

  connectedCallback(): void {
    super.connectedCallback();
    this.loading = true;
    this.error = false;
  }

  willUpdate(changedProperties: PropertyValues): void {
    // if src is changed, start loading again
    if (changedProperties.has('src')) {
      this.loading = true;
      this.error = false;
    }
  }

  firstUpdated(): void {
    this.iframe.addEventListener('load', (e) => this.handleIframeEvent(OrIFrameEventType.LOADED, e));
    this.iframe.addEventListener('error', (e) => this.handleIframeEvent(OrIFrameEventType.ERROR, e));
  }

  disconnectedCallback(): void {
    this.iframe.removeEventListener('load', (e) => this.handleIframeEvent(OrIFrameEventType.LOADED, e));
    this.iframe.removeEventListener('error', (e) => this.handleIframeEvent(OrIFrameEventType.ERROR, e));
  }

  render() {
    return html`
      <div class="wrapper">
        ${this.loading ? html`<or-loading-indicator></or-loading-indicator>` : html``}
        ${this.error ? html`<div class="error">Error loading iframe</div>` : html``}
        <iframe
          ?hidden="${this.loading || this.error}"
          id="or-iframe"
          src="${this.src}"
        ></iframe>
      </div>
    `;
  }
}
