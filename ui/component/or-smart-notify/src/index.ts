import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";

import moment from "moment";

import {icon} from "@fortawesome/fontawesome-svg-core";
import {faClock} from "@fortawesome/free-regular-svg-icons";
import {faTimes} from "@fortawesome/free-solid-svg-icons";

import {AssetQuery, ClientRole} from "@openremote/model";
import manager from "@openremote/core";

@customElement("or-smart-notify")
class OrSmartNotify extends LitElement {
    // default function on smartyNotify
    @property({type: Function})
    private onChange: any;

    // if smart notify popup is visible
    @property({type: Boolean})
    private isVisible: boolean = false;

    // If smart notify is active
    @property({type: Boolean})
    private isActive: boolean = false;

    // If smart notify is active
    @property({type: Boolean})
    private isDisabled: boolean = false;

    // If smart notify is active
    @property({type: Object})
    private smartNotify: any = {};

    // default function on smartyNotify
    @property({type: String})
    private currentTime: string = "-";

    constructor() {
        super();
        const self: any = this;
        this.currentTime = moment().format("HH:mm");

        setInterval(function() {
            self.currentTime = moment().format("HH:mm");
        }, 500);
    }

    public getSmartNotify() {
        const self: any = this;
        return new Promise((resolve: any, reject: any) => {
            const smartNotifyQuery: AssetQuery = {
                names: [{predicateType: "string", value: "SMART_NOTIFY_ASSET"}]
            };
            manager.rest.api.AssetResource.queryAssets(smartNotifyQuery).then((response: any) => {
                console.log("Setting Smart Notify");
                if (response.data) {
                    self.smartNotify = response.data[0];
                    self.isActive = self.smartNotify.attributes.SMART_NOTIFY_ENABLED.value;
                    self.onChange();
                    resolve(response);
                }

            }).catch((reason: any) =>  {
                reject(Error("Error:" + reason));
                console.log("Error:" + reason);
            });

        });
    }

    public checkSmartNotifyMarkers() {
        const self: any = this;
        if (this.isActive && this.smartNotify.attributes.SMART_NOTIFY_RESULT.value.markers && this.smartNotify.attributes.SMART_NOTIFY_RESULT.value.markers.length > 0) {
        } else if (this.isActive && this.smartNotify.attributes.SMART_NOTIFY_RESULT.value.markers.length === 0) {
            this.getSmartNotify().then(() => {
                setTimeout(() => {
                    self.checkSmartNotifyMarkers();
                }, 3000);
            });
        }
    }

    public setSmartNotify(e: any) {
        const isChecked = e.target.checked;
        if (isChecked) {
            this.smartNotify.attributes.SMART_NOTIFY_ENABLED.value = moment();
            this.smartNotify.attributes.SMART_NOTIFY_ENABLED.timestamp = moment();
            this.isActive = true;
        } else {
            this.smartNotify.attributes.SMART_NOTIFY_ENABLED.value = null;
            this.isActive = false;
        }

        manager.rest.api.AssetResource.update(this.smartNotify.id, this.smartNotify).then((response: any) => {
            console.log("Setting Smart Notify");
            this.smartNotify.version = this.smartNotify.version + 1;
            if (isChecked) {
                this.checkSmartNotifyMarkers();
            } else {

                // Clear the smart notify markers and smart notify in frontend
                this.smartNotify.attributes.SMART_NOTIFY_ENABLED.value = false;
                this.smartNotify.attributes.SMART_NOTIFY_RESULT.value.markers = [];
                this.onChange();
            }

        }).catch((reason: any) => console.log("Error:" + reason));
    }

    public close() {
        this.isVisible = false;
    }

