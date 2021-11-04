import { LitElement, html, css, TemplateResult } from "lit";
import {customElement, property} from "lit/decorators.js";
import { Node, PickerType, AttributeInternalValue, AssetState, WellknownMetaItems, Asset, NodeDataType, AttributeRef } from "@openremote/model";
import { nodeConverter } from "../converters/node-converter";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import rest from "@openremote/rest";
import "@openremote/or-asset-tree";
import { ResizeObserver } from "resize-observer";
import { project } from "./flow-editor";
import { NodeUtilities } from "../node-structure";
import { translate, i18next } from "@openremote/or-translate";
import { PickerStyle } from "../styles/picker-styles";
import { AssetModelUtil, Util } from "@openremote/core";
import { OrMwcAttributeSelector } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrAddAttributeRefsEvent } from "@openremote/or-mwc-components/or-mwc-dialog";

@customElement("internal-picker")
export class InternalPicker extends translate(i18next)(LitElement) {
    @property({ converter: nodeConverter, reflect: true }) public node!: Node;
    @property({ type: Number, reflect: true }) public internalIndex!: number;

    @property({ type: Object }) private selectedAsset!: Asset;

    private resizeObserver!: ResizeObserver;

    constructor() {
        super();
    }

    public get internal() {
        return this.node.internals![this.internalIndex];
    }

    public static get styles() {
        return [css`
            :host{
                padding: 0;
                margin: 0;
                display: flex;
                flex-direction: column;
            }`,
            PickerStyle];
    }

    protected firstUpdated() {
        this.addEventListener("contextmenu", (e) => e.stopPropagation());
        this.addEventListener("mousedown", (e) => {
            project.createUndoSnapshot();
            return project.notifyChange();
        });
        this.resizeObserver = new ResizeObserver((a, b) => {
            const rect = a[0]!.contentRect;
            this.node.size = { x: rect.width - 20, y: rect.height - 20 };
        });
        this.resizeObserver.observe(this);
    }

    protected render() {
        switch (this.internal.picker!.type) {
            case PickerType.ASSET_ATTRIBUTE:
                if (this.internal.value && !this.selectedAsset)
                {
                    this.setSelectedAssetFromInternalValue();
                }
                return this.assetAttributeInput;
            case PickerType.COLOR:
                return this.colorInput;
            case PickerType.DOUBLE_DROPDOWN:
                return this.doubleDropdownInput;
            case PickerType.CHECKBOX:
                return this.checkBoxInput;
            case PickerType.DROPDOWN:
                return this.dropdownInput;
            case PickerType.MULTILINE:
                return this.multilineInput;
            case PickerType.NUMBER:
                return this.numberInput;
            case PickerType.TEXT:
                return this.textInput;
            default:
                return html`unimplemented picker`;
        }
    }

    private async setSocketTypeDynamically(value: AttributeInternalValue) {
        const results = (await rest.api.AssetResource.queryAssets({
            ids: [value.assetId!],
            select: {
                excludeParentInfo: true,
                excludePath: true,
                attributes: [
                    value.attributeName!
                ]
            }
        })).data;

        const socket = this.node.outputs![0] || this.node.inputs![0];
        socket.type = NodeDataType.ANY;
        if (results == null) { return; }
        if (results[0] == null) { return; }
        if (results[0].attributes == null) { return; }
        try {
            const relevantAttribute = results[0].attributes[value.attributeName!];
            const descriptors = AssetModelUtil.getValueDescriptors();
            const relevantDescriptor = descriptors.find((c) => c.name === relevantAttribute.type);
            socket.type = NodeUtilities.convertValueTypeToSocketType(relevantDescriptor!);
        } catch (e) {
            console.error(e);
        }
    }

    private async setSelectedAssetFromInternalValue(){
        const response = await rest.api.AssetResource.queryAssets({
            ids: [this.internal.value.assetId],
            select: {
                excludeParentInfo: true,
                excludePath: true
            }
        });

        if (response.data.length === 0) {
            console.warn(`Asset with id ${this.internal.value.assetId} is missing`);
            return;
        }
        this.selectedAsset = response.data[0];
    }

