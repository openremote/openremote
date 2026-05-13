import {customElement, property, state} from "lit/decorators.js";
import {AttributePicker, AttributePickerPickedEvent} from "./attribute-picker";
import {PropertyValues, html, unsafeCSS} from "lit";
import {DefaultColor5, Util} from "@openremote/core";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {AssetDescriptor, AssetModelUtil, AssetTypeInfo, AttributeDescriptor, WellknownMetaItems} from "@openremote/model";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import "./assettype-list";

/**
 * Custom Event that is dispatched upon closing the dialog.
 * Contains a map that is keyed by {@link AssetDescriptor.name}, with an array of {@link AttributeDescriptor}s of the selected attributes.
 */
export class OrAssetTypeAttributePickerPickedEvent extends AttributePickerPickedEvent {

    public static readonly NAME = "or-asset-type-attribute-picker-picked";

    constructor(attrDescriptors: Map<string, AttributeDescriptor[]>) {
        super(OrAssetTypeAttributePickerPickedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attrDescriptors
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAssetTypeAttributePickerPickedEvent.NAME]: OrAssetTypeAttributePickerPickedEvent;
    }
}

/**
 * The "Attribute Picker" component using the {@link OrAssetTree} component for selecting assets and its attributes.
 *
 * @attribute {object} assetTypeFilter -Callback method for consumers to filter the asset type list shown. Returning true will make the asset type visible, returning false hides it.
 * @attribute {object} attributeFilter - Callback method for consumers to filter the attribute list shown. Returning true will make the attribute visible, returning false hides it.
 */
@customElement("or-assettype-attribute-picker")
export class OrAssetTypeAttributePicker extends AttributePicker {

    @property()
    public assetTypeFilter?: (descriptor: AssetDescriptor) => boolean;

    @property()
    public attributeFilter?: (descriptor: AttributeDescriptor) => boolean;

    public selectedAttributes: Map<string, AttributeDescriptor[]> = new Map();

    @state()
    protected _selectedAssetType?: string;

    @state()
    protected _loadedAttributeTypes?: AttributeDescriptor[];

    @state()
    protected _loadedAssetTypes?: AssetDescriptor[];

    public setSelectedAttributes(selectedAttributes: Map<string, AttributeDescriptor[]>) {
        this.selectedAttributes = selectedAttributes;
        return this;
    }

    protected willUpdate(changedProps: PropertyValues) {

        if (!this._loadedAssetTypes) {
            this._loadedAssetTypes = this._loadAssetTypes();
        }

        if (changedProps.has("_selectedAssetType")) {
            const descriptor = this._getAssetDescriptorByName(this._selectedAssetType);
            if (descriptor) {
                this._loadedAttributeTypes = this._loadAttributeTypes(descriptor);
            }
        }
        // Update UI manually
        this._updateDialogContent();
        this._updateDialogActions();

        return super.willUpdate(changedProps);
    }


    /**
     * Function that will load and update the available asset types up for selection.
     * Also applies the filtering such as {@link assetTypeFilter} if set.
     */
    protected _loadAssetTypes(): AssetDescriptor[] {
        let assetTypes = AssetModelUtil.getAssetDescriptors();
        if (this.assetTypeFilter !== undefined) {
            assetTypes = assetTypes.filter(this.assetTypeFilter);
        }
        this._loadedAssetTypes = assetTypes;
        return assetTypes || [];
    }


    /**
     * Function that will load and update the available attributes up for selection.
     * The {@link descriptor} parameter is usually the selected asset type.
     * Also applies the filtering such as {@link showOnlyDatapointAttrs}, {@link showOnlyRuleStateAttrs} and {@link attributeFilter} if set.
     */
    protected _loadAttributeTypes(descriptor: AssetDescriptor): AttributeDescriptor[] {
        const typeInfo: AssetTypeInfo = (AssetModelUtil.getAssetTypeInfo(descriptor) as AssetTypeInfo);
        let descriptors: AttributeDescriptor[] | undefined = typeInfo.attributeDescriptors;

        // Apply necessary filtering
        if (this.attributeFilter !== undefined) {
            descriptors = descriptors?.filter(this.attributeFilter);
        }
        if (this.showOnlyDatapointAttrs) {
            descriptors = descriptors?.filter(d => d.meta?.[WellknownMetaItems.STOREDATAPOINTS]);
        }
        if (this.showOnlyRuleStateAttrs) {
            descriptors = descriptors?.filter(d => d.meta?.[WellknownMetaItems.RULESTATE]);
        }

        this._loadedAttributeTypes = descriptors || [];
        return descriptors || [];
    }


    /* ----------------------------- */


