import { html, LitElement, css, PropertyValues } from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-realm-card";
import {customElement, property} from "lit/decorators.js";
import manager, { ManagerAppConfig } from "@openremote/core";
import { ManagerConfRealm, Realm } from "@openremote/model";
import { OrMwcDialog, showDialog, DialogAction } from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-components/or-loader";

@customElement("or-conf-realm")
export class OrConfRealm extends LitElement {

  static styles = css`
    #btn-add-realm {
      margin-top: 4px;
    }
    `;

  @property({attribute: false})
  public config: ManagerAppConfig = {};

  protected _availableRealms: Realm[] = [];
  protected _allRealms: Realm[] = [];
  protected _addedRealm: null|string = null

  protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
    const app = this
    manager.rest.api.RealmResource.getAccessible().then((response)=>{
      app._allRealms = response.data as Realm[];
      app._allRealms.push({displayName: 'Default', name:'default'})
      app._loadListOfAvailableRealms()
    });
  }

  protected _removeRealm(realm:string){
    if (this.config.realms){
      delete this.config?.realms[realm]
      this._loadListOfAvailableRealms()
      this.requestUpdate()
    }
  }

  protected _loadListOfAvailableRealms(){
    const app = this
    this._availableRealms = this._allRealms.filter(function(realm){
      if (realm.name && app.config?.realms){
        if (!app.config?.realms[realm.name]){
          return realm
        }
      }
      return null
    }).sort(function(a, b){
      if (a.name && b.name){
        return (a.name > b.name) ? 1 : -1
      }
      return -1
    })
  }

  protected _showAddingRealmDialog(){
    this._addedRealm = null;
    const _AddRealmToView =  () => {
      if (this._addedRealm){
        if (!this.config.realms){
          this.config.realms = {}
        }
        this.config.realms[this._addedRealm] = {
          styles: ":host > * {--or-app-color1:#FFFFFF;--or-app-color2:#F9F9F9;--or-app-color3:#4c4c4c;--or-app-color4:#4d9d2a;--or-app-color5:#CCCCCC;--or-app-color6:#be0000;"
      }
        this._loadListOfAvailableRealms()
        this.requestUpdate()
        return true
      }
      return false
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
      .setHeading(i18next.t('configuration.addRealmCustomization'))
      .setActions(dialogActions)
      .setContent(html `
        <or-mwc-input class="selector" label="Realm" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._addedRealm = e.detail.value}" .type="${InputType.SELECT}" .options="${Object.entries(this._availableRealms).map(([key, value]) => {return [value.name, value.displayName]})}"></or-mwc-input>
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
                            or-mwc-input.selector {
                              width: 300px;
                              display: block;
                              padding: 10px 20px;
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
        ${Object.entries(this.config.realms === undefined ? {} : this.config.realms).map(function([key , value]){
          return html`<or-conf-realm-card .expanded="${app._addedRealm === key}" .name="${key}" .realm="${value}" .onRemove="${() => {app._removeRealm(key)}}"></or-conf-realm-card>`
        })}
      </div>
      
      <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" .label="${i18next.t('configuration.addRealmCustomization')}" icon="plus" @click="${() => this._showAddingRealmDialog()}"></or-mwc-input>
    `
  }
}
