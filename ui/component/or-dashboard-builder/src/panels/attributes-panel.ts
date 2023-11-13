import {css, CSSResult, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {Asset, AssetModelUtil, Attribute, AttributeRef} from "@openremote/model";
import {style} from "../style";
import {when} from "lit/directives/when.js";
import {map} from "lit/directives/map.js";
import {guard} from "lit/directives/guard.js";
import {i18next} from "@openremote/or-translate";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import manager, {DefaultColor5, Util} from "@openremote/core";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

export class AttributesSelectEvent extends CustomEvent<AttributeRef[]> {

    public static readonly NAME = "attribute-select";

    constructor(attributeRefs: AttributeRef[]) {
        super(AttributesSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attributeRefs
        });
    }
}

const styling = css`
  #attribute-list {
    overflow: auto;
    flex: 1 1 0;
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
`

@customElement('attributes-panel')
export class AttributesPanel extends LitElement {

    @property()
    protected attributeRefs: AttributeRef[] = [];

    @property()
    protected multi: boolean = false;

    @property()
    protected onlyDataAttrs: boolean = false;

    @property()
    protected attributeFilter?: (attribute: Attribute<any>) => boolean;

    @state()
    protected loadedAssets: Asset[] = [];

    static get styles(): CSSResult[] {
        return [styling, style];
    }

    // Lit lifecycle method to compute values during update
    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);

        if (!this.attributeRefs) {
            this.attributeRefs = [];
        }
        if (changedProps.has("attributeRefs") && this.attributeRefs) {
            if(this.attributeRefs.filter(ar => !this.getLoadedAsset(ar)).length > 0) {
                this.loadAssets(this.attributeRefs);
            }
            // Only dispatch event when it CHANGED, so not from 'undefined' to [];
            if(changedProps.get("attributeRefs")) {
                this.dispatchEvent(new AttributesSelectEvent(this.attributeRefs))
            }
        }
    }

    protected getLoadedAsset(attrRef: AttributeRef): Asset | undefined {
        return this.loadedAssets?.find((asset) => asset.id === attrRef.id)
    }

    protected removeWidgetAttribute(attributeRef: AttributeRef) {
        if (this.attributeRefs != null) {
            this.attributeRefs = this.attributeRefs.filter(ar => ar !== attributeRef);
        }
    }

    protected loadAssets(attributeRefs: AttributeRef[]) {
        this.fetchAssets(attributeRefs).then(assets => {
            this.loadedAssets = assets;
        })
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    // TODO: Move this to more generic spot?
    async fetchAssets(attributeRefs: AttributeRef[] = []): Promise<Asset[]> {
        let assets: Asset[] = [];
        await manager.rest.api.AssetResource.queryAssets({
            ids: attributeRefs.map((x: AttributeRef) => x.id) as string[],
            realm: { name: manager.displayRealm },
            select: {
                attributes: attributeRefs.map((x: AttributeRef) => x.name) as string[]
            }
        }).then(response => {
            assets = response.data;
        }).catch((reason) => {
            console.error(reason);
            showSnackbar(undefined, i18next.t('errorOccurred'));
        });
        return assets;
    }

    protected openAttributeSelector(attributeRefs: AttributeRef[], multi: boolean, onlyDataAttrs = true, attributeFilter?: (attribute: Attribute<any>) => boolean) {
        let dialog: OrAttributePicker;
        if (attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setSelectedAttributes(attributeRefs).setShowOnlyDatapointAttrs(onlyDataAttrs).setAttributeFilter(attributeFilter));
        } else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setShowOnlyDatapointAttrs(onlyDataAttrs))
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.attributeRefs = event.detail;
        })
    }

    protected render(): TemplateResult {
        return html`
            <div>
                ${when(this.attributeRefs.length > 0, () => html`

                    <div id="attribute-list">
                        ${guard([this.attributeRefs, this.loadedAssets], () => html`
                            ${map(this.attributeRefs, (attributeRef: AttributeRef) => {
                                const asset = this.getLoadedAsset(attributeRef);
                                if (asset) {
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
                                            <button class="button-clear"
                                                    @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                                <or-icon icon="close-circle"></or-icon>
                                            </button>
                                        </div>
                                    `;
                                } else {
                                    return undefined;
                                }
                            })}
                        `)}
                    </div>

                `, () => html`
                    <span style="padding: 14px 0; display: block;">${i18next.t('noAttributesConnected')}</span>
                `)}

                <!-- Button that opens attribute selection -->
                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('attribute')}" icon="${(this.multi || this.attributeRefs.length === 0) ? "plus" : "swap-horizontal"}"
                              style="margin-top: 8px;"
                              @or-mwc-input-changed="${() => this.openAttributeSelector(this.attributeRefs, this.multi, this.onlyDataAttrs, this.attributeFilter)}">
                </or-mwc-input>
            </div>
        `;
    }

}
