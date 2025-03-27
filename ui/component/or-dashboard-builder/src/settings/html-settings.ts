import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {HtmlWidgetConfig} from "../widgets/html-widget";
import { InputType, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import {WidgetSettings} from "../util/widget-settings";
import {i18next} from "@openremote/or-translate";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {createRef, Ref, ref } from "lit/directives/ref.js";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
    
  or-mwc-dialog {
      margin-bottom: 20px;
      margin-right: 16px;
      width:100%;
      height:100%;
  }
`;

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
                        <or-mwc-input .type="${InputType.BUTTON}" label="Custom HTML" icon="language-html5" @or-mwc-input-changed="${() => this.openHtmlInputDialog(this.widgetConfig.html)}"></or-mwc-input>
                    </div
                    <div>
                        <a href="https://wysiwyghtml.com/" target="_blank" rel="nofollow">Online&nbsp;HTML&nbsp;markup&nbsp;editor</a>
                    </div>
                </settings-panel>
            </div>
        `;
    }



    protected openHtmlInputDialog(content?: string) {
        const reference: Ref<OrMwcInput> = createRef();
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("Insert18there"))
            .setContent(()=> html `
                    <div>
                        <or-mwc-input
                                style="width:100%;"
                                ${ref(reference)}
                                .type="${InputType.TEXTAREA}" 
                                .label="${i18next.t('input label')}"
                                resizeVertical
                                fullwidth
                                fullheight
                                .value="${content}"
                        ></or-mwc-input>
                    </div>
                `)
                .setStyles(html`    
                    <style>
                        .mdc-dialog__surface {
                            width: 1200px;
                        }
    
                        #dialog-content {
                            padding: 24px;
                        }
                    </style>
                `)
                .setActions([{
                    actionName: "cancel",
                    content: "cancel"
                }, {
                    actionName: "ok",
                    content: "ok",
                    action: () => {
                        if (reference.value?.value) {
                            this.widgetConfig.html = reference.value.value
                            this.notifyConfigUpdate();
                        }
                    }
                }])
        )
        console.log("generated config:", this.widgetConfig.html)

    }

}
