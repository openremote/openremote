import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";

@customElement("or-info")
export class OrInfo extends LitElement {

  @property({attribute: false})
  public title: string = "";

  @property({attribute: false})
  public description: string = "";

    static get styles() {
        return css`
          or-icon{
            font-size: inherit;
          }
        `;
    }

  protected _showAddingRealmDialog(){
    const dialogActions: DialogAction[] = [
      {
        default: true,
        actionName: "ok",
        content: i18next.t("ok")
      },

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setHeading(this.title)
      .setActions(dialogActions)
      .setContent(html `${this.description}`)
      .setStyles(html`
                        <style>
                            .mdc-dialog__surface {
                              padding: 4px 8px;
                            }
                            #dialog-content {
                                flex: 1;    
                                overflow: visible;
                                min-height: 0;
                                padding: 0;
                            }
                        </style>
                    `)
      .setDismissAction(null));

  }


    render() {
        return html`
          <or-icon icon="information-outline" @click="${() => {this._showAddingRealmDialog()}}"></or-icon>
            
   `;
    }
}
