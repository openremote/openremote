import { html, LitElement, css, PropertyValues } from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-realm-card";
import {customElement, property} from "lit/decorators.js";
import manager, { DEFAULT_ICONSET, DefaultColor5, ManagerRealmConfig } from "@openremote/core";
import { Realm } from "@openremote/model";
import { OrMwcDialog, showDialog, DialogAction } from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";

@customElement("or-conf-realm")
export class OrConfRealm extends LitElement {

  static styles = css`
    `;

  @property({attribute: false})
  public realms: { [name: string]: ManagerRealmConfig } = {};

  protected _availableRealms: Realm[] = [];
  protected _allRealms: Realm[] = [];

  protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
    const app = this
    manager.rest.api.RealmResource.getAccessible().then((response)=>{
      app._allRealms = response.data as Realm[];
      app._allRealms.push({displayName: 'Default', name:'default'})
      app._loadListOfAvailableRealms()
    });
  }

  protected _removeRealm(realm:string){
    delete this.realms[realm]
    this._loadListOfAvailableRealms()
    this.requestUpdate()
  }

  protected _loadListOfAvailableRealms(){
    const app = this
    this._availableRealms = this._allRealms.filter(function(realm){
      if (realm.name){
        if (app.realms[realm.name] === undefined){
          return realm
        }
      }
      return null
    })
  }

  protected _showAddingRealmDialog(){
    let selectedRealm = "";
    const _AddRealmToView =  () => {
      this.realms[selectedRealm] = this.realms["default"] ? this.realms["default"] : {}
      this._loadListOfAvailableRealms()
      this.requestUpdate()
    }
    const dialogActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: i18next.t("cancel")
      },
      {
        default: true,
        actionName: "ok",
        content: i18next.t("ok"),
        action: _AddRealmToView
      },

    ];
    const dialog = showDialog(new OrMwcDialog()
      .setActions(dialogActions)
      .setContent(html `
        <or-mwc-input label="Realm" @or-mwc-input-changed="${(e: OrInputChangedEvent) => selectedRealm = e.detail.value}" .type="${InputType.SELECT}" .options="${Object.entries(this._availableRealms).map(([key, value]) => {return [value.name, value.displayName]})}"></or-mwc-input>
      `)
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



  updated(changedProperties: PropertyValues) {
    super.updated(changedProperties);
  }

  render() {
    const app = this;
    return html`
      <div class="panels">
        ${Object.entries(this.realms === undefined ? {} : this.realms).map(function([key , value]){
          return html`<or-conf-realm-card .name="${key}" .realm="${value}" .onRemove="${() => {app._removeRealm(key)}}"></or-conf-realm-card>`
        })}
      </div>
      
      <or-mwc-input class="btn-add-realm" id="name-input" .type="${InputType.BUTTON}" label="Add Realm" icon="plus" @click="${() => this._showAddingRealmDialog()}"></or-mwc-input>
    `
  }
}
