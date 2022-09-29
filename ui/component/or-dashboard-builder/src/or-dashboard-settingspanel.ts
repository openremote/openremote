import manager, { DefaultColor5 } from "@openremote/core";
import {Asset, AssetModelUtil, AttributeRef, DashboardWidget} from "@openremote/model";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {i18next} from "@openremote/or-translate";
import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import { until } from "lit/directives/until.js";
import {OrWidgetConfig} from "./widgets/or-base-widget";
import {style} from "./style";

//language=css
export const widgetSettingsStyling = css`

    /* ------------------------------- */
    .switchMwcInputContainer {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    /* ---------------------------- */
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

    /* ---------------------------- */
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

    /* ---------------------------- */
`

export enum SettingsPanelType {
    SINGLE_ATTRIBUTE, MULTI_ATTRIBUTE,
}


@customElement('or-dashboard-settingspanel')
export class OrDashboardSettingsPanel extends LitElement {

    @property()
    public type?: SettingsPanelType;

    @property()
    public widget?: DashboardWidget;

    @state()
    private loadedAssets?: Asset[];

    static get styles() {
        return [style, widgetSettingsStyling];
    }

    render() {
        const config = this.widget?.widgetConfig;
        switch (this.type) {

            case SettingsPanelType.SINGLE_ATTRIBUTE:
            case SettingsPanelType.MULTI_ATTRIBUTE: {
                if (!config.attributeRefs) {
                    return html`
                        <span>${i18next.t('errorOccurred')}</span>
                    `
                } else {
                    return html`
                        ${until(this.getAttributeHTML(config, (this.type == SettingsPanelType.MULTI_ATTRIBUTE)), html`${i18next.t('loading')}`)}
                    `;
                }
            }
        }
    }


    /* --------------------------------- */

    // UTILITY FUNCTIONS


    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    async getAttributeHTML(config: any, multi: boolean) {
        if (!this.loadedAssets) {
            this.loadedAssets = await this.fetchAssets(config);
        }
        return html`
            <div style="padding: 0 14px 12px 14px;">
                ${(config.attributeRefs == null || config.attributeRefs.length == 0) ? html`
                    <span>${i18next.t('noAttributesConnected')}</span>
                ` : undefined}
                <div id="attribute-list">
                    ${(config.attributeRefs != null && this.loadedAssets != null) ? config.attributeRefs.map((attributeRef: AttributeRef) => {
                        const asset = this.loadedAssets?.find((x: Asset) => {
                            return x.id == attributeRef.id;
                        }) as Asset;
                        return (asset != null) ? html`
                            <div class="attribute-list-item">
                                <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type))}</span>
                                <div class="attribute-list-item-label">
                                    <span>${asset.name}</span>
                                    <span style="font-size:14px; color:grey;">${attributeRef.name}</span>
                                </div>
                                <button class="button-clear"
                                        @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                    <or-icon icon="close-circle"></or-icon>
                                </button>
                            </div>
                        ` : undefined;
                    }) : undefined}
                </div>
                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('attribute')}" icon="plus"
                              style="margin-top: 24px; margin-left: -7px;"
                              @or-mwc-input-changed="${() => this.openDialog(config.attributeRefs, multi)}">
                </or-mwc-input>
            </div>
        `
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required. TODO: Simplify this to only request data needed for attribute list
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if (config.attributeRefs) {
            if (config.attributeRefs != null) {
                let assets: Asset[] = [];
                await manager.rest.api.AssetResource.queryAssets({
                    ids: config.attributeRefs?.map((x: AttributeRef) => {
                        return x.id;
                    }) as string[]
                }).then(response => {
                    assets = response.data;
                }).catch((reason) => {
                    console.error(reason);
                    showSnackbar(undefined, i18next.t('errorOccurred'));
                });
                return assets;
            }
        } else {
            console.error("Error: attributeRefs are not present in widget config!");
        }
    }

    setWidgetAttributes(selectedAttrs?: AttributeRef[]) {
        if (this.widget?.widgetConfig != null) {
            this.widget.widgetConfig.attributeRefs = selectedAttrs;
            this.fetchAssets(this.widget.widgetConfig).then((assets) => {
                this.loadedAssets = assets;
                this.requestUpdate("widget");
                this.forceParentUpdate(new Map<string, any>([["widget", this.widget], ["loadedAssets", assets]]));
            });
        }
    }

    removeWidgetAttribute(attributeRef: AttributeRef) {
        if (this.widget?.widgetConfig?.attributeRefs != null) {
            this.widget.widgetConfig.attributeRefs.splice(this.widget.widgetConfig.attributeRefs.indexOf(attributeRef), 1);
            this.requestUpdate("widget");
            this.forceParentUpdate(new Map<string, any>([["widget", this.widget]]));
        }
    }

    // Opening the attribute picker dialog, and listening to its result. (UI related)
    openDialog(attributeRefs: AttributeRef[], multi: boolean) {
        let dialog: OrAttributePicker;
        if (attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setSelectedAttributes(attributeRefs).setShowOnlyDatapointAttrs(true));
        } else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setShowOnlyDatapointAttrs(true))
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.setWidgetAttributes(event.detail);
        })
    }


}
