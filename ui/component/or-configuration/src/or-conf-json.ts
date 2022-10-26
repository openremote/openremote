import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import { i18next } from "@openremote/or-translate";
import { DialogAction } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { ManagerAppConfig } from "@openremote/core";
import { createRef, Ref, ref } from "lit/directives/ref.js";
import { OrAceEditor } from "@openremote/or-components/or-ace-editor";

@customElement("or-conf-json")
export class OrConfJson extends LitElement {

  @property({attribute: false})
  public managerConfig: ManagerAppConfig = {};

  protected _aceEditor: Ref<OrAceEditor> = createRef();

  public beforeSave() {
    if (!this._aceEditor.value) {
      return;
    }
    const value = this._aceEditor.value.getValue()
    this.managerConfig = JSON.parse(value ? value : "");
  }

  protected _showManagerConfigDialog(){
    const _saveConfig = ()=>{
      this.beforeSave()
      document.dispatchEvent(
        new CustomEvent('saveManagerConfig',
          {detail: {value: this.managerConfig}}
        )
      )
    }
    const dialogActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: i18next.t("cancel")
      },
      {
        actionName: "ok",
        content: i18next.t("save"),
        action: _saveConfig
      },

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setActions(dialogActions)
      .setHeading("manager_config.json")
      .setContent(html `<or-ace-editor ${ref(this._aceEditor)} .value="${this.managerConfig}" ></or-ace-editor>`)
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
      .setDismissAction(null));

  }

  render() {
    return html`
      <or-mwc-input type="button" label="JSON" outlined icon="pencil" @click="${() => {this._showManagerConfigDialog()}}"></or-mwc-input>
    `
  }



}
