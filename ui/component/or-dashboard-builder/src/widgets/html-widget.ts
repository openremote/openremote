import {css, html, PropertyValues, TemplateResult } from "lit";
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';
import {OrWidget} from "../util/or-widget";
import {WidgetConfig} from "../util/widget-config";
import {WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import { customElement, query} from "lit/decorators.js";
import {createRef, Ref, ref } from "lit/directives/ref.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import {throttle} from "lodash";
import {i18next} from "@openremote/or-translate";
import {OrAceEditor} from "@openremote/or-components/or-ace-editor";
import "ace-builds/src-noconflict/mode-html";
import "ace-builds/webpack-resolver";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import DOMPurify from 'dompurify'

export interface HtmlWidgetConfig extends WidgetConfig {
    html: string,
    sanitizerConfig: Object
}

function getDefaultConfig(): HtmlWidgetConfig {
    return {
        html: '<!DOCTYPE html>\n' +
            '<html><head></head><body><p class="demoTitle">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n' +
            '<span style="background: #4d9d2a; color: #fff;"> &nbsp;OpenRemote&nbsp;</span>\n' +
            '&nbsp;HTML&nbsp;widget</p> <p class="introText"><strong>Paste your HTML, use any WYSIWYG editor for easy generation.</strong></p>\n' +
            '<p style="text-align: center;"> <img src="https://docs.openremote.io/assets/images/architecture-32e43028000b886c4d7a6e76aeba65cb.jpg" style="width: 90%; max-width: 752px;"></p>\n' +
            '<p>Write markup with&nbsp;<a rel="nofollow" target="_blank" href="https://en.wikipedia.org/wiki/HTML">HTML</a></p></body></html>',
        sanitizerConfig: {
            ALLOWED_TAGS: ['p', 'div', 'span', 'img', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'br', 'h1', 'h2', 'h3'],
            ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'style', 'src'],
            ALLOWED_STYLES: [
                'color', 'font-size', 'text-align', 'margin', 'padding',
                'font-weight', 'font-style', 'text-decoration', 'background'
            ],
            ADD_TAGS: ['!doctype'],
            ADD_ATTR: ['target'], // Allow target="_blank" for links
            RETURN_DOM_FRAGMENT: false,
            RETURN_DOM: false,
            WHOLE_DOCUMENT: true,
            RETURN_TRUSTED_TYPE: true
        }
    }
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
    
  .switch-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
  }
`

@customElement("html-widget")
export class HtmlWidget extends OrWidget {

    protected widgetConfig!: HtmlWidgetConfig;

    @query("#widget-wrapper")
    protected widgetWrapperElem?: HTMLElement;

    protected resizeObserver?: ResizeObserver;

    static getManifest(): WidgetManifest {
        return {
            displayName: "HTML",
            displayIcon: "language-html5",
            getContentHtml(config: WidgetConfig): OrWidget {
                return new HtmlWidget(config);
            },
            getDefaultConfig(): WidgetConfig {
                return getDefaultConfig();
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

@customElement("html-settings")
export class HtmlSettings extends WidgetSettings {

    protected readonly widgetConfig!: HtmlWidgetConfig;

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <!-- HTML input -->
                <settings-panel displayName="Content" expanded="${true}">
                    <div>
                        <or-mwc-input .type="${InputType.BUTTON}" outlined label="Custom HTML" icon="language-html5" @or-mwc-input-changed="${() => this.openHtmlInputDialog(this.widgetConfig.html)}"></or-mwc-input>
                    </div
                </settings-panel>
            </div>
        `;
    }



    protected openHtmlInputDialog(content?: string) {
        const editorRef: Ref<OrAceEditor> = createRef();

        showDialog(new OrMwcDialog()
            .setHeading(i18next.t("HTML Editor"))
            .setContent(()=> html `
                <div>
                    <or-ace-editor ${ref(editorRef)} .value="${content}" .mode="${this._getMode()}"></or-ace-editor>
                </div>
            `)
            .setActions([
                {actionName: "cancel", content: "cancel"},
                {actionName: "save", content: "save", action: () => {
                        if (editorRef.value) {
                            if (!editorRef.value.validate()) {
                                console.warn("HMTL is not valid");
                                showSnackbar(undefined, i18next.t('errorOccurred'));
                            } else if (editorRef.value.getValue()!.length > 0) {
                                this.widgetConfig.html = DOMPurify.sanitize(editorRef.value.getValue() ?? "", this.widgetConfig.sanitizerConfig)
                                if (DOMPurify.removed.length >= 1) {
                                    console.warn("Purified Content:", DOMPurify.removed);
                                }
                                this.notifyConfigUpdate();
                            }
                        }
                    }
                }])
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        width: 1024px;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                        padding: 0;
                        overflow: visible;
                        height: 60vh;
                    }
                </style>
            `)
        )
    }

    protected _getMode() {
        return "ace/mode/html";
    }

}
