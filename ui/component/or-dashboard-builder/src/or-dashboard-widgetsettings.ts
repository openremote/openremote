import {Asset, AssetModelUtil, AttributeRef, DashboardWidget } from "@openremote/model";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType } from "../../or-mwc-components/lib/or-mwc-input";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAttributePicker, OrAttributePickerPickedEvent } from "@openremote/or-attribute-picker";
import {until} from "lit/directives/until.js";
import {style} from './style';
import { getAssetDescriptorIconTemplate } from "@openremote/or-icon";
import {DefaultColor5, manager } from "@openremote/core";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

//language=css
const widgetSettingsStyling = css`
    #attribute-list {
        overflow: auto;
        flex: 1 1 0;
        min-height: 150px;
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }

    .button-clear {
        background: none;
        visibility: hidden;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .attribute-list-item:hover .button-clear {
        visibility: visible;
    }

    .button-clear:hover {
        --or-icon-fill: var(--or-app-color4);
    }

    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        line-height: 16px;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    .attribute-list-item .button.delete {
        display: none;
    }

    .attribute-list-item:hover .button.delete {
        display: block;
    }
`

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [unsafeCSS(tableStyle), widgetSettingsStyling, style]
    }

    @property({type: Object})
    protected selectedWidget: DashboardWidget | undefined;

    @state()
    protected loadedAssets: Asset[] | undefined;

    @state()
    protected expandedPanels: string[];

    constructor() {
        super();
        this.expandedPanels = ['Attributes'];
    }

    updated(changedProperties: Map<string, any>) {
        super.updated(changedProperties);
        console.log(changedProperties);
        if(changedProperties.has("selectedWidget")) {
            if(this.selectedWidget != null) {
                if(this.selectedWidget.dataConfig == null) {
                    this.selectedWidget.dataConfig = {};
                }
                if(this.selectedWidget.dataConfig.attributes == null) {
                    this.selectedWidget.dataConfig.attributes = [];
                }
                this.loadAssets();
            }
        }
    }

    loadAssets() {
        manager.rest.api.AssetResource.queryAssets({
            ids: this.selectedWidget?.dataConfig?.attributes?.map((x) => { return x.id; }) as string[]
        }).then(response => {
            this.loadedAssets = response.data;
        })
    }

    expandPanel(panelName: string): void {
        if(this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if(indexOf > -1) { this.expandedPanels.splice(indexOf, 1); }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }

    deleteSelected() {
        this.dispatchEvent(new CustomEvent("delete", {detail: this.selectedWidget }));
    }

    openDialog() {
        let dialog: OrAttributePicker;
        if(this.selectedWidget?.dataConfig?.attributes != null) {
            dialog = showDialog(new OrAttributePicker()) //.setShowOnlyDatapointAttrs(true)) //.setMultiSelect(true).setSelectedAttributes(this.selectedWidget?.dataConfig?.attributes))
        } else {
            dialog = showDialog(new OrAttributePicker())
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.setWidgetAttributes(event.detail);
        })
    }

    setWidgetAttributes(selectedAttrs?: AttributeRef[]) {
        if(this.selectedWidget?.dataConfig?.attributes != null) {
            selectedAttrs?.forEach((attr) => {
                this.selectedWidget?.dataConfig?.attributes?.push(attr);
            });
            this.requestUpdate("selectedWidget");
        }
    }

    removeWidgetAttribute(attributeRef: AttributeRef) {
        if(this.selectedWidget?.dataConfig?.attributes != null) {
            this.selectedWidget.dataConfig.attributes.splice(this.selectedWidget.dataConfig.attributes.indexOf(attributeRef), 1);
            this.requestUpdate("selectedWidget");
        }
    }

    protected render() {
        return html`
            <div>
                <div id="settings">
                    <div id="settings-panels">
                        <div>
                            <button style="display: flex; align-items: center; padding: 12px; background: #F0F0F0; width: 100%; border: none;" @click="${() => { this.expandPanel('Attributes'); }}">
                                <or-icon icon="${this.expandedPanels.includes('Attributes') ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                <span style="margin-left: 6px;">Attributes</span>
                            </button>
                        </div>
                        <div>
                            ${this.expandedPanels.includes('Attributes') ? html`
                                <div style="padding: 12px;">
                                    ${(this.selectedWidget?.dataConfig?.attributes == null || this.selectedWidget.dataConfig.attributes.length == 0) ? html`
                                        <span>No attributes connected.</span>
                                    ` : undefined}
                                    <div id="attribute-list">
                                        ${(this.selectedWidget?.dataConfig?.attributes != null && this.loadedAssets != null) ? this.selectedWidget.dataConfig.attributes.map((attributeRef) => {
                                            const asset = this.loadedAssets?.find((x: Asset) => { return x.id == attributeRef.id; }) as Asset;
                                            return (asset != null) ? html`
                                                <div class="attribute-list-item">
                                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type), undefined, undefined)}</span>
                                                    <div class="attribute-list-item-label">
                                                        <span>${asset.name}</span>
                                                        <span style="font-size:14px; color:grey;">${attributeRef.name}</span>
                                                    </div>
                                                    <button class="button-clear" @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                                        <or-icon icon="close-circle" ></or-icon>
                                                    </button>
                                                </div>
                                            ` : undefined;
                                        }) : undefined}
                                    </div>
                                    <or-mwc-input .type="${InputType.BUTTON}" label="Attribute" icon="plus" style="margin-top: 16px;" @click="${() => this.openDialog()}"></or-mwc-input>
                                </div>
                            ` : null}
                        </div>
                        <div>
                            <button style="display: flex; align-items: center; padding: 12px; background: #F0F0F0; width: 100%; border: none;" @click="${() => { this.expandPanel('Display'); }}">
                                <or-icon icon="${this.expandedPanels.includes('Display') ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                <span style="margin-left: 6px;">Display</span>
                            </button>
                        </div>
                        <div>
                            ${this.expandedPanels.includes('Display') ? html`
                            <div style="padding: 12px;">
                                <span>Setting 2</span>
                            </div>
                        ` : null}
                        </div>
                        <div>
                            <div style="display: flex; align-items: center; padding: 12px; background: #F0F0F0;">
                                <or-icon icon="chevron-right"></or-icon>
                                <span style="margin-left: 6px;">Settings</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="actions" style="position: absolute; bottom: 20px; right: 20px;">
                    <or-mwc-input type="${InputType.BUTTON}" outlined icon="delete" label="Delete Component" @click="${() => { this.deleteSelected(); }}"></or-mwc-input>
                </div>
            </div>
        `
    }
}