    private get assetAttributeInput(): TemplateResult {

        const openDialog = () => {
            let _selectedAttributes : AttributeRef[] = [];
            let val = this.node.internals![this.internalIndex].value;
            if (val){
                _selectedAttributes = [{
                    id: val.assetId,
                    name: val.attributeName
                }];
            }

            const hostElement = document.body;
            const dialog = new OrMwcAttributeSelector();
            dialog.showOnlyRuleStateAttrs = false;
            dialog.showOnlyDatapointAttrs = false;
            dialog.multiSelect = false;
            dialog.selectedAttributes = _selectedAttributes;
            dialog.isOpen = true;
            dialog.addEventListener(OrAddAttributeRefsEvent.NAME, async (ev: any) => {
                const value: AttributeInternalValue = {
                    assetId: ev.detail.selectedAttributes[0].id,
                    attributeName: ev.detail.selectedAttributes[0].name
                };
                await this.setSocketTypeDynamically(value);
                this.setValue(value);
                await this.setSelectedAssetFromInternalValue();
            });
    
            hostElement.append(dialog);
        };

        let selectedAttrLabel = '?';
        if (this.selectedAsset && this.selectedAsset.attributes && this.internal?.value?.attributeName)
        {
            const attrName = this.internal?.value?.attributeName;
            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, this.selectedAsset.type!);
            let attr = this.selectedAsset.attributes[attrName];
            if (attr)
                selectedAttrLabel = Util.getAttributeLabel(attr, attributeDescriptor, this.selectedAsset.type!, true)
        }

        return html`
        ${(this.selectedAsset ? html`<span class="attribute-label">${selectedAttrLabel}</span>` : null)}
        <or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("attribute")}" icon="format-list-bulleted-square" @click="${() => openDialog()}"></or-mwc-input>
        `;
    }

    private get colorInput(): TemplateResult {
        return html`<or-mwc-input type="color"></or-mwc-input>`; // looks strange
    }

    private get doubleDropdownInput(): TemplateResult {
        return html`unimplemented`;
    }

    private get dropdownInput(): TemplateResult {
        return html`<writable-dropdown @input="${(e: any) => this.setValue(e.target.value)}" .value="${this.internal.value}" .options="${this.internal.picker!.options}">
        </writable-dropdown>`;
    }

    private get checkBoxInput(): TemplateResult {
        return html`<input type="checkbox" ?checked="${this.internal.value || false}" @input="${(e: any) => this.setValue(e.target.checked)}"/>`;
        return html`<or-mwc-input type="checkbox" 
        @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                this.setValue(e.detail.value);
            }}"></or-mwc-input>`;
    }

    private get multilineInput(): TemplateResult {
        const sizeString = this.node.size ? `width:${this.node.size.x}px; height:${this.node.size.y}px` : ``;
        return html`<textarea @wheel="${(e: any) => {
            if (e.target.clientHeight < e.target.scrollHeight) {
                return e.stopPropagation();
            }
        }}" style="${sizeString}" @input="${(e: any) => this.setValue(e.target.value)}" placeholder="${this.internal.name!}">${this.internal.value || ""}</textarea>`;
    }

    private get numberInput(): TemplateResult {
        return html`<input @wheel="${(e: any) => {
            if (e.target === this.shadowRoot!.activeElement) {
                return e.stopPropagation();
            }
        }}" @input="${(e: any) => this.setValue(parseFloat(e.target.value))}" value="${this.internal.value || 0}" type="number" placeholder="${this.internal.name!}"/>`;
    }

    private get textInput(): TemplateResult {
        return html`<input @input="${(e: any) => this.setValue(e.target.value)}" value="${this.internal.value || ""}" type="text" placeholder="${this.internal.name!}"/>`;
    }

    private setValue(value: any) {
        this.node.internals![this.internalIndex].value = value;
        this.onPicked();
    }

    private async onPicked() {
        await this.updateComplete;
        this.dispatchEvent(new CustomEvent("picked"));
    }
}
