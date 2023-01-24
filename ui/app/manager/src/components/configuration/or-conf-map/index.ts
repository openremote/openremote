import { css, html, LitElement, PropertyValues } from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import "./or-conf-map-card";
import { customElement, property } from "lit/decorators.js";
import manager from "@openremote/core";
import { MapConfig } from "@openremote/model";
import { DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-components/or-loading-indicator";


@customElement("or-conf-map")
export class OrConfMap extends LitElement {


  static styles = css`
    #btn-add-realm {
      margin-top: 4px;
    }
  `;

  @property({attribute: false})
  public config: MapConfig = {};

  protected _availableRealms: {name:string, displayName:string}[] = [];
  protected _allRealms: {name:string, displayName:string}[] = [];
  protected _addedRealm: null|string = null

  protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
    const app = this
    manager.rest.api.RealmResource.getAccessible().then((response)=>{
      app._allRealms = response.data as {name:string, displayName:string}[];
      app._allRealms.push({name: 'default', displayName: 'Default'})
      app._loadListOfAvailableRealms()
    });
  }

  protected _removeRealm(realm:string){
    if (this.config){
      delete this.config[realm]
      this._loadListOfAvailableRealms()
      this.requestUpdate()
    }
  }

  protected _loadListOfAvailableRealms(){
    const app = this
    this._availableRealms = this._allRealms.filter(function(realm){
      if (!!realm.name && !!app.config){
        if (realm.name in app.config){
          return null
        }
      }
      return realm
    }).sort(function(a, b){
      if (a.displayName && b.displayName){
        return (a.displayName > b.displayName) ? 1 : -1
      }
      return -1
    })
  }

  protected _showAddingRealmDialog(){
    this._addedRealm = null;
    const _AddRealmToView =  () => {
      if (this._addedRealm){
        if (!this.config){
          this.config = {}
        }
        this.config[this._addedRealm] = {bounds: [4.42, 51.88, 4.55, 51.94], center: [4.485222, 51.911712], zoom: 14, minZoom: 14, maxZoom: 19, boxZoom: false}
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
      .setHeading(i18next.t('configuration.addMapCustomization'))
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
        ${Object.entries(this.config === undefined ? {} : this.config).map(([key , value]) => {
      return html`<or-conf-map-card .expanded="${app._addedRealm === key}" .name="${key}" .map="${value}" .onRemove="${() => {app._removeRealm(key)}}"></or-conf-map-card>`
    })}
      </div>
      
      <or-mwc-input id="btn-add-realm" .type="${InputType.BUTTON}" .label="${i18next.t('configuration.addMapCustomization')}" icon="plus" @click="${() => this._showAddingRealmDialog()}"></or-mwc-input>
    `
  }


}
