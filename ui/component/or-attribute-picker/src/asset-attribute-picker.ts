import {customElement, property, state} from "lit/decorators.js";
import {AttributePicker, AttributePickerPickedEvent} from "./attribute-picker";
import {Asset, Attribute, AttributeRef, WellknownMetaItems} from "@openremote/model";
import manager, {DefaultColor5, Util} from "@openremote/core";
import {OrAssetTree, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {html, unsafeCSS} from "lit";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";

/**
 * Custom Event that is dispatched upon closing the dialog.
 * Contains a list of {@link AttributeRef} of the selected attributes.
 */
export class OrAssetAttributePickerPickedEvent extends AttributePickerPickedEvent {

    public static readonly NAME = "or-asset-attribute-picker-picked";

    constructor(attrRefs: AttributeRef[]) {
        super(OrAssetAttributePickerPickedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attrRefs
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAssetAttributePickerPickedEvent.NAME]: OrAssetAttributePickerPickedEvent;
    }
}

/**
 * The "Attribute Picker" component using the {@link OrAssetTree} component for selecting assets and its attributes.
 *
 * @attribute {object} attributeFilter - Callback method for consumers to filter the attribute list shown. Returning true will make the attribute visible, returning false hides it.
 */
@customElement("or-asset-attribute-picker")
export class OrAssetAttributePicker extends AttributePicker {

    @property()
    public attributeFilter?: (attribute: Attribute<any>) => boolean;

    @state()
    protected _assetAttributes?: (Attribute<any>)[];

    public selectedAssets: string[] = [];
    public selectedAttributes: AttributeRef[] = [];

    protected _asset?: Asset;


    public setAttributeFilter(attributeFilter: ((attribute: Attribute<any>) => boolean) | undefined): this {
        this.attributeFilter = attributeFilter;
        return this;
    }

    public setSelectedAssets(selectedAssets: string[]): this {
        this.selectedAssets = selectedAssets;
        this._updateDialogContent();
        return this;
    }

    public setSelectedAttributes(selectedAttributes: AttributeRef[]): this {
        this.selectedAttributes = selectedAttributes;
        if(this.addBtn) {
            this.addBtn.disabled = this.selectedAttributes.length === 0;
        }
        return this;
    }

    protected _setDialogActions(): void {
        this.actions = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                actionName: "add",
                content: html`
                    <or-mwc-input id="add-btn" class="button" label="add" .type="${InputType.BUTTON}"></or-mwc-input>`,
                action: () => {
                    if (!this.addBtn.disabled) {
                        this.dispatchEvent(new OrAssetAttributePickerPickedEvent(this.selectedAttributes));
                    }
                }
            }
        ];
    }

    protected _setDialogContent(): void {

        console.log("asset-attribute-picker assets;", this.selectedAssets);
        console.log("asset-attribute-picker attributes;", this.selectedAttributes);
        console.log("asset-attribute-picker assetattributes;", this._assetAttributes);

        this.content = () => html`
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 260px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <or-asset-tree id="chart-asset-tree" readonly .selectedIds="${this.selectedAssets.length > 0 ? this.selectedAssets : null}"
                                   @or-asset-tree-selection="${(ev: OrAssetTreeSelectionEvent) => this._onAssetSelectionChanged(ev)}">
                    </or-asset-tree>
                </div>
                <div class="col" style="flex: 1 1 auto;width: 260px;overflow: auto;">
                    ${when(this._assetAttributes && this._assetAttributes.length > 0, () => {
            const selectedNames = this.selectedAttributes.filter(attrRef => attrRef.id === this._asset?.id).map(attrRef => attrRef.name!);
            return html`
                            <div class="attributes-header">
                                <or-translate value="attribute_plural"></or-translate>
                            </div>
                            ${until(
                this._getAttributesTemplate(this._assetAttributes!, undefined, selectedNames, this.multiSelect, (attrNames) => this._onAttributesSelect(attrNames)),
                html`<or-loading></or-loading>`
            )}
                        `
        }, () => html`
                        <div style="display: flex;align-items: center;text-align: center;height: 100%;padding: 0 20px;">
                            <span style="width:100%">
                                <or-translate value="${
            (this._assetAttributes && this._assetAttributes.length === 0) ?
                ((this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) ? "noDatapointsOrRuleStateAttributes" :
                        this.showOnlyDatapointAttrs ? "noDatapointsAttributes" :
                            this.showOnlyRuleStateAttrs ? "noRuleStateAttributes" : "noAttributesToShow"
                ) : "selectAssetOnTheLeft"}">
                                </or-translate>
                            </span>
                        </div>
                    `)}
                </div>
        `;
    }


    /**
     * Event callback function that is triggered once a user selects an asset.
     * It fetches the attributes of that specific asset, and caches these to be displayed later.
     * Also applies the filtering such as {@link showOnlyDatapointAttrs}, {@link showOnlyRuleStateAttrs} and {@link attributeFilter} if set.
     */
    protected async _onAssetSelectionChanged(event: OrAssetTreeSelectionEvent) {
        this._assetAttributes = undefined;
        if (!this.multiSelect) {
            this.selectedAttributes = [];
        }
        // Disable buttons / elements if necessary
        this.addBtn.disabled = this.selectedAttributes.length === 0;
        const assetTree = event.target as OrAssetTree;
        assetTree.disabled = true;

        let selectedAsset = event.detail.newNodes.length === 0 ? undefined : event.detail.newNodes[0].asset;
        this._asset = selectedAsset;

        if (selectedAsset) {

            // Fetch the asset in full, including all its attributes
            const assetResponse = await manager.rest.api.AssetResource.get(selectedAsset.id!);
            selectedAsset = assetResponse.data;

            if (selectedAsset) {
                this._assetAttributes = Object.values(selectedAsset.attributes!)
                    .map(attr => ({...attr, id: selectedAsset!.id!}))
                    .sort(Util.sortByString((attribute) => attribute.name!));

                if (this.attributeFilter) {
                    this._assetAttributes = this._assetAttributes.filter((attr) => this.attributeFilter!(attr))
                }

                if (this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) {
                    this._assetAttributes = this._assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyDatapointAttrs) {
                    this._assetAttributes = this._assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyRuleStateAttrs) {
                    this._assetAttributes = this._assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                }
            }
        }
        // Enable interaction with the asset tree again
        assetTree.disabled = false;
    }


    /**
     * HTML Callback function when the selected attributes have been updated.
     *
     * @remarks
     * The {@link attrNames} parameter contains a list of attribute names VISIBLE in the list.
     * So, selected attributes of other assets, will merge together with the new {@link attrNames}.
     */
    protected _onAttributesSelect(attrNames: string[]) {
        this.setSelectedAttributes([
            ...this.selectedAttributes.filter(attributeRef => attributeRef.id !== this._asset!.id),
            ...attrNames.map(a => ({id: this._asset?.id, name: a}))
        ]);
    }
}
