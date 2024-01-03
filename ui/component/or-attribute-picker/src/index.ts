import {html, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-asset-tree";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-mwc-components/or-mwc-list";
import {OrAssetTree, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import manager, {DefaultColor2, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import {Asset, Attribute, AttributeRef, WellknownMetaItems} from "@openremote/model";
import {ListItem, ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DialogAction, DialogActionBase, OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

export class OrAttributePickerPickedEvent extends CustomEvent<AttributeRef[]> {

    public static readonly NAME = "or-attribute-picker-picked";

    constructor(detail: AttributeRef[]) {
        super(OrAttributePickerPickedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAttributePickerPickedEvent.NAME]: OrAttributePickerPickedEvent;
    }
}

@customElement("or-attribute-picker")
export class OrAttributePicker extends OrMwcDialog {

    @property({type: Boolean})
    public showOnlyDatapointAttrs?: boolean = false;

    @property({type: Boolean})
    public showOnlyRuleStateAttrs?: boolean = false;


    @property()
    public attributeFilter?: (attribute: Attribute<any>) => boolean;


    @property({type: Boolean})
    public multiSelect?: boolean = false;

    public selectedAttributes: AttributeRef[] = [];
    public selectedAssets: string[] = [];

    @state()
    protected assetAttributes?: (Attribute<any>)[];
    protected asset?: Asset;

    @query("#add-btn")
    protected addBtn!: OrMwcInput;

    constructor() {
        super();

        this.heading = i18next.t("selectAttributes");
        this.setDialogContent();
        this.setDialogActions();
        this.dismissAction = null;

        this.styles = `
            .attributes-header {
                line-height: 48px;
                padding: 0 15px;
                background-color: ${unsafeCSS(DefaultColor2)};
                font-weight: bold;
                border-bottom: 1px solid ${unsafeCSS(DefaultColor2)};
            }
            footer.mdc-dialog__actions {
                border-top: 1px solid ${unsafeCSS(DefaultColor5)};
            }
            #header {
                background-color: ${unsafeCSS(DefaultColor4)} !important;
            }
            #dialog-content {
                padding: 0;
            }
        `;
    }

    public setShowOnlyDatapointAttrs(showOnlyDatapointAttrs: boolean | undefined): OrAttributePicker {
        this.showOnlyDatapointAttrs = showOnlyDatapointAttrs;
        return this;
    }

    public setShowOnlyRuleStateAttrs(showOnlyRuleStateAttrs: boolean | undefined): OrAttributePicker {
        this.showOnlyRuleStateAttrs = showOnlyRuleStateAttrs;
        return this;
    }

    public setAttributeFilter(attributeFilter: ((attribute: Attribute<any>) => boolean) | undefined): OrAttributePicker {
        this.attributeFilter = attributeFilter;
        return this;
    }

    public setMultiSelect(multiSelect: boolean | undefined): OrAttributePicker {
        this.multiSelect = multiSelect;
        return this;
    }

    public setSelectedAttributes(selectedAttributes: AttributeRef[]): OrAttributePicker {
        this.selectedAttributes = selectedAttributes;
        return this;
    }

    public setSelectedAssets(selectedAssets: string[]): OrAttributePicker {
        this.selectedAssets = selectedAssets;
        this.setDialogContent();
        return this;
    }

    public setOpen(isOpen: boolean): OrAttributePicker  {
        super.setOpen(isOpen);
        return this;
    }

    public setHeading(heading: TemplateResult | string | undefined): OrAttributePicker {
        super.setHeading(heading);
        return this;
    }

    public setContent(content: TemplateResult | (() => TemplateResult) | undefined): OrAttributePicker {
        throw new Error("Cannot modify attribute picker content");
    }

    public setActions(actions: DialogAction[] | undefined): OrAttributePicker {
        throw new Error("Cannot modify attribute picker actions");
    }

    public setDismissAction(action: DialogActionBase | null | undefined): OrAttributePicker {
        throw new Error("Cannot modify attribute picker dismiss action");
    }

    public setStyles(styles: string | TemplateResult | undefined): OrAttributePicker {
        throw new Error("Cannot modify attribute picker styles");
    }

    public setAvatar(avatar: boolean | undefined): OrAttributePicker {
        throw new Error("Cannot modify attribute picker avatar setting");
    }

    protected setDialogActions(): void {
        this.actions = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                actionName: "add",
                content: html`<or-mwc-input id="add-btn" class="button" label="add"
                                            .type="${InputType.BUTTON}" ?disabled="${!this.selectedAttributes.length}"
                                            @or-mwc-input-changed="${(ev: Event) => { if (!this.selectedAttributes.length) { ev.stopPropagation(); return false; } } }"></or-mwc-input>`,
                action: () => {

                    if (!this.selectedAttributes.length) {
                        return;
                    }

                    this.dispatchEvent(new OrAttributePickerPickedEvent(this.selectedAttributes));
                }
            }
        ];
    }

    protected setDialogContent(): void {

        const getListItems: () => ListItem[] = () => {
            if (!this.assetAttributes) {
                return [];
            }

            return this.assetAttributes.map((attribute) => {
                return {
                    text: Util.getAttributeLabel(undefined, attribute, undefined, true),
                    value: attribute.name
                } as ListItem
            });
        };

        let selectedAttribute: ListItem | undefined = undefined;
        if (!this.multiSelect && this.selectedAttributes.length === 1 && this.selectedAttributes[0].name) {
            selectedAttribute = {
                text: Util.getAttributeLabel(undefined, this.selectedAttributes[0], undefined, true),
                value: this.selectedAttributes[0].name
            };
        }

        this.content = () => html`
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 260px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <or-asset-tree id="chart-asset-tree" readonly
                                   .selectedIds="${ this.selectedAssets.length > 0 ? this.selectedAssets : null }"
                                   @or-asset-tree-selection="${(ev: OrAssetTreeSelectionEvent) => this._onAssetSelectionChanged(ev)}">
                    </or-asset-tree>
                </div>
                <div class="col" style="flex: 1 1 auto;width: 260px;overflow: auto;">
                ${this.assetAttributes && this.assetAttributes.length > 0 ? html`
                    <div class="attributes-header">
                        <or-translate value="attribute_plural"></or-translate>
                    </div>
                    ${this.multiSelect
                        ?
                        html`<div style="display: grid">
                                            <or-mwc-list
                                                    id="attribute-selector" .type="${ListType.MULTI_CHECKBOX}" .listItems="${getListItems()}"
                                                    .values="${this.selectedAttributes.filter(attributeRef => attributeRef.id === this.asset!.id).map(attributeRef => attributeRef.name!)}"
                                                    @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onAttributeSelectionChanged([...this.selectedAttributes.filter(attributeRef => attributeRef.id !== this.asset!.id),...ev.detail.map(li => {return {id: this.asset!.id!, name:li.value as string} as AttributeRef;})])}"></or-mwc-list>
                                        </div>`
                        :
                        html`<or-mwc-input id="attribute-selector"
                                                style="display:flex;"
                                                .label="${i18next.t("attribute")}"
                                                .type="${InputType.LIST}"
                                                .options="${getListItems().map(item => ([item, item.text]))}"
                                                @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                                    this._onAttributeSelectionChanged(
                                                        [
                                                            {
                                                                id: this.asset!.id!,
                                                                name: (ev.detail.value as ListItem).value as string
                                                            } as AttributeRef
                                                        ]);
                                                }}"></or-mwc-input>`}
                ` : html`<div style="display: flex;align-items: center;text-align: center;height: 100%;padding: 0 20px;"><span style="width:100%">
                            <or-translate value="${
            (this.assetAttributes && this.assetAttributes.length === 0) ?
                ((this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) ? "noDatapointsOrRuleStateAttributes" :
                        this.showOnlyDatapointAttrs ? "noDatapointsAttributes" :
                            this.showOnlyRuleStateAttrs ? "noRuleStateAttributes" : "noAttributesToShow"
                ) : "selectAssetOnTheLeft"}">
                            </or-translate></span></div>`}
                </div>
        `;
    }

    protected async _onAssetSelectionChanged(event: OrAssetTreeSelectionEvent) {
        this.assetAttributes = undefined;
        if (!this.multiSelect) {
            this.selectedAttributes = [];
        }
        this.addBtn.disabled = this.selectedAttributes.length === 0;
        const assetTree = event.target as OrAssetTree;
        assetTree.disabled = true;

        let selectedAsset = event.detail.newNodes.length === 0 ? undefined : event.detail.newNodes[0].asset;
        this.asset = selectedAsset;

        if (selectedAsset) {
            // Load the asset attributes
            const assetResponse = await manager.rest.api.AssetResource.get(selectedAsset.id!);
            selectedAsset = assetResponse.data;

            if (selectedAsset) {
                this.assetAttributes = Object.values(selectedAsset.attributes!).map(attr => { return {...attr, id: selectedAsset!.id!}; })
                    .sort(Util.sortByString((attribute) => attribute.name!));

                if (this.attributeFilter){
                    this.assetAttributes = this.assetAttributes.filter((attr) => this.attributeFilter!(attr))
                }

                if (this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyDatapointAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyRuleStateAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                }
            }
        }

        assetTree.disabled = false;
    }

    protected _onAttributeSelectionChanged(attributeRefs: AttributeRef[]) {
        this.selectedAttributes = attributeRefs;
        this.addBtn.disabled = this.selectedAttributes.length === 0;
    }
}
