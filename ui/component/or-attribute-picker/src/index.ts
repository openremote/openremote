import {html, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-asset-tree";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-mwc-components/or-mwc-list";
import {OrAssetTree, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import manager, {DefaultColor2, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import {Attribute, AttributeRef, WellknownMetaItems} from "@openremote/model";
import {ListItem, ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

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

type AttributeAndId = Attribute<any> & {id: string};

@customElement("or-attribute-picker")
export class OrAttributePicker extends OrMwcDialog {

    @property({type: Boolean})
    public showOnlyDatapointAttrs?: boolean = false;

    @property({type: Boolean})
    public showOnlyRuleStateAttrs?: boolean = false;

    @property({type: Boolean})
    public multiSelect?: boolean = false;

    @state()
    protected assetAttributes?: (AttributeAndId)[];

    public selectedAttributes: AttributeRef[] = [];

    @query("#add-btn")
    protected addBtn!: OrMwcInput;

    constructor() {
        super();

        this.dialogTitle = i18next.t("selectAttributes");
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

    protected setDialogActions(): void {
        this.dialogActions = [
            {
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                actionName: "add",
                content: html`<or-mwc-input id="add-btn" class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" ?disabled="${!this.selectedAttributes.length}"></or-mwc-input>`,
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
                    value: attribute.name,
                    data: attribute
                } as ListItem
            });
        };


        this.dialogContent = () => html`
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 260px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <or-asset-tree id="chart-asset-tree" readonly
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
                                                    .values="${this.selectedAttributes.map(attribute => attribute.name!)}"
                                                    @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onAttributeSelectionChanged(ev.detail.map(li => li.data as AttributeAndId))}"></or-mwc-list>
                                        </div>`
                        :
                        html`<or-mwc-input id="attribute-selector"
                                                style="display:flex;"
                                                .label="${i18next.t("attribute")}"
                                                .type="${InputType.LIST}"
                                                .options="${getListItems().map(item => ([item, item.text]))}"
                                                @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onAttributeSelectionChanged([(ev.detail.value as ListItem).data as AttributeAndId])}"
                                                }"></or-mwc-input>`
                    }
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
        this.selectedAttributes = [];
        this.addBtn.disabled = true;
        const assetTree = event.target as OrAssetTree;
        assetTree.disabled = true;

        let selectedAsset = event.detail.newNodes.length === 0 ? undefined : event.detail.newNodes[0].asset;

        if (selectedAsset) {
            // Load the asset attributes
            const assetResponse = await manager.rest.api.AssetResource.get(selectedAsset.id!);
            selectedAsset = assetResponse.data;

            if (selectedAsset) {
                this.assetAttributes = Object.values(selectedAsset.attributes!).map(attr => { return {...attr, id: selectedAsset!.id!}; });

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

    protected _onAttributeSelectionChanged(attributes: AttributeAndId[]) {
        this.selectedAttributes = attributes.map(attr => {return {id: attr.id, name: attr.name} as AttributeRef});
        this.addBtn.disabled = this.selectedAttributes.length === 0;
    }
}
