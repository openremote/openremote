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
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { AssetModelUtil } from "@openremote/model";
import { style } from "../style";
import { when } from "lit/directives/when.js";
import { map } from "lit/directives/map.js";
import { guard } from "lit/directives/guard.js";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import manager, { DefaultColor5, Util } from "@openremote/core";
import { getAssetDescriptorIconTemplate } from "@openremote/or-icon";
import { OrAttributePicker, OrAttributePickerPickedEvent } from "@openremote/or-attribute-picker";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
export class AttributeActionEvent extends CustomEvent {
    constructor(asset, attributeRef, action) {
        super(AttributeActionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                asset: asset,
                attributeRef: attributeRef,
                action: action
            }
        });
    }
}
AttributeActionEvent.NAME = "attribute-action";
export class AttributesSelectEvent extends CustomEvent {
    constructor(assets, attributeRefs) {
        super(AttributesSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                assets: assets,
                attributeRefs: attributeRefs
            }
        });
    }
}
AttributesSelectEvent.NAME = "attribute-select";
const styling = css `
  #attribute-list {
    overflow: auto;
    flex: 1 1 0;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  .attribute-list-item {
    position: relative;
    cursor: pointer;
    display: flex;
    flex-direction: row;
    align-items: stretch;
    gap: 10px;
    padding: 0;
    min-height: 50px;
  }
  
  .attribute-list-item-icon {
    display: flex;
    align-items: center;
    --or-icon-width: 20px;
  }

  .attribute-list-item-label {
    display: flex;
    justify-content: center;
    flex: 1 1 0;
    line-height: 16px;
    flex-direction: column;
  }
  
  .attribute-list-item-actions {
    flex: 1;
    justify-content: end;
    align-items: center;
    display: flex;
    gap: 8px;
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

  .button-action {
    background: none;
    visibility: hidden;
    color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    display: inline-block;
    border: none;
    padding: 0;
    cursor: pointer;
  }

  .attribute-list-item:hover .attribute-list-item-actions {
    background: white;
    z-index: 1;
  }
  
  .attribute-list-item:hover .button-action {
    visibility: visible;
  }

  .button-action:hover {
    --or-icon-fill: var(--or-app-color4);
  }
`;
let AttributesPanel = class AttributesPanel extends LitElement {
    constructor() {
        super(...arguments);
        this.attributeRefs = [];
        this.multi = false;
        this.onlyDataAttrs = false;
        this.loadedAssets = [];
    }
    static get styles() {
        return [styling, style];
    }
    // Lit lifecycle method to compute values during update
    willUpdate(changedProps) {
        super.willUpdate(changedProps);
        if (!this.attributeRefs) {
            this.attributeRefs = [];
        }
        if (changedProps.has("attributeRefs") && this.attributeRefs) {
            this.loadAssets().then((assets) => {
                // Only dispatch event when it CHANGED, so not from 'undefined' to [];
                if (changedProps.get("attributeRefs")) {
                    this.dispatchEvent(new AttributesSelectEvent(assets, this.attributeRefs));
                }
            });
        }
    }
    getLoadedAsset(attrRef) {
        var _a;
        return (_a = this.loadedAssets) === null || _a === void 0 ? void 0 : _a.find((asset) => asset.id === attrRef.id);
    }
    removeWidgetAttribute(attributeRef) {
        if (this.attributeRefs != null) {
            this.attributeRefs = this.attributeRefs.filter(ar => ar !== attributeRef);
        }
    }
    loadAssets() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.attributeRefs.filter(ar => !this.getLoadedAsset(ar)).length > 0) {
                const assets = yield this.fetchAssets(this.attributeRefs);
                this.loadedAssets = assets;
                return assets;
            }
            else {
                return this.loadedAssets;
            }
        });
    }
    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    // TODO: Move this to more generic spot?
    fetchAssets(attributeRefs = []) {
        return __awaiter(this, void 0, void 0, function* () {
            let assets = [];
            yield manager.rest.api.AssetResource.queryAssets({
                ids: attributeRefs.map((x) => x.id),
                realm: { name: manager.displayRealm },
                select: {
                    attributes: attributeRefs.map((x) => x.name)
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, "errorOccurred");
            });
            return assets;
        });
    }
    onAttributeActionClick(asset, attributeRef, action) {
        this.dispatchEvent(new AttributeActionEvent(asset, attributeRef, action));
    }
    openAttributeSelector(attributeRefs, multi, onlyDataAttrs = true, attributeFilter) {
        let dialog;
        if (attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setSelectedAttributes(attributeRefs).setShowOnlyDatapointAttrs(onlyDataAttrs).setAttributeFilter(attributeFilter));
        }
        else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(multi).setShowOnlyDatapointAttrs(onlyDataAttrs));
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event) => {
            this.attributeRefs = event.detail;
        });
    }
    render() {
        return html `
            <div>
                ${when(this.attributeRefs.length > 0, () => html `

                    <div id="attribute-list">
                        ${guard([this.attributeRefs, this.loadedAssets, this.attributeActionCallback, this.attributeLabelCallback], () => html `
                            ${map(this.attributeRefs, (attributeRef) => {
            const asset = this.getLoadedAsset(attributeRef);
            if (asset) {
                const attribute = asset.attributes[attributeRef.name];
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, true);
                return html `
                                        <div class="attribute-list-item">
                                            <div class="attribute-list-item-icon">
                                                <span>${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type))}</span>
                                            </div>
                                            <div class="attribute-list-item-label">
                                                ${when(!!this.attributeLabelCallback, () => this.attributeLabelCallback(asset, attribute, label), () => html `
                                                            <span>${asset.name}</span>
                                                            <span style="font-size:14px; color:grey;">${label}</span>
                                                        `)}
                                            </div>
                                            <div class="attribute-list-item-actions">
                                                
                                                <!-- Custom actions defined by callback -->
                                                ${when(!!this.attributeActionCallback, () => {
                    return this.attributeActionCallback(attributeRef).map((action) => html `
                                                        <button class="button-action" .disabled="${action.disabled}" title="${action.tooltip}" @click="${() => this.onAttributeActionClick(asset, attributeRef, action)}">
                                                            <or-icon icon="${action.icon}"></or-icon>
                                                        </button>
                                                    `);
                })}
                                                <!-- Remove attribute button -->
                                                <button class="button-action" title="${i18next.t('delete')}" @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                                    <or-icon icon="close-circle"></or-icon>
                                                </button>
                                            </div>
                                        </div>
                                    `;
            }
            else {
                return undefined;
            }
        })}
                        `)}
                    </div>

                `, () => html `
                    <span style="padding: 14px 0; display: block;"><or-translate value="noAttributesConnected"></or-translate></span>
                `)}

                <!-- Button that opens attribute selection -->
                <or-mwc-input .type="${InputType.BUTTON}" label="attribute" icon="${(this.multi || this.attributeRefs.length === 0) ? "plus" : "swap-horizontal"}"
                              style="margin-top: 8px;"
                              @or-mwc-input-changed="${() => this.openAttributeSelector(this.attributeRefs, this.multi, this.onlyDataAttrs, this.attributeFilter)}">
                </or-mwc-input>
            </div>
        `;
    }
};
__decorate([
    property()
], AttributesPanel.prototype, "attributeRefs", void 0);
__decorate([
    property()
], AttributesPanel.prototype, "multi", void 0);
__decorate([
    property()
], AttributesPanel.prototype, "onlyDataAttrs", void 0);
__decorate([
    property()
], AttributesPanel.prototype, "attributeFilter", void 0);
__decorate([
    property()
], AttributesPanel.prototype, "attributeLabelCallback", void 0);
__decorate([
    property()
], AttributesPanel.prototype, "attributeActionCallback", void 0);
__decorate([
    state()
], AttributesPanel.prototype, "loadedAssets", void 0);
AttributesPanel = __decorate([
    customElement('attributes-panel')
], AttributesPanel);
export { AttributesPanel };
//# sourceMappingURL=attributes-panel.js.map