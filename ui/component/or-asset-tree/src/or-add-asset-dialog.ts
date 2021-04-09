import {css, customElement, html, LitElement, property, query, unsafeCSS} from "lit-element";
import {AgentDescriptor, Asset, AssetDescriptor, WellknownAssets} from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import {AssetTreeConfig, OrAssetTreeSelectionEvent} from "./index";
import {
    createListGroup,
    ListGroupItem,
    ListItem,
    OrMwcList,
    OrMwcListChangedEvent
} from "@openremote/or-mwc-components/or-mwc-list";
import {i18next} from "@openremote/or-translate";
import {AssetModelUtil, DefaultColor2, DefaultColor5, Util} from "@openremote/core";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";

export type OrAddAssetDetail = {
    name: string | undefined;
    descriptor: AssetDescriptor | AgentDescriptor | undefined;
};

export class OrAddChangedEvent extends CustomEvent<OrAddAssetDetail> {

        public static readonly NAME = "or-add-asset-changed";

    constructor(addAssetDetail: OrAddAssetDetail) {
        super(OrAddChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: addAssetDetail
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAddChangedEvent.NAME]: OrAddChangedEvent;
    }
}

@customElement("or-add-asset-dialog")
export class OrAddAssetDialog extends LitElement {

    @property({attribute: false})
    public config!: AssetTreeConfig;

    @property({attribute: false})
    public agentTypes!: AgentDescriptor[];

    @property({attribute: false})
    public assetTypes!: AssetDescriptor[];

    @property({attribute: false})
    public parent?: Asset;

    @property({attribute: false})
    public selectedType?: AgentDescriptor | AssetDescriptor;

    @property({attribute: false})
    protected showParentAssetSelector: boolean = false;

    @property({attribute: false})
    selectedChildAssetType: string = "";

    public name: string = "New Asset";

    @query("#name-input")
    protected nameInput!: OrInput;

    @query("#agent-list")
    protected agentList?: OrMwcList;

    @query("#asset-list")
    protected assetList?: OrMwcList;

    @query("#parent-asset-list")
    protected parentAssetList?: OrMwcList;

    public static get styles() {
        // language=CSS
        return css`
            #name-wrapper {
                display: flex;
                padding: 10px;
            }
            
            #name-wrapper > * {
                margin: 0 5px;
                flex: 1;
            }
            #toggle-parent-selector,
            #remove-parent {
                flex: 0 0 50px;
            }
            
            #parent-selector {
                max-width: 250px;
                border-left: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
            }
            
            #mdc-dialog-form-add {
                display: flex;
                height: 600px;
                width: 800px;
                border: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
            }
            #asset-type-option-container {
                padding: 15px;
                flex: 1 1 auto;
                overflow: auto;
                max-width: 100%;
                font-size: 16px;
                background-color: var(--or-app-color2, ${unsafeCSS(DefaultColor2)});
            }
            #type-list {
                padding-left: 10px;
                width: 260px;
                overflow: auto;
                text-transform: capitalize;
                border-right: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
            }
            
        `;
    }
    
