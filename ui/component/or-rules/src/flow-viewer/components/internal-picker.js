var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { AssetModelUtil } from "@openremote/model";
import { nodeConverter } from "../converters/node-converter";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import rest from "@openremote/rest";
import "@openremote/or-asset-tree";
import { ResizeObserver } from "resize-observer";
import { project } from "./flow-editor";
import { NodeUtilities } from "../node-structure";
import { translate, i18next } from "@openremote/or-translate";
import { PickerStyle } from "../styles/picker-styles";
import { Util } from "@openremote/core";
import { OrAttributePicker, OrAttributePickerPickedEvent } from "@openremote/or-attribute-picker";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { getAssetDescriptorIconTemplate } from "@openremote/or-icon";
let InternalPicker = class InternalPicker extends translate(i18next)(LitElement) {
    constructor() {
        super();
    }
    get internal() {
        return this.node.internals[this.internalIndex];
    }
    static get styles() {
        return [css `
            :host{
                padding: 0;
                margin: 0;
                display: flex;
                flex-direction: column;
                --or-app-color4: var(--or-mwc-input-color);
            }`,
            PickerStyle,
            css `.attribute-label-white {
                background: #ffffff;
            }
            .selected-asset-container {
                display: flex;
                align-items: center;
            }
            .selected-asset-container:hover {
                cursor: pointer;
                background-color: #F9F9F9;
            }
            .selected-asset-label {
                padding: 5px;
                display: flex;
                flex-direction: column;
                line-height: 16px;
                justify-content: flex-start;
                font-size: 14px;
                text-align: left;
            }
            .selected-asset-label .asset {
                color: rgb(76, 76, 76);
            }
            .selected-asset-label .asset-attribute {
                color: grey;
            }
            .selected-asset-icon {
                display: flex;
                justify-content: center;
                padding: 0px 5px 0px 5px;
                --or-icon-width: 20px;
            }`];
    }
    firstUpdated() {
        this.addEventListener("contextmenu", (e) => e.stopPropagation());
        this.addEventListener("mousedown", (e) => {
            project.createUndoSnapshot();
            return project.notifyChange();
        });
        this.resizeObserver = new ResizeObserver((a, b) => {
            const rect = a[0].contentRect;
            this.node.size = { x: rect.width - 20, y: rect.height - 20 };
        });
        this.resizeObserver.observe(this);
    }
    render() {
        switch (this.internal.picker.type) {
            case "ASSET_ATTRIBUTE" /* PickerType.ASSET_ATTRIBUTE */:
                if (this.internal.value && !this.selectedAsset) {
                    this.setSelectedAssetFromInternalValue();
                }
                return this.assetAttributeInput;
            case "COLOR" /* PickerType.COLOR */:
                return this.colorInput;
            case "DOUBLE_DROPDOWN" /* PickerType.DOUBLE_DROPDOWN */:
                return this.doubleDropdownInput;
            case "CHECKBOX" /* PickerType.CHECKBOX */:
                return this.checkBoxInput;
            case "DROPDOWN" /* PickerType.DROPDOWN */:
                return this.dropdownInput;
            case "MULTILINE" /* PickerType.MULTILINE */:
                return this.multilineInput;
            case "NUMBER" /* PickerType.NUMBER */:
                return this.numberInput;
            case "TEXT" /* PickerType.TEXT */:
                return this.textInput;
            default:
                return html `unimplemented picker`;
        }
    }
    setSocketTypeDynamically(value) {
        return __awaiter(this, void 0, void 0, function* () {
            const results = (yield rest.api.AssetResource.queryAssets({
                ids: [value.assetId],
                select: {
                    attributes: [
                        value.attributeName
                    ]
                }
            })).data;
            const socket = this.node.outputs[0] || this.node.inputs[0];
            socket.type = "ANY" /* NodeDataType.ANY */;
            if (results == null) {
                return;
            }
            if (results[0] == null) {
                return;
            }
            if (results[0].attributes == null) {
                return;
            }
            try {
                const relevantAttribute = results[0].attributes[value.attributeName];
                const descriptors = AssetModelUtil.getValueDescriptors();
                const relevantDescriptor = descriptors.find((c) => c.name === relevantAttribute.type);
                socket.type = NodeUtilities.convertValueTypeToSocketType(relevantDescriptor);
            }
            catch (e) {
                console.error(e);
            }
        });
    }
    setSelectedAssetFromInternalValue() {
        return __awaiter(this, void 0, void 0, function* () {
            const response = yield rest.api.AssetResource.queryAssets({
                ids: [this.internal.value.assetId]
            });
            if (response.data.length === 0) {
                console.warn(`Asset with id ${this.internal.value.assetId} is missing`);
                return;
            }
            this.selectedAsset = response.data[0];
        });
    }
    get assetAttributeInput() {
        var _a, _b, _c, _d;
        const openDialog = () => {
            let _selectedAttributes = [];
            let _selectedAssets = [];
            let val = this.node.internals[this.internalIndex].value;
            if (val) {
                _selectedAttributes = [{
                        id: val.assetId,
                        name: val.attributeName
                    }];
            }
            if (this.selectedAsset && this.selectedAsset.id) {
                _selectedAssets = [this.selectedAsset.id];
            }
            const dialog = showDialog(new OrAttributePicker()
                .setShowOnlyRuleStateAttrs(true)
                .setShowOnlyDatapointAttrs(false)
                .setMultiSelect(false)
                .setSelectedAttributes(_selectedAttributes))
                .setSelectedAssets(_selectedAssets);
            dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (ev) => __awaiter(this, void 0, void 0, function* () {
                const value = {
                    assetId: ev.detail[0].id,
                    attributeName: ev.detail[0].name
                };
                yield this.setSocketTypeDynamically(value);
                this.setValue(value);
                yield this.setSelectedAssetFromInternalValue();
            }));
        };
        let selectedAttrLabel = '?';
        if (this.selectedAsset && this.selectedAsset.attributes && ((_b = (_a = this.internal) === null || _a === void 0 ? void 0 : _a.value) === null || _b === void 0 ? void 0 : _b.attributeName)) {
            const attrName = (_d = (_c = this.internal) === null || _c === void 0 ? void 0 : _c.value) === null || _d === void 0 ? void 0 : _d.attributeName;
            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, this.selectedAsset.type);
            let attr = this.selectedAsset.attributes[attrName];
            if (attr) {
                selectedAttrLabel = Util.getAttributeLabel(attr, attributeDescriptor, this.selectedAsset.type, true);
            }
        }
        const descriptor = this.selectedAsset ? AssetModelUtil.getAssetDescriptor(this.selectedAsset.type) : undefined;
        const myIcon = getAssetDescriptorIconTemplate(descriptor);
        return html `<div>
            ${(this.selectedAsset ?
            html `<div class="attribute-label attribute-label-white selected-asset-container" @click="${() => openDialog()}">
                        <div class="selected-asset-icon">${myIcon}</div>
                        <div class="selected-asset-label">
                            <div class="asset">${this.selectedAsset.name}</div>
                            <div class="asset-attribute">${selectedAttrLabel}</div>
                        </div>
                    </div>` :
            html `<or-mwc-input class="attribute-label-white" .type="${InputType.BUTTON}" label="attribute" icon="plus" @or-mwc-input-changed="${() => openDialog()}"></or-mwc-input>`)}
        </div>`;
    }
    get colorInput() {
        return html `<or-mwc-input type="color"></or-mwc-input>`; // looks strange
    }
    get doubleDropdownInput() {
        return html `unimplemented`;
    }
    get dropdownInput() {
        return html `<writable-dropdown @input="${(e) => this.setValue(e.target.value)}" .value="${this.internal.value}" .options="${this.internal.picker.options}">
        </writable-dropdown>`;
    }
    get checkBoxInput() {
        return html `<input type="checkbox" ?checked="${this.internal.value || false}" @input="${(e) => this.setValue(e.target.checked)}"/>`;
        return html `<or-mwc-input type="checkbox" 
        @or-mwc-input-changed="${(e) => {
            this.setValue(e.detail.value);
        }}"></or-mwc-input>`;
    }
    get multilineInput() {
        const sizeString = this.node.size ? `width:${this.node.size.x}px; height:${this.node.size.y}px` : ``;
        return html `<textarea @wheel="${(e) => {
            if (e.target.clientHeight < e.target.scrollHeight) {
                return e.stopPropagation();
            }
        }}" style="${sizeString}" @input="${(e) => this.setValue(e.target.value)}" placeholder="${this.internal.name}">${this.internal.value || ""}</textarea>`;
    }
    get numberInput() {
        return html `<input @wheel="${(e) => {
            if (e.target === this.shadowRoot.activeElement) {
                return e.stopPropagation();
            }
        }}" @input="${(e) => this.setValue(parseFloat(e.target.value))}" value="${this.internal.value || 0}" type="number" placeholder="${this.internal.name}"/>`;
    }
    get textInput() {
        return html `<input @input="${(e) => this.setValue(e.target.value)}" value="${this.internal.value || ""}" type="text" placeholder="${this.internal.name}"/>`;
    }
    setValue(value) {
        this.node.internals[this.internalIndex].value = value;
        this.onPicked();
    }
    onPicked() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.updateComplete;
            this.dispatchEvent(new CustomEvent("picked"));
        });
    }
};
__decorate([
    property({ converter: nodeConverter, reflect: true })
], InternalPicker.prototype, "node", void 0);
__decorate([
    property({ type: Number, reflect: true })
], InternalPicker.prototype, "internalIndex", void 0);
__decorate([
    property({ type: Object })
], InternalPicker.prototype, "selectedAsset", void 0);
InternalPicker = __decorate([
    customElement("internal-picker")
], InternalPicker);
export { InternalPicker };
//# sourceMappingURL=internal-picker.js.map