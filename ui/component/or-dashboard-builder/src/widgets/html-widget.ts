import {css, html, PropertyValues, TemplateResult } from "lit";
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';
import {OrWidget} from "../util/or-widget";
import {WidgetConfig} from "../util/widget-config";
import {WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import { customElement, query, queryAll, state, property } from "lit/decorators.js";
import {HtmlSettings} from "../settings/html-settings";
import { when } from "lit/directives/when.js";
import {throttle} from "lodash";
import DOMPurify from 'dompurify'

export interface HtmlWidgetConfig extends WidgetConfig {
    html: string,
    css: string,
    sanitizerConfig: Object
}

function getDefaultWidgetConfig() {
    return {
        html: '',
        css: '',
        sanitizerConfig: {
            ALLOWED_TAGS: ['p', 'div', 'span', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'br', 'h1', 'h2', 'h3'],
            ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
            ALLOWED_STYLES: [
                'color', 'font-size', 'text-align', 'margin', 'padding',
                'font-weight', 'font-style', 'text-decoration'
            ],
            ADD_ATTR: ['target'], // Allow target="_blank" for links
            RETURN_DOM_FRAGMENT: false,
            RETURN_DOM: false
        }
    } as HtmlWidgetConfig;
}

const styling = css`
  #widget-wrapper {
    height: 100%;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }
    
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  .attr-input {
    width: 100%;
    box-sizing: border-box;
  }
`
//This widget requires some good security checks, free input fields may allow code injection!

@customElement("html-widget")
export class HtmlWidget extends OrWidget {

    protected widgetConfig!: HtmlWidgetConfig;

    @state()
    protected _loading = false;

    @query("#widget-wrapper")
    protected widgetWrapperElem?: HTMLElement;

    @queryAll(".attr-input")
    protected attributeInputElems?: NodeList;

    protected resizeObserver?: ResizeObserver;

    // DOMPurify configuration
    private sanitizerConfig = {
        ALLOWED_TAGS: ['p', 'div', 'span', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'br', 'h1', 'h2', 'h3'],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
        ALLOWED_STYLES: [
            'color', 'font-size', 'text-align', 'margin', 'padding',
            'font-weight', 'font-style', 'text-decoration'
        ],
        ADD_ATTR: ['target'], // Allow target="_blank" for links
        RETURN_DOM_FRAGMENT: false,
        RETURN_DOM: false
    };


    static getManifest(): WidgetManifest {
        return {
            displayName: "HTML",
            displayIcon: "language-html5",
            getContentHtml(config: WidgetConfig): OrWidget {
                return new HtmlWidget(config);
            },
            getDefaultConfig(): WidgetConfig {
                return getDefaultWidgetConfig();
            },
            getSettingsHtml(config: WidgetConfig): WidgetSettings {
                return new HtmlSettings(config);
            }

        }
    }

    // TODO: Improve this to be more efficient
    refreshContent(force: boolean): void {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as HtmlWidgetConfig;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.resizeObserver?.disconnect();
        delete this.resizeObserver;
    }

    protected willUpdate(changedProps: PropertyValues) {

        // If widgetConfig, and the attributeRefs of them have changed...
        if(changedProps.has("widgetConfig") && this.widgetConfig) {
        }

        // Workaround for an issue with scalability of or-attribute-input when using 'display: flex'.
        // The percentage slider doesn't scale properly, causing the dragging knob to glitch.
        // Why? Because the Material Design element listens to a window resize, not a container resize.
        // So we manually trigger this event when the attribute-input-widget changes in size.
        if(!this.resizeObserver && this.widgetWrapperElem) {
            this.resizeObserver = new ResizeObserver(throttle(() => {
                window.dispatchEvent(new Event('resize'));
            }, 200));
            this.resizeObserver.observe(this.widgetWrapperElem);
        }

        return super.willUpdate(changedProps);
    }


    protected render(): TemplateResult {
        const sanitizedContent = this.getSanitizedContent();
        return html`
            <div id="widget-wrapper">
                <div class="sanitized-html">${unsafeHTML(sanitizedContent)}</div>
            </div>
        `;
    }

    // Sanitize the HTML content
    private getSanitizedContent(): string {
        return DOMPurify.sanitize(this.widgetConfig.html, this.widgetConfig.sanitizerConfig);
    }
}