    protected _setDialogContent(): void {

        const assetTypes = this._loadedAssetTypes || this._loadAssetTypes();
        const selectedTypeNames = this.selectedAttributes ? Array.from(this.selectedAttributes.keys()) : undefined;
        const assetTypeItems = this._getAssetTypeDescriptors(assetTypes, assetTypes.filter(type => !selectedTypeNames || selectedTypeNames.includes(type.name!)));
        const assetDescriptor = this._getAssetDescriptorByName(this._selectedAssetType);
        const attributeTypes = (this._loadedAttributeTypes || (assetDescriptor ? this._loadAttributeTypes(assetDescriptor) : undefined))?.sort(Util.sortByString(item => item.name!));

        this.content = () => html`
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 320px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <asset-type-list .listItems="${assetTypeItems}" style="--or-icon-fill: #000000;" @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {
                        if (evt.detail.length === 1) this._onAssetTypeItemClick(evt.detail[0] as ListItem);
                    }}"
                    ></asset-type-list>
                </div>
                <div class="col" style="flex: 1 1 auto;width: 320px;overflow: auto;">
                    ${when(attributeTypes && attributeTypes.length > 0, () => {
                        const selectedAttrNames = this._selectedAssetType ? this.selectedAttributes.get(this._selectedAssetType)?.map(desc => desc.name!) : undefined;
                        return html`
                            <div class="attributes-header">
                                <or-translate value="attribute_plural"></or-translate>
                            </div>
                            ${until(
                                    this._getAttributesTemplate(undefined, attributeTypes, selectedAttrNames, this.multiSelect, (attrNames) => this._onAttributesSelect(attrNames)),
                                    html`<or-loading></or-loading>`
                            )}
                        `;
                    }, () => html`
                        <div style="display: flex;align-items: center;text-align: center;height: 100%;padding: 0 20px;">
                            <span style="width:100%">
                                <or-translate value="${
                                        (attributeTypes && attributeTypes.length === 0) ?
                                                ((this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) ? "noDatapointsOrRuleStateAttributes" :
                                                                this.showOnlyDatapointAttrs ? "noDatapointsAttributes" :
                                                                        this.showOnlyRuleStateAttrs ? "noRuleStateAttributes" : "noAttributesToShow"
                                                ) : "selectAssetTypeOnTheLeft"}">
                                </or-translate>
                            </span>
                        </div>
                    `)}
                </div>
            </div>
        `;
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
                        this.dispatchEvent(new OrAssetTypeAttributePickerPickedEvent(this.selectedAttributes));
                    }
                }
            }
        ];
    }


    /**
     * HTML Callback function when the selected asset type has been updated.
     */
    protected _onAssetTypeItemClick(listItem: ListItem) {
        this._selectedAssetType = (listItem.data as AssetDescriptor).name;
    }


    /**
     * HTML callback function when the selected attributes have been updated.
     */
    protected _onAttributesSelect(attrNames: string[]) {
        if (!this._selectedAssetType) {
            console.warn("Could not select attribute, since no asset type seemed to be selected.");
            return;
        }
        if (!this._loadedAttributeTypes) {
            console.warn("Could not select attribute, since the attribute list seems to be empty?");
            return;
        }
        this.selectedAttributes.set(this._selectedAssetType, this._loadedAttributeTypes?.filter(desc => attrNames.includes(desc.name!)));
    }


    /**
     * Function that maps the {@link AssetDescriptor}s to the formatted {@link ListItem}s.
     * Uses helpers like {@link Util.getAssetTypeLabel} and sorts by {@link descriptorType} so that agents show up first.
     */
    protected _getAssetTypeDescriptors(descriptors: AssetDescriptor[], selected?: AssetDescriptor[], withNoneValue?: ListItem): ListItem[] {
        const items: ListItem[] = descriptors?.map((descriptor) => {
            return {
                styleMap: {
                    "--or-icon-fill": descriptor.colour ? "#" + descriptor.colour : "unset"
                },
                icon: descriptor.icon,
                trailingIcon: selected?.includes(descriptor) ? 'cloud-upload-outline' : undefined,
                text: Util.getAssetTypeLabel(descriptor),
                value: descriptor.name!,
                data: descriptor
            } as ListItem;
        }).sort((a, b) => (a.data.descriptorType === "agent" ? 0 : 1) - (b.data.descriptorType === "agent" ? 0 : 1) || a.text!.localeCompare(b.text!));

        if (withNoneValue) {
            items?.splice(0, 0, withNoneValue);
        }
        return items;
    }


    /**
     * Utility method to get the cached {@link AssetDescriptor} by its name
     */
    protected _getAssetDescriptorByName(name?: string): AssetDescriptor | undefined {
        return name ? this._loadedAssetTypes?.find(at => at.name === name) : undefined;
    }
}
