var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var AttributeInputWidget_1;
import { css, html } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { customElement, query, queryAll } from "lit/decorators.js";
import { AttributeInputSettings } from "../settings/attribute-input-settings";
import { when } from "lit/directives/when.js";
import { throttle } from "lodash";
import { Util } from "@openremote/core";
import "@openremote/or-attribute-input";
function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        readonly: false,
        showHelperText: true
    };
}
const styling = css `
  #widget-wrapper {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }

  .attr-input {
    width: 100%;
    box-sizing: border-box;
  }
`;
let AttributeInputWidget = AttributeInputWidget_1 = class AttributeInputWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "Attribute",
            displayIcon: "form-textbox",
            getContentHtml(config) {
                return new AttributeInputWidget_1(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            },
            getSettingsHtml(config) {
                return new AttributeInputSettings(config);
            }
        };
    }
    // TODO: Improve this to be more efficient
    refreshContent(force) {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig));
    }
    static get styles() {
        return [...super.styles, styling];
    }
    disconnectedCallback() {
        var _a;
        super.disconnectedCallback();
        (_a = this.resizeObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
        delete this.resizeObserver;
    }
    updated(changedProps) {
        // If widgetConfig, and the attributeRefs of them have changed...
        if (changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            if (attributeRefs.length > 0 && !this.isAttributeRefLoaded(attributeRefs[0])) {
                this.loadAssets(attributeRefs);
            }
        }
        // Workaround for an issue with scalability of or-attribute-input when using 'display: flex'.
        // The percentage slider doesn't scale properly, causing the dragging knob to glitch.
        // Why? Because the Material Design element listens to a window resize, not a container resize.
        // So we manually trigger this event when the attribute-input-widget changes in size.
        if (!this.resizeObserver && this.widgetWrapperElem) {
            this.resizeObserver = new ResizeObserver(throttle(() => {
                window.dispatchEvent(new Event('resize'));
            }, 200));
            this.resizeObserver.observe(this.widgetWrapperElem);
        }
        return super.updated(changedProps);
    }
    loadAssets(attributeRefs) {
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
        });
    }
    render() {
        var _a;
        const config = this.widgetConfig;
        const attribute = (config.attributeRefs.length > 0 && ((_a = this.loadedAssets[0]) === null || _a === void 0 ? void 0 : _a.attributes)) ? this.loadedAssets[0].attributes[config.attributeRefs[0].name] : undefined;
        const readOnlyMetaItem = Util.getMetaValue("readOnly" /* WellknownMetaItems.READONLY */, attribute);
        return html `
            ${when(config.attributeRefs.length > 0 && attribute && this.loadedAssets && this.loadedAssets.length > 0, () => {
            var _a, _b;
            return html `
                    <div id="widget-wrapper">
                        <or-attribute-input class="attr-input" fullWidth
                                            .assetType="${(_a = this.loadedAssets[0]) === null || _a === void 0 ? void 0 : _a.type}"
                                            .attribute="${attribute}"
                                            .assetId="${(_b = this.loadedAssets[0]) === null || _b === void 0 ? void 0 : _b.id}"
                                            .disabled="${!this.loadedAssets}"
                                            .readonly="${config.readonly || readOnlyMetaItem || this.getEditMode()}"
                                            .hasHelperText="${config.showHelperText}"
                        ></or-attribute-input>
                    </div>
                `;
        }, () => html `
                <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                    <span><or-translate value="noAttributesConnected"></or-translate></span>
                </div>
            `)}
        `;
    }
};
__decorate([
    query("#widget-wrapper")
], AttributeInputWidget.prototype, "widgetWrapperElem", void 0);
__decorate([
    queryAll(".attr-input")
], AttributeInputWidget.prototype, "attributeInputElems", void 0);
AttributeInputWidget = AttributeInputWidget_1 = __decorate([
    customElement("attribute-input-widget")
], AttributeInputWidget);
export { AttributeInputWidget };
//# sourceMappingURL=attribute-input-widget.js.map