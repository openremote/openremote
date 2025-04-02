import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {HtmlWidgetConfig} from "../widgets/html-widget";
import { InputType, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import {WidgetSettings} from "../util/widget-settings";
import {i18next} from "@openremote/or-translate";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {createRef, Ref, ref } from "lit/directives/ref.js";
import "@openremote/or-components/or-ace-editor";
import {OrAceEditor} from "@openremote/or-components/or-ace-editor";
import "ace-builds/src-noconflict/mode-html";
import "ace-builds/webpack-resolver";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import DOMPurify from 'dompurify'

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
    
  //or-mwc-dialog {
  //    margin-bottom: 20px;
  //    margin-right: 16px;
  //    width:100%;
  //    height:100%;
  //}
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
                    <or-ace-editor ${ref(editorRef)} .value="${content}" .mode="ace/mode/html" style="height: 60vh; width: 1024px;"></or-ace-editor>
                </div>
            `)
            .setActions([
                {actionName: "cancel", content: "cancel"},
                {actionName: "save", content: "save", action: () => {
                        if (editorRef.value) {
                            const editor = editorRef.value;
                            console.log('Editor:', editor)
                            if (!editor!.validate()) {
                                console.warn("HMTL is not valid");
                                showSnackbar(undefined, i18next.t('errorOccurred'));
                            } else if (editor!.getValue()!.length > 0) {
                                this.widgetConfig.html = DOMPurify.sanitize(editor!.getValue() ?? "", this.widgetConfig.sanitizerConfig)
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
        console.log("generated config:", this.widgetConfig.html)

    }



}