    public toggleVisibility() {
        this.isVisible = !this.isVisible;
    }
    protected render() {
        // language=HTML
        return html`
              <style>
                    #smartNotifyControlPanel {
                        position: fixed;
                        left: 20px;
                        bottom: 146px;
                        width: 315px;
                        z-index: 2;
                        background-color: #FFFFFF;
                            
                        -webkit-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                        -moz-box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                        box-shadow: 1px 1px 2px 0px rgba(0,0,0,0.28);
                    }
                    .layout.horizontal {
                        display: -ms-flexbox;
                        display: -webkit-flex;
                        display: flex;
                        
                            -ms-flex-direction: row;
                        -webkit-flex-direction: row;
                        flex-direction: row;
                        
                        
                        -ms-flex-align: center;
                        -webkit-align-items: center;
                        align-items: center;
                        
                        -ms-flex-pack: center;
                        -webkit-justify-content: center;
                        justify-content: center;
                    }
                    
                    .t-center {
                        text-align: center;
                    }
                    
                    .controller-timeline{
                        padding: 5px 20px;
                        height: 32px;
                        line-height: 32px;
                        border-bottom: solid 1px #dedede;
                    }
                    
                    .flex {
                        -ms-flex: 1 1 0.000000001px;
                        -webkit-flex: 1;
                        flex: 1;
                        -webkit-flex-basis: 0.000000001px;
                        flex-basis: 0.000000001px;
                    }
                    
                    .padding-2-5 {
                        padding: 2px 5px;
                    }
                    .padding-10 {
                        padding: 10px;
                    }
                    #smart-notify-toggle {
                        margin-top: 29px;
                    }
                    
                    .smart-notify-toggle-label {
                        color: #FFFFFF;
                        background-color: #00477c;
                        border-top-left-radius: 5px;
                        border-top-right-radius: 5px;
                        padding: 2px 8px;
                        font-size: 12px;
                        height: 19px;
                    }
                    
                    .smart-notify-toggle-icon {
                        background-color: #555753;
                        border-bottom-left-radius: 5px;
                        border-bottom-right-radius: 5px;
                        margin-top: 2px;
                        height: 48px;
                    }

                    .smart-notify-toggle-icon[active] {
                        background-color: #00477c;
                    }
                    
                    icon {
                        height: 20px;
                        width: 20px;
                   }     
                         
                  icon .svg-inline--fa {
                        height: 20px;
                        width: 20px;
                  }
                  
                   icon[large] {
                        height: 28px;
                        width: 28px;
                }
                   
                icon[large] .svg-inline--fa {
                    height: 28px;
                    width: 28px;
                    margin: 10px;
                }
                  
                icon[large] svg path {
                      fill: #FFFFFF;
                      color: #FFFFFF;
                }

                /* The switch - the box around the slider */
                .switch {
                  position: relative;
                  display: inline-block;
                  width: 36px;
                  height: 14px;
                  margin: 10px 0;
                }
                
                /* Hide default HTML checkbox */
                .switch input {
                  opacity: 0;
                  width: 0;
                  height: 0;
                }
                
                /* The slider */
                .slider {
                  position: absolute;
                  cursor: pointer;
                  top: 0;
                  left: 0;
                  right: 0;
                  bottom: 0;
                  background-color: #ccc;
                  -webkit-transition: .4s;
                  transition: .4s;
                }
                
                .slider:before {
                    position: absolute;
                    content: "";
                    height: 20px;
                    width: 20px;
                    left: 0;
                    top: -3px;
                    background-color: white;
                    -webkit-transition: .4s;
                    transition: .4s;
                    box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.6);
                }
                
                input:checked + .slider:before {
                  background-color: var(--app-primary-color);
                }
                
                input:focus + .slider {
                }
                
                input:checked + .slider:before {
                  -webkit-transform: translateX(20px);
                  -ms-transform: translateX(20px);
                  transform: translateX(20px);
                }
                
                /* Rounded sliders */
                .slider.round {
                  border-radius: 34px;
                }
                
                .slider.round:before {
                  border-radius: 50%;
                }
              </style>
            
         ${this.isVisible ? html`
            <div id="smartNotifyControlPanel">
                <div class="layout horizontal controller-timeline t-center">
                    <icon>${icon(faClock).node.item(0)}</icon>
                    <div class="flex padding-2-5" style="font-weight: 600;">Smart Notify</div>
                 
                    <a style="cursor: pointer;height: 22px;" @click="${this.close}">
                       <icon>${icon(faTimes).node.item(0)}</icon>
                    </a>
                </div>
                 <div class="layout horizontal">
                    <div style="background-color: var(--app-lightgrey-color, #dedede);" class="flex padding-10">Activeren</div>
                    <div class="flex t-center">
                        <label class="switch">
                          <input @change="${this.setSmartNotify}" ?disabled="${this.isDisabled}" ?checked="${this.isActive}" type="checkbox">
                          <span class="slider round"></span>
                        </label>
                    </div>
                </div>
                ${this.isActive ? html`
                    <div class="layout horizontal">
                        <div style="background-color: var(--app-lightgrey-color, #dedede);" class="flex padding-10">Starttijd</div>
                        <div class="flex t-center">${this.isActive ? moment(this.smartNotify.attributes.SMART_NOTIFY_ENABLED.timestamp).format("HH:mm")  : "-"}</div>
                    </div>
                ` : ``}
            </div>
        ` : ``}
         <div id="smart-notify-toggle" @click="${this.toggleVisibility}">
            <div class="smart-notify-toggle-label">${this.currentTime}</div>
            <div class="smart-notify-toggle-icon" ?active="${this.isActive}">
                <icon large>${icon(faClock).node.item(0)}</icon>
            </div>
        </div>
               
    `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        this.isDisabled = !manager.hasRole(ClientRole.WRITE_ASSETS);
    }

}
