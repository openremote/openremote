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
import {Util} from "@openremote/core";

export interface HtmlWidgetConfig extends WidgetConfig {
    html: string;
    css: string,
}

function getDefaultWidgetConfig() {
    return {
        html: '',
        css: '',
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
        return html`
            <div id="widget-wrapper">
                     ${unsafeHTML(this.widgetConfig.html)}  
            </div>
        `;
    }
}
