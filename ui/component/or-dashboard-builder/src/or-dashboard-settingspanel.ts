import manager, { DefaultColor5, Util } from "@openremote/core";
import {Asset, AssetModelUtil, AttributeRef} from "@openremote/model";
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
        color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
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

    .threshold-list-item:hover .button-clear {
        visibility: visible;
    }
`

export enum SettingsPanelType {
    SINGLE_ATTRIBUTE, MULTI_ATTRIBUTE, THRESHOLDS,
}


@customElement('or-dashboard-settingspanel')
export class OrDashboardSettingsPanel extends LitElement {

    @property()
    private readonly type?: SettingsPanelType;

    @property()
    private readonly widgetConfig: OrWidgetConfig | any;
    private _config?: OrWidgetConfig | any; // local duplicate that does not update parent. Sent back in dispatchEvent() only.

    @property({type: Boolean}) // used for SINGLE_ATTRIBUTE and MULTI_ATTRIBUTE types.
    private readonly onlyDataAttrs: boolean = true;

    @state()
    private loadedAssets?: Asset[];

    static get styles() {
        return [style, widgetSettingsStyling];
    }


    /* ---------------------------- */

    // Setting local duplicate config before triggering update,
    // and fetching assets if any of the AttributeRefs are not pulled yet.
    willUpdate(changedProperties: Map<string, any>) {
        if(changedProperties.has("widgetConfig")) {
            if(!this._config || (changedProperties.get("widgetConfig") != this._config) && this.widgetConfig.attributeRefs) {
                const loadedRefs: AttributeRef[] = this.widgetConfig.attributeRefs.filter((attrRef: AttributeRef) => this.isAttributeRefLoaded(attrRef));
                if(loadedRefs.length != this.widgetConfig.attributeRefs.length) {
                    this.fetchAssets(this.widgetConfig).then(assets => {
                        this.loadedAssets = assets;
                    });
                }
            }
            this._config = (this.widgetConfig ? JSON.parse(JSON.stringify(this.widgetConfig)) : undefined);
        }
    }


    /* ---------------------------------- */

    render() {
        switch (this.type) {

            case SettingsPanelType.SINGLE_ATTRIBUTE:
            case SettingsPanelType.MULTI_ATTRIBUTE: {
                if (!this._config.attributeRefs) {
                    return html`<span>${i18next.t('errorOccurred')}</span>`;
                } else {
                    return html`
                        ${until(this.getAttributeHTML(this._config, (this.type == SettingsPanelType.MULTI_ATTRIBUTE), this.onlyDataAttrs), html`${i18next.t('loading')}`)}
                    `;
                }
            }
            case SettingsPanelType.THRESHOLDS: {
                if (!this._config.thresholds) {
                    return html`<span>${i18next.t('errorOccurred')}</span>`;
                } else {
                    return html`
                        ${until(this.getThresholdsHTML(this._config), html`${i18next.t('loading')}`)}
                    `;
                }
            }
        }
    }


    /* --------------------------------- */

    // UTILITY FUNCTIONS


    // Method to update the Grid. For example after changing a setting.
    updateParent(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    async getAttributeHTML(config: any, multi: boolean, onlyDataAttrs: boolean = true) {
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
                        if(asset) {
                            const attribute = asset.attributes![attributeRef.name!];
                            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                            const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, true);
                            return html`
                                <div class="attribute-list-item">
                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type))}</span>
                                    <div class="attribute-list-item-label">
                                        <span>${asset.name}</span>
                                        <span style="font-size:14px; color:grey;">${label}</span>
                                    </div>
                                    <button class="button-clear" @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                        <or-icon icon="close-circle"></or-icon>
                                    </button>
                                </div>
                            `;
                        } else {
                            return undefined;
                        }
                    }) : undefined}
                </div>
                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('attribute')}" icon="plus"
                              style="margin-top: 24px; margin-left: -7px;"
                              @or-mwc-input-changed="${() => this.openDialog(config.attributeRefs, multi, onlyDataAttrs)}">
                </or-mwc-input>
            </div>
        `
    }

    isAttributeRefLoaded(attrRef: AttributeRef) {
        return this.loadedAssets?.find((asset) => asset.id == attrRef.id)
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if (config.attributeRefs) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[],
                select: {
                    attributes: config.attributeRefs?.map((x: AttributeRef) => x.name) as string[]
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        } else {
            console.error("Error: attributeRefs are not present in widget config!");
        }
    }

    setWidgetAttributes(selectedAttrs?: AttributeRef[]) {
        if (this._config != null) {
            this._config.attributeRefs = selectedAttrs;
            this.fetchAssets(this._config).then((assets) => {
                this.loadedAssets = assets;
                this.updateComplete.then(() => {
                    this.updateParent(new Map<string, any>([["config", this._config], ["loadedAssets", assets]]));
                });
            });
        }
    }

    removeWidgetAttribute(attributeRef: AttributeRef) {
        if (this._config.attributeRefs != null) {
            this._config.attributeRefs.splice(this._config.attributeRefs.indexOf(attributeRef), 1);
            this.updateParent(new Map<string, any>([["config", this._config]]));
        }
    }

    // Opening the attribute picker dialog, and listening to its result. (UI related)
    openDialog(attributeRefs: AttributeRef[], multi: boolean, onlyDataAttrs: boolean = true) {
        let dialog: OrAttributePicker;
        if (attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setSelectedAttributes(attributeRefs).setShowOnlyDatapointAttrs(onlyDataAttrs));
        } else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setShowOnlyDatapointAttrs(onlyDataAttrs))
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.setWidgetAttributes(event.detail);
        })
    }



    /* ---------------------------------------------------- */

    async getThresholdsHTML(config: OrWidgetConfig | any) {
        if(config.thresholds) {
            return html`
                <div id="thresholds-list" style="padding: 0 14px 12px 14px;">
                    ${(config.thresholds as [number, string][]).sort((x, y) => (x[0] < y[0]) ? -1 : 1).map((threshold, index) => {
                        return html`
                            <div class="threshold-list-item" style="padding: 8px 0; display: flex; flex-direction: row; align-items: center;">
                                <div style="height: 100%; padding: 8px 14px 8px 0;">
                                    <or-mwc-input type="${InputType.COLOUR}" value="${threshold[1]}" style="width: 32px; height: 32px;"
                                                  @or-mwc-input-changed="${(event: CustomEvent) => {
                                                      this._config.thresholds[index][1] = event.detail.value;
                                                      this.updateParent(new Map<string, any>([["config", this._config]]));
                                                  }}"
                                    ></or-mwc-input>
                                </div>
                                <or-mwc-input type="${InputType.NUMBER}" comfortable .value="${threshold[0]}" ?disabled="${index == 0}"
                                              .min="${config.min}" .max="${config.max}"
                                              @or-mwc-input-changed="${(event: CustomEvent) => {
                                                  if(event.detail.value >= config.min && event.detail.value <= config.max) {
                                                      this._config.thresholds[index][0] = event.detail.value;
                                                      this.updateParent(new Map<string, any>([["config", this._config]]));
                                                  }
                                              }}"
                                ></or-mwc-input>
                                <button class="button-clear" style="margin-left: 8px; ${index == 0 ? 'visibility: hidden;' : undefined}"
                                        @click="${() => { this.removeThreshold(this._config, threshold); }}">
                                    <or-icon icon="close-circle"></or-icon>
                                </button>
                            </div>
                        `
                    })}
                    <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('threshold')}" icon="plus"
                                  style="margin-top: 24px; margin-left: -7px;"
                                  @or-mwc-input-changed="${() => this.addNewThreshold(this._config)}">
                    </or-mwc-input>
                </div>
            `
        }
    }

    removeThreshold(config: any, threshold: [number, string]) {
        config.thresholds = (config.thresholds as [number, string][]).filter((x) => x != threshold);
        this.updateParent(new Map<string, any>([["config", this._config]]));
    }
    addThreshold(config: any, threshold: [number, string]) {
        (config.thresholds as [number, string][]).push(threshold);
        this.updateParent(new Map<string, any>([["config", this._config]]));
    }
    addNewThreshold(config: any) {
        const suggestedValue = (config.thresholds[config.thresholds.length - 1][0] + 10);
        this.addThreshold(config, [(suggestedValue <= config.max ? suggestedValue : config.max), "#000000"]);
        this.updateComplete.then(() => {
            const elem = this.shadowRoot?.getElementById('thresholds-list') as HTMLElement;
            const inputField = Array.from(elem.children)[elem.children.length - 2] as HTMLElement;
            (inputField.children[1] as HTMLElement).setAttribute('focused', 'true');
        })
    }
}
