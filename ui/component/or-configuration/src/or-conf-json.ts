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
export class OrConfRealm extends LitElement {

  static get styles() {
    return css`
    :host {
        display: flex;
        width: 100%;
        height: 100%;
    }

    @media screen and (max-width: 1400px) {
        :host > * {
            flex-grow: 0;
        }

        :host {
            flex-direction: column;
        }
    }
`;
  }

  @property({attribute: false})
  public managerConfig: ManagerAppConfig = {};

  protected _aceEditor: Ref<OrAceEditor> = createRef();


  protected _showManagerConfigDialog(){
    const dialogActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: i18next.t("cancel")
      },
      {
        default: true,
        actionName: "ok",
        content: i18next.t("ok")
      },

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setActions(dialogActions)
      .setContent(html `<or-ace-editor ${ref(this._aceEditor)} .value="${this.managerConfig}"></or-ace-editor>`)
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