    constructor() {
        super();
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, (event: OrAssetTreeSelectionEvent) => {
            this.parent = event.detail.newNodes[0].asset;
        });
    }

    protected render() {

        const mapDescriptors: (descriptors: (AssetDescriptor | AgentDescriptor)[]) => ListItem[] =
            (descriptors) =>
                descriptors.map((descriptor) => {
                    return {
                        styleMap: {
                            "--or-icon-fill": descriptor.colour ? "#" + descriptor.colour : "unset"
                        },
                        icon: descriptor.icon,
                        text: Util.getAssetTypeLabel(descriptor),
                        value: descriptor.name!,
                        data: descriptor
                    }
                }).sort(Util.sortByString((listItem) => listItem.text));

        const agentItems = mapDescriptors(this.agentTypes);
        const assetItems = mapDescriptors(this.assetTypes);
        const lists: ListGroupItem[] = [];

        if (agentItems.length > 0) {
            lists.push(
                {
                    heading: i18next.t("agents"),
                    list: html`<or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) this.onTypeChanged(true, evt.detail[0] as ListItem); }}" .listItems="${agentItems}" id="agent-list"></or-mwc-list>`
                }
            );
        }
        if (assetItems.length > 0) {
            lists.push(
                {
                    heading: i18next.t("assets"),
                    list: html`<or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) this.onTypeChanged(false, evt.detail[0] as ListItem); }}" .listItems="${assetItems}" id="asset-list"></or-mwc-list>`
                }
            );
        }

        const parentStr = this.parent ? this.parent.name + " (" + this.parent.id + ")" : i18next.t("none");

        return html`
            <div class="col">
                <div id="name-wrapper">
                    <or-mwc-input id="name-input" .type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined .label="${i18next.t("name")}" .value="${this.name}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.onNameChanged(e.detail.value)}"></or-mwc-input>
                    <or-mwc-input id="parent" .type="${InputType.TEXT}" comfortable readonly outlined .label="${i18next.t("parent")}" .value="${parentStr}" @click="${() => this._onToggleParentAssetSelector()}"></or-mwc-input>
                    <or-mwc-input id="toggle-parent-selector" icon="pencil" type="${InputType.BUTTON}" @click="${() => this._onToggleParentAssetSelector()}"></or-mwc-input>
                    <or-mwc-input id="remove-parent" ?disabled="${!this.parent}" type="${InputType.BUTTON}" icon="close" @click="${() => this._onDeselectClicked()}"></or-mwc-input>
                </div>
                <form id="mdc-dialog-form-add" class="row">
                    <div id="type-list" class="col">
                        ${createListGroup(lists)}
                    </div>
                    <div id="asset-type-option-container" class="col">
                        ${!this.selectedType 
                        ? html`` 
                        : this.getTypeTemplate(this.selectedType)}
                    </div>
                    ${!this.showParentAssetSelector
                        ? html``
                        : html`<or-asset-tree id="parent-selector" class="col" .showSortBtn="${false}" selectedNodes readonly></or-asset-tree>`
                    }
                </form>
            </div>
        `;
    }

    protected getTypeTemplate(descriptor: AgentDescriptor | AssetDescriptor) {

        if (!descriptor.name) {
            return false;
        }
        
        const assetTypeInfo = AssetModelUtil.getAssetTypeInfo(descriptor.name),
            attributes = assetTypeInfo?.attributeDescriptors?.filter(e => !e.optional),
            optionalAttributes = assetTypeInfo?.attributeDescriptors?.filter(e => !!e.optional);

        return html`
            <or-icon style="--or-icon-fill: ${descriptor.colour ? "#" + descriptor.colour : "unset"}" id="type-icon" .icon="${descriptor.icon}"></or-icon>
            <or-translate style="text-transform: capitalize; margin-bottom: 1.5em" id="type-description" .value="${Util.getAssetTypeLabel(descriptor)}"></or-translate>
            
            ${!attributes
                ? html``
                : html`
                    <div>
                        <strong>${i18next.t("attribute_plural")}</strong>
                            <ul style="margin-top: 0.5em">
                                ${attributes.map(item => html`<li>${Util.getAttributeLabel(undefined, item, undefined, true)}</li>`)}
                            </ul>
                        </div>
                    `}

            ${!optionalAttributes
                ? html``
                : html`
                    <div>
                        <strong>${i18next.t("optional_attributes")}</strong>
                        <ul style="margin-top: 0.5em">
                            ${optionalAttributes.map(item => html`<li>${Util.getAttributeLabel(undefined, item, undefined, true)}</li>`)}
                        </ul>
                    </div>
                `} 
        `;
    }

    protected onTypeChanged(isAgent: boolean, listItem: ListItem) {
        const descriptor = listItem.data as AssetDescriptor | AgentDescriptor;
        this.selectedType = descriptor;

        // Deselect other list selection
        const otherList = isAgent ? this.assetList : this.agentList;
        if (otherList) {
            otherList.values = undefined;
        }
        this.onModified();
    };

    protected onNameChanged(name: string) {
        this.name = name;
        this.onModified();
    }

    protected onModified() {
        this.dispatchEvent(new OrAddChangedEvent({
            name: this.name,
            descriptor: this.selectedType
        }));
    }

    protected _onToggleParentAssetSelector(): void {
        this.showParentAssetSelector = !this.showParentAssetSelector; 
    }

    protected _onDeselectClicked() {
        this.parent = undefined;
    }
}
